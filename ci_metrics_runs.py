# #!/usr/bin/env python3
# import os, time, sys, argparse, shutil
# from pathlib import Path
# from github import Github
# import boto3
# import jenkins

# def ensure_dir(d: Path):
#     d.mkdir(parents=True, exist_ok=True)

# def trigger_and_collect_github(run_id: int, outdir: Path):
#     gh = Github(os.environ["GITHUB_TOKEN"])
#     repo = gh.get_repo(os.environ["GITHUB_REPO"])
#     # dispatch your workflow
#     repo.create_workflow_dispatch(
#         workflow_id="ci-cd.yml",
#         ref="main",
#         inputs={"run_id": str(run_id)}
#     )
#     time.sleep(5)
#     # poll for completion
#     workflow = repo.get_workflow("ci-cd.yml")
#     for _ in range(60):
#         runs = workflow.get_runs(branch="main")
#         for r in runs:
#             if r.event == "workflow_dispatch" and r.status == "completed":
#                 log_zip = repo.get_workflow_run(r.id).download_logs()
#                 with open(outdir / f"github-{run_id}.zip", "wb") as f:
#                     f.write(log_zip)
#                 return
#         time.sleep(10)
#     print("⏰ GitHub run timeout", file=sys.stderr)

# def trigger_and_collect_codebuild(run_id: int, outdir: Path):
#     cb = boto3.client("codebuild")
#     resp = cb.start_build(projectName=os.environ["CODEBUILD_PROJECT"])
#     build_id = resp["build"]["id"]
#     for _ in range(60):
#         build = cb.batch_get_builds(ids=[build_id])["builds"][0]
#         status = build["buildStatus"]
#         if status in ("SUCCEEDED","FAILED","FAULT","TIMED_OUT"):
#             logs = build["logs"]
#             cw = boto3.client("logs")
#             events = cw.get_log_events(
#                 logGroupName=logs["cloudWatchLogs"]["groupName"],
#                 logStreamName=logs["cloudWatchLogs"]["streamName"]
#             )["events"]
#             with open(outdir / f"codebuild-{run_id}.log","w") as f:
#                 for e in events:
#                     f.write(e["message"] + "\n")
#             return
#         time.sleep(10)
#     print("⏰ CodeBuild timeout", file=sys.stderr)

# def trigger_and_collect_jenkins(run_id: int, outdir: Path):
#     server = jenkins.Jenkins(
#         os.environ["JENKINS_URL"],
#         username=os.environ["JENKINS_USER"],
#         password=os.environ["JENKINS_TOKEN"]
#     )
#     job = os.environ["JENKINS_JOB"]
#     next_number = server.get_job_info(job)["nextBuildNumber"]
#     server.build_job(job, {"RUN_ID": run_id})
#     for _ in range(60):
#         info = server.get_build_info(job, next_number)
#         if not info["building"]:
#             console = server.get_build_console_output(job, next_number)
#             with open(outdir / f"jenkins-{run_id}.log","w") as f:
#                 f.write(console)
#             return
#         time.sleep(10)
#     print("⏰ Jenkins timeout", file=sys.stderr)

# def main():
#     parser = argparse.ArgumentParser(description="Run CI metrics across platforms")
#     parser.add_argument("-n","--runs", type=int, default=10,
#                         help="Number of runs per tool")
#     parser.add_argument("-o","--outdir", default="ci-logs",
#                         help="Directory to save all logs")
#     args = parser.parse_args()

#     # prepare directories
#     base = Path(args.outdir)
#     for tool in ("github","codebuild","jenkins"):
#         d = base / tool
#         if d.exists():
#             shutil.rmtree(d)
#         ensure_dir(d)

#     # trigger each tool, one run at a time
#     for i in range(1, args.runs + 1):
#         print(f"\n=== Run {i}/{args.runs} ===")
#         trigger_and_collect_github(i, base / "github")
#         trigger_and_collect_codebuild(i, base / "codebuild")
#         trigger_and_collect_jenkins(i, base / "jenkins")
#         time.sleep(5)

# if __name__ == "__main__":
#     main()

#!/usr/bin/env python3
import os
import time
import sys
import argparse
import shutil
from pathlib import Path
from github import Github
import boto3
import jenkins
import requests  # Add this import

def ensure_dir(d: Path):
    d.mkdir(parents=True, exist_ok=True)

# Add this new function for dispatching the workflow directly using GitHub's API.
def trigger_workflow_dispatch(run_id: int):
    repo_name = os.environ["GITHUB_REPO"]  # Format: "owner/repo"
    token = os.environ["GITHUB_TOKEN"]
    # Use your workflow file name (or ID) in the URL
    url = f"https://api.github.com/repos/{repo_name}/actions/workflows/ci-cd.yml/dispatches"
    headers = {
        "Authorization": f"Bearer {token}",
        "Accept": "application/vnd.github.v3+json"
    }
    data = {
        "ref": "main",  # The branch you want to run on.
        "inputs": {"run_id": str(run_id)}
    }
    response = requests.post(url, headers=headers, json=data)
    response.raise_for_status()
    print("Workflow dispatch triggered successfully.")

def trigger_and_collect_github(run_id: int, outdir: Path):
    # Use our custom API call helper instead of repo.create_workflow_dispatch:
    trigger_workflow_dispatch(run_id)
    time.sleep(5)
    
    # Now, continue with polling the workflow run status.
    gh = Github(os.environ["GITHUB_TOKEN"])
    repo = gh.get_repo(os.environ["GITHUB_REPO"])
    workflow = repo.get_workflow("ci-cd.yml")
    for _ in range(60):
        runs = workflow.get_runs(branch="main")
        for r in runs:
            if r.event == "workflow_dispatch" and r.status == "completed":
                log_zip = repo.get_workflow_run(r.id).download_logs()
                with open(outdir / f"github-{run_id}.zip", "wb") as f:
                    f.write(log_zip)
                return
        time.sleep(10)
    print("⏰ GitHub run timeout", file=sys.stderr)

def trigger_and_collect_codebuild(run_id: int, outdir: Path):
    cb = boto3.client("codebuild")
    resp = cb.start_build(projectName=os.environ["CODEBUILD_PROJECT"])
    build_id = resp["build"]["id"]
    for _ in range(60):
        build = cb.batch_get_builds(ids=[build_id])["builds"][0]
        status = build["buildStatus"]
        if status in ("SUCCEEDED","FAILED","FAULT","TIMED_OUT"):
            logs = build["logs"]
            cw = boto3.client("logs")
            events = cw.get_log_events(
                logGroupName=logs["cloudWatchLogs"]["groupName"],
                logStreamName=logs["cloudWatchLogs"]["streamName"]
            )["events"]
            with open(outdir / f"codebuild-{run_id}.log","w") as f:
                for e in events:
                    f.write(e["message"] + "\n")
            return
        time.sleep(10)
    print("⏰ CodeBuild timeout", file=sys.stderr)

def trigger_and_collect_jenkins(run_id: int, outdir: Path):
    server = jenkins.Jenkins(
        os.environ["JENKINS_URL"],
        username=os.environ["JENKINS_USER"],
        password=os.environ["JENKINS_TOKEN"]
    )
    job = os.environ["JENKINS_JOB"]
    next_number = server.get_job_info(job)["nextBuildNumber"]
    server.build_job(job, {"RUN_ID": run_id})
    for _ in range(60):
        info = server.get_build_info(job, next_number)
        if not info["building"]:
            console = server.get_build_console_output(job, next_number)
            with open(outdir / f"jenkins-{run_id}.log","w") as f:
                f.write(console)
            return
        time.sleep(10)
    print("⏰ Jenkins timeout", file=sys.stderr)

def main():
    parser = argparse.ArgumentParser(description="Run CI metrics across platforms")
    parser.add_argument("-n","--runs", type=int, default=10,
                        help="Number of runs per tool")
    parser.add_argument("-o","--outdir", default="ci-logs",
                        help="Directory to save all logs")
    args = parser.parse_args()

    # Prepare directories
    base = Path(args.outdir)
    for tool in ("github", "codebuild", "jenkins"):
        d = base / tool
        if d.exists():
            shutil.rmtree(d)
        ensure_dir(d)

    # Trigger each tool, one run at a time.
    for i in range(1, args.runs + 1):
        print(f"\n=== Run {i}/{args.runs} ===")
        trigger_and_collect_github(i, base / "github")
        trigger_and_collect_codebuild(i, base / "codebuild")
        trigger_and_collect_jenkins(i, base / "jenkins")
        time.sleep(5)

if __name__ == "__main__":
    main()

