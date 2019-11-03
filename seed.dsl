def gitUrl = "https://github.com/example/project.git"

job("Test-Build") {
    description "Creates Test Build project"
    //parameters {
    //    stringParam('COMMIT', 'HEAD', 'Commit to build')
    //}
    scm {
        git {
            remote {
                url gitUrl.
                branch "origin/master"
            }
            extensions {
                wipeOutWorkspace()
                localBranch master
            }
        }
    }
    steps {
        shell "Look: I'm building master!"
    }
}
