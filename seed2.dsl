import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

/* def slurper = new ConfigSlurper()
slurper.classLoader = this.class.classLoader
def config = slurper.parse(readFileFromWorkspace('pipelines/moddel/mdProducts.config'))
*/
def config = readYaml file: "${WORKSPACE}/config.yaml"

config.each { jobname, data ->
  Job build(DslFactory dslFactory) {
    dslFactory.pipelineJob("$pipelineFolderName/$pipelineName") {
    it.description this.jobDescription

    def gitUrl = "https://github.com/vonnetworking/jenkins_seed_job.git"
    definition {
        cps {
            sandbox()

        script("""
          timestamps {
            node('moddel') {
              stage ('Clone') {
                  checkout([\$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [ [\$class: 'LocalBranch'], [\$class: 'WipeWorkspace'], [\$class: 'DisableRemotePoll'], [\$class: 'BuildChooserSetting',buildChooser: [\$class: 'DefaultBuildChooser']]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'bitbucket-clone', url: \' $gitURL \']]])
              } // End of stage: Clone
              stage ('Init') {
                environmentVariables {
                  env('TESTVAR', '1')
                  propertiesFile('pipeline.properties')
                }
              }
              stage ('Check Env') {
                sh '''echo ${TESTVAR}'''
              }
            }
          }""") //End script
        } //End cps
      } //end definition
    } //end Job Def
  } //end Job factory
} //end each block

println "BRANCHES: End"
