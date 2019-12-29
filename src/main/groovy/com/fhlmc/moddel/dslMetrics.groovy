package com.fhlmc.moddel

def config = new ConfigSlurper().parse(new File('../../../../../../pipelines/moddel/mdProducts.config').toURL())

println 'id,bitbucketprojectkey,bitbucketprojectrepo,branches,RC,CD'
id = 1
config.mdproducts.each { name, data ->
    def rcPipeline = (! data.rcPipeline.isEmpty()) ? data.rcPipeline : 'n'
    def cdPipeline = (! data.cdPipeline.isEmpty()) ? data.cdPipeline : 'n'

    println "$id,$data.bitbucketprojectkey,$data.bitbucketprojectrepo,y,$rcPipeline, $cdPipeline"
    id++
}