#!/bin/bash
echo "Backing up current deployed WAR (if exists)..."
if [ -f /opt/tomcat10/webapps/JobManagement_CODEBUILD.war ]; then
  cp /opt/tomcat10/webapps/JobManagement_CODEBUILD.war /tmp/codebuild_bak/JobManagement_CODEBUILD.war_bak
fi
