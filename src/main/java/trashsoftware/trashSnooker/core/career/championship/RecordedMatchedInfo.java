package trashsoftware.trashSnooker.core.career.championship;

import trashsoftware.trashSnooker.core.career.ChampionshipData;
import trashsoftware.trashSnooker.core.career.ChampionshipStage;

public class RecordedMatchedInfo {
    
    public final String hostPlayerId;
    public final int year;
    public final ChampionshipData data;
    public final ChampionshipStage stage;
    public final int idNum;
    
    RecordedMatchedInfo(String hostPlayerId, int year, ChampionshipData data,
                        ChampionshipStage stage,
                        int idNum) {
        this.hostPlayerId = hostPlayerId;
        this.year = year;
        this.data = data;
        this.stage = stage;
        this.idNum = idNum;
    }
}
