// https://jenkins.io/doc/book/pipeline/jenkinsfile/
// Scripted pipeline (not declarative)
pipeline {
  triggers {
    pollSCM '* * * * *'
  }
  options {
    buildDiscarder(logRotator(numToKeepStr: '10'))
    disableConcurrentBuilds()
  }
  agent {
    docker { 
      image 'mauriciojost/scala-sbt-ci:openjdk8-scala2.12.8-sbt1.2.8-0.2.0'
      args '--cpus=2 --memory=4G'
    }
  }
  stages {
    stage('Full fetch') {
      steps {
        sh 'git fetch --depth=10000'
        sh 'git fetch --tags'
      }
    }

    stage('Test') {
      steps {
        wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'gnome-terminal']) {
          sh 'sbt -Dsbt.color=always -Dsbt.global.base=.sbt -Dsbt.boot.directory=.sbt -Dsbt.ivy.home=.ivy2 clean "set every coverageEnabled := true" test coverageReport'
        }
      }
    }
    stage('Coverage') {
      steps {
        wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'gnome-terminal']) {
          sh 'sbt -Dsbt.color=always -Dsbt.global.base=.sbt -Dsbt.boot.directory=.sbt -Dsbt.ivy.home=.ivy2 coverageAggregate'
        }
        step([$class: 'ScoveragePublisher', reportDir: 'target/scala-2.12/scoverage-report', reportFile: 'scoverage.xml'])
      }
    }
  }
  post {  
    failure {  
      emailext body: "<b>[JENKINS] Failure</b>Project: ${env.JOB_NAME} <br>Build Number: ${env.BUILD_NUMBER} <br> Build URL: ${env.BUILD_URL}", from: '', mimeType: 'text/html', replyTo: '', subject: "ERROR CI: ${env.JOB_NAME}", to: "mauriciojostx@gmail.com", attachLog: true, compressLog: false;
    }  
    success {  
      emailext body: "<b>[JENKINS] Success</b>Project: ${env.JOB_NAME} <br>Build Number: ${env.BUILD_NUMBER} <br> Build URL: ${env.BUILD_URL}", from: '', mimeType: 'text/html', replyTo: '', subject: "SUCCESS CI: ${env.JOB_NAME}", to: "mauriciojostx@gmail.com", attachLog: false, compressLog: false;
    }  
  }
}
