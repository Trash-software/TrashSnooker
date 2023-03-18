package trashsoftware.trashSnooker.fxml.statsViews;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.db.DBAccess;
import trashsoftware.trashSnooker.util.db.EntireGameRecord;
import trashsoftware.trashSnooker.util.db.EntireGameTitle;

import java.util.*;

public class OpponentRecord extends RecordTree {

    final StatsView.PlayerAi thisPlayer;
    final List<EntireGameTitle> egtList;

    OpponentRecord(StatsView.PlayerAi thisPlayer, StatsView.PlayerAi opponent, List<EntireGameTitle> egtList, ResourceBundle strings) {
        super(opponent.toString(), strings);
        this.thisPlayer = thisPlayer;
        this.egtList = egtList;
    }

    @Override
    void setRightPane(Pane rightPane) {
        rightPane.getChildren().clear();

        String oppoName = shown;

        List<EntireGameRecord> entireRecords = new ArrayList<>();
        for (EntireGameTitle egt : egtList) {
            entireRecords.add(DBAccess.getInstance().getMatchDetail(egt));
        }

        GridPane gridPane = new GridPane();
        gridPane.setVgap(10.0);
        gridPane.setHgap(20.0);
        gridPane.setAlignment(Pos.CENTER);

        SortedMap<Integer, int[]> playerOppoWinsByTotalFrames = new TreeMap<>();
        int thisWinFrames = 0;
        int oppoWinFrames = 0;
        int thisWinMatches = 0;
        for (EntireGameRecord egr : entireRecords) {
            boolean thisIsP1 = egr.getTitle().player1Id.equals(thisPlayer.playerId);
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

        int rowIndex = 0;
        gridPane.add(new Label(DataLoader.getInstance().getPlayerPerson(thisPlayer.playerId).getName()), 0, rowIndex);
        gridPane.add(new Label(oppoName), 4, rowIndex);
        gridPane.add(new Label(String.format("交手%d次，共%d局", egtList.size(),
                        thisWinFrames + oppoWinFrames)),
                2, rowIndex);
        rowIndex++;

        double matchWinRate = (double) thisWinMatches / egtList.size() * 100;
        gridPane.add(new Label("胜利"), 2, rowIndex);
        gridPane.add(new Label(String.valueOf(thisWinMatches)), 0, rowIndex);
        gridPane.add(new Label(String.format("%.1f%%", matchWinRate)), 1, rowIndex);
        gridPane.add(new Label(String.valueOf(egtList.size() - thisWinMatches)),
                4, rowIndex);
        gridPane.add(new Label(String.format("%.1f%%", 100 - matchWinRate)), 3, rowIndex);
        rowIndex++;

        double frameWinRate = (double) thisWinFrames / (thisWinFrames + oppoWinFrames) * 100;
        gridPane.add(new Label("总胜局数"), 2, rowIndex);
        gridPane.add(new Label(String.valueOf(thisWinFrames)), 0, rowIndex);
        gridPane.add(new Label(String.format("%.1f%%", frameWinRate)), 1, rowIndex);
        gridPane.add(new Label(String.valueOf(oppoWinFrames)),
                4, rowIndex);
        gridPane.add(new Label(String.format("%.1f%%", 100 - frameWinRate)), 3, rowIndex);
        rowIndex++;

        gridPane.add(new Separator(), 0, rowIndex++, 5, 1);
        for (Map.Entry<Integer, int[]> entry : playerOppoWinsByTotalFrames.entrySet()) {
            int pWins = entry.getValue()[0];
            int oWins = entry.getValue()[1];
            double pRate = (double) pWins / (pWins + oWins) * 100;
            gridPane.add(
                    new Label(String.format("%d局%d胜制",
                            entry.getKey(), entry.getKey() / 2 + 1)),
                    2, rowIndex);
            gridPane.add(new Label(String.valueOf(pWins)), 0, rowIndex);
            gridPane.add(new Label(String.format("%.1f%%", pRate)), 1, rowIndex);
            gridPane.add(new Label(String.format("%.1f%%", 100 - pRate)), 3, rowIndex);
            gridPane.add(new Label(String.valueOf(oWins)), 4, rowIndex);
            rowIndex++;
        }

        rightPane.getChildren().add(gridPane);
    }
}
