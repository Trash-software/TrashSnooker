package trashsoftware.trashSnooker.core.career;

public class TourCareer {
    
    public final Career career;
    public final int seedNum;  // 几号种子
    
    TourCareer(Career career, int seedNum) {
        this.career = career;
        this.seedNum = seedNum;
    }

    @Override
    public String toString() {
        return "[" + seedNum + "]:" + career.getPlayerPerson().getPlayerId();
    }
}
