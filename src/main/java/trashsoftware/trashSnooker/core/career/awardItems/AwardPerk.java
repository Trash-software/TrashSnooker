package trashsoftware.trashSnooker.core.career.awardItems;

public class AwardPerk extends AwardMaterial {
    private final int nPerks;

    public AwardPerk(int nPerks) {
        this.nPerks = nPerks;
    }

    public int getPerks() {
        return nPerks;
    }

    @Override
    public String toString() {
        return String.valueOf(nPerks);
    }
}
