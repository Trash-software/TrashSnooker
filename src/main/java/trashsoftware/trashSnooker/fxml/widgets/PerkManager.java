package trashsoftware.trashSnooker.fxml.widgets;

import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.fxml.CareerView;
import trashsoftware.trashSnooker.util.DataLoader;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class PerkManager {

    public static final int AIMING = 1;  // 从1开始，连续；如果要改就要改很多地方
    public static final int CUE_PRECISION = 2;
    public static final int POWER = 3;
    public static final int POWER_CONTROL = 4;
    public static final int SPIN = 5;
    public static final int SPIN_CONTROL = 6;
    public static final int ANTI_HAND = 7;
    public static final int REST = 8;

    public static final int N_CATEGORIES = 8;

    private final CareerView parent;
    private PlayerPerson.ReadableAbility ability;
    private PlayerPerson.ReadableAbility previewAbility;
    private int availPerks;
    private int cost;
    private final int[] addedPerks = new int[8];

    public PerkManager(CareerView parent, int availPerks, PlayerPerson.ReadableAbility ability) {
        this.availPerks = availPerks;
        this.parent = parent;

        setAbility(ability);
    }

    private void setAbility(PlayerPerson.ReadableAbility ability) {
        this.ability = ability;
        this.previewAbility = ability.clone();
    }

    public void synchronizePerks() {
        clearSelections();
        availPerks = CareerManager.getInstance().getHumanPlayerCareer().getAvailablePerks();
    }

    public PlayerPerson.ReadableAbility getOriginalAbility() {
        return ability;
    }

    public PlayerPerson.ReadableAbility getShownAbility() {
        return previewAbility;
    }

    public int getAvailPerks() {
        return availPerks;
    }

    public static int indexToCategory(int index) {
        return index + 1;
    }

    public static int categoryToIndex(int cat) {
        return cat - 1;
    }

    public int addPerkTo(int cat) {
        availPerks--;
        addedPerks[categoryToIndex(cat)] += 1;
        previewAbility.addPerks(cat, 1);
        double afterAdd = previewAbility.getAbilityByCat(cat);
        int moneySpent = moneySpent(afterAdd);
        cost += moneySpent;
        parent.notifyPerksChanged();
        return addedPerks[categoryToIndex(cat)];
    }

    public int getPerksAddedTo(int cat) {
        return addedPerks[categoryToIndex(cat)];
    }
    
    public int getPerksSelected() {
        return Arrays.stream(addedPerks).sum();
    }

    public void clearSelections() {
        this.previewAbility = this.ability.clone();

        int sum = 0;
        for (int i = 0; i < addedPerks.length; i++) {
            if (addedPerks[i] != 0) {
                sum += addedPerks[i];
                addedPerks[i] = 0;
            }
        }
        availPerks += sum;
        cost = 0;
    }

    public int getCost() {
        return cost;
    }

    public UpgradeRec applyPerks() {
        Map<String, double[]> skillUpdateRec = new HashMap<>();

        for (int i = 0; i < addedPerks.length; i++) {
            if (addedPerks[i] != 0) {
                int cat = indexToCategory(i);
                double past = ability.getAbilityByCat(cat);
                double upgraded = previewAbility.getAbilityByCat(cat);
                String name = PlayerPerson.ReadableAbility.getStringByCat(cat);
                skillUpdateRec.put(name, new double[]{past, upgraded});
            }
        }

        this.ability = previewAbility;
        DataLoader.getInstance().updatePlayer(this.ability.toPlayerPerson());

        this.previewAbility = this.ability.clone();
        int sum = 0;
        for (int i = 0; i < addedPerks.length; i++) {
            if (addedPerks[i] != 0) {
                sum += addedPerks[i];
                addedPerks[i] = 0;
            }
        }
        UpgradeRec ur = new UpgradeRec(sum, cost, skillUpdateRec);
        cost = 0;
        return ur;
    }

    public static int moneySpent(double abilityAfterAdd) {
        if (abilityAfterAdd < 70) return 300;
        int tick = (int) ((abilityAfterAdd - 70) / 5);  // [0-6)
        return 350 + tick * tick * 50;
    }

    public record UpgradeRec(int perkUsed, int moneyCost, Map<String, double[]> abilityUpdated) {
    }
}
