def call() {
    sh "docker build --tag ktdrive-shared:${env.BUILD_NUMBER} ."
}