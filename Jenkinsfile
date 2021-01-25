pipeline {

	agent any
	stages {
		   
		stage('Build and Publish Consent Docker Image to ECR') {
                        steps {
                                withCredentials([string(credentialsId: 'EKS-Region', variable: 'REGION'), string(credentialsId: 'Registry-Name', variable: 'REGISTRY_NAME')]) {

					sh './consentBuild.sh'
					sh 'ls'
					sh 'docker login -u AWS -p $(aws ecr get-login-password --region $REGION) $REGISTRY_NAME'
					sh 'echo $REGISTRY_NAME"/consent-manager/consent:${BUILD_NUMBER}"'
					sh 'docker build  consent -t  $REGISTRY_NAME"/consent-manager/consent:${BUILD_NUMBER}"'
					sh 'docker push $REGISTRY_NAME"/consent-manager/consent:${BUILD_NUMBER}"'
				}

                        }
                        post {
                                success {
                                        echo "Published Consent Docker Image to ECR"
                                }
                        }
                }
 
                stage('Deploy Consent Application in EKS') {
                        steps {
				sh 'kubectl apply -f consent/kubernetes/deployment.yml'
                                sh 'kubectl apply -f consent/kubernetes/service.yml'
							
				withCredentials([string(credentialsId: 'Registry-Name', variable: 'REGISTRY_NAME')]) {
                                        sh 'kubectl set image deployment/consent  consent=$REGISTRY_NAME"/consent-manager/consent:${BUILD_NUMBER}" -n consent-manager'
                                }
                        }
                        post {
                                success {
                                        echo "Deployed Consent to EKS"
                                }
                        }
                }

		stage('Build and Publish User Docker Image to ECR') {
                        steps {
                                withCredentials([string(credentialsId: 'EKS-Region', variable: 'REGION'), string(credentialsId: 'Registry-Name', variable: 'REGISTRY_NAME')]) {

					sh './userBuild.sh'
					sh 'ls'
					sh 'docker login -u AWS -p $(aws ecr get-login-password --region $REGION) $REGISTRY_NAME'
					sh 'echo $REGISTRY_NAME"/consent-manager/user:${BUILD_NUMBER}"'
					sh 'docker build user -t  $REGISTRY_NAME"/consent-manager/user:${BUILD_NUMBER}"'
					sh 'docker push $REGISTRY_NAME"/consent-manager/user:${BUILD_NUMBER}"'
				}

                        }
                        post {
                                success {
                                        echo "Published User Docker Image to ECR"
                                }
                        }
                }
 
                stage('Deploy User Application in EKS') {
                        steps {
				sh 'kubectl apply -f user/kubernetes/deployment.yml'
                                sh 'kubectl apply -f user/kubernetes/service.yml'
							
				withCredentials([string(credentialsId: 'Registry-Name', variable: 'REGISTRY_NAME')]) {
                                        sh 'kubectl set image deployment/user-test  user-test=$REGISTRY_NAME"/consent-manager/user:${BUILD_NUMBER}" -n consent-manager'
                                }
                        }
                        post {
                                success {
                                        echo "Deployed User to EKS"
                                }
                        }
                }

		stage('Build and Publish Dataflow Docker Image to ECR') {
                        steps {
                                withCredentials([string(credentialsId: 'EKS-Region', variable: 'REGION'), string(credentialsId: 'Registry-Name', variable: 'REGISTRY_NAME')]) {

					sh './dataflowBuild.sh'
					sh 'ls'
					sh 'docker login -u AWS -p $(aws ecr get-login-password --region $REGION) $REGISTRY_NAME'
					sh 'echo $REGISTRY_NAME"/consent-manager/dataflow:${BUILD_NUMBER}"'
					sh 'docker build dataflow -t  $REGISTRY_NAME"/consent-manager/dataflow:${BUILD_NUMBER}"'
					sh 'docker push $REGISTRY_NAME"/consent-manager/dataflow:${BUILD_NUMBER}"'
				}

                        }
                        post {
                                success {
                                        echo "Published Dataflow Docker Image to ECR"
                                }
                        }
                }
 
                stage('Deploy Dataflow Application in EKS') {
                        steps {
				sh 'kubectl apply -f dataflow/kubernetes/deployment.yml'
                                sh 'kubectl apply -f dataflow/kubernetes/service.yml'
							
				withCredentials([string(credentialsId: 'Registry-Name', variable: 'REGISTRY_NAME')]) {
                                        sh 'kubectl set image deployment/dataflow  dataflow=$REGISTRY_NAME"/consent-manager/dataflow:${BUILD_NUMBER}" -n consent-manager'
                                }
                        }
                        post {
                                success {
                                        echo "Deployed Dataflow to EKS"
                                }
                        }
                }

		stage('Build and Publish Haproxy Docker Image to ECR') {
                        steps {
                                withCredentials([string(credentialsId: 'EKS-Region', variable: 'REGION'), string(credentialsId: 'Registry-Name', variable: 'REGISTRY_NAME')]) {

					sh 'docker login -u AWS -p $(aws ecr get-login-password --region $REGION) $REGISTRY_NAME'
					sh 'echo $REGISTRY_NAME"/consent-manager/haproxy:${BUILD_NUMBER}"'
					sh 'docker build haproxy -t  $REGISTRY_NAME"/consent-manager/haproxy:${BUILD_NUMBER}"'
					sh 'docker push $REGISTRY_NAME"/consent-manager/haproxy:${BUILD_NUMBER}"'
				}

                        }
                        post {
                                success {
                                        echo "Published Haproxy Docker Image to ECR"
                                }
                        }
                }
 
                stage('Deploy Haproxy Application in EKS') {
                        steps {
				sh 'kubectl apply -f haproxy/kubernetes/deployment.yml'
                                sh 'kubectl apply -f haproxy/kubernetes/service.yml'
							
				withCredentials([string(credentialsId: 'Registry-Name', variable: 'REGISTRY_NAME')]) {
                                        sh 'kubectl set image deployment/haproxy-test  haproxy-test=$REGISTRY_NAME"/consent-manager/haproxy:${BUILD_NUMBER}" -n consent-manager'
                                }
                        }
                        post {
                                success {
                                        echo "Deployed Haproxy to EKS"
                                }
                        }
                }

		
	}
}

