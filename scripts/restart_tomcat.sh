#!/bin/bash
echo "Restarting Tomcat..."
sudo rm -rf /opt/tomcat10/webapps/JobManagement_CODEBUILD
sleep 5
sudo /opt/tomcat10/bin/startup.sh
