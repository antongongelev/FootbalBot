# FootballBot

Telegram bot for managing football team players number

#### Deploy:

* Set the following environment variables in Docker-compose.yaml:
  - DOMAIN_CHAT_ID='ID телеграм чата' (example -> 1234567890)
  - DOMAIN_BOT_NAME='Имя телеграм бота' (example -> BotName)
  - DOMAIN_BOT_TOKEN='Токен телеграм бота' (example -> 1234567890)
  - DOMAIN_FOOTBALL_DAY='Дни и время футбола через запятую' (example -> 'Среда-20:00, Воскресенье-20:00')
  - DOMAIN_FOOTBALL_CHECK_IN_BEFORE_HOURS='Время в часах, за которое разрешена запись на ближайшую игру' (example -> 60)
  - DOMAIN_FOOTBALL_CHECK_IN_HOUR='Конкретный час, в который открывается запись после наступления времени открытия записи' (example -> 12)
  - DOMAIN_FOOTBALL_SEND_TEAM_REPORT_BEFORE_HOURS='Время в часах до игры, после наступления которого в чат будет послано напоминание' (example -> 1)
  - DOMAIN_FOOTBALL_IS_IGNORE_ADDITION='Флаг, разрешено ли приводить друзей вне чата' (example -> true)
  - DOMAIN_FOOTBALL_IS_IGNORE_INTERROGATION='Флаг, разрешено ли ставить под сомнение свое присутствие на игре' (example -> true)
* Run the following command

```bash
docker-compose up -d
```
