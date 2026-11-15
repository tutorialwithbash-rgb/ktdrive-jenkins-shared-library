# KTDrive Jenkins Shared Library

This Jenkins Shared Library provides reusable Pipeline steps for building and
publishing Docker images, updating an npm package version, publishing a package
to GitHub Packages, and committing a version change.

## Configure the library

1. In Jenkins, add this repository under **Manage Jenkins → System → Global
   Pipeline Libraries**.
2. Set a library name (for example, `ktdrive`), the repository URL, and the
   default branch or version.
3. Add the Jenkins credentials required by the steps you plan to use. See
   [Credentials](#credentials) for the credential types and IDs.
4. Ensure the build agent has the tools required by those steps: Docker with
   Buildx for image builds, Git for version commits, and Node.js/npm and Bun
   for package operations.

Import the library at the top of a Jenkinsfile:

```groovy
@Library('ktdrive') _
```

## Example Pipeline

This example builds and pushes a Docker image, increments the package's patch
version, publishes the package, and commits the version update. Remove or
adjust stages that are not part of your workflow.

```groovy
@Library('ktdrive') _

pipeline {
    agent any

    environment {
        IMAGE_NAME = 'my-dockerhub-user/my-app:ci'
    }

    stages {
        stage('Build image') {
            steps {
                script {
                    buildImage(env.IMAGE_NAME)
                }
            }
        }

        stage('Push image') {
            steps {
                script {
                    dockerLogin()
                    dockerPush(env.IMAGE_NAME)
                }
            }
        }

        stage('Release package') {
            steps {
                script {
                    def nextVersion = incrementVersion('patch')
                    echo "Publishing version ${nextVersion}"
                    pushToNpm()
                    commitVersionUpdate()
                }
            }
        }
    }
}
```

`commitVersionUpdate` pushes to the configured branch, so use it only in a
workflow where the Jenkins checkout and credentials are allowed to push. The
package must also be configured to publish to GitHub Packages; see the
`pushToNpm` step below.

## Pipeline steps

### `buildImage(String imageName, String dockerfilePath = 'Dockerfile', String context = '.')`

Builds and loads a Docker image locally using Docker Buildx. The command passes
the Jenkins secret-text credential `GITHUB_TOKEN` to the build as a BuildKit
secret named `github_token`:

```sh
docker buildx build \
  --secret id=github_token,env=GITHUB_TOKEN \
  --load \
  -f <dockerfilePath> \
  -t <imageName> \
  <context>
```

The Dockerfile can consume this secret for authenticated build steps. The
credential ID is fixed as `GITHUB_TOKEN` in the implementation.

```groovy
buildImage('my-dockerhub-user/my-app:ci')
buildImage('my-dockerhub-user/my-app:ci', 'docker/Dockerfile', '.')
```

The Jenkins agent needs Docker with Buildx support and access to a Docker
daemon.

### `dockerLogin()`

Logs in to Docker using Jenkins `usernamePassword` credentials with the fixed
ID `docker-hub-repo`. It reads the username and password from Jenkins and sends
the password to `docker login --password-stdin`.

```groovy
dockerLogin()
```

This step does not accept a credential ID argument. Configure the credential in
Jenkins with the expected ID, or update the library implementation to use a
different ID.

### `dockerPush(String imageName)`

Pushes an already-built and tagged image to its registry. The push is retried
up to three times.

```groovy
dockerPush('my-dockerhub-user/my-app:ci')
```

Log in first with `dockerLogin()` when pushing to Docker Hub. The image name
should include the appropriate registry namespace and tag.

### `incrementVersion(String releaseType = 'patch')`

Runs `npm version` with `--no-git-tag-version` and `--ignore-scripts`, updates
the version in `package.json`, and returns the resulting version string.
Supported release types are `major`, `minor`, and `patch`; other values fail the
step.

```groovy
def nextVersion = incrementVersion('minor')
echo "Next version: ${nextVersion}"
```

Requires npm and a project `package.json`.

### `pushToNpm(Map config = [:])`

Installs dependencies with `bun install --frozen-lockfile`, writes a temporary
`.npmrc` containing a GitHub Packages authentication token, and runs
`npm publish`. The temporary `.npmrc` is removed when the shell step exits.

The optional `credentialsId` setting selects a Jenkins **Secret text**
credential. It defaults to `github-npm-token`.

```groovy
pushToNpm()
pushToNpm(credentialsId: 'github-npm-token')
```

Requires Bun, npm, the project's Bun lockfile, and package metadata configured
to publish to GitHub Packages (for example, an appropriate `publishConfig`
registry in `package.json`). The step provides authentication but does not set
the registry for the package.

### `commitVersionUpdate(Map config = [:])`

Stages `package.json`. If there are staged changes, it commits them with the
message `chore: update package version [skip ci]` and pushes `HEAD` to the
configured branch on the existing `origin` remote. If there are no staged
changes, it prints a message and returns without committing. Any other changes
already staged in the workspace are included in that commit as well.

Supported settings and defaults:

| Setting | Default | Description |
| --- | --- | --- |
| `credentialsId` | `github-crendentials` | Jenkins `usernamePassword` credential used for the push. Note the default ID is spelled `crendentials` in the implementation. |
| `branch` | `main` | Destination branch on `origin`. |
| `gitUserName` | `Jenkins` | Git author name for the commit. |
| `gitUserEmail` | `jenkins@ktdrive.com` | Git author email for the commit. |

```groovy
commitVersionUpdate(
    credentialsId: 'github-credentials',
    branch: 'main',
    gitUserName: 'Jenkins',
    gitUserEmail: 'jenkins@example.com'
)
```

The `repository` option is not supported; the step pushes to the checkout's
existing `origin`. Ensure the Jenkins credential has permission to push to the
target branch.

## Credentials

| Credential ID | Jenkins type | Used by |
| --- | --- | --- |
| `GITHUB_TOKEN` | Secret text | `buildImage` BuildKit secret |
| `docker-hub-repo` | Username with password | `dockerLogin` |
| `github-npm-token` | Secret text | `pushToNpm` (default; can be overridden) |
| `github-crendentials` | Username with password | `commitVersionUpdate` (default; can be overridden) |

Credential IDs are fixed for `buildImage` and `dockerLogin`. The IDs used by
`pushToNpm` and `commitVersionUpdate` can be overridden through their `config`
map.

## Repository layout

```text
.
├── src/
│   └── com/example/Docker.groovy  # Docker build, login, and push implementation
└── vars/                          # Public Jenkins Pipeline steps
    ├── buildImage.groovy
    ├── commitVersionUpdate.groovy
    ├── dockerLogin.groovy
    ├── dockerPush.groovy
    ├── incrementVersion.groovy
    ├── pushToNpm.groovy
    └── runTest.groovy
```

`runTest(imageName)` currently delegates to `Docker.runTest(String)`, but that
method is not implemented in `src/com/example/Docker.groovy`. Do not call this
step until the implementation is added.
