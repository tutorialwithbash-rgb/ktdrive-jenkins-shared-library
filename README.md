# KTDrive Jenkins Shared Library

This repository contains a small set of Jenkins shared-library steps for Docker image builds, Docker Hub pushes, version bumps, and GitHub package publishing. The public entry points live in `vars/`; the implementation logic is in `src/com/example/Docker.groovy`.

## Current implementation

The repository currently exposes these pipeline steps:

- `buildImage(imageName, dockerfilePath = 'Dockerfile', context = '.')`
- `dockerLogin()`
- `dockerPush(imageName)`
- `incrementVersion(releaseType = 'patch')`
- `pushToNpm(config = [:])`
- `commitVersionUpdate(config = [:])`

There is also a `runTest.groovy` wrapper, but the underlying `Docker` class does not currently define `runTest(String)`. In other words, the wrapper exists, but the method it calls is not implemented yet.

## Repository layout

```text
.
├── src/
│   ├── Main.groovy                 # Standalone example entry point
│   └── com/example/Docker.groovy  # Docker helpers used by the vars scripts
├── vars/
│   ├── buildImage.groovy           # buildImage(imageName, dockerfilePath, context)
│   ├── dockerLogin.groovy          # dockerLogin()
│   ├── dockerPush.groovy           # dockerPush(imageName)
│   ├── incrementVersion.groovy     # incrementVersion(releaseType)
│   ├── pushToNpm.groovy            # pushToNpm(config)
│   ├── commitVersionUpdate.groovy # commitVersionUpdate(config)
│   └── runTest.groovy             # runTest(imageName) wrapper (not implemented in Docker.groovy)
└── README.md
```

## Jenkins setup

1. In Jenkins, add this repository as a Global Pipeline Library.
2. Give the library a name (for example `ktdrive`) and configure the repository URL and branch/tag/commit.
3. Create the credentials used by the steps in Jenkins and reference them by ID from the pipeline config.
4. Ensure the agent has Docker, npm, and Bun available when the relevant steps are used.

## Example usage

```groovy
@Library('ktdrive') _

pipeline {
    agent any

    stages {
        stage('Build image') {
            steps {
                script {
                    buildImage('ktdrive-app:ci')
                }
            }
        }

        stage('Version bump') {
            steps {
                script {
                    def nextVersion = incrementVersion('patch')
                    echo "Next version: ${nextVersion}"
                }
            }
        }

        stage('Publish package') {
            steps {
                script {
                    pushToNpm(credentialsId: 'github-npm-token')
                }
            }
        }
    }
}
```

## Pipeline steps

### `buildImage(String imageName, String dockerfilePath = 'Dockerfile', String context = '.')`

Calls `new Docker(this).buildDockerImage(imageName, dockerfilePath, context)` and runs:

```sh
docker build -f <dockerfilePath> -t <imageName> <context>
```

Example:

```groovy
buildImage('my-app:ci')
buildImage('my-app:ci', 'docker/Dockerfile', '.')
```

### `dockerLogin()`

Uses a Jenkins `usernamePassword` credential with the default ID `docker-hub-repo` and logs in to Docker Hub using `docker login --password-stdin`.

```groovy
dockerLogin()
```

### `dockerPush(String imageName)`

Pushes the named Docker image to the configured registry:

```groovy
dockerPush('my-app:ci')
```

### `incrementVersion(String releaseType = 'patch')`

Runs:

```sh
npm version <releaseType> --no-git-tag-version --ignore-scripts
```

Allowed values are `major`, `minor`, and `patch`. Unsupported values will fail the step.

```groovy
def nextVersion = incrementVersion('minor')
```

### `pushToNpm(Map config = [:])`

Uses a Jenkins **Secret text** credential passed as `GITHUB_TOKEN` and runs:

```sh
bun install --frozen-lockfile
printf '//npm.pkg.github.com/:_authToken=%s\n' "$GITHUB_TOKEN" > .npmrc
npm publish
```

Default credential ID: `github-npm-token`

```groovy
pushToNpm(credentialsId: 'github-npm-token')
```

### `commitVersionUpdate(Map config = [:])`

Stages `package.json`, checks whether there are staged changes, and if so commits them with a message ending in `[skip ci]` and pushes to the configured branch.

Current defaults in the script:

- `credentialsId`: `github-crendentials`
- `branch`: `main`
- `gitUserName`: `Jenkins`
- `gitUserEmail`: `jenkins@ktdrive.com`

This step uses a temporary Git askpass helper and pushes with:

```sh
git push origin "HEAD:refs/heads/$GIT_BRANCH"
```

Example:

```groovy
commitVersionUpdate(
    credentialsId: 'github-crendentials',
    gitUserName: 'Jenkins',
    gitUserEmail: 'jenkins@ktdrive.com',
    branch: 'main'
)
```

Note: this implementation does not accept a `repository` parameter and does not build a remote URL from one; it pushes to `origin`.

### `runTest(String imageName)`

A wrapper exists for this step, but the method is not implemented in `src/com/example/Docker.groovy` at the moment. If this step is needed, the Docker class must include a matching `runTest(String)` method before it can be used in a pipeline.

## Notes

- The shared library is a set of reusable building blocks rather than a full deployment framework.
- Docker operations require a Jenkins agent with access to a Docker daemon.
- `src/Main.groovy` is a standalone sample and is not a Jenkins pipeline step.
- Publishing and version-push steps are side effects that should only be executed in the appropriate release workflow with restricted credentials.
