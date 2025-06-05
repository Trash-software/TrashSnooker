package trashsoftware.trashSnooker.core.career;

import org.json.JSONObject;
import trashsoftware.trashSnooker.core.person.PlayerPerson;
import trashsoftware.trashSnooker.core.career.aiMatch.AiVsAi;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.util.Util;

import java.util.Calendar;

public abstract class CareerRanker {
    
    public final Career career;
    public final GameRule type;
    private int rankFrom1;
    
    protected CareerRanker(GameRule type, Career career) {
        this.career = career;
        this.type = type;
    }
    
    public static CareerRanker fromJson(JSONObject jsonObject, CareerManager careerManager) {
        int rank = jsonObject.getInt("rank");
        GameRule gameRule = GameRule.valueOf(jsonObject.getString("type"));
        Career career1 = careerManager.findCareerByPlayerId(jsonObject.getString("career"));  // todo: O(k*m*n) 循环
        String className = jsonObject.getString("ranker");

        CareerRanker ranker;
        if (ByTier.class.getSimpleName().equals(className)) {
            ranker = new ByTier(
                    gameRule, 
                    career1,
                    jsonObject.getInt("rankScore"),
                    jsonObject.getInt("totalWins"),
                    jsonObject.getInt("totalMatches"),
                    jsonObject.getInt("tier"),
                    jsonObject.getDouble("winRate")
            );
        } else if (ByAwards.class.getSimpleName().equals(className)) {
            ranker = new ByAwards(
                    gameRule,
                    career1,
                    jsonObject.getInt("oneSeasonAwards"),
                    jsonObject.getInt("twoSeasonsAwards"),
                    jsonObject.getInt("totalAwards")
            );
        } else {
            throw new RuntimeException("No such ranker: " + className);
        }
        
        ranker.setRankFrom1(rank);
        
        return ranker;
    }

    public void setRankFrom1(int rankFrom1) {
        this.rankFrom1 = rankFrom1;
    }

    public int getRankFrom1() {
        return rankFrom1;
    }

    /**
     * 可能存在球员看不起小比赛的情况
     *
     * @param selfRanking 本人的排名，从0计
     * @param front       前一位的，如果本人是冠军则null
     * @param back        后一位的，如果本人是垫底则null
     */
    public boolean willJoinMatch(ChampionshipData data, int selfRanking,
                                 CareerRanker front, CareerRanker back) {

        if (career.isHumanPlayer()) return true;  // 我们无权替真人玩家决定

        if ("God".equals(career.getPlayerPerson().category)) return false;  // Master别出来打比赛
        if (!career.getPlayerPerson().isPlayerOf(data.type)) return false;  // 不是玩这个的

        if (selfRanking < 16) {
            int champAwd = data.getAwardByRank(ChampionshipScore.Rank.CHAMPION);

            int selfAwd = rankedScore(data.getSelection());

            if (data.getClassLevel() <= 2) return true;  // 重要比赛，要去

            double mustJoinRatio = selfAwd * 0.2;
            if (champAwd >= mustJoinRatio) return true;  // 大比赛，要去

            if (!data.isRanked() && data.getClassLevel() >= 4) return false;  // 小的非排名赛，算了吧

            int frontAwd = front == null ? Integer.MAX_VALUE : front.rankedScore(data.getSelection());
            int backAwd = back == null ? 0 : back.rankedScore(data.getSelection());

            if (selfAwd + champAwd < frontAwd) {
                return false;  // 拿了冠军也追不上前一名
            }
            if (backAwd + champAwd < selfAwd) {
                return false;  // 后一名拿了冠军也追不上我
            }
        }
        return true;
    }
    
    /**
     * 返回排位的依据，越大越靠前
     */
    public abstract int rankedScore();
    
    protected abstract void fillJson(JSONObject json);
    
    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        json.put("career", career.getPlayerPerson().getPlayerId());
        json.put("type", type.name());
        json.put("ranker", getClass().getSimpleName());
        json.put("rank", rankFrom1);
        
        fillJson(json);
        
        return json;
    }
    
    public int rankedScore(ChampionshipData.Selection selection) {
        return rankedScore();
    }

    protected double winScore() {
        return AiVsAi.playerSimpleWinningScore(
                career.getPlayerPerson(),
                career.getPlayerPerson().getAiPlayStyle(),
                PlayerPerson.ReadableAbility.fromPlayerPerson(career.getPlayerPerson()),
                false
        );
    }

    public static class ByAwards extends CareerRanker {

        private int oneSeasonAwards;
        private int twoSeasonsAwards;
        private int totalAwards;

        protected ByAwards(GameRule type, Career career, Calendar timestamp) {
            super(type, career);
            
            calculateAwards(timestamp);
        }

        private ByAwards(GameRule type, Career career, 
                         int oneSeasonAwards,
                         int twoSeasonsAwards,
                         int totalAwards) {
            super(type, career);

            this.oneSeasonAwards = oneSeasonAwards;
            this.twoSeasonsAwards = twoSeasonsAwards;
            this.totalAwards = totalAwards;
        }

        public static int twoSeasonsCompare(ByAwards t, ByAwards o) {
            return compare(t, o, t.twoSeasonsAwards, o.twoSeasonsAwards);
        }

        public static int oneSeasonCompare(ByAwards t, ByAwards o) {
            return compare(t, o, t.oneSeasonAwards, o.oneSeasonAwards);
        }

        private static int compare(ByAwards t, ByAwards o,
                                   int tAwd, int oAwd) {
            int awdCmp = -Integer.compare(tAwd, oAwd);
            if (awdCmp != 0) return awdCmp;
            if ("God".equals(t.career.getPlayerPerson().category) && 
                    !"God".equals(o.career.getPlayerPerson().category)) {
                return 1;
            }
            if ("God".equals(o.career.getPlayerPerson().category) && 
                    !"God".equals(t.career.getPlayerPerson().category)) {
                return -1;
            }
            int rndCmp = Boolean.compare(t.career.getPlayerPerson().isRandom, 
                    o.career.getPlayerPerson().isRandom);
            if (rndCmp != 0) return rndCmp;
            return -Double.compare(t.winScore(), o.winScore());
        }

        @Override
        public int rankedScore() {
            return getTwoSeasonsAwards();
        }

        @Override
        public int rankedScore(ChampionshipData.Selection selection) {
            return getEffectiveAward(selection);
        }

        @Override
        protected void fillJson(JSONObject json) {
            json.put("oneSeasonAwards", oneSeasonAwards);
            json.put("twoSeasonsAwards", twoSeasonsAwards);
            json.put("totalAwards", totalAwards);
        }

        private void calculateAwards(Calendar timestamp) {
            oneSeasonAwards = 0;
            twoSeasonsAwards = 0;
            totalAwards = 0;

            Calendar twoYearBefore = Calendar.getInstance();
            twoYearBefore.set(timestamp.get(Calendar.YEAR) - 2,
                    timestamp.get(Calendar.MONTH),
                    timestamp.get(Calendar.DAY_OF_MONTH) - 1);  // 上上届要算

            Calendar oneYearBefore = Calendar.getInstance();
            oneYearBefore.set(timestamp.get(Calendar.YEAR) - 1,
                    timestamp.get(Calendar.MONTH),
                    timestamp.get(Calendar.DAY_OF_MONTH) - 1);  // 上上届要算

            for (ChampionshipScore score : career.getChampionshipScores()) {
                int rankAwards = 0;
                int completeAwards = 0;
                if (score.data.type == type) {
                    for (ChampionshipScore.Rank rank : score.ranks) {
                        int awd = score.data.getAwardByRank(rank);
                        // todo: 没考虑平分奖金，如单杆最高、满分杆
                        completeAwards += awd;
                        if (rank.ranked) rankAwards += awd;
                    }
                    totalAwards += completeAwards;
                    if (score.data.ranked) {
                        if (oneYearBefore.before(score.timestamp)) {
                            oneSeasonAwards += rankAwards;
                        }
                        if (twoYearBefore.before(score.timestamp)) {
                            twoSeasonsAwards += rankAwards;
                        }
                    }
                }
            }
        }

        public int getEffectiveAward(ChampionshipData.Selection selection) {
            switch (selection) {
                case REGULAR:
                case ALL_CHAMP:
                default:
                    return twoSeasonsAwards;
                case SINGLE_SEASON:
                    return oneSeasonAwards;
            }
        }

        public int getTwoSeasonsAwards() {
            return twoSeasonsAwards;
        }

        public int getOneSeasonAwards() {
            return oneSeasonAwards;
        }

        public int getTotalAwards() {
            return totalAwards;
        }

        @Override
        public String toString() {
            return career.getPlayerPerson().getPlayerId() + ": " + twoSeasonsAwards;
        }
    }
    
    public static class ByTier extends CareerRanker {
        
        private int rankScore;
        private int totalWins;
        private int totalMatches;
        private int tier;
        private double winRate;
        
        protected ByTier(GameRule type, Career career, Calendar endTime) {
            super(type, career);
            
            calculate(endTime);
        }

        private ByTier(GameRule type, Career career, 
                       int rankScore, int totalWins, int totalMatches, int tier, double winRate) {
            super(type, career);

            this.rankScore = rankScore;
            this.totalWins = totalWins;
            this.totalMatches = totalMatches;
            this.tier = tier;
            this.winRate = winRate;
        }

        @Override
        public int rankedScore() {
            return rankScore;
        }

        @Override
        protected void fillJson(JSONObject json) {
            json.put("rankScore", rankScore);
            json.put("totalWins", totalWins);
            json.put("totalMatches", totalMatches);
            json.put("tier", tier);
            json.put("winRate", winRate);
        }

        /**
         * 仅对职业7档以上生效
         */
        public static int computeTier(int rankFrom0, double winRate, int totalTierPlayers) {
            if (winRate >= 0.7) {
                if (rankFrom0 < 3) return 11;
                else return 10;
            }
            if (winRate >= 0.6) {
                if (rankFrom0 < 8) return 10;
                else return 9;
            }
            if (winRate >= 0.55) {
                if (rankFrom0 < 18) return 9;
                else return 8;
            }
            if (winRate >= 0.5) {
                if (rankFrom0 < 36) return 8;
                else return 7;
            }
//            if (winRate > 0.4) {
//                return 7;
//            }
            return 7;
//            
//            
//            return fullTier(rankFrom0 + Math.max(0, CareerManager.TIER_LIMIT - totalTierPlayers));
        }

        public void setTier(int tier) {
            this.tier = tier;
        }

        public int getTier() {
            return tier;
        }
        
        public static int compareWithTierSet(ByTier t, ByTier o) {
            int tierCmp = -Integer.compare(t.tier, o.tier);
            if (tierCmp != 0) return tierCmp;
            else return roughCompare(t, o);
        }

        public static int roughCompare(ByTier t, ByTier o) {
            int tTier = t.rankScore;
            int oTier = o.rankScore;
            
            int awdCmp = -Integer.compare(tTier, oTier);
            if (awdCmp != 0) return awdCmp;
            if ("God".equals(t.career.getPlayerPerson().category) &&
                    !"God".equals(o.career.getPlayerPerson().category)) {
                return 1;
            }
            if ("God".equals(o.career.getPlayerPerson().category) &&
                    !"God".equals(t.career.getPlayerPerson().category)) {
                return -1;
            }
            int rndCmp = Boolean.compare(t.career.getPlayerPerson().isRandom,
                    o.career.getPlayerPerson().isRandom);
            if (rndCmp != 0) return rndCmp;
            return -Double.compare(t.winScore(), o.winScore());
        }
        
        private void calculate(Calendar endTime) {
            rankScore = 0;
            totalWins = 0;
            totalMatches = 0;
            
            for (ChampionshipScore score : career.getChampionshipScores()) {
                if (!score.data.ranked) continue;
                if (endTime != null) {
                    var withYear = score.data.getWithYear(score.getYear());
                    Calendar champTime = withYear.toCalendar();
                    if (champTime.after(endTime)) continue;  // 不算到这么晚的比赛
                }
                
                ChampionshipScore.Rank[] allRanks = score.data.ranksOfLosers;
                int mainRounds = 0;
                for (int i = 0; i < allRanks.length; i++) {
                    if (!allRanks[i].isMain) {
                        mainRounds = i;
                        break;
                    }
                }
                
                for (ChampionshipScore.Rank rank : score.ranks) {
                    if (rank.ranked) {
                        if (rank == ChampionshipScore.Rank.CHAMPION) {
                            totalMatches += mainRounds;
                            totalWins += mainRounds;
                            break;
                        } else {
                            int index = Util.indexOf(rank, allRanks);
                            if (index != -1) {
                                // 逻辑：是什么rank，就相当于赢了多少场-1
                                // 正赛选手相当于自动赢了资格赛
                                int matchesCount = Math.max(0, mainRounds - index);
                                int winsCount = Math.max(0, matchesCount - 1);
                                
                                totalMatches += matchesCount;
                                totalWins += winsCount;

                                break;  // 一场score肯定就只有一次，其他的都是一些乱七八糟的，比如单杆最高（中八还没有）
                            }
                        }
                    }
                }
            }
            
            winRate = totalMatches == 0 ? 0 : (double) totalWins / totalMatches;
            
            if (totalMatches < 10) {
                rankScore = totalWins;
            } else {
                rankScore = (int) (10 + Math.round(winRate * 100));
            }
        }
        
        public boolean canHaveTier() {
            return totalMatches >= 6 && winRate >= 0.33;
        }

        public double getWinRate() {
            return winRate;
        }

        public int getTotalMatches() {
            return totalMatches;
        }

        public int getTotalWins() {
            return totalWins;
        }
    }
}
