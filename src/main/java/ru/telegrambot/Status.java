package ru.telegrambot;

public enum Status {

    READY("готов"),
    DOES_NOT_KNOW("под вопросом"),
    NOT_READY("не готов");

    private final String status;

    Status(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }
}
