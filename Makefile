development:
	docker-compose \
		--project-directory=${PWD} \
		--project-name=footballbot \
		-f deploy/docker-compose.development.yml \
		up -d

stop-development:
	docker-compose \
		--project-directory=${PWD} \
		--project-name=footballbot \
		-f deploy/docker-compose.development.yml \
		down

production:
	make stop || true
	mvn clean package spring-boot:repackage
	mvn liquibase:update -Dliquibase.propertyFile=application.yml -Dliquibase.propertyFileWillOverride=application.production.yml
	java \
		-Xmx330m \
		-Xss512k \
		-Dspring.profiles.active=production \
		-Xdebug -Xrunjdwp:transport=dt_socket,address=8786,server=y,suspend=n \
		-jar target/FootballBot-1.0-SNAPSHOT.jar &

stop:
	kill -9 $$(ps aux | grep FootballBot | grep -v grep | awk '{print $$2}')

psql:
	docker exec -ti footbalbot_postgres psql --username=bot --dbname=football_bot
