package trashsoftware.trashSnooker.core.ai;

import trashsoftware.trashSnooker.core.phy.Phy;

public class AiCuePref {
    private final Phy phy;
    private boolean mustAttack;
    
    public AiCuePref(Phy phy) {
        this.phy = phy;
    }

    public void setMustAttack(boolean mustAttack) {
        this.mustAttack = mustAttack;
    }

    public boolean isMustAttack() {
        return mustAttack;
    }

    public Phy getPhy() {
        return phy;
    }
}
