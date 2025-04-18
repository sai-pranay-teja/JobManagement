/*
 * Jenkinsfile: A/B Test Mode for CI/CD Pipeline Optimization
 * - Mode A: Patterns Enabled (workspace cache, incremental build, parallel tests)
 * - Mode B: Patterns Disabled (full build, sequential tests)
 * - Captures per-stage metrics in CSV with delta comparison
 */

def pipelineStartTime = 0L

def leadTimeForChanges = 0L

def rollbackTime = 'N/A'

def buildFailed = false

def compilationError = false

def mode = 'A' // Switch to 'B' to disable patterns

def stageMetrics = [:]

def metricsCsv = "${env.WORKSPACE}/stage_metrics.csv"

def formatTime = { long start, long end -> end - start }

def logStageTime = { stageName, time -> stageMetrics[stageName] = time }

def appendToCsv = {
    def header = 'Run Mode,Build Time,Test Time,Deploy Time,Lead Time,Total Time\n'
    def values = "${mode},${stageMetrics['Build']},${stageMetrics['Test']},${stageMetrics['Deploy']},${leadTimeForChanges},${stageMetrics['Total']}\n"
    if (!fileExists(metricsCsv)) {
        writeFile file: metricsCsv, text: header + values
    } else {
        writeFile file: metricsCsv, text: values, append: true
    }
}

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
                    sh "rm -f ${ROLLBACK_LOG}"
                    pipelineStartTime = sh(script: 'date +%s', returnStdout: true).trim().toLong()
                }
            }
        }

        stage('Build WAR') {
            steps {
                script {
                    try {
                        def buildStart = System.currentTimeMillis()

                        sh 'mkdir -p build/WEB-INF/classes'

                        if (mode == 'A') {
                            def changed = sh(
                                script: 'find src -name "*.java" -newer build/WEB-INF/classes',
                                returnStdout: true
                            ).trim()
                            if (changed) {
                                sh "javac -cp 'src/main/webapp/WEB-INF/lib/*' -d build/WEB-INF/classes ${changed}"
                            } else {
                                sh "find src -name '*.java' | xargs javac -cp 'src/main/webapp/WEB-INF/lib/*' -d build/WEB-INF/classes"
                            }
                        } else {
                            sh "find src -name '*.java' | xargs javac -cp 'src/main/webapp/WEB-INF/lib/*' -d build/WEB-INF/classes"
                        }

                        sh '''
                            cp -R src/main/resources/* build/WEB-INF/classes/
                            cp -R src/main/webapp/* build/
                            jar -cvf ${WAR_NAME} -C build .
                        '''

                        def buildEnd = System.currentTimeMillis()
                        logStageTime('Build', formatTime(buildStart, buildEnd))

                    } catch (Exception e) {
                        buildFailed = true
                        compilationError = true
                        error("Build failed: ${e.message}")
                    }
                }
            }
        }

        stage('Test') {
            when {
                expression { !buildFailed }
            }
            steps {
                script {
                    def testStart = System.currentTimeMillis()

                    if (mode == 'A') {
                        parallel(
                            "Unit Tests": {
                                sh '''
                                    mkdir -p test_output_unit
                                    javac -cp 'src/main/webapp/WEB-INF/lib/*:src' -d test_output_unit src/main/test/TestAppPart1.java
                                    java -cp 'test_output_unit:src/main/webapp/WEB-INF/lib/*' org.junit.platform.console.ConsoleLauncher --select-class TestAppPart1 --details summary
                                '''
                            },
                            "Integration Tests": {
                                sh '''
                                    mkdir -p test_output_integration
                                    javac -cp 'src/main/webapp/WEB-INF/lib/*:src' -d test_output_integration src/main/test/TestAppPart2.java
                                    java -cp 'test_output_integration:src/main/webapp/WEB-INF/lib/*' org.junit.platform.console.ConsoleLauncher --select-class TestAppPart2 --details summary
                                '''
                            }
                        )
                    } else {
                        sh '''
                            mkdir -p test_output_all
                            javac -cp 'src/main/webapp/WEB-INF/lib/*:src' -d test_output_all src/main/test/*.java
                            java -cp 'test_output_all:src/main/webapp/WEB-INF/lib/*' org.junit.platform.console.ConsoleLauncher --scan-class-path test_output_all --details summary
                        '''
                    }

                    def testEnd = System.currentTimeMillis()
                    logStageTime('Test', formatTime(testStart, testEnd))
                }
            }
        }

        stage('Deploy WAR') {
            when {
                expression { !buildFailed }
            }
            steps {
                script {
                    def deployStart = System.currentTimeMillis()
                    def commitTime = sh(script: 'git log -1 --format=%ct', returnStdout: true).trim().toLong()
                    leadTimeForChanges = System.currentTimeMillis()/1000 - commitTime

                    sh '''
                        scp ${SSH_OPTS} -i ${SSH_KEY} ${WAR_NAME} ${SSH_USER}@${SSH_HOST}:${DEPLOY_DIR}/
                        ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} <<EOF
                            chmod 755 ${DEPLOY_DIR}/${WAR_NAME}
                            ${TOMCAT_HOME}/bin/catalina.sh stop || true
                            ${TOMCAT_HOME}/bin/catalina.sh start
EOF
                    '''

                    def deployEnd = System.currentTimeMillis()
                    logStageTime('Deploy', formatTime(deployStart, deployEnd))
                }
            }
        }
    }

    post {
        always {
            script {
                def pipelineEnd = System.currentTimeMillis()
                logStageTime('Total', formatTime(pipelineStartTime * 1000, pipelineEnd))

                appendToCsv()

                echo "\n=== PIPELINE METRICS (${mode}) ==="
                stageMetrics.each { k, v -> echo "${k} Time: ${v} ms" }
                echo "Lead Time for Changes: ${leadTimeForChanges} sec"
                echo "==========================="
            }
        }
    }
}
