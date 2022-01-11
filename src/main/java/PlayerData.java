public class PlayerData {

    private Status status = Status.NOT_READY;
    private int calledPlayers;

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
