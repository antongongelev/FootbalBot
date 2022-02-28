package ru.telegrambot.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.telegrambot.domain.Team;

@Service
public class TeamService {
    @Autowired
    private TeamRepository database;

    public TeamService(TeamRepository db) {
        database = db;
    }

    public void save(Team origin) throws JsonProcessingException {
        TeamEntity entity = new TeamEntity();
        entity.value = origin.serialize();
        database.deleteAll();
        database.save(entity);
    }

    public Team load() throws JsonProcessingException {
        Iterable<TeamEntity> all = database.findAll();
        if (!all.iterator().hasNext()) {
            return new Team();
        }
        TeamEntity firstAndLast = all.iterator().next();
        return new Team(firstAndLast.value);
    }
}
