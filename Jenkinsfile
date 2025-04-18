/*
 * Pattern-Driven CI/CD Jenkinsfile (Java-Servlet Job Portal)
 * Includes optimization patterns, per-stage benchmarking, and delta tracking.
 */

def pipelineStartTime = 0L

def leadTimeForChanges = 0L

def rollbackTime = 'N/A'

def buildFailed = false

def compilationError = false

def capturedException = null

def buildTime = 0L

def testTime = 0L

def deployTime = 0L

def stageStartTime = 0L

def stageEndTime = 0L

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
        STAGE_METRICS_FILE    = "${WORKSPACE}/stage_metrics.csv"
    }

    options {
        timestamps()
        buildDiscarder(logRotator(numToKeepStr: '10'))
    }

    stages {
        stage('Initialize') {
            steps {
                script {
                    sh "rm -f ${ROLLBACK_LOG}"
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
                    stageStartTime = sh(script: 'date +%s', returnStdout: true).trim().toInteger()
                    try {
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
                    } catch (Exception e) {
                        buildFailed = true
                        compilationError = true
                        capturedException = e
                        currentBuild.result = 'FAILURE'
                    } finally {
                        stageEndTime = sh(script: 'date +%s', returnStdout: true).trim().toInteger()
                        buildTime = stageEndTime - stageStartTime
                    }
                }
            }
        }

        stage('Handle Compilation Error') {
            when {
                expression { buildFailed && compilationError }
            }
            steps {
                script {
                    def backupExists = sh(
                        script: "ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} 'test -f ${BACKUP_DIR}/${WAR_NAME}_bak && echo exists || echo missing'",
                        returnStdout: true
                    ).trim()

                    if (backupExists == 'exists') {
                        echo 'Compilation failed — performing rollback.'
                        def rollbackStart = sh(script: 'date +%s', returnStdout: true).trim().toInteger()
                        sh """
                            ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} <<EOF
                                cp ${BACKUP_DIR}/${WAR_NAME}_bak ${DEPLOY_DIR}/${WAR_NAME}
                                ${TOMCAT_HOME}/bin/catalina.sh stop || true
                                ${TOMCAT_HOME}/bin/catalina.sh start
EOF
                        """
                        def rollbackEnd = sh(script: 'date +%s', returnStdout: true).trim().toInteger()
                        rollbackTime = (rollbackEnd - rollbackStart).toString()
                    } else {
                        echo 'Compilation failed — but no backup file found, rollback not possible.'
                    }
                }
            }
        }

        stage('Backup WAR') {
            when {
                expression { !buildFailed }
            }
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
            when {
                expression { !buildFailed }
            }
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
            when {
                expression { !buildFailed }
            }
            steps {
                script {
                    stageStartTime = sh(script: 'date +%s', returnStdout: true).trim().toInteger()
                    def unitLog = readFile("${TEST_RESULTS_LOG}-unit").toLowerCase()
                    def intLog  = readFile("${TEST_RESULTS_LOG}-integration").toLowerCase()
                    if ((unitLog =~ /\[\s*[1-9]\d*\s*tests failed\s*\]/) || (intLog =~ /\[\s*[1-9]\d*\s*tests failed\s*\]/)) {
                        error('Test failure detected — aborting pipeline.')
                    }
                    stageEndTime = sh(script: 'date +%s', returnStdout: true).trim().toInteger()
                    testTime = stageEndTime - stageStartTime
                }
            }
        }

        stage('Deploy WAR') {
            when {
                expression { !buildFailed }
            }
            steps {
                script {
                    stageStartTime = sh(script: 'date +%s', returnStdout: true).trim().toInteger()
                    def deployStart = stageStartTime
                    def commitTime  = sh(script: 'git log -1 --format=%ct', returnStdout: true).trim().toInteger()
                    leadTimeForChanges = deployStart - commitTime

                    sh '''
                        scp ${SSH_OPTS} -i ${SSH_KEY} ${WAR_NAME} ${SSH_USER}@${SSH_HOST}:${DEPLOY_DIR}/
                        ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} <<EOF
                            ${TOMCAT_HOME}/bin/catalina.sh stop || true
                            ${TOMCAT_HOME}/bin/catalina.sh start
EOF
                    '''

                    stageEndTime = sh(script: 'date +%s', returnStdout: true).trim().toInteger()
                    deployTime = stageEndTime - stageStartTime
                    writeFile file: DEPLOYMENT_TIME_FILE, text: "${deployTime}"
                }
            }
        }
    }

    post {
        always {
            script {
                def pipelineEnd  = sh(script: 'date +%s', returnStdout: true).trim().toInteger()
                def totalTime    = pipelineEnd - pipelineStartTime
                def deployTimeLog = fileExists(DEPLOYMENT_TIME_FILE) ? readFile(DEPLOYMENT_TIME_FILE).trim() : 'N/A'

                echo "\n=== CI/CD METRICS ==="
                echo "Build Stage Duration   : ${buildTime} sec"
                echo "Test Stage Duration    : ${testTime} sec"
                echo "Deploy Stage Duration  : ${deployTime} sec"
                echo "Total Pipeline Time    : ${totalTime} sec"
                echo "Deployment Time        : ${deployTimeLog} sec"
                echo "Lead Time for Changes  : ${leadTimeForChanges} sec"
                if (rollbackTime != 'N/A') {
                    echo "Rollback Time          : ${rollbackTime} sec"
                }

                def unitLog = fileExists("${TEST_RESULTS_LOG}-unit") ? readFile("${TEST_RESULTS_LOG}-unit") : ''
                def intLog  = fileExists("${TEST_RESULTS_LOG}-integration") ? readFile("${TEST_RESULTS_LOG}-integration") : ''
                echo "Unit Test Results:\n${unitLog}"
                echo "Integration Test Results:\n${intLog}"
                echo "======================"

                writeFile file: STAGE_METRICS_FILE, text: "Build,Test,Deploy,Total\n${buildTime},${testTime},${deployTime},${totalTime}"
                archiveArtifacts artifacts: 'stage_metrics.csv'
            }
        }
    }
}
