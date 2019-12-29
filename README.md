Jenkins-DSL
============

Overview
------------

The Jenkins "Job DSL / Plugin" is comprised of two parts. The DSL and the Jenkins plugin. DSL stands for Domain Specific Language. It uses Groovy and allows users to describe a job by a script. The DSL allows the definition of a job, and then offers a useful set of functions to configure common Jenkins items. The other part, Jenkins plugin, manages the scripts and the updating of the Jenkins jobs which are created and maintained as a result.

This git repositiory stores Jenkins DSL oto create jobs for Modern Delivery projects

Features
------------

  1. DSL - Scriptable via Groovy
  2. DSL - Direct control of XML, so that anything possible in a config.xml is possible via the DSL
  3. DSL - Helper methods for common job configurations, e.g. scm, triggers, build steps
  4. Plugin - DSL can be put directly in a job
  5. Plugin - DSL can be put into SCM and polled using standard SCM triggering
  6. Plugin - Multiple DSLs can be referenced at a time
  7. Plugin - Tracks Templates used, will update derivative jobs when template is changed

Description
-------------

To set up a feature toggle to test a change to the Jenkins Pipeline Script generator without negatively impacting existing pipelines.

Usage Steps
------------

Choose a simple, descriptive name for your new property and set it in whichever project will be your test project in the mdProducts.config file. Make sure you also provide a description of the flag in the documentation at the bottom. See the below example for the new feature toggle "enableSonar".


##### mdProducts.config product block
```sh
$ git clone https://github.com/akashnimare/foco/
$ cd foco

$ service_platform_demos {                           
$ bitbucketprojectkey = 'EXAMPLES'                        // BitBucket project key
$ bitbucketprojectname = 'Modern Delivery Examples'      // Human readable Bitbucket
$ bitbucketprojectrepo = 'service-platform-demos'       // Name of the repository (MUST NOT CONTAIN A TILDA (~) as we use this to provide a unique separator in the name
$ productLdapGroup = 'modp_atlassian_gg'               // Product team\'s AD Group to govern entitlements
$ rcPipeline = 'y'
$ nightlyPipeline = 'n'
$ enableSonar = 'y'
}

```

##### mdProducts.config documentation block
```sh
$ enableSonar             (optional)  - Flag to determine whether or not to run a SonarQube Scan on the project
```

Suggested Links
---------------

1. [How-To: Set up a Feature Toggle for a New Pipeline Script Change ](https://moddel-tw.fhlmc.com:8044/display/TECHPILLAR/How-To%3A+Set+up+a+Feature+Toggle+for+a+New+Pipeline+Script+Change)
2. [Jenkins DSL plugin](https://github.com/jenkinsci/job-dsl-plugin/wiki)
3. [Jenkins instances for Modern Delivery](https://moddel-tw.fhlmc.com:8065/)

