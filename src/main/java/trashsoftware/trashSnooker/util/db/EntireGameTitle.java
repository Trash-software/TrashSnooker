package trashsoftware.trashSnooker.util.db;

import trashsoftware.trashSnooker.core.GameType;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class EntireGameTitle {
    
    public static final DateFormat FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public final Timestamp startTime;
    public final GameType gameType;
    public final String player1Name;
    public final String player2Name;
    public final boolean player1isAi;
    public final boolean player2isAi;
    public final int totalFrames;
    
    EntireGameTitle(Timestamp startTime, GameType gameType,
                    String player1Name, String player2Name,
                    boolean player1isAi, boolean player2isAi, 
                    int totalFrames) {
        this.startTime = startTime;
        this.gameType = gameType;
        this.player1Name = player1Name;
        this.player2Name = player2Name;
        this.player1isAi = player1isAi;
        this.player2isAi = player2isAi;
        this.totalFrames = totalFrames;
    }

    @Override
    public String toString() {
        return String.format("%s %s (%d) %s", 
                FORMAT.format(startTime),
                player1Name,
                totalFrames,
                player2Name);
    }
}
