//package com.moddel.lib

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

class genPipeline {
    String name
    String git_url

    Job build(DslFactory dslFactory) {
      dslFactory.pipelineJob("$name") {
          "description" "Description"
          concurrentBuild(false)

          triggers {
              scm("*/2 * * * *")
          }

          definition {
              cps {
                  script("""
                      timestamps {
                      node {
                        stage ("Checkout") {
                            checkout([\$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[url: \' $git_url \']]])
                        }
                        stage ("Setup Env") {
                            script {
                                envFileContents = readFile("\${WORKSPACE}/manifest.yaml")
                                config = readYaml file: "\${WORKSPACE}/manifest.yaml"
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
  } // end class def
