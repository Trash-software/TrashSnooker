package trashsoftware.trashSnooker.fxml.statsViews;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import trashsoftware.trashSnooker.core.essential.Game;
import trashsoftware.trashSnooker.core.infoRec.AttackAnalysis;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.metrics.TableMetrics;
import trashsoftware.trashSnooker.core.training.TrainType;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.fxml.widgets.GamePane;
import trashsoftware.trashSnooker.util.config.ConfigLoader;

import java.util.*;

public class GraphicalPositionView extends VBox {

    private final Color leftColor = Color.TOMATO;
    private final Color leftColorDark = leftColor.deriveColor(0, 1.0, 0.7, 1.0);
    private final Color rightColor = Color.BLUE;
    private final Color rightColorDark = rightColor.deriveColor(0, 1.0, 0.7, 1.0);

    Color[] sucColors = {leftColor, rightColor};
    Color[] failColors = {leftColorDark, rightColorDark};

    GamePane gamePane;
    CheckBox p1Check = new CheckBox(), p2Check = new CheckBox();
    CheckBox showDetailCheck = new CheckBox();
    GridPane infoPane = new GridPane();
    GameValues gameValues;
    Game<?, ?> fakeGame;
    List<CheckBox> frameCheckBoxes = new ArrayList<>();
    List<CheckBox> allCheckBoxes = new ArrayList<>(List.of(p1Check, p2Check, showDetailCheck));
    List<AttackAnalysis.PotAttemptRec>[] attempts;

    public GraphicalPositionView(GameValues originalValues,
                                 List<AttackAnalysis.PotAttemptRec>[] attempts,
                                 ResourceBundle resourceBundle) {
        this.attempts = attempts;

        setPadding(new Insets(5.0));

        setAlignment(Pos.TOP_CENTER);
        setSpacing(10.0);
        infoPane.setAlignment(Pos.TOP_CENTER);
        infoPane.setVgap(10.0);
        infoPane.setHgap(20.0);

        gameValues = originalValues.clone();
        gameValues.setTrain(TrainType.TABLE_THUMB, null);

        gamePane = new GamePane(resourceBundle);

        double[] resolution = ConfigLoader.getInstance().getEffectiveResolution();
        gamePane.setupPane(gameValues, 0.6 * 1536 / resolution[0]);
        fakeGame = Game.createGame(null,
                gameValues,
                null,
                1,
                1);

        Font font = new Font(App.FONT.getName(), 14);
        gamePane.getGraphicsContext().setFont(font);

        p1Check.setSelected(true);
        p2Check.setSelected(true);

        refreshPane();
        showDetailCheck.setText(resourceBundle.getString("showGraphicalStatsDetail"));

        infoPane.add(p1Check, 1, 0);
        infoPane.add(p2Check, 6, 0);
        infoPane.add(showDetailCheck, 4, 0);
        getChildren().add(gamePane);
        getChildren().add(infoPane);
    }

    void addFrameCheckAt(Pane container) {
        CheckBox frameCheck = new CheckBox();
        frameCheck.setSelected(true);
        frameCheckBoxes.add(frameCheck);
        allCheckBoxes.add(frameCheck);
        container.getChildren().add(frameCheck);
    }

    public void finishSetup() {
        for (CheckBox checkBox : allCheckBoxes) {
            checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> refreshPane());
        }
        refreshPane();
    }

    private void drawOneP(int index,
                          boolean drawDetail,
                          Collection<Integer> framesIndexes,
                          Map<TableMetrics.PocketName, int[]> pocketSuccesses) {
        List<AttackAnalysis.PotAttemptRec> playerRec = attempts[index];
        for (AttackAnalysis.PotAttemptRec par : playerRec) {
            if (!framesIndexes.contains(par.frameIndex)) {
                continue;
            }
            if (par.potInfo.pocketName() == null || par.potInfo.whitePos() == null || par.potInfo.targetPos() == null) {
                System.err.println("Should not be null");
                continue;
            }

            int[] pocketSuc = pocketSuccesses.computeIfAbsent(par.potInfo.pocketName(), k -> new int[2]);
            pocketSuc[0]++;

            Color color;
            if (par.isSuccess()) {
                pocketSuc[1]++;
                color = sucColors[index];
                gamePane.drawCircle(par.potInfo.whitePos(), 15.0, color);
            } else {
                color = failColors[index];
                gamePane.drawCross(par.potInfo.whitePos(), 15.0, color);
            }

            if (drawDetail) {
                if (par.potInfo.collisionPos() == null) {
                    continue;
                }

                double x0 = gamePane.canvasX(par.potInfo.whitePos()[0]);
                double y0 = gamePane.canvasY(par.potInfo.whitePos()[1]);
                double x1 = gamePane.canvasX(par.potInfo.collisionPos()[0]);
                double y1 = gamePane.canvasY(par.potInfo.collisionPos()[1]);
                double x2 = gamePane.canvasX(par.potInfo.targetPos()[0]);
                double y2 = gamePane.canvasY(par.potInfo.targetPos()[1]);

                double x3, y3;
                if (par.potInfo.tarCushionPos() == null) {
                    double[] openCenter = gameValues.getOpenCenter(par.potInfo.pocketName());
                    x3 = gamePane.canvasX(openCenter[0]);
                    y3 = gamePane.canvasY(openCenter[1]);
                } else {
                    x3 = gamePane.canvasX(par.potInfo.tarCushionPos()[0]);
                    y3 = gamePane.canvasY(par.potInfo.tarCushionPos()[1]);
                }

                gamePane.getLineGraphics().setStroke(color);
                gamePane.getLineGraphics().setLineWidth(1);
                gamePane.getLineGraphics().strokeLine(x0, y0, x1, y1);
                
                // 因为AI打的不定球（斯诺克彩球、中八彩球等）没有记录indicated target，故采用首先碰到的球。这其实有问题，但暂时这样了
                int hitBall = par.cueInfoRec.getFirstHit();
                Color targetColor = gameValues.rule.ballBaseColor(hitBall);
                gamePane.getGraphicsContext().setFill(targetColor.interpolate(gameValues.table.tableColor, 0.4));
                double ballRadius = gameValues.ball.ballRadius * gamePane.getScale();
                gamePane.getGraphicsContext().fillOval(x2 - ballRadius, y2 - ballRadius,
                        ballRadius * 2, ballRadius * 2);

                if (par.isSuccess()) {
                    gamePane.getLineGraphics().setLineDashes();
                } else {
                    gamePane.getLineGraphics().setLineDashes(5, 5);
                }
                gamePane.getLineGraphics().strokeLine(x2, y2, x3, y3);
                gamePane.getLineGraphics().setLineDashes();
            }
        }
    }

    private void refreshPane() {
        //        gamePane.setupBalls(fakeGame, false);
        gamePane.drawTable(fakeGame);
//        gamePane.drawStoppedBalls(fakeGame.getTable(), fakeGame.getAllBalls(), null);
        Set<Integer> frameIndexes = new TreeSet<>();
        for (int i = 0; i < frameCheckBoxes.size(); i++) {
            if (frameCheckBoxes.get(i).isSelected()) frameIndexes.add(i + 1);  // frameIndex也是从1开始
        }

        Map<TableMetrics.PocketName, int[]> pocketSuccesses = new TreeMap<>();

        boolean drawDetail = showDetailCheck.isSelected();

        if (p1Check.isSelected()) drawOneP(0, drawDetail, frameIndexes, pocketSuccesses);
        if (p2Check.isSelected()) drawOneP(1, drawDetail, frameIndexes, pocketSuccesses);

        for (Map.Entry<TableMetrics.PocketName, int[]> entry : pocketSuccesses.entrySet()) {
            int[] attemptSuc = entry.getValue();
            drawTextBesidePocket(entry.getKey(), String.format("%d/%d", attemptSuc[1], attemptSuc[0]));
        }
    }

    private void drawTextBesidePocket(TableMetrics.PocketName pocketName, String text) {
        double[] openCenter = gameValues.getOpenCenter(pocketName);
        double[] tableCenter = gameValues.table.tableCenter();

        double hDt = openCenter[0] - tableCenter[0];
        double vDt = openCenter[1] - tableCenter[1];

        double textX = gamePane.canvasX(openCenter[0] - hDt * 0.08);
        double textY = gamePane.canvasY(openCenter[1] - vDt * 0.08);

        gamePane.getGraphicsContext().setFill(Color.WHITE);
        gamePane.getGraphicsContext().fillText(text, textX, textY);
    }
}
