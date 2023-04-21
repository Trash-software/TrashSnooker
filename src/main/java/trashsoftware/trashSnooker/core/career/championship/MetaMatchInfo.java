package trashsoftware.trashSnooker.core.career.championship;

import trashsoftware.trashSnooker.core.career.ChampDataManager;
import trashsoftware.trashSnooker.core.career.ChampionshipData;
import trashsoftware.trashSnooker.core.career.ChampionshipStage;

public class MetaMatchInfo {
    
    public final String hostPlayerId;
    public final int year;
    public final ChampionshipData data;
    public final ChampionshipStage stage;
    public final int idNum;
    
    MetaMatchInfo(String hostPlayerId, int year, ChampionshipData data,
                  ChampionshipStage stage,
                  int idNum) {
        this.hostPlayerId = hostPlayerId;
        this.year = year;
        this.data = data;
        this.stage = stage;
        this.idNum = idNum;
    }
    
    public static MetaMatchInfo fromString(String matchId) {
        if (matchId == null) return null;

        try {
            String[] parts = matchId.split("-");
            String[] champInfo = parts[0].split("\\+");

            int year = Integer.parseInt(champInfo[1]);
            String dataId = champInfo[2];
            ChampionshipData data = ChampDataManager.getInstance().findDataById(dataId);
            
            if (parts.length == 2) {
                return new MetaMatchInfo(champInfo[0], year, data,
                        ChampionshipStage.PRE_ROUND_1,  // 将就
                        Integer.parseInt(parts[1]));
            }

            return new MetaMatchInfo(champInfo[0], year, data,
                    ChampionshipStage.valueOf(parts[1]),
                    Integer.parseInt(parts[2]));
        } catch (RuntimeException e) {
            System.err.println("Outdated match id format: " + matchId);
            return null;
        }
    }

    /**
     * @see Championship#uniqueId()
     * @see MatchTreeNode#generateId(Championship, ChampionshipStage)
     */
    public String toString() {
        return String.format("%s+%d+%s-%s-%d",
                hostPlayerId,
                year,
                data.getId(),
                stage.name(),
                idNum);
    }
    
    public String normalReadable() {
        return String.format("%d %s %s",
                year,
                data.getName(),
                stage.getShown());
    }
}
