package ru.telegrambot.domain;

public class PlayerData {

    private Status status;
    private int calledPlayers;

    public PlayerData(Status status, int calledPlayers) {
        this.status = status;
        this.calledPlayers = calledPlayers;
    }

    public PlayerData(Status status) {
        this.status = status;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public int getCalledPlayers() {
        return calledPlayers;
    }

    public void setCalledPlayers(int calledPlayers) {
        this.calledPlayers = calledPlayers;
    }
}
