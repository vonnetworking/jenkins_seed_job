package moddel

import com.fhlmc.moddel.MDNightlyBuildPipelineBuilder

def slurper = new ConfigSlurper()
slurper.classLoader = this.class.classLoader
def config = slurper.parse(readFileFromWorkspace('pipelines/moddel/mdProducts.config'))

println "Nightly: Start"
// create job for every ndmprojects
config.mdproducts.each { mdname, data ->
    // set job description
    def jobdescription = "Modern Delivery -nightly build"

    // Flag to setup the Nightly pipeline
    def nightlyPipeline = (! data.nightlyPipeline.isEmpty()) ? data.nightlyPipeline : ''

    def envvar = (! data.envvar.isEmpty()) ? data.envvar : ''

    // gradle plugin properties
    def gradleVersion = (! data.gradleVersion.isEmpty()) ? data.gradleVersion : config.gradleVersionDefault
    def gradleroot = (! data.gradleroot.isEmpty()) ? data.gradleroot : config.gradleRootDefault
    def gradleTasks = (! data.gradleTasksNightly.isEmpty()) ? data.gradleTasksNightly : config.gradleTasksNightlyDefault
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
  	def buildTool = (!data.buildTool.isEmpty()) ? data.buildTool : ''
    def buildCommand = (!data.buildCommand.isEmpty()) ? data.buildCommand : config.buildCommandDefault

    // cobertura plugin properties
    def coberturaXml = (! data.coberturaXml.isEmpty()) ? data.coberturaXml : ''

    // derive ldapGroup from the gitlab group e.g. ndmp_train_committer_nimble_gg
    def ldapGroup = "${data.productLdapGroup}".toLowerCase()
    def pipelineFolder = "${data.bitbucketprojectkey}".toLowerCase()

    println "The folder ${data.bitbucketprojectkey} has the pipeline ${pipelineFolder}~${data.bitbucketprojectrepo}-release-candidate"

    folder("${data.bitbucketprojectkey}") {
        displayName("${data.bitbucketprojectname}")
    }
    if(data.nightlyPipeline == 'y') {
        new MDNightlyBuildPipelineBuilder(
                pipelineName: "${pipelineFolder}~${data.bitbucketprojectrepo}~nightly",
                pipelineFolderName: "${data.bitbucketprojectkey}",
          		BBProjectName: "${data.bitbucketprojectname}",
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
          		buildTool: "${buildTool}",
      			buildCommand: "${buildCommand}",
          		mvnjunitResults: "${mvnjunitResults}",
        		mvnjacocoClasses: "${mvnjacococlasses}",
        		mvnjacocoSource: "${mvnjacocosource}",
        ).build(this)
    }
}

println "Nightly: End"
