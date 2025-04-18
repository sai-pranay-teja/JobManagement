/*
 * Pattern-Driven CI/CD Jenkinsfile (Java-Servlet Job Portal)
 * Includes optimization patterns and per-stage benchmarking hooks.
 */

def pipelineStartTime = 0L

def leadTimeForChanges = 0L

def rollbackTime = 'N/A'

pipeline {
    agent any

    environment {
        TOMCAT_HOME           = '/opt/tomcat10'
        WAR_NAME              = 'JobManagement_JENKINS.war'
        DEPLOY_DIR            = "${TOMCAT_HOME}/webapps"
        WAR_STORAGE           = "${WORKSPACE}"
        SSH_KEY               = '/var/lib/jenkins/.ssh/id_rsa'
        SSH_USER              = 'root'
        SSH_HOST              = '18.61.60.110'
        SSH_OPTS              = '-o StrictHostKeyChecking=no'
        BACKUP_DIR            = '/tmp/jenkins_bak'

        DEPLOYMENT_TIME_FILE  = "${WORKSPACE}/deployment_time.log"
        TEST_RESULTS_LOG      = "${WORKSPACE}/test_results.log"
        ROLLBACK_LOG          = "${WORKSPACE}/rollback.log"
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Initialize') {
            steps {
                script {
                    pipelineStartTime = sh(script: 'date +%s', returnStdout: true).trim().toInteger()
                }
            }
        }

        stage('Checkout') {
            steps {
                git url: 'https://github.com/sai-pranay-teja/JobManagement.git', branch: 'main'
            }
        }

        stage('Build WAR (Incremental)') {
            steps {
                script {
                    sh 'mkdir -p build/WEB-INF/classes'
                    def changed = sh(
                        script: 'find src -name "*.java" -newer build/WEB-INF/classes',
                        returnStdout: true
                    ).trim()
                    if (changed) {
                        echo "Compiling changed files:\n${changed}"
                        sh "javac -cp 'src/main/webapp/WEB-INF/lib/*' -d build/WEB-INF/classes ${changed}"
                    } else {
                        echo 'No changes detected — performing full compile.'
                        sh '''
                            find src -name "*.java" | xargs javac -cp 'src/main/webapp/WEB-INF/lib/*' -d build/WEB-INF/classes
                        '''
                    }
                    sh '''
                        cp -R src/main/resources/* build/WEB-INF/classes/
                        cp -R src/main/webapp/* build/
                        jar -cvf ${WAR_NAME} -C build .
                    '''
                }
            }
        }

        stage('Backup WAR') {
            steps {
                script {
                    sh "mkdir -p ${BACKUP_DIR}"
                    if (fileExists("${WAR_STORAGE}/${WAR_NAME}")) {
                        sh "cp ${WAR_STORAGE}/${WAR_NAME} ${BACKUP_DIR}/${WAR_NAME}_bak"
                    } else {
                        echo "WAR not found — no backup created."
                    }
                }
            }
        }

        stage('Run Parallel Tests') {
            parallel {
                stage('Unit Tests (Part1)') {
                    steps {
                        sh '''
                            mkdir -p test_output_unit
                            javac -cp 'src/main/webapp/WEB-INF/lib/*:src' -d test_output_unit src/main/test/TestAppPart1.java
                            java -cp 'test_output_unit:src/main/webapp/WEB-INF/lib/*' org.junit.platform.console.ConsoleLauncher --select-class TestAppPart1 --details summary > ${TEST_RESULTS_LOG}-unit 2>&1 || true
                        '''
                    }
                }
                stage('Integration Tests (Part2)') {
                    steps {
                        sh '''
                            mkdir -p test_output_integration
                            javac -cp 'src/main/webapp/WEB-INF/lib/*:src' -d test_output_integration src/main/test/TestAppPart2.java
                            java -cp 'test_output_integration:src/main/webapp/WEB-INF/lib/*' org.junit.platform.console.ConsoleLauncher --select-class TestAppPart2 --details summary > ${TEST_RESULTS_LOG}-integration 2>&1 || true
                        '''
                    }
                }
            }
        }

        stage('Early Test Failure Exit') {
            steps {
                script {
                    def unitLog = readFile("${TEST_RESULTS_LOG}-unit")
                    def intLog  = readFile("${TEST_RESULTS_LOG}-integration")
                    if (unitLog.contains('FAILURE') || intLog.contains('FAILURE')) {
                        error('Test failure detected — aborting pipeline.')
                    }
                }
            }
        }

        stage('Deploy WAR') {
            steps {
                script {
                    def deployStart = sh(script: 'date +%s', returnStdout: true).trim().toInteger()
                    def commitTime  = sh(script: 'git log -1 --format=%ct', returnStdout: true).trim().toInteger()
                    leadTimeForChanges = deployStart - commitTime

                    sh '''
                        scp ${SSH_OPTS} -i ${SSH_KEY} ${WAR_NAME} ${SSH_USER}@${SSH_HOST}:${DEPLOY_DIR}/
                        ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} <<EOF
                            pkill -f 'org.apache.catalina.startup.Bootstrap' || true
                            sleep 5
                            ${TOMCAT_HOME}/bin/catalina.sh start|| true
                            ${TOMCAT_HOME}/bin/catalina.sh stop
EOF
                    '''

                    def deployEnd      = sh(script: 'date +%s', returnStdout: true).trim().toInteger()
                    def deployDuration = deployEnd - deployStart
                    writeFile file: DEPLOYMENT_TIME_FILE, text: "${deployDuration}"
                }
            }
        }
    }

    post {
        failure {
            script {
                def rollbackStart = sh(script: 'date +%s', returnStdout: true).trim().toInteger()
                sh '''
                    ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} <<EOF
                        cp ${BACKUP_DIR}/${WAR_NAME}_bak ${DEPLOY_DIR}/${WAR_NAME}
                        ${TOMCAT_HOME}/bin/catalina.sh start|| true
                        ${TOMCAT_HOME}/bin/catalina.sh stop
EOF
                '''
                def rollbackEnd = sh(script: 'date +%s', returnStdout: true).trim().toInteger()
                rollbackTime = rollbackEnd - rollbackStart
                writeFile file: ROLLBACK_LOG, text: "${rollbackTime}"
            }
        }
        always {
            script {
                def pipelineEnd  = sh(script: 'date +%s', returnStdout: true).trim().toInteger()
                def totalTime    = pipelineEnd - pipelineStartTime
                def deployTime   = fileExists(DEPLOYMENT_TIME_FILE) ? readFile(DEPLOYMENT_TIME_FILE).trim() : 'N/A'
                def rollbackVal  = fileExists(ROLLBACK_LOG) ? readFile(ROLLBACK_LOG).trim() : 'N/A'
                def unitLog      = fileExists("${TEST_RESULTS_LOG}-unit") ? readFile("${TEST_RESULTS_LOG}-unit") : ''
                def intLog       = fileExists("${TEST_RESULTS_LOG}-integration") ? readFile("${TEST_RESULTS_LOG}-integration") : ''

                echo "\n=== CI/CD METRICS ==="
                echo "Total Pipeline Time   : ${totalTime} sec"
                echo "Deployment Time       : ${deployTime} sec"
                echo "Lead Time for Changes : ${leadTimeForChanges} sec"
                echo "Rollback Time         : ${rollbackVal} sec"
                echo "Unit Test Results     : ${unitLog.readLines().findAll{it.contains('Tests')}.join(' | ')}"
                echo "Integration Test Results: ${intLog.readLines().findAll{it.contains('Tests')}.join(' | ')}"
                echo "======================"
            }
        }
    }
}
