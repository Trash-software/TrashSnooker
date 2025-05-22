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
        int loserIndex = 1 - winnerIndex;
        int[] remBalls = new int[]{7, 7};
        int indexOnePlayBlack = -1;
        int maxRemDiff = 0;
        int breakPlayerIndex = fir.cueRecs.getFirst().player - 1;

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

            if (!cue.isFoul() && (score > 0 || (i == 0 && !cue.pots.isEmpty()))) {
                // 这里要处理开球进的情况，因为在现有算分体系下开球进球是不加分的
                currentBreak.breakCues++;
            } else {
                breaks[p].add(currentBreak);
                currentBreak = new Break(i + 1);
            }

            if (cue.isFoul()) {
                currentBreak.foul = true;
            }

            for (int bi = 0; bi < 2; bi++) {
                remBalls[bi] = 7 - cue.scoresAfter[bi];
                if (remBalls[bi] == 0 && indexOnePlayBlack == -1) {
                    indexOnePlayBlack = i;
                }
            }
            int scoreDiff = cue.scoresAfter[0] - cue.scoresAfter[1];
            if (Math.abs(scoreDiff) > Math.abs(maxRemDiff)) {
                maxRemDiff = scoreDiff;
            }
        }

        // catch last break, 0分的也算进来了
        if (currentBreak.breakCues > 0) {
            breaks[lastCue.getPlayer() - 1].add(currentBreak);
//            if (currentBreak.goodBreak()) {
//                goodBreaks[lastCue.getPlayer() - 1].add(currentBreak);
//            }
        }
        fillPotBreaks();
        
        if (breaks[winnerIndex].size() == 1 && breaks[loserIndex].isEmpty()) {
            frameKinds.add(FrameKind.BREAK_CLEAR);
        }
        if (potBreaks[winnerIndex].size() == 1 && winnerIndex != breakPlayerIndex) {
            frameKinds.add(FrameKind.CONTINUE_CLEAR);
        }

        System.out.println(breaks[0]);
        System.out.println(breaks[1]);
    }
}
