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
import trashsoftware.trashSnooker.util.Util;
import trashsoftware.trashSnooker.util.db.DBAccess;
import trashsoftware.trashSnooker.util.db.EntireGameRecord;
import trashsoftware.trashSnooker.util.db.EntireGameTitle;

import java.util.*;

public class GameTypeTree extends RecordTree {
    private final StatsView.PlayerAi pai;
    private final GameRule gameRule;
    final boolean careerOnly;

    private int breaksFiftyRow;
    private int breaksCenturyRow;
    private int breaks147Row;
    private int[] breaksScores;

    GameTypeTree(StatsView.PlayerAi pai, GameRule gameRule, boolean careerOnly, ResourceBundle strings) {
        super(GameRule.toReadable(gameRule), strings);
        this.pai = pai;
        this.gameRule = gameRule;
        this.careerOnly = careerOnly;
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
        resultPane.add(new Label(showNumber(potAttempts)), 1, rowIndex);
//        resultPane.add(new Label(strings.getString("statsAttacksSuc")), 0, rowIndex);
        resultPane.add(new Label(showNumber(potSuccesses)), 2, rowIndex);
//            resultPane.add(new Label("进攻成功率"), 0, rowIndex);
        resultPane.add(new Label(
                showPercent(potSuccesses, potAttempts)), 3, rowIndex++);

        int longPotAttempts = potRecords[2];
        int longPotSuccesses = potRecords[3];
        resultPane.add(new Label(strings.getString("statsLongAttacks1")), 0, rowIndex);
        resultPane.add(new Label(showNumber(longPotAttempts)), 1, rowIndex);
//        resultPane.add(new Label(strings.getString("statsLongAttacksSuc")), 0, rowIndex);
        resultPane.add(new Label(showNumber(longPotSuccesses)), 2, rowIndex);
//            resultPane.add(new Label("长台进攻成功率"), 0, rowIndex);
        resultPane.add(new Label(
                showPercent(longPotSuccesses, longPotAttempts)), 3, rowIndex++);

        int restAttempts = potRecords[8];
        int restSuccesses = potRecords[9];
        resultPane.add(new Label(strings.getString("statsRestAttacks1")), 0, rowIndex);
        resultPane.add(new Label(showNumber(restAttempts)), 1, rowIndex);
//        resultPane.add(new Label(strings.getString("statsRestAttacksSuc")), 0, rowIndex);
        resultPane.add(new Label(showNumber(restSuccesses)), 2, rowIndex);
//            resultPane.add(new Label("进攻成功率"), 0, rowIndex);
        resultPane.add(new Label(
                showPercent(restSuccesses, restAttempts)), 3, rowIndex++);

        int positionAttempts = potRecords[6];
        int positionSuccesses = potRecords[7];
        resultPane.add(new Label(strings.getString("statsPositions1")), 0, rowIndex);
        resultPane.add(new Label(showNumber(positionAttempts)), 1, rowIndex);
//        resultPane.add(new Label(strings.getString("statsPositionsSuc")), 0, rowIndex);
        resultPane.add(new Label(showNumber(positionSuccesses)), 2, rowIndex);
//            resultPane.add(new Label("进攻成功率"), 0, rowIndex);
        resultPane.add(new Label(
                showPercent(positionSuccesses, positionAttempts)), 3, rowIndex++);

        int defAttempts = potRecords[4];
        int defSuccesses = potRecords[5];
        resultPane.add(new Label(strings.getString("statsDefenses1")), 0, rowIndex);
        resultPane.add(new Label(showNumber(defAttempts)), 1, rowIndex);
//        resultPane.add(new Label(strings.getString("statsDefensesSuc")), 0, rowIndex);
        resultPane.add(new Label(showNumber(defSuccesses)), 2, rowIndex);
//            resultPane.add(new Label("防守成功率"), 0, rowIndex);
        resultPane.add(new Label(
                showPercent(defSuccesses, defAttempts)), 3, rowIndex++);

        int solves = potRecords[10];
        int solveSuccesses = potRecords[11];
        resultPane.add(new Label(strings.getString("statsEscapes1")), 0, rowIndex);
        resultPane.add(new Label(showNumber(solves)), 1, rowIndex);
//        resultPane.add(new Label(strings.getString("statsEscapesSuc")), 0, rowIndex);
        resultPane.add(new Label(showNumber(solveSuccesses)), 2, rowIndex);
//            resultPane.add(new Label("防守成功率"), 0, rowIndex);
        resultPane.add(new Label(
               showPercent(solveSuccesses, solves)), 3, rowIndex++);

        resultPane.add(new Separator(), 0, rowIndex++, 4, 1);

        if (gameRule.snookerLike()) {
            breaksScores = db.getSnookerBreaksTotal(gameRule, pai.playerId, pai.isAi);
            resultPane.add(new Label(strings.getString("totalPoints")), 0, rowIndex);
            resultPane.add(new Label(showNumber(breaksScores[0])), 1, rowIndex++);
            resultPane.add(new Label(strings.getString("highestBreak")), 0, rowIndex);
            resultPane.add(new Label(showNumber(breaksScores[1])), 1, rowIndex++);

            resultPane.add(new Label(strings.getString("single50")), 0, rowIndex);
            breaksFiftyRow = rowIndex;
            resultPane.add(new Label(showNumber(breaksScores[2])), 1, rowIndex++);

            resultPane.add(new Label(strings.getString("single100")), 0, rowIndex);
            breaksCenturyRow = rowIndex;
            resultPane.add(new Label(showNumber(breaksScores[3])), 1, rowIndex++);

            resultPane.add(new Label(strings.getString("single147")), 0, rowIndex);
            breaks147Row = rowIndex;
            resultPane.add(new Label(showNumber(breaksScores[4])), 1, rowIndex++);
        } else if (gameRule == GameRule.CHINESE_EIGHT || gameRule == GameRule.AMERICAN_NINE) {
            breaksScores = db.getNumberedBallGamesTotal(gameRule, pai.playerId, pai.isAi);
            resultPane.add(new Label(strings.getString("numBreaks")), 0, rowIndex);
            resultPane.add(new Label(showNumber(breaksScores[0])), 1, rowIndex);
            resultPane.add(new Label(showNumber(breaksScores[1])), 2, rowIndex);
            resultPane.add(new Label(showPercent(breaksScores[1], breaksScores[0])),
                    3, rowIndex++);
            resultPane.add(new Label(strings.getString("breakClears")), 0, rowIndex);
            resultPane.add(new Label(showNumber(breaksScores[2])), 1, rowIndex++);
            resultPane.add(new Label(strings.getString("continueClears")), 0, rowIndex);
            resultPane.add(new Label(showNumber(breaksScores[3])), 1, rowIndex++);
            resultPane.add(new Label(strings.getString("highestSingleBalls")), 0, rowIndex);
            resultPane.add(new Label(showNumber(breaksScores[4])), 1, rowIndex++);
            
            if (gameRule == GameRule.AMERICAN_NINE) {
                resultPane.add(new Label(strings.getString("goldNines")), 0, rowIndex);
                resultPane.add(new Label(showNumber(breaksScores[5])), 1, rowIndex++);
            }
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
                DBAccess.getInstance().getAllPveMatches(gameRule, pai.playerId, pai.isAi, careerOnly);
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
        int durations = 0;
        for (EntireGameRecord egr : entireRecords) {
            durations += egr.getFrameDurations().values().stream().reduce(0, Integer::sum);
            boolean thisIsP1 = egr.getTitle().player1Id.equals(pai.playerId);
            int[] playerWinsInThisMatchSize =
                    playerWinsByTotalFrames.computeIfAbsent(
                            egr.getTitle().totalFrames, k -> new int[2]);
            playerWinsInThisMatchSize[1]++;
            int[] p1p2wins = egr.getP1P2WinsCount();
            totalFrames += egr.getP1P2WinsCount()[0] + egr.getP1P2WinsCount()[1];
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
        final int totalDuration = durations;

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
            resultPane.add(new Label(showNumber(allMatches.size())), 1, rowIndex);
            resultPane.add(new Label(showNumber(thisWinMatchesFinal)), 2, rowIndex);
            resultPane.add(new Label(
                    showPercent(thisWinMatchesFinal, allMatches.size())),
                    3, rowIndex++);

            resultPane.add(new Label(strings.getString("totalFramesLife")), 0, rowIndex);
            resultPane.add(new Label(showNumber(totalFramesFinal)), 1, rowIndex);
            resultPane.add(new Label(showNumber(thisWinFramesFinal)), 2, rowIndex);
            resultPane.add(new Label(
                    showPercent(thisWinFramesFinal, totalFramesFinal)),
                    3, rowIndex++);

            resultPane.add(new Label(strings.getString("statsFinalsOver3")), 0, rowIndex);
            resultPane.add(new Label(showNumber(finalFrames1)), 1, rowIndex);
            resultPane.add(new Label(showNumber(finalFrameWins1)), 2, rowIndex);
            resultPane.add(new Label(
                    showPercent(finalFrameWins1, finalFrames1)),
                    3, rowIndex++);

            resultPane.add(new Separator(), 0, rowIndex++, 4, 1);

            // 更新各种率
            if (gameRule.snookerLike()) {
                // 50+
                resultPane.add(new Label(showPercent(breaksScores[2], totalFramesFinal, 2)),
                        2, breaksFiftyRow);
                // 100+
                resultPane.add(new Label(showPercent(breaksScores[3], totalFramesFinal, 2)),
                        2, breaksCenturyRow);
                // Maximum
                resultPane.add(new Label(showOneOver10000(breaksScores[4], totalFramesFinal, 2)),
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
            
            // 时长
            resultPane.add(new Label(strings.getString("totalDuration")), 0, rowIndex);
            resultPane.add(new Label(Util.secondsToString(totalDuration)), 1, rowIndex++);
        });
    }
}
