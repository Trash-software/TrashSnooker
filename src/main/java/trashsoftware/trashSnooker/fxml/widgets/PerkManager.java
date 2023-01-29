package trashsoftware.trashSnooker.fxml.widgets;

import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.fxml.CareerView;

public class PerkManager {
    
    public static final int AIMING = 1;  // 从1开始，连续；如果要改就要改很多地方
    public static final int CUE_PRECISION = 2;
    public static final int POWER = 3;
    public static final int POWER_CONTROL = 4;
    public static final int SPIN = 5;
    public static final int SPIN_CONTROL = 6;
    public static final int ANTI_HAND = 7;
    public static final int REST = 8;

    private CareerView parent;
    private PlayerPerson.ReadableAbility ability;
    private int availPerks;
    private int[] addedPerks = new int[8];

    public PerkManager(CareerView parent, int availPerks) {
        this.availPerks = availPerks;
        this.parent = parent;
    }

    public void setAbility(PlayerPerson.ReadableAbility ability) {
        this.ability = ability;
    }

    public int getAvailPerks() {
        return availPerks;
    }
    
    public int addPerkTo(int cat) {
        availPerks--;
        addedPerks[cat - 1] += 1;
        parent.noticePerksChanged();
        return addedPerks[cat - 1];
    }
    
    public int getPerksAddedTo(int cat) {
        return addedPerks[cat - 1];
    }
    
    public void clearSelections() {
        int sum = 0;
        for (int i = 0; i < addedPerks.length; i++) {
            if (addedPerks[i] != 0) {
                sum += addedPerks[i];
                addedPerks[i] = 0;
            }
        }
        availPerks += sum;
    }
    
    public int applyPerks() {
        int sum = 0;
        for (int i = 0; i < addedPerks.length; i++) {
            if (addedPerks[i] != 0) {
                sum += addedPerks[i];
                ability.addPerks(i + 1, addedPerks[i]);
                addedPerks[i] = 0;
            }
        }
        return sum;
    }

    public PlayerPerson.ReadableAbility getAbility() {
        return ability;
    }
}
