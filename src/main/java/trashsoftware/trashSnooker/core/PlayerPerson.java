package trashsoftware.trashSnooker.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.ai.AiPlayStyle;
import trashsoftware.trashSnooker.fxml.widgets.PerkManager;

import java.util.*;

public class PlayerPerson {

    public final String category;
    public final HandBody handBody;
    public final double psy;
    private final String playerId;
    private final String name;
    private final double maxPowerPercentage;
    private final double controllablePowerPercentage;
    private final double maxSpinPercentage;
    private final double precisionPercentage;
    private final double anglePrecision;
    private final double longPrecision;
    private final List<Cue> privateCues = new ArrayList<>();
    private final double solving;
    private final double minPullDt;
    private final double maxPullDt;
    private final double aimingOffset;  // 瞄和打的偏差，正值向右偏
    private final double powerControl;
    // 横向的mu，横向的sigma，纵向的mu，纵向的sigma
    // 横向的左塞负右塞正，纵向的高杆正低杆负
    private final double[] cuePointMuSigmaXY;
    private final double cueSwingMag;
    private final CuePlayType cuePlayType;
    private final AiPlayStyle aiPlayStyle;
    private boolean isCustom;
    public final boolean isRandom;

    public PlayerPerson(String playerId,
                        String name,
                        String category,
                        double maxPowerPercentage,
                        double controllablePowerPercentage,
                        double maxSpinPercentage,
                        double precisionPercentage,
                        double anglePrecision,
                        double longPrecision,
                        double solving,
                        double minPullDt,
                        double maxPullDt,
                        double aimingOffset,
                        double cueSwingMag,
                        double[] cuePointMuSigmaXY,
                        double powerControl,
                        double psy,
                        CuePlayType cuePlayType,
                        @Nullable AiPlayStyle aiPlayStyle,
                        HandBody handBody) {
        this.playerId = playerId;
        this.name = name;
        this.category = category;
        this.maxPowerPercentage = maxPowerPercentage;
        this.controllablePowerPercentage = controllablePowerPercentage;
        this.maxSpinPercentage = maxSpinPercentage;
        this.precisionPercentage = precisionPercentage;
        this.anglePrecision = anglePrecision;
        this.longPrecision = longPrecision;
        this.solving = solving;
        this.minPullDt = minPullDt;
        this.maxPullDt = maxPullDt;
        this.aimingOffset = aimingOffset;
        this.cueSwingMag = cueSwingMag;
        this.cuePointMuSigmaXY = cuePointMuSigmaXY;
        this.powerControl = powerControl;
        this.psy = psy;
        this.cuePlayType = cuePlayType;
        this.aiPlayStyle = aiPlayStyle == null ? deriveAiStyle() : aiPlayStyle;
        this.handBody = handBody;
        
        this.isRandom = this.playerId.startsWith("random_");
    }

    public PlayerPerson(String playerId,
                        String name,
                        double maxPowerPercentage,
                        double controllablePowerPercentage,
                        double maxSpinPercentage,
                        double aimingPercentage,
                        double cuePrecision,  // 出杆歪不歪
                        double anglePrecision,
                        double longPrecision,
                        double powerControl,
                        double spinControl,  // 出杆挑不挑
                        AiPlayStyle aiPlayStyle,
                        boolean isCustom,
                        HandBody handBody) {
        this(
                playerId,
                name,
                estimateCategory(aimingPercentage, powerControl, spinControl),
                maxPowerPercentage,
                controllablePowerPercentage,
                maxSpinPercentage,
                aimingPercentage,
                anglePrecision,
                longPrecision,
                (aimingPercentage + spinControl) / 2,
                50.0,
                200.0,
                0.0,
                100.0,
                estimateCuePoint(cuePrecision, spinControl),
                powerControl,
                90,
                CuePlayType.DEFAULT_PERFECT,
                aiPlayStyle,
                handBody == null ? HandBody.DEFAULT : handBody
        );

        setCustom(isCustom);
    }

    public static PlayerPerson randomPlayer(String id,
                                            String name,
                                            double abilityLow,
                                            double abilityHigh) {
        return randomPlayer(id,
                name,
                Math.random() < 0.4,
                abilityLow,
                abilityHigh,
                false);
    }

    public static PlayerPerson randomPlayer(String id,
                                            String name,
                                            boolean leftHanded,
                                            double abilityLow,
                                            double abilityHigh,
                                            boolean isCustom) {
        Random random = new Random();
        double left = leftHanded ?
                100.0 : 50.0;
        double right = leftHanded ?
                50.0 : 100.0;

        double power = generateDouble(random, abilityLow, Math.min(abilityHigh + 5.0, 99.5));
        return new PlayerPerson(
                id,
                name,
                Math.min(power / 0.9, 100),
                power,
                generateDouble(random, abilityLow, abilityHigh),
                generateDouble(random, abilityLow, abilityHigh),
                generateDouble(random, abilityLow, abilityHigh),
                1.0,
                1.0,
                generateDouble(random, abilityLow, abilityHigh),
                generateDouble(random, abilityLow, abilityHigh),
                null,
                isCustom,
                new PlayerPerson.HandBody(
                        180.0,
                        1.0,
                        left,
                        right,
                        generateDouble(random, abilityLow, abilityHigh)
                )
        );
    }

    private static double generateDouble(Random random, double origin, double bound) {
        double d = random.nextDouble();
        return origin + d * (bound - origin);
    }

    private static String estimateCategory(double precisionPercentage,
                                           double control1, double control2) {
        double avg = (precisionPercentage + control1 + control2) / 3;
        return avg > 80.0 ? "Professional" :
                (avg > 50.0 ? "Amateur" : " Noob");
    }

    private static double[] estimateCuePoint(double cuePrecision, double spinControl) {
        return new double[]{0,
                (100 - cuePrecision) / 4.0,
                0,
                (100 - spinControl) / 4.0};
    }

    private AiPlayStyle deriveAiStyle() {
        double position = powerControl;
        return new AiPlayStyle(
                Math.min(99.5, precisionPercentage * 1.1),
                Math.min(99.5, precisionPercentage),
                Math.min(99.5, position),
                Math.min(99.5, position),
                Math.min(100, precisionPercentage),
                50,
                "right",
                maxPowerPercentage * 0.9 < 80.0,  // 不化简是为了易读
                2
        );
    }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        obj.put("spin", getMaxSpinPercentage());
        obj.put("precision", getPrecisionPercentage());
        obj.put("anglePrecision", getAnglePrecision());
        obj.put("longPrecision", getLongPrecision());
        obj.put("name", getName());

        obj.put("maxPower", getMaxPowerPercentage());
        obj.put("controllablePower", getControllablePowerPercentage());
        obj.put("solving", getSolving());

        JSONArray cueAction = new JSONArray(List.of(getMinPullDt(), getMaxPullDt()));
        obj.put("pullDt", cueAction);
        obj.put("cuePlayType", getCuePlayType().toString());
        obj.put("aimingOffset", getAimingOffset());
        obj.put("cueSwingMag", getCueSwingMag());

        JSONArray cuePoint = new JSONArray(cuePointMuSigmaXY);
        obj.put("cuePointMuSigma", cuePoint);
        obj.put("powerControl", getPowerControl());

        JSONArray privateCues = new JSONArray();
        for (Cue cue : getPrivateCues()) {
            privateCues.put(cue.getName());
        }

        obj.put("privateCues", privateCues);
        obj.put("category", category);
        obj.put("psy", psy);

        obj.put("ai", getAiPlayStyle().toJsonObject());

        JSONObject handObj = new JSONObject();
        handObj.put("left", handBody.leftHand);
        handObj.put("right", handBody.rightHand);
        handObj.put("rest", handBody.restPot);

        obj.put("height", handBody.height);
        obj.put("width", handBody.bodyWidth);
        obj.put("hand", handObj);

        return obj;
    }

    public double getSolving() {
        return solving;
    }

    public boolean isCustom() {
        return isCustom;
    }

    public void setCustom(boolean custom) {
        isCustom = custom;
    }

    public AiPlayStyle getAiPlayStyle() {
        return aiPlayStyle;
    }

    public double[] getCuePointMuSigmaXY() {
        return cuePointMuSigmaXY;
    }

    public CuePlayType getCuePlayType() {
        return cuePlayType;
    }

    public double getAimingOffset() {
        return aimingOffset;
    }

    public double getCueSwingMag() {
        return cueSwingMag;
    }

    public double getMaxPullDt() {
        return maxPullDt;
    }

    public double getMinPullDt() {
        return minPullDt;
    }

    public void addPrivateCue(Cue privateCue) {
        privateCues.add(privateCue);
    }

    public List<Cue> getPrivateCues() {
        return privateCues;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerPerson that = (PlayerPerson) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    public String getName() {
        return name;
    }

    public String getPlayerId() {
        return playerId;
    }

    public double getMaxPowerPercentage() {
        return maxPowerPercentage;
    }

    public double getControllablePowerPercentage() {
        return controllablePowerPercentage;
    }

    public double getMaxSpinPercentage() {
        return maxSpinPercentage;
    }

    public double getPrecisionPercentage() {
        return precisionPercentage;
    }

    public double getAnglePrecision() {
        return anglePrecision;
    }

    public double getLongPrecision() {
        return longPrecision;
    }

    public double getPowerControl() {
        return powerControl;
    }

    public enum Hand {
        LEFT(1.0),
        RIGHT(1.0),
        REST(0.7);

        public final double powerMul;

        Hand(double powerMul) {
            this.powerMul = powerMul;
        }
    }

    public static class ReadableAbility {
        public String category;
        public double aiming;
        public double normalPower;
        public double maxPower;
        public double spin;
        public double cuePrecision;  // 出杆, 范围0-100
        public double powerControl;
        public double spinControl;  // 范围0-100
        private String playerId;
        private String name;
        private HandBody handBody;

        public static ReadableAbility fromPlayerPerson(PlayerPerson playerPerson) {
            ReadableAbility ra = new ReadableAbility();
            ra.playerId = playerPerson.playerId;
            ra.name = playerPerson.name;
            ra.category = playerPerson.category;
            ra.aiming = playerPerson.getPrecisionPercentage();
            ra.cuePrecision =
                    Math.max(0,
                            100.0 - (playerPerson.getCuePointMuSigmaXY()[0] +
                                    playerPerson.getCuePointMuSigmaXY()[1]) * 4.0);
            ra.normalPower = playerPerson.getControllablePowerPercentage();
            ra.maxPower = playerPerson.getMaxPowerPercentage();
            ra.powerControl = playerPerson.getPowerControl();
            ra.spin = playerPerson.getMaxSpinPercentage();
            ra.spinControl = Math.max(0,
                    100.0 - (playerPerson.getCuePointMuSigmaXY()[2] +
                            playerPerson.getCuePointMuSigmaXY()[3]) * 4.0);

            ra.handBody = playerPerson.handBody;
            return ra;
        }

        /**
         * @return 非惯用手的好坏
         */
        public double getAnotherHandGoodness() {
            return (handBody.getSecondary().hand == Hand.REST) ?
                    handBody.getThird().skill : handBody.getSecondary().skill;
        }

        public double getRestGoodness() {
            return (handBody.getSecondary().hand == Hand.REST) ?
                    handBody.getSecondary().skill : handBody.getThird().skill;
        }

        public static double addPerksHowMany(int addWhat, int perks) {
            switch (addWhat) {
                case PerkManager.AIMING:
                    return perks * 0.5;
                case PerkManager.CUE_PRECISION:
                    return perks * 0.5;
                case PerkManager.POWER:
                    return perks * 1.0;
                case PerkManager.POWER_CONTROL:
                    return perks * 0.8;
                case PerkManager.SPIN:
                    return perks * 0.8;
                case PerkManager.SPIN_CONTROL:
                    return perks * 0.7;
                case PerkManager.ANTI_HAND:
                    return perks * 0.9;
                case PerkManager.REST:
                    return perks * 1.2;
                default:
                    throw new RuntimeException("Unknown type " + addWhat);
            }
        }

        public void addPerks(int addWhat, int perks) {
            double many = addPerksHowMany(addWhat, perks);
            switch (addWhat) {
                case PerkManager.AIMING:
                    aiming += many;
                    break;
                case PerkManager.CUE_PRECISION:
                    cuePrecision += many / 100.0;
                    break;
                case PerkManager.POWER:
                    normalPower += many;
                    maxPower += many / 0.9;
                    break;
                case PerkManager.POWER_CONTROL:
                    powerControl += many;
                    break;
                case PerkManager.SPIN:
                    spin += many;
                    break;
                case PerkManager.SPIN_CONTROL:
                    spinControl += many / 100.0;
                    break;
                case PerkManager.ANTI_HAND:
                    if (handBody.getPrimary().hand == Hand.LEFT) {
                        handBody.rightHand += many;
                    } else {
                        handBody.leftHand += many;
                    }
                    break;
                case PerkManager.REST:
                    handBody.restPot += many;
                    break;
                default:
                    throw new RuntimeException("Unknown type " + addWhat);
            }
        }

        public PlayerPerson toPlayerPerson() {
            return new PlayerPerson(
                    playerId,
                    name,
                    maxPower,
                    normalPower,
                    spin,
                    aiming,
                    cuePrecision,
                    1.0,
                    1.0,
                    powerControl,
                    spinControl,
                    null,
                    true,
                    handBody
            );
        }
    }

    public static class HandSkill implements Comparable<HandSkill> {
        public final Hand hand;
        public final double skill;

        HandSkill(Hand hand, double skill) {
            this.hand = hand;
            this.skill = skill;
        }

        @Override
        public int compareTo(@NotNull PlayerPerson.HandSkill o) {
            return -Double.compare(this.skill, o.skill);
        }

        @Override
        public String toString() {
            return "HandSkill{" +
                    "hand=" + hand +
                    ", skill=" + skill +
                    '}';
        }
    }

    public static class HandBody {
        public static final HandBody DEFAULT =
                new HandBody(180.0, 1.0, 50.0, 100.0, 80.0);

        public final double height;
        public final double bodyWidth;
        public double leftHand;
        public double rightHand;
        public double restPot;
        private final boolean leftHandRest;    // 是否用左手拿架杆

        private final HandSkill[] precedence;

        public HandBody(double height, double bodyWidth,
                        double leftHand, double rightHand, double restPot) {
            this.height = height;
            this.bodyWidth = bodyWidth;
            this.leftHand = leftHand;
            this.rightHand = rightHand;
            this.restPot = restPot;

            precedence = new HandSkill[]{
                    new HandSkill(Hand.LEFT, leftHand),
                    new HandSkill(Hand.RIGHT, rightHand),
                    new HandSkill(Hand.REST, restPot)
            };
            Arrays.sort(precedence);
            this.leftHandRest = precedence[0].hand == Hand.RIGHT;
        }

        public static double getPowerMulOfHand(@Nullable HandSkill handSkill) {
            return handSkill == null ? 1.0 : (handSkill.skill * handSkill.hand.powerMul / 100);
        }

        public static double getPrecisionOfHand(@Nullable HandSkill handSkill) {
            return handSkill == null ? 1.0 : (handSkill.skill / 100);
        }

        public static double getSdOfHand(@Nullable HandSkill handSkill) {
            return handSkill == null ? 1.0 : (100.0 / handSkill.skill);
        }

        public HandSkill getPrimary() {
            return precedence[0];
        }

        public HandSkill getSecondary() {
            return precedence[1];
        }

        public HandSkill getThird() {
            return precedence[2];
        }

        public double getRestPot() {
            return restPot;
        }

        public boolean isLeftHandRest() {
            return leftHandRest;
        }

        public HandSkill getHandSkillByHand(Hand hand) {
            for (HandSkill handSkill : precedence) {
                if (handSkill.hand == hand) return handSkill;
            }
            throw new RuntimeException("No such hand");
        }
    }
}
