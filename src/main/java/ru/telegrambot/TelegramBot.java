package ru.telegrambot;

import org.apache.commons.lang3.StringUtils;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
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
            System.out.println("BOT_NAME: " + getBotName());
            System.out.println("CHAT_NAME: " + getChatName());
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
                        sendMessage(message, getMessage(Constants.TEAM, message));
                        break;
                    case Constants.ADD_ME:
                        team.addSelf(getFrom(message));
                        sendMessage(message, getMessage(Constants.ADD_ME, message));
                        break;
                    case Constants.DO_NOT_KNOW:
                        team.doNotKnow(getFrom(message));
                        sendMessage(message, getMessage(Constants.DO_NOT_KNOW, message));
                        break;
                    case Constants.REMOVE_ME:
                        team.removeMe(getFrom(message));
                        sendMessage(message, getMessage(Constants.REMOVE_ME, message));
                        break;
                    case Constants.HELP:
                        sendMessage(message, getMessage(Constants.HELP, message));
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
            team.addFriends(getFrom(message), number);
            sendMessage(message, getFrom(message) + " сделал +" + number + ". Итого: " + team.getTotal());
            return true;
        }
        if (Constants.MINUS_PATTERN.matcher(formattedText).matches()) {
            int number = Integer.parseInt(formattedText.substring(1));
            team.removeFriends(getFrom(message), number);
            sendMessage(message, getFrom(message) + " сделал -" + number + ". Итого: " + team.getTotal());
            return true;
        }
        return false;
    }

    private void validateChat(Message message) {
        if (!"group".equals(message.getChat().getType()) || !getChatName().equals(message.getChat().getTitle())) {
            throw new TeamException("Этот бот не предназначен для данного чата");
        }
    }

    private String getMessage(String command, Message message) {
        switch (command) {
            case Constants.ADD_ME:
                return getFrom(message) + " вписался. Итого: " + team.getTotal();
            case Constants.REMOVE_ME:
                return getFrom(message) + " слился. Итого: " + team.getTotal();
            case Constants.DO_NOT_KNOW:
                return getFrom(message) + " под вопросом. Итого: " + team.getTotal();
            case Constants.TEAM:
                return team.getFullReport();
            case Constants.HELP:
                return getHelp();
        }
        return StringUtils.EMPTY;
    }

    private String getHelp() {
        return "Основные команды:" + System.lineSeparator() +
                "'+' - Идешь сам" + System.lineSeparator() +
                "'-' - Сливаешься" + System.lineSeparator() +
                "'?' - Пока под вопросом" + System.lineSeparator() +
                "'+n' - Плюсуешь n друзей (1-9)" + System.lineSeparator() +
                "'-n' - Минусуешь n друзей (1-9)" + System.lineSeparator() +
                "'Состав' - Узнать состав на ближайшую среду";
    }

    private String getFrom(Message message) {
        String playerName;
        String lastName = message.getFrom().getLastName();
        if (StringUtils.isEmpty(lastName)) {
            playerName = message.getFrom().getUserName();
        } else {
            playerName = message.getFrom().getLastName() + " " + message.getFrom().getFirstName();
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
