#!/usr/bin/env python3

import os
import time
import subprocess
import requests

REPO = os.environ["GITHUB_REPOSITORY"]
TOKEN = os.environ["GITHUB_TOKEN"]
BRANCH = os.environ.get("GITHUB_BRANCH", "main")

DEPLOY_SCRIPT = "./deploy.sh"
DEPLOYED_FILE = "./deployed-commit"

API_URL = f"https://api.github.com/repos/{REPO}/commits/{BRANCH}"

headers = {
    "Authorization": f"Bearer {TOKEN}",
    "Accept": "application/vnd.github+json"
}

def get_latest_commit():
    response = requests.get(API_URL, headers=headers, timeout=10)
    response.raise_for_status()
    return response.json()["sha"]

def get_deployed_commit():
    try:
        with open(DEPLOYED_FILE) as f:
            return f.read().strip()
    except FileNotFoundError:
        return None

def set_deployed_commit(commit):
    with open(DEPLOYED_FILE, "w") as f:
        f.write(commit)

while True:
    try:
        latest = get_latest_commit()
        deployed = get_deployed_commit()

        if latest != deployed:
            print(f"New commit detected: {latest}", flush=True)

            result = subprocess.run([DEPLOY_SCRIPT], check=False)

            if result.returncode == 0:
                set_deployed_commit(latest)
                print("Deployment successful", flush=True)
            else:
                print(f"Deployment failed with exit code: {result.returncode}", flush=True)
    except Exception as e:
        print(f"Watcher error: {e}", flush=True)

    time.sleep(120)