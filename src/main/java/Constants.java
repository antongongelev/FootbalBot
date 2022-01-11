import java.time.DayOfWeek;
import java.util.regex.Pattern;

public interface Constants {

    String BOT_NAME = "CSITeamBot";
    String BOT_TOKEN = "5007880884:AAHWldgd6GneCSD_B86pV2xIz7XljF9YZ_c";
    String TEAM = "СОСТАВ";
    String HELP = "/HELP";
    String ADD_ME = "+";
    String REMOVE_ME = "-";
    String DO_NOT_KNOW = "?";
    DayOfWeek DAY_OF_WEEK = DayOfWeek.WEDNESDAY;
    String CHAT_NAME = "333";
    Pattern MINUS_PATTERN = Pattern.compile("[-][1-9]");
    Pattern PLUS_PATTERN = Pattern.compile("[+][1-9]");
}
