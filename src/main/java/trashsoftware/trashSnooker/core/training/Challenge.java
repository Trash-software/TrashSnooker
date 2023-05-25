package trashsoftware.trashSnooker.core.training;

import trashsoftware.trashSnooker.core.metrics.GameRule;

import java.util.Objects;

public class Challenge {
    public final GameRule rule;
    public final TrainType type;

    public Challenge(GameRule rule, TrainType trainType) {
        this.rule = rule;
        this.type = trainType;
    }
    
    public static Challenge fromJson(String jsonString) {
        String[] spl = jsonString.split("\\+");
        return new Challenge(GameRule.valueOf(spl[0]), TrainType.valueOf(spl[1]));
    }
    
    public String toJsonString() {
        return rule.name() + "+" + type.name();
    }

    @Override
    public String toString() {
        return rule.toString() + " " + type.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Challenge challenge = (Challenge) o;
        return rule == challenge.rule && type == challenge.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(rule, type);
    }
}
