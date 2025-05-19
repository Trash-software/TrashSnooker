package trashsoftware.trashSnooker.core.infoRec;

import org.jetbrains.annotations.NotNull;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.core.snooker.AbstractSnookerGame;

import java.util.*;

public class SnookerFrameAnalyze extends FrameAnalyze<SnookerFrameAnalyze.SnookerBreak> {
    
    @SuppressWarnings("unchecked")
    private final List<SnookerBreak>[] goodBreaks = (List<SnookerBreak>[]) new List<?>[2];
    private final SnookerBreak[] highestBreaks = new SnookerBreak[2];
    private final int[] foulGains = new int[2];
    private int[] remainingBallsCount;  // 每一杆结束后，台面上剩余球数，不计算白球，以及如金球那种特殊的
    private int maxScoreDiff;

    SnookerFrameAnalyze(GameRule rule) {
        super(rule);
        
        goodBreaks[0] = new ArrayList<>();
        goodBreaks[1] = new ArrayList<>();
    }

    @Override
    protected void analyze(FrameInfoRec fir) {
        int remBalls = AbstractSnookerGame.nBalls(rule);
        int nCues = fir.cueRecs.size();
        remainingBallsCount = new int[nCues];

        int winner = fir.winner;

        int winnerIndex = winner - 1;

        CueInfoRec lastCue = fir.cueRecs.getLast();

        int winnerScore = lastCue.scoresAfter[winnerIndex];

        // 这个是进入尾盘的标志
        int indexWhenOneRedRemain = -1;

        // 分析单杆数据
        SnookerBreak currentBreak = new SnookerBreak(0);
        for (int i = 0; i < nCues; i++) {
            CueInfoRec cue = fir.cueRecs.get(i);
            if (currentBreak.player == 0) {
                currentBreak.player = cue.player;
            }
            int p = cue.player - 1;
            int score = cue.gainScores[p];

            if (!cue.isFoul() && score > 0) {
                currentBreak.breakScore += score;
                currentBreak.breakCues++;
            } else {
                breaks[p].add(currentBreak);
                if (currentBreak.goodBreak()) {
                    goodBreaks[p].add(currentBreak);
                }
                currentBreak = new SnookerBreak(i + 1);
            }

            if (cue.isFoul()) {
                currentBreak.foul = true;
                foulGains[0] += cue.gainScores[0];
                foulGains[1] += cue.gainScores[1];
            }

            // 反正红球进了无论如何不捡出来
            remBalls -= cue.pots.getOrDefault(1, 0);

            // 清彩
            if (cue.target >= 2 && !cue.isFoul() && !cue.isSnookerFreeBall()) {
                if (remBalls > 6) {
                    System.err.println("Something went wrong: when start clearing colors, remaining " +
                            "ball is still " + remBalls);
                    remBalls = 8 - cue.target;  // 这一杆开始时剩余的球数，比如黄球，8 - 2 = 6
                }
                remBalls -= cue.pots.getOrDefault(cue.target, 0);
            }
            if (remBalls == 7 && indexWhenOneRedRemain == -1) {
                indexWhenOneRedRemain = i;
            }
            remainingBallsCount[i] = remBalls;

            int scoreDiff = cue.scoresAfter[0] - cue.scoresAfter[1];
            if (Math.abs(scoreDiff) > Math.abs(maxScoreDiff)) {
                maxScoreDiff = scoreDiff;
            }
        }

        // catch last break, 0分的也算进来了
        if (currentBreak.breakCues > 0) {
            breaks[lastCue.getPlayer() - 1].add(currentBreak);
            if (currentBreak.goodBreak()) {
                goodBreaks[lastCue.getPlayer() - 1].add(currentBreak);
            }
        }
        for (int i = 0; i < highestBreaks.length; i++) {
            highestBreaks[i] = Collections.max(breaks[i]);
//            System.out.println(highestBreaks[i]);
        }
        fillPotBreaks();

        // 分析是否是翻盘
        if ((fir.winner == 1 && maxScoreDiff < 0) || (fir.winner == 2 && maxScoreDiff > 0)) {
            int absMSD = Math.abs(maxScoreDiff);
            if (winnerScore <= absMSD + 18) {
                // 暂定为3颗球翻盘？
                frameKinds.add(FrameKind.COMEBACK);
            }
        }

        int gameMaxCues = AbstractSnookerGame.maxBreakCueCount(rule);
        // 分析是否为单杆制胜
        SnookerBreak winnerBestBreak = highestBreaks[winnerIndex];
        int winnerFoulGains = foulGains[winnerIndex];
        if (winnerBestBreak.getBreakCues() >= gameMaxCues * 0.75 ||
                winnerBestBreak.getBreakScore() >= (winnerScore - winnerFoulGains) * 0.9) {
            frameKinds.add(FrameKind.SINGLE_BREAK_WIN);
            return;
        }

        // 至少打进一颗球的
        List<SnookerBreak> winnerPotBreaks = potBreaks[winnerIndex];

        // 判断尾盘
        int remBallWhenFinish = remainingBallsCount[nCues - 1];
        boolean endGameBattle = false;
        // 还要检查index是否有效，--因为可能concede-- 并不因为什么，单纯检查一下
        if (indexWhenOneRedRemain != -1) {
            // 从台面仅剩一颗红球开始，后面发生了多少杆
            int cuesAfterOneRedRemain = nCues - indexWhenOneRedRemain;
            System.out.println(Arrays.toString(remainingBallsCount));
            System.out.println("One red rem: " + indexWhenOneRedRemain + ", " + cuesAfterOneRedRemain);
            if (cuesAfterOneRedRemain >= 14 - remBallWhenFinish) {
                endGameBattle = true;
                frameKinds.add(FrameKind.END_BATTLE);
            }
        }

        List<SnookerBreak> winnerGoodBreaks = goodBreaks[winnerIndex];

        // 乱局
        if (winnerGoodBreaks.isEmpty()) {
            frameKinds.add(FrameKind.SCRAPPY);
            return;
        }

        // 判断中盘，在乱局之后：没有单杆较高分的不能算中盘争夺决胜
//        if (!endGameBattle) {
            Break winningBreak = winnerGoodBreaks.getLast();
            int breakLocation = winnerPotBreaks.indexOf(winningBreak);
            if (breakLocation != -1 && breakLocation >= winnerPotBreaks.size() - 2) {
                // 是最后的一两次上手
                System.out.println("Winning break: " + winningBreak);
                int indexBefore = winningBreak.fromIndex - 1;
                CueInfoRec cirBeforeThisBreak = fir.cueRecs.get(indexBefore);
                double allRedMaxScore = (AbstractSnookerGame.nBalls(rule) - 6) * 8;
                double scoreDiffHere = Math.abs(cirBeforeThisBreak.scoresAfter[0] - cirBeforeThisBreak.scoresAfter[1]);
                if (scoreDiffHere < allRedMaxScore * 0.5) {
                    List<Break> interactive = findInteractiveRoundsBefore(indexBefore, 3);
                    System.out.println("Inter: " + interactive);
                    if (interactive.size() >= 4) {
                        frameKinds.add(FrameKind.MID_BATTLE);
                    }
                }
            }
//        }

        if (winnerPotBreaks.size() < 5) {
            frameKinds.add(FrameKind.FEW_VISIT_WIN);
        }

        Collections.sort(frameKinds);
    }

    @Override
    public String toString() {
        return "SnookerFrameAnalyze{" +
                "rule=" + rule +
                ", breaks=" + Arrays.toString(breaks) +
                ", highestBreaks=" + Arrays.toString(highestBreaks) +
                '}';
    }

    public static class SnookerBreak extends Break {

        int breakScore;

        SnookerBreak(int fromIndex) {
            super(fromIndex);
        }

        @Override
        public int compareTo(@NotNull Break o) {
            return Integer.compare(breakScore, ((SnookerBreak) o).breakScore);
        }

        @Override
        public boolean valid() {
            return breakScore > 0;
        }

        public int getBreakScore() {
            return breakScore;
        }

        public boolean goodBreak() {
            return breakScore >= 25 || breakCues >= 8;
        }

        @Override
        public String toString() {
            return "SnookerBreak{" +
                    "player=" + player +
                    ", breakScore=" + breakScore +
                    ", fromIndex=" + fromIndex +
                    ", breakCues=" + breakCues +
                    ", 'lastIncludedIndex'=" + getLastIncludedIndex() +
                    '}';
        }
    }
}
