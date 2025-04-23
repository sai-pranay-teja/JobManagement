#!/usr/bin/env python3
import os, time, sys, argparse, shutil, requests
from pathlib import Path
from github import Github
import boto3
import jenkins as jenkins_module
import re

# Disable Jenkins crumb checks
jenkins_module.Jenkins.maybe_add_crumb = lambda self, req: None

def ensure_dir(d: Path):
    d.mkdir(parents=True, exist_ok=True)

# --- GitHub Actions ---
def collect_github(run_id: int, outdir: Path):
    gh = Github(os.environ["GITHUB_TOKEN"])
    repo = gh.get_repo(os.environ["GITHUB_REPO"])
    wf = repo.get_workflow("ci-cd.yml")
    prev_ids = {r.id for r in wf.get_runs(branch="main")}
    # trigger dispatch
    url = f"https://api.github.com/repos/{os.environ['GITHUB_REPO']}/actions/workflows/ci-cd.yml/dispatches"
    headers = {"Authorization": f"Bearer {os.environ['GITHUB_TOKEN']}", "Accept": "application/vnd.github.v3+json"}
    requests.post(url, headers=headers, json={"ref":"main","inputs":{"run_id":str(run_id)}}).raise_for_status()
    # poll for new run
    for _ in range(60):
        for r in wf.get_runs(branch="main"):
            if r.id not in prev_ids and r.status == "completed":
                logs_url = r.raw_data.get("logs_url")
                resp = requests.get(logs_url, headers=headers)
                resp.raise_for_status()
                from io import BytesIO
                import zipfile
                zf = zipfile.ZipFile(BytesIO(resp.content))
                for name in zf.namelist():
                    if name.lower().endswith(('.log', '.txt')):
                        data = zf.read(name)
                        text = data.decode('utf-8', errors='ignore')
                        # suffix = 'rollback' if 'rollback deployment time' in text.lower() else 'deploy'
                        m = re.search(r'\|\s*Rollback Time.*\|\s*([0-9]+)\s*\|', text)
                        suffix = 'rollback' if m else 'deploy'
                        path = outdir / f"gha-{suffix}-{run_id}.log"
                        with open(path, 'wb') as f:
                            f.write(data)
                        return
        time.sleep(10)
    print("⏰ GitHub timeout", file=sys.stderr)

# --- AWS CodeBuild ---
def collect_codebuild(run_id: int, outdir: Path):
    cb = boto3.client("codebuild")
    build = cb.start_build(projectName=os.environ["CODEBUILD_PROJECT"])["build"]
    build_id = build["id"]
    for _ in range(60):
        binfo = cb.batch_get_builds(ids=[build_id])["builds"][0]
        if binfo["buildStatus"] in ("SUCCEEDED","FAILED","FAULT","TIMED_OUT"):
            logs_info = binfo.get("logs", {})
            # CloudWatch logs: groupName and streamName at top level
            group = logs_info.get("groupName")
            stream = logs_info.get("streamName")
            if group and stream:
                events = boto3.client("logs").get_log_events(
                    logGroupName=group, logStreamName=stream
                )["events"]
                log_text = "\n".join(e.get("message","") for e in events)
            else:
                log_text = None
            if log_text is not None:
                m = re.search(r'\|\s*Rollback Time.*\|\s*([0-9]+)\s*\|', log_text)
                suffix = 'rollback' if m else 'deploy'
                path = outdir / f"codebuild-{suffix}-{run_id}.log"
                with open(path, 'w', encoding='utf-8', errors='replace') as f:
                    f.write(log_text)
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
                suffix = 'rollback' if 'rollback took' in console.lower() else 'deploy'
                path = outdir / f"jenkins-{suffix}-{run_id}.log"
                with open(path, 'w', encoding='utf-8', errors='replace') as f:
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
    for tool in ("gha","codebuild","jenkins"):
        d = base / tool
        if d.exists(): shutil.rmtree(d)
        ensure_dir(d)

    for i in range(1, args.runs+1):
        print(f"\n=== Run {i}/{args.runs} ===")
        collect_github(i, base/"gha")
        collect_codebuild(i, base/"codebuild")
        collect_jenkins(i, base/"jenkins")
        time.sleep(5)

if __name__=="__main__":
    main()
