jar:
	mvn -f pom.xml clean compile assembly:single

deploy_jar:
	mvn clean install deploy
