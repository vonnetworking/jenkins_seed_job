import com.moddel.lib.BranchPipelineGen

def config = new Yaml().load(("${WORKSPACE}/config.yaml" as File).text)

config.each { jobname, data ->
  println "Building Branches Job " + jobname + " Using data: " + data
  def git_url = data.git_url
  new genPipeline(
    name: jobname,
    git_url: git_url).build(this)
  println "BRANCHES: End"
} //end each block
