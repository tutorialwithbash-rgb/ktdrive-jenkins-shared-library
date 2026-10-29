
def call(LinkedHashMap config = [:]) {
    // ... rest of your code stays the same
    def credentialsId = config.get('credentialsId') ?: 'github-npm-token'

    withCredentials([
            string(
                    credentialsId: credentialsId,
                    variable: 'GITHUB_TOKEN'
            )
    ]) {
        sh '''
            set +x
            set -e
            trap 'rm -f .npmrc' EXIT

            printf '%s\\n' \
              '@tutorialwithbash-rgb:registry=https://npm.pkg.github.com/' \
              "//npm.pkg.github.com/:_authToken=${GITHUB_TOKEN}" \
              > .npmrc

            npm publish
        '''
    }
}
