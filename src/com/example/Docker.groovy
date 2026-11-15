
package com.example

class Docker implements Serializable {
    def script

    Docker(script) {
        this.script = script
    }

    def buildDockerImage(
        String imageName,
        String dockerfilePath = "Dockerfile",
        String context = "."
    ) {
        script.echo "Building Docker image with GitHub Packages authentication..."

        script.withCredentials([
            script.string(
                credentialsId: 'GITHUB_TOKEN',
                variable: 'GITHUB_TOKEN'
            )
        ]) {
            script.sh """
                docker buildx build \
                    --secret id=github_token,env=GITHUB_TOKEN \
                    --load \
                    -f ${dockerfilePath} \
                    -t ${imageName} \
                    ${context}
            """
        }
    }

    def dockerLogin() {
        script.withCredentials([
            script.usernamePassword(
                credentialsId: 'docker-hub-repo',
                passwordVariable: 'PASS',
                usernameVariable: 'USER'
            )
        ]) {
            script.sh '''
                echo "$PASS" | docker login \
                    -u "$USER" \
                    --password-stdin
            '''
        }
    }

    def dockerPush(String imageName) {
        script.sh "docker push ${imageName}"
    }
}
