package ru.telegrambot;

public enum Status {

    READY("Готов"),
    DOES_NOT_KNOW("Под вопросом"),
    NOT_READY("Не готов");

    private final String status;

    Status(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
