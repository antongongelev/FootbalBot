package ru.telegrambot.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrBuilder;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import ru.telegrambot.data.TeamService;

import javax.annotation.PostConstruct;
import java.util.Random;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Value("${domain.chat.id}")
    private String CHAT_ID;

    @Value("${domain.bot.token}")
    private String BOT_TOKEN;

    @Value("${domain.bot.name}")
    private String BOT_NAME;

    @Value("${domain.football.day}")
    private String FOOTBALL_DAY;

    @Value("${domain.football.check-in-before-hours}")
    private String CHECK_IN_BEFORE;

    @Value("${domain.football.send-team-report-before-hours}")
    private String SEND_TEAM_REPORT_BEFORE;

    @Value("${domain.football.is-ignore-interrogation}")
    private String IS_IGNORE_INTERROGATION;

    @Value("${domain.football.is-ignore-addition}")
    private String IS_IGNORE_ADDITION;

    private boolean isReadyToSet;

    private boolean isTeamReportSent;

    private static String CLEAR_CODE = StringUtils.EMPTY;

    private static long CLEAR_TIMESTAMP = 0L;

    private static long DAY_UPDATE_TIMESTAMP = 0L;

    private Team team = new Team();

    private boolean isStrictEnroll;

    @Autowired
    private TeamService teamService;

    @Autowired
    private DayService dayService;

    @PostConstruct
    public void initialize() {
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(this);
            System.out.println("Bot has been launched with params:");
            System.out.println("CHAT_ID: " + CHAT_ID);
            System.out.println("BOT_NAME: " + BOT_NAME);
            System.out.println("TOKEN: " + BOT_TOKEN);
            System.out.println("FOOTBALL_DAY: " + FOOTBALL_DAY);
            System.out.println("CHECK_IN_BEFORE_HOURS: " + CHECK_IN_BEFORE);
            System.out.println("SEND_TEAM_REPORT_BEFORE_HOURS: " + SEND_TEAM_REPORT_BEFORE);
            System.out.println("IS_IGNORE_ADDITION: " + IS_IGNORE_ADDITION);
            System.out.println("IS_IGNORE_INTERROGATION: " + IS_IGNORE_INTERROGATION);
            isStrictEnroll = isIgnoreAddition() && isIgnoreInterrogation();
        } catch (TelegramApiException e) {
            throw new BeanInitializationException("Cannot register TelegramBot: ", e);
        }
    }

    @Scheduled(fixedDelay = 60_000)
    public void task() {

        // Проверим день
        try {
            dayService.validateDay();
        } catch (TeamException e) {
            team = new Team();
            isReadyToSet = false;
            isTeamReportSent = false;
            try {
                teamService.save(team);
            } catch (JsonProcessingException ex) {
                ex.printStackTrace();
            }
            sendMessage(e.getLocalizedMessage());
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        // Проверим, можно ли записываться
        try {
            if (!isReadyToSet && dayService.isTimeToCheckIn()) {
                isReadyToSet = true;

                Team loadedTeam = teamService.load();
                if (loadedTeam.getTeam().isEmpty()) {
                    // После перезапуска бота мб случай, когда мы уже
                    // начали набирать состав, поэтому не стоит спамить
                    sendMessage("Набираем состав на " + dayService.getDay());
                }
            }
        } catch (IllegalAccessException | JsonProcessingException e) {
            e.printStackTrace();
        }

        // Проверим, нужно ли оповещение о составе
        try {
            if (!isTeamReportSent && dayService.isTimeToSendTeamReport()) {
                isTeamReportSent = true;

                team = teamService.load();
                String teamReport = team.getTeamReport(dayService.getDay(), isStrictEnroll);
                sendMessage("Футбол скоро начнется...");
                sendMessage(teamReport);
            }
        } catch (IllegalAccessException | JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        if (message == null || !message.hasText()) {
            return;
        }
        try {
            validateChat(message);
        } catch (TeamException e) {
            sendMessageToSender(message, e.getLocalizedMessage());
            return;
        }

        try {
            team = teamService.load();
            if (isClearCode(message)) {
                processClear(message);
                return;
            }
            if (isDayUpdate()) {
                updateDay(message);
                return;
            }
            if (processFriends(message)) {
                return;
            }
            switch (message.getText().replaceAll(" ", "").toUpperCase()) {
                case Constants.TEAM:
                    String teamReport = team.getTeamReport(dayService.getDay(), isStrictEnroll);
                    sendMessage(teamReport);
                    break;
                case Constants.ADD_ME:
                    if (!isReadyToSet()) {
                        return;
                    }
                    String addSelf = team.addSelf(getFrom(message));
                    teamService.save(team);
                    sendMessage(addSelf);
                    break;
                case Constants.DO_NOT_KNOW:
                    if (isIgnoreInterrogation() || !isReadyToSet()) {
                        return;
                    }
                    String doNotKnow = team.doNotKnow(getFrom(message));
                    teamService.save(team);
                    sendMessage(doNotKnow);
                    break;
                case Constants.REMOVE_ME:
                    if (!isReadyToSet()) {
                        return;
                    }
                    String removeMe = team.removeMe(getFrom(message));
                    teamService.save(team);
                    sendMessage(removeMe);
                    break;
                case Constants.HELP:
                    String help = getHelp();
                    sendMessage(help);
                    break;
                case Constants.UPDATE_DAY:
                    DAY_UPDATE_TIMESTAMP = System.currentTimeMillis();
                    sendMessage(String.format("Текущее расписание игр:" + System.lineSeparator() + "'%s'" + System.lineSeparator() + "Отправьте новое расписания в " +
                                                      "течение минуты", FOOTBALL_DAY));
                    break;
                case Constants.CLEAR:
                    if (!isReadyToSet()) {
                        return;
                    }
                    CLEAR_CODE = generateCode();
                    CLEAR_TIMESTAMP = System.currentTimeMillis();
                    sendMessage("Для сброса состава отправьте код '" + CLEAR_CODE + "' в течение минуты");
                    break;
                default:
                    break;
            }
        } catch (TeamException | JsonProcessingException e) {
            sendMessage("Какая-то ошибка при обработке запроса: " + e.getLocalizedMessage());
        }
    }

    private boolean isDayUpdate() {
        if (DAY_UPDATE_TIMESTAMP == 0) {
            return false;
        }
        long current = System.currentTimeMillis();
        if (DAY_UPDATE_TIMESTAMP < current && current < DAY_UPDATE_TIMESTAMP + 60_000) {
            DAY_UPDATE_TIMESTAMP = 0;
            return true;
        }
        DAY_UPDATE_TIMESTAMP = 0;
        return false;
    }

    private void updateDay(Message message) {
        boolean success = dayService.updateDay(message.getText());
        if (!success) {
            sendMessage("Не удалось обновить расписание" + System.lineSeparator() + "Расписание остается прежним:" + System.lineSeparator() + FOOTBALL_DAY);
            return;
        }
        FOOTBALL_DAY = message.getText();
        sendMessage(getFrom(message) + " обновил расписание" + System.lineSeparator() + "Новое расписание:" + System.lineSeparator() + FOOTBALL_DAY);
    }

    private boolean isIgnoreInterrogation() {
        return Boolean.parseBoolean(IS_IGNORE_INTERROGATION);
    }

    private boolean isIgnoreAddition() {
        return Boolean.parseBoolean(IS_IGNORE_ADDITION);
    }

    private void processClear(Message message) throws JsonProcessingException {
        team = new Team();
        teamService.save(team);
        CLEAR_TIMESTAMP = 0L;
        CLEAR_CODE = StringUtils.EMPTY;
        sendMessage(getFrom(message) + " сбросил состав");
    }

    private boolean isClearCode(Message message) {
        long current = System.currentTimeMillis();
        return CLEAR_CODE.equals(message.getText()) && (CLEAR_TIMESTAMP < current && current < CLEAR_TIMESTAMP + 60_000);
    }

    private String generateCode() {
        StrBuilder builder = new StrBuilder();
        Random random = new Random();
        for (int i = 0; i < Constants.CODE_LENGTH; i++) {
            builder.append(random.nextInt(10));
        }
        return builder.toString();
    }

    private boolean processFriends(Message message) throws JsonProcessingException {
        String formattedText = message.getText().replaceAll(" ", "").toUpperCase();
        if (Constants.PLUS_PATTERN.matcher(formattedText).matches()) {
            if (isIgnoreAddition() || !isReadyToSet()) {
                return true;
            }
            int number = Integer.parseInt(formattedText.substring(1));
            String addFriends = team.addFriends(getFrom(message), number);
            teamService.save(team);
            sendMessage(addFriends);
            return true;
        }
        if (Constants.MINUS_PATTERN.matcher(formattedText).matches()) {
            if (isIgnoreAddition() || !isReadyToSet()) {
                return true;
            }
            int number = Integer.parseInt(formattedText.substring(1));
            String removeFriends = team.removeFriends(getFrom(message), number);
            teamService.save(team);
            sendMessage(removeFriends);
            return true;
        }
        return false;
    }

    private void validateChat(Message message) {
        String id = String.valueOf(message.getChat().getId());
        if (!CHAT_ID.equals(id)) {
            throw new TeamException("Этот бот не предназначен для данного чата");
        }
    }

    private String getHelp() {
        return "Основные команды:" + System.lineSeparator() +
                "'+' - Идешь" + System.lineSeparator() +
                "'-' - Сливаешься" + System.lineSeparator() +
                (isIgnoreInterrogation() ? "" : "'?' - Под вопросом" + System.lineSeparator()) +
                (isIgnoreAddition() ? "" : "'+n' - Плюсуешь n друзей (1-9)" + System.lineSeparator() +
                        "'-n' - Минусуешь n друзей (1-9)" + System.lineSeparator()) +
                "'Состав' - Узнать состав" + System.lineSeparator() +
                "'Сброс' - Сброс состава" + System.lineSeparator() +
                "'Расписание' - Измененить расписание";
    }

    private String getFrom(Message message) {
        String playerName;
        User user = message.getFrom();
        String firstName = user.getFirstName();
        String lastName = user.getLastName();
        String userName = user.getUserName();

        if (StringUtils.isNotEmpty(firstName) && StringUtils.isNotEmpty(lastName)) {
            playerName = firstName + " " + lastName;
        } else if (StringUtils.isNotEmpty(userName)) {
            playerName = userName;
        } else if (StringUtils.isNotEmpty(firstName)) {
            playerName = firstName;
        } else {
            playerName = lastName;
        }

        return playerName;
    }

    private void sendMessage(String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(CHAT_ID);
        sendMessage.setText(getPureString(message));

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendMessageToSender(Message message, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setText(text);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private boolean isReadyToSet() {
        if (isReadyToSet) {
            return true;
        }
        sendMessage("Возможность изменения состава будет доступна после оповещения");
        return false;
    }

    @Override
    public String getBotUsername() {
        return BOT_NAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }

    private String getPureString(String string) {
        String s1 = string.replaceAll("_", "-");
        String s2 = s1.replaceAll("@", "-");
        return s2.replaceAll("&", "-");
    }
}
