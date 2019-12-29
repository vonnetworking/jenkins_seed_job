package com.fhlmc.moddel

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

/**
 * Class for creating an Moddel Release Candidate Job
 */
class MDProdReleasePipelineBuilder {

    String pipelineName
    String pipelineFolderName
    String jobDescription
    String ldapGroup
    String prodReleasePOLdapGroup
    String BBProject
    String BBGitRepo
    String spinnnakerAppName
    String notificationRecipients
    String notificationSender
    String prodReleasePipeline


    Job build(DslFactory dslFactory) {
        dslFactory.pipelineJob("$pipelineFolderName/$pipelineName") {
            it.description this.jobDescription
           if (prodReleasePOLdapGroup =='') {
              authorization {
                permission('hudson.model.Item.Read:anonymous')
                permission('hudson.model.Item.Read', 'anonymous')
                permission('hudson.model.Item.Read', ldapGroup)
                permission('hudson.model.Item.Cancel', ldapGroup)
                permission('hudson.model.Item.Workspace', ldapGroup)
                permission('hudson.model.Item.Build', ldapGroup)
            	}
            } else {
            authorization {
                permission('hudson.model.Item.Read:anonymous')
                permission('hudson.model.Item.Read', 'anonymous')
                permission('hudson.model.Item.Read', prodReleasePOLdapGroup)
                permission('hudson.model.Item.Cancel', prodReleasePOLdapGroup)
                permission('hudson.model.Item.Workspace', prodReleasePOLdapGroup)
                permission('hudson.model.Item.Build', prodReleasePOLdapGroup)
            	}
            }
            concurrentBuild(false)

            /* TECHPILLAR-2428 and 2768  commenting the trigger
            triggers {
                bitbucketPush()
            }*/

            def gitURL = "https://moddel-tw.fhlmc.com:8046/scm/${BBProject}/${BBGitRepo}.git"
            def fortifyScan = "md-fortify-scan-${BBGitRepo}"
            def prodReleasePipeline = "\'${prodReleasePipeline}\'"
            def spinnnakerAppName = "${BBProject}${BBGitRepo}".replaceAll("[^a-zA-Z0-9]", "")
			def releaseScope
          
            definition {
                cps {
                    sandbox()
                    script("""
                        @Library('mdjsl') _

                        import java.util.*
                        import java.text.*

                        timestamps {
                            node('moddel') {
                           		 stage ('Clone') {
                                    checkout([\$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [ [\$class: 'LocalBranch'], [\$class: 'WipeWorkspace'], [\$class: 'DisableRemotePoll'], [\$class: 'BuildChooserSetting',buildChooser: [\$class: 'DefaultBuildChooser']]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'bitbucket-clone', url: \' $gitURL \']]]) 
                                sleep 5
                                } // End of stage: Clone
                          
                                stage('Input Options'){
                                      def continueBuild = false
                                    echo "select deploy environment [CTE, Prod]"
                                       // script {
                                            releaseScope = env.RELEASE_SCOPE
                                            echo "\${releaseScope}"
                                            releaseScope = input message: 'User input required', ok: 'Release',
                                            parameters: [choice(name: 'RELEASE_SCOPE', choices: 'CTE\\nProduction', description: 'What is the release environment?')]

                                            echo "Electing to deploy to \${releaseScope}"
											env.shellReleaseScope = releaseScope
                                            if (releaseScope == 'Production'){
                                            echo "Release readiness state validation"
                                            script {
                                                
                                                try {
                                                    def userInput = input(
 														id: 'userInput', message: 'Please provide the Release Readiness Issue Key and the Fix Version. By clicking proceed you are confirming that this change has passed release readiness.', parameters: [
														[\$class: 'StringParameterDefinition', defaultValue: '', description: 'Mandatory: Release Readiness Issue Key. For Ex: TECHPILLAR-1234', name: 'Release_Readiness_Issue_Key'],
														[\$class: 'StringParameterDefinition', defaultValue: '', description: 'Optional: Fix Version for this release. For Ex: V1.0', name: 'FixVersion']
														])

                                                    //def Release_Readiness_Issue_Key = userInput['Release_Readiness_Issue_Key']
                                                 //   withCredentials([string([usernameColonPassword(credentialsId: 'bitbucket-clone', variable: 'JIRAUSERPASS')])]) { not working as expected
                                                     withCredentials([usernameColonPassword(credentialsId: 'bitbucket-clone', variable: 'JIRAUSERPASS')]) {
                                                                // Jenkins API call to Jira 
                                                                 sh('''curl -D- -u "\$JIRAUSERPASS" -X GET -H "Content-Type: application/json" https://moddel-tw.fhlmc.com:8043/rest/api/2/issue/'''+userInput['Release_Readiness_Issue_Key']+''' -o response.json''')
                                                            }
                                                    def response_data = readJSON file:'response.json'
                                                    echo response_data.fields.issuetype['name']
                                                    echo response_data.key
                                                    echo response_data.fields.status['name']
                                                    
                                                    //Checking if the Release Readiness Issue Key is in Done Status
                                                    if(response_data.fields.status['name']=="Done" && response_data.fields.issuetype['name']=="Rel-Readiness"){
                                                          println "Release Readiness Issue Key: \${response_data.key} is passed with validation"
                                                          continueBuild = true
                                                    } //end if
                                                    else if(response_data.fields.issuetype['name']!="Rel-Readiness"){
                                                          println "Aborting the build. The entered Issue Key: \${response_data.key} does not belong to Issue Type: Rel-Readiness."
                                                          error('Aborting the build. The entered Issue Key does not belong to Issue Type: Rel-Readiness.')
                                                    } 
                                                    else{
												 		  println "Aborting the build. The entered Release Readiness Issue Key: \${response_data.key} is not in DONE Status."
												 		  error('Aborting the build. The entered Release Readiness Issue Key is not in DONE Status')
												 	 }
                                                    
                                                    //Checking if the Version entered is same as the FixVersion on the Release Readiness Issue
                                                    if(userInput['FixVersion']){
                                                       echo "Checking if the Version entered is same as the FixVersion on the Release Readiness Issue"
                                                      // def issueFixVersion = response_data.fields.fixVersions.name[0]  
                                                       // def relVersion = userInput['FixVersion']
                                                      if(response_data.fields.fixVersions.name[0] != userInput['FixVersion']){ 
                                                     		 echo "Error Scenario"
                                                      		 continueBuild = false
                                                         	 error('Aborting the build. The entered Release Readiness Issue version doesnt match the Release Version Provided on this build')
                                                         }
                                                    }//end if

                                                } catch(err) { // input false
                                                 script{
                                                    echo "In catch Block"
                                                  //  def user = err.getCauses()[0].getUser()
                                                   // userInput = false
                                                  //  echo "Aborted by: [\${user}]"
                                                  currentBuild.result = 'FAILURE'
                                                }//end script
                                            }

                                                                                                
                                                    if (continueBuild == true) {
                                                        // Release approed for prod deploment
                                                        //echo "Release is ready for Prod deployment successful"
                                                        def response_data = readJSON file: 'response.json'
                                                        println "Release \${response_data.key} status is \${response_data.fields.status['name']} and ready for Prod Deployment."
                                                    } else {
                                                        // Release not ready for prod deployment
                                                        echo "Release is not ready for prod deployment"
                                                        currentBuild.rawBuild.result = Result.ABORTED
														throw new hudson.AbortException('Aborting as release is not ready for production deployment')
														echo 'Further stages will not be executed'
                                                    } // end if else
                                                                                           
                                          } // script
                                        } // end if

                            } // end stage

  		stage('Deploy to CTE') {
			if (releaseScope == 'CTE'){	
				checkout([\$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [ [\$class: 'LocalBranch'], [\$class: 'WipeWorkspace'], [\$class: 'DisableRemotePoll'], [\$class: 'BuildChooserSetting',buildChooser: [\$class: 'DefaultBuildChooser']]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'bitbucket-clone', url: \' $gitURL \']]])               
				echo "Checkout deployment scripts"
				dir('spinnaker') {
					checkout([\$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [[\$class: 'LocalBranch'], [\$class: 'WipeWorkspace']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'bitbucket-clone', url: 'https://moddel-tw.fhlmc.com:8046/scm/gi/spinnaker.git']]])
				}
				echo "deploy to CTE"
				sh '''
					#!/bin/bash
					sleep 3
					cd spinnaker/Spinnaker/freddiemac/spinnaker/onboarding/scripts/
					chmod 755 ./*
					./SpinnakerDeploy.sh \${WORKSPACE}/ndm/deploy/cd-userparams_pp.config SystemParams.config Restricted
                  '''
            } 
        } // end stage. Job end here if CTE endpoint selected

        stage('Deploy to Production'){
        	 if (releaseScope == 'Production'){
                echo "prepare artifact for prod release"
                //Cloning workspace for production deployment YAML
               // checkout([\$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [ [\$class: 'LocalBranch'], [\$class: 'WipeWorkspace'], [\$class: 'DisableRemotePoll'], [\$class: 'BuildChooserSetting',buildChooser: [\$class: 'DefaultBuildChooser']]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'bitbucket-clone', url: \' $gitURL \']]])               
                echo "Checkout deployment scripts"
                // prepare prod image
                withCredentials([string(credentialsId: 'nimbolt', variable: 'PASSWORD')]) {
                    sh '''
                           echo "Demote Current Pord Images and yml"
                          # moddel-prod-to-prodprev
                        '''
                } // end of withCreds

				dir('spinnaker') {
					checkout([\$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [[\$class: 'LocalBranch'], [\$class: 'WipeWorkspace']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'bitbucket-clone', url: 'https://moddel-tw.fhlmc.com:8046/scm/gi/spinnaker.git']]])
				}
                echo "deploy to Production"
                echo 'Invoking the Prod Release Deployment Pipeline to Production'
                sh '''
                   #!/bin/bash
                   sleep 3
                   cd spinnaker/Spinnaker/freddiemac/spinnaker/onboarding/scripts/
                   chmod 755 ./*
                   ./SpinnakerDeploy.sh \${WORKSPACE}/ndm/deploy/cd-userparams.config SystemParams.config Restricted
               '''
          } // end if
        } // end deploy stage

        stage('Production Smoke Test') {
                echo "Production Smoke Tests Manual"
           sleep 3
        } // end of stage

        stage('Approve Smoke Test'){
        	 if (releaseScope == 'Production'){
                script{
                    def userInput
                    try {
                        userInput = input(
                                id: 'Proceed1', message: 'Was smoke test successful?', parameters: [
                                [\$class: 'BooleanParameterDefinition', defaultValue: true, description: '', name: 'Please confirm you agree with this']
                        ])
                    } catch(err) {
                        def user = err.getCauses()[0].getUser()
                        userInput = false
                        echo "Aborted by: [\${user}]"
                    }
                  
                        if (userInput == true) {
                            echo "Smoke test was successful"
                            echo "promote artifact"
                            withCredentials([string(credentialsId: 'nimbolt', variable: 'PASSWORD')]) {
                            	//GIT_SHORT_COMMIT = sh(returnStdout: true, script: "git log -n 1 --pretty=format:'%h'").trim()
                            //echo in jenkins console

                            //wanted to send these info to build artifacts, append to any file
                            //sh("echo \${GIT_SHORT_COMMIT} > \${GIT_SHORT_COMMIT}")
                            echo "Checout deployment scripts and application code"
                            checkout([\$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [ [\$class: 'LocalBranch'], [\$class: 'WipeWorkspace'], [\$class: 'DisableRemotePoll'], [\$class: 'BuildChooserSetting',buildChooser: [\$class: 'DefaultBuildChooser']]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'bitbucket-clone', url: \' $gitURL \']]]) 
                    		dir('spinnaker') {
                        		checkout([\$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [[\$class: 'LocalBranch'], [\$class: 'WipeWorkspace']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'bitbucket-clone', url: 'https://moddel-tw.fhlmc.com:8046/scm/gi/spinnaker.git']]])
                        		//checkout([\$class: 'GitSCM', branches: [[name: '*/feature/c48273-prod-or-cte-deploy']], doGenerateSubmoduleConfigurations: false, extensions: [[\$class: 'LocalBranch'], [\$class: 'WipeWorkspace']], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'bitbucket-clone', url: 'https://moddel-tw.fhlmc.com:8046/scm/gi/spinnaker.git']]])
	                }

                            sh '''
                         		sleep 3
                       			cd spinnaker/Spinnaker/freddiemac/spinnaker/onboarding/scripts/
                        		chmod 755 ./*
                        		./TagPushPPToProdDockerImage.sh \${WORKSPACE}/ndm/deploy/cd-userparams_pp.config \${WORKSPACE}/ndm/images
                            '''

                            def images = []
                            sh(returnStdout: true, script: "cat ndm/images")
                                    .trim()
                                    .split('\\n').each { images << it }

                            images.each { image ->
                                image = image.split()[0]
                                println "uploading yaml files to the image: \${image}"
                                withEnv(["DOCKER_IMAGE=\${image}"]) {
                                    rtUpload(
                                            serverId: 'moddel-art',
                                            spec:
                                                    ''' {
                                            "files": [
                                            {
                                                "pattern": "ndm/deploy/*.yml",
                                                "target": "docker-moddel/train/\${DOCKER_IMAGE}/md-prod/",
                                                "flat": "true"
                                            }
                                            ]
                                        } '''
                                    ) // end rtUpload
                                } // end of withEnv
                            } // end of images.each loop
                            } // end of withCreds
                        } else {
                            echo "Smoke test failed"
                            currentBuild.result = 'FAILURE'
                        }
                    
                } // end script
            }
        } // end of stage

        stage ('Notification') {
                echo "Stage Notify"
                script {
                emailext (
                    subject: "Status of pipeline: \${currentBuild.fullDisplayName}",
                    body: "Your Production Release :\${env.BUILD_URL} has completed with status : \${currentBuild.currentResult }",
                    to: \'$notificationRecipients\',
                    from: \'$notificationSender\'
                   )
                } // end script
            }
                                }

                        }
                       """.stripIndent())
                } // end cps
            } // end definition
        } // end Timestamp job
    } // end Factory DSL
} // end class MDProdReleasePipelineBuilder
