# FootbalBot
Telegram bot for managing footbal team players number

Steps to deploy:

0)Register your bot using BotFather in Telegram

1)Run `mvn clean package spring-boot:repackage`

2)Run `java -Dspring.profiles.active=production -Ddomain.chat.name=YOUR_GROUP_CHAT_NAME -Ddomain.bot.name=YOUR_BOT_NAME -Ddomain.bot.token=YOUR_BOT_TOKEN -jar target/FootballBot-1.0-SNAPSHOT.jar`

3)Finish
