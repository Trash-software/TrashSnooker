package trashsoftware.trashSnooker.fxml.statsViews;

import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.util.db.DBAccess;
import trashsoftware.trashSnooker.util.db.EntireGameRecord;
import trashsoftware.trashSnooker.util.db.EntireGameTitle;

import java.util.*;

public class GameTypeTree extends RecordTree {
    private final StatsView.PlayerAi pai;
    private final GameRule gameRule;

    private int breaksFiftyRow;
    private int breaksCenturyRow;
    private int breaks147Row;
    private int[] breaksScores;

    GameTypeTree(StatsView.PlayerAi pai, GameRule gameRule, ResourceBundle strings) {
        super(GameRule.toReadable(gameRule), strings);
        this.pai = pai;
        this.gameRule = gameRule;
    }

    @Override
    void setRightPane(Pane rightPane) {
        rightPane.getChildren().clear();

        final GridPane resultPane = new GridPane();
        resultPane.setVgap(10.0);
        resultPane.setHgap(20.0);
        resultPane.setAlignment(Pos.CENTER);

        DBAccess db = DBAccess.getInstance();
        int[] potRecords = db.getBasicPotStatusAll(gameRule, pai.playerId, pai.isAi);

        int rowIndex = 0;

        resultPane.add(new Label(strings.getString("statsAttempts")), 1, rowIndex);
        resultPane.add(new Label(strings.getString("statsSuccess")), 2, rowIndex);
        resultPane.add(new Label(strings.getString("statsSuccessRate")), 3, rowIndex++);

        int potAttempts = potRecords[0];
        int potSuccesses = potRecords[1];
        resultPane.add(new Label(strings.getString("statsAttacks1")), 0, rowIndex);
        resultPane.add(new Label(String.valueOf(potAttempts)), 1, rowIndex);
//        resultPane.add(new Label(strings.getString("statsAttacksSuc")), 0, rowIndex);
        resultPane.add(new Label(String.valueOf(potSuccesses)), 2, rowIndex);
//            resultPane.add(new Label("进攻成功率"), 0, rowIndex);
        resultPane.add(new Label(
                potAttempts == 0 ? "0%" :
                        String.format("%.1f%%", potSuccesses * 100.0 / potAttempts)), 3, rowIndex++);

        int longPotAttempts = potRecords[2];
        int longPotSuccesses = potRecords[3];
        resultPane.add(new Label(strings.getString("statsLongAttacks1")), 0, rowIndex);
        resultPane.add(new Label(String.valueOf(longPotAttempts)), 1, rowIndex);
//        resultPane.add(new Label(strings.getString("statsLongAttacksSuc")), 0, rowIndex);
        resultPane.add(new Label(String.valueOf(longPotSuccesses)), 2, rowIndex);
//            resultPane.add(new Label("长台进攻成功率"), 0, rowIndex);
        resultPane.add(new Label(
                longPotAttempts == 0 ? "0%" :
                        String.format("%.1f%%",
                                longPotSuccesses * 100.0 / longPotAttempts)), 3, rowIndex++);

        int restAttempts = potRecords[8];
        int restSuccesses = potRecords[9];
        resultPane.add(new Label(strings.getString("statsRestAttacks1")), 0, rowIndex);
        resultPane.add(new Label(String.valueOf(restAttempts)), 1, rowIndex);
//        resultPane.add(new Label(strings.getString("statsRestAttacksSuc")), 0, rowIndex);
        resultPane.add(new Label(String.valueOf(restSuccesses)), 2, rowIndex);
//            resultPane.add(new Label("进攻成功率"), 0, rowIndex);
        resultPane.add(new Label(
                restAttempts == 0 ? "0%" :
                        String.format("%.1f%%", restSuccesses * 100.0 / restAttempts)), 3, rowIndex++);

        int positionAttempts = potRecords[6];
        int positionSuccesses = potRecords[7];
        resultPane.add(new Label(strings.getString("statsPositions1")), 0, rowIndex);
        resultPane.add(new Label(String.valueOf(positionAttempts)), 1, rowIndex);
//        resultPane.add(new Label(strings.getString("statsPositionsSuc")), 0, rowIndex);
        resultPane.add(new Label(String.valueOf(positionSuccesses)), 2, rowIndex);
//            resultPane.add(new Label("进攻成功率"), 0, rowIndex);
        resultPane.add(new Label(
                positionAttempts == 0 ? "0%" :
                        String.format("%.1f%%", positionSuccesses * 100.0 / positionAttempts)), 3, rowIndex++);

        int defAttempts = potRecords[4];
        int defSuccesses = potRecords[5];
        resultPane.add(new Label(strings.getString("statsDefenses1")), 0, rowIndex);
        resultPane.add(new Label(String.valueOf(defAttempts)), 1, rowIndex);
//        resultPane.add(new Label(strings.getString("statsDefensesSuc")), 0, rowIndex);
        resultPane.add(new Label(String.valueOf(defSuccesses)), 2, rowIndex);
//            resultPane.add(new Label("防守成功率"), 0, rowIndex);
        resultPane.add(new Label(
                defAttempts == 0 ? "0%" :
                        String.format("%.1f%%",
                                defSuccesses * 100.0 / defAttempts)), 3, rowIndex++);

        int solves = potRecords[10];
        int solveSuccesses = potRecords[11];
        resultPane.add(new Label(strings.getString("statsEscapes1")), 0, rowIndex);
        resultPane.add(new Label(String.valueOf(solves)), 1, rowIndex);
//        resultPane.add(new Label(strings.getString("statsEscapesSuc")), 0, rowIndex);
        resultPane.add(new Label(String.valueOf(solveSuccesses)), 2, rowIndex);
//            resultPane.add(new Label("防守成功率"), 0, rowIndex);
        resultPane.add(new Label(
                solves == 0 ? "0%" :
                        String.format("%.1f%%",
                                solveSuccesses * 100.0 / solves)), 3, rowIndex++);

        resultPane.add(new Separator(), 0, rowIndex++, 4, 1);

        if (gameRule.snookerLike()) {
            breaksScores = db.getSnookerBreaksTotal(gameRule, pai.playerId, pai.isAi);
            resultPane.add(new Label(strings.getString("totalPoints")), 0, rowIndex);
            resultPane.add(new Label(String.valueOf(breaksScores[0])), 1, rowIndex++);
            resultPane.add(new Label(strings.getString("highestBreak")), 0, rowIndex);
            resultPane.add(new Label(String.valueOf(breaksScores[1])), 1, rowIndex++);

            resultPane.add(new Label(strings.getString("single50")), 0, rowIndex);
            breaksFiftyRow = rowIndex;
            resultPane.add(new Label(String.valueOf(breaksScores[2])), 1, rowIndex++);

            resultPane.add(new Label(strings.getString("single100")), 0, rowIndex);
            breaksCenturyRow = rowIndex;
            resultPane.add(new Label(String.valueOf(breaksScores[3])), 1, rowIndex++);

            resultPane.add(new Label(strings.getString("single147")), 0, rowIndex);
            breaks147Row = rowIndex;
            resultPane.add(new Label(String.valueOf(breaksScores[4])), 1, rowIndex++);
        } else if (gameRule == GameRule.CHINESE_EIGHT || gameRule == GameRule.SIDE_POCKET) {
            breaksScores = db.getNumberedBallGamesTotal(gameRule, pai.playerId, pai.isAi);
            resultPane.add(new Label(strings.getString("numBreaks")), 0, rowIndex);
            resultPane.add(new Label(String.valueOf(breaksScores[0])), 1, rowIndex++);
            resultPane.add(new Label(strings.getString("numBreaksSuc")), 0, rowIndex);
            resultPane.add(new Label(String.valueOf(breaksScores[1])), 1, rowIndex++);
            resultPane.add(new Label(strings.getString("breaksSucRatio")), 0, rowIndex);
            resultPane.add(new Label(String.format("%.1f%%",
                            breaksScores[1] * 100.0 / breaksScores[0])),
                    1, rowIndex++);
            resultPane.add(new Label(strings.getString("breakClears")), 0, rowIndex);
            resultPane.add(new Label(String.valueOf(breaksScores[2])), 1, rowIndex++);
            resultPane.add(new Label(strings.getString("continueClears")), 0, rowIndex);
            resultPane.add(new Label(String.valueOf(breaksScores[3])), 1, rowIndex++);
            resultPane.add(new Label(strings.getString("highestSingleBalls")), 0, rowIndex);
            resultPane.add(new Label(String.valueOf(breaksScores[4])), 1, rowIndex++);
        }
        resultPane.add(new Separator(), 0, rowIndex++, 4, 1);
        final Button gameStatsButton = new Button(strings.getString("matchStats"));
        resultPane.add(gameStatsButton, 0, rowIndex++);

        final int lastNormalRowIndex = rowIndex;
        gameStatsButton.setOnAction(e -> {
            ProgressIndicator pi = new ProgressIndicator();
            resultPane.add(pi, 0, lastNormalRowIndex);
            Thread thread = new Thread(() ->
                    fillGameStats(pi, resultPane, gameStatsButton, lastNormalRowIndex));
            thread.start();
        });

        rightPane.getChildren().add(resultPane);
    }

    private void fillGameStats(final ProgressIndicator indicator, final GridPane resultPane,
                               final Button button, final int startRowIndex) {
        long st = System.currentTimeMillis();
        List<EntireGameTitle> allMatches =
                DBAccess.getInstance().getAllPveMatches(gameRule, pai.playerId, pai.isAi);
        List<EntireGameRecord> entireRecords = new ArrayList<>();
        for (EntireGameTitle egt : allMatches) {
            entireRecords.add(DBAccess.getInstance().getMatchDetail(egt));
        }

        // int[2]{胜场，总场次} of 这个 match size
        // 注：和下面有opponent那个不一样
        SortedMap<Integer, int[]> playerWinsByTotalFrames = new TreeMap<>();
        int thisWinFrames = 0;
        int totalFrames = 0;
        int thisWinMatches = 0;
        int finalFrames = 0;  // 决胜局
        int finalFrameWins = 0;
        for (EntireGameRecord egr : entireRecords) {
            boolean thisIsP1 = egr.getTitle().player1Id.equals(pai.playerId);
            int[] playerWinsInThisMatchSize =
                    playerWinsByTotalFrames.computeIfAbsent(
                            egr.getTitle().totalFrames, k -> new int[2]);
            playerWinsInThisMatchSize[1]++;
            int[] p1p2wins = egr.getP1P2WinsCount();
            totalFrames += egr.getTitle().totalFrames;
            boolean isFinal = false;
            if (egr.getTitle().totalFrames >= 3 && p1p2wins[0] + p1p2wins[1] == egr.getTitle().totalFrames) {
                finalFrames++;
                isFinal = true;
            }

            if (thisIsP1) {
                if (p1p2wins[0] > p1p2wins[1]) {
                    thisWinMatches++;
                    playerWinsInThisMatchSize[0]++;
                    if (isFinal) finalFrameWins++;
                }
                thisWinFrames += p1p2wins[0];
            } else {
                if (p1p2wins[1] > p1p2wins[0]) {
                    thisWinMatches++;
                    playerWinsInThisMatchSize[0]++;
                    if (isFinal) finalFrameWins++;
                }
                thisWinFrames += p1p2wins[1];
            }
        }

        final int thisWinFramesFinal = thisWinFrames;
        final int totalFramesFinal = totalFrames;
        final int thisWinMatchesFinal = thisWinMatches;
        final int finalFrames1 = finalFrames;
        final int finalFrameWins1 = finalFrameWins;

        System.out.println("db time: " + (System.currentTimeMillis() - st));

        Platform.runLater(() -> {
            indicator.setManaged(false);
            indicator.setVisible(false);
            button.setManaged(false);
            button.setVisible(false);

            int rowIndex = startRowIndex;

            resultPane.add(new Label(strings.getString("statsMatchesTitle")), 1, rowIndex);
            resultPane.add(new Label(strings.getString("statsMatchesWinTitle")), 2, rowIndex);
            resultPane.add(new Label(strings.getString("statsWinRate")), 3, rowIndex++);

            resultPane.add(new Label(strings.getString("totalMatches")), 0, rowIndex);
            resultPane.add(new Label(String.valueOf(allMatches.size())), 1, rowIndex);
            resultPane.add(new Label(String.valueOf(thisWinMatchesFinal)), 2, rowIndex);
            resultPane.add(new Label(
                            String.format("%.1f%%",
                                    (double) thisWinMatchesFinal / allMatches.size() * 100)),
                    3, rowIndex++);

            resultPane.add(new Label(strings.getString("totalFramesLife")), 0, rowIndex);
            resultPane.add(new Label(String.valueOf(totalFramesFinal)), 1, rowIndex);
            resultPane.add(new Label(String.valueOf(thisWinFramesFinal)), 2, rowIndex);
            resultPane.add(new Label(
                            String.format("%.1f%%",
                                    (double) thisWinFramesFinal / totalFramesFinal * 100)),
                    3, rowIndex++);

            resultPane.add(new Label(strings.getString("statsFinalsOver3")), 0, rowIndex);
            resultPane.add(new Label(String.valueOf(finalFrames1)), 1, rowIndex);
            resultPane.add(new Label(String.valueOf(finalFrameWins1)), 2, rowIndex);
            resultPane.add(new Label(
                            finalFrames1 == 0 ?
                                    "--" :
                                    String.format("%.1f%%",
                                            (double) finalFrameWins1 / finalFrames1 * 100)),
                    3, rowIndex++);

            resultPane.add(new Separator(), 0, rowIndex++, 4, 1);

            // 更新各种率
            if (gameRule.snookerLike()) {
                // 50+
                resultPane.add(new Label(String.format("%.2f%%", breaksScores[2] * 100.0 / totalFramesFinal)),
                        2, breaksFiftyRow);
                // 100+
                resultPane.add(new Label(String.format("%.2f%%", breaksScores[3] * 100.0 / totalFramesFinal)),
                        2, breaksCenturyRow);
                // 147
                resultPane.add(new Label(String.format("%.2f‱", breaksScores[4] * 10000.0 / totalFramesFinal)),
                        2, breaks147Row);
            }

            // 每种局长的胜率
            for (Map.Entry<Integer, int[]> entry : playerWinsByTotalFrames.entrySet()) {
                int pWins = entry.getValue()[0];
                int total = entry.getValue()[1];
                double pRate = (double) pWins / total * 100;
                resultPane.add(
                        new Label(String.format(strings.getString("bestOfNFrames"),
                                entry.getKey(), entry.getKey() / 2 + 1)),
                        0, rowIndex);
                resultPane.add(new Label(String.format(strings.getString("boNStats"), total, pWins)), 1, rowIndex);
                resultPane.add(new Label(String.format("%.1f%%", pRate)), 2, rowIndex);
                rowIndex++;
            }
        });
    }
}
