development:
	docker-compose \
		--project-directory=${PWD} \
		--project-name=footballbot \
		-f deploy/docker-compose.development.yml \
		up

stop-development:
	docker-compose \
		--project-directory=${PWD} \
		--project-name=footballbot \
		-f deploy/docker-compose.development.yml \
		down

production:
	mvn clean package spring-boot:repackage
	java \
		-Xmx330m \
		-Xss512k \
		-Dspring.profiles.active=production \
		-jar target/FootballBot-1.0-SNAPSHOT.jar
