def call(LinkedHashMap config = [:]) {
    withCredentials([
            usernamePassword(
                    credentialsId: config.get('credentialsId'),
                    usernameVariable: 'USER',
                    passwordVariable: 'PASS'
            )
    ]) {
        sh """
            set +x
            set -eu

            git config --global user.name "${config.get('gitUserName')}"
            git config --global user.email "${config.get('gitUserEmail')}"

            git remote set-url origin "https://\\${USER}:\\${PASS}@github.com/${config.get('repository')}"

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