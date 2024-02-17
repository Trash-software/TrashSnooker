package trashsoftware.trashSnooker.fxml.statsViews;

import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.util.db.DBAccess;
import trashsoftware.trashSnooker.util.db.EntireGameRecord;
import trashsoftware.trashSnooker.util.db.EntireGameTitle;

import java.util.*;

public class PlayerTypeTree extends RecordTree {
    final boolean isAi;
    boolean expand = false;

    PlayerTypeTree(boolean isAi, ResourceBundle strings) {
        super(isAi ? strings.getString("typeComputer") : strings.getString("typePlayer"), strings);

        this.isAi = isAi;
    }

    @Override
    void setRightPane(Pane rightPane) {
        rightPane.getChildren().clear();
        if (!expand) {
            VBox vBox = new VBox();
            Button button = new Button(strings.getString("queryRecords"));
            button.setOnAction(event -> {
                expand = true;
                setRightPane(rightPane);
            });
            vBox.getChildren().add(button);
            rightPane.getChildren().add(vBox);
        } else {
            GridPane gridPane = new GridPane();
            gridPane.setVgap(10.0);
            gridPane.setHgap(20.0);
            gridPane.setAlignment(Pos.CENTER);

            appendToPane(GameRule.SNOOKER, gridPane);
            appendToPane(GameRule.MINI_SNOOKER, gridPane);
            appendToPane(GameRule.SNOOKER_TEN, gridPane);
            appendToPane(GameRule.CHINESE_EIGHT, gridPane);
            appendToPane(GameRule.LIS_EIGHT, gridPane);
            appendToPane(GameRule.AMERICAN_NINE, gridPane);

            rightPane.getChildren().add(gridPane);
        }
    }

    private void appendToPane(GameRule gameRule, GridPane gridPane) {
        List<EntireGameTitle> egtList = DBAccess.getInstance().getAllPveMatches(gameRule);
        List<EntireGameRecord> entireRecords = new ArrayList<>();
        for (EntireGameTitle egt : egtList) {
            entireRecords.add(DBAccess.getInstance().getMatchDetail(egt));
        }

        SortedMap<Integer, int[]> playerOppoWinsByTotalFrames = new TreeMap<>();
        int thisWinFrames = 0;
        int oppoWinFrames = 0;
        int thisWinMatches = 0;
        for (EntireGameRecord egr : entireRecords) {
            boolean thisIsP1 = egr.getTitle().player1isAi == isAi;
            int[] p1p2wins = egr.getP1P2WinsCount();
            int[] playerOppoWinsInThisMatchSize =
                    playerOppoWinsByTotalFrames.computeIfAbsent(
                            egr.getTitle().totalFrames, k -> new int[2]);
            if (thisIsP1) {
                if (p1p2wins[0] > p1p2wins[1]) {
                    thisWinMatches++;
                    playerOppoWinsInThisMatchSize[0]++;
                } else {
                    playerOppoWinsInThisMatchSize[1]++;
                }
                thisWinFrames += p1p2wins[0];
                oppoWinFrames += p1p2wins[1];
            } else {
                if (p1p2wins[1] > p1p2wins[0]) {
                    thisWinMatches++;
                    playerOppoWinsInThisMatchSize[0]++;
                } else {
                    playerOppoWinsInThisMatchSize[1]++;
                }
                thisWinFrames += p1p2wins[1];
                oppoWinFrames += p1p2wins[0];
            }
        }
        
        int rowIndex = gridPane.getRowCount();
        gridPane.add(new Label(GameRule.toReadable(gameRule)), 2, rowIndex++);

        String thisShown = isAi ? strings.getString("typeComputer") : strings.getString("typePlayer");
        String oppoShown = isAi ? strings.getString("typePlayer") : strings.getString("typeComputer");

        gridPane.add(new Label(thisShown), 0, rowIndex);
        gridPane.add(new Label(oppoShown), 4, rowIndex);
        gridPane.add(new Label(String.format("交手%d次，共%d局", egtList.size(),
                        thisWinFrames + oppoWinFrames)),
                2, rowIndex);
        rowIndex++;

        double matchWinRate = (double) thisWinMatches / egtList.size();
        gridPane.add(new Label("胜利"), 2, rowIndex);
        gridPane.add(new Label(String.valueOf(thisWinMatches)), 0, rowIndex);
        gridPane.add(new Label(showPercent(matchWinRate)), 1, rowIndex);
        gridPane.add(new Label(String.valueOf(egtList.size() - thisWinMatches)),
                4, rowIndex);
        gridPane.add(new Label(showPercent(1 - matchWinRate)), 3, rowIndex);
        rowIndex++;

        double frameWinRate = (double) thisWinFrames / (thisWinFrames + oppoWinFrames);
        gridPane.add(new Label("总胜局数"), 2, rowIndex);
        gridPane.add(new Label(String.valueOf(thisWinFrames)), 0, rowIndex);
        gridPane.add(new Label(showPercent(frameWinRate)), 1, rowIndex);
        gridPane.add(new Label(String.valueOf(oppoWinFrames)),
                4, rowIndex);
        gridPane.add(new Label(showPercent(1 - frameWinRate)), 3, rowIndex);
        rowIndex++;

        gridPane.add(new Separator(), 0, rowIndex++, 5, 1);
        for (Map.Entry<Integer, int[]> entry : playerOppoWinsByTotalFrames.entrySet()) {
            int pWins = entry.getValue()[0];
            int oWins = entry.getValue()[1];
            double pRate = (double) pWins / (pWins + oWins) * 1;
            gridPane.add(
                    new Label(String.format("%d局%d胜制",
                            entry.getKey(), entry.getKey() / 2 + 1)),
                    2, rowIndex);
            gridPane.add(new Label(String.valueOf(pWins)), 0, rowIndex);
            gridPane.add(new Label(showPercent(pRate)), 1, rowIndex);
            gridPane.add(new Label(showPercent(1 - pRate)), 3, rowIndex);
            gridPane.add(new Label(String.valueOf(oWins)), 4, rowIndex);
            rowIndex++;
        }
        gridPane.add(new Separator(), 0, rowIndex, 5, 1);
    }
}
