pipeline {
    agent any
    tools{
        jdk 'jdk18'
        maven 'maven3'
    }
    
    environment{
        SCANNER_HOME = tool 'sonar-scanner'
    }
    

    stages {
        stage('git checkout') {
            steps {
                git branch: 'main', changelog: false, poll: false, url: '<github-link>'
            }
        }
         stage('code compile') {
            steps {
                sh "mvn clean compile"
            }
        }
         stage('run test cases') {
            steps {
                sh "mvn test"
            }
        }
        stage('sonarqube analysis') {
            steps {
                 withSonarQubeEnv('sonar-server') {
                      sh ''' $SCANNER_HOME/bin/sonar-scanner -Dsonar.projectName=Java-WebApp \
                        -Dsonar.java.binaries=. \
                        -Dsonar.projectKey=Java-WebApp '''
                } 
            }
        }
        stage('OWASP Dependency Check') {
            steps {
                   dependencyCheck additionalArguments: '--scan ./   ', odcInstallation: 'DP'
                   dependencyCheckPublisher pattern: '**/dependency-check-report.xml'
            }
        }
       stage('Maven Build') {
            steps {
                    sh "mvn clean package"
            }
        }
        stage('Docker Build & Push') {
            steps {
                   script {
                       withDockerRegistry(credentialsId: '97f49438-8584-4213-8d5b-771f283ba994', toolName: 'docker') {
                            sh "docker build -t webapp ."
                            sh "docker tag webapp siva1369/webapp:latest"
                            sh "docker push siva1369/webapp:latest "
                        }
                   } 
            }
        }
        
        stage('Docker Image scan') {
            steps {
                    sh "trivy image siva1369/webapp:latest "
            }
        }
        stage('Docker container') {
            steps {
                   script {
                       withDockerRegistry(credentialsId: '97f49438-8584-4213-8d5b-771f283ba994', toolName: 'docker') {
                            sh "docker run --name webapp -d -p 8181:8080 siva1369/webapp:latest"
                            
                        }
                   } 
            }
        }

        
    }
}
