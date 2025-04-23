import os
print("GITHUB_REPO =", os.environ.get("GITHUB_REPO"))
# Optionally, check the token in a secure way (e.g., printing only a part of it)
token = os.environ.get("GITHUB_TOKEN")
print("GITHUB_TOKEN =", token[:4] + "..." if token else "Not Set")
