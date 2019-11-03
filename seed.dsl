def gitUrl = "https://github.com/vonnetworking/jenkins_seed_job.git"

job("Test-Build") {
    description "Creates Test Build project"
    //parameters {
    //    stringParam('COMMIT', 'HEAD', 'Commit to build')
    //}
    scm {
        git {
            remote {
                url gitUrl
                branch "origin/master"
            }
        }
    }
    steps {
        shell "Look: I'm building master!"
    }
}
