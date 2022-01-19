package ru.telegrambot;

public class PlayerData {

    private Status status = Status.NOT_READY;
    private int calledPlayers;

    public PlayerData(Status status, int calledPlayers) {
        this.status = status;
        this.calledPlayers = calledPlayers;
    }

    public PlayerData() {
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
