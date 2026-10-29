def call(LinkedHashMap config = [:]) {
    withCredentials([
            usernamePassword(
                    credentialsId: config.get('credentialsId') ?: 'github-credentials',
                    usernameVariable: 'USER',
                    passwordVariable: 'PASSW'
            )
    ]) {
        sh """
            set +x
            set -eu

            git config user.name "${config.get('gitUserName') ?: 'Jenkins'}"
            git config user.email "${config.get('gitUserEmail') ?: 'jenkins@ktdrive.com'}"

            git remote set-url origin "https://\\${USER}:\\${PASSW}@github.com/${config.get('repository')}"

            git add package.json

            if git diff --cached --quiet; then
                echo "No version changes to commit."
                exit 0
            fi

            git commit -m "chore: update package version [skip ci]"
            git push origin HEAD:"${config.get('branch') ?: 'jenkins-job'}"
        """
    }
}