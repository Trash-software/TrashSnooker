package trashsoftware.trashSnooker.core.career;

public enum ChampionshipStage {
        FINAL("决赛", true),
        SEMI_FINAL("半决赛", true),
        QUARTER_FINAL("四分之一决赛", true),
        ROUND_3("第三轮", true),
        ROUND_2("第二轮", true),
        ROUND_1("第一轮", true),
        PRE_ROUND_3("预选赛第三轮", false),
        PRE_ROUND_2("预选赛第二轮", false),
        PRE_ROUND_1("预选赛第一轮", false);

        public final String shown;
        public final boolean isMain;  // 是否为正赛

        ChampionshipStage(String shown, boolean isMain) {
            this.shown = shown;
            this.isMain = isMain;
        }

//        @Override
//        public String toString() {
//            return shown;
//        }
    }