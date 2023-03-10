package trashsoftware.trashSnooker.core.career;

import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.util.Util;

public enum ChampionshipStage {
    FINAL(true),
    SEMI_FINAL(true),
    QUARTER_FINAL(true),
    ROUND_3(true),
    ROUND_2(true),
    ROUND_1(true),
    PRE_ROUND_4(false),
    PRE_ROUND_3(false),
    PRE_ROUND_2(false),
    PRE_ROUND_1(false);

    public final boolean isMain;  // 是否为正赛

    ChampionshipStage(boolean isMain) {
        this.isMain = isMain;
    }

    public static ChampionshipStage[] getSequence(int mainRounds, int preRounds) {
        int rounds = mainRounds + preRounds;
        int mainSkip = ROUND_1.ordinal() - mainRounds + 1;
        int preSkip = PRE_ROUND_1.ordinal() - preRounds + 1;

        ChampionshipStage[] res = new ChampionshipStage[mainRounds + preRounds];
        for (int i = 0; i < rounds; i++) {
            if (i <= 2) {
                res[i] = values()[i];
            } else if (i < mainRounds) {
                res[i] = values()[mainSkip + i];
            } else {
                res[i] = values()[preSkip + i - mainRounds];
            }
        }
        return res;
    }

    public String getShown() {
        return App.getStrings().getString(Util.toLowerCamelCase(name()));
    }

    //        @Override
//        public String toString() {
//            return shown;
//        }
}