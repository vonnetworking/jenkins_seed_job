package com.fhlmc.moddel

import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

/**
 *Class for creating an Moddel Nightly Job
 */
class MDNightlyBuildPipelineBuilder {

    String pipelineName
    String pipelineFolderName
    String BBProjectName
  	String jobDescription
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
					pollSCM {
							scmpoll_spec('H/10 1-6 * * *')
							// Ignore changes notified by SCM post-commit hooks.
							ignorePostCommitHooks(true)
							}
            }

            def gitURL = "https://moddel-tw.fhlmc.com:8046/scm/${BBProject}/${BBGitRepo}.git"
            def fortifyScan = "md-fortify-scan-${BBGitRepo}"
          	if(buildTool == ''){buildTool = 'genericBuild'}

            definition {
                cps{
                    sandbox()

                    script("""
                timestamps {
                   node('moddel') {
                                stage ('Clone') {
                                    checkout([\$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [ [\$class: 'LocalBranch'], [\$class: 'WipeWorkspace'], [\$class: 'DisableRemotePoll'], [\$class: 'BuildChooserSetting',buildChooser: [\$class: 'InverseBuildChooser']]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'bitbucket-clone', url: \' $gitURL \']]]) 
                                } // End of stage: Clone
                                
                                 stage ('Build') {
                                	buildCommand = \'${buildCommand}\'
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
                            if (propertiesSet) {
                                echo 'Using sonar-project.properties file -- default sonar.sources and sonar.java.binaries will NOT be used'
                                sh "/opt/sonar-scanner/bin/sonar-scanner -Dsonar.host.url=\$SONAR_HOST -Dsonar.projectKey='${defaultSonarProjectKey}' -Dsonar.projectName='${defaultSonarProjectName}'"
                            } else {
                                sh "/opt/sonar-scanner/bin/sonar-scanner -Dsonar.host.url=\$SONAR_HOST -Dsonar.projectKey='${defaultSonarProjectKey}' -Dsonar.projectName='${defaultSonarProjectName}' -Dsonar.sources='${defaultSonarSrc}' -Dsonar.java.binaries='${defaultSonarJavaBinaries}' -Dsonar.jacoco.reportPaths='./build/jacoco/test.exec'"
                            }
                        }

                        stage ('InfoSec Scan') {
                        	buildTool = \'${buildTool}\'
                            if (buildTool == 'npm'){
                            
                                 sh ''' 
                                #!/bin/bash
                                set +e
                                ls -l infosec/
                                echo "Running InfoSec Scan"
                                ~/fortify/bin/sourceanalyzer -b $fortifyScan -clean
                                ~/fortify/bin/sourceanalyzer -b $fortifyScan /**/*.ts /**/*.js /**/*.tsx
                                ~/fortify/bin/sourceanalyzer -b $fortifyScan -filter infosec/fortify.filter -scan -f fortifyScan.fpr
                                ~/fortify/bin/ReportGenerator -format pdf -f fortifyScan.pdf -source fortifyScan.fpr
                                ~/fortify/bin/FPRUtility -information -categoryIssueCounts -project fortifyScan.fpr
                                ~/fortify/bin/FPRUtility -information -categoryIssueCounts -project fortifyScan.fpr -f fortify-results.txt
                                egrep -c 'Cross-Site Scripting|Path Manipulation|SQL Injection|critical issues' ./fortify-results.txt
                                if [ \\\$? == 0 ]; then echo "Critical issues found in the Fortify Scan." && exit 1; else echo "No issues found in the Fortify Scan."; fi
                                 '''
                                def artifactsExist = fileExists 'fortifyScan_build.pdf'
                                if(artifactsExist)
                                {
                                	archiveArtifacts artifacts: 'fortifyScan_build.pdf'
                                }
                             } else
                             {
                                                          sh ''' 
                                #!/bin/bash
                                set +e
                                ls -l infosec/
                                echo "Running InfoSec Scan"
                                ~/fortify/bin/sourceanalyzer -b $fortifyScan -clean
                                ~/fortify/bin/sourceanalyzer -b $fortifyScan -source 1.8 -java-build-dir "build/classes/java/**/" "src/main/**/*"
                                ~/fortify/bin/sourceanalyzer -b $fortifyScan -filter infosec/fortify.filter -scan -f fortifyScan.fpr
                                ~/fortify/bin/ReportGenerator -format pdf -f fortifyScan.pdf -source fortifyScan.fpr
                                ~/fortify/bin/FPRUtility -information -categoryIssueCounts -project fortifyScan.fpr
                                ~/fortify/bin/FPRUtility -information -categoryIssueCounts -project fortifyScan.fpr -f fortify-results.txt
                                egrep -c 'Cross-Site Scripting|Path Manipulation|SQL Injection|critical issues' ./fortify-results.txt
                                if [ \\\$? == 0 ]; then echo "Critical issues found in the Fortify Scan." && exit 1; else echo "No issues found in the Fortify Scan."; fi
                                 '''
                                def artifactsExist = fileExists 'fortifyScan_build.pdf'
                                if(artifactsExist)
                                {
                                	archiveArtifacts artifacts: 'fortifyScan_build.pdf'
                                }
                                
                             }
                             
                        }
                    }
                }				
            """.stripIndent())
                }
            }
        }
    }
}
