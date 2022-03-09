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

psql:
	docker exec -ti footbalbot_postgres psql --username=bot --dbname=football_bot
