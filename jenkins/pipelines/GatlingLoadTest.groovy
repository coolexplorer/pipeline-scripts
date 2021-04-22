pipeline {
    node('default') {
        stage('test') {
            sh 'echo hello world'
        }
    }    
}