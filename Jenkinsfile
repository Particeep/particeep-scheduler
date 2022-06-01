#!groovy

pipeline {
  agent { label 'basic-builder' }
  environment {
    SBT_HOME = tool name: 'sbt_1.6.2', type: 'org.jvnet.hudson.plugins.SbtPluginBuilder$SbtInstallation'
    PATH = "${env.SBT_HOME}/bin:${PATH}"
  }
  options {
    timeout(time: 1, unit: 'HOURS')
    // Only keep the 10 most recent builds
    buildDiscarder(logRotator(numToKeepStr:'10', daysToKeepStr:'10'))
  }
  tools {
    jdk    "JDK_11"
  }
  stages {
    stage('Scala Build') {
      steps {
        sh 'sbt clean update compile'
      }
    }
    stage('Scala Test & Coverage') {
      steps {
        sh 'sbt coverage test'
        sh 'sbt coverageReport'
        sh 'sbt coverageAggregate'
        junit '**/target/test-reports/*.xml'
        cobertura(coberturaReportFile:'**/target/scala-2.13/coverage-report/cobertura.xml')
      }
    }
    stage('Scala Lint') {
      steps {
        sh 'sbt scalastyle'
        recordIssues(
          id: 'scala-lint',
          tool: checkStyle(pattern: '**/target/scalastyle-result.xml')
        )
      }
    }
    stage('Approuve PR') {
      when { changeRequest target: 'develop' }
      steps {
        echo 'Approuve PR'
      }
    }
  }
  post {
    always {
      recordIssues(
        tool: scala()
      )
      recordIssues(
        tool: taskScanner(
          includePattern: '**/*.scala, **/*.ts, **/*.tsx',
          excludePattern: '**/api_client/**, **/node_modules/**',
          normalTags: 'TODO, FIXME',
          highTags: 'XXX'
        )
      )
    }
  }
}
