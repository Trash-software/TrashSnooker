package trashsoftware.trashSnooker.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.ai.AiPlayStyle;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.cue.CueBrand;
import trashsoftware.trashSnooker.core.cue.CueSize;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.fxml.widgets.PerkManager;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.config.ConfigLoader;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.PinyinDict;
import trashsoftware.trashSnooker.util.Util;

import java.util.*;

public class PlayerPerson {

    // 出杆准确度转化为的readable的multiplier。同样的打点标准差，这个值越大，readable的值越好
    private static final double CUE_PRECISION_FACTOR = 3.0;

    public final String category;
    public final HandBody handBody;
    public final double psy;
    public final boolean isRandom;
    private final String playerId;
    private final String name;  // 名字的原版
    private transient final String shownName;
    private final double maxPowerPercentage;
    private final double controllablePowerPercentage;
    private final double maxSpinPercentage;
    private final double precisionPercentage;
    private final double anglePrecision;
    private final double longPrecision;
    private final List<CueBrand> privateCues = new ArrayList<>();
    private final double solving;
    private final double minPullDt;  // 最小力时的拉杆长度
    private final double maxPullDt;
    private final double minExtension;  // 最小力时的延伸长度
    private final double maxExtension;
    private final double aimingOffset;  // 瞄和打的偏差，正值向右偏
    private final double powerControl;
    // 横向的mu，横向的sigma，纵向的mu，纵向的sigma
    // 横向的左塞负右塞正，纵向的高杆正低杆负
    private final double[] cuePointMuSigmaXY;
    private final double cueSwingMag;
    private final CuePlayType cuePlayType;
    private final AiPlayStyle aiPlayStyle;
    private final Sex sex;
    private final boolean underage;
    private final Map<GameRule, Double> participates;
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
                        double[] pullDtExtensionDt,
                        double aimingOffset,
                        double cueSwingMag,
                        double[] cuePointMuSigmaXY,
                        double powerControl,
                        double psy,
                        CuePlayType cuePlayType,
                        @Nullable AiPlayStyle aiPlayStyle,
                        HandBody handBody,
                        Sex sex,
                        boolean underage,
                        Map<GameRule, Double> participates) {

        boolean needTranslate = !Objects.equals(
                ConfigLoader.getInstance().getLocale().getLanguage().toLowerCase(Locale.ROOT),
                "zh");

        this.playerId = playerId.replace('\'', '_');
        this.isRandom = this.playerId.startsWith("random_");
        this.name = name;
        this.shownName = (isRandom ? getShownNameOfRandom(this.playerId) :
                (needTranslate && PinyinDict.getInstance().needTranslate(name)) ?
                        PinyinDict.getInstance().translateChineseName(name) : name);
        this.category = category;
        this.maxPowerPercentage = maxPowerPercentage;
        this.controllablePowerPercentage = controllablePowerPercentage;
        this.maxSpinPercentage = maxSpinPercentage;
        this.precisionPercentage = precisionPercentage;
        this.anglePrecision = anglePrecision;
        this.longPrecision = longPrecision;
        this.solving = solving;
        this.minPullDt = pullDtExtensionDt[0];
        this.maxPullDt = pullDtExtensionDt[1];
        this.minExtension = pullDtExtensionDt[2];
        this.maxExtension = pullDtExtensionDt[3];
        this.aimingOffset = aimingOffset;
        this.cueSwingMag = cueSwingMag;
        this.cuePointMuSigmaXY = cuePointMuSigmaXY;
        this.powerControl = powerControl;
        this.psy = psy;
        this.cuePlayType = cuePlayType;
        this.aiPlayStyle = aiPlayStyle == null ? deriveAiStyle() : aiPlayStyle;
        this.handBody = handBody;
        this.sex = sex;
        this.underage = underage;

        this.participates = participates;
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
                        Sex sex,
                        boolean underage,
                        Map<GameRule, Double> participateGames) {
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
                new double[]{50, 200, 50, 150},
                0.0,
                0.0,
                estimateCuePoint(cuePrecision, spinControl),
                powerControl,
                psy,
                CuePlayType.DEFAULT_PERFECT,
                aiPlayStyle,
                handBody == null ? HandBody.DEFAULT : handBody,
                sex,
                underage,
                participateGames
        );

        setCustom(isCustom);
    }

    public static PlayerPerson fromJson(String playerId, JSONObject personObj) {
        String name = personObj.getString("name");

        AiPlayStyle aiPlayStyle;
        if (personObj.has("ai")) {
            JSONObject aiObject = personObj.getJSONObject("ai");
            aiPlayStyle = AiPlayStyle.fromJson(aiObject);
        } else {
            aiPlayStyle = null;
        }

        PlayerPerson.HandBody handBody;
        if (personObj.has("hand")) {
            JSONObject handObj = personObj.getJSONObject("hand");
            handBody = new PlayerPerson.HandBody(
                    personObj.getDouble("height"),
                    personObj.getDouble("width"),
                    handObj.getDouble("left"),
                    handObj.getDouble("right"),
                    handObj.getDouble("rest")
            );
        } else {
            handBody = PlayerPerson.HandBody.DEFAULT;
        }

        PlayerPerson playerPerson;

        JSONArray pullDt = personObj.getJSONArray("pullDt");
        double[] pullDtExtensionDt;
        if (pullDt.length() == 2) {
            pullDtExtensionDt = new double[]{pullDt.getDouble(0), pullDt.getDouble(1), 50, 150};
        } else if (pullDt.length() == 4) {
            pullDtExtensionDt = new double[]{pullDt.getDouble(0),
                    pullDt.getDouble(1),
                    pullDt.getDouble(2),
                    pullDt.getDouble(3)};
        } else {
            System.err.println("Player " + playerId + " has wrong pull dt");
            pullDtExtensionDt = new double[]{50, 200, 50, 150};
        }

        double aimingOffset = personObj.getDouble("aimingOffset");
        double cueSwingMag = personObj.getDouble("cueSwingMag");
        String cuePlayTypeStr = personObj.getString("cuePlayType");
        CuePlayType cuePlayType = parseCuePlayType(cuePlayTypeStr);

        if (personObj.has("specialAction")) {
            cuePlayType.setSpecialAction(parseSpecialAction(personObj.getJSONObject("specialAction")));
        }

        JSONArray muSigmaArray = personObj.getJSONArray("cuePointMuSigma");
        double[] muSigma = new double[4];
        for (int i = 0; i < 4; ++i) {
            muSigma[i] = muSigmaArray.getDouble(i);
        }

        PlayerPerson.Sex sex = personObj.has("sex") ?
                PlayerPerson.Sex.valueOf(personObj.getString("sex")) :
                PlayerPerson.Sex.M;

        playerPerson = new PlayerPerson(
                playerId,
                name,
                personObj.getString("category"),
                personObj.getDouble("maxPower"),
                personObj.getDouble("controllablePower"),
                personObj.getDouble("spin"),
                personObj.getDouble("precision"),
                personObj.getDouble("anglePrecision"),
                personObj.getDouble("longPrecision"),
                personObj.getDouble("solving"),
                pullDtExtensionDt,
                aimingOffset,
                cueSwingMag,
                muSigma,
                personObj.getDouble("powerControl"),
                personObj.getDouble("psy"),
                cuePlayType,
                aiPlayStyle,
                handBody,
                sex,
                personObj.has("underage") && personObj.getBoolean("underage"),
                parseParticipates(personObj.has("games") ? personObj.get("games") : null)
        );
        
        if (personObj.has("privateCues")) {
            JSONArray priCues = personObj.getJSONArray("privateCues");
            for (int i = 0; i < priCues.length(); i++) {
                String cbi = priCues.getString(i);
                CueBrand cueBrand = DataLoader.getInstance().getCueById(cbi);
                if (cueBrand != null) {
                    playerPerson.getPrivateCues().add(cueBrand);
                } else {
                    EventLogger.warning("Private cue brand '" + cbi + "' not available");
                }
            }
        }

        return playerPerson;
    }

    public static String getShownNameOfRandom(String id) {
        String[] spl = id.split("_");
        int n = Integer.parseInt(spl[spl.length - 1].strip());
        return String.format(App.getStrings().getString("randomPlayerName"), n);
    }

    private static Map<GameRule, Double> participatesAll() {
        Map<GameRule, Double> res = new HashMap<>();
        for (GameRule rule : GameRule.values()) {
            res.put(rule, 1.0);
        }
        return res;
    }

    public static Map<GameRule, Double> parseParticipates(Object object) {
        if (object == null) {
            // 没有专门声明，就是全都参加
            return participatesAll();
        }
        Map<GameRule, Double> result = new HashMap<>();
        if (object instanceof JSONArray array) {
            for (Object key : array) {
                String s = String.valueOf(key);
                if ("all".equals(s)) return participatesAll();
                if ("sidePocket".equals(s)) s = "americanNine";  // 填坑

                String enumName = Util.toAllCapsUnderscoreCase(s);
                try {
                    result.put(GameRule.valueOf(enumName), 1.0);
                } catch (IllegalArgumentException e) {
                    System.err.println("Unknown game '" + enumName + "'");
                }
            }
        } else if (object instanceof JSONObject json) {
            for (String key : json.keySet()) {
                String enumName = Util.toAllCapsUnderscoreCase(key);
                try {
                    result.put(GameRule.valueOf(enumName), json.getDouble(key));
                } catch (IllegalArgumentException e) {
                    System.err.println("Unknown game '" + enumName + "'");
                }
            }
        }
        return result;
    }

    public static PlayerPerson randomPlayer(String id,
                                            String name,
                                            double abilityLow,
                                            double abilityHigh) {
        Sex sex = Math.random() < 0.1 ? Sex.F : Sex.M;
        return randomPlayer(id,
                name,
                Math.random() < 0.25,
                abilityLow,
                abilityHigh,
                false,
                randomHeight(sex),
                sex);
    }
    
    private static double randomHeight(Sex sex) {
        double mean = sex.stdHeight;
        double sd = (sex.maxHeight - sex.minHeight) / 8;
        double g = new Random().nextGaussian();
        return mean + g * sd;
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

        PlayerPerson person = new PlayerPerson(
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
                        sex == Sex.M ? 1.0 : 0.9,
                        left,
                        right,
                        Math.max(10, Math.min(90, generateDouble(random, abilityLow * restMul, abilityHigh * restMul)))
                ),
                sex,
                false,
                participatesAll()
        );

        if (sex == Sex.F) {
            for (String fLimitId : new String[]{"GirlCue", "GirlPoolCue"}) {
                CueBrand cb = DataLoader.getInstance().getCueById(fLimitId);
                person.addPrivateCue(cb);
            }
        }
        return person;
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

    private static CuePlayType parseCuePlayType(String s) {
        return new CuePlayType(s);
    }

    private static CuePlayType.SpecialAction parseSpecialAction(JSONObject special) {
        if (special.has("doubleCueAction")) {
            JSONObject object = special.getJSONObject("doubleCueAction");
            return CuePlayType.DoubleAction.fromJson(object);
        }
        return null;
    }

    private static double[] estimateCuePoint(double cuePrecision, double spinControl) {
        return new double[]{0,
                (100 - cuePrecision) / CUE_PRECISION_FACTOR,
                0,
                (100 - spinControl) / CUE_PRECISION_FACTOR};
    }


    public static String getPlayerCategoryShown(String category, ResourceBundle strings) {
        if ("All".equals(category)) {
            return strings.getString("catAll");
        } else if ("Professional".equals(category)) {
            return strings.getString("catProf");
        } else if ("Amateur".equals(category)) {
            return strings.getString("catAmateur");
        } else if ("Noob".equals(category)) {
            return strings.getString("catNoob");
        } else if ("God".equals(category)) {
            return strings.getString("catGod");
        } else {
            return strings.getString("catUnk");
        }
    }

    private AiPlayStyle deriveAiStyle() {
        double position = powerControl;
        return new AiPlayStyle(
                Math.min(99.5, precisionPercentage * 1.05),
                Math.min(99.5, precisionPercentage),
                Math.min(99.5, position),
                Math.min(99.5, position),
                Math.min(100, precisionPercentage),
                50,
                new double[]{15.0, Math.min(60, controllablePowerPercentage * 0.8)},
                precisionPercentage * 0.9,
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

        JSONArray cueAction = new JSONArray(List.of(getMinPullDt(), getMaxPullDt(), getMinExtension(), getMaxExtension()));
        obj.put("pullDt", cueAction);
        obj.put("cuePlayType", getCuePlayType().toString());
        obj.put("aimingOffset", getAimingOffset());
        obj.put("cueSwingMag", getCueSwingMag());

        JSONArray cuePoint = new JSONArray(cuePointMuSigmaXY);
        obj.put("cuePointMuSigma", cuePoint);
        obj.put("powerControl", getPowerControl());

        JSONArray privateCues = new JSONArray();
        for (CueBrand cue : getPrivateCues()) {
            privateCues.put(cue.getCueId());
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

        JSONObject games = new JSONObject();
        for (GameRule gameRule : participates.keySet()) {
            games.put(Util.toLowerCamelCase(gameRule.name()), participates.get(gameRule));
        }
        obj.put("games", games);

        if (cuePlayType.hasAnySpecialAction()) {
            JSONObject spe = cuePlayType.specialActionsJson();
            obj.put("specialAction", spe);
        }

        return obj;
    }
    
    public static CueBrand getPreferredCue(GameRule gameRule, PlayerPerson person) {
        CueSize[] suggested = gameRule.suggestedCues;

        // 先看私杆
        if (person != null) {
            for (CueSize size : suggested) {
                // size是按照推荐顺序排的
                for (CueBrand cue : person.getPrivateCues()) {
                    if (cue.tipSize == size) return cue;
                }
            }
        }

        Collection<CueBrand> publicCues = DataLoader.getInstance().getPublicCues().values();
        // 再看公杆
        for (CueSize size : suggested) {
            // size是按照推荐顺序排的
            for (CueBrand cue : publicCues) {
                if (cue.tipSize == size) return cue;
            }
        }

        System.err.println("Using break cue to play");
        return DataLoader.getInstance().getStdBreakCueBrand();  // 不会运行到这一步的
    }

    public CueBrand getPreferredCue(GameRule gameRule) {
        return getPreferredCue(gameRule, this);
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

    public boolean isUnderage() {
        return underage;
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

    public double getMinExtension() {
        return minExtension;
    }

    public double getMaxExtension() {
        return maxExtension;
    }

    public void addPrivateCue(CueBrand privateCue) {
        privateCues.add(privateCue);
    }

    /**
     * @return 这个人出道就应该有的杆型，不包含生涯模式买的
     */
    public List<CueBrand> getPrivateCues() {
        return privateCues;
    }

    @Override
    public String toString() {
        return playerId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PlayerPerson that = (PlayerPerson) o;
        return Objects.equals(playerId, that.playerId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(playerId);
    }

    /**
     * @return 返回图形界面上显示的名字
     */
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

    public Map<GameRule, Double> getParticipateGames() {
        return participates;
    }

    public boolean isPlayerOf(GameRule gameRule) {
        return participates.containsKey(gameRule);
    }

    public double skillLevelOfGame(GameRule gameRule) {
        return participates.getOrDefault(gameRule, 0.5);
    }

    /**
     * 返回该球员在选定的发力下，控制力量的标准差
     */
    public double getPowerSd(double selectedPower, HandSkill handSkill) {
        double sd = (100.0 - getPowerControl()) / 100.0;
        return sd * getErrorMultiplierOfPower(selectedPower) * HandBody.getSdOfHand(handSkill);
    }

    public double getErrorMultiplierOfPower(double selectedPower) {
        double ctrlAblePwr = getControllablePowerPercentage();
        double mul = 1;
        if (selectedPower > ctrlAblePwr) {
            // 超过正常发力范围，打点准确度大幅下降
            // 一般来说，球手的最大力量大于可控力量15%左右
            // 打点准确度最大应下降3倍
            mul += (selectedPower - ctrlAblePwr) / 5;
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
        F("sexF", 145, 190, 168, 0.88);

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
        private PlayerPerson originalPerson;  // 记录与复现用，不参与数值计算
        private double multiplier;

        public static ReadableAbility fromPlayerPerson(PlayerPerson playerPerson) {
            return fromPlayerPerson(playerPerson, 1.0);
        }

        public static ReadableAbility fromPlayerPerson(PlayerPerson playerPerson,
                                                       double handFeelEffort) {
            ReadableAbility ra = new ReadableAbility();
            ra.multiplier = handFeelEffort;
            ra.playerId = playerPerson.playerId;
            ra.name = playerPerson.name;
            ra.shownName = playerPerson.shownName;
            ra.category = playerPerson.category;
            ra.aiming = playerPerson.getPrecisionPercentage() * handFeelEffort;
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
            ra.originalPerson = playerPerson;
            return ra;
        }

        public Sex getSex() {
            return originalPerson.sex;
        }

        public HandBody getHandBody() {
            return handBody;
        }

        public String getName() {
            return name;
        }

        public String getShownName() {
            return shownName;
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
                    unit = (120 - aiming) * 0.04;
                    break;
                case PerkManager.CUE_PRECISION:
                    unit = (120 - cuePrecision) * 0.04;
                    break;
                case PerkManager.POWER:
                    double sexPowerMax = 120 * getSex().powerMul;
                    unit = (sexPowerMax - normalPower) * 0.04;
                    break;
                case PerkManager.POWER_CONTROL:
                    unit = (120 - powerControl) * 0.04;
                    break;
                case PerkManager.SPIN:
                    unit = (120 - spin) * 0.04;
                    break;
                case PerkManager.SPIN_CONTROL:
                    unit = (120 - spinControl) * 0.04;
                    break;
                case PerkManager.ANTI_HAND:
                    unit = (120 - handBody.getAntiHand().skill) * 0.04;
                    break;
                case PerkManager.REST:
                    unit = (120 - handBody.getRest().skill) * 0.06;
                    minimum = 0.3;
                    break;
                default:
                    throw new RuntimeException("Unknown type " + addWhat);
            }
            return Math.max(unit, minimum);
        }
        
        public static String getStringByCat(int what) {
            return switch (what) {
                case PerkManager.AIMING -> "aiming";
                case PerkManager.CUE_PRECISION -> "cuePrecision";
                case PerkManager.POWER -> "normalPower";
                case PerkManager.POWER_CONTROL -> "powerControl";
                case PerkManager.SPIN -> "spin";
                case PerkManager.SPIN_CONTROL -> "spinControl";
                case PerkManager.ANTI_HAND -> "antiHand";
                case PerkManager.REST -> "rest";
                default -> throw new RuntimeException("Unknown type " + what);
            };
        }

        public double getAbilityByCat(int what) {
            return switch (what) {
                case PerkManager.AIMING -> aiming;
                case PerkManager.CUE_PRECISION -> cuePrecision;
                case PerkManager.POWER -> normalPower;
                case PerkManager.POWER_CONTROL -> powerControl;
                case PerkManager.SPIN -> spin;
                case PerkManager.SPIN_CONTROL -> spinControl;
                case PerkManager.ANTI_HAND -> handBody.getAntiHand().skill;
                case PerkManager.REST -> handBody.getRest().skill;
                default -> throw new RuntimeException("Unknown type " + what);
            };
        }

        public void addPerks(int addWhat, int perks) {
            if (perks == 0) return;
            System.out.println("Add " + addWhat + " " + perks);

            while (perks > 0) {
                perks--;
                double many = addPerksHowMany(addWhat);
                switch (addWhat) {
                    case PerkManager.AIMING -> {
                        aiming += many;
                        aiming = Math.min(aiming, 100.0);
                    }
                    case PerkManager.CUE_PRECISION -> {
                        cuePrecision += many;
                        cuePrecision = Math.min(cuePrecision, 100.0);
                    }
                    case PerkManager.POWER -> {
                        normalPower += many;
                        maxPower += many / 0.9;
                        maxPower = Math.min(maxPower, 100.0 * getSex().powerMul);
                        normalPower = Math.min(normalPower, maxPower);
                    }
                    case PerkManager.POWER_CONTROL -> {
                        powerControl += many;
                        powerControl = Math.min(powerControl, 100.0);
                    }
                    case PerkManager.SPIN -> {
                        spin += many;
                        spin = Math.min(spin, 100.0);
                    }
                    case PerkManager.SPIN_CONTROL -> {
                        spinControl += many;
                        spinControl = Math.min(spinControl, 100.0);
                    }
                    case PerkManager.ANTI_HAND -> {
                        handBody.getAntiHand().skill += many;
                        handBody.getAntiHand().skill = Math.min(handBody.getAntiHand().skill, 100.0);
                    }
                    case PerkManager.REST -> {
                        handBody.getRest().skill += many;
                        handBody.getRest().skill = Math.min(handBody.getRest().skill, 100.0);
                    }
                    default -> throw new RuntimeException("Unknown type " + addWhat);
                }
            }
        }

        public PlayerPerson toPlayerPerson() {
            if (multiplier != 1.0) {
                throw new RuntimeException("Cannot export to player person: this one is modified");
            }
            PlayerPerson person = new PlayerPerson(
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
                    getSex() == Sex.M ? 90.0 : 75.0,
                    null,
                    true,
                    handBody,
                    getSex(),
                    originalPerson.underage,
                    originalPerson.participates
            );
            person.privateCues.addAll(originalPerson.privateCues);
            return person;
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

        public int precedenceOfHand(Hand hand) {
            for (int i = 0; i < precedence.length; i++) {
                if (precedence[i].hand == hand) return i;
            }
            throw new IndexOutOfBoundsException();
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
