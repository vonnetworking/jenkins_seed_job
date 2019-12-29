import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job

/* def slurper = new ConfigSlurper()
slurper.classLoader = this.class.classLoader
def config = slurper.parse(readFileFromWorkspace('pipelines/moddel/mdProducts.config'))
*/
def config = readYaml file:"${env.WORKSPACE}} + "/config.yaml"

config.each { jobname, data ->
  println "Job: " + jobname
  println "data: " + data
} //end each block

println "BRANCHES: End"
