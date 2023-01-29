package trashsoftware.trashSnooker.core.career;

public class CareerRank {
    
    public final int rank;
    public final Career career;
    public final int recentAwards;
    
    CareerRank(int rank, Career career, int recentAwards) {
        this.rank = rank;
        this.career = career;
        this.recentAwards = recentAwards;
    }

    public Career getCareer() {
        return career;
    }
    
    public int getRankFrom1() {
        return rank + 1;
    }

    public int getRecentAwards() {
        return recentAwards;
    }
}
