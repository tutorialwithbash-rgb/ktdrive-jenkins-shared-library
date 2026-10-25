package com.example

class Docker implements Serializable {
    def script

    Docker(script) {
        this.script = script
    }

    def buildDockerImage(String imageName) {
        script.echo "Building Docker image: ${imageName}"
        script.sh "docker build -t ${imageName} ."
    }

    def runTest(String imageName) {
        script.sh "docker run --rm ${imageName}"
    }
}
