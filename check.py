import os
import jenkins

server = jenkins.Jenkins(
    os.environ["JENKINS_URL"],
    username=os.environ["JENKINS_USER"],
    password=os.environ["JENKINS_TOKEN"]
)

job = os.environ["JENKINS_JOB"]
print(server.get_job_info(job))
