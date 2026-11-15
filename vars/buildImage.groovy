
import com.example.Docker

def call(
    String imageName,
    String dockerfilePath = "Dockerfile",
    String context = "."
) {
    def docker = new Docker(this)

    docker.buildDockerImage(
        imageName,
        dockerfilePath,
        context
    )
}
