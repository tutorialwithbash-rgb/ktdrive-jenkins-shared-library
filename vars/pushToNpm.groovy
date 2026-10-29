
def call(LinkedHashMap config = [:]) {
    withCredentials([
            string(
                    credentialsId: config.get('credentialsId') ?: 'github-npm-token',
                    variable: 'GITHUB_TOKEN'
            )
    ]) {
        sh '''
            set +x
            set -eu
            trap 'rm -f .npmrc' EXIT

            printf '//npm.pkg.github.com/:_authToken=%s\\n' \
                "$GITHUB_TOKEN" > .npmrc

            npm publish
        '''
    }
}