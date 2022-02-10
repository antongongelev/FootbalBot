package ru.telegrambot.domain;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.StrBuilder;
import org.springframework.beans.factory.BeanInitializationException;
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

import javax.annotation.PostConstruct;
import java.util.Random;

@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Value("${domain.chat.name}")
    private String CHAT_NAME;

    @Value("${domain.bot.token}")
    private String BOT_TOKEN;

    @Value("${domain.bot.name}")
    private String BOT_NAME;

    private static String CLEAR_CODE = StringUtils.EMPTY;

    private static long CLEAR_TIMESTAMP = 0L;

    private Team team = new Team();

    @PostConstruct
    public void initialize() {
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(this);
            System.out.println("Bot has been launched");
        } catch (TelegramApiException e) {
            throw new BeanInitializationException("Cannot register TelegramBot", e);
        } finally {
            System.out.println("Bot params:");
            System.out.println("CHAT_NAME: " + CHAT_NAME);
            System.out.println("BOT_NAME: " + BOT_NAME);
            System.out.println("TOKEN: " + BOT_TOKEN);
        }
    }

    @Override
    public void onUpdateReceived(Update update) {
        Message message = update.getMessage();
        if (message != null && message.hasText()) {
            try {
                validateChat(message);
            } catch (TeamException e) {
                sendMessage(message, e.getLocalizedMessage());
                return;
            }
            try {
                team.validateDay();
            } catch (TeamException e) {
                team = new Team();
                sendMessage(message, e.getLocalizedMessage());
            }
            try {
                if (isClearCode(message)) {
                    processClear(message);
                    return;
                }
                if (processFriends(message)) {
                    return;
                }
                switch (message.getText().replaceAll(" ", "").toUpperCase()) {
                    case Constants.TEAM:
                        String teamReport = team.getTeamReport();
                        sendMessage(message, teamReport);
                        break;
                    case Constants.ADD_ME:
                        String addSelf = team.addSelf(getFrom(message));
                        sendMessage(message, addSelf);
                        break;
                    case Constants.DO_NOT_KNOW:
                        String doNotKnow = team.doNotKnow(getFrom(message));
                        sendMessage(message, doNotKnow);
                        break;
                    case Constants.REMOVE_ME:
                        String removeMe = team.removeMe(getFrom(message));
                        sendMessage(message, removeMe);
                        break;
                    case Constants.HELP:
                        String help = getHelp();
                        sendMessage(message, help);
                        break;
                    case Constants.CLEAR:
                        CLEAR_CODE = generateCode();
                        CLEAR_TIMESTAMP = System.currentTimeMillis();
                        sendMessage(message, "Для сброса состава отправьте код '" + CLEAR_CODE + "' в течение минуты");
                        break;
                    default:
                        break;
                }
            } catch (TeamException e) {
                sendMessage(message, e.getLocalizedMessage());
            }
        }
    }

    private void processClear(Message message) {
        team = new Team();
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

    private boolean processFriends(Message message) {
        String formattedText = message.getText().replaceAll(" ", "").toUpperCase();
        if (Constants.PLUS_PATTERN.matcher(formattedText).matches()) {
            int number = Integer.parseInt(formattedText.substring(1));
            String addFriends = team.addFriends(getFrom(message), number);
            sendMessage(message, addFriends);
            return true;
        }
        if (Constants.MINUS_PATTERN.matcher(formattedText).matches()) {
            int number = Integer.parseInt(formattedText.substring(1));
            String removeFriends = team.removeFriends(getFrom(message), number);
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
    }

    private String getHelp() {
        return "Основные команды:" + System.lineSeparator() +
                "'+' - Идешь сам" + System.lineSeparator() +
                "'-' - Сливаешься" + System.lineSeparator() +
                "'?' - Под вопросом" + System.lineSeparator() +
                "'+n' - Плюсуешь n друзей (1-9)" + System.lineSeparator() +
                "'-n' - Минусуешь n друзей (1-9)" + System.lineSeparator() +
                "'Состав' - Узнать состав на ближайшую среду" + System.lineSeparator() +
                "'Сброс' - Принудительный сброс состава";
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

    @Override
    public String getBotUsername() {
        return BOT_NAME;
    }

    @Override
    public String getBotToken() {
        return BOT_TOKEN;
    }
}
