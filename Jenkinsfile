/*
 * Pattern-Driven CI/CD Jenkinsfile (Java-Servlet Job Portal)
 * Includes optimization patterns, benchmarking, and A/B mode comparison.
 */

def pipelineStartTime = 0L
def buildTime = 0L
def testTime = 0L
def deployTime = 0L
def totalTime = 0L
def leadTimeForChanges = 0L
def rollbackTime = 'N/A'

def buildFailed = false
def compilationError = false

def mode = 'A' // 'A' = patterns enabled, 'B' = patterns disabled

def metrics = [:]

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
        CSV_FILE              = "${WORKSPACE}/stage_metrics.csv"
    }

    options {
        timestamps()
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

        stage('Build') {
            steps {
                script {
                    def start = System.currentTimeMillis()
                    try {
                        sh 'mkdir -p build/WEB-INF/classes'

                        if (mode == 'A') {
                            def changed = sh(script: 'find src -name "*.java" -newer build/WEB-INF/classes', returnStdout: true).trim()
                            if (changed) {
                                echo "Compiling changed files:\n${changed}"
                                sh "javac -cp 'src/main/webapp/WEB-INF/lib/*' -d build/WEB-INF/classes ${changed}"
                            } else {
                                echo 'No changes detected — performing full compile.'
                                sh 'find src -name "*.java" | xargs javac -cp "src/main/webapp/WEB-INF/lib/*" -d build/WEB-INF/classes'
                            }
                        } else {
                            echo '[Mode B] Full compile mode.'
                            sh 'find src -name "*.java" | xargs javac -cp "src/main/webapp/WEB-INF/lib/*" -d build/WEB-INF/classes'
                        }

                        sh '''
                            cp -R src/main/resources/* build/WEB-INF/classes/
                            cp -R src/main/webapp/* build/
                            jar -cvf ${WAR_NAME} -C build .
                        '''
                    } catch (Exception e) {
                        buildFailed = true
                        compilationError = true
                        error("Build failed: ${e.message}")
                    }
                    buildTime = (System.currentTimeMillis() - start) / 1000
                }
            }
        }

        stage('Backup WAR') {
            when {
                expression { !buildFailed }
            }
            steps {
                sh "mkdir -p ${BACKUP_DIR}"
                sh "cp ${WAR_NAME} ${BACKUP_DIR}/${WAR_NAME}_bak || true"
            }
        }

        stage('Run Tests') {
            when {
                expression { !buildFailed }
            }
            steps {
                script {
                    def start = System.currentTimeMillis()
                    if (mode == 'A') {
                        parallel(
                            'Unit Tests': {
                                sh '''
                                    mkdir -p test_output_unit
                                    javac -cp 'src/main/webapp/WEB-INF/lib/*:src' -d test_output_unit src/main/test/TestAppPart1.java
                                    java -cp 'test_output_unit:src/main/webapp/WEB-INF/lib/*' org.junit.platform.console.ConsoleLauncher --select-class TestAppPart1 --details summary > ${TEST_RESULTS_LOG}-unit 2>&1 || true
                                '''
                            },
                            'Integration Tests': {
                                sh '''
                                    mkdir -p test_output_integration
                                    javac -cp 'src/main/webapp/WEB-INF/lib/*:src' -d test_output_integration src/main/test/TestAppPart2.java
                                    java -cp 'test_output_integration:src/main/webapp/WEB-INF/lib/*' org.junit.platform.console.ConsoleLauncher --select-class TestAppPart2 --details summary > ${TEST_RESULTS_LOG}-integration 2>&1 || true
                                '''
                            }
                        )
                    } else {
                        echo '[Mode B] Sequential test execution.'
                        sh '''
                            mkdir -p test_output_all
                            find src/main/test -name "*.java" | xargs javac -cp 'src/main/webapp/WEB-INF/lib/*:src' -d test_output_all
                            java -cp 'test_output_all:src/main/webapp/WEB-INF/lib/*' org.junit.platform.console.ConsoleLauncher --scan-class-path test_output_all --details summary > ${TEST_RESULTS_LOG}-combined 2>&1 || true
                        '''
                    }
                    testTime = (System.currentTimeMillis() - start) / 1000
                }
            }
        }

        stage('Deploy') {
            when {
                expression { !buildFailed }
            }
            steps {
                script {
                    def deployStart = System.currentTimeMillis()
                    def commitTime = sh(script: 'git log -1 --format=%ct', returnStdout: true).trim().toInteger()
                    def now = sh(script: 'date +%s', returnStdout: true).trim().toInteger()
                    leadTimeForChanges = now - commitTime

                    sh '''
                        scp ${SSH_OPTS} -i ${SSH_KEY} ${WAR_NAME} ${SSH_USER}@${SSH_HOST}:${DEPLOY_DIR}/
                        ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} <<EOF
                            chmod 644 ${DEPLOY_DIR}/${WAR_NAME}
                            ${TOMCAT_HOME}/bin/catalina.sh stop || true
                            ${TOMCAT_HOME}/bin/catalina.sh start
EOF
                    '''
                    deployTime = (System.currentTimeMillis() - deployStart) / 1000
                }
            }
        }
    }

    post {
    always {
        script {
            totalTime = (System.currentTimeMillis() - pipelineStartTime) / 1000
            echo "\n=== PIPELINE METRICS (${mode}) ==="
            echo "Build Time           : ${buildTime} sec"
            echo "Test Time            : ${testTime} sec"
            echo "Deploy Time          : ${deployTime} sec"
            echo "Lead Time for Change : ${leadTimeForChanges} sec"
            echo "Total Pipeline Time  : ${totalTime} sec"
            echo "=============================="

            def header = 'Run Mode,Build Time,Test Time,Deploy Time,Lead Time,Total Time\n'
            def line = "${mode},${buildTime},${testTime},${deployTime},${leadTimeForChanges},${totalTime}\n"

            def csvExists = fileExists(CSV_FILE)
            if (!csvExists) {
                writeFile file: CSV_FILE, text: header + line
            } else {
                // Read the existing content, append new content, and write back to the file
                def currentContent = readFile(CSV_FILE)
                writeFile file: CSV_FILE, text: currentContent + line
            }
        }
    }
}

}
