/*
 * Pattern-Driven CI/CD Jenkinsfile
 * Includes: Workspace Cache, Incremental Build, Parallel Tests,
 * Early Test Failure Detection, Selective Test Execution,
 * Benchmarking hooks for per-stage timing, deployment, rollback, and overall metrics.
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
        SSH_KEY               = '/var/lib/jenkins/.ssh/id_rsa'
        SSH_USER              = 'root'
        SSH_HOST              = '18.61.60.110'
        SSH_OPTS              = '-o StrictHostKeyChecking=no'
        BACKUP_DIR            = '/tmp/jenkins_bak'

        DEPLOYMENT_TIME_FILE  = "${WORKSPACE}/deployment_time.log"
        TEST_RESULTS_LOG_BASE = "${WORKSPACE}/test_results"
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
                    pipelineStartTime = System.currentTimeMillis() / 1000
                }
            }
        }

        stage('Checkout') {
            steps {
                script {
                    def start = System.currentTimeMillis() / 1000
                    git url: 'https://github.com/sai-pranay-teja/JobManagement.git', branch: 'main'
                    stash name: 'sourceCode'
                    def duration = (System.currentTimeMillis() / 1000) - start
                    echo "Stage Checkout duration: ${duration} sec"
                }
            }
        }

        stage('Restore Workspace Cache') {
            steps {
                script {
                    def start = System.currentTimeMillis() / 1000
                    try {
                        unstash 'm2cache'
                        echo 'Workspace cache restored.'
                    } catch (e) {
                        echo 'No existing cache.'
                    }
                    def duration = (System.currentTimeMillis() / 1000) - start
                    echo "Stage Restore Cache duration: ${duration} sec"
                }
            }
        }

        stage('Build WAR (Incremental)') {
  steps {
    script {
      // Ensure build directory exists
      sh 'mkdir -p build/WEB-INF/classes'

      // Find all .java files newer than the last build timestamp
      def changed = sh(
        script: 'find src -name "*.java" -newer build/WEB-INF/classes',
        returnStdout: true
      ).trim()

      if (changed) {
        echo "Compiling only changed files:\n${changed}"
        // Compile the changed files only
        sh "javac -cp 'src/main/webapp/WEB-INF/lib/*' -d build/WEB-INF/classes ${changed}"
      } else {
        echo "No changes detected—doing a full compile."
        // Full compile of all Java sources
        sh '''
          find src -name "*.java" | xargs javac -cp 'src/main/webapp/WEB-INF/lib/*' -d build/WEB-INF/classes
        '''
      }

      // Copy resources & package WAR
      sh '''
        cp -R src/main/resources/* build/WEB-INF/classes/
        cp -R src/main/webapp/* build/
        jar -cvf ${WAR_NAME} -C build .
      '''
    }
  }
}


        stage('Save Workspace Cache') {
            steps {
                script {
                    def start = System.currentTimeMillis() / 1000
                    stash name: 'm2cache', includes: '**/.m2/**'
                    def duration = (System.currentTimeMillis() / 1000) - start
                    echo "Stage Save Cache duration: ${duration} sec"
                }
            }
        }

        stage('Backup WAR') {
            steps {
                script {
                    def start = System.currentTimeMillis() / 1000
                    sh "mkdir -p ${BACKUP_DIR}"
                    if (fileExists("${WORKSPACE}/${WAR_NAME}")) {
                        sh "cp ${WORKSPACE}/${WAR_NAME} ${BACKUP_DIR}/${WAR_NAME}_bak"
                        echo "Backup saved: ${BACKUP_DIR}/${WAR_NAME}_bak"
                    } else {
                        echo "No WAR to backup."
                    }
                    def duration = (System.currentTimeMillis() / 1000) - start
                    echo "Stage Backup WAR duration: ${duration} sec"
                }
            }
        }

        stage('Run Parallel Tests') {
            parallel {
                stage('Unit Tests (Part1)') {
                    steps {
                        script {
                            def start = System.currentTimeMillis() / 1000
                            sh '''#!/bin/bash
                            mkdir -p test_output_unit
                            javac -cp "src/main/webapp/WEB-INF/lib/*:src" -d test_output_unit \
                                src/main/test/TestAppPart1.java || true
                            java -cp "test_output_unit:src/main/webapp/WEB-INF/lib/*" \
                                org.junit.platform.console.ConsoleLauncher \
                                --select-class TestAppPart1 --details summary \
                                > ${TEST_RESULTS_LOG_BASE}-unit.log 2>&1 || true
                            '''
                            def duration = (System.currentTimeMillis() / 1000) - start
                            echo "Stage Unit Tests duration: ${duration} sec"
                        }
                    }
                }
                stage('Integration Tests (Part2)') {
                    steps {
                        script {
                            def start = System.currentTimeMillis() / 1000
                            sh '''#!/bin/bash
                            mkdir -p test_output_integration
                            javac -cp "src/main/webapp/WEB-INF/lib/*:src" -d test_output_integration \
                                src/main/test/TestAppPart2.java || true
                            java -cp "test_output_integration:src/main/webapp/WEB-INF/lib/*" \
                                org.junit.platform.console.ConsoleLauncher \
                                --select-class TestAppPart2 --details summary \
                                > ${TEST_RESULTS_LOG_BASE}-integration.log 2>&1 || true
                            '''
                            def duration = (System.currentTimeMillis() / 1000) - start
                            echo "Stage Integration Tests duration: ${duration} sec"
                        }
                    }
                }
            }
        }

        stage('Early Test Failure Exit') {
            steps {
                script {
                    def summary = readFile("${TEST_RESULTS_LOG_BASE}-unit.log") + readFile("${TEST_RESULTS_LOG_BASE}-integration.log")
                    if (summary.contains('FAIL') || summary.contains('ERROR')) {
                        error('Early exit: test failure detected')
                    }
                }
            }
        }

        stage('Deploy WAR') {
            steps {
                script {
                    def start = System.currentTimeMillis() / 1000
                    def commitTime = sh(script: 'git log -1 --format=%ct', returnStdout: true).trim().toLong()
                    leadTimeForChanges = (start - commitTime)

                    sh '''#!/bin/bash
                    scp ${SSH_OPTS} -i ${SSH_KEY} ${WAR_NAME} ${SSH_USER}@${SSH_HOST}:${DEPLOY_DIR}/
                    ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} <<EOF
                      pkill -f 'org.apache.catalina.startup.Bootstrap' || true
                      sleep 5
                      ${TOMCAT_HOME}/bin/catalina.sh start|| true
                      ${TOMCAT_HOME}/bin/catalina.sh stop
EOF
                    '''

                    def duration = (System.currentTimeMillis() / 1000) - start
                    writeFile file: DEPLOYMENT_TIME_FILE, text: "${duration}"
                    echo "Stage Deploy duration: ${duration} sec"
                }
            }
        }
    }

    post {
        failure {
            script {
                def start = System.currentTimeMillis() / 1000
                sh '''#!/bin/bash
                ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} <<EOF
                  cp ${BACKUP_DIR}/${WAR_NAME}_bak ${DEPLOY_DIR}/${WAR_NAME}
                  ${TOMCAT_HOME}/bin/catalina.sh start|| true
                  ${TOMCAT_HOME}/bin/catalina.sh stop
EOF
                '''
                def duration = (System.currentTimeMillis() / 1000) - start
                writeFile file: ROLLBACK_LOG, text: "${duration}"
                echo "Rollback duration: ${duration} sec"
            }
        }

        always {
            script {
                def end = System.currentTimeMillis() / 1000
                    def total = end - pipelineStartTime
                def deployTime = fileExists(DEPLOYMENT_TIME_FILE) ? readFile(DEPLOYMENT_TIME_FILE).trim() : 'N/A'
                def rollback = fileExists(ROLLBACK_LOG) ? readFile(ROLLBACK_LOG).trim() : 'N/A'

                echo "\n=========== CI/CD METRICS ==========="
                echo "Total Pipeline Time   : ${total} sec"
                echo "Deployment Time       : ${deployTime} sec"
                echo "Lead Time for Changes : ${leadTimeForChanges} sec"
                echo "Rollback Time         : ${rollback} sec"
                echo "======================================"
            }
        }
    }
}
