@Grab('org.yaml:snakeyaml:1.17')
import org.yaml.snakeyaml.Yaml

import BranchPipelineGen

def config = new Yaml().load(("${WORKSPACE}/config.yaml" as File).text)

config.each { jobname, data ->
  println "Building Branches Job " + jobname + " Using data: " + data
  def git_url = data.git_url
  def project_type = data.type.trim()
  if( project_type == "mdmpl" ) {
    new BranchPipelineGenMDMPL(
      name: jobname,
      git_url: git_url).build(this)
    println "BRANCHES: End"
  }else{
    new BranchPipelineGen(
      name: jobname,
      git_url: git_url).build(this)
    println "BRANCHES: End"
  }
} //end each block
