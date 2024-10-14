package trashsoftware.trashSnooker.fxml.widgets;

import trashsoftware.trashSnooker.core.PlayerHand;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.fxml.CareerView;
import trashsoftware.trashSnooker.util.DataLoader;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class PerkManager {

    public static final int AIMING = 1;  // 从1开始，连续；如果要改就要改很多地方
    public static final int CUE_PRECISION = 2;
    public static final int POWER = 3;
    public static final int POWER_CONTROL = 4;
    public static final int SPIN = 5;
    public static final int SPIN_CONTROL = 6;
//    public static final int ANTI_HAND = 7;
//    public static final int REST = 8;
    
    public static final int[] CATEGORIES = {
            AIMING, CUE_PRECISION, POWER, POWER_CONTROL, SPIN, SPIN_CONTROL
    };

    public static final int N_CATEGORIES = 16;

    private final CareerView parent;
    private PlayerPerson.ReadableAbility ability;
    private PlayerPerson.ReadableAbility previewAbility;

//    private Map<PlayerHand.Hand, PlayerPerson.ReadableAbilityHand> handAbilityMap;
//    private Map<PlayerHand.Hand, PlayerPerson.ReadableAbilityHand> previewHandAbilityMap;

    private int availPerks;
    private int cost;
    private final Map<Integer, Integer> generalAddedPerks = new TreeMap<>(Map.of(
            AIMING, 0
    ));
    private final Map<PlayerHand.Hand, Map<Integer, Integer>> handAddedPerks = new TreeMap<>();

    {
        for (PlayerHand.Hand hand : PlayerHand.Hand.values()) {
            Map<Integer, Integer> handMap = new TreeMap<>();
            for (int cat : new int[]{CUE_PRECISION, POWER, POWER_CONTROL, SPIN, SPIN_CONTROL}) {
                handMap.put(cat, 0);
            }
            handAddedPerks.put(hand, handMap);
        }
    }

    public PerkManager(CareerView parent, int availPerks, PlayerPerson.ReadableAbility ability) {
        this.availPerks = availPerks;
        this.parent = parent;

        setAbility(ability);
    }

    private void setAbility(PlayerPerson.ReadableAbility ability) {
        this.ability = ability;
        this.previewAbility = ability.clone();

//        handAbilityMap = Map.of(
//                PlayerHand.Hand.LEFT, ability.left,
//                PlayerHand.Hand.RIGHT, ability.right,
//                PlayerHand.Hand.REST, ability.rest
//        );
//        previewHandAbilityMap = Map.of(
//                PlayerHand.Hand.LEFT, previewAbility.left,
//                PlayerHand.Hand.RIGHT, previewAbility.right,
//                PlayerHand.Hand.REST, previewAbility.rest
//        );
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

    public PlayerPerson.ReadableAbilityHand getOriginalOf(PlayerHand.Hand hand) {
//        return handAbilityMap.get(hand);
        return switch (hand) {
            case LEFT -> ability.left;
            case RIGHT -> ability.right;
            case REST -> ability.rest;
        };
    }

    public PlayerPerson.ReadableAbilityHand getShownOf(PlayerHand.Hand hand) {
        return switch (hand) {
            case LEFT -> previewAbility.left;
            case RIGHT -> previewAbility.right;
            case REST -> previewAbility.rest;
        };
    }

    public int getAvailPerks() {
        return availPerks;
    }

//    public static int indexToCategory(int index) {
//        return index + 1;
//    }

//    public static int categoryToIndex(int cat) {
//        return cat - 1;
//    }

    boolean isGeneral(int cat) {
        return cat == AIMING;
    }

    public int addPerkTo(int cat, PlayerHand.Hand hand) {
        availPerks--;
        double afterAdd;
        if (isGeneral(cat)) {
            generalAddedPerks.put(cat, generalAddedPerks.get(cat) + 1);
            previewAbility.addPerks(cat, 1);
            afterAdd = previewAbility.getAbilityByCat(cat);
        } else {
            Map<Integer, Integer> handMap = handAddedPerks.get(hand);
            handMap.put(cat, handMap.get(cat) + 1);
            PlayerPerson.ReadableAbilityHand rahPreview = getShownOf(hand);
            rahPreview.addPerks(cat, 1);
            afterAdd = rahPreview.getAbilityByCat(cat);
        }

//        addedPerks[categoryToIndex(cat)] += 1;
//        previewAbility.addPerks(cat, 1);
//        double afterAdd = previewAbility.getAbilityByCat(cat);
        int moneySpent = moneySpent(afterAdd);
        cost += moneySpent;
        parent.notifyPerksChanged();
//        return addedPerks[categoryToIndex(cat)];
        return getPerksAddedTo(cat, hand);
    }

    public int getPerksAddedTo(int cat, PlayerHand.Hand hand) {
        if (isGeneral(cat)) {
            return generalAddedPerks.get(cat);
        } else {
            Map<Integer, Integer> handMap = handAddedPerks.get(hand);
            return handMap.get(cat);
        }
    }

    public int getPerksSelected() {
        int sum = generalAddedPerks.values().stream().reduce(0, Integer::sum);
        for (Map<Integer, Integer> handVal : handAddedPerks.values()) {
            sum += handVal.values().stream().reduce(0, Integer::sum);
        }
        return sum;
    }
    
    private int resetAddedPerks() {
        int sum = 0;
        for (var entry : generalAddedPerks.entrySet()) {
            sum += entry.setValue(0);
        }
        for (var handMap : handAddedPerks.values()) {
            for (var entry : handMap.entrySet()) {
                sum += entry.setValue(0);
            }
        }
        return sum;
    }

    public void clearSelections() {
        this.previewAbility = this.ability.clone();
        

        int sum = resetAddedPerks();

        availPerks += sum;
        cost = 0;
    }

    public int getCost() {
        return cost;
    }

    public UpgradeRec applyPerks() {
        Map<String, double[]> skillUpdateRec = new HashMap<>();
        
        for (var entry : generalAddedPerks.entrySet()) {
            int cat = entry.getKey();
            double past = ability.getAbilityByCat(cat);
            double upgraded = previewAbility.getAbilityByCat(cat);
            String name = PlayerPerson.ReadableAbility.getStringByCat(cat, null);
            skillUpdateRec.put(name, new double[]{past, upgraded});
        }
        for (var handEntry : handAddedPerks.entrySet()) {
            PlayerHand.Hand hand = handEntry.getKey();
            PlayerPerson.ReadableAbilityHand rahPast = getOriginalOf(hand);
            PlayerPerson.ReadableAbilityHand rahUpgraded = getShownOf(hand);
            for (var entry : handEntry.getValue().entrySet()) {
                int cat = entry.getKey();
                double past = rahPast.getAbilityByCat(cat);
                double upgraded = rahUpgraded.getAbilityByCat(cat);
                String name = PlayerPerson.ReadableAbility.getStringByCat(cat, hand);
                skillUpdateRec.put(name, new double[]{past, upgraded});
            }
        }

//        for (int i = 0; i < addedPerks.length; i++) {
//            if (addedPerks[i] != 0) {
//                int cat = indexToCategory(i);
//                double past = ability.getAbilityByCat(cat);
//                double upgraded = previewAbility.getAbilityByCat(cat);
//                String name = PlayerPerson.ReadableAbility.getStringByCat(cat);
//                skillUpdateRec.put(name, new double[]{past, upgraded});
//            }
//        }

        this.ability = previewAbility;
        DataLoader.getInstance().updatePlayer(this.ability.toPlayerPerson());

        this.previewAbility = this.ability.clone();
        int sum = resetAddedPerks();
        
//        for (int i = 0; i < addedPerks.length; i++) {
//            if (addedPerks[i] != 0) {
//                sum += addedPerks[i];
//                addedPerks[i] = 0;
//            }
//        }
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
