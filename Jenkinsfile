// Jenkinsfile — Declarative CD Pipeline
// Triggered by GitHub Actions after images are pushed to GHCR.
// Deploys to staging → manual approval → production via Helm.

pipeline {
    agent any

    parameters {
        string(name: 'IMAGE_TAG',      defaultValue: 'latest', description: 'Docker image tag (git SHA)')
        string(name: 'COMMIT_SHA',     defaultValue: '',       description: 'Full git commit SHA')
        string(name: 'TRIGGERED_BY',   defaultValue: 'manual', description: 'Trigger source')
    }

    environment {
        IMAGE_REGISTRY  = "ghcr.io/${env.GITHUB_OWNER ?: 'your-org'}/exam-platform"
        HELM_CHART      = './charts/exam-platform'
        STAGING_NS      = 'exam-platform-staging'
        PROD_NS         = 'exam-platform'
        SERVICES        = 'auth-service enrollment-service payment-service exam-service grading-service certificate-service notification-service api-gateway'
    }

    stages {

        // ── STAGE 6: PULL IMAGES FROM GHCR ────────────────────
        stage('Pull Images from GHCR') {
            steps {
                withCredentials([string(credentialsId: 'ghcr-token', variable: 'GHCR_TOKEN')]) {
                    sh """
                        echo \$GHCR_TOKEN | docker login ghcr.io -u \$GITHUB_OWNER --password-stdin
                        for svc in ${SERVICES}; do
                            docker pull ${IMAGE_REGISTRY}/\$svc:${params.IMAGE_TAG}
                            echo "Pulled \$svc:${params.IMAGE_TAG}"
                        done
                    """
                }
            }
        }

        // ── STAGE 7: DEPLOY TO STAGING ─────────────────────────
        stage('Deploy to Staging') {
            steps {
                sh """
                    helm upgrade --install exam-platform-staging ${HELM_CHART} \\
                        --namespace ${STAGING_NS} --create-namespace \\
                        --set global.image.tag=${params.IMAGE_TAG} \\
                        --set global.image.registry=${IMAGE_REGISTRY} \\
                        -f ${HELM_CHART}/values-dev.yaml \\
                        --wait --timeout 5m
                    echo 'Staging deployment complete'
                """
            }
        }

        // ── STAGE 8: SMOKE TESTS ────────────────────────────────
        stage('Smoke Tests') {
            steps {
                sh """
                    echo 'Running smoke tests against staging...'
                    sleep 10
                    curl -f http://localhost:8081/actuator/health || exit 1
                    curl -f http://localhost:8082/actuator/health || exit 1
                    curl -f http://localhost:8084/actuator/health || exit 1
                    echo 'All smoke tests passed'
                """
            }
        }

        // ── STAGE 9: MANUAL APPROVAL GATE ──────────────────────
        stage('Approve Production Deploy') {
            steps {
                input(
                    message: "Deploy image tag '${params.IMAGE_TAG}' to PRODUCTION?",
                    ok: 'Yes, Deploy to Production',
                    submitter: 'admin,devops-team'
                )
            }
        }

        // ── STAGE 10: DEPLOY TO PRODUCTION ─────────────────────
        stage('Deploy to Production') {
            steps {
                sh """
                    helm upgrade --install exam-platform ${HELM_CHART} \\
                        --namespace ${PROD_NS} --create-namespace \\
                        --set global.image.tag=${params.IMAGE_TAG} \\
                        --set global.image.registry=${IMAGE_REGISTRY} \\
                        --set deployment.strategy=RollingUpdate \\
                        -f ${HELM_CHART}/values-prod.yaml \\
                        --wait --timeout 10m
                    echo 'Production deployment complete'
                """
            }
        }

        // ── STAGE 11: POST-DEPLOY HEALTH CHECK ─────────────────
        stage('Post-Deploy Health Check') {
            steps {
                sh """
                    echo 'Running post-deploy health checks...'
                    sleep 15
                    curl -f http://localhost:8080/actuator/health || exit 1
                    echo 'Production health check passed'
                """
            }
        }
    }

    post {
        success {
            echo "Deployment of ${params.IMAGE_TAG} to production SUCCEEDED"
            emailext(
                subject: "✅ [exam-platform] Production deploy SUCCESS — ${params.IMAGE_TAG}",
                body: "Deployment of image tag ${params.IMAGE_TAG} (commit: ${params.COMMIT_SHA}) completed successfully.",
                to: 'team@examplatform.com'
            )
        }
        failure {
            echo "Deployment FAILED — initiating rollback..."
            sh "helm rollback exam-platform --namespace ${PROD_NS} || true"
            emailext(
                subject: "❌ [exam-platform] Production deploy FAILED — ${params.IMAGE_TAG}",
                body: "Deployment of image tag ${params.IMAGE_TAG} FAILED. Helm rollback initiated. Check Jenkins for details.",
                to: 'team@examplatform.com'
            )
        }
        always {
            cleanWs()
        }
    }
}
