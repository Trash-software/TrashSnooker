package trashsoftware.trashSnooker.core.snooker;

import trashsoftware.trashSnooker.core.metrics.GameValues;

public enum MaximumType {
    NONE,
    MAXIMUM_75,
    MAXIMUM_107,
    MAXIMUM_147,
    MAXIMUM_167;
    
    public static MaximumType inferFromScore(int nReds, int breakScore) {
        return switch (nReds) {
            case 15 -> breakScore >= 147 ? MAXIMUM_147 : NONE;
            case 10 -> breakScore >= 107 ? MAXIMUM_107 : NONE;
            case 6 -> breakScore >= 75 ? MAXIMUM_75 : NONE;
            default -> NONE;
        };
    }
}
