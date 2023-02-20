package ru.telegrambot.domain;

public class PlayerData {

    private long timestamp = System.currentTimeMillis();
    private Status status;
    private int calledPlayers;

    public PlayerData(long timestamp, Status status, int calledPlayers) {
        this.timestamp = timestamp;
        this.status = status;
        this.calledPlayers = calledPlayers;
    }

    public PlayerData(){}

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

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
