package ru.telegrambot.domain;

import java.util.regex.Pattern;

public interface Constants {

    String TEAM = "СОСТАВ";
    String HELP = "/HELP";
    String UPDATE_DAY = "РАСПИСАНИЕ";
    String CLEAR = "СБРОС";
    String ADD_ME = "+";
    String REMOVE_ME = "-";
    String DO_NOT_KNOW = "?";
    Pattern MINUS_PATTERN = Pattern.compile("[-][1-9]");
    Pattern PLUS_PATTERN = Pattern.compile("[+][1-9]");
    String DELIMITER = "======================";
    int CODE_LENGTH = 4;
}
