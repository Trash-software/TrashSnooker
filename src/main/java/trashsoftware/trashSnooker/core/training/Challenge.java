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
