def call() {
    sh "docker run --rm ktdrive-shared:${env.BUILD_NUMBER}"
}
