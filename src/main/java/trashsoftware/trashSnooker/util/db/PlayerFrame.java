package trashsoftware.trashSnooker.util.db;

import java.util.Objects;

public class PlayerFrame {
    public final String playerName;
    public final int frameIndex;
    
    PlayerFrame(String playerName, int frameIndex) {
        this.playerName = playerName;
        this.frameIndex = frameIndex;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerFrame that = (PlayerFrame) o;
        return frameIndex == that.frameIndex && Objects.equals(playerName, that.playerName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerName, frameIndex);
    }
}
