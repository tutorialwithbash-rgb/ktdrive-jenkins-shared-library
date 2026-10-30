# KTDrive Jenkins Shared Library

Reusable Jenkins Pipeline steps for standardizing common CI/CD tasks across KTDrive projects. The library provides small, composable Groovy steps for building and running Docker images, incrementing an npm package version, publishing a package to GitHub Packages, and committing a version update to GitHub.

## Features

- **Docker image build:** Builds an image from the workspace's `Dockerfile` and tags it with the requested image name.
- **Docker-based test run:** Runs the selected image as a container and removes the container when it exits. A non-zero container exit code fails the Pipeline step.
- **npm versioning:** Increments the package version as a `major`, `minor`, or `patch` release without creating a Git tag or running npm lifecycle scripts.
- **GitHub Packages publishing:** Installs dependencies with Bun using the frozen lockfile, authenticates to GitHub Packages using a Jenkins secret-text credential, and runs `npm publish`.
- **Version update commit:** Stages `package.json`, skips the commit if there are no staged changes, and pushes a version-update commit to the configured GitHub branch using a Jenkins username/password credential.

## Repository layout

```text
.
├── src/
│   ├── Main.groovy                 # Standalone example entry point
│   └── com/example/Docker.groovy  # Docker build and test implementation
└── vars/
    ├── buildImage.groovy           # Pipeline step: buildImage(imageName)
    ├── commitVersionUpdate.groovy   # Pipeline step: commitVersionUpdate(config)
    ├── incrementVersion.groovy     # Pipeline step: incrementVersion(releaseType)
    ├── pushToNpm.groovy             # Pipeline step: pushToNpm(config)
    └── runTest.groovy               # Pipeline step: runTest(imageName)
```

Scripts in `vars/` are the public Pipeline steps. The Docker implementation is in `src/com/example/Docker.groovy`; Jenkins makes classes in `src/` available to the shared library.

## Configure Jenkins

1. In **Manage Jenkins → System → Global Pipeline Libraries**, add this repository as a shared library and choose the version (branch, tag, or commit) that Pipelines should use.
2. Give the library a name, such as `ktdrive`, and configure its SCM repository URL and any required checkout credentials.
3. Configure the credentials used by publishing and version-commit steps. Credentials must be stored in Jenkins; do not put tokens or passwords in a Jenkinsfile.
4. Ensure the Jenkins agent has the tools required by the steps used in the Pipeline: Docker CLI and daemon access for image operations, npm for versioning, and Bun plus npm for package publishing.

## Use in a Jenkinsfile

Load the library with the name configured in Jenkins, then call its global steps:

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

        stage('Run tests') {
            steps {
                script {
                    runTest('ktdrive-app:ci')
                }
            }
        }
    }
}
```

The Docker steps operate on the checked-out workspace. `buildImage` expects a `Dockerfile` in the workspace; `runTest` runs the image and propagates its exit status to Jenkins.

## Pipeline steps

### `buildImage(String imageName)`

Builds the Docker image from the current workspace:

```groovy
buildImage('my-app:ci')
```

### `runTest(String imageName)`

Runs the named image with `docker run --rm`:

```groovy
runTest('my-app:ci')
```

The image must already exist on the agent, for example from an earlier `buildImage` step.

### `incrementVersion(String releaseType = 'patch')`

Runs `npm version` without creating a tag or running lifecycle scripts. Accepted release types are `major`, `minor`, and `patch`; an unsupported value fails the Pipeline step:

```groovy
def nextVersion = incrementVersion('minor')
```

This step requires a Node/npm project with a `package.json` in the current workspace.

### `pushToNpm(Map config = [:])`

Installs dependencies with `bun install --frozen-lockfile`, then publishes the package with `npm publish`. It uses a Jenkins **Secret text** credential whose ID is supplied as `credentialsId`; if omitted, the step uses `github-npm-token`. The credential is exposed only for the publishing block, shell tracing is disabled, and the temporary `.npmrc` is removed on exit.

```groovy
pushToNpm(credentialsId: 'github-npm-token')
```

The package must be configured for GitHub Packages, and the token must have permission to publish it. The workspace must contain the package metadata and a Bun lockfile.

### `commitVersionUpdate(Map config = [:])`

Stages `package.json`, checks whether the Git index has staged changes, and commits and pushes when there is something to commit. Supply a Jenkins **Username with password** credential ID and the Git identity and repository details:

```groovy
commitVersionUpdate(
    credentialsId: 'github-write-access',
    gitUserName: 'Jenkins',
    gitUserEmail: 'jenkins@example.com',
    repository: 'OWNER/REPOSITORY.git',
    branch: 'main'
)
```

`branch` is optional and defaults to `jenkins-job`. The password in the Jenkins username/password credential must be a valid GitHub personal access token with write access to the target repository (for a fine-grained token, grant **Contents: read and write**); the username should be the GitHub account associated with that token. The step supplies credentials through a temporary Git askpass helper rather than embedding the token in the remote URL, and adds `[skip ci]` to the commit message.

## Notes

- These steps are building blocks; each project is responsible for composing them into its own stages, selecting versions and tags, and configuring agent labels and permissions.
- Docker operations require the Jenkins agent to have access to a Docker daemon.
- Publishing and pushing changes are external side effects. Run those steps only in the appropriate release workflow and protect their Jenkins credentials and branch permissions.
- `src/Main.groovy` is a standalone “Hello world” example; it is not a Pipeline step.
