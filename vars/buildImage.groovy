import com.example.Docker

def call(String imageName, String dockerfilePath = "Dockerfile", String context = ".") {
    return new Docker(this).buildDockerImage(imageName, dockerfilePath, context)
}