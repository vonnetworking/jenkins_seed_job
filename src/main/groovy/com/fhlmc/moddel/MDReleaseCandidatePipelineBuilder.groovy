package com.fhlmc.moddel

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

/**
 * Class for creating an Moddel Release Candidate Job
 */
class MDReleaseCandidatePipelineBuilder {

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
    String cdPipeline
  	String buildTool
    String buildCommand
    String notificationSender
    String notificationRecipients
  	

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
            def fortifyScan = "md-fortify-scan-${BBGitRepo}"
            def cdPipeline = "\'${cdPipeline}\'"
            def spinnnakerAppName = "${BBProject}${BBGitRepo}".replaceAll("[^a-zA-Z0-9]", "")
            def emailRecipients ="${notificationRecipients}"
            def emailSender ="${notificationSender}"
            
          	if(buildTool == ''){buildTool = 'genericBuild'}
          
            definition {
                cps {
                    sandbox()

                    script("""
                @Library('mdjsl') _
                import groovy.json.JsonSlurperClassic

                timestamps {
                    node('moddel') {
                                stage ('Clone') {
                                    checkout([\$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [ [\$class: 'LocalBranch'], [\$class: 'WipeWorkspace'], [\$class: 'DisableRemotePoll'], [\$class: 'BuildChooserSetting',buildChooser: [\$class: 'DefaultBuildChooser']]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'bitbucket-clone', url: \' $gitURL \']]]) 
                                } // End of stage: Clone
                                
                                   stage ('Build') {
                                	buildComnand = \'${buildCommand}\'
                                	if ('$buildTool' == 'maven') {
                                    	if (fileExists ('pom.xml')) {
                                    		sh "mvn clean $buildCommand"
                                    	}
                                    	else {
                                    		sh "find . -name 'pom.xml' -exec mvn clean $buildCommand -f '{}' \\';'"
                                    	}
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
                        /*    if('$buildTool' == 'npm' && !propertiesSet){ echo 'sonar-project.properties is required for NPM builds!' }
                            if (propertiesSet) {
                                echo 'Using sonar-project.properties file -- default sonar.sources and sonar.java.binaries will NOT be used'
                                sh "/opt/sonar-scanner/bin/sonar-scanner -Dsonar.host.url=\$SONAR_HOST -Dsonar.projectKey='${defaultSonarProjectKey}' -Dsonar.projectName='${defaultSonarProjectName}'"
                            } else {
                                sh "/opt/sonar-scanner/bin/sonar-scanner -Dsonar.host.url=\$SONAR_HOST -Dsonar.projectKey='${defaultSonarProjectKey}' -Dsonar.projectName='${defaultSonarProjectName}' -Dsonar.sources='${defaultSonarSrc}' -Dsonar.java.binaries='${defaultSonarJavaBinaries}' -Dsonar.jacoco.reportPaths='./build/jacoco/test.exec'"
                            }
                            sleep 10
                            sh "curl -X GET -H 'Accept: application/json' \$SONAR_HOST/api/qualitygates/project_status?projectKey=${defaultSonarProjectKey} > status.json"
                            def json = readFile("status.json")
                            def data = new JsonSlurperClassic().parseText(json)
                            echo data.toString()
                            if (!"OK".equals(data["projectStatus"]["status"])) {
                                error  "Quality Gate failure"
                            } */
                        }
                        
                        stage ('InfoSec Scan') {

                        buildTool = \'${buildTool}\'
                        sh "wget https://moddel-tw.fhlmc.com:8046/projects/TECHPILLAR/repos/jenkinsdsl/raw/utils/fortify/fortifyScan.groovy -O fortifyScan.groovy"
                        fortifyScanUtil = load 'fortifyScan.groovy'
                        fortifyScanUtil.execute(buildTool)
                             
                        }

                       stage ('Docker') {
                            def buildDockerImages = fileExists 'ndm/images'
                            if (buildDockerImages) {
                                echo 'Found ndm/images. Commencing build-images.'
                            
                                sh '''
                                build-images
                                '''
                            } else {error  "ERROR: Cannot build images. Did not find ndm/images"}

                            sh '''
                            stop-rm-containers
                            run-containers
                            '''
                        }

                        stage ('Test RC') { 
                            def TestRCTool = fileExists 'FitNesseRoot'
                            if (TestRCTool) {
                                echo 'Running Fitnesse Tests'
    
                                sh ''' 
                                sleep 10 # Give a little time for containers to come up
                                run-fitnesse ./ 8501
                                ''' 
    
                                step([\$class: 'FitnesseResultsRecorder', fitnessePathToXmlResultsIn: 'fitnesse-report.xml'])

                            } else {
                                echo 'Running Cucumber Tests'

                                 sh ''' 
                                 sleep 10 # Give a little time for containers to come up
                                 gradle intTest
                                 ''' 
                                /*
                                This plugin requires that you use cucumber library to generate a json report. 
                                The plugin uses the json report to produce html reports that are available from jenkins on the build page after a build has run.
                                
                                https://wiki.jenkins.io/display/JENKINS/Cucumber+Reports+Plugin
                                https://jenkins.io/doc/pipeline/steps/cucumber-reports/
                                */
                                cucumber failedFeaturesNumber: -1, failedScenariosNumber: -1, failedStepsNumber: -1, fileIncludePattern: '*.json', trendsLimit: 10, buildStatus: 'FAILURE', jsonReportDirectory: 'build', pendingStepsNumber: -1, skippedStepsNumber: -1, sortingMethod: 'ALPHABETICAL', undefinedStepsNumber: -1
                            }
                            
                                println "Assessing results of the Integration tests"
                                println "Current Build Status: " + currentBuild.result
                                
                                if (currentBuild.result == 'FAILURE') {
                                    error  "ERROR: Found Issues during the Integration Tests"
                                }
                            
                            // Always stop running containers
                            sh ''' 
                            #!/bin/bash
                            stop-rm-containers
                            '''
                          
                        } // End of stage: Test RC
                        
                        stage ('Publish') { 
                            def ProdInstance = fileExists '/home/nimble/bin/moddel-docker-tag-push'
                            if (ProdInstance) {
                                println "Assessing if we have passed all Quality Gates and can publish Images"
                                println "Build Status before publishing: " + currentBuild.result
                            
                                if (currentBuild.result == 'FAILURE') {
                                    error  "One or more Quality Gates have failed. Unable to Publish"
                                } else {
                                    withCredentials([string(credentialsId: 'nimbolt', variable: 'PASSWORD')]) {
                                    
                                    GIT_SHORT_COMMIT = sh(returnStdout: true, script: "git log -n 1 --pretty=format:'%h'").trim()
                                    //echo in jenkins console
                                    echo GIT_SHORT_COMMIT
                                    //wanted to send these info to build artifacts, append to any file
                                    sh("echo \${GIT_SHORT_COMMIT} > \${GIT_SHORT_COMMIT}")
        
                                        sh ''' 
                                            . \${WORKSPACE}/ndm/deploy/cd-userparams_rc.config
                                            moddel-docker-tag-push md-rc-\${AppVersion} \${GIT_SHORT_COMMIT}
                                        '''       
                                        
                                        def images = []
                                        sh (returnStdout: true, script: "cat ndm/images")
                                        .trim()
                                        .split('\\n').each { images << it }

                                        images.each { image ->
                                            image = image.split()[0]
                                            println "uploading yaml files to the image: \${image}"                                        
                                            withEnv(["DOCKER_IMAGE=\${image}"]) {
                                                rtUpload (
                                                    serverId: 'moddel-art',
                                                    spec:
                                                        ''' {
                                                            "files": [
                                                                {
                                                                    "pattern": "ndm/deploy/*.yml",
                                                                    "target": "docker-moddel/train/\${DOCKER_IMAGE}/md-rc/",
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
                                   ./SpinnakerDeploy.sh \${WORKSPACE}/ndm/deploy/cd-userparams_rc.config SystemParams.config Confidential
                                   '''
                                                               
                                } // End of if currentBuild.result
                            }  // End of if ProdInstance
                        } // End of stage: Publish

                           // after manual step to proceed add deploy to PP
        stage ('Publish for PreProd') {
            // Implement user prompt to publish artifact for preprod deployment
            def userInput = true
            def didTimeout = false

            try {
                timeout(time: 14400, unit: 'SECONDS') { // hold request for user input for 4 hours
                    userInput = input(
                            id: 'Proceed1', message: 'By clicking the proceed button you are attesting the code is ready for production release.', parameters: [
                            [\$class: 'BooleanParameterDefinition', defaultValue: true, description: '', name: 'Please confirm you want to publish for PreProd. NOTE: No production release occurs as part of this approval.']
                    ])
                }
            } catch(err) { // timeout reached or input false (abort)

                def user = err.getCauses()[0].getUser()
                if('SYSTEM' == user.toString()) { // SYSTEM means timeout.
                    didTimeout = true
                } else {
                    userInput = false
                    echo "Aborted by: [\${user}]"
                }
            } // end try catch

            if (didTimeout) { // if timeout is reached 4 hours
                // on time out end build
                echo "no input was received before timeout, PreProd artifact not published"
            } else if (userInput == true) { // if user proceeds with timeout
                // Prepare and publish build artifact
                echo "Publish image and yml for preprod"
                def ProdInstance = fileExists '/home/nimble/bin/moddel-docker-tag-push'
                if (ProdInstance) {
                    println "Assessing if we have passed all Quality Gates and can publish Images"
                    println "Build Status before publishing: " + currentBuild.result

                    if (currentBuild.result == 'FAILURE') {
                        error "One or more Quality Gates have failed. Unable to Publish"
                    } else {
                        withCredentials([string(credentialsId: 'nimbolt', variable: 'PASSWORD')]) {

                            GIT_SHORT_COMMIT = sh(returnStdout: true, script: "git log -n 1 --pretty=format:'%h'").trim()
                            //echo in jenkins console

                            //wanted to send these info to build artifacts, append to any file
                            sh("echo \${GIT_SHORT_COMMIT} > \${GIT_SHORT_COMMIT}")

                            sh '''
                        \t    . \${WORKSPACE}/ndm/deploy/cd-userparams_pp.config
                                moddel-docker-tag-push md-pp-\${AppVersion} \${GIT_SHORT_COMMIT}
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
                                                "target": "docker-moddel/train/\${DOCKER_IMAGE}/md-pp/",
                                                "flat": "true"
                                            }
                                            ]
                                        } '''
                                    ) // end rtUpload
                                } // end of withEnv
                            } // end of images.each loop
                        } // end of withCredentials
                    } // end else if Build.result
                } // end if prodInstance
            } else {
                // End build
                echo "PreProd artifact not published"
                echo currentBuild.result
            } // end else user input
        }// end of preprod publish stage

        stage(" Notify")
        {
            echo "Stage Notify"
               emailext (
                    subject: "Status of pipeline: \${currentBuild.fullDisplayName}",
                    body: "Your Production Release : \${env.BUILD_URL} has completed with status : \${currentBuild.currentResult}",
                    to: \'$notificationRecipients\',
                    from: \'$notificationSender\'
            )
        }  // end stage Notify
                                  
                    }   // End of node
                } 	    // End of timestamps			
            """.stripIndent())
                }
            }
        }
    }
}