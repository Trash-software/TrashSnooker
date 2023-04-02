package trashsoftware.trashSnooker.fxml.statsViews;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.text.TextAlignment;
import trashsoftware.trashSnooker.core.metrics.GameRule;
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
        MatchRecordPage page = new MatchRecordPage();

        int rowIndex = 0;
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
        page.add(new Label(egt.getP1Name() + "\n" + p1Ai), 1, rowIndex, 2, 1);
        page.add(new Label(String.valueOf(p1p2Wins[0])), 3, rowIndex);
        page.add(new Label(String.format("(%d)", egt.totalFrames)), 4, rowIndex);
        page.add(new Label(String.valueOf(p1p2Wins[1])), 5, rowIndex);

        HBox p2Box = new HBox();
        p2Box.setAlignment(Pos.TOP_RIGHT);
        Label p2NameLabel = new Label(egt.getP2Name() + "\n" + p2Ai);
        p2NameLabel.setTextAlignment(TextAlignment.RIGHT);
        p2NameLabel.setWrapText(true);
        p2Box.getChildren().add(p2NameLabel);

        page.add(p2Box, 6, rowIndex, 2, 1);
        rowIndex++;

        int[][] playersTotalBasics = matchRec.totalBasicStats();
//            System.out.println(Arrays.deepToString(playersTotalBasics));
        page.add(new Label(strings.getString("statsAttacks1")), 0, rowIndex);
        page.add(new Label(String.valueOf(playersTotalBasics[0][0])), 1, rowIndex);
        page.add(new Label(String.valueOf(playersTotalBasics[1][0])), 7, rowIndex);
//        rowIndex++;

//        page.add(new Label("进攻成功次数"), 0, rowIndex);
        page.add(new Label(String.valueOf(playersTotalBasics[0][1])), 2, rowIndex);
        page.add(new Label(String.valueOf(playersTotalBasics[1][1])), 6, rowIndex);

        page.add(new Label(
                        showPercent(playersTotalBasics[0][1], playersTotalBasics[0][0])),
                3, rowIndex);
        page.add(new Label(
                        showPercent(playersTotalBasics[1][1], playersTotalBasics[1][0])),
                5, rowIndex);
        rowIndex++;

        page.add(new Label(strings.getString("statsLongAttacks1")), 0, rowIndex);
        page.add(new Label(String.valueOf(playersTotalBasics[0][2])), 1, rowIndex);
        page.add(new Label(String.valueOf(playersTotalBasics[1][2])), 7, rowIndex);
//        rowIndex++;

//        page.add(new Label("长台进攻成功次数"), 0, rowIndex);
        page.add(new Label(String.valueOf(playersTotalBasics[0][3])), 2, rowIndex);
        page.add(new Label(String.valueOf(playersTotalBasics[1][3])), 6, rowIndex);

        page.add(new Label(
                        showPercent(playersTotalBasics[0][3], playersTotalBasics[0][2])),
                3, rowIndex);
        page.add(new Label(
                        showPercent(playersTotalBasics[1][3], playersTotalBasics[1][2])),
                5, rowIndex);
        rowIndex++;

        page.add(new Label(strings.getString("statsRestAttacks1")), 0, rowIndex);
        page.add(new Label(String.valueOf(playersTotalBasics[0][8])), 1, rowIndex);
        page.add(new Label(String.valueOf(playersTotalBasics[1][8])), 7, rowIndex);
//        rowIndex++;

//        page.add(new Label("架杆进攻成功次数"), 0, rowIndex);
        page.add(new Label(String.valueOf(playersTotalBasics[0][9])), 2, rowIndex);
        page.add(new Label(String.valueOf(playersTotalBasics[1][9])), 6, rowIndex);

        page.add(new Label(
                        showPercent(playersTotalBasics[0][9], playersTotalBasics[0][8])),
                3, rowIndex);
        page.add(new Label(
                        showPercent(playersTotalBasics[1][9], playersTotalBasics[1][8])),
                5, rowIndex);
        rowIndex++;

        page.add(new Label(strings.getString("statsPositions1")), 0, rowIndex);
        page.add(new Label(String.valueOf(playersTotalBasics[0][6])), 1, rowIndex);
        page.add(new Label(String.valueOf(playersTotalBasics[1][6])), 7, rowIndex);
//        rowIndex++;

//        page.add(new Label("走位成功次数"), 0, rowIndex);
        page.add(new Label(String.valueOf(playersTotalBasics[0][7])), 2, rowIndex);
        page.add(new Label(String.valueOf(playersTotalBasics[1][7])), 6, rowIndex);

        page.add(new Label(
                        showPercent(playersTotalBasics[0][7], playersTotalBasics[0][6])),
                3, rowIndex);
        page.add(new Label(
                        showPercent(playersTotalBasics[1][7], playersTotalBasics[1][6])),
                5, rowIndex);
        rowIndex++;

        page.add(new Label(strings.getString("statsDefenses1")), 0, rowIndex);
        page.add(new Label(String.valueOf(playersTotalBasics[0][4])), 1, rowIndex);
        page.add(new Label(String.valueOf(playersTotalBasics[1][4])), 7, rowIndex);
//        rowIndex++;

//        page.add(new Label("防守成功次数"), 0, rowIndex);
        page.add(new Label(String.valueOf(playersTotalBasics[0][5])), 2, rowIndex);
        page.add(new Label(String.valueOf(playersTotalBasics[1][5])), 6, rowIndex);

        page.add(new Label(
                        showPercent(playersTotalBasics[0][5], playersTotalBasics[0][4])),
                3, rowIndex);
        page.add(new Label(
                        showPercent(playersTotalBasics[1][5], playersTotalBasics[1][4])),
                5, rowIndex);
        rowIndex++;

        page.add(new Label(strings.getString("statsEscapes1")), 0, rowIndex);
        page.add(new Label(String.valueOf(playersTotalBasics[0][10])), 1, rowIndex);
        page.add(new Label(String.valueOf(playersTotalBasics[1][10])), 7, rowIndex);
//        rowIndex++;

//        page.add(new Label("解球成功次数"), 0, rowIndex);
        page.add(new Label(String.valueOf(playersTotalBasics[0][11])), 2, rowIndex);
        page.add(new Label(String.valueOf(playersTotalBasics[1][11])), 6, rowIndex);

        page.add(new Label(
                        showPercent(playersTotalBasics[0][11], playersTotalBasics[0][10])),
                3, rowIndex);
        page.add(new Label(
                        showPercent(playersTotalBasics[1][11], playersTotalBasics[1][10])),
                5, rowIndex);
        rowIndex++;

        page.add(new Separator(), 0, rowIndex++, 8, 1);

        if (egt.gameRule.snookerLike()) {
            int[][] totalSnookerScores = ((EntireGameRecord.Snooker) matchRec).totalScores();
            page.add(new Label(strings.getString("totalPoints")), 0, rowIndex);
            page.add(new Label(String.valueOf(totalSnookerScores[0][0])), 2, rowIndex);
            page.add(new Label(String.valueOf(totalSnookerScores[1][0])), 6, rowIndex);
            rowIndex++;

            page.add(new Label(strings.getString("highestBreak")), 0, rowIndex);
            page.add(new Label(String.valueOf(totalSnookerScores[0][1])), 2, rowIndex);
            page.add(new Label(String.valueOf(totalSnookerScores[1][1])), 6, rowIndex);
            rowIndex++;

            page.add(new Label("50+"), 0, rowIndex);
            page.add(new Label(String.valueOf(totalSnookerScores[0][2])), 2, rowIndex);
            page.add(new Label(String.valueOf(totalSnookerScores[1][2])), 6, rowIndex);
            rowIndex++;

            page.add(new Label("100+"), 0, rowIndex);
            page.add(new Label(String.valueOf(totalSnookerScores[0][3])), 2, rowIndex);
            page.add(new Label(String.valueOf(totalSnookerScores[1][3])), 6, rowIndex);
            rowIndex++;

            page.add(new Label("147"), 0, rowIndex);
            page.add(new Label(String.valueOf(totalSnookerScores[0][4])), 2, rowIndex);
            page.add(new Label(String.valueOf(totalSnookerScores[1][4])), 6, rowIndex);
            rowIndex++;
        } else if (egt.gameRule == GameRule.CHINESE_EIGHT ||
                egt.gameRule == GameRule.SIDE_POCKET) {
            int[][] numberedBreaks = ((EntireGameRecord.NumberedBall) matchRec).totalScores();
            page.add(new Label("开球次数"), 0, rowIndex);
            page.add(new Label(String.valueOf(numberedBreaks[0][0])), 2, rowIndex);
            page.add(new Label(String.valueOf(numberedBreaks[1][0])), 6, rowIndex);
            rowIndex++;

            page.add(new Label("开球进球次数"), 0, rowIndex);
            page.add(new Label(String.valueOf(numberedBreaks[0][1])), 2, rowIndex);
            page.add(new Label(String.valueOf(numberedBreaks[1][1])), 6, rowIndex);

            page.add(new Label(String.format("%.1f%%",
                            numberedBreaks[0][1] * 100.0 / numberedBreaks[0][0])),
                    3, rowIndex);
            page.add(new Label(String.format("%.1f%%",
                            numberedBreaks[1][1] * 100.0 / numberedBreaks[1][0])),
                    5, rowIndex);
            rowIndex++;

            page.add(new Label("炸清"), 0, rowIndex);
            page.add(new Label(String.valueOf(numberedBreaks[0][2])), 2, rowIndex);
            page.add(new Label(String.valueOf(numberedBreaks[1][2])), 6, rowIndex);
            rowIndex++;

            page.add(new Label("接清"), 0, rowIndex);
            page.add(new Label(String.valueOf(numberedBreaks[0][3])), 2, rowIndex);
            page.add(new Label(String.valueOf(numberedBreaks[1][3])), 6, rowIndex);
            rowIndex++;

            page.add(new Label("单杆最高球数"), 0, rowIndex);
            page.add(new Label(String.valueOf(numberedBreaks[0][4])), 2, rowIndex);
            page.add(new Label(String.valueOf(numberedBreaks[1][4])), 6, rowIndex);
            rowIndex++;
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
            } else if (egt.gameRule == GameRule.CHINESE_EIGHT ||
                    egt.gameRule == GameRule.LIS_EIGHT ||
                    egt.gameRule == GameRule.SIDE_POCKET) {
                // 炸清，接清
                PlayerFrameRecord.Numbered p1nr = (PlayerFrameRecord.Numbered) p1r;
                PlayerFrameRecord.Numbered p2nr = (PlayerFrameRecord.Numbered) p2r;

                String breakClear = egt.gameRule == GameRule.CHINESE_EIGHT || egt.gameRule == GameRule.LIS_EIGHT ?
                        strings.getString("breakClears") :
                        strings.getString("bigGolds");
                String continueClear = egt.gameRule == GameRule.CHINESE_EIGHT || egt.gameRule == GameRule.LIS_EIGHT ?
                        strings.getString("continueClears") :
                        strings.getString("smallGolds");

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
}
