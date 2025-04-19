pipeline {
    agent any

    environment {
    SSH_USER       = 'root'
    SSH_HOST       = '40.192.66.15'
    SSH_KEY        = '/var/lib/jenkins/.ssh/id_rsa'
    REMOTE_BACKUP_DIR = '/tmp/jenkins_bak'
    WAR_NAME       = 'JobManagement_JENKINS.war'
    WAR_STORAGE    = '.'
    SSH_OPTS       = '-o StrictHostKeyChecking=no'
    TEST_CLASSES_CACHE = 'test_cache'
}


    stages {
        stage('Checkout Source') {
            steps {
                checkout scm
            }
        }

        stage('Measure Baseline Build + Test') {
            steps {
                script {
                    def startBuild = System.currentTimeMillis()
                    echo "Measuring baseline build + test..."

                    // Full compile & test for baseline
                    sh 'find src/main/java/model -name "*.java" | xargs javac -cp "src/main/webapp/WEB-INF/lib/*" -d build/WEB-INF/classes'
                    
                    sh 'javac -cp "src/main/webapp/WEB-INF/lib/*:src/main/resources" -d build/WEB-INF/classes src/main/java/model/*.java'
                    sh 'cp src/main/resources/config.properties test_output/'
                    sh 'java -cp "test_output:src/main/webapp/WEB-INF/lib/*" org.junit.platform.console.ConsoleLauncher --scan-class-path test_output --details summary || true'

                    def endBuild = System.currentTimeMillis()
                    def baselineTime = (endBuild - startBuild) / 1000
                    echo "Baseline time: ${baselineTime} seconds"

                    env.BASELINE_TIME = baselineTime.toString()
                }
            }
        }

        stage('Decide Mode Dynamically') {
            steps {
                script {
                    // Calculating baseline build time
                    def baseBuildTime = env.BASELINE_TIME.toDouble()
                    def threshold = 5 // Threshold in seconds
                    if (baseBuildTime >= threshold) {
                        echo "Running Optimized Mode A"
                        env.MODE = 'A'
                    } else {
                        echo "Running Baseline Mode B"
                        env.MODE = 'B'
                    }
                }
            }
        }

        stage('Validate Mode') {
            steps {
                script {
                    if (env.MODE != 'A' && env.MODE != 'B') {
                        error "Invalid MODE: ${env.MODE}. It must be either 'A' or 'B'."
                    }
                    echo "Running in Mode: ${env.MODE}"
                }
            }
        }

        stage('Build') {
            steps {
                script {
                    def startBuild = System.currentTimeMillis()
                    echo "Build Start"

                    if (env.MODE == 'A') {
                        echo "Running Incremental Build"
                        // Only compile if test classes aren't cached
                        if (!fileExists("${TEST_CLASSES_CACHE}/test_classes.jar")) {
                            echo "No Cache Found. Compiling Test Classes"
                            sh 'javac -cp "src/main/webapp/WEB-INF/lib/*" -d build/WEB-INF/classes src/main/java/model/*.java'
                            sh "jar cf ${TEST_CLASSES_CACHE}/test_classes.jar build/"
                        } else {
                            echo "Using Cached Test Classes"
                        }
                    } else {
                        echo "[Mode B] Full compile"
                        sh 'find src/main/java/model -name "*.java" | xargs javac -cp "src/main/webapp/WEB-INF/lib/*" -d build/WEB-INF/classes'
                    }

                    // Copy resources and web app
                    sh 'cp -R src/main/resources/* build/WEB-INF/classes/'
                    sh 'cp -R src/main/webapp/* build/'

                    // Build WAR file
                    sh 'jar -cvf JobManagement_JENKINS.war -C build .'

                    def endBuild = System.currentTimeMillis()
                    def buildTime = (endBuild - startBuild) / 1000
                    echo "Build Time: ${buildTime} seconds"
                    env.BUILD_TIME = buildTime.toString()
                }
            }
        }

        stage('Deploy to Tomcat') {
            steps {
                script {
                    env.DEPLOY_START = System.currentTimeMillis().toString()

                    echo "Deploying WAR to Tomcat..."
                    sh '''
                    ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} "mkdir -p ${REMOTE_BACKUP_DIR}"
                    scp ${SSH_OPTS} -i ${SSH_KEY} ${WAR_STORAGE}/${WAR_NAME} ${SSH_USER}@${SSH_HOST}:${REMOTE_BACKUP_DIR}/${WAR_NAME}_bak
                    ssh ${SSH_OPTS} -i ${SSH_KEY} ${SSH_USER}@${SSH_HOST} "sudo rm -rf /opt/tomcat10/webapps/JobManagement_JENKINS || true; sudo /opt/tomcat10/bin/catalina.sh stop || true; sudo /opt/tomcat10/bin/catalina.sh start"
                    '''


                }
            }
        }

        stage('Test') {
            steps {
                script {
                    echo "Running Tests..."

                    // Prepare test directories and classes
                    sh 'mkdir -p test_unit test_int'
                    sh 'javac -cp "src/main/webapp/WEB-INF/lib/*:src" -d test_unit src/main/test/TestAppPart1.java'
                    sh 'javac -cp "src/main/webapp/WEB-INF/lib/*:src" -d test_int src/main/test/TestAppPart2.java'

                    // Run tests in parallel
                    sh 'java -cp "test_unit:src/main/webapp/WEB-INF/lib/*" org.junit.platform.console.ConsoleLauncher --select-class TestAppPart1 --details summary > test_unit.log 2>&1 &'
                    sh 'java -cp "test_int:src/main/webapp/WEB-INF/lib/*" org.junit.platform.console.ConsoleLauncher --select-class TestAppPart2 --details summary > test_int.log 2>&1 &'

                    // Wait for both test executions
                    sh 'wait'

                    echo "Tests Completed"
                }
            }
        }

        stage('Cache Save') {
            steps {
                script {
                    echo "Saving Cache..."

                    // Save test classes cache
                    sh "mkdir -p ${TEST_CLASSES_CACHE}"
                    sh "cp -R test_unit ${TEST_CLASSES_CACHE}/test_unit"
                    sh "cp -R test_int ${TEST_CLASSES_CACHE}/test_int"
                }
            }
        }

stage('Finalize Metrics') {
    steps {
        script {
            echo "Finalizing Metrics..."

            def now = System.currentTimeMillis()

            def deployStart = env.DEPLOY_START ? env.DEPLOY_START.toLong() : 0
            def commitTime = env.COMMIT_TIME ? env.COMMIT_TIME.toLong() : 0
            def pipelineStart = env.PIPELINE_START ? env.PIPELINE_START.toLong() : 0

            def jvmSetupStart = env.JVM_SETUP_START ? env.JVM_SETUP_START.toLong() : 0
            def jvmSetupEnd = env.JVM_SETUP_END ? env.JVM_SETUP_END.toLong() : 0
            def buildCacheRestoreStart = env.BUILD_CACHE_RESTORE_START ? env.BUILD_CACHE_RESTORE_START.toLong() : 0
            def buildCacheRestoreEnd = env.BUILD_CACHE_RESTORE_END ? env.BUILD_CACHE_RESTORE_END.toLong() : 0
            def buildCacheSaveStart = env.BUILD_CACHE_SAVE_START ? env.BUILD_CACHE_SAVE_START.toLong() : 0
            def buildCacheSaveEnd = env.BUILD_CACHE_SAVE_END ? env.BUILD_CACHE_SAVE_END.toLong() : 0
            def testCacheRestoreStart = env.TEST_CACHE_RESTORE_START ? env.TEST_CACHE_RESTORE_START.toLong() : 0
            def testCacheRestoreEnd = env.TEST_CACHE_RESTORE_END ? env.TEST_CACHE_RESTORE_END.toLong() : 0
            def testCacheSaveStart = env.TEST_CACHE_SAVE_START ? env.TEST_CACHE_SAVE_START.toLong() : 0
            def testCacheSaveEnd = env.TEST_CACHE_SAVE_END ? env.TEST_CACHE_SAVE_END.toLong() : 0

def buildTime = env.BUILD_TIME ? env.BUILD_TIME.toDouble().toInteger() : 0
def testTime = env.TEST_TIME ? env.TEST_TIME.toDouble().toInteger() : 0
def jvmStartupTime = env.JVM_STARTUP_TIME ? env.JVM_STARTUP_TIME.toDouble().toInteger() : 0



            def deployTime = (now - deployStart) / 1000
            def leadTime = (now - commitTime) / 1000
            def totalTime = (now - pipelineStart) / 1000

            def jvmSetupTime = (jvmSetupEnd - jvmSetupStart) / 1000
            def buildCacheRestoreTime = (buildCacheRestoreEnd - buildCacheRestoreStart) / 1000
            def buildCacheSaveTime = (buildCacheSaveEnd - buildCacheSaveStart) / 1000
            def testCacheRestoreTime = (testCacheRestoreEnd - testCacheRestoreStart) / 1000
            def testCacheSaveTime = (testCacheSaveEnd - testCacheSaveStart) / 1000

            def netBuild = buildTime - buildCacheRestoreTime - buildCacheSaveTime
            def netTest = testTime - jvmStartupTime
            def netTotal = totalTime - buildCacheRestoreTime - buildCacheSaveTime - testCacheRestoreTime - testCacheSaveTime - jvmSetupTime - jvmStartupTime

            echo """
🚀 Mode: Mode ${env.MODE}
Build Time          : ${buildTime} sec
Test Time           : ${testTime} sec
Deploy Time         : ${deployTime} sec
Lead Time           : ${leadTime} sec
Total Pipeline Time : ${totalTime} sec
Overhead (JVM Setup)              : ${jvmSetupTime} sec
Overhead (Cache Restore - Build) : ${buildCacheRestoreTime} sec
Overhead (Cache Save - Build)    : ${buildCacheSaveTime} sec
Overhead (Cache Restore - Test)  : ${testCacheRestoreTime} sec
Overhead (Cache Save - Test)     : ${testCacheSaveTime} sec
Overhead (JVM Startup Time)      : ${jvmStartupTime} sec
➡️ Pure Build   : ${netBuild} sec
➡️ Pure Test    : ${netTest} sec
➡️ Pure Total   : ${netTotal} sec
"""

            writeFile file: 'stage_metrics.csv', text: "MODE,BUILD_TIME,TEST_TIME,DEPLOY_TIME,LEAD_TIME,TOTAL_TIME,JVM_SETUP,BUILD_CACHE_RESTORE,BUILD_CACHE_SAVE,TEST_CACHE_RESTORE,TEST_CACHE_SAVE,JVM_STARTUP,NET_BUILD_TIME,NET_TEST_TIME,NET_TOTAL_TIME\n"
            appendToFile file: 'stage_metrics.csv', text: "${env.MODE},${buildTime},${testTime},${deployTime},${leadTime},${totalTime},${jvmSetupTime},${buildCacheRestoreTime},${buildCacheSaveTime},${testCacheRestoreTime},${testCacheSaveTime},${jvmStartupTime},${netBuild},${netTest},${netTotal}\n"
        }
    }
}

    }
}
