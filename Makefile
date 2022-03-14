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
	PGPASSWORD=dd1b42145cf89501f1f41c3220b91337e83c7514f3dbff53f3c78e3a536fc8ff \
	psql --host=ec2-176-34-105-15.eu-west-1.compute.amazonaws.com --username=hnlrzebjiwpxjg --dbname=dbcofcaflth2he