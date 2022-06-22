pipeline {
    agent any

    triggers {
    }

    options {
        skipDefaultCheckout()
    }

    environment {
        ENABLE_PUSH = true
        ENABLE_DEPLOY = true

        DEPLOY_JOB = 'deploy'
    }

    stages {
        stage('checkout') {
            steps {
                script {
                    checkout scm

                    git_commit = get_git_commit()
                    git_description = get_git_description()

                    VERSION = get_build_version(git_commit)

                    update_build_name(git_commit)
                    update_build_description(git_description)
                }
            }
        }
        stage('test') {
            agent {
                docker {
                    image "${DOCKER_REGISTRY}/tools/php-composer:7.4"
                    reuseNode true
                }
            }
            steps {
                dir('app') {
                    sh """
                        composer install
                        ./bin/phpunit tests \
                            --log-junit '../reports/unitreport.xml'
                    """
                }
            }
            post {
                always {
                    junit 'reports/unitreport.xml'
                }
            }
        }
        stage('build') {
            when {
                expression { return VERSION }
            }
            steps {
                sh "make build -e VERSION=${VERSION}"
            }
        }
        stage('publish') {
            when {
                expression { return env.ENABLE_PUSH.toBoolean() && VERSION }
            }
            steps {
                script {
                    sh "make push -e VERSION=${VERSION}"
                }
            }
        }
        stage('deploy') {
            when {
                expression {
                    return env.ENABLE_DEPLOY.toBoolean() && (env.DEPLOY_JOB) && VERSION
                }
            }
            steps {
                build job: env.DEPLOY_JOB,
                    wait: false,
                    parameters: [
                        string(name: 'version', value: VERSION)
                    ]
            }
        }
    }
    post {
        success {
            bitbucketStatusNotify(buildState: 'SUCCESSFUL')
        }
        unsuccessful {
            bitbucketStatusNotify(buildState: 'FAILED')
        }
    }
}

// git support
String get_git_commit() {
    return sh(returnStdout: true, script: "git log -n 1 --pretty=format:'%h'").trim()
}

String get_git_description() {
    return sh(returnStdout: true, script: 'git log -1 --pretty=%B').trim()
}

// versions
String get_build_version(String commit) {
    return commit
}

// build info
void update_build_name(String value) {
    currentBuild.displayName = "#${BUILD_NUMBER} (${value})"
}

void update_build_description( String value) {
    currentBuild.description = "${value}"
}
