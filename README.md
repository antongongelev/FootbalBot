# FootbalBot

Telegram bot for managing football team players number

#### Deploy:

* Set the following environment variables in Docker-compose.yaml:
    * DOMAIN_CHAT_NAME=Your telegram chat name (use '' if it contains several words)
    * DOMAIN_BOT_NAME=Your bot name
    * DOMAIN_BOT_TOKEN=Your bot token
    * DOMAIN_FOOTBALL_DAY=Day of Week (in Russian)
    * DOMAIN_FOOTBALL_CHECK_IN_BEFORE_HOURS=Time before event to enroll (in hours)
* Run the following command

```bash
docker-compose up -d
```
