pipeline {
    agent any

    environment {
        TOMCAT_HOME = "/opt/tomcat10"
        WAR_NAME = "JobManagement.war"
        WAR_STORAGE = "/var/lib/jenkins"  // Save WAR in Jenkins directory before deployment
        INVENTORY_FILE = "/home/ubuntu/inventory"  // Ansible inventory file
        PLAYBOOK_FILE = "/home/ubuntu/ansible_tomcat_deploy.yml"  // Ansible playbook
    }

    stages {

        stage('Clean Workspace') {
            steps {
                cleanWs()
            }
        }

        stage('Checkout') {
            steps {
                git url: 'https://github.com/sai-pranay-teja/JobManagement.git', branch: 'main'
            }
        }
        
        stage('Compile') {
            steps {
                sh 'mkdir -p build/WEB-INF/classes'
                sh '''
                   javac -cp "${WORKSPACE}/src/main/webapp/WEB-INF/lib/*" -d build/WEB-INF/classes $(find src -name "*.java")
                   '''
            }
        }

        stage('Copy Resources') {
            steps {
                sh 'cp -R src/main/resources/* build/WEB-INF/classes/'
                sh 'cp -R src/main/webapp/* build/'
            }
        }
        
        stage('Package WAR') {
            steps {
                sh 'jar -cvf ${WAR_NAME} -C build .'
                sh 'mv ${WAR_NAME} ${WAR_STORAGE}/'  // Move WAR to Jenkins directory
                archiveArtifacts artifacts: "${WAR_STORAGE}/${WAR_NAME}", fingerprint: true
            }
        }

        stage('Deploy with Ansible') {
            steps {
                sh "ansible-playbook -i ${INVENTORY_FILE} ${PLAYBOOK_FILE}"
            }
        }
    }

    post {
        failure {
            echo 'Build or deployment failed.'
        }
        success {
            echo 'Build and deployment succeeded.'
        }
    }
}
