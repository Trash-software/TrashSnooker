package trashsoftware.trashSnooker.core.career.aiMatch;

import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.career.Career;
import trashsoftware.trashSnooker.core.career.ChampionshipData;

public class ChineseAiVsAi extends AiVsAi {
    public ChineseAiVsAi(Career p1, Career p2, ChampionshipData data, int totalFrames) {
        super(p1, p2, data, totalFrames);
    }

    @Override
    protected void simulateOneFrame(boolean isFinalFrame) {
        gaussianRandom(isFinalFrame);
    }
}
