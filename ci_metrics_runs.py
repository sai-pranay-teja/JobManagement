#!/usr/bin/env python3
import os, time, sys, argparse, shutil, zipfile, requests
from pathlib import Path
from github import Github
import boto3
import jenkins as jenkins_module

# Disable Jenkins crumb checks
jenkins_module.Jenkins.maybe_add_crumb = lambda self, req: None

def ensure_dir(d: Path):
    d.mkdir(parents=True, exist_ok=True)

# --- GitHub Actions ---
def trigger_workflow_dispatch(run_id: int):
    repo = os.environ["GITHUB_REPO"]
    token = os.environ["GITHUB_TOKEN"]
    url = f"https://api.github.com/repos/{repo}/actions/workflows/ci-cd.yml/dispatches"
    headers = {"Authorization": f"Bearer {token}", "Accept": "application/vnd.github.v3+json"}
    payload = {"ref": "main", "inputs": {"run_id": str(run_id)}}
    r = requests.post(url, headers=headers, json=payload)
    if r.status_code != 204:
        print(f"[GitHub] dispatch error: {r.status_code} {r.text}", file=sys.stderr)
    r.raise_for_status()

def collect_github(run_id: int, outdir: Path):
    trigger_workflow_dispatch(run_id)
    time.sleep(5)
    gh = Github(os.environ["GITHUB_TOKEN"])
    repo = gh.get_repo(os.environ["GITHUB_REPO"])
    wf = repo.get_workflow("ci-cd.yml")
    for _ in range(60):
        for r in wf.get_runs(branch="main"):
            if r.event == "workflow_dispatch" and r.status == "completed":
                logs_url = r.raw_data["logs_url"]
                resp = requests.get(logs_url, headers={"Authorization": f"Bearer {os.environ['GITHUB_TOKEN']}"})
                resp.raise_for_status()
                zp = outdir / f"github-{run_id}.zip"
                with open(zp, "wb") as f:
                    f.write(resp.content)
                # extract only first .log or .txt
                with zipfile.ZipFile(zp, 'r') as z:
                    for name in z.namelist():
                        if name.lower().endswith(('.log', '.txt')):
                            with z.open(name) as src, open(outdir / f"github-{run_id}.log", 'wb') as dst:
                                dst.write(src.read())
                            break
                zp.unlink()
                return
        time.sleep(10)
    print("⏰ GitHub timeout", file=sys.stderr)

# --- AWS CodeBuild ---
def collect_codebuild(run_id: int, outdir: Path):
    cb = boto3.client("codebuild")
    build_id = cb.start_build(projectName=os.environ["CODEBUILD_PROJECT"])["build"]["id"]
    for _ in range(60):
        b = cb.batch_get_builds(ids=[build_id])["builds"][0]
        if b["buildStatus"] in ("SUCCEEDED", "FAILED", "FAULT", "TIMED_OUT"):
            logs_info = b.get("logs", {})
            # CloudWatch logs
            group = logs_info.get("groupName")
            stream = logs_info.get("streamName")
            if group and stream:
                events = boto3.client("logs").get_log_events(
                    logGroupName=group, logStreamName=stream
                )["events"]
                with open(outdir / f"codebuild-{run_id}.log", 'w', encoding='utf-8', errors='replace') as f:
                    for e in events:
                        f.write(e.get("message", "") + '\n')
                return
            # S3 logs via location field
            s3logs = logs_info.get("s3Logs", {})
            loc = s3logs.get("location")
            if loc and loc.startswith("s3://"):
                _, path = loc.split("s3://", 1)
                bucket, key = path.split("/", 1)
                obj = boto3.client("s3").get_object(Bucket=bucket, Key=key)
                body = obj["Body"].read().decode('utf-8', errors='replace')
                with open(outdir / f"codebuild-{run_id}.log", 'w', encoding='utf-8', errors='replace') as f:
                    f.write(body)
                return
            print(f"⏰ No logs for CodeBuild {build_id}", file=sys.stderr)
            return
        time.sleep(10)
    print("⏰ CodeBuild timeout", file=sys.stderr)

# --- Jenkins Pipeline ---
def collect_jenkins(run_id: int, outdir: Path):
    server = jenkins_module.Jenkins(
        os.environ["JENKINS_URL"],
        username=os.environ["JENKINS_USER"],
        password=os.environ["JENKINS_TOKEN"]
    )
    job = os.environ["JENKINS_JOB"]
    info = server.get_job_info(job)
    prev_num = info.get("lastBuild", {}).get("number", 0)
    server.build_job(job)
    for _ in range(60):
        info = server.get_job_info(job)
        last = info.get("lastBuild", {}).get("number", 0)
        if last > prev_num:
            build_info = server.get_build_info(job, last)
            if not build_info.get("building", True):
                console = server.get_build_console_output(job, last)
                with open(outdir / f"jenkins-{run_id}.log", 'w', encoding='utf-8', errors='replace') as f:
                    f.write(console)
                return
        time.sleep(5)
    print("⏰ Jenkins timeout", file=sys.stderr)

# --- Main Entry ---
def main():
    p = argparse.ArgumentParser()
    p.add_argument("-n","--runs", type=int, default=10, help="runs per tool")
    p.add_argument("-o","--outdir", default="ci-logs", help="output folder")
    args = p.parse_args()

    base = Path(args.outdir)
    for tool in ("github", "codebuild", "jenkins"):
        d = base / tool
        if d.exists(): shutil.rmtree(d)
        ensure_dir(d)

    for i in range(1, args.runs + 1):
        print(f"\n=== Run {i}/{args.runs} ===")
        collect_github(i, base / "github")
        collect_codebuild(i, base / "codebuild")
        collect_jenkins(i, base / "jenkins")
        time.sleep(5)

if __name__ == "__main__":
    main()
