package trashsoftware.trashSnooker.util.db;

import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.util.DataLoader;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class EntireGameTitle {
    
    public static final DateFormat FORMAT =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public final Timestamp startTime;
    public final GameRule gameRule;
    public final String player1Id;
    public final String player2Id;
    public final boolean player1isAi;
    public final boolean player2isAi;
    public final int totalFrames;
    public final String matchId;  // 在生涯模式中这场比赛的唯一ID；为null in快速游戏
    
    EntireGameTitle(Timestamp startTime, GameRule gameRule,
                    String player1Id, String player2Id,
                    boolean player1isAi, boolean player2isAi,
                    int totalFrames, String matchId) {
        this.startTime = startTime;
        this.gameRule = gameRule;
        this.player1Id = player1Id;
        this.player2Id = player2Id;
        this.player1isAi = player1isAi;
        this.player2isAi = player2isAi;
        this.totalFrames = totalFrames;
        this.matchId = matchId;
    }

    @Override
    public String toString() {
        return String.format("%s %s (%d) %s", 
                FORMAT.format(startTime),
                getP1Name(),
                totalFrames,
                getP2Name());
    }
    
    public String getP1Name() {
        return DataLoader.getInstance().getPlayerPerson(player1Id).getName();
    }
    
    public String getP2Name() {
        return DataLoader.getInstance().getPlayerPerson(player2Id).getName();
    }
}
