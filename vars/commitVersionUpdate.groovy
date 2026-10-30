
def call(LinkedHashMap config = [:]) {

    withCredentials([
            usernamePassword(
                    credentialsId: config.get('credentialsId') ?: 'github-crendentials',
                    usernameVariable: 'GIT_USERNAME',
                    passwordVariable: 'GIT_PASSWORD'
            )
    ]) {

        withEnv([
                "GIT_BRANCH=${config.get('branch') ?: 'main'}",
                "GIT_COMMIT_NAME=${config.get('gitUserName') ?: 'Jenkins'}",
                "GIT_COMMIT_EMAIL=${config.get('gitUserEmail') ?: 'jenkins@ktdrive.com'}"
        ]) {

            sh '''
                set +x
                set -eu

                git config user.name "$GIT_COMMIT_NAME"
                git config user.email "$GIT_COMMIT_EMAIL"

                askpass=$(mktemp)
                trap 'rm -f "$askpass"' EXIT

                printf '%s\\n' \
                    '#!/bin/sh' \
                    'case "$1" in' \
                    '    *Username*) printf "%s\\n" "$GIT_USERNAME" ;;' \
                    '    *Password*) printf "%s\\n" "$GIT_PASSWORD" ;;' \
                    '    *) exit 1 ;;' \
                    'esac' > "$askpass"

                chmod 700 "$askpass"

                git add package.json

                if git diff --cached --quiet; then
                    echo "No version changes to commit."
                    exit 0
                fi

                git commit -m "chore: update package version [skip ci]"

                export GIT_ASKPASS="$askpass"
                export GIT_TERMINAL_PROMPT=0

                git push origin "HEAD:refs/heads/$GIT_BRANCH"
            '''
        }
    }
}
