#!/bin/bash

source ./define-heroku-variables.sh

java -jar FootballBot-1.0-SNAPSHOT.jar -Xmx330m -Xss512k -Dspring.profiles.active=production