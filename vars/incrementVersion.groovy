def call(String releaseType = 'patch') {
    if (!(releaseType in ['major', 'minor', 'patch'])) {
        error "Use major, minor, or patch."
    }

    def nextVersion = sh(
            script: "npm version ${releaseType} --no-git-tag-version --ignore-scripts",
            returnStdout: true
    ).trim()

    echo "New version: ${nextVersion}"
    return nextVersion
}