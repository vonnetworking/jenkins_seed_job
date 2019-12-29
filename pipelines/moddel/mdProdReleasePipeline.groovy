package moddel

import com.fhlmc.moddel.MDProdReleasePipelineBuilder

def slurper = new ConfigSlurper()
slurper.classLoader = this.class.classLoader
def config = slurper.parse(readFileFromWorkspace('pipelines/moddel/mdProducts.config'))

println "Preprod: Start"
// create job for every ndmprojects
config.mdproducts.each { mdname, data ->
    // set job description
    def jobdescription = "Modern Delivery Release Management Pipeline"

    // Flag to setup the Prod Release pipeline
    def prodReleasePipeline = (!data.prodReleasePipeline.isEmpty()) ? data.prodReleasePipeline : ''
println prodReleasePipeline
    // derive ldapGroup from the gitlab group e.g. ndmp_train_committer_nimble_gg
    def ldapGroup = "${data.productLdapGroup}".toLowerCase()
    def pipelineFolder = "${data.bitbucketprojectkey}".toLowerCase()
    def prodReleasePOLdapGroup = "${data.prodReleasePOLdapGroup}".toLowerCase()

    
    // notification sender and recipient
    def notificationSender = "${config.notificationSender}"
    def notificationRecipients = (!data.notificationRecipients.isEmpty()) ? data.notificationRecipients : notificationSender
    

    println "The folder ${data.bitbucketprojectkey} has the CD pipeline ${pipelineFolder}~${data.bitbucketprojectrepo}~prod-release"

    folder("${data.bitbucketprojectkey}") {
        displayName("${data.bitbucketprojectname}")
    }

    if (data.prodReleasePipeline == 'y') {
        new MDProdReleasePipelineBuilder(
                pipelineName: "${pipelineFolder}~${data.bitbucketprojectrepo}~prod-release",
                pipelineFolderName: "${data.bitbucketprojectkey}",
                BBProject: "${pipelineFolder}",
                BBGitRepo: "${data.bitbucketprojectrepo}",
                jobDescription: "${jobdescription}",
                ldapGroup: "${ldapGroup}",
                prodReleasePOLdapGroup: "${prodReleasePOLdapGroup}",
                prodReleasePipeline: "${prodReleasePipeline}",
              	notificationSender: "${notificationSender}",
                notificationRecipients: "${data.notificationRecipients}"
        ).build(this)
    }
}

println "RC: End"
