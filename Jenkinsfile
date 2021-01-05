pipeline {

	agent any
	stages {
	        stage('Configue ACR login') {
        		steps {
				withCredentials([string(credentialsId: 'Azure-Container-Registry', variable: 'SECRET')]) {
                			sh 'az acr login --name $SECRET'
				}
	            	}
        	}
		stage('Deploying Consent') {
			steps {
				sh './gradlew :consent:build'
                withCredentials([string(credentialsId: 'Azure-Container-Registry', variable: 'SECRET')]) {
					sh 'az acr login --name $SECRET'
					sh 'docker build . -f consent/Dockerfile -t  $SECRET".azurecr.io/consent-test:${BUILD_NUMBER}"'
					sh 'docker push $SECRET".azurecr.io/consent-testt:${BUILD_NUMBER}"'
                    sh 'kubectl apply -f consent/kubernetes/deployment.yml'
                    sh 'kubectl apply -f consent/kubernetes/service.yml'
                    sh 'kubectl set image deployment/consent  consent=$SECRET".azurecr.io/consent-test:${BUILD_NUMBER}" -n consent-manager'
				}
                
			}
			post {
				success {
					echo "Deployed Consent"
				}
			}
		}
   
		stage('Deploying Dataflow') {
			steps {
				sh './gradlew :dataflow:build'
                withCredentials([string(credentialsId: 'Azure-Container-Registry', variable: 'SECRET')]) {
					sh 'az acr login --name $SECRET'
					sh 'docker build . -f dataflow/Dockerfile -t  $SECRET".azurecr.io/dataflow-test:${BUILD_NUMBER}"'
					sh 'docker push $SECRET".azurecr.io/dataflow-testt:${BUILD_NUMBER}"'
                    sh 'kubectl apply -f dataflow/kubernetes/deployment.yml'
                    sh 'kubectl apply -f dataflow/kubernetes/service.yml'
                    sh 'kubectl set image deployment/dataflow  dataflow=$SECRET".azurecr.io/dataflow-test:${BUILD_NUMBER}" -n consent-manager'
				}
                
			}
			post {
				success {
					echo "Deployed Dataflow"
				}
			}
		}
		
	}
}


