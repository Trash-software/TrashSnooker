package trashsoftware.trashSnooker.core.career;

import javafx.fxml.FXML;
import trashsoftware.trashSnooker.util.Util;

public class RankedCareer {

    public final int rank;  // rank from 0
    public final Career career;
    private int tier;
    private final int totalWins;
    private final int totalMatches;
    private final double winRate;

    private final int shownAwards;
    private final int totalAwards;

    private RankedCareer(int rank,
                         Career career,
                         int totalWins,
                         int totalMatches,
                         double winRate,
                         int shownAwards,
                         int totalAwards) {
        this.rank = rank;
        this.career = career;
        this.totalWins = totalWins;
        this.totalMatches = totalMatches;
        this.winRate = winRate;

        this.shownAwards = shownAwards;
        this.totalAwards = totalAwards;
    }

    static RankedCareer createByAwards(int rank, Career career, int shownAwards, int totalAwards) {
        return new RankedCareer(rank,
                career,
                0,
                0,
                0,
                shownAwards,
                totalAwards);
    }

    static RankedCareer createByTier(int rank, Career career, int totalWins, int totalMatches,
                                     double winRate) {
        return new RankedCareer(
                rank,
                career,
                totalWins,
                totalMatches,
                winRate,
                0,
                0
        );
    }

    public Career getCareer() {
        return career;
    }

    @FXML
    public int getRankFrom1() {
        return rank + 1;
    }

    @FXML
    public String getShownAwards() {
        return Util.moneyToReadable(shownAwards);
    }

    @FXML
    public String getTotalAwards() {
        return Util.moneyToReadable(totalAwards);
    }

    public void setTier(int tier) {
        this.tier = tier;
    }
    
    public int getTierNum() {
        return tier;
    }

    @FXML
    public String getTier() {
        return tier == 0 ? "-" : String.valueOf(tier);
    }

    @FXML
    public String getTotalWins() {
        return String.format("%,d/%,d", totalWins, totalMatches);
    }

    public String getWinRate() {
        return String.format("%.2f%%", winRate * 100);
    }

    public int getTotalMatchesNum() {
        return totalMatches;
    }
    
    public double getWinRateNum() {
        return winRate;
    }
}
