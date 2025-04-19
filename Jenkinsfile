pipeline {
    agent any

    environment {
        REMOTE_USER = 'root'
        REMOTE_HOST = '40.192.66.15'
        REMOTE_BACKUP_DIR = '/tmp/actions_bak'
        MODE = 'A'  // Default mode
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
                    sh 'cp src/main/resources/config.properties build/WEB-INF/classes/'
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
                    sh 'jar -cvf JobManagement_ACTIONS.war -C build .'

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
                    echo "Deploying WAR to Tomcat..."
                    sh """
                    ssh -o StrictHostKeyChecking=no -i key.pem ${env.REMOTE_USER}@${env.REMOTE_HOST} "mkdir -p ${env.REMOTE_BACKUP_DIR}"
                    scp -o StrictHostKeyChecking=no -i key.pem JobManagement_ACTIONS.war ${env.REMOTE_USER}@${env.REMOTE_HOST}:${env.REMOTE_BACKUP_DIR}/JobManagement_ACTIONS.war_bak
                    ssh -o StrictHostKeyChecking=no -i key.pem ${env.REMOTE_USER}@${env.REMOTE_HOST} <<EOF
                        sudo rm -rf /opt/tomcat10/webapps/JobManagement_ACTIONS || true
                        sudo /opt/tomcat10/bin/shutdown.sh || true
                        sudo /opt/tomcat10/bin/startup.sh
                    EOF
                    """
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

                    def deployStart = currentBuild.startTime
                    def deployEnd = System.currentTimeMillis()

                    def deployTime = (deployEnd - deployStart) / 1000
                    echo "Deployment Time: ${deployTime} seconds"

                    def totalTime = (deployEnd - currentBuild.timestamp) / 1000
                    echo "Total Pipeline Time: ${totalTime} seconds"
                    
                    // Output final metrics
                    echo """
                    🚀 Mode: ${env.MODE}
                    Build Time: ${env.BUILD_TIME} seconds
                    Deployment Time: ${deployTime} seconds
                    Total Time: ${totalTime} seconds
                    """
                }
            }
        }
    }
}
