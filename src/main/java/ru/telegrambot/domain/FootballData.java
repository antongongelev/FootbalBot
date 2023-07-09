package ru.telegrambot.domain;

public class FootballData {

    private String duration;
    private String place;
    private Integer maxPlayers;

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public void setPlace(String place) {
        this.place = place;
    }

    public void setMaxPlayers(Integer maxPlayers) {
        this.maxPlayers = maxPlayers;
    }

    public String getDuration() {
        return duration;
    }

    public String getPlace() {
        return place;
    }

    public Integer getMaxPlayers() {
        return maxPlayers;
    }
}
