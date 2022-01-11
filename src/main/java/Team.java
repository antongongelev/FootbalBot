import org.apache.commons.lang3.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.HashMap;
import java.util.Locale;

public class Team {

    private final LocalDateTime date = LocalDateTime.now();
    private final String wednesday = getNearest();
    private final HashMap<String, PlayerData> team = new HashMap<>();


    private String getNearest() {
        LocalDate localDate = LocalDate.of(date.getYear(), date.getMonthValue(), date.getDayOfMonth());
        LocalDate nearest = localDate.with(TemporalAdjusters.nextOrSame(Constants.DAY_OF_WEEK));
        return nearest.format(DateTimeFormatter.ofPattern("dd MMMM", new Locale("ru")));
    }

    public void validateDay() {
        if (!wednesday.equals(getNearest())) {
            throw new TeamException("Состав был сброшен. Набираем на " + wednesday);
        }
    }

    public String getFullReport() {
        return "Состав на " + getWednesday() + ": " + System.lineSeparator() + getPlayers() + "Итого: " + getTotal();
    }

    public String getTotal() {
        if (getDoubt() > 0) {
            return getSure() + " и ещё " + getDoubt() + " под вопросом";
        }
        return String.valueOf(getSure());
    }

    private int getSure() {
        return getSelf() + getFriends();
    }

    private int getFriends() {
        return team.values().stream().map(PlayerData::getCalledPlayers).reduce(0, Integer::sum);
    }

    private int getSelf() {
        return (int) team.values().stream().filter(p -> Status.READY == p.getStatus()).count();
    }

    private int getDoubt() {
        return (int) team.values().stream().filter(p -> Status.DOES_NOT_KNOW == p.getStatus()).count();
    }

    public String getWednesday() {
        return wednesday;
    }

    private String getPlayers() {
        StringBuilder builder = new StringBuilder();
        team.entrySet()
            .stream()
            .filter(this::isRelevantPlayer)
            .forEach(e -> builder.append(getPlayerReport(e)));
        if (StringUtils.isEmpty(builder.toString())) {
            builder.append("Пока никого...").append(System.lineSeparator());
        }
        return builder.toString();
    }

    private boolean isRelevantPlayer(java.util.Map.Entry<String, PlayerData> player) {
        return (Status.READY == player.getValue().getStatus() || Status.DOES_NOT_KNOW == player.getValue().getStatus()) || player.getValue().getCalledPlayers() > 0;
    }

    private String getPlayerReport(java.util.Map.Entry<String, PlayerData> player) {
        int called = player.getValue().getCalledPlayers();
        if (called <= 0) {
            return player.getKey() + " -> " + player.getValue().getStatus().getStatus() + System.lineSeparator();
        }
        return player.getKey() + " -> " + player.getValue().getStatus().getStatus() + ". Позвал +" + called + System.lineSeparator();
    }

    public void addSelf(String player) {
        PlayerData playerData = team.get(player);
        if (playerData == null) {
            PlayerData data = new PlayerData();
            data.setStatus(Status.READY);
            team.put(player, data);
        } else {
            if (Status.READY == playerData.getStatus()) {
                throw new TeamException(player + " попытался вписаться, хотя уже был вписан");
            }
            playerData.setStatus(Status.READY);
        }
    }

    public void doNotKnow(String player) {
        PlayerData playerData = team.get(player);
        if (playerData == null) {
            PlayerData data = new PlayerData();
            data.setStatus(Status.DOES_NOT_KNOW);
            team.put(player, data);
        } else {
            if (Status.DOES_NOT_KNOW == playerData.getStatus()) {
                throw new TeamException(player + " попытался поставить 'Под вопросом', хотя уже был под вопросом");
            }
            playerData.setStatus(Status.DOES_NOT_KNOW);
        }
    }

    public void removeMe(String player) {
        PlayerData playerData = team.get(player);
        if (playerData == null) {
            PlayerData data = new PlayerData();
            data.setStatus(Status.NOT_READY);
            team.put(player, data);
        } else {
            if (Status.NOT_READY == playerData.getStatus()) {
                throw new TeamException(player + " попытался слиться, хотя и не собирался приходить");
            }
            playerData.setStatus(Status.NOT_READY);
        }
    }

    public void addFriends(String player, int number) {
        PlayerData playerData = team.get(player);
        if (playerData == null) {
            PlayerData data = new PlayerData();
            data.setCalledPlayers(data.getCalledPlayers() + number);
            team.put(player, data);
        } else {
            playerData.setCalledPlayers(playerData.getCalledPlayers() + number);
        }
    }

    public void removeFriends(String player, int number) {
        PlayerData playerData = team.get(player);
        if (playerData == null) {
            throw new TeamException(player + " хотел сделать -" + number + ", но он столько не звал");
        } else {
            if (playerData.getCalledPlayers() - number < 0) {
                throw new TeamException(player + " хотел сделать -" + number + ", но он столько не звал");
            }
            playerData.setCalledPlayers(playerData.getCalledPlayers() - number);
        }
    }
}
