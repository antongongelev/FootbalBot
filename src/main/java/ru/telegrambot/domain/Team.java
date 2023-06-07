package ru.telegrambot.domain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Team {

    private HashMap<String, PlayerData> team = new HashMap<>();
    private final ObjectMapper mapper = new ObjectMapper();

    public Team() {

    }

    public Team(String jsonInput) throws JsonProcessingException {
        TypeReference<HashMap<String, PlayerData>> typeRef
                = new TypeReference<HashMap<String, PlayerData>>() {
        };
        team = mapper.readValue(jsonInput, typeRef);
    }

    public String getTeamReport(String footballDay, boolean isStrictEnroll) {
        return "Состав на " + footballDay + ": " + System.lineSeparator()
                + Constants.DELIMITER + System.lineSeparator()
                + getPlayers(isStrictEnroll)
                + Constants.DELIMITER + System.lineSeparator()
                + "Итого: " + getTotal();
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

    private String getPlayers(boolean isStrictEnroll) {
        StringBuilder builder = new StringBuilder();
        List<Map.Entry<String, PlayerData>> relevantPlayers = team.entrySet()
                                                                  .stream()
                                                                  .filter(this::isRelevantPlayer)
                                                                  .sorted(Comparator.comparingLong(o -> o.getValue().getTimestamp()))
                                                                  .collect(Collectors.toList());

        for (int i = 0; i < relevantPlayers.size(); i++) {
            builder.append(getPlayerReport(relevantPlayers.get(i), i, isStrictEnroll));
        }
        if (StringUtils.isEmpty(builder.toString())) {
            builder.append("Пока никого...").append(System.lineSeparator());
        }
        return builder.toString();
    }

    private boolean isRelevantPlayer(java.util.Map.Entry<String, PlayerData> player) {
        return (Status.READY == player.getValue().getStatus() || Status.DOES_NOT_KNOW == player.getValue().getStatus()) || player.getValue().getCalledPlayers() > 0;
    }

    private String getPlayerReport(Map.Entry<String, PlayerData> player, int i, boolean isStrictEnroll) {
        int called = player.getValue().getCalledPlayers();
        return ++i +
                ". " +
                player.getKey() +
                (isStrictEnroll ? "" : (" -> " + player.getValue().getStatus().getStatus() + (called > 0 ? ". Позвал +" + called : ""))) +
                System.lineSeparator();
    }

    public String addSelf(String player) {
        PlayerData playerData = team.get(player);
        if (playerData == null) {
            PlayerData data = new PlayerData(Status.READY);
            team.put(player, data);
            return player + " вписался. Итого: " + getTotal();
        } else {
            Status status = playerData.getStatus();
            if (Status.READY == status) {
                return player + " попытался вписаться, хотя уже был вписан";
            }
            playerData.setStatus(Status.READY);
            // обновим время записи
            playerData.setTimestamp(System.currentTimeMillis());
            if (Status.CALLED_FRIENDS == status) {
                return player + " вписался. Итого: " + getTotal();
            }
            return player + " поменял статус с '" + status.getStatus() + "' на '" + Status.READY.getStatus() + "'. Итого: " + getTotal();
        }
    }

    public String doNotKnow(String player) {
        PlayerData playerData = team.get(player);
        if (playerData == null) {
            PlayerData data = new PlayerData(Status.DOES_NOT_KNOW);
            team.put(player, data);
            return player + " под вопросом. Итого: " + getTotal();
        } else {
            Status status = playerData.getStatus();
            if (Status.DOES_NOT_KNOW == status) {
                return player + " усомнился что придет, хотя и так не был уверен";
            }
            playerData.setStatus(Status.DOES_NOT_KNOW);
            if (Status.CALLED_FRIENDS == status) {
                return player + " под вопросом. Итого: " + getTotal();
            }
            return player + " поменял статус с '" + status.getStatus() + "' на '" + Status.DOES_NOT_KNOW.getStatus() + "'. Итого: " + getTotal();
        }
    }

    public String removeMe(String player) {
        PlayerData playerData = team.get(player);
        if (playerData == null) {
            PlayerData data = new PlayerData(Status.NOT_READY);
            team.put(player, data);
            return player + " слился. Итого: " + getTotal();
        } else {
            Status status = playerData.getStatus();
            if (Status.NOT_READY == status) {
                return player + " попытался слиться, хотя и не собирался приходить";
            }
            playerData.setStatus(Status.NOT_READY);
            if (Status.CALLED_FRIENDS == status) {
                return player + " слился. Итого: " + getTotal();
            }
            return player + " поменял статус с '" + status.getStatus() + "' на '" + Status.NOT_READY.getStatus() + "'. Итого: " + getTotal();
        }
    }

    public String addFriends(String player, int number) {
        PlayerData playerData = team.get(player);
        if (playerData == null) {
            PlayerData data = new PlayerData(Status.CALLED_FRIENDS);
            data.setCalledPlayers(data.getCalledPlayers() + number);
            team.put(player, data);
        } else {
            playerData.setCalledPlayers(playerData.getCalledPlayers() + number);
        }
        return player + " сделал +" + number + ". Итого: " + getTotal();
    }

    public String removeFriends(String player, int number) {
        PlayerData playerData = team.get(player);
        if (playerData == null) {
            return player + " хотел сделать -" + number + ", но он столько не звал";
        } else {
            if (playerData.getCalledPlayers() - number < 0) {
                return player + " хотел сделать -" + number + ", но он столько не звал";
            }
            playerData.setCalledPlayers(playerData.getCalledPlayers() - number);
            return player + " сделал -" + number + ". Итого: " + getTotal();
        }
    }

    public HashMap<String, PlayerData> getTeam() {
        return team;
    }

    public String serialize() throws JsonProcessingException {
        return mapper.writerWithDefaultPrettyPrinter()
                     .writeValueAsString(team);
    }
}
