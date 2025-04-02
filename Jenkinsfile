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
        
        // stage('Package WAR') {
        //     steps {
        //         sh 'jar -cvf ${WAR_NAME} -C build .'
        //         sh 'mv ${WAR_NAME} ${WAR_STORAGE}/'  // Move WAR to Jenkins directory
        //         archiveArtifacts artifacts: "${WAR_STORAGE}/${WAR_NAME}", fingerprint: true, allowEmptyArchive: true
        //     }
        // }
        stage('Package WAR') {
            steps {
                sh 'jar -cvf ${WORKSPACE}/${WAR_NAME} -C build .'  // WAR is created in workspace
            //    sh 'mv ${WORKSPACE}/${WAR_NAME} ${WAR_STORAGE}/'  // Move WAR to /var/lib/jenkins
                sh 'ls -lh ${WORKSPACE}/'  // Debugging: Check if WAR exists
                archiveArtifacts artifacts: "${WAR_NAME}", fingerprint: true
    }
}


//        stage('Verify Playbook Exists') {
//             steps {
//                 sh 'ls -l /var/lib/jenkins/workspace/'
//                 sh 'cat /var/lib/jenkins/workspace/ansible_tomcat_deploy.yml'
//     }
// }


// stage('Check Ansible Path') {
//     steps {
//         sh 'which ansible-playbook'
//         sh 'ansible-playbook --version'
//     }
// }

stage('Check Ansible Path') {
    steps {
        sh 'echo $PATH'
        sh 'which ansible-playbook || echo "Ansible not found"'
    }
}


        // stage('Deploy with Ansible') {
        //     steps {
        //         // sh "ansible-playbook -i ${INVENTORY_FILE} ${PLAYBOOK_FILE}"
        //         sh '/usr/local/bin/ansible-playbook -i $WORKSPACE/inventory $WORKSPACE/ansible_tomcat_deploy.yml'
        //     }
        // }
        
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


