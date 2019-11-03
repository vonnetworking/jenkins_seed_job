pipeline {
    agent any 

    stages {    
        stage('test') { 
            steps { 
                sh 'echo hello'
            }            
        stage ('Test 2: Master') {
            when { branch 'master' }
            steps { 
                echo 'I only execute on the master branch.' 
            }
        }

        stage ('Test 2: Dev') {
            when { not { branch 'master' } }
            steps {
                echo 'I execute on non-master branches.'
            }
        }
    }
}
