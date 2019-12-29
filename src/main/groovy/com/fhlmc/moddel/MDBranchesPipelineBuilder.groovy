package com.fhlmc.moddel

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

/**
 * Class for creating an Moddel Branches Job 
 */
class MDBranchesPipelineBuilder {

    String pipelineName
    String pipelineFolderName
    String jobDescription
    String BBProjectName
    String ldapGroup
    String BBProject
    String BBGitRepo
    String envVar = ''
    String gradleVersion
    String gradleTasks
    String gradleSwitches
    String gradleBuildScriptDir
    String junitResults = ''
    String coverageResults = ''
    List<String> emails = []
    String jacocoExec = '**/build/jacoco*/**/*.exec'
    String jacocoClasses
    String jacocoSource
    String jacocoExcludeClasses
    String jacocoMinLines
    String jacocoMaxLines
    String coberturaXml = ''
    String mvnjunitResults 
	String mvnjacocoExec = '**/target/coverage-reports/*.exec'	
	String mvnjacocoClasses	
	String mvnjacocoSource	
    String defaultSonarProjectKey
    String defaultSonarProjectName
    String defaultSonarSrc
    String defaultSonarJavaBinaries
    String spinnnakerAppName
    String cdPipeline
  	String buildTool
    String buildCommand

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

            triggers {
                bitbucketPush()
            }

            def gitURL = "https://moddel-tw.fhlmc.com:8046/scm/${BBProject}/${BBGitRepo}.git"
            def spinnnakerAppName = "${BBProject}${BBGitRepo}".replaceAll("[^a-zA-Z0-9]", "")
            def cdPipeline = "\'${cdPipeline}\'"
          	if(buildTool == ''){buildTool = 'genericBuild'}
          	

            definition {
                cps {
                    sandbox()

                    script("""
                        @Library('mdjsl') _
                        timestamps {
                            node('moddel') {
                                stage ('Clone') {
                                    checkout([\$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [ [\$class: 'LocalBranch'], [\$class: 'WipeWorkspace'], [\$class: 'DisableRemotePoll'], [\$class: 'BuildChooserSetting',buildChooser: [\$class: 'InverseBuildChooser']]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'bitbucket-clone', url: \' $gitURL \']]]) 
                                } // End of stage: Clone
                                
                                stage ('Build') {
                                	buildComnand = \'${buildCommand}\'
                                	if ('$buildTool' == 'maven') {
                                    	//if ( fileExists 'pom.xml') {
                                    	//	sh "mvn clean $buildCommand"
                                    	//}
                                    	//else {
                                    		sh "find . -name 'pom.xml' -exec mvn clean $buildCommand -f '{}' \\';'"
                                    	//}
                                        
                                    // test properties
                                    junit \'$mvnjunitResults\'
                                    jacoco( 
                                        execPattern: \'$mvnjacocoExec\',
                                        classPattern: \'$mvnjacocoClasses\',
                                        sourcePattern: \'$mvnjacocoSource\',
                                        inclusionPattern: '**/*.class',
                                        exclusionPattern: \'$jacocoExcludeClasses\'
                                    )
                                    } else 
                                        {
                                            sh '''
                                              start-headless-display						
                                              gradle build
                                            '''
                                    
                                        if('$buildTool' != 'npm')
                                        {
                                        junit \'$junitResults\'
                                        jacoco( 
                                            execPattern: \'$jacocoExec\',
                                            classPattern: \'$jacocoClasses\',
                                            sourcePattern: \'$jacocoSource\',
                                            inclusionPattern: '**/*.class',
                                            exclusionPattern: \'$jacocoExcludeClasses\'
                                        )
                                        }
                                    }
                                
                                } // End of stage: Build
                        
                                stage ('SonarQube') {
                                    echo 'Sonar Scanner'
                                    def propertiesSet = fileExists 'sonar-project.properties'
                                    if('$buildTool' == 'npm' && !propertiesSet){ echo 'sonar-project.properties is required for NPM builds!' }
                                    if (propertiesSet) {
                                        echo 'Using sonar-project.properties file -- default sonar.sources and sonar.java.binaries will NOT be used'
                                        sh "/opt/sonar-scanner/bin/sonar-scanner -Dsonar.host.url=\$SONAR_HOST -Dsonar.projectKey='${defaultSonarProjectKey}' -Dsonar.projectName='${defaultSonarProjectName}'"
                                    } else {
                                        sh "/opt/sonar-scanner/bin/sonar-scanner -Dsonar.host.url=\$SONAR_HOST -Dsonar.projectKey='${defaultSonarProjectKey}' -Dsonar.projectName='${defaultSonarProjectName}' -Dsonar.sources='${defaultSonarSrc}' -Dsonar.java.binaries='${defaultSonarJavaBinaries}' -Dsonar.jacoco.reportPaths='./build/jacoco/test.exec'"
                                    }
                                } // End of stage: SonarQube
                                
                                if ($cdPipeline == 'y') {
                                   stage ('Docker') {
                                        def buildDockerImages = fileExists 'ndm/images'
                                        if (buildDockerImages) {
                                            echo 'Found ndm/images. Commencing build-images.'
                                        
                                            sh '''
                                            build-images
                                            '''
                                        } else {
                                            error  "ERROR: Cannot build images. Did not find ndm/images"
                                        }
                                   } // End of stage: Docker
                                        
                                    stage ('Deploy To Dev?') { 
                                        deployPrompt = promptDeployDev(message: 'Deploy into Dev')
                                        
                                        if (deployPrompt.didTimeout) {
                                            echo "no input was received before timeout"
                                        } else if (deployPrompt.userInput == true) {
                                            echo "this was successful"
                                            def ProdInstance = fileExists '/home/nimble/bin/moddel-docker-tag-push'
                                            
                                            if (ProdInstance) {
                                                println "Assessing if we have passed all Quality Gates and can publish Images"
                                                println "Build Status before publishing: " + currentBuild.result

                                                if (currentBuild.result == 'FAILURE') {
                                                    error  "One or more Quality Gates have failed. Unable to Publish"
                                                } else {
                                                    withCredentials([string(credentialsId: 'nimbolt', variable: 'PASSWORD')]) { 
                                                        GIT_SHORT_COMMIT = sh(returnStdout: true, script: "git log -n 1 --pretty=format:'%h'").trim()
                                                        sh("echo \${GIT_SHORT_COMMIT} > GIT_SHORT_COMMIT")
                                                        //sh(". \$WORKSPACE/ndm/deploy/cd-userparams_dev.config; moddel-docker-tag-push md-dev-\$AppVersion \${GIT_SHORT_COMMIT}")
                                                                 
				                                        sh ''' 
				                                            . \$WORKSPACE/ndm/deploy/cd-userparams_dev.config
				                                            moddel-docker-tag-push md-dev-\$AppVersion \${GIT_SHORT_COMMIT}                       
				                                        '''       
                                                        def images = []
                                                        sh (returnStdout: true, script: "cat ndm/images")
                                                        .trim()
                                                        .split('\\n').each { images << it }

                                                        images.each { image ->
                                                            image = image.split()[0]
                                                            println "uploading yaml files to the image: \${image}"
                                                            
                                                            withEnv(["DOCKER_IMAGE=\$image"]) {
                                                                rtUpload (
                                                                    serverId: 'moddel-art',
                                                                    spec:
                                                                        ''' {
                                                                            "files": 
                                                                            [
                                                                                    {
                                                                                    "pattern": "ndm/deploy/*.yml",
                                                                                    "target": "docker-moddel/train/\${DOCKER_IMAGE}/md-dev/",
                                                                                    "flat": "true"
                                                                                    }
                                                                            ]
                                                                        } '''
                                                                )
                                                            } // end of withEnv
                                                        } // end of images.each loop
                                                    } // end of withCredentials

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

                                                } // End of if currentBuild.result 
                                            }  // End of if ProdInstance
                                        } else {
                                            log.warning 'There was an issue with the deployment'
                                            currentBuild.result = 'FAILURE'
                                        } 

                                    } // End of stage: Deploy Dev?
                                } // end of feature flag: cdPipeline                      
                            } // end of node
                        } // end of timestamp				
                    """.stripIndent())
                } // end of cps
            } // end of definition
        } // end of pipelineJob
    } // end of dslFactory
} // end of class
