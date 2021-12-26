package trashsoftware.trashSnooker.core;

public enum PlayerType {
    PLAYER("玩家"),
    COMPUTER("电脑");

    private final String shown;

    PlayerType(String shown) {
        this.shown = shown;
    }

    @Override
    public String toString() {
        return shown;
    }
}
