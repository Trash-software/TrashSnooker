package trashsoftware.trashSnooker.core;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.ai.AiPlayStyle;
import trashsoftware.trashSnooker.core.cue.CueBrand;
import trashsoftware.trashSnooker.core.cue.CueSize;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.fxml.widgets.PerkManager;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.PinyinDict;
import trashsoftware.trashSnooker.util.Util;
import trashsoftware.trashSnooker.util.config.ConfigLoader;

import java.util.*;

public class PlayerPerson {

    // 出杆准确度转化为的readable的multiplier。同样的打点标准差，这个值越大，readable的值越好
    public static final double CUE_PRECISION_FACTOR = 3.0;

    public final String category;
    public final HandBody handBody;
    public final double psy;
    public final boolean isRandom;
    private final String playerId;
    private final String name;  // 名字的原版
    private transient final String shownName;
    private final double precisionPercentage;
    private final double anglePrecision;
    private final double longPrecision;
    private final List<CueBrand> privateCues = new ArrayList<>();
    private final double solving;

    private final double aimingOffset;  // 瞄和打的偏差，正值向右偏
    private final AiPlayStyle aiPlayStyle;
    private final Sex sex;
    private final boolean underage;
    private final Map<GameRule, Double> participates;
    private boolean isCustom;

    public PlayerPerson(String playerId,
                        String name,
                        String category,
                        double precisionPercentage,
                        double anglePrecision,
                        double longPrecision,
                        double solving,
                        double aimingOffset,
                        double psy,
                        @Nullable AiPlayStyle aiPlayStyle,
                        @NotNull HandBody handBody,
                        Sex sex,
                        boolean underage,
                        boolean isCustom,
                        Map<GameRule, Double> participates) {

        boolean needTranslate = !Objects.equals(
                ConfigLoader.getInstance().getLocale().getLanguage().toLowerCase(Locale.ROOT),
                "zh");

        this.playerId = playerId.replace('\'', '_');
        this.isRandom = this.playerId.startsWith("random_");
        this.isCustom = isCustom;
        this.name = name;
        this.shownName = (isRandom ? getShownNameOfRandom(this.playerId) :
                (needTranslate && PinyinDict.getInstance().needTranslate(name)) ?
                        PinyinDict.getInstance().translateChineseName(name) : name);
        this.category = category;

        this.precisionPercentage = precisionPercentage;
        this.anglePrecision = anglePrecision;
        this.longPrecision = longPrecision;
        this.solving = solving;

        this.aimingOffset = aimingOffset;

        this.psy = psy;

        this.handBody = handBody;
        this.aiPlayStyle = aiPlayStyle == null ? deriveAiStyle() : aiPlayStyle;
        this.sex = sex;
        this.underage = underage;

        this.participates = participates;
    }

//    public static PlayerPerson createNormal(String playerId,
//                                            String name,
//                                            double maxPowerPercentage,
//                                            double controllablePowerPercentage,
//                                            double maxSpinPercentage,
//                                            double aimingPercentage,
//                                            double cuePrecision,  // 出杆歪不歪
//                                            double anglePrecision,
//                                            double longPrecision,
//                                            double powerControl,
//                                            double spinControl,  // 出杆挑不挑
//                                            double psy,
//                                            CuePlayType cuePlayType,
//                                            AiPlayStyle aiPlayStyle,
//                                            boolean isCustom,
//                                            Sex sex,
//                                            boolean underage,
//                                            Map<GameRule, Double> participateGames) {
//
//        PlayerHand primaryHand = new PlayerHand(
//                PlayerHand.Hand.RIGHT,
//                maxPowerPercentage,
//                controllablePowerPercentage,
//                maxSpinPercentage,
//                new double[]{50, 200, 50, 150},
//                calculateSwingMag(cuePrecision),
//                estimateCuePoint(cuePrecision, spinControl),
//                powerControl,
//                cuePlayType
//        );
//        HandBody handBody = HandBody.createFromPrimary(
//                180, sex == Sex.F ? 0.9 : 1,
//                primaryHand, 0.5, 0.8
//        );
//
//        PlayerPerson pp = new PlayerPerson(
//                playerId,
//                name,
//                estimateCategory(aimingPercentage, powerControl, spinControl),
//                aimingPercentage,
//                anglePrecision,
//                longPrecision,
//                (aimingPercentage + spinControl) / 2,
//                0.0,
//                psy,
//                aiPlayStyle,
//                handBody,
//                sex,
//                underage,
//                participateGames
//        );
//
//        pp.setCustom(isCustom);
//        return pp;
//    }

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
        if (personObj.has("dominant")) {
            // 这里并不一定真的是
            JSONObject primaryObj = personObj.getJSONObject("dominant");
            JSONObject secondaryObj = null;
            JSONObject restObj = null;
            if (personObj.has("nonDominant")) {
                secondaryObj = personObj.getJSONObject("nonDominant");
            }
            if (personObj.has("rest")) {
                restObj = personObj.getJSONObject("rest");
            }

            PlayerHand primary = PlayerHand.fromJson(primaryObj);
            PlayerHand secondary = PlayerHand.fromJson(secondaryObj, primary, 0.5, 0.5);
            PlayerHand rest = PlayerHand.fromJson(restObj, primary, 0.8, 0.8 * PlayerHand.REST_NATIVE_POWER_MUL);

            PlayerHand left, right;
            if (primary.hand == PlayerHand.Hand.LEFT) {
                left = primary;
                right = secondary;
            } else {
                left = secondary;
                right = primary;
            }

            try {
                handBody = new HandBody(
                        personObj.getDouble("height"),
                        personObj.getDouble("width"),
                        left,
                        right,
                        rest
                );
            } catch (IllegalArgumentException iae) {
                throw new RuntimeException("Illegal hands for player " + name + " (" + playerId + ")", iae);
            }
        } else {
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

            PlayerHand.Hand primaryHand;
            if (personObj.has("hand")) {
                JSONObject handObj = personObj.getJSONObject("hand");
                double left = handObj.getDouble("left");
                double right = handObj.getDouble("right");
                primaryHand = left > right ? PlayerHand.Hand.LEFT : PlayerHand.Hand.RIGHT;
            } else {
                primaryHand = PlayerHand.Hand.RIGHT;
            }

            PlayerHand primary = new PlayerHand(
                    primaryHand,
                    personObj.getDouble("maxPower"),
                    personObj.getDouble("controllablePower"),
                    personObj.getDouble("spin"),
                    pullDtExtensionDt,
                    cueSwingMag,
                    muSigma,
                    personObj.getDouble("powerControl"),
                    cuePlayType
            );

            if (personObj.has("hand")) {
                JSONObject handObj = personObj.getJSONObject("hand");
                handBody = HandBody.createFromLeftRight(
                        personObj.getDouble("height"),
                        personObj.getDouble("width"),
                        primary,
                        handObj.getDouble("left") / 100,
                        handObj.getDouble("right") / 100,
                        handObj.getDouble("rest") / 100
                );
            } else {
                handBody = HandBody.createFromPrimary(
                        personObj.getDouble("height"),
                        personObj.getDouble("width"),
                        primary,
                        0.5,
                        0.8
                );
            }
        }

        PlayerPerson playerPerson;

        double aimingOffset = personObj.getDouble("aimingOffset");

        PlayerPerson.Sex sex = personObj.has("sex") ?
                PlayerPerson.Sex.valueOf(personObj.getString("sex")) :
                PlayerPerson.Sex.M;

        playerPerson = new PlayerPerson(
                playerId,
                name,
                personObj.getString("category"),
                personObj.getDouble("precision"),
                personObj.getDouble("anglePrecision"),
                personObj.getDouble("longPrecision"),
                personObj.getDouble("solving"),
                aimingOffset,
                personObj.getDouble("psy"),
                aiPlayStyle,
                handBody,
                sex,
                personObj.has("underage") && personObj.getBoolean("underage"),
                false,
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

        String cueSwingStr;
        double rnd = Math.random();
        if (rnd < 0.25) {
            cueSwingStr = "l,l,r,r,l,l,r,r";
        } else if (rnd < 0.5) {
            cueSwingStr = "r,r,l,l,r,r,l,l";
        } else if (rnd < 0.75) {
            cueSwingStr = "l,l,l,l,r,r,r,r";
        } else {
            cueSwingStr = "r,r,r,r,l,l,l,l";
        }

        double maxPowerPercentage = Math.min(power / 0.9, 100 * sex.powerMul);
        double spin = generateDouble(random, abilityLow, abilityHigh);
        double aimingPrecision = generateDouble(random, abilityLow, abilityHigh);
        double spinControl = generateDouble(random, abilityLow, abilityHigh);
        double powerControl = generateDouble(random, abilityLow, abilityHigh);
        double cuePrecision = generateDouble(random, abilityLow, abilityHigh);

        PlayerHand primaryHand = new PlayerHand(
                leftHanded ? PlayerHand.Hand.LEFT : PlayerHand.Hand.RIGHT,
                maxPowerPercentage,
                power,
                spin,
                new double[]{50, 200, 50, 150},
                calculateSwingMag(cuePrecision),
                estimateCuePoint(cuePrecision, spinControl),
                powerControl,
                CuePlayType.createBySwing(cueSwingStr)
        );
        double restAbility = Math.max(10, Math.min(90, generateDouble(random, abilityLow * restMul, abilityHigh * restMul))) / 100.0;
        HandBody handBody = HandBody.createFromPrimary(
                180, sex == Sex.F ? 0.9 : 1,
                primaryHand, random.nextDouble(0.5, 0.7), restAbility
        );

        PlayerPerson person = new PlayerPerson(
                id,
                name,
                estimateCategory(aimingPrecision, powerControl, spinControl),
                aimingPrecision,
                1.0,
                1.0,
                generateDouble(random, abilityLow, abilityHigh),
                0.0,
                sex == Sex.M ? 90.0 : 75.0,
                null,
                handBody,
                sex,
                false,
                isCustom,
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

    static CuePlayType parseCuePlayType(String s) {
        return new CuePlayType(s);
    }

    static CuePlayType.SpecialAction parseSpecialAction(JSONObject special) {
        if (special.has("doubleCueAction")) {
            JSONObject object = special.getJSONObject("doubleCueAction");
            return CuePlayType.DoubleAction.fromJson(object);
        }
        return null;
    }

    static double calculateSwingMag(double cuePrecision) {
        return Math.min((100 - cuePrecision), 25);
    }

    static double[] estimateCuePoint(double cuePrecision, double spinControl) {
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
        PlayerHand primary = handBody.getPrimary();
        double position = primary.powerControl;
        return new AiPlayStyle(
                Math.min(99.5, precisionPercentage * 1.05),
                Math.min(99.5, precisionPercentage),
                Math.min(99.5, position),
                Math.min(99.5, position),
                Math.min(100, precisionPercentage),
                50,
                new double[]{15.0, Math.min(60, primary.controllablePowerPercentage * 0.8)},
                75,
                precisionPercentage * 0.9,
                "right",
                primary.maxPowerPercentage * 0.9 < 80.0,  // 不化简是为了易读
                2
        );
    }

    public JSONObject toJsonObject() {
        JSONObject obj = new JSONObject();
        obj.put("precision", getPrecisionPercentage());
        obj.put("anglePrecision", getAnglePrecision());
        obj.put("longPrecision", getLongPrecision());
        obj.put("name", name);
        
        obj.put("solving", getSolving());
        
        obj.put("aimingOffset", getAimingOffset());

        JSONArray privateCues = new JSONArray();
        for (CueBrand cue : getPrivateCues()) {
            privateCues.put(cue.getCueId());
        }
        
        obj.put("dominant", handBody.getDominantHand().toJson());
        obj.put("nonDominant", handBody.getAntiHand().toJson());
        obj.put("rest", handBody.getRest().toJson());

        obj.put("privateCues", privateCues);
        obj.put("category", category);
        obj.put("psy", psy);

        obj.put("ai", getAiPlayStyle().toJsonObject());

        obj.put("height", handBody.height);
        obj.put("width", handBody.bodyWidth);
        obj.put("sex", sex.name());

        JSONObject games = new JSONObject();
        for (GameRule gameRule : participates.keySet()) {
            games.put(Util.toLowerCamelCase(gameRule.name()), participates.get(gameRule));
        }
        obj.put("games", games);

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

    public double getAimingOffset() {
        return aimingOffset;
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

    public double getPrecisionPercentage() {
        return precisionPercentage;
    }

    public double getAnglePrecision() {
        return anglePrecision;
    }

    public double getLongPrecision() {
        return longPrecision;
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
    
    public PlayerHand getPrimaryHand() {
        return handBody.getPrimary();
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
    
    public static class ReadableAbilityHand implements Cloneable {
        private ReadableAbility parent;
        public PlayerHand.Hand hand;
        public double normalPower;
        public double maxPower;
        public double spin;
        public double cuePrecision;  // 出杆, 范围0-100
        public double powerControl;
        public double spinControl;  // 范围0-100

        private CuePlayType cuePlayType;
        
        private PlayerHand originalHand;
        
        static ReadableAbilityHand fromPlayerHand(ReadableAbility parent, PlayerHand playerHand, double handFeelEffort) {
            ReadableAbilityHand rah = new ReadableAbilityHand();
            rah.parent = parent;
            rah.hand = playerHand.hand;
            rah.originalHand = playerHand;
            rah.normalPower = playerHand.controllablePowerPercentage;
            rah.maxPower = playerHand.maxPowerPercentage;
            rah.powerControl = playerHand.getPowerControl() * handFeelEffort;
            rah.spin = playerHand.getMaxSpinPercentage();
            rah.cuePlayType = playerHand.cuePlayType;
            
            rah.cuePrecision = playerHand.computeCuePrecision(handFeelEffort);
            rah.spinControl = playerHand.computeSpinControl(handFeelEffort);
            
            return rah;
        }

        @Override
        public ReadableAbilityHand clone() {
            ReadableAbilityHand rah;
            try {
                rah = (ReadableAbilityHand) super.clone();
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }

            return rah;
        }

        double average() {
            return toPlayerHand().average();
        }
        
        PlayerHand toPlayerHand() {
            double swingMag = calculateSwingMag(cuePrecision);
            double[] cuePoint = estimateCuePoint(cuePrecision, spinControl);
            
            return new PlayerHand(
                    hand,
                    maxPower,
                    normalPower,
                    spin,
                    originalHand.getPullDtExtensionDt(), 
                    swingMag,
                    cuePoint,
                    powerControl,
                    cuePlayType
            );
        }

        public double addPerksHowMany(int addWhat) {
            double unit;
            double minimum = 0.2;

            switch (addWhat) {
                case PerkManager.CUE_PRECISION:
                    unit = (120 - cuePrecision) * 0.04;
                    break;
                case PerkManager.POWER:
                    double sexPowerMax = 120 * parent.getSex().powerMul;
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
                default:
                    throw new RuntimeException("Unknown type " + addWhat);
            }
            return Math.max(unit, minimum);
        }

        public double getAbilityByCat(int what) {
            return switch (what) {
                case PerkManager.CUE_PRECISION -> cuePrecision;
                case PerkManager.POWER -> normalPower;
                case PerkManager.POWER_CONTROL -> powerControl;
                case PerkManager.SPIN -> spin;
                case PerkManager.SPIN_CONTROL -> spinControl;
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
                    case PerkManager.CUE_PRECISION -> {
                        cuePrecision += many;
                        cuePrecision = Math.min(cuePrecision, 100.0);
                    }
                    case PerkManager.POWER -> {
                        normalPower += many;
                        maxPower += many / 0.9;
                        maxPower = Math.min(maxPower, 100.0 * parent.getSex().powerMul);
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
                    default -> throw new RuntimeException("Unknown type " + addWhat);
                }
            }
        }

        @Override
        public String toString() {
            return "ReadableAbilityHand{" +
//                    "parent=" + parent +
                    ", hand=" + hand +
                    ", normalPower=" + normalPower +
                    ", maxPower=" + maxPower +
                    ", spin=" + spin +
                    ", cuePrecision=" + cuePrecision +
                    ", powerControl=" + powerControl +
                    ", spinControl=" + spinControl +
                    ", cuePlayType=" + cuePlayType +
                    ", \noriginalHand=" + originalHand +
                    '}';
        }
    }

    public static class ReadableAbility implements Cloneable {
        public String category;
        public double aiming;

        private String playerId;
        private String name;
        private String shownName;
        
        public ReadableAbilityHand left;
        public ReadableAbilityHand right;
        public ReadableAbilityHand rest;

        private HandBody handBodyBasic;
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
//            ra.cuePlayType = playerPerson.cuePlayType;
            ra.category = playerPerson.category;
            ra.aiming = playerPerson.getPrecisionPercentage() * handFeelEffort;
//            ra.cuePrecision =
//                    Math.max(0,
//                            100.0 - (playerPerson.getCuePointMuSigmaXY()[0] +
//                                    playerPerson.getCuePointMuSigmaXY()[1]) * CUE_PRECISION_FACTOR) * handFeelEffort;
//            ra.normalPower = playerPerson.getControllablePowerPercentage();
//            ra.maxPower = playerPerson.getMaxPowerPercentage();
//            ra.powerControl = playerPerson.getPowerControl() * handFeelEffort;
//            ra.spin = playerPerson.getMaxSpinPercentage();
//            ra.spinControl = Math.max(0,
//                    100.0 - (playerPerson.getCuePointMuSigmaXY()[2] +
//                            playerPerson.getCuePointMuSigmaXY()[3]) * CUE_PRECISION_FACTOR) * handFeelEffort;
            
            ra.left = ReadableAbilityHand.fromPlayerHand(ra, playerPerson.handBody.getLeft(), handFeelEffort);
            ra.right = ReadableAbilityHand.fromPlayerHand(ra, playerPerson.handBody.getRight(), handFeelEffort);
            ra.rest = ReadableAbilityHand.fromPlayerHand(ra, playerPerson.handBody.getRest(), handFeelEffort);

            ra.handBodyBasic = playerPerson.handBody;
            ra.originalPerson = playerPerson;
            return ra;
        }

        public Sex getSex() {
            return originalPerson.sex;
        }

        public HandBody getHandBody() {
            return handBodyBasic;
        }

        public String getName() {
            return name;
        }

        public String getShownName() {
            return shownName;
        }
        
        public ReadableAbilityHand primary() {
            double leftAvg = left.average();
            double rightAvg = right.average();
            return leftAvg > rightAvg ? left : right;
        }

        public ReadableAbilityHand antiHand() {
            ReadableAbilityHand primary = primary();
            return primary == left ? right : left;
        }

        /**
         * @return 非惯用手的好坏
         */
        public double getAnotherHandGoodness() {
            return antiHand().average();
        }

        public double getRestGoodness() {
            return rest.average();
        }

        public double addPerksHowMany(int addWhat) {
            double unit;
            double minimum = 0.2;

            switch (addWhat) {
                case PerkManager.AIMING:
                    unit = (120 - aiming) * 0.04;
                    break;
//                case PerkManager.CUE_PRECISION:
//                    unit = (120 - cuePrecision) * 0.04;
//                    break;
//                case PerkManager.POWER:
//                    double sexPowerMax = 120 * getSex().powerMul;
//                    unit = (sexPowerMax - normalPower) * 0.04;
//                    break;
//                case PerkManager.POWER_CONTROL:
//                    unit = (120 - powerControl) * 0.04;
//                    break;
//                case PerkManager.SPIN:
//                    unit = (120 - spin) * 0.04;
//                    break;
//                case PerkManager.SPIN_CONTROL:
//                    unit = (120 - spinControl) * 0.04;
//                    break;
//                case PerkManager.ANTI_HAND:
//                    unit = (120 - handBody.getAntiHand().skill) * 0.04;
//                    break;
//                case PerkManager.REST:
//                    unit = (120 - handBody.getRest().skill) * 0.06;
//                    minimum = 0.3;
//                    break;
                default:
                    throw new RuntimeException("Unknown type " + addWhat);
            }
            return Math.max(unit, minimum);
        }

        public double getAbilityByCat(int what) {
            return switch (what) {
                case PerkManager.AIMING -> aiming;
//                case PerkManager.CUE_PRECISION -> cuePrecision;
//                case PerkManager.POWER -> normalPower;
//                case PerkManager.POWER_CONTROL -> powerControl;
//                case PerkManager.SPIN -> spin;
//                case PerkManager.SPIN_CONTROL -> spinControl;
//                case PerkManager.ANTI_HAND -> handBody.getAntiHand().skill;
//                case PerkManager.REST -> handBody.getRest().skill;
                default -> throw new RuntimeException("Unknown type " + what);
            };
        }

        public static String getStringByCat(int what, PlayerHand.Hand hand) {
            return switch (what) {
                case PerkManager.AIMING -> "aiming";
                case PerkManager.CUE_PRECISION -> "cuePrecision" + "-" + hand.name();
                case PerkManager.POWER -> "normalPower" + "-" + hand.name();
                case PerkManager.POWER_CONTROL -> "powerControl" + "-" + hand.name();
                case PerkManager.SPIN -> "spin" + "-" + hand.name();
                case PerkManager.SPIN_CONTROL -> "spinControl" + "-" + hand.name();
//                case PerkManager.ANTI_HAND -> "antiHand";
//                case PerkManager.REST -> "rest";
                default -> throw new RuntimeException("Unknown type " + what);
            };
        }

        public double maxAbilityByCat(int what) {
            if (what == PerkManager.POWER) {
                return getSex().powerMul * 100;
            } else {
                return 100;
            }
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
//                    case PerkManager.CUE_PRECISION -> {
//                        cuePrecision += many;
//                        cuePrecision = Math.min(cuePrecision, 100.0);
//                    }
//                    case PerkManager.POWER -> {
//                        normalPower += many;
//                        maxPower += many / 0.9;
//                        maxPower = Math.min(maxPower, 100.0 * getSex().powerMul);
//                        normalPower = Math.min(normalPower, maxPower);
//                    }
//                    case PerkManager.POWER_CONTROL -> {
//                        powerControl += many;
//                        powerControl = Math.min(powerControl, 100.0);
//                    }
//                    case PerkManager.SPIN -> {
//                        spin += many;
//                        spin = Math.min(spin, 100.0);
//                    }
//                    case PerkManager.SPIN_CONTROL -> {
//                        spinControl += many;
//                        spinControl = Math.min(spinControl, 100.0);
//                    }
//                    case PerkManager.ANTI_HAND -> {
//                        handBody.getAntiHand().skill += many;
//                        handBody.getAntiHand().skill = Math.min(handBody.getAntiHand().skill, 100.0);
//                    }
//                    case PerkManager.REST -> {
//                        handBody.getRest().skill += many;
//                        handBody.getRest().skill = Math.min(handBody.getRest().skill, 100.0);
//                    }
                    default -> throw new RuntimeException("Unknown type " + addWhat);
                }
            }
        }

        public PlayerPerson toPlayerPerson() {
            if (multiplier != 1.0) {
                throw new RuntimeException("Cannot export to player person: this one is modified");
            }
            
            ReadableAbilityHand primary = primary();
            
            PlayerHand leftHand = left.toPlayerHand();
            PlayerHand rightHand = right.toPlayerHand();
            PlayerHand restHand = rest.toPlayerHand();
            
            HandBody handBody = new HandBody(
                    handBodyBasic.height,
                    handBodyBasic.bodyWidth,
                    leftHand,
                    rightHand,
                    restHand
            );
            
            PlayerPerson person = new PlayerPerson(
                    playerId,
                    name,
                    estimateCategory(aiming, primary.powerControl, primary.spinControl),
                    aiming,
                    1.0,
                    1.0,
                    (aiming + primary.spinControl) / 2,
                    originalPerson.aimingOffset,
                    getSex() == Sex.M ? 90.0 : 75.0,
                    null,
                    handBody,
                    getSex(),
                    originalPerson.underage,
                    originalPerson.isCustom,
                    originalPerson.participates
            );
            
//            PlayerPerson person = new PlayerPerson(
//                    playerId,
//                    name,
//                    maxPower,
//                    normalPower,
//                    spin,
//                    aiming,
//                    cuePrecision,
//                    1.0,
//                    1.0,
//                    powerControl,
//                    spinControl,
//                    getSex() == Sex.M ? 90.0 : 75.0,
//                    cuePlayType,
//                    null,
//                    true,
//                    handBody,
//                    getSex(),
//                    originalPerson.underage,
//                    originalPerson.participates
//            );
            person.privateCues.addAll(originalPerson.privateCues);
            return person;
        }

        @Override
        public ReadableAbility clone() {
            try {
                ReadableAbility clone = (ReadableAbility) super.clone();
                // TODO: copy mutable state here, so the clone can't change the internals of the original
                clone.left = left.clone();
                clone.right = right.clone();
                clone.rest = rest.clone();
//                clone.handBodyBasic = handBody.clone();
                return clone;
            } catch (CloneNotSupportedException e) {
                throw new AssertionError();
            }
        }
    }

//    public static class HandSkill implements Comparable<HandSkill>, Cloneable {
//        public final Hand hand;
//        public double skill;
//
//        HandSkill(Hand hand, double skill) {
//            this.hand = hand;
//            this.skill = skill;
//        }
//
//        @Override
//        protected Object clone() throws CloneNotSupportedException {
//            return super.clone();
//        }
//
//        @Override
//        public int compareTo(@NotNull PlayerPerson.HandSkill o) {
//            return -Double.compare(this.skill, o.skill);
//        }
//
//        @Override
//        public String toString() {
//            return "HandSkill{" +
//                    "hand=" + hand +
//                    ", skill=" + skill +
//                    '}';
//        }
//    }

    public static class HandBody implements Cloneable {
//        public static final HandBody DEFAULT =
//                new HandBody(180.0, 1.0, 50.0, 100.0, 80.0);

        public final double height;
        public final double bodyWidth;
        private final boolean leftHandRest;    // 是否用左手拿架杆

        private PlayerHand[] precedence = new PlayerHand[3];

        public HandBody(double height, double bodyWidth,
                        PlayerHand left, PlayerHand right, PlayerHand rest) {
            this.height = height;
            this.bodyWidth = bodyWidth;
            
            if (left.hand != PlayerHand.Hand.LEFT || right.hand != PlayerHand.Hand.RIGHT || rest.hand != PlayerHand.Hand.REST) {
                throw new IllegalArgumentException("Wrong hands");
            }

            precedence[0] = left;
            precedence[1] = right;
            precedence[2] = rest;

            Arrays.sort(precedence);
            this.leftHandRest = precedence[0].hand == PlayerHand.Hand.RIGHT;
        }

        public static HandBody createFromPrimary(double height, double bodyWidth,
                                                 PlayerHand primary, double secondarySkill, double restSkill) {
            boolean leftHandPrimary = primary.hand == PlayerHand.Hand.LEFT;
            PlayerHand secondary = primary.derive(
                    leftHandPrimary ? PlayerHand.Hand.RIGHT : PlayerHand.Hand.LEFT,
                    secondarySkill,
                    secondarySkill
            );
            PlayerHand rest = primary.derive(PlayerHand.Hand.REST,
                    restSkill,
                    restSkill * PlayerHand.REST_NATIVE_POWER_MUL);
            return new HandBody(height, bodyWidth,
                    leftHandPrimary ? primary : secondary,
                    leftHandPrimary ? secondary : primary,
                    rest);
        }

        public static HandBody createFromLeftRight(double height, double bodyWidth,
                                                   PlayerHand primary,
                                                   double leftSkill, double rightSkill, double restSkill) {
            boolean leftHandPrimary = primary.hand == PlayerHand.Hand.LEFT;
            
            double secondarySkill = leftHandPrimary ? rightSkill : leftSkill;
            
            PlayerHand secondary = primary.derive(
                    leftHandPrimary ? PlayerHand.Hand.RIGHT : PlayerHand.Hand.LEFT,
                    secondarySkill,
                    secondarySkill
            );
            PlayerHand rest = primary.derive(PlayerHand.Hand.REST,
                    restSkill,
                    restSkill * 0.8);
            return new HandBody(height, bodyWidth,
                    leftHandPrimary ? primary : secondary,
                    leftHandPrimary ? secondary : primary,
                    rest);
        }

//        public static double getPowerMulOfHand(@Nullable PlayerHand handSkill) {
//            return handSkill == null ? 1.0 : (handSkill.skill * handSkill.hand.powerMul / 100);
//        }
//
//        public static double getPrecisionOfHand(@Nullable PlayerHand handSkill) {
//            return handSkill == null ? 1.0 : (handSkill.skill / 100);
//        }
//
//        public static double getSdOfHand(@Nullable PlayerHand handSkill) {
//            return handSkill == null ? 1.0 : (100.0 / handSkill.skill);
//        }

        public int precedenceOfHand(PlayerHand.Hand hand) {
            for (int i = 0; i < precedence.length; i++) {
                if (precedence[i].hand == hand) return i;
            }
            throw new IndexOutOfBoundsException();
        }

        @Override
        protected HandBody clone() throws CloneNotSupportedException {
            HandBody clone = (HandBody) super.clone();
            clone.precedence = new PlayerHand[precedence.length];
            for (int i = 0; i < clone.precedence.length; i++) {
                clone.precedence[i] = precedence[i].clone();
            }
            return clone;
        }

        @NotNull
        public PlayerHand getPrimary() {
            return precedence[0];
        }

        @NotNull
        public PlayerHand getSecondary() {
            return precedence[1];
        }

        @NotNull
        public PlayerHand getThird() {
            return precedence[2];
        }

        public boolean isLeftHandRest() {
            return leftHandRest;
        }
        
        @NotNull
        public PlayerHand getDominantHand() {
            PlayerHand left = getLeft();
            PlayerHand right = getRight();
            return left.average() > right.average() ? left : right;
        }

        @NotNull
        public PlayerHand getAntiHand() {
            PlayerHand left = getLeft();
            PlayerHand right = getRight();
            return left.average() <= right.average() ? left : right;
        }

        public PlayerHand getLeft() {
            for (PlayerHand hs : precedence) {
                if (hs.hand == PlayerHand.Hand.LEFT) return hs;
            }
            throw new RuntimeException("Precedences are: " + Arrays.stream(precedence).map(k -> k.hand).toList());
        }

        public PlayerHand getRight() {
            for (PlayerHand hs : precedence) {
                if (hs.hand == PlayerHand.Hand.RIGHT) return hs;
            }
            throw new RuntimeException("Precedences are: " + Arrays.stream(precedence).map(k -> k.hand).toList());
        }

        public PlayerHand getRest() {
            for (PlayerHand hs : precedence) {
                if (hs.hand == PlayerHand.Hand.REST) return hs;
            }
            throw new RuntimeException("Precedences are: " + Arrays.stream(precedence).map(k -> k.hand).toList());
        }

        public PlayerHand getHandSkillByHand(PlayerHand.Hand hand) {
            for (PlayerHand handSkill : precedence) {
                if (handSkill.hand == hand) return handSkill;
            }
            throw new RuntimeException("No such hand");
        }
    }
}
