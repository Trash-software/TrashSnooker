package trashsoftware.trashSnooker.core;

import org.json.JSONObject;
import trashsoftware.trashSnooker.core.person.PlayerHand;

import java.util.ArrayList;
import java.util.List;

public class CuePlayType {

    public static final CuePlayType DEFAULT_PERFECT =
            new CuePlayType("p1.0,h800,e500;s");

    private double pullSpeedMul = 1.0;
    private long pullHoldMs = 800;
    private long endHoldMs = 500;
    private final List<Double> sequence = new ArrayList<>();
    private final String swingSeq;
    private SpecialAction specialAction;  // 暂定就0到1个

    public CuePlayType(String s) {
        String[] arr = s.split(";");
        String[] phe = arr[0].split(",");
        String[] playSeq = arr[1].split(",");

        for (String part : phe) {
            if (part.startsWith("p")) {
                pullSpeedMul = Double.parseDouble(part.substring(1));
            } else if (part.startsWith("h")) {
                pullHoldMs = Long.parseLong(part.substring(1));
            } else if (part.startsWith("e")) {
                endHoldMs = Long.parseLong(part.substring(1));
            }
        }
        
        this.swingSeq = arr[1];
        
        for (String part : playSeq) {
            switch (part) {
                case "s":
                    sequence.add(0.0);
                    break;
                case "l":
                    sequence.add(-1.0);
                    break;
                case "r":
                    sequence.add(1.0);
                    break;
            }
        }
    }
    
    public static CuePlayType createBySwing(String swing) {
        return new CuePlayType("p1.0,h800,e500;" + swing);
    }
    
    public String toJsonStr() {
        return String.format("p%.1f,h%d,e%d;%s", pullSpeedMul, pullHoldMs, endHoldMs, swingSeq);
    }

    public void setSpecialAction(SpecialAction specialAction) {
        this.specialAction = specialAction;
    }
    
    public boolean willApplySpecial(double selectedPower, PlayerHand.Hand hand) {
        return specialAction != null && specialAction.willApply(selectedPower, hand);
    }

    /**
     * 在 {@link CuePlayType#willApplySpecial(double, PlayerHand.Hand)} 返回true之后调用
     */
    public SpecialAction getSpecialAction() {
        return specialAction;
    }
    
    public boolean hasAnySpecialAction() {
        return specialAction != null;
    }
    
    public JSONObject specialActionsJson() {
        JSONObject object = new JSONObject();
        if (specialAction instanceof DoubleAction) {
            object.put("doubleCueAction", specialAction.toJson());
        }
        return object;
    }

    public double getPullSpeedMul() {
        return pullSpeedMul;
    }

    public long getPullHoldMs() {
        return pullHoldMs;
    }

    public long getEndHoldMs() {
        return endHoldMs;
    }

    public List<Double> getSequence() {
        return sequence;
    }
    
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("p%f,h%d,e%d;", pullSpeedMul, pullHoldMs, endHoldMs));
        if (sequence.isEmpty()) {
            builder.append("s");
        } else {
            for (double d : sequence) {
                if (d == 0.0) builder.append("s");
                else if (d == 1.0) builder.append("r");
                else if (d == -1.0) builder.append("l");
                builder.append(',');
            }
            builder.setLength(builder.length() - 1);
        }
        return builder.toString();
    }
    
    public static abstract class SpecialAction {
        abstract boolean willApply(double selectedPower, PlayerHand.Hand hand);
        
        abstract JSONObject toJson();
    }

    /**
     * 二段出杆动作
     */
    public static class DoubleAction extends SpecialAction {
        private final double minPower;  // 在这两个力之间会有
        private final double maxPower;
        public final double minDt;  // 余下的正常出杆最短行程
        public final double maxDt;
        public final int holdMs;
        public final double speedMul;  // 前段的速度
        
        DoubleAction(double minPower, double maxPower, double minDt, double maxDt, 
                            int holdMs, double speedMul) {
            this.minPower = minPower;
            this.maxPower = maxPower;
            this.holdMs = holdMs;
            this.minDt = minDt;
            this.maxDt = maxDt;
            this.speedMul = speedMul;
        }
        
        public static DoubleAction fromJson(JSONObject object) {
            return new CuePlayType.DoubleAction(
                    object.getDouble("minPower"),
                    object.getDouble("maxPower"),
                    object.getDouble("minDt"),
                    object.getDouble("maxDt"),
                    object.getInt("holdMs"),
                    object.getDouble("speed")
            );
        }

        @Override
        JSONObject toJson() {
            JSONObject json = new JSONObject();
            json.put("minPower", minPower);
            json.put("maxPower", maxPower);
            json.put("minDt", minDt);
            json.put("maxDt", maxDt);
            json.put("holdMs", holdMs);
            json.put("speed", speedMul);
            return json;
        }

        @Override
        boolean willApply(double selectedPower, PlayerHand.Hand hand) {
            return (hand == PlayerHand.Hand.LEFT || hand == PlayerHand.Hand.RIGHT) && 
                    (selectedPower >= minPower && selectedPower <= maxPower);
        }
        
        public double stoppingDtToWhite(double selectedPower) {
            return Algebra.shiftRange(minPower, maxPower, minDt, maxDt, selectedPower);
        }
    }
}
