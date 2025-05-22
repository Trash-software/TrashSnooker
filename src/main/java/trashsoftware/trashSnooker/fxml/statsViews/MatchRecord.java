package trashsoftware.trashSnooker.fxml.statsViews;

import javafx.geometry.Pos;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import trashsoftware.trashSnooker.core.SubRule;
import trashsoftware.trashSnooker.core.attempt.CueType;
import trashsoftware.trashSnooker.core.career.championship.MatchTreeNode;
import trashsoftware.trashSnooker.core.career.championship.MetaMatchInfo;
import trashsoftware.trashSnooker.core.infoRec.*;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;
import trashsoftware.trashSnooker.core.snooker.SnookerBall;
import trashsoftware.trashSnooker.core.table.NumberedBallTable;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.fxml.widgets.AdversarialBar;
import trashsoftware.trashSnooker.fxml.widgets.MatchRecordPage;
import trashsoftware.trashSnooker.util.Util;
import trashsoftware.trashSnooker.util.db.DBAccess;
import trashsoftware.trashSnooker.util.db.EntireGameRecord;
import trashsoftware.trashSnooker.util.db.EntireGameTitle;
import trashsoftware.trashSnooker.util.db.PlayerFrameRecord;

import java.util.*;
import java.util.stream.Collectors;

public class MatchRecord extends RecordTree {
    final EntireGameTitle egt;

    private Set<Integer> expandedFrames = new TreeSet<>();

    MatchRecord(EntireGameTitle egt, ResourceBundle strings) {
        super(egt.toString(), strings);
        this.egt = egt;
    }

    @Override
    void setRightPane(Pane rightPane) {
        fillRightPane(rightPane);
    }

    private void fillRightPane(Pane rightPane) {
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
        var subRules = egt.getSubRules();
        if (!subRules.isEmpty()) {
            String subRuleStr = subRules.stream().map(SubRule::toString).collect(Collectors.joining(" "));
            page.add(new Label(subRuleStr), 4, rowIndex++);
        }

        String[] winLost;
        int[] p1p2Wins = matchRec.getP1P2WinsCount();
        if (matchRec.isFinished()) {
            if (p1p2Wins[0] > p1p2Wins[1]) {
                winLost = new String[]{"WIN", "", "LOST"};
            } else {
                winLost = new String[]{"LOST", "", "WIN"};
            }
        } else {
            winLost = new String[]{"", "IN PROGRESS", ""};
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

        // nullable
        MatchInfoRec matchInfoRec = MatchInfoRec.tryToLoad(Util.entireBeginTimeToFileName(egt.startTime));

        // 分局显示
        for (Map.Entry<Integer, PlayerFrameRecord[]> entry :
                matchRec.getFrameRecords().entrySet()) {

            if (matchInfoRec != null) {
                boolean currentExpanded = expandedFrames.contains(entry.getKey());
                Hyperlink frameLink = new Hyperlink(strings.getString(currentExpanded ?
                        "collapseDetail" : "expandDetail"));
                HBox frameIndexBox = new HBox();
                frameIndexBox.setAlignment(Pos.CENTER_LEFT);
                frameIndexBox.setSpacing(5.0);
                frameIndexBox.getChildren().add(new Label(
                        String.format(strings.getString("nthFrameFmt"), entry.getKey())));
                frameIndexBox.getChildren().add(frameLink);
                frameLink.setOnAction(e -> {
                    int index = entry.getKey();
                    if (expandedFrames.contains(index)) {
                        expandedFrames.remove(index);
                    } else {
                        expandedFrames.add(index);
                    }
                    fillRightPane(rightPane);
                });
                page.add(frameIndexBox, 0, rowIndex);
            }

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

            if (matchInfoRec != null && expandedFrames.contains(entry.getKey())) {
                fillFrameRecord(page, rowIndex, matchInfoRec, entry.getKey());
                rowIndex++;
            }
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
                4, rowIndex);
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

    public void fillFrameRecord(MatchRecordPage page,
                                int rowIndex,
                                MatchInfoRec mir,
                                int frameIndexFrom1) {
        FrameInfoRec fir = mir.getFrame(frameIndexFrom1 - 1);
        Canvas p1Canvas = null, p2Canvas = null;
        FrameAnalyze frameAnalyze = fir.getFrameAnalyze();

        if (egt.gameRule.snookerLike()) {
            p1Canvas = playerSnookerCueRecords(fir, 1);
            p2Canvas = playerSnookerCueRecords(fir, 2);
        } else if (egt.gameRule.eightBallLike()) {
            p1Canvas = playerChineseEightCueRecords(fir, 1);
            p2Canvas = playerChineseEightCueRecords(fir, 2);
        }

        if (p1Canvas != null && p2Canvas != null) {
            page.add(p1Canvas, 1, rowIndex, 3, 1);
            page.add(p2Canvas, 5, rowIndex, 3, 1);
        }

        if (frameAnalyze != null) {
            List<FrameAnalyze.FrameKind> frameKinds = frameAnalyze.getFrameKinds();
            Label kindLabel = new Label();
            if (frameKinds.isEmpty()) {
                kindLabel.setText(FrameAnalyze.FrameKind.NORMAL.shown(strings));
            } else {
                String showContent = frameKinds
                        .stream()
                        .map(fk -> fk.shown(strings))
                        .collect(Collectors.joining("\n"));
                kindLabel.setText(showContent);
            }
            page.add(kindLabel, 4, rowIndex);
        }
    }

    private Canvas playerChineseEightCueRecords(FrameInfoRec fir, int playerNumberFrom1) {
        Canvas canvas = new Canvas();

        double cellSize = 16;
        double ballDiameter = 12;
//        double margin = (cellSize - ballDiameter) / 2;
        int eachRowBalls = 10;

        int nCues = fir.getCueRecs().size();
        double height = Math.ceil((double) nCues / eachRowBalls) * cellSize;
        canvas.setWidth(cellSize * eachRowBalls);
        canvas.setHeight(height);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setTextAlign(TextAlignment.CENTER);
        double fontSize = 8;
        double textDown = fontSize * 0.36;
        gc.setFont(new Font(App.FONT.getName(), fontSize));

        for (int i = 0; i < nCues; i++) {
            int r = i / eachRowBalls;
            int c = i % eachRowBalls;
            double x = (c + 0.5) * cellSize;
            double y = (r + 0.5) * cellSize;
            CueInfoRec cir = fir.getCueRecs().get(i);
            boolean drawn = false;
            if (cir.getPlayer() == playerNumberFrom1) {
                if (cir.getAttemptBase().type == CueType.BREAK && cir.getAttemptBase().isSuccess()) {
                    gc.setFill(Color.DARKGREEN);
                    gc.fillText("P",
                            x,
                            y + textDown);
                }
                if (cir.legallyPot()) {
                    if (cir.getPots().isEmpty()) {
                        System.err.println("Legal pot of ceb should have at least one pot ball");
                        continue;
                    }
                    for (var entry : cir.getPots().entrySet()) {
                        // 目前只画第一个
                        int num = entry.getKey();
                        NumberedBallTable.drawPoolBallEssential(
                                x, y, ballDiameter, PoolBall.poolBallBaseColor(num), num,
                                gc);
                        drawn = true;
                        break;
                    }
                }
                if (cir.isFoul()) {
                    gc.setFill(Color.BLACK);
                    gc.fillText("X",
                            x,
                            y + textDown);
                }
            }
            if (!drawn) {
                gc.setStroke(cir.getPlayer() == playerNumberFrom1 ? Color.BLACK : Color.DARKGRAY);
                gc.strokeOval(x - ballDiameter / 2,
                        y - ballDiameter / 2,
                        ballDiameter,
                        ballDiameter);
            }
        }

        return canvas;
    }

    private Canvas playerSnookerCueRecords(FrameInfoRec fir, int playerNumberFrom1) {
//        int playerIndex = playerNumberFrom1 - 1;
        Canvas canvas = new Canvas();

        FrameAnalyze fa = fir.getFrameAnalyze();
        SnookerFrameAnalyze sfa = fa instanceof SnookerFrameAnalyze ? (SnookerFrameAnalyze) fa : null;

        double cellWidth = 16;
        double cellHeight = sfa == null ? 16 : 24;
        double ballDiameter = 12;
        double margin = (cellWidth - ballDiameter) / 2;
        int eachRowBalls = 10;
        double width = cellWidth * eachRowBalls;

        int nCues = fir.getCueRecs().size();
        double height = Math.ceil((double) nCues / eachRowBalls) * cellHeight;
        canvas.setWidth(width);
        canvas.setHeight(height);

        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setLineWidth(1.0);
        gc.setTextAlign(TextAlignment.CENTER);
        double fontSize = 8;
        double textDown = fontSize * 0.36;
        gc.setFont(new Font(App.FONT.getName(), fontSize));

        for (int i = 0; i < nCues; i++) {
            int r = i / eachRowBalls;
            int c = i % eachRowBalls;
            double x = c * cellWidth + margin;
            double y = (r + 1) * cellHeight - ballDiameter - margin;
            double textX = x + ballDiameter * 0.5;
            double textY = y + ballDiameter * 0.5 + textDown;
            CueInfoRec cir = fir.getCueRecs().get(i);
            boolean drawn = false;
            if (cir.getPlayer() == playerNumberFrom1) {
                if (cir.legallyPot()) {
                    if (cir.getPots().size() != 1) {
                        System.err.println("? legal shot should contain multiple kinds of potted balls in snooker.");
                        continue;
                    }

                    for (var entry : cir.getPots().entrySet()) {
                        // only iterates once
                        Color color = SnookerBall.snookerColor(entry.getKey());
                        gc.setFill(color);
                        gc.fillOval(x, y, ballDiameter, ballDiameter);
                        if (cir.isSnookerFreeBall()) {
                            // 覆盖：用原本的目标
                            gc.setFill(SnookerBall.snookerColor(cir.getTarget()));
                            gc.fillOval(x, y, ballDiameter, ballDiameter);
                            gc.setFill(Color.WHITE);
                            gc.fillText("F",
                                    textX,
                                    textY);
                        } else if (entry.getValue() > 1) {
                            gc.setFill(Color.WHITE);
                            gc.fillText(String.valueOf(entry.getValue()),
                                    textX,
                                    textY);
                        }
                        drawn = true;
                        break;
                    }
                }
                if (cir.isFoul()) {
                    gc.setFill(Color.BLACK);
                    gc.fillText("X",
                            textX,
                            textY);
                }
            } else {
                // 对手打的
                if (cir.getGainScores()[playerNumberFrom1 - 1] != 0) {
                    gc.setFill(Color.BLACK);
                    gc.fillText("+" + cir.getGainScores()[playerNumberFrom1 - 1],
                            textX,
                            textY);
                }
            }

            if (!drawn) {
                gc.setStroke(cir.getPlayer() == playerNumberFrom1 ? Color.BLACK : Color.DARKGRAY);
                gc.strokeOval(x,
                        y,
                        ballDiameter,
                        ballDiameter);
            }
        }

        if (sfa != null) {
            // 绘制单杆
            List<SnookerFrameAnalyze.SnookerBreak> playerBreaks = sfa.getBreaks(playerNumberFrom1);
            gc.setStroke(Color.DARKGRAY);
            gc.setFill(Color.BLACK);
            for (SnookerFrameAnalyze.SnookerBreak b : playerBreaks) {
                if (!b.validContinuous()) continue;
                int begin = b.getFromIndex();
                int end = begin + b.getBreakCues() - 1;  // inclusive
//                System.out.println("Break:" + b + ", " + end);
                int bRow = begin / eachRowBalls;
                int eRow = end / eachRowBalls;
                int bCol = begin % eachRowBalls;
                int eCol = end % eachRowBalls;
                double bx = bCol * cellWidth + 1;
                double ex = eCol * cellWidth - 1;
                double beginY = bRow * cellHeight + 4;
                double endY = beginY;
                gc.strokeLine(bx, beginY, bx, beginY + 3);
                if (bRow == eRow) {
                    // 仅一行
                    gc.strokeLine(bx, beginY, ex, beginY);
                } else {
                    for (int r = bRow; r <= eRow; r++) {
                        double y = r * cellHeight + 4;
                        double left, right;
                        if (r == bRow) {
                            left = bx;
                            right = width;
                        } else if (r == eRow) {
                            left = 0;
                            right = ex;
                        } else {
                            left = 0;
                            right = width;
                        }
                        endY = y;
                        gc.strokeLine(left, y, right, y);
                    }
                }
                int textR = end / eachRowBalls;
                int textC = end % eachRowBalls;
                double x = textC * cellWidth;
                double y = textR * cellHeight;
                gc.strokeLine(x + cellWidth - 3, endY, x + cellWidth - 1, endY);
                gc.strokeLine(x + cellWidth - 1, endY, x + cellWidth - 1, endY + 3);
                gc.fillText(String.valueOf(b.getBreakScore()),
                        x + cellWidth * 0.4,
                        y + fontSize * 0.75);
            }
        }
        return canvas;
    }
}
