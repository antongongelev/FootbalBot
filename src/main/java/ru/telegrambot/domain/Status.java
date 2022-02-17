package ru.telegrambot.domain;

public enum Status {

    READY("Готов"),
    DOES_NOT_KNOW("Под вопросом"),
    CALLED_FRIENDS("Позвал друзей"),
    NOT_READY("Не готов");

    private final String status;

    Status(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
