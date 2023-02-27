package trashsoftware.trashSnooker.core.career.championship;

import trashsoftware.trashSnooker.core.career.Career;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.ChampionshipData;

import java.util.Calendar;
import java.util.List;

public class SnookerChampionship extends Championship {
    
    public SnookerChampionship(ChampionshipData data, Calendar timestamp) {
        super(data, timestamp);
    }

    @Override
    protected List<Career> getParticipantsByRank(boolean playerJoin) {
        return CareerManager.getInstance().participants(
                data.getTotalPlaces(),
                data.isProfessionalOnly(),
                playerJoin,
                data.getType(),
                data.getSelection()
        );
    }
}
