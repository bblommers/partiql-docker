jar:
	mvn -f pom.xml clean compile assembly:single

deploy_jar:
	mvn clean deploy

example:
	docker build -t d . --no-cache && docker run -it d
