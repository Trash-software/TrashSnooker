package trashsoftware.trashSnooker.core;

import java.util.ArrayList;
import java.util.List;

public class CuePlayType {

    public static final CuePlayType DEFAULT_PERFECT =
            new CuePlayType("p1.0,h800,e500;s");

    private double pullSpeedMul = 1.0;
    private long pullHoldMs = 800;
    private long endHoldMs = 500;
    private final List<Double> sequence = new ArrayList<>();

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
        for (double d: getSequence()) {
            if (d == 0.0) builder.append("s");
            else if (d == 1.0) builder.append("r");
            else if (d == -1.0) builder.append("l");
        }
        return builder.toString();
    }
}
