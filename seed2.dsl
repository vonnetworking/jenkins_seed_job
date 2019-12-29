import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job
@Grab('org.yaml:snakeyaml:1.17')
import org.yaml.snakeyaml.Yaml

class genPipeline {
    def config = new Yaml().load(("${WORKSPACE}/config.yaml" as File).text)

    config.each { jobname, data ->
      println "Building Job " + jobname + " Using data: " + data
      Job build(DslFactory dslFactory) {
        dslFactory.pipelineJob("$pipelineFolderName/$pipelineName") {
            it.description this.jobDescription
            concurrentBuild(false)

            triggers {
                scm("*/2 * * * *")
            }

            def git_url = data.git_url

            { definition {
                cps {
                    sandbox()

                    script("""
                        timestamps {
                        node {
                          label any
                          stage ("Checkout") {
                              checkout([\$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: \' $git_url \']]])
                          }
                          stage ("Setup Env") {
                              script {
                                  envFileContents = readFile("${WORKSPACE}/manifest.yaml")
                                  config = readYaml file: "${WORKSPACE}/manifest.yaml"
                                  echo "Pipeline Version: " + config.pipeline_version
                                  echo "initializing MD pipeline common lib version " + config.pipeline_version
                              }
                              library 'MDPipeline@' + config.pipeline_version
                          }
                          MPLPipelineV2 {}
                          }
                        } // end of timestamp
                    """.stripIndent())
                } // end of cps
              } // end of definition
          } // end of pipelineJob
      } // end of dslFactory
    } //end each block
  println "BRANCHES: End"
}

new genPipeline().build(this)
