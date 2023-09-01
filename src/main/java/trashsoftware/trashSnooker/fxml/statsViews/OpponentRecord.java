package trashsoftware.trashSnooker.fxml.statsViews;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import trashsoftware.trashSnooker.fxml.widgets.AdversarialBar;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.db.DBAccess;
import trashsoftware.trashSnooker.util.db.EntireGameRecord;
import trashsoftware.trashSnooker.util.db.EntireGameTitle;

import java.util.*;

public class OpponentRecord extends RecordTree {

    final StatsView.PlayerAi thisPlayer;
    final List<EntireGameTitle> egtList;

    OpponentRecord(StatsView.PlayerAi thisPlayer,
                   StatsView.PlayerAi opponent,
                   List<EntireGameTitle> egtList,
                   ResourceBundle strings) {
        super(opponent.toString(), strings);
        this.thisPlayer = thisPlayer;
        this.egtList = egtList;
    }

    @Override
    void setRightPane(Pane rightPane) {
        rightPane.getChildren().clear();

        final String oppoName = shown;

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

        addRowTo(gridPane,
                rowIndex++,
                strings.getString("statsMatchesWinTitle"),
                thisWinMatches,
                egtList.size() - thisWinMatches);

        addRowTo(gridPane,
                rowIndex++,
                strings.getString("statsFramesWinTitle"),
                thisWinFrames,
                oppoWinFrames);

        gridPane.add(new Separator(), 0, rowIndex++, 5, 1);
        for (Map.Entry<Integer, int[]> entry : playerOppoWinsByTotalFrames.entrySet()) {
            int pWins = entry.getValue()[0];
            int oWins = entry.getValue()[1];
            double pRate = (double) pWins / (pWins + oWins) * 100;
            
            addRowTo(gridPane,
                    rowIndex++,
                    String.format(strings.getString("bestOfNFrames"),
                            entry.getKey(), entry.getKey() / 2 + 1),
                    pWins,
                    oWins);
        }

        rightPane.getChildren().add(gridPane);
    }

    private void addRowTo(GridPane gridPane,
                          int rowIndex,
                          String string,
                          int v1,
                          int v2) {
        double rate = (double) v1 / (v1 + v2);

        VBox matchWinBox = new VBox();
        matchWinBox.setAlignment(Pos.CENTER);
        matchWinBox.getChildren().add(new Label(string));
        matchWinBox.getChildren().add(new AdversarialBar(rate));

        gridPane.add(matchWinBox, 2, rowIndex);
        gridPane.add(new Label(String.valueOf(v1)), 0, rowIndex);
        gridPane.add(new Label(showPercent(rate)), 1, rowIndex);
        gridPane.add(new Label(String.valueOf(v2)),
                4, rowIndex);
        gridPane.add(new Label(showPercent(1 - rate)), 3, rowIndex);
    }
}
