package trashsoftware.trashSnooker.core.career.challenge;

import org.jetbrains.annotations.NotNull;
import trashsoftware.trashSnooker.fxml.App;

import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;

public abstract class RewardCondition implements Comparable<RewardCondition> {

    public static final RewardCondition CLEARANCE = new RewardCondition(10) {
        @Override
        public boolean fulfilled(ChallengeHistory.Record record) {
            return record.clearedAll;
        }

        @Override
        public String toJsonString() {
            return "clearance";
        }

        @Override
        public String toReadable() {
            return App.getStrings().getString("challengeClearance");
        }
    };

    private static final Map<String, BiFunction<Integer, Integer, Boolean>> BINARY = Map.of(
            ">", (a, b) -> a > b,
            ">=", (a, b) -> a >= b
    );
    
    private final int primaryPrecedence;
    
    RewardCondition(int primaryPrecedence) {
        this.primaryPrecedence = primaryPrecedence;
    }

    public static RewardCondition parse(String text) {
        if (text.equals("clearance")) {
            return CLEARANCE;
        }
        String[] tokens = text.split(",");
        if (tokens.length == 3) {
            String operator = tokens[0].strip();
            String what = tokens[1].strip();
            int number = Integer.parseInt(tokens[2].strip());

            BiFunction<Integer, Integer, Boolean> func = BINARY.get(operator);

            if ("score".equals(what)) {
                return new ScoreComparator(
                        String.join(",", new String[]{operator, what, String.valueOf(number)}), 
                        func, 
                        number,
                        // todo: 这里假设了一定是大于等于。但应该也没别的的吧？
                        String.format(App.getStrings().getString("challengeOverScore"), number)
                );
            }
        }
        throw new RuntimeException("Cannot parse " + text);
    }

    @Override
    public int compareTo(@NotNull RewardCondition o) {
        int primary = Integer.compare(primaryPrecedence, o.primaryPrecedence);
        if (primary != 0) return primary;
        
        return Integer.compare(secondaryPrecedence(), o.secondaryPrecedence());
    }
    
    protected int secondaryPrecedence() {
        return 0;
    }

    public abstract boolean fulfilled(ChallengeHistory.Record record);

    public abstract String toJsonString();
    
    public abstract String toReadable();

    @Override
    public String toString() {
        return toJsonString();
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(toJsonString());
    }

    @Override
    public boolean equals(Object obj) {
        if (getClass() != obj.getClass()) return false;
        
        RewardCondition other = (RewardCondition) obj;
        return Objects.equals(toJsonString(), other.toJsonString());
    }

    public static class ScoreComparator extends RewardCondition {

        private final String string;
        private final BiFunction<Integer, Integer, Boolean> func;
        private final int number;
        private final String readable;

        private ScoreComparator(String string, 
                                BiFunction<Integer, Integer, Boolean> func, 
                                int number,
                                String readable) {
            super(5);
            this.string = string;
            this.func = func;
            this.number = number;
            this.readable = readable;
        }

        @Override
        public boolean fulfilled(ChallengeHistory.Record record) {
            return func.apply(record.score, number);
        }

        @Override
        public String toJsonString() {
            return string;
        }

        @Override
        public String toReadable() {
            return readable;
        }

        @Override
        protected int secondaryPrecedence() {
            return number;
        }
    }
}
