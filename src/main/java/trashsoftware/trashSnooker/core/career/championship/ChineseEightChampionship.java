package trashsoftware.trashSnooker.core.career.championship;

import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.ChampionshipData;
import trashsoftware.trashSnooker.core.career.TourCareer;

import java.util.Calendar;
import java.util.List;

public class ChineseEightChampionship extends Championship {

    public ChineseEightChampionship(ChampionshipData data, Calendar timestamp) {
        super(data, timestamp);
    }

    @Override
    protected List<TourCareer> getParticipantsByRank(boolean playerJoin, boolean humanQualified) {
        return CareerManager.getInstance().participants(
                data,
                playerJoin,
                humanQualified
        );
    }
}
