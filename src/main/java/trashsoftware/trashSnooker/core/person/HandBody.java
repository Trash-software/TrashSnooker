package trashsoftware.trashSnooker.core.person;

import org.jetbrains.annotations.NotNull;
import trashsoftware.trashSnooker.core.cue.CueBrand;
import trashsoftware.trashSnooker.core.metrics.TableMetrics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

public class HandBody implements Cloneable {
    public final double height;
    public final double bodyWidth;
    private final boolean leftHandRest;    // 是否用左手持架杆，注意不是杆

    private PlayerHand[] precedence = new PlayerHand[3];

    transient double nonDominantGeneral;
    transient double restGeneral;

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
        if (rest.getRestUseHand() != null) {
            leftHandRest = rest.getRestUseHand() == PlayerHand.Hand.RIGHT;
        } else {
            leftHandRest = precedence[0].hand == PlayerHand.Hand.RIGHT;
        }

//            System.out.println("---Precedence:");
//            for (PlayerHand ph : precedence) {
//                System.out.println(ph.hand + ": " + ph.average());
//            }

        PlayerHand dominant = getDominantHand();
        double dominantAvg = dominant.average();
        PlayerHand nonDominant = getAntiHand();
        double nonDomAvg = nonDominant.average();
        PlayerHand restHand = getRest();
        double restAvg = restHand.average();

        this.nonDominantGeneral = nonDomAvg / dominantAvg;
        this.restGeneral = restAvg / dominantAvg;
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

    public int precedenceOfHand(PlayerHand.Hand hand) {
        for (int i = 0; i < precedence.length; i++) {
            if (precedence[i].hand == hand) return i;
        }
        throw new IndexOutOfBoundsException();
    }
    
    public static double[] restStandingPosition(double whiteX, double whiteY,
                                                double aimingX, double aimingY,
                                                double cueAngleDeg,
                                                PlayerPerson person,
                                                double cueLength) {
        double upBodyLength = person.handBody.height * 10 - 851;
        double cosAngle = Math.cos(Math.toRadians(cueAngleDeg));
//        double heightMul = 1.75 * Math.cos(Math.toRadians(cueAngleDeg));

        double cueTailX = whiteX - cueLength * aimingX * cosAngle;
        double cueTailY = whiteY - cueLength * aimingY * cosAngle;
        
        // 身体大约在斜后方。这里是横向的距离
        int sign = person.handBody.leftHandRest ? -1 : 1;
        double waistToAssX = aimingY * upBodyLength * 0.4 * sign;
        double waistToAssY = -aimingX * upBodyLength * 0.4 * sign;
        
        return new double[]{
                cueTailX - aimingX * upBodyLength * 1.05 + waistToAssX,
                cueTailY - aimingY * upBodyLength * 1.05 + waistToAssY
        };
    }

    public static double[][] personStandingPosition(double whiteX, double whiteY,
                                                    double aimingX, double aimingY,
                                                    double cueAngleDeg,
                                                    PlayerPerson person,
                                                    PlayerHand.Hand hand,
                                                    double cueLength) {
        double upBodyLength = person.handBody.height * 10 - 851;
        double cosAngle = Math.cos(Math.toRadians(cueAngleDeg));
        
        double cueTailX = whiteX - cueLength * aimingX * cosAngle;
        double cueTailY = whiteY - cueLength * aimingY * cosAngle;

        int sign = hand == PlayerHand.Hand.LEFT ? -1 : 1;
        double widthMulMin = person.handBody.bodyWidth * 250.0 * sign;
        double widthMulMax = upBodyLength * 0.68 * sign;
        double personWidthX1 = aimingY * widthMulMin;
        double personWidthY1 = -aimingX * widthMulMin;
        double personWidthX2 = aimingY * widthMulMax;
        double personWidthY2 = -aimingX * widthMulMax;

        return new double[][]{
                {cueTailX + personWidthX1,
                        cueTailY + personWidthY1},
                {cueTailX + personWidthX2,
                        cueTailY + personWidthY2},
        };
    }

    /**
     * 返回这个位置可用的所有手，以优先级排序
     */
    public static List<PlayerHand.CueHand> getPlayableHands(double whiteX,
                                                            double whiteY,
                                                            double aimingX,
                                                            double aimingY,
                                                            double cueAngleDeg,
                                                            TableMetrics tableMetrics,
                                                            PlayerPerson person,
                                                            CueBrand cueBrand) {
        List<PlayerHand.CueHand> result = new ArrayList<>();
//        result.add(PlayerHand.Hand.REST);

        class PosCal {
            boolean proceed(PlayerHand.Hand hand, PlayerHand.CueExtension extension) {
                double cueLength = cueBrand.getWoodPartLength() + cueBrand.getExtensionLength(extension);
                double[][] standPos = personStandingPosition(whiteX, whiteY,
                        aimingX, aimingY,
                        cueAngleDeg,
                        person,
                        hand,
                        cueLength);
                if (!tableMetrics.isInOuterTable(standPos[0][0], standPos[0][1]) ||
                        !tableMetrics.isInOuterTable(standPos[1][0], standPos[1][1])) {
                    result.add(new PlayerHand.CueHand(hand, cueBrand, extension));
                    return true;
                }
                return false;
            }
            
            boolean processRest(PlayerHand.CueExtension extension) {
                double cueLength = cueBrand.getWoodPartLength() + cueBrand.getExtensionLength(extension);
                double[] standPos = restStandingPosition(whiteX, whiteY,
                        aimingX, aimingY,
                        cueAngleDeg,
                        person,
                        cueLength);
                if (!tableMetrics.isInOuterTable(standPos[0], standPos[1])) {
                    result.add(new PlayerHand.CueHand(PlayerHand.Hand.REST, cueBrand, extension));
                    return true;
                }
                return false;
            }
        }
        
        PosCal posCal = new PosCal();
        if (!posCal.proceed(PlayerHand.Hand.LEFT, PlayerHand.CueExtension.NO)) {
            posCal.proceed(PlayerHand.Hand.LEFT, PlayerHand.CueExtension.SHORT);
        }
        if (!posCal.proceed(PlayerHand.Hand.RIGHT, PlayerHand.CueExtension.NO)) {
            posCal.proceed(PlayerHand.Hand.RIGHT, PlayerHand.CueExtension.SHORT);
        }
        if (!posCal.processRest(PlayerHand.CueExtension.NO)) {
            if (!posCal.processRest(PlayerHand.CueExtension.SHORT)) {
                if (!posCal.processRest(PlayerHand.CueExtension.SOCKET)) {
                    // 双层套筒无论如何都行
                    result.add(new PlayerHand.CueHand(PlayerHand.Hand.REST, cueBrand, PlayerHand.CueExtension.SOCKET_DOUBLE));
                }
            }
        }
        
        result.sort((b, a) -> Double.compare(person.handBody.getHandGeneralMultiplier(a), 
                person.handBody.getHandGeneralMultiplier(b)));

        return result;
    }

    public static CuePlayerHand getBestHandFromPosition(double whiteX,
                                                        double whiteY,
                                                        double aimingX,
                                                        double aimingY,
                                                        double cueAngleDeg,
                                                        TableMetrics tableMetrics,
                                                        PlayerPerson person,
                                                        CueBrand cueBrand) {
        List<PlayerHand.CueHand> cueHands = getPlayableHands(
                whiteX, whiteY, aimingX, aimingY, cueAngleDeg, tableMetrics, person, cueBrand);
        PlayerHand.CueHand cueHand = cueHands.getFirst();
        return new CuePlayerHand(person.handBody.getHandSkillByHand(cueHand.hand), 
                cueBrand, 
                cueHand.extension);
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

    public double getNonDominantGeneral() {
        return nonDominantGeneral;
    }

    public double getRestGeneral() {
        return restGeneral;
    }
    
    public double getHandGeneralMultiplier(PlayerHand.CueHand cueHand) {
        if (cueHand.hand == getDominantHand().hand) return cueHand.extension.factor;
        else if (cueHand.hand == getAntiHand().hand) return nonDominantGeneral * cueHand.extension.factor;
        else return restGeneral * cueHand.extension.factor;
    }

    public double getHandGeneralMultiplier(CuePlayerHand cuePlayerHand) {
        if (cuePlayerHand.playerHand == getDominantHand()) return cuePlayerHand.extension.factor;
        else if (cuePlayerHand.playerHand == getAntiHand())
            return nonDominantGeneral * cuePlayerHand.extension.factor;
        else return restGeneral * cuePlayerHand.extension.factor;
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

    public CuePlayerHand getHandSkillByHand(PlayerHand.CueHand cueHand) {
        for (PlayerHand handSkill : precedence) {
            if (handSkill.hand == cueHand.hand) {
                return new CuePlayerHand(handSkill,
                        cueHand.getCueBrand(),
                        cueHand.extension);
            }
        }
        throw new RuntimeException("No such hand");
    }
}
