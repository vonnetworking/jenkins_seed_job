package moddel

import com.fhlmc.moddel.MDCDPipelineBuilder

def slurper = new ConfigSlurper()
slurper.classLoader = this.class.classLoader
def config = slurper.parse(readFileFromWorkspace('pipelines/moddel/mdProducts.config'))

println "Preprod: Start"
// create job for every ndmprojects
config.mdproducts.each { mdname, data ->
    // set job description
    def jobdescription = "Modern Delivery Continous Delivery Pipeline"

    // Flag to setup the CD pipeline
    def cdPipeline = (!data.rcPipeline.isEmpty()) ? data.rcPipeline : ''

    // derive ldapGroup from the gitlab group e.g. ndmp_train_committer_nimble_gg
    def ldapGroup = "${data.productLdapGroup}".toLowerCase()
    def pipelineFolder = "${data.bitbucketprojectkey}".toLowerCase()

    println "The folder ${data.bitbucketprojectkey} has the CD pipeline ${pipelineFolder}~${data.bitbucketprojectrepo}~deploy"

    folder("${data.bitbucketprojectkey}") {
        displayName("${data.bitbucketprojectname}")
    }

    if (data.cdPipeline == 'y') {
        new MDCDPipelineBuilder(
                pipelineName: "${pipelineFolder}~${data.bitbucketprojectrepo}~preprod",
                pipelineFolderName: "${data.bitbucketprojectkey}",
                BBProject: "${pipelineFolder}",
                BBGitRepo: "${data.bitbucketprojectrepo}",
                jobDescription: "${jobdescription}",
                ldapGroup: "${ldapGroup}",
        ).build(this)
    }
}

println "RC: End"
