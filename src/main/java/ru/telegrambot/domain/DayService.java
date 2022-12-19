package ru.telegrambot.domain;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Locale;

@Service
public class DayService {

    @Value("${domain.football.day}")
    private String FOOTBALL_DAY;
    private String footballDay;

    @PostConstruct
    private void setup() throws IllegalAccessException {
        footballDay = getNearest();
    }

    public void validateDay() throws IllegalAccessException {
        String nearest = getNearest();
        if (!footballDay.equals(nearest)) {
            footballDay = getNearest();
            throw new TeamException("Состав на " + footballDay + " был сброшен. Набираем на " + nearest);
        }
    }

    private String getNearest() throws IllegalAccessException {
        LocalDate localDate = LocalDate.now();
        LocalDate nearest = localDate.with(TemporalAdjusters.nextOrSame(getDayOfWeek(FOOTBALL_DAY)));
        return nearest.format(DateTimeFormatter.ofPattern("d MMMM", new Locale("ru")));
    }

    public String getDay() {
        return footballDay;
    }

    private DayOfWeek getDayOfWeek(String day) throws IllegalAccessException {
        if (StringUtils.isEmpty(day)) {
            throw new IllegalAccessException("Неверный день для футбола");
        }
        switch (day.toUpperCase()) {
            case "ПОНЕДЕЛЬНИК":
                return DayOfWeek.MONDAY;
            case "ВТОРНИК":
                return DayOfWeek.TUESDAY;
            case "СРЕДА":
                return DayOfWeek.WEDNESDAY;
            case "ЧЕТВЕРГ":
                return DayOfWeek.THURSDAY;
            case "ПЯТНИЦА":
                return DayOfWeek.FRIDAY;
            case "СУББОТА":
                return DayOfWeek.SATURDAY;
            case "ВОСКРЕСЕНЬЕ":
                return DayOfWeek.SUNDAY;
            default:
                throw new IllegalAccessException("Неверный день для футбола");
        }
    }
}
