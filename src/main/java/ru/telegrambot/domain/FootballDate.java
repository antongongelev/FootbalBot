package ru.telegrambot.domain;

import java.time.LocalDateTime;

public class FootballDate {

    private final String period;
    private final LocalDateTime date;

   FootballDate(String period, LocalDateTime date) {
       this.period = period;
       this.date = date;
   }

    public LocalDateTime getDate() {
        return date;
    }

    public String getPeriod() {
        return period;
    }
}
