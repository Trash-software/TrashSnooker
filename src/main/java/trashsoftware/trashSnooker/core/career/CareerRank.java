package trashsoftware.trashSnooker.core.career;

import javafx.fxml.FXML;

public class CareerRank {
    
    public final int rank;
    public final Career career;
    private final int shownAwards;
    private final int totalAwards;
    
    CareerRank(int rank, Career career, int shownAwards, int totalAwards) {
        this.rank = rank;
        this.career = career;
        this.shownAwards = shownAwards;
        this.totalAwards = totalAwards;
    }

    public Career getCareer() {
        return career;
    }
    
    public int getRankFrom1() {
        return rank + 1;
    }

    @FXML
    public int getShownAwards() {
        return shownAwards;
    }
    
    @FXML
    public int getTotalAwards() {
        return totalAwards;
    }
}
