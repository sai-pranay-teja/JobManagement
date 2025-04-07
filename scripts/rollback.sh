#!/bin/bash
echo "Rolling back to backup WAR file..."
if [ -f /tmp/codebuild_bak/JobManagement_CODEBUILD.war_bak ]; then
  cp /tmp/codebuild_bak/JobManagement_CODEBUILD.war_bak /opt/tomcat10/webapps/JobManagement_CODEBUILD.war
  sudo /opt/tomcat10/bin/shutdown.sh || true
  sleep 5
  sudo /opt/tomcat10/bin/startup.sh
  echo "Rollback completed."
else
  echo "No backup WAR file found! Rollback aborted."
  exit 1
fi
