package ru.telegrambot;

import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class TelegramBot extends TelegramLongPollingBot {

    private static String CHAT_NAME = StringUtils.EMPTY;
    private static String BOT_TOKEN = StringUtils.EMPTY;
    private static String BOT_NAME = StringUtils.EMPTY;
    private Team team = new Team();

    public static void main(String[] args) {
        try {
            CHAT_NAME = args[0];
            BOT_NAME = args[1];
            BOT_TOKEN = args[2];
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(new TelegramBot());
            System.out.println("Bot has been launched with params:");
            System.out.println("CHAT_NAME: " + getChatName());
            System.out.println("BOT_NAME: " + getBotName());
            System.out.println("TOKEN: " + getToken());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

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
                    default:
                        break;
                }
            } catch (TeamException e) {
                sendMessage(message, e.getLocalizedMessage());
            }
        }
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
        if (!("group".equals(type) || "supergroup".equals(type)) || !getChatName().contains(title)) {
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
                "'Состав' - Узнать состав на ближайшую среду";
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

    public String getBotUsername() {
        return getBotName();
    }

    public String getBotToken() {
        return getToken();
    }

    private static String getBotName() {
        if (StringUtils.isEmpty(BOT_NAME)) {
            return Constants.BOT_NAME;
        }
        return BOT_NAME;
    }

    private static String getChatName() {
        if (StringUtils.isEmpty(CHAT_NAME)) {
            return Constants.CHAT_NAME;
        }
        return CHAT_NAME;
    }

    private static String getToken() {
        if (StringUtils.isEmpty(BOT_TOKEN)) {
            return Constants.BOT_TOKEN;
        }
        return BOT_TOKEN;
    }
}
