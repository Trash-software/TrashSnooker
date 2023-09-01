package trashsoftware.trashSnooker.fxml.statsViews;

import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import trashsoftware.trashSnooker.core.career.championship.MatchTreeNode;
import trashsoftware.trashSnooker.core.career.championship.MetaMatchInfo;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.fxml.widgets.AdversarialBar;
import trashsoftware.trashSnooker.fxml.widgets.MatchRecordPage;
import trashsoftware.trashSnooker.util.Util;
import trashsoftware.trashSnooker.util.db.DBAccess;
import trashsoftware.trashSnooker.util.db.EntireGameRecord;
import trashsoftware.trashSnooker.util.db.EntireGameTitle;
import trashsoftware.trashSnooker.util.db.PlayerFrameRecord;

import java.util.Map;
import java.util.ResourceBundle;

public class MatchRecord extends RecordTree {
    final EntireGameTitle egt;

    MatchRecord(EntireGameTitle egt, ResourceBundle strings) {
        super(egt.toString(), strings);
        this.egt = egt;
    }

    @Override
    void setRightPane(Pane rightPane) {
        rightPane.getChildren().clear();

        EntireGameRecord matchRec = DBAccess.getInstance().getMatchDetail(egt);
        MetaMatchInfo careerMatchInfo = MatchTreeNode.analyzeMatchId(egt.matchId);

        MatchRecordPage page = new MatchRecordPage();

        int rowIndex = 0;

        if (careerMatchInfo != null) {
            page.add(new Label(String.valueOf(careerMatchInfo.year)), 3, rowIndex);
            page.add(new Label(careerMatchInfo.data.getName()), 4, rowIndex);
            page.add(new Label(careerMatchInfo.stage.getShown()), 5, rowIndex);
            rowIndex++;
        }

        String[] winLost;
        int[] p1p2Wins = matchRec.getP1P2WinsCount();
        if (p1p2Wins[0] < egt.totalFrames / 2 + 1 && p1p2Wins[1] < egt.totalFrames / 2 + 1) {
            winLost = new String[]{"", "IN PROGRESS", ""};
        } else {
            if (p1p2Wins[0] > p1p2Wins[1]) {
                winLost = new String[]{"WIN", "", "LOST"};
            } else {
                winLost = new String[]{"LOST", "", "WIN"};
            }
        }
        page.add(new Label(winLost[0]), 1, rowIndex);
        page.add(new Label(winLost[1]), 4, rowIndex);
        page.add(new Label(winLost[2]), 7, rowIndex);
        rowIndex++;

        String p1Ai = egt.player1isAi ? strings.getString("typeComputer") : strings.getString("typePlayer");
        String p2Ai = egt.player2isAi ? strings.getString("typeComputer") : strings.getString("typePlayer");

        Label p1NameLabel = new Label(egt.getP1Name() + "\n" + p1Ai);
        p1NameLabel.setWrapText(true);
        page.add(p1NameLabel, 1, rowIndex, 2, 1);

        page.add(new Label(String.valueOf(p1p2Wins[0])), 3, rowIndex);
        page.add(new Label(String.format("(%d)", egt.totalFrames)), 4, rowIndex);
        page.add(new Label(String.valueOf(p1p2Wins[1])), 5, rowIndex);

        Label p2NameLabel = new Label(egt.getP2Name() + "\n" + p2Ai);
        p2NameLabel.setWrapText(true);
        page.add(p2NameLabel, 6, rowIndex, 2, 1);

        rowIndex++;

        int[][] playersTotalBasics = matchRec.totalBasicStats();

        addSucComparison(page,
                rowIndex++,
                strings.getString("statsAttacks1"),
                playersTotalBasics,
                0,
                1);

        addSucComparison(page,
                rowIndex++,
                strings.getString("statsLongAttacks1"),
                playersTotalBasics,
                2,
                3);

        addSucComparison(page,
                rowIndex++,
                strings.getString("statsRestAttacks1"),
                playersTotalBasics,
                8,
                9);

        addSucComparison(page,
                rowIndex++,
                strings.getString("statsPositions1"),
                playersTotalBasics,
                6,
                7);

        addSucComparison(page,
                rowIndex++,
                strings.getString("statsDefenses1"),
                playersTotalBasics,
                4,
                5);

        addSucComparison(page,
                rowIndex++,
                strings.getString("statsEscapes1"),
                playersTotalBasics,
                10,
                11);

        page.add(new Separator(), 0, rowIndex++, 8, 1);

        boolean poolLike = egt.gameRule == GameRule.CHINESE_EIGHT ||
                egt.gameRule == GameRule.LIS_EIGHT ||
                egt.gameRule == GameRule.AMERICAN_NINE;
        if (egt.gameRule.snookerLike()) {
            int[][] totalSnookerScores = ((EntireGameRecord.Snooker) matchRec).totalScores();
            
            addScoreComparison(page,
                    rowIndex++,
                    strings.getString("totalPoints"),
                    totalSnookerScores,
                    0);

            addScoreComparison(page,
                    rowIndex++,
                    strings.getString("highestBreak"),
                    totalSnookerScores,
                    1);

            addScoreComparison(page,
                    rowIndex++,
                    strings.getString("single50"), 
                    totalSnookerScores,
                    2);

            addScoreComparison(page,
                    rowIndex++,
                    strings.getString("single100"),
                    totalSnookerScores,
                    3);

            addScoreComparison(page,
                    rowIndex++,
                    strings.getString("single147"),
                    totalSnookerScores,
                    4);
        } else if (poolLike) {
            int[][] numberedBreaks = ((EntireGameRecord.NumberedBall) matchRec).totalScores();
            
            addSucComparison(page,
                    rowIndex++,
                    strings.getString("numBreaks"),
                    numberedBreaks,
                    0,
                    1);

            addScoreComparison(page,
                    rowIndex++,
                    strings.getString("breakClears"),
                    numberedBreaks,
                    2);

            addScoreComparison(page,
                    rowIndex++,
                    strings.getString("continueClears"),
                    numberedBreaks,
                    3);

            addScoreComparison(page,
                    rowIndex++,
                    strings.getString("highestSingleBalls"),
                    numberedBreaks,
                    4);

            if (egt.gameRule == GameRule.AMERICAN_NINE) {
                addScoreComparison(page,
                        rowIndex++,
                        strings.getString("goldNines"),
                        numberedBreaks,
                        5);
            }
        }

        page.add(new Separator(), 0, rowIndex, 8, 1);
        rowIndex++;

        // 分局显示
        for (Map.Entry<Integer, PlayerFrameRecord[]> entry :
                matchRec.getFrameRecords().entrySet()) {
            PlayerFrameRecord p1r = entry.getValue()[0];
            PlayerFrameRecord p2r = entry.getValue()[1];
            page.add(new Label(
                            Util.secondsToString(matchRec.getFrameDurations().get(entry.getKey()))),
                    4, rowIndex);

            Label p1ScoreLabel = new Label();
            Label p2ScoreLabel = new Label();
            if (egt.gameRule.snookerLike()) {
                PlayerFrameRecord.Snooker p1sr = (PlayerFrameRecord.Snooker) p1r;
                PlayerFrameRecord.Snooker p2sr = (PlayerFrameRecord.Snooker) p2r;
                p1ScoreLabel.setText(String.valueOf(p1sr.snookerScores[0]));
                p2ScoreLabel.setText(String.valueOf(p2sr.snookerScores[0]));
                if (p1sr.snookerScores[1] >= 50) {
                    Label p1SinglePole = new Label();
                    p1SinglePole.setText(String.format("(%d)", p1sr.snookerScores[1]));
                    page.add(p1SinglePole, 1, rowIndex);
                }
                if (p2sr.snookerScores[1] >= 50) {
                    Label p2SinglePole = new Label();
                    p2SinglePole.setText(String.format("(%d)", p2sr.snookerScores[1]));
                    page.add(p2SinglePole, 7, rowIndex);
                }
            } else if (poolLike) {
                // 炸清，接清
                PlayerFrameRecord.Numbered p1nr = (PlayerFrameRecord.Numbered) p1r;
                PlayerFrameRecord.Numbered p2nr = (PlayerFrameRecord.Numbered) p2r;

                String breakClear = egt.gameRule == GameRule.CHINESE_EIGHT || egt.gameRule == GameRule.LIS_EIGHT ?
                        strings.getString("breakClears") :
                        strings.getString("bigGolds");
                String continueClear = egt.gameRule == GameRule.CHINESE_EIGHT || egt.gameRule == GameRule.LIS_EIGHT ?
                        strings.getString("continueClears") :
                        strings.getString("smallGolds");
                String goldNone = strings.getString("goldNines");

                if (p1nr.clears[2] > 0) {
                    Label p1Extra = new Label(breakClear);
                    page.add(p1Extra, 2, rowIndex);
                }
                if (p1nr.clears[3] > 0) {
                    Label p1Extra = new Label(continueClear);
                    page.add(p1Extra, 2, rowIndex);
                }
                if (p2nr.clears[2] > 0) {
                    Label p2Extra = new Label(breakClear);
                    page.add(p2Extra, 6, rowIndex);
                }
                if (p2nr.clears[3] > 0) {
                    Label p2Extra = new Label(continueClear);
                    page.add(p2Extra, 6, rowIndex);
                }
                if (p1nr.clears[5] > 0) {
                    Label p1Extra = new Label(goldNone);
                    page.add(p1Extra, 2, rowIndex);
                }
                if (p2nr.clears[5] > 0) {
                    Label p2Extra = new Label(goldNone);
                    page.add(p2Extra, 6, rowIndex);
                }
            }

            if (p1r.winnerName.equals(egt.player1Id)) {
                p2ScoreLabel.setDisable(true);
                page.add(new Label("⚫"), 3, rowIndex);
            } else {
                p1ScoreLabel.setDisable(true);
                page.add(new Label("⚫"), 5, rowIndex);
            }
            page.add(p1ScoreLabel, 2, rowIndex);
            page.add(p2ScoreLabel, 6, rowIndex);

            rowIndex++;
        }

        rightPane.getChildren().add(page);
    }
    
    private void addScoreComparison(GridPane page,
                                    int rowIndex,
                                    String string,
                                    int[][] playersStats,
                                    int dataIndex) {
        int v1 = playersStats[0][dataIndex];
        int v2 = playersStats[1][dataIndex];
        page.add(new Label(string), 0, rowIndex);
        page.add(new Label(String.valueOf(v1)), 2, rowIndex);
        page.add(new Label(String.valueOf(v2)), 6, rowIndex);
        
        page.add(new AdversarialBar((double) v1 / (v1 + v2)), 
                4 , rowIndex);
    }

    private void addSucComparison(GridPane page,
                                  int rowIndex,
                                  String string,
                                  int[][] playersStats,
                                  int dataIndex,
                                  int dataIndexSuc) {
        double rate1 = (double) playersStats[0][dataIndexSuc] / playersStats[0][dataIndex];
        double rate2 = (double) playersStats[1][dataIndexSuc] / playersStats[1][dataIndex];

        page.add(new Label(string), 0, rowIndex);
        page.add(new Label(String.valueOf(playersStats[0][dataIndex])), 1, rowIndex);
        page.add(new Label(String.valueOf(playersStats[1][dataIndex])), 7, rowIndex);

        page.add(new Label(String.valueOf(playersStats[0][dataIndexSuc])), 2, rowIndex);
        page.add(new Label(String.valueOf(playersStats[1][dataIndexSuc])), 6, rowIndex);

        page.add(new AdversarialBar(
                        playersStats[0][dataIndex],
                        rate1,
                        playersStats[1][dataIndex],
                        rate2),
                4, rowIndex);

        page.add(new Label(
                        showPercent(rate1)),
                3, rowIndex);
        page.add(new Label(
                        showPercent(rate2)),
                5, rowIndex);
    }
}
