import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

def gitUrl = "https://github.com/vonnetworking/jenkins_seed_job.git"
def pipelineFolderName = "TEST_FOLDER"
def pipelineName = "TEST_JOB"


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
