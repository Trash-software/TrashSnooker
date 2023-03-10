package trashsoftware.trashSnooker.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.ai.AiPlayStyle;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.fxml.widgets.PerkManager;
import trashsoftware.trashSnooker.util.ConfigLoader;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.PinyinDict;

import java.util.*;

public class PlayerPerson {

    // 出杆准确度转化为的readable的multiplier。同样的打点标准差，这个值越大，readable的值越好
    private static final double CUE_PRECISION_FACTOR = 3.0;

    public final String category;
    public final HandBody handBody;
    public final double psy;
    public final boolean isRandom;
    private final String playerId;
    private final String name;
    private transient final String shownName;
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
    private final Sex sex;
    private boolean isCustom;

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
                        HandBody handBody,
                        Sex sex) {

        boolean needTranslate = !Objects.equals(
                ConfigLoader.getInstance().getLocale().getLanguage().toLowerCase(Locale.ROOT),
                "zh");
        
        this.playerId = playerId.replace('\'', '_');
        this.name = name;
        this.shownName = (needTranslate && PinyinDict.getInstance().needTranslate(name)) ?
                PinyinDict.getInstance().translateChineseName(name) : name; 
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
        this.sex = sex;

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
                        double psy,
                        AiPlayStyle aiPlayStyle,
                        boolean isCustom,
                        HandBody handBody,
                        Sex sex) {
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
                0.0,
                estimateCuePoint(cuePrecision, spinControl),
                powerControl,
                psy,
                CuePlayType.DEFAULT_PERFECT,
                aiPlayStyle,
                handBody == null ? HandBody.DEFAULT : handBody,
                sex
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
                false,
                180.0,
                Sex.M);
    }

    public static PlayerPerson randomPlayer(String id,
                                            String name,
                                            boolean leftHanded,
                                            double abilityLow,
                                            double abilityHigh,
                                            boolean isCustom,
                                            double height,
                                            Sex sex) {
        Random random = new Random();
        double left = leftHanded ?
                100.0 : 50.0;
        double right = leftHanded ?
                50.0 : 100.0;

        double power = generateDouble(random, abilityLow, Math.min(abilityHigh + 5.0, 99.5));
        power *= sex.powerMul;

        double heightPercentage = (height - sex.minHeight) / (sex.maxHeight - sex.minHeight);
        double restMul = (2 - heightPercentage) / 1.6;

        return new PlayerPerson(
                id,
                name,
                Math.min(power / 0.9, 100 * sex.powerMul),
                power,
                generateDouble(random, abilityLow, abilityHigh),
                generateDouble(random, abilityLow, abilityHigh),
                generateDouble(random, abilityLow, abilityHigh),
                1.0,
                1.0,
                generateDouble(random, abilityLow, abilityHigh),
                generateDouble(random, abilityLow, abilityHigh),
                sex == Sex.M ? 90.0 : 75.0,
                null,
                isCustom,
                new PlayerPerson.HandBody(
                        height,
                        1.0,
                        left,
                        right,
                        Math.max(10, Math.min(90, generateDouble(random, abilityLow * restMul, abilityHigh * restMul)))
                ),
                sex
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
                (100 - cuePrecision) / CUE_PRECISION_FACTOR,
                0,
                (100 - spinControl) / CUE_PRECISION_FACTOR};
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
        obj.put("name", name);

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
        handObj.put("left", handBody.getHandSkillByHand(Hand.LEFT).skill);
        handObj.put("right", handBody.getHandSkillByHand(Hand.RIGHT).skill);
        handObj.put("rest", handBody.getHandSkillByHand(Hand.REST).skill);

        obj.put("height", handBody.height);
        obj.put("width", handBody.bodyWidth);
        obj.put("hand", handObj);
        obj.put("sex", sex.name());

        return obj;
    }

    public Cue getPreferredCue(GameRule gameRule) {
        Cue.Size[] suggested = gameRule.suggestedCues;

        // 先看私杆
        for (Cue.Size size : suggested) {
            // size是按照推荐顺序排的
            for (Cue cue : getPrivateCues()) {
                if (cue.tipSize == size) return cue;
            }
        }

        Collection<Cue> publicCues = DataLoader.getInstance().getPublicCues().values();
        // 再看公杆
        for (Cue.Size size : suggested) {
            // size是按照推荐顺序排的
            for (Cue cue : publicCues) {
                if (cue.tipSize == size) return cue;
            }
        }

        System.err.println("Using break cue to play");
        return DataLoader.getInstance().getStdBreakCue();  // 不会运行到这一步的
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
        return Objects.hash(playerId);
    }

    public String getName() {
        return shownName;
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

    public Sex getSex() {
        return sex;
    }

    public double getErrorMultiplierOfPower(double selectedPower) {
        double ctrlAblePwr = getControllablePowerPercentage();
        double mul = 1;
        if (selectedPower > ctrlAblePwr) {
            // 超过正常发力范围，打点准确度大幅下降
            // 一般来说，球手的最大力量大于可控力量15%左右
            // 打点准确度最大应下降5倍
            mul += (selectedPower - ctrlAblePwr) / 3;
        }
        return mul * selectedPower / ctrlAblePwr;
    }

    public enum Hand {
        LEFT(1.0),
        RIGHT(1.0),
        REST(0.8);

        public final double powerMul;

        Hand(double powerMul) {
            this.powerMul = powerMul;
        }
    }

    public enum Sex {
        M("sexM", 155, 205, 180, 1.0),
        F("sexF", 145, 190, 168, 0.85);

        public final String key;
        public final double minHeight;
        public final double maxHeight;
        public final double stdHeight;
        public final double powerMul;

        Sex(String key, double minHeight, double maxHeight, double stdHeight, double powerMul) {
            this.key = key;
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
            this.stdHeight = stdHeight;
            this.powerMul = powerMul;
        }

        @Override
        public String toString() {
            return App.getStrings().getString(key);
        }
    }

    public static class ReadableAbility implements Cloneable {
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
        private String shownName;
        private HandBody handBody;
        private Sex sex;

        public static ReadableAbility fromPlayerPerson(PlayerPerson playerPerson) {
            return fromPlayerPerson(playerPerson, 1.0);
        }

        public static ReadableAbility fromPlayerPerson(PlayerPerson playerPerson,
                                                       double handFeelEffort) {
            ReadableAbility ra = new ReadableAbility();
            ra.playerId = playerPerson.playerId;
            ra.name = playerPerson.name;
            ra.shownName = playerPerson.shownName;
            ra.category = playerPerson.category;
            ra.aiming = playerPerson.getPrecisionPercentage();
            ra.cuePrecision =
                    Math.max(0,
                            100.0 - (playerPerson.getCuePointMuSigmaXY()[0] +
                                    playerPerson.getCuePointMuSigmaXY()[1]) * CUE_PRECISION_FACTOR) * handFeelEffort;
            ra.normalPower = playerPerson.getControllablePowerPercentage();
            ra.maxPower = playerPerson.getMaxPowerPercentage();
            ra.powerControl = playerPerson.getPowerControl() * handFeelEffort;
            ra.spin = playerPerson.getMaxSpinPercentage();
            ra.spinControl = Math.max(0,
                    100.0 - (playerPerson.getCuePointMuSigmaXY()[2] +
                            playerPerson.getCuePointMuSigmaXY()[3]) * CUE_PRECISION_FACTOR) * handFeelEffort;

            ra.handBody = playerPerson.handBody;
            ra.sex = playerPerson.sex;
            return ra;
        }

        public Sex getSex() {
            return sex;
        }

        public String getName() {
            return name;
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

        public double addPerksHowMany(int addWhat) {
            double unit;
            double minimum = 0.2;

            switch (addWhat) {
                case PerkManager.AIMING:
                    unit = (100 - aiming) * 0.05;
                    break;
                case PerkManager.CUE_PRECISION:
                    unit = (100 - cuePrecision) * 0.05;
                    break;
                case PerkManager.POWER:
                    double sexPowerMax = 100 * sex.powerMul;
                    unit = (sexPowerMax - normalPower) * 0.05;
                    break;
                case PerkManager.POWER_CONTROL:
                    unit = (100 - powerControl) * 0.05;
                    break;
                case PerkManager.SPIN:
                    unit = (100 - spin) * 0.05;
                    break;
                case PerkManager.SPIN_CONTROL:
                    unit = (100 - spinControl) * 0.05;
                    break;
                case PerkManager.ANTI_HAND:
                    unit = (100 - handBody.getAntiHand().skill) * 0.05;
                    break;
                case PerkManager.REST:
                    unit = (100 - handBody.getRest().skill) * 0.075;
                    minimum = 0.3;
                    break;
                default:
                    throw new RuntimeException("Unknown type " + addWhat);
            }
            return Math.max(unit, minimum);
        }

        public void addPerks(int addWhat, int perks) {
            if (perks == 0) return;
            System.out.println("Add " + addWhat + " " + perks);

            while (perks > 0) {
                perks--;
                double many = addPerksHowMany(addWhat);
                switch (addWhat) {
                    case PerkManager.AIMING:
                        aiming += many;
                        break;
                    case PerkManager.CUE_PRECISION:
                        cuePrecision += many;
                        break;
                    case PerkManager.POWER:
                        normalPower += many;
                        maxPower += many / 0.9;
                        maxPower = Math.min(maxPower, 100.0 * sex.powerMul);
                        break;
                    case PerkManager.POWER_CONTROL:
                        powerControl += many;
                        break;
                    case PerkManager.SPIN:
                        spin += many;
                        break;
                    case PerkManager.SPIN_CONTROL:
                        spinControl += many;
                        break;
                    case PerkManager.ANTI_HAND:
                        handBody.getAntiHand().skill += many;
                        break;
                    case PerkManager.REST:
                        handBody.getRest().skill += many;
//                    handBody.restPot += many;
                        break;
                    default:
                        throw new RuntimeException("Unknown type " + addWhat);
                }
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
                    sex == Sex.M ? 90.0 : 75.0,
                    null,
                    true,
                    handBody,
                    sex
            );
        }

        @Override
        public ReadableAbility clone() {
            try {
                ReadableAbility clone = (ReadableAbility) super.clone();
                // TODO: copy mutable state here, so the clone can't change the internals of the original
                clone.handBody = handBody.clone();
                return clone;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }
    }

    public static class HandSkill implements Comparable<HandSkill>, Cloneable {
        public final Hand hand;
        public double skill;

        HandSkill(Hand hand, double skill) {
            this.hand = hand;
            this.skill = skill;
        }

        @Override
        protected Object clone() throws CloneNotSupportedException {
            return super.clone();
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

    public static class HandBody implements Cloneable {
        public static final HandBody DEFAULT =
                new HandBody(180.0, 1.0, 50.0, 100.0, 80.0);

        public final double height;
        public final double bodyWidth;
        private final boolean leftHandRest;    // 是否用左手拿架杆

        private HandSkill[] precedence;

        public HandBody(double height, double bodyWidth,
                        double leftHand, double rightHand, double restPot) {
            this.height = height;
            this.bodyWidth = bodyWidth;

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

        @Override
        protected HandBody clone() throws CloneNotSupportedException {
            HandBody clone = (HandBody) super.clone();
            clone.precedence = new HandSkill[precedence.length];
            for (int i = 0; i < clone.precedence.length; i++) {
                clone.precedence[i] = (HandSkill) precedence[i].clone();
            }
            return clone;
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

        public boolean isLeftHandRest() {
            return leftHandRest;
        }

        public HandSkill getAntiHand() {
            HandSkill sec = getSecondary();
            return sec.hand == Hand.REST ? getThird() : sec;
        }

        public HandSkill getRest() {
            for (HandSkill hs : precedence) {
                if (hs.hand == Hand.REST) return hs;
            }
            throw new RuntimeException();
        }

        public HandSkill getHandSkillByHand(Hand hand) {
            for (HandSkill handSkill : precedence) {
                if (handSkill.hand == hand) return handSkill;
            }
            throw new RuntimeException("No such hand");
        }
    }
}
