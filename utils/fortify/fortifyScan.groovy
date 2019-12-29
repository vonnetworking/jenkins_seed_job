#!groovy
/**
 * fortifyScan
 * Author: F403930
 * Description: fortifyScan provides Fortify scan feature in Modern Delivery toolchain pipeline
 * Function:
 *    Running Fortify Scan with Java or Angular application in Modern Delivery toolchain pipeline
 *    Applying application specific or generic pipeline with the scan
 *    Generate .fpr file and pdf report
 *    Generate HTML report and display in Jenkins build summary - leftside bar (Fortify Scan Report)
 *    Make pipeline decision - fail or continue the pipeline based on Infosec criteria
 **/

def execute(String microServiceType){

  //Get Fortify scan project name
  microServiceName = "${JOB_NAME}"

  //check filter - if no filter detected in application repo, use generic filter.
  def fortifyFilterExist = fileExists 'infosec/'

  if(fortifyFilterExist)
  {
    echo "Application specific filter found, will apply it with Fortify scan."
  }
  else{
    sh'''
				 echo "Application specific filter not found, will use generic one."
				 mkdir infosec
				 wget https://moddel-tw.fhlmc.com:8046/projects/TECHPILLAR/repos/toolchain-infosec/raw/toolchain-infosec/fortify/filter/default/fortify_generic_filter.txt -P infosec/
				 cd infosec
				 ls -lrt
				 cd ../
				 ls -lrt
				 '''
  }

  //SCA runs fortify scan
  if(microServiceType.equals("npm"))
  {
    echo "Fortify scan for Angular microservice will start.."
    fortifyAngularScan(microServiceName)
  }
  else
  {
    echo "Fortify scan for Java microservice will start.."
    fortifyJavaScan(microServiceName)
  }

  // attach pdf report
  def artifactsExist = fileExists 'fortifyScan.pdf'

  if(artifactsExist)
  {
    sh "mv fortifyScan.pdf fortifyScan_${BUILD_NUMBER}.pdf"
    archiveArtifacts artifacts: 'fortifyScan_*.pdf'
  }

  // get criteria data
  (crossSiteScriptingCount, sqlInjectionCount, pathManipulationCount, commandInjectionCount) = getFortifyIssueByCategory()

  println crossSiteScriptingCount
  println sqlInjectionCount
  println pathManipulationCount
  println commandInjectionCount

  //create html report
  createFortifyReport(crossSiteScriptingCount, sqlInjectionCount, pathManipulationCount, commandInjectionCount)
}

def fortifyJavaScan(def microServiceName){

    //This is just for DryRun test purpose. It includes below variable and if-else condition.  Will be removed when pushed into Prod
    def dryRunEnvControl = "prod"
    //def dryRunEnvControl = "non-prod"

    if(dryRunEnvControl.equals("non-prod")) {

        sh '''
                #!/bin/bash
                set +e
                set init
                echo "Running NP InfoSec Scan - JS"

                # Jenkins Dev and devbox
               /opt/fortify/bin/sourceanalyzer -b ''' + microServiceName + ''' -clean
               /opt/fortify/bin/sourceanalyzer -b ''' + microServiceName + ''' -gradle -verbose -debug gradle clean build            
               /opt/fortify/bin/sourceanalyzer -b ''' + microServiceName + ''' -filter infosec/*.* -verbose -scan -f fortifyScan.fpr
               /opt/fortify/bin/ReportGenerator -format pdf -f fortifyScan.pdf -source fortifyScan.fpr
               /opt/fortify/bin/ReportGenerator -format xml -f fortifyScan.xml -source fortifyScan.fpr
               /opt/fortify/bin/FPRUtility -information -categoryIssueCounts -project fortifyScan.fpr
               /opt/fortify/bin/FPRUtility -information -categoryIssueCounts -project fortifyScan.fpr -f fortify-issue-category-output.txt
                
             '''
    }
    else {
 //       println System.getenv('PATH');
        sh '''
                #!/bin/bash
                set +e
                echo "Running InfoSec Scan - JS"
                
                export PATH=$PATH:/opt/fortify/bin
                echo $PATH
                
                # Jenkins PROD
               sourceanalyzer -b ''' + microServiceName + ''' -clean
               sourceanalyzer -b ''' + microServiceName + ''' -gradle -verbose gradle clean build               
               sourceanalyzer -b ''' + microServiceName + ''' -filter infosec/*.* -verbose -scan -f fortifyScan.fpr
               /opt/fortify/bin/ReportGenerator -format pdf -f fortifyScan.pdf -source fortifyScan.fpr
               /opt/fortify/bin/ReportGenerator -format xml -f fortifyScan.xml -source fortifyScan.fpr
               /opt/fortify/bin/FPRUtility -information -categoryIssueCounts -project fortifyScan.fpr
               /opt/fortify/bin/FPRUtility -information -categoryIssueCounts -project fortifyScan.fpr -f fortify-issue-category-output.txt

             '''
    }
}


def fortifyAngularScan(def microServiceName){

    //This is just for DryRun test purpose. It includes below variable and if-else condition.  Will be removed when pushed into Prod
    def dryRunEnvControl = "prod"
    //def dryRunEnvControl = "non-prod"

    if(dryRunEnvControl.equals("non-prod")) {

         sh '''
                #!/bin/bash
                set +e
                echo "Running NP InfoSec Scan - AS"

               # Jenkins Dev and devbox
               /opt/fortify/bin/sourceanalyzer -b ''' + microServiceName + ''' -clean
               /opt/fortify/bin/sourceanalyzer -b ''' + microServiceName + ''' -gradle -verbose gradle clean build
               /opt/fortify/bin/sourceanalyzer -b ''' + microServiceName + ''' **/*.ts **/*.tsx **/*.js -Dcom.fortify.sca.EnableDOMModeling=true -Dcom.fortify.sca.hoa.Enable=true
               /opt/fortify/bin/sourceanalyzer -Xmx1500M -Xms400M -Xss24M -b ''' + microServiceName + ''' -filter infosec/*.* -verbose -scan -f fortifyScan.fpr
               /opt/fortify/bin/ReportGenerator -format pdf -f fortifyScan.pdf -source fortifyScan.fpr
               /opt/fortify/bin/ReportGenerator -format xml -f fortifyScan.xml -source fortifyScan.fpr
               /opt/fortify/bin/FPRUtility -information -categoryIssueCounts -project fortifyScan.fpr
               /opt/fortify/bin/FPRUtility -information -categoryIssueCounts -project fortifyScan.fpr -f fortify-issue-category-output.txt
 '''
    }
    else {
        sh '''
                #!/bin/bash
                set +e
                export PATH=$PATH:/opt/fortify/bin
                echo $PATH

                echo "Running InfoSec Scan - AS"
               # Jenkins Prod
               ~/fortify/bin/sourceanalyzer -b ''' + microServiceName + ''' -clean
               ~/fortify/bin/sourceanalyzer -b ''' + microServiceName + ''' gradle clean build
               ~/fortify/bin/sourceanalyzer -b ''' + microServiceName + ''' **/*.ts **/*.tsx **/*.js -Dcom.fortify.sca.EnableDOMModeling=true -Dcom.fortify.sca.hoa.Enable=true
               ~/fortify/bin/sourceanalyzer -Xmx1500M -Xms400M -Xss24M -b ''' + microServiceName + ''' -filter infosec/*.* -scan -f fortifyScan.fpr
               ~/fortify/bin/ReportGenerator -format pdf -f fortifyScan.pdf -source fortifyScan.fpr
               ~/fortify/bin/ReportGenerator -format xml -f fortifyScan.xml -source fortifyScan.fpr
               ~/fortify/bin/FPRUtility -information -categoryIssueCounts -project fortifyScan.fpr
               ~/fortify/bin/FPRUtility -information -categoryIssueCounts -project fortifyScan.fpr -f fortify-issue-category-output.txt

             '''
    }
}

def getFortifyIssueByCategory(){

  //define issue count
  def crossSiteScriptingCount = 0
  def sqlInjectionCount = 0
  def pathManipulationCount = 0
  def commandInjectionCount = 0

  categoryOutput = readFile "fortify-issue-category-output.txt"
  echo "${categoryOutput}"

  def categoryList = categoryOutput.split('\n')

  for(int i = 0; i < categoryList.size(); i++ )
  {
    def line = categoryList[i]
    println line

    if(line.contains("Cross-Site Scripting"))
    {
      crossSiteScriptingCount = crossSiteScriptingCount + line.findAll( /\d+/ )[0].toInteger()
      echo "Cross-Site Scripting issue count: ${crossSiteScriptingCount}"
    }
    // detect SQL Injection and remove NoSQL Injection: MongoDB
    if(line.contains("SQL Injection")&&!line.contains("NoSQL"))
    {
      sqlInjectionCount = sqlInjectionCount + line.findAll( /\d+/ )[0].toInteger()
      echo "SQL Injection issue count: ${sqlInjectionCount}"
    }

    if(line.contains("Path Manipulation"))
    {
      pathManipulationCount = pathManipulationCount + line.findAll( /\d+/ )[0].toInteger()
      echo "Path Manipulation issue count: ${pathManipulationCount}"
    }

    if(line.contains("Command Injection"))
    {
      commandInjectionCount = commandInjectionCount + line.findAll( /\d+/ )[0].toInteger()
      echo "Command Injection issue count: ${commandInjectionCount}"
    }

  }

  return [crossSiteScriptingCount.toString(), sqlInjectionCount.toString(), pathManipulationCount.toString(), commandInjectionCount.toString()]
}

def createFortifyReport(def crossSiteScriptingCount, def sqlInjectionCount, def pathManipulationCount, def commandInjectionCount)
{
  sh'''
  wget https://moddel-tw.fhlmc.com:8046/projects/TECHPILLAR/repos/jenkinsdsl/raw/utils/fortify/fortify_report_category_template.html?at=refs%2Fheads%2FTECHPILLAR-2130-V1-Integration-DryRun -O fortify_report_category_template.html
 '''
  String fortifyReportData = readFile encoding: 'UTF-8', file: "fortify_report_category_template.html"
  String status
  boolean result

  //replacing tempalte issue count by priority
  fortifyReportData = replaceFortifyReportTemplateData("<%=crossSiteScriptingCount%>", crossSiteScriptingCount, fortifyReportData)
  fortifyReportData = replaceFortifyReportTemplateData("<%=sqlInjectionCount%>", sqlInjectionCount, fortifyReportData)
  fortifyReportData = replaceFortifyReportTemplateData("<%=pathManipulationCount%>", pathManipulationCount, fortifyReportData)
  fortifyReportData = replaceFortifyReportTemplateData("<%=commandInjectionCount%>", commandInjectionCount, fortifyReportData)

  // Fortify standard - No issue in infosec categories
  if(crossSiteScriptingCount.equals("0") && sqlInjectionCount.equals("0") && pathManipulationCount.equals("0") && commandInjectionCount.equals("0")){
    status = "Passed"
    result = false
  }

  else{
    status = "Failed"
    result = true
  }

  //replacing tempalte scan status
  fortifyReportData = replaceFortifyReportTemplateData("<%=scanResult%>", status, fortifyReportData)

  def workspace = pwd()
  echo "${workspace}"

  steps.echo "${fortifyReportData}"
  steps.writeFile file: "${workspace}/fortify_status_${BUILD_NUMBER}.html", text: "${fortifyReportData}"

    // TO-DO -- Commenting publishHTML step for now as this requires further analysis --
/*  steps.publishHTML(target: [
    allowMissing: false,
    alwaysLinkToLastBuild: true,
    keepAll: true,
    reportDir: "${workspace}",
    reportFiles: "fortify_status_${BUILD_NUMBER}.html",
    reportName: "Fortify Scan Report"])
*/
  if(result){
    error("Security risk found in InfoSec defined issue categories. Please review fortify pdf report and have the fix accordingly")
  }
}


def replaceFortifyReportTemplateData(String param, String paramValue, String fortifyReportData) {
  if(!paramValue?.trim()){
    paramValue = "N/A"
  }
  fortifyReportData = fortifyReportData.replace(param, paramValue)
  return fortifyReportData
}

return this