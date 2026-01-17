#!/usr/bin/env groovy

/**
 * Update Kubernetes manifests with new image tags
 */
def call(Map config = [:]) {
    def imageTag = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-credentials'
    def gitUserName = config.gitUserName ?: 'Jenkins CI'
    def gitUserEmail = config.gitUserEmail ?: 'jenkins@example.com'
    def gitBranch = config.gitBranch ?: 'master'   // Default branch if not provided
    
    echo "Updating Kubernetes manifests with image tag: ${imageTag}"
    
    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {
        // Configure Git
        sh """
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"
        """
        
        // Update deployment manifests with new image tags
        sh """
            sed -i "s|image: .*easyshop-app:.*|image: vignesh997/easyshop-app:${imageTag}|g" ${manifestsPath}/08-easyshop-deployment.yaml
            
            if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                sed -i "s|image: .*easyshop-migration:.*|image: vignesh997/easyshop-migration:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
            fi
            
            if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
                sed -i "s|host: .*|host: easyshop.letsdeployit.com|g" ${manifestsPath}/10-ingress.yaml
            fi
            
            if git diff --quiet; then
                echo "No changes to commit"
            else
                git add ${manifestsPath}/*.yaml
                git commit -m "Update image tags to ${imageTag} and ensure correct domain [ci skip]"

                # Fix detached HEAD by creating/using the branch locally
                git checkout -B ${gitBranch}

                # Set up credentials for push
                git remote set-url origin https://\${GIT_USERNAME}:\${GIT_PASSWORD}@github.com/Vignesh8312/shoppyapp_hackathon.git
                git push origin ${gitBranch}
            fi
        """
    }
}
