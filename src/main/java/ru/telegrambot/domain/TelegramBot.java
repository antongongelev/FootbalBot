package ru.telegrambot.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrBuilder;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Value("${domain.chat.name}")
    private String CHAT_NAME;

    @Value("${domain.bot.token}")
    private String BOT_TOKEN;

    @Value("${domain.bot.name}")
    private String BOT_NAME;

    @Value("${domain.football.day}")
    private String FOOTBALL_DAY;

    @Value("${domain.football.check-in-before-hours}")
    private String CHECK_IN_BEFORE;

    private Long chatID;

    private boolean isReadyToSet;

    private static String CLEAR_CODE = StringUtils.EMPTY;

    private static long CLEAR_TIMESTAMP = 0L;

    private Team team = new Team();

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
            System.out.println("CHAT_NAME: " + CHAT_NAME);
            System.out.println("BOT_NAME: " + BOT_NAME);
            System.out.println("TOKEN: " + BOT_TOKEN);
            System.out.println("FOOTBALL_DAY: " + FOOTBALL_DAY);
            System.out.println("CHECK_IN_BEFORE_HOURS: " + CHECK_IN_BEFORE);
        } catch (TelegramApiException e) {
            throw new BeanInitializationException("Cannot register TelegramBot: ", e);
        }

        ScheduledExecutorService taskService = Executors.newScheduledThreadPool(1);
        taskService.scheduleAtFixedRate(() -> {
            if (Objects.isNull(chatID)) {
                return;
            }

            try {
                dayService.validateDay();
            } catch (TeamException e) {
                team = new Team();
                isReadyToSet = false;
                try {
                    teamService.save(team);
                } catch (JsonProcessingException ex) {
                    ex.printStackTrace();
                }
                sendMessage(e.getLocalizedMessage());
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }

            try {
                if (!isReadyToSet && dayService.isFootballSoon()) {
                    isReadyToSet = true;
                    sendMessage("Набираем состав на " + dayService.getDay());
                }
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }, 0, 60, TimeUnit.MINUTES);
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
            sendMessage(message, e.getLocalizedMessage());
            return;
        }

        try {
            team = teamService.load();
            if (isClearCode(message)) {
                processClear(message);
                return;
            }
            if (processFriends(message)) {
                return;
            }
            switch (message.getText().replaceAll(" ", "").toUpperCase()) {
                case Constants.TEAM:
                    String teamReport = team.getTeamReport(dayService.getDay());
                    sendMessage(message, teamReport);
                    break;
                case Constants.ADD_ME:
                    if (!isReadyToSet(message)) {
                        return;
                    }
                    String addSelf = team.addSelf(getFrom(message));
                    sendMessage(message, addSelf);
                    break;
                case Constants.DO_NOT_KNOW:
                    if (!isReadyToSet(message)) {
                        return;
                    }
                    String doNotKnow = team.doNotKnow(getFrom(message));
                    sendMessage(message, doNotKnow);
                    break;
                case Constants.REMOVE_ME:
                    if (!isReadyToSet(message)) {
                        return;
                    }
                    String removeMe = team.removeMe(getFrom(message));
                    sendMessage(message, removeMe);
                    break;
                case Constants.HELP:
                    String help = getHelp();
                    sendMessage(message, help);
                    break;
                case Constants.CLEAR:
                    if (!isReadyToSet(message)) {
                        return;
                    }
                    CLEAR_CODE = generateCode();
                    CLEAR_TIMESTAMP = System.currentTimeMillis();
                    sendMessage(message, "Для сброса состава отправьте код '" + CLEAR_CODE + "' в течение минуты");
                    break;
                default:
                    break;
            }
            teamService.save(team);
        } catch (TeamException | JsonProcessingException e) {
            sendMessage(message, e.getLocalizedMessage());
        }
    }

    private void processClear(Message message) throws JsonProcessingException {
        team = new Team();
        teamService.save(team);
        CLEAR_TIMESTAMP = 0L;
        CLEAR_CODE = StringUtils.EMPTY;
        sendMessage(message, getFrom(message) + " сбросил состав");
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
            if (!isReadyToSet(message)) {
                return true;
            }
            int number = Integer.parseInt(formattedText.substring(1));
            String addFriends = team.addFriends(getFrom(message), number);
            teamService.save(team);
            sendMessage(message, addFriends);
            return true;
        }
        if (Constants.MINUS_PATTERN.matcher(formattedText).matches()) {
            if (!isReadyToSet(message)) {
                return true;
            }
            int number = Integer.parseInt(formattedText.substring(1));
            String removeFriends = team.removeFriends(getFrom(message), number);
            teamService.save(team);
            sendMessage(message, removeFriends);
            return true;
        }
        return false;
    }

    private void validateChat(Message message) {
        String type = message.getChat().getType();
        String title = message.getChat().getTitle();
        if (!("group".equals(type) || "supergroup".equals(type)) || !CHAT_NAME.contains(title)) {
            throw new TeamException("Этот бот не предназначен для данного чата");
        }
        chatID = message.getChatId();
    }

    private String getHelp() {
        return "Основные команды:" + System.lineSeparator() +
                "'+' - Идешь сам" + System.lineSeparator() +
                "'-' - Сливаешься" + System.lineSeparator() +
                "'?' - Под вопросом" + System.lineSeparator() +
                "'+n' - Плюсуешь n друзей (1-9)" + System.lineSeparator() +
                "'-n' - Минусуешь n друзей (1-9)" + System.lineSeparator() +
                "'Состав' - Узнать состав" + System.lineSeparator() +
                "'Сброс' - Сброс состава";
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

        String s1 = playerName.replaceAll("_", "-");
        String s2 = s1.replaceAll("@", "-");
        return s2.replaceAll("&", "-");
    }

    private void sendMessage(String message) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(chatID.toString());
        sendMessage.setText(message);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(Message message, String text) {
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

    private boolean isReadyToSet(Message message) {
        if (isReadyToSet) {
            return true;
        }
        sendMessage(message, "Возможность изменения состава будет доступна после оповещения");
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
}
