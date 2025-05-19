package trashsoftware.trashSnooker.core.infoRec;

import trashsoftware.trashSnooker.core.metrics.GameRule;

public class EightBallFrameAnalyze extends FrameAnalyze<FrameAnalyze.Break> {
    protected EightBallFrameAnalyze(GameRule gameRule) {
        super(gameRule);
    }

    @Override
    protected void analyze(FrameInfoRec fir) {
        int nCues = fir.cueRecs.size();
//        remainingBallsCount = new int[nCues];

        int winner = fir.winner;
        int winnerIndex = winner - 1;
        int[] remBalls = new int[2];

        CueInfoRec lastCue = fir.cueRecs.getLast();
        // 分析单杆数据
        Break currentBreak = new Break(0);
        for (int i = 0; i < nCues; i++) {
            CueInfoRec cue = fir.cueRecs.get(i);
            if (currentBreak.player == 0) {
                currentBreak.player = cue.player;
            }
            int p = cue.player - 1;
            int score = cue.gainScores[p];

            if (!cue.isFoul() && score > 0) {
                currentBreak.breakCues++;
            } else {
                breaks[p].add(currentBreak);
                currentBreak = new SnookerFrameAnalyze.SnookerBreak(i + 1);
            }

            if (cue.isFoul()) {
                currentBreak.foul = true;
            }

//            // 反正红球进了无论如何不捡出来
//            remBalls -= cue.pots.getOrDefault(1, 0);
//
//            // 清彩
//            if (cue.target >= 2 && !cue.isFoul() && !cue.isSnookerFreeBall()) {
//                if (remBalls > 6) {
//                    System.err.println("Something went wrong: when start clearing colors, remaining " +
//                            "ball is still " + remBalls);
//                    remBalls = 8 - cue.target;  // 这一杆开始时剩余的球数，比如黄球，8 - 2 = 6
//                }
//                remBalls -= cue.pots.getOrDefault(cue.target, 0);
//            }
//            if (remBalls == 7 && indexWhenOneRedRemain == -1) {
//                indexWhenOneRedRemain = i;
//            }
//            remainingBallsCount[i] = remBalls;
//
//            int scoreDiff = cue.scoresAfter[0] - cue.scoresAfter[1];
//            if (Math.abs(scoreDiff) > Math.abs(maxScoreDiff)) {
//                maxScoreDiff = scoreDiff;
//            }
        }

        // catch last break, 0分的也算进来了
        if (currentBreak.breakCues > 0) {
            breaks[lastCue.getPlayer() - 1].add(currentBreak);
//            if (currentBreak.goodBreak()) {
//                goodBreaks[lastCue.getPlayer() - 1].add(currentBreak);
//            }
        }
        fillPotBreaks();
    }
}
