package ru.telegrambot.domain;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

@Service
public class DayService {

    @Value("${domain.football.day}")
    private String FOOTBALL_DAY;

    @Value("${domain.football.check-in-before-hours}")
    private String CHECK_IN_BEFORE;
    private String footballDay;
    private final static DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("d MMMM H:mm", new Locale("ru"));

    @PostConstruct
    private void setup() throws IllegalAccessException {
        footballDay = getNearest();
    }

    public void validateDay() throws IllegalAccessException {
        String nearest = getNearest();
        if (!footballDay.equals(nearest)) {
            footballDay = getNearest();
            throw new TeamException("Следующий футбол будет " + nearest);
        }
    }

    public boolean isFootballSoon() throws IllegalAccessException {
        return getNearestDate().minusHours(Long.parseLong(CHECK_IN_BEFORE)).isBefore(LocalDateTime.now());
    }

    private LocalDateTime getNearestDate() throws IllegalAccessException {

        Optional<LocalDateTime> nearest = Arrays.stream(FOOTBALL_DAY.split(";")).map(String::trim).map(i -> {
            try {
                LocalDateTime now = LocalDateTime.now();

                String day = i.substring(0, i.indexOf('_'));
                String hour = i.substring(i.indexOf('_') + 1, i.indexOf(':'));
                String minute = i.substring(i.indexOf(':') + 1);

                if (now.getDayOfWeek() == getDayOfWeek(day) &&
                        (now.getHour() < Integer.parseInt(hour) || now.getHour() == Integer.parseInt(hour) && now.getMinute() < Integer.parseInt(minute))) {
                    return now.withHour(Integer.parseInt(hour)).withMinute(Integer.parseInt(minute));
                }
                return now.with(TemporalAdjusters.next(getDayOfWeek(day))).withHour(Integer.parseInt(hour)).withMinute(Integer.parseInt(minute));

            } catch (IllegalAccessException e) {
                throw new RuntimeException("Неверно заданы дни футбола");
            }

        }).min((o1, o2) -> {
            if (o1.isEqual(o2)) {
                return 0;
            }
            if (o1.isBefore(o2)) {
                return -1;
            }
            return 1;
        });

        if (!nearest.isPresent()) {
            throw new IllegalAccessException("Неверно заданы дни футбола");

        }

        return nearest.get();
    }

    private String getNearest() throws IllegalAccessException {
        return getNearestDate().format(FORMATTER);
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
