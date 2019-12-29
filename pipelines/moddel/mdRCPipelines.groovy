package moddel

import com.fhlmc.moddel.MDReleaseCandidatePipelineBuilder

def slurper = new ConfigSlurper()
slurper.classLoader = this.class.classLoader
def config = slurper.parse(readFileFromWorkspace('pipelines/moddel/mdProducts.config'))

println "RC: Start"
// create job for every ndmprojects
config.mdproducts.each { mdname, data ->
    // set job description
    def jobdescription = "Modern Delivery -release candidate build"

    // Flag to setup the Release Candidate pipeline
    def rcPipeline = (!data.rcPipeline.isEmpty()) ? data.rcPipeline : ''

    def envvar = (!data.envvar.isEmpty()) ? data.envvar : ''

    // gradle plugin properties
    def gradleVersion = (!data.gradleVersion.isEmpty()) ? data.gradleVersion : config.gradleVersionDefault
    def gradleroot = (!data.gradleroot.isEmpty()) ? data.gradleroot : config.gradleRootDefault
    def gradleTasks = (!data.gradleTasksRC.isEmpty()) ? data.gradleTasksRC : config.gradleTasksRCDefault
    def gradleswitches = (data.gradleswitches.isEmpty()) ? config.gradleSwitchesDefault : ''

    // junit results folder
    def junitResults = (! data.junitResults.isEmpty()) ? data.junitResults : config.junitResultsDefault
    def mvnjunitResults = (! data.mvnjunitResults.isEmpty()) ? data.mvnjunitResults : config.mvnjunitResultsDefault

    // jacoco plugin properties
    def coverageResults = (! data.coverageResults.isEmpty()) ? data.coverageResults : ''
    def jacocosource = (! data.jacocosource.isEmpty()) ? data.jacocosource : ''
    def jacococlasses = (! data.jacococlasses.isEmpty()) ? data.jacococlasses : config.jacocoClassesDefault
    def jacocoexcludeclasses = (! data.jacocoexcludeclasses.isEmpty()) ? data.jacocoexcludeclasses : config.jacocoExcludeClassesDefault
    def jacocominlines = (! data.jacocominlines.isEmpty()) ? data.jacocominlines : config.jacocoMinlinesDefault
    def jacocomaxlines = (! data.jacocomaxlines.isEmpty()) ? data.jacocomaxlines : config.jacocoMaxlinesDefault
  
    def mvnjacococlasses = (! data.mvnjacococlasses.isEmpty()) ? data.mvnjacococlasses : config.mvnjacocoClassesDefault
    def mvnjacocosource = (! data.mvnjacocosource.isEmpty()) ? data.mvnjacocosource : ''

    def defaultSonarProjectKey = "${data.bitbucketprojectkey}".toUpperCase() + ":${data.bitbucketprojectrepo}"
    def defaultSonarProjectName = "${data.bitbucketprojectkey}".toUpperCase() + ":${data.bitbucketprojectrepo}"
    def defaultSonarSrc = "${config.defaultSonarSrc}"
    def defaultSonarJavaBinaries = "${config.defaultSonarJavaBinaries}"

  	// build tool support
  	def buildTool = (!data.buildTool.isEmpty()) ? data.buildTool : ''
  	def buildCommand = (!data.buildCommand.isEmpty()) ? data.buildCommand : config.buildCommandDefault

    // cobertura plugin properties
    def coberturaXml = (!data.coberturaXml.isEmpty()) ? data.coberturaXml : ''

    // derive ldapGroup from the gitlab group e.g. ndmp_train_committer_nimble_gg
    def ldapGroup = "${data.productLdapGroup}".toLowerCase()
    def pipelineFolder = "${data.bitbucketprojectkey}".toLowerCase()
    def cdPipeline = (! data.cdPipeline.isEmpty()) ? data.cdPipeline : 'n'

    // notification sender and recipient
    def notificationSender = "${config.notificationSender}"
    def notificationRecipients = (!data.notificationRecipients.isEmpty()) ? data.notificationRecipients : notificationSender
    
  
    println "The notification recipients: ${data.notificationRecipients} for the pipeline ${pipelineFolder}~${data.bitbucketprojectrepo}-release-candidate"
    println "The folder ${data.bitbucketprojectkey} has the pipeline ${pipelineFolder}~${data.bitbucketprojectrepo}-release-candidate"

    folder("${data.bitbucketprojectkey}") {
        displayName("${data.bitbucketprojectname}")
    }
    if (data.rcPipeline == 'y') {
        new MDReleaseCandidatePipelineBuilder(
                pipelineName: "${pipelineFolder}~${data.bitbucketprojectrepo}~release-candidate",
                pipelineFolderName: "${data.bitbucketprojectkey}",
                BBProject: "${pipelineFolder}",
                BBGitRepo: "${data.bitbucketprojectrepo}",
                jobDescription: "${jobdescription}",
                ldapGroup: "${ldapGroup}",
                envVar: "${envvar}",
                gradleBuildScriptDir: "${gradleroot}",
                gradleVersion: "${gradleVersion}",
                gradleTasks: "${gradleTasks}",
                gradleSwitches: "${gradleswitches}",
                junitResults: "${junitResults}",
                coverageResults: "${coverageResults}",
                jacocoClasses: "${jacococlasses}",
                jacocoSource: "${jacocosource}",
                jacocoExcludeClasses: "${jacocoexcludeclasses}",
                jacocoMinLines: "${jacocominlines}",
                jacocoMaxLines: "${jacocomaxlines}",
                coberturaXml: "${coberturaXml}",
                defaultSonarProjectKey: "${defaultSonarProjectKey}",
                defaultSonarProjectName: "${defaultSonarProjectName}",
                defaultSonarSrc: "${defaultSonarSrc}",
                defaultSonarJavaBinaries: "${defaultSonarJavaBinaries}",
                cdPipeline: "${cdPipeline}",
          		buildTool: "${buildTool}",
          		buildCommand: "${buildCommand}",
          		mvnjunitResults: "${mvnjunitResults}",
       			mvnjacocoClasses: "${mvnjacococlasses}",
        		mvnjacocoSource: "${mvnjacocosource}",
                notificationSender: "${notificationSender}",
                notificationRecipients: "${data.notificationRecipients}"
        ).build(this)
    }
}

println "RC: End"
