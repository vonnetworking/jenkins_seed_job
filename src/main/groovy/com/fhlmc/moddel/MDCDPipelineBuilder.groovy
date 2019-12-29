package com.fhlmc.moddel

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

/*
 * Factory class to generate Preprod Deployment Pipelines
 * It will be triggered manually or via the succesful completion of a release-candidate build
 * The Pipeline will:
 *  1) Deploy the release candidate image (Docker image tag :md-rc) from the modern delivery docker registery into preprod
 *  2) Run automated acceptance tests against the preprod environment
 *  3) If automated acceptance tests are successful, promote the release candidate to preprod (Docker image tag :md-pp)
 *  4) Ensure Preprod is up with a valid set of containers (assuming this is not a new product, which may not have a valid preprod image)
 *  4a) Bring down the preprod environment
 *  4b) Deploy preprod containers in the preprod environment in dsl
 *
 */
class MDCDPipelineBuilder {

    String pipelineName
    String pipelineFolderName
  	String jobDescription
    String ldapGroup
    String BBProject
    String BBGitRepo
    String spinnnakerAppName

    Job build(DslFactory dslFactory) {
        dslFactory.pipelineJob("$pipelineFolderName/$pipelineName") {
            it.description this.jobDescription
            authorization {
                permission('hudson.model.Item.Read:anonymous')
                permission('hudson.model.Item.Read', 'anonymous')
                permission('hudson.model.Item.Read', ldapGroup)
                permission('hudson.model.Item.Cancel', ldapGroup)
                permission('hudson.model.Item.Workspace', ldapGroup)
                permission('hudson.model.Item.Build', ldapGroup)
            }
            concurrentBuild(false)

            def gitURL = "https://moddel-tw.fhlmc.com:8046/scm/${BBProject}/${BBGitRepo}.git"
            def spinnnakerAppName = "${BBProject}${BBGitRepo}".replaceAll("[^a-zA-Z0-9]", "")

            definition {
                cps{
                    sandbox()
                    script("""
                            @Library('mdjsl') _
                            pipeline { 
                                agent { 
                                    label 'moddel'
                                } 
                            
                                stages {
                                    stage('Clone') {
                                        steps {
                                            deleteDir()
                                            gitCheckout(
                                                branch: 'master',
                                                url: "$gitURL"
                                            )
                                         } // end of step
                                    }  // end of stage
                               
                                    stage('Deploy RC To Preprod') { 
                                        steps { 
                                        
                                   
                                   sh '''
                                                                                                       
                                   echo ''
                                   echo 'Invoking the Release Candidate CD Pipeline'
                                   echo ''
                                   .  \$WORKSPACE/ndm/deploy/cd-userparams_rc.config
                                   SPINNAKER_EVENT_ID=\$(curl http://fmaccdspin-np-gate.itn01.n.fhlmc.com/webhooks/webhook/\${AppNameHere}-md-rc-deploy -X POST -H "content-type: application/json" -d "{ }" | jq '.eventId' | sed  's/"//g')
                                   /home/nimble/bin/pipelinestatus -a \$AppNameHere -id \$SPINNAKER_EVENT_ID
                                   echo ''
                                   echo "To view the status of the deployment, please logon to your NonProd VDI and navigate to the url below:"
                                   echo "https://fmaccdspin-np.itn01.n.fhlmc.com/#/applications/\${AppNameHere}/executions"
                                   echo ''
                                   '''
                                                                               
                                        } // end of step
                                    } // end of stage
                                    
                                    stage('e2e Acceptance Test') { 
                                        steps { 
                                            withCredentials([string(credentialsId: 'spinnaker_curl', variable: 'SPIN_PASSWORD')]) { 
                                                    sh ''' 
                                                    echo 'e2e Acceptance Test'
                                                    .  \$WORKSPACE/ndm/deploy/cd-userparams_rc.config
                                                    SPINNAKER_ENDPOINT="http://fmaccdspin-np-gate.itn01.n.fhlmc.com/applications/\${AppNameHere}/loadBalancers"
                                                    SERVICEURL=`curl -L -u \"ae_spinkr_jnkns_mgr:\$SPIN_PASSWORD\" -X GET "\$SPINNAKER_ENDPOINT" 2> /dev/null | jq ".[].manifest.status.loadBalancer.ingress[].hostname" | sed 's/"//g'`
                                                    
                                                    CURL_CMD="curl -w httpcode=%{http_code}"
                                                    CURL_MAX_CONNECTION_TIMEOUT="-m 100"
                                                    CURL_RETURN_CODE=0
                                                    CURL_OUTPUT=`\${CURL_CMD} \${CURL_MAX_CONNECTION_TIMEOUT} \${SERVICEURL} 2> /dev/null` || CURL_RETURN_CODE=\$?
                                                    echo "httpcode is \$httpCode"
                                                    
                                                    if [ \${CURL_RETURN_CODE} -ne 0 ]; then  
                                                        echo "Curl connection failed with return code - \${CURL_RETURN_CODE}"
                                                       #SPINNAKER_ENDPOINT='http://fmaccdspin-np-gate.itn01.n.fhlmc.com/webhooks/webhook/delete-rc-deployment'
                                                        #curl "\$SPINNAKER_ENDPOINT" -X POST -H "content-type: application/json" -d "{ }"
                                                        #SPINNAKER_EVENT_ID=\$(curl "\$SPINNAKER_ENDPOINT" -X POST -H "content-type: application/json" -d "{ }" | jq '.eventId' | sed  's/"//g')
                                                        #/home/nimble/bin/pipelinestatus -a \${AppNameHere} -id \$SPINNAKER_EVENT_ID
                                                        exit 1
                                                    else
                                                        echo "Testing in staging is successfull. Promote images"
                                                        
                                                    fi
                                                    '''
                                            } // end of withCreds       
                                        } // end of step
                                    } // end of stage                                                             
                                    
                                    stage('Promote Images') { 
                                        steps { 
                                            withCredentials([string(credentialsId: 'nimbolt', variable: 'PASSWORD')]) {
                                                sh '''
                                                    echo "Promote Images"
                                                    moddel-rc-to-pp
                                                '''
                                            } // end of withCreds
                                        } // end of step
                                    } // end of stage  

                                    stage ('Start PreProd') {
                                        steps {
                                            withCredentials([string(credentialsId: 'spinnaker_curl', variable: 'SPIN_PASSWORD')]) {  

                                                // clone spinnaker repo
                                                dir('spinnaker') {
                                                    checkout([\$class: 'GitSCM', branches: [[name: 'origin/master']], doGenerateSubmoduleConfigurations: false, extensions: [[\$class: 'LocalBranch'], [\$class: 'WipeWorkspace']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'bitbucket-clone', url: 'https://moddel-tw.fhlmc.com:8046/scm/gi/spinnaker.git']]])
                                                }

                                                sh '''
                                                #!/bin/bash
                                                sleep 3
                                                cd spinnaker/Spinnaker/freddiemac/spinnaker/onboarding/scripts/
                                                chmod 755 ./*
                                                ./SpinnakerDeploy.sh \$WORKSPACE/ndm/deploy/cd-userparams_dev.config SystemParams.config Confidential
                                                '''
                                            } // end of withCreds    
                                        } // end of step
                                    } // end of stage  
                                                                                                                                             
                                } // end of stages
                            } // end of pipeline				
                        """.stripIndent()
                    )
                }
            }
        } // dslFactory.pipelineJob
    } // end of dslFactory
}
