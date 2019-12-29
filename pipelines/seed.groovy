/* If you want, you can define your seed job in the DSL and create it via the REST API.
A gradle task is configured that can be used to create/update jobs via the Jenkins REST API, if desired. 
Normally a seed job is used to keep jobs in sync with the DSL, but this runner might be useful if you'd rather 
process the DSL outside of the Jenkins environment or if you want to create the seed job from a DSL script.

./gradle rest -Dpattern=<pattern> -DbaseUrl=<baseUrl> [-Dusername=<username>] [-Dpassword=<password>]

    pattern - ant-style path pattern of files to include
    baseUrl - base URL of Jenkins server
    username - Jenkins username, if secured
    password - Jenkins password or token, if secured

 See https://github.com/sheehan/job-dsl-gradle-example#rest-api-runner
*/
job('NDMJobDSL') {
    scm {
        git 'https://moddel-tw.fhlmc.com:8046/scm/techpillar/jenkinsdsl.git'
    }

    steps {
        gradle 'clean build'
        dsl {
            external 'jobs/devops/**/*.groovy'
            additionalClasspath 'src/main/groovy'
        }
    }
    publishers {
        archiveJunit 'build/test-results/**/*.xml'
    }
}
