package trashsoftware.trashSnooker.core.infoRec;

import org.jetbrains.annotations.NotNull;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.util.Util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

public abstract class FrameAnalyze<B extends FrameAnalyze.Break> {

    @SuppressWarnings("unchecked")
    protected final List<B>[] breaks = (List<B>[]) new List<?>[2];
    @SuppressWarnings("unchecked")
    protected final List<B>[] potBreaks = (List<B>[]) new List<?>[2];
    protected final GameRule rule;
    protected final List<FrameKind> frameKinds = new ArrayList<>();

    protected FrameAnalyze(GameRule gameRule) {
        this.rule = gameRule;

        for (int bi = 0; bi < breaks.length; bi++) {
            breaks[bi] = new ArrayList<>();
        }
    }

    protected void fillPotBreaks() {
        for (int bi = 0; bi < breaks.length; bi++)
            potBreaks[bi] = breaks[bi].stream()
                    .filter(Break::valid)
                    .toList();
    }

    protected abstract void analyze(FrameInfoRec fir);

    @NotNull
    public List<FrameKind> getFrameKinds() {
        return frameKinds;
    }

    public List<B> getBreaks(int playerFrom1) {
        return breaks[playerFrom1 - 1];
    }

    List<Break> findInteractiveRoundsBefore(int cueIndex, int interMax) {
        int playerIndex = -1;
        List<Break> result = new ArrayList<>();
        int[] breakIndexes = new int[2];
//        System.out.println("Looking for " + cueIndex);
//        System.out.println("P1 breaks:" + breaks[0]);
//        System.out.println("P2 breaks:" + breaks[1]);

        for (int bi = 0; bi < 2; bi++) {
            List<? extends Break> playerBreak = breaks[bi];
            // inclusive
            int indexInBreak = 0;
            for (; indexInBreak < playerBreak.size(); indexInBreak++) {
                Break b = playerBreak.get(indexInBreak);
                if (b.fromIndex > cueIndex) {
                    indexInBreak -= 1;
                    break;
                }
                if (b.getLastIncludedIndex() >= cueIndex) {
                    playerIndex = bi;
//                    if (bi != playerIndex) System.err.println("Found exact player index but not matched with provided");
                    break;
                }
            }

            breakIndexes[bi] = indexInBreak;
        }
//        System.out.println("Starts: " + Arrays.toString(breakIndexes));
        if (playerIndex == -1) {
            System.err.println("Exact player index not found");
            playerIndex = 0;
        }

        OUT_LOOP:
        while (breakIndexes[0] >= 0 || breakIndexes[1] >= 0) {
            // 一定要先检查击球的那个人
            int[] order = playerIndex == 0 ? new int[]{0, 1} : new int[]{1, 0};
            for (int bi : order) {
                if (breakIndexes[bi] < 0) continue;
                Break b = breaks[bi].get(breakIndexes[bi]--);
                if (b.breakCues > interMax) {
                    break OUT_LOOP;
                } else {
                    result.add(b);
                }
            }
        }
        result.sort(Comparator.comparingInt(a -> a.fromIndex));

        return result;
    }

    public static class Break implements Comparable<Break> {
        int player;
        final int fromIndex;
        int breakCues;
        boolean foul;

        Break(int fromIndex) {
            this.fromIndex = fromIndex;
        }

        @Override
        public int compareTo(@NotNull SnookerFrameAnalyze.Break o) {
            return Integer.compare(breakCues, o.breakCues);
        }

        boolean cueIndexInsideThisBreak(int cueIndex) {
            return cueIndex >= fromIndex && cueIndex <= getLastIncludedIndex();
        }

        int getLastIncludedIndex() {
            return fromIndex + breakCues;
        }

        public int getBreakCues() {
            return breakCues;
        }

        public int getFromIndex() {
            return fromIndex;
        }

        public boolean validContinuous() {
            return breakCues >= 2;
        }
        
        public boolean valid() {
            return !foul && breakCues > 0;
        }

        @Override
        public String toString() {
            return "Break{" +
                    "player=" + player +
                    ", fromIndex=" + fromIndex +
                    ", breakCues=" + breakCues +
                    ", 'lastIncludedIndex'=" + getLastIncludedIndex() +
                    '}';
        }
    }

    public enum FrameKind {
        NORMAL,
        RESTARTED,
        BREAK_CLEAR,
        CONTINUE_CLEAR,
        SINGLE_BREAK_WIN,
        FEW_VISIT_WIN,
        MID_BATTLE,
        END_BATTLE,
        COMEBACK,
        SCRAPPY;

        private final String specifiedKey;

        FrameKind(String specifiedKey) {
            this.specifiedKey = specifiedKey;
        }

        FrameKind() {
            this(null);
        }

        public String shown(ResourceBundle strings) {
            String key = stringKey();
            return strings.containsKey(key) ? strings.getString(key) : name();
        }

        public String stringKey() {
            return specifiedKey == null ?
                    Util.toLowerCamelCase("FRAME_KIND_" + name()) : specifiedKey;
        }
    }
}
