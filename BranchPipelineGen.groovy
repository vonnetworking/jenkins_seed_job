//package com.moddel.lib

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

class BranchPipelineGen {
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
                          /* grabs config file from repo if it exists @ /pipeline_config.yaml
                             if the repo does not contain a config file then a default configuration
                             is created and written to the workspace for use in further stages

                             vars
                             config_file - string - defines path to config file in workspace
                             config - map - contains pipeline config params
                          */
                            script {
                                def config_file = "\${WORKSPACE}/pipeline_config.yaml"
                                def config_file_exists = fileExists 'file'
                                if (config_file_exists) {
                                  config = readYaml file: config_file
                                } else {
                                  config = { "pipeline_version": "master" }
                                  writeYaml file: config_file, data: config
                                }
                                echo "Pipeline Version: " + config.pipeline_version
                                echo "initializing MD pipeline common lib version " + config.pipeline_version
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
