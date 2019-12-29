import javaposse.jobdsl.dsl.DslFactory
import javaposse.jobdsl.dsl.Job
@Grab('org.yaml:snakeyaml:1.17')
import org.yaml.snakeyaml.Yaml

/* def slurper = new ConfigSlurper()
slurper.classLoader = this.class.classLoader
def config = slurper.parse(readFileFromWorkspace('pipelines/moddel/mdProducts.config'))
*/

def config = new Yaml().load(("${WORKSPACE}/config.yaml" as File).text)

config.each { jobname, data ->
  println "Job: " + jobname
  println "data: " + data
} //end each block

println "BRANCHES: End"
