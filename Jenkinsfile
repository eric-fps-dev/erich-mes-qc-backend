// =============================================================================
// Jenkinsfile — mes-qc-service (Java Spring Boot / Maven)
// =============================================================================
// Declarative pipeline triggered by GitHub Actions after CI passes.
// Builds Docker image via multi-stage Dockerfile, pushes to DockerHub,
// and sends notifications (Email + Teams).
//
// Deployment is handled manually (or via Watchtower for dev).
//
// Jenkins Prerequisites:
//   Credentials:
//     - 'FPS_DEV_Jenkins'              : Git SSH/HTTPS credentials
//     - 'fps-dockerhub-username-creds' : DockerHub username/password
//     - 'teams-webhook-url'            : MS Teams incoming webhook (Secret Text)
//   Global Env Vars:
//     - NOTIFICATION_EMAIL   (email recipients for build notifications)
//   Plugins:
//     - Email Extension, Credentials Binding
// =============================================================================

pipeline {
    agent any

    parameters {
        choice(
            name: 'ENVIRONMENT',
            choices: ['dev', 'staging', 'demo', 'prod'],
            description: 'Select the environment to build (Determines Spring Profile)'
        )
        string(
            name: 'VERSION_TAG',
            defaultValue: 'dev-latest',
            description: 'Exact Git Tag or Short SHA to use as Docker Image Tag'
        )
        string(
            name: 'GIT_COMMIT',
            defaultValue: '',
            description: 'Specific git commit to build'
        )
    }

    environment {
        FPS_DOCKER_HUB_REGISTRY = "fcd18388/mes-qc-service"

        DOCKER_HUB_IMAGE = "${FPS_DOCKER_HUB_REGISTRY}:${params.VERSION_TAG}"

        LATEST_TAG = "${params.ENVIRONMENT == 'prod' ? 'latest' : params.ENVIRONMENT + '-latest'}"
        DOCKER_HUB_LATEST = "${FPS_DOCKER_HUB_REGISTRY}:${LATEST_TAG}"

        DOCKER_BUILDKIT = '1'

        NOTIFICATION_EMAIL_ADDR = "${env.NOTIFICATION_EMAIL ?: 'eric.huang@fpscorp.ca'}"
    }

    stages {
        // =====================================================================
        // Stage 1: Checkout
        // =====================================================================
        stage('Checkout Code') {
            steps {
                script {
                    def checkoutTarget = params.GIT_COMMIT ? "${params.GIT_COMMIT}" :
                                        (params.VERSION_TAG.startsWith('v') ? "refs/tags/${params.VERSION_TAG}" : "*/${params.ENVIRONMENT}")
                    echo "Checking out Git target: ${checkoutTarget}"
                    checkout([
                        $class: 'GitSCM',
                        branches: [[name: checkoutTarget]],
                        extensions: [[$class: 'CleanBeforeCheckout']],
                        userRemoteConfigs: [[
                            credentialsId: 'FPS_DEV_Jenkins',
                            url: 'https://github.com/FPS-Food-Process-Solutions-Corp/mes-qc-backend'
                        ]]
                    ])
                }
            }
        }

        // =====================================================================
        // Stage 2: Docker Build (Multi-stage)
        // =====================================================================
        stage('Build Docker Image') {
            steps {
                sh """
                    docker buildx build \
                    -t ${DOCKER_HUB_IMAGE} \
                    -t ${DOCKER_HUB_LATEST} \
                    --build-arg SPRING_PROFILES_ACTIVE=${params.ENVIRONMENT} \
                    --pull \
                    --load .
                """
            }
        }

        stage('Push Image') {
           steps {
                script {
                    withCredentials([usernamePassword(
                        credentialsId: 'fps-dockerhub-username-creds',
                         usernameVariable: 'DOCKER_USER',
                          passwordVariable: 'DOCKER_PWD'
                    )]) {
                        sh "echo \$DOCKER_PWD | docker login -u \$DOCKER_USER --password-stdin"
                        sh "docker push ${DOCKER_HUB_IMAGE}"
                        sh "docker push ${DOCKER_HUB_LATEST}"
                        sh "docker logout"
                    }
                }
           }
        }
    }

    post {
        success {
            script {
                echo "****************************************************************"
                echo "✅ SUCCESS: Build and Push Completed"
                echo "****************************************************************"
                echo "📌 Project:     mes-qc-service"
                echo "🏷️ Version:     ${params.VERSION_TAG}"
                echo "🌍 Environment: ${params.ENVIRONMENT}"
                echo "📦 Image Tag:   ${DOCKER_HUB_IMAGE}"
                echo "📦 Latest Tag:  ${DOCKER_HUB_LATEST}"
                echo "****************************************************************"
            }

            emailext(
                subject: "✅ SUCCESS: mes-qc-service ${params.VERSION_TAG} → ${params.ENVIRONMENT}",
                body: """<p>Docker image <b>mes-qc-service</b> built and pushed successfully.</p>
                         <ul>
                           <li><b>Environment:</b> ${params.ENVIRONMENT}</li>
                           <li><b>Version:</b> ${params.VERSION_TAG}</li>
                           <li><b>Image:</b> ${DOCKER_HUB_IMAGE}</li>
                           <li><b>Commit:</b> ${params.GIT_COMMIT}</li>
                           <li><b>Build:</b> <a href="${BUILD_URL}">${JOB_NAME} #${BUILD_NUMBER}</a></li>
                         </ul>""",
                mimeType: 'text/html',
                to: "${NOTIFICATION_EMAIL_ADDR}"
            )

            withCredentials([string(credentialsId: 'teams-webhook-url', variable: 'TEAMS_WEBHOOK')]) {
                sh """
                    curl -sf -X POST -H 'Content-Type: application/json' \\
                        -d '{
                            "@type": "MessageCard",
                            "themeColor": "00FF00",
                            "summary": "Build Success",
                            "sections": [{
                                "activityTitle": "✅ mes-qc-service ${params.VERSION_TAG} (${params.ENVIRONMENT}) pushed to DockerHub",
                                "facts": [
                                    { "name": "Environment", "value": "${params.ENVIRONMENT}" },
                                    { "name": "Version",     "value": "${params.VERSION_TAG}" },
                                    { "name": "Image",       "value": "${env.DOCKER_HUB_IMAGE}" }
                                ],
                                "markdown": true
                            }],
                            "potentialAction": [{
                                "@type": "OpenUri",
                                "name": "View Build",
                                "targets": [{ "os": "default", "uri": "${env.BUILD_URL}" }]
                            }]
                        }' \\
                        "\$TEAMS_WEBHOOK" || echo "Teams notification failed (non-fatal)"
                """
            }
        }

        failure {
            emailext(
                subject: "❌ FAILURE: mes-qc-service ${params.VERSION_TAG} → ${params.ENVIRONMENT}",
                body: """<p>Docker build/push for <b>mes-qc-service</b> <span style="color:red">failed</span>.</p>
                         <ul>
                           <li><b>Environment:</b> ${params.ENVIRONMENT}</li>
                           <li><b>Version:</b> ${params.VERSION_TAG}</li>
                           <li><b>Commit:</b> ${params.GIT_COMMIT}</li>
                           <li><b>Build:</b> <a href="${BUILD_URL}">${JOB_NAME} #${BUILD_NUMBER}</a></li>
                         </ul>
                         <p>Please check the <a href="${BUILD_URL}console">console output</a> for details.</p>""",
                mimeType: 'text/html',
                to: "${NOTIFICATION_EMAIL_ADDR}"
            )

            withCredentials([string(credentialsId: 'teams-webhook-url', variable: 'TEAMS_WEBHOOK')]) {
                sh """
                    curl -sf -X POST -H 'Content-Type: application/json' \\
                        -d '{
                            "@type": "MessageCard",
                            "themeColor": "FF0000",
                            "summary": "Build Failed",
                            "sections": [{
                                "activityTitle": "❌ mes-qc-service ${params.VERSION_TAG} (${params.ENVIRONMENT}) build FAILED",
                                "facts": [
                                    { "name": "Environment", "value": "${params.ENVIRONMENT}" },
                                    { "name": "Version",     "value": "${params.VERSION_TAG}" },
                                    { "name": "Commit",      "value": "${params.GIT_COMMIT}" }
                                ],
                                "markdown": true
                            }],
                            "potentialAction": [{
                                "@type": "OpenUri",
                                "name": "View Build Log",
                                "targets": [{ "os": "default", "uri": "${env.BUILD_URL}console" }]
                            }]
                        }' \\
                        "\$TEAMS_WEBHOOK" || echo "Teams notification failed (non-fatal)"
                """
            }
        }

        always {
            cleanWs()
            script {
                sh 'docker image prune -f || true'
            }
        }
    }
}
