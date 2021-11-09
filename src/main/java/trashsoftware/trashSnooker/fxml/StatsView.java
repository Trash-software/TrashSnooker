package trashsoftware.trashSnooker.fxml;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import trashsoftware.trashSnooker.core.GameType;
import trashsoftware.trashSnooker.fxml.widgets.MatchRecordPage;
import trashsoftware.trashSnooker.util.Util;
import trashsoftware.trashSnooker.util.db.DBAccess;
import trashsoftware.trashSnooker.util.db.EntireGameRecord;
import trashsoftware.trashSnooker.util.db.EntireGameTitle;
import trashsoftware.trashSnooker.util.db.PlayerFrameRecord;

import java.net.URL;
import java.util.*;

public class StatsView implements Initializable {
    @FXML
    TreeView<RecordTree> treeView;

    @FXML
    TableView<RecordItem> tableView;

    @FXML
    VBox rightPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initTree();
    }

    private void initTree() {
        TreeItem<RecordTree> root = new TreeItem<>(new RecordTree("记录"));
        DBAccess db = DBAccess.getInstance();
        List<String> names = db.listAllPlayerNames();
        for (String name : names) {
            root.getChildren().add(new PersonTreeItem(name));
        }
        treeView.setRoot(root);
        treeView.getSelectionModel().selectedItemProperty()
                .addListener(((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        RecordTree value = newValue.getValue();
                        value.setRightPane(rightPane);
                    }
                }));
        root.setExpanded(true);
    }

    public static class PersonTreeItem extends TreeItem<RecordTree> {
        private final String name;

        private boolean firstTimeChildren = true;

        PersonTreeItem(String name) {
            this.name = name;
            setValue(new RecordTree(this.name));
        }

        private static ObservableList<TreeItem<RecordTree>> buildChildren(String name) {
            ObservableList<TreeItem<RecordTree>> children = FXCollections.observableArrayList();
            for (GameType gameType : GameType.values()) {
                TreeItem<RecordTree> typeItem = new TreeItem<>(new GameTypeTree(name, gameType));
                typeItem.getChildren().add(new RecordSorting(gameType, name, "time"));
                typeItem.getChildren().add(new RecordSorting(gameType, name, "opponent"));
                children.add(typeItem);
            }
            return children;
        }

        @Override
        public ObservableList<TreeItem<RecordTree>> getChildren() {
            if (firstTimeChildren) {
                firstTimeChildren = false;
                super.getChildren().setAll(buildChildren(name));
            }
            return super.getChildren();
        }

        @Override
        public boolean isLeaf() {
            return false;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public static class GameTypeTree extends RecordTree {
        private final String name;
        private final GameType gameType;

        GameTypeTree(String name, GameType gameType) {
            super(getString(gameType));
            this.name = name;
            this.gameType = gameType;
        }

        public static String getString(GameType gameType) {
            if (gameType == GameType.SNOOKER) {
                return "斯诺克";
            } else if (gameType == GameType.MINI_SNOOKER) {
                return "小斯诺克";
            } else if (gameType == GameType.CHINESE_EIGHT) {
                return "中式八球";
            } else if (gameType == GameType.SIDE_POCKET) {
                return "美式九球";
            } else {
                return "";
            }
        }

        @Override
        void setRightPane(Pane rightPane) {
            rightPane.getChildren().clear();
            GridPane resultPane = new GridPane();
            resultPane.setVgap(10.0);
            resultPane.setHgap(20.0);
            resultPane.setAlignment(Pos.CENTER);

            DBAccess db = DBAccess.getInstance();
            int[] potRecords = db.getBasicPotStatusAll(gameType, name);

            int rowIndex = 0;

            int potAttempts = potRecords[0];
            int potSuccesses = potRecords[1];
            resultPane.add(new Label("进攻次数"), 0, rowIndex);
            resultPane.add(new Label(String.valueOf(potAttempts)), 1, rowIndex++);
            resultPane.add(new Label("进攻成功次数"), 0, rowIndex);
            resultPane.add(new Label(String.valueOf(potSuccesses)), 1, rowIndex++);
            resultPane.add(new Label("进攻成功率"), 0, rowIndex);
            resultPane.add(new Label(
                    potAttempts == 0 ? "0%" :
                            String.format("%.1f%%", potSuccesses * 100.0 / potAttempts)), 1, rowIndex++);

            int longPotAttempts = potRecords[2];
            int longPotSuccesses = potRecords[3];
            resultPane.add(new Label("长台进攻次数"), 0, rowIndex);
            resultPane.add(new Label(String.valueOf(longPotAttempts)), 1, rowIndex++);
            resultPane.add(new Label("长台进攻成功次数"), 0, rowIndex);
            resultPane.add(new Label(String.valueOf(longPotSuccesses)), 1, rowIndex++);
            resultPane.add(new Label("长台进攻成功率"), 0, rowIndex);
            resultPane.add(new Label(
                    longPotAttempts == 0 ? "0%" :
                            String.format("%.1f%%",
                                    longPotSuccesses * 100.0 / longPotAttempts)), 1, rowIndex++);

            int defAttempts = potRecords[4];
            int defSuccesses = potRecords[5];
            resultPane.add(new Label("防守次数"), 0, rowIndex);
            resultPane.add(new Label(String.valueOf(defAttempts)), 1, rowIndex++);
            resultPane.add(new Label("防守成功次数"), 0, rowIndex);
            resultPane.add(new Label(String.valueOf(defSuccesses)), 1, rowIndex++);
            resultPane.add(new Label("防守成功率"), 0, rowIndex);
            resultPane.add(new Label(
                    defAttempts == 0 ? "0%" :
                            String.format("%.1f%%",
                                    defSuccesses * 100.0 / defAttempts)), 1, rowIndex++);

            if (gameType.snookerLike) {
                int[] breaksScores = db.getSnookerBreaksTotal(gameType, name);
                resultPane.add(new Label("总得分"), 0, rowIndex);
                resultPane.add(new Label(String.valueOf(breaksScores[0])), 1, rowIndex++);
                resultPane.add(new Label("最高单杆"), 0, rowIndex);
                resultPane.add(new Label(String.valueOf(breaksScores[1])), 1, rowIndex++);
                resultPane.add(new Label("单杆50+"), 0, rowIndex);
                resultPane.add(new Label(String.valueOf(breaksScores[2])), 1, rowIndex++);
                resultPane.add(new Label("单杆100+"), 0, rowIndex);
                resultPane.add(new Label(String.valueOf(breaksScores[3])), 1, rowIndex++);
                resultPane.add(new Label("单杆147"), 0, rowIndex);
                resultPane.add(new Label(String.valueOf(breaksScores[4])), 1, rowIndex++);
            } else if (gameType == GameType.CHINESE_EIGHT || gameType == GameType.SIDE_POCKET) {
                int[] breaksScores = db.getNumberedBallGamesTotal(gameType, name);
                resultPane.add(new Label("炸清"), 0, rowIndex);
                resultPane.add(new Label(String.valueOf(breaksScores[0])), 1, rowIndex++);
                resultPane.add(new Label("接清"), 0, rowIndex);
                resultPane.add(new Label(String.valueOf(breaksScores[1])), 1, rowIndex++);
                resultPane.add(new Label("最高单杆球数"), 0, rowIndex);
                resultPane.add(new Label(String.valueOf(breaksScores[2])), 1, rowIndex++);
            }
            rightPane.getChildren().add(resultPane);
        }
    }

    public static class RecordTree {
        protected final String shown;

        RecordTree(String shown) {
            this.shown = shown;
        }

        void setRightPane(Pane rightPane) {
            rightPane.getChildren().clear();
        }

        @Override
        public String toString() {
            return shown;
        }
    }

    public static class RecordSorting extends TreeItem<RecordTree> {
        private final String shown;
        private final String playerName;
        private final GameType gameType;
        private boolean firstTimeChildren = true;

        RecordSorting(GameType gameType, String playerName, String shown) {
            this.shown = shown;
            this.playerName = playerName;
            this.gameType = gameType;

            setValue(new RecordTree(getShowingStr()));
        }

        private static ObservableList<TreeItem<RecordTree>> buildChildren(
                GameType gameType, String playerName, String type) {
            ObservableList<TreeItem<RecordTree>> children = FXCollections.observableArrayList();
            DBAccess dbAccess = DBAccess.getInstance();
            List<EntireGameTitle> gameRecords = dbAccess.getAllMatches(gameType, playerName);
            if ("time".equals(type)) {
                for (EntireGameTitle egt : gameRecords) {
                    children.add(new TreeItem<>(new MatchRecord(egt)));
                }
            } else {
                Map<String, List<EntireGameTitle>> opponentMap = new HashMap<>();
                for (EntireGameTitle egt : gameRecords) {
                    String oppoName = egt.player1Name.equals(playerName) ?
                            egt.player2Name : egt.player1Name;
                    List<EntireGameTitle> list =
                            opponentMap.get(oppoName);
                    if (list == null) {
                        list = new ArrayList<>();
                        opponentMap.put(oppoName, list);
                        children.add(new TreeItem<>(
                                new OpponentRecord(playerName, oppoName, list)));
                    }
                    list.add(egt);
                }
            }
            return children;
        }

        @Override
        public ObservableList<TreeItem<RecordTree>> getChildren() {
            if (firstTimeChildren) {
                firstTimeChildren = false;
                super.getChildren().setAll(buildChildren(gameType, playerName, shown));
            }
            return super.getChildren();
        }

        @Override
        public boolean isLeaf() {
            return false;
        }

        public String getShowingStr() {
            if ("time".equals(shown)) return "比赛";
            else return "对手";
        }
    }

    public static class RecordItem {

    }

    public static class OpponentRecord extends RecordTree {
        final String thisPlayer;
        final List<EntireGameTitle> egtList;

        OpponentRecord(String thisPlayer, String opponent, List<EntireGameTitle> egtList) {
            super(opponent);
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
                boolean thisIsP1 = egr.getTitle().player1Name.equals(thisPlayer);
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
            gridPane.add(new Label(thisPlayer), 0, rowIndex);
            gridPane.add(new Label(oppoName), 2, rowIndex);
            rowIndex++;

            gridPane.add(new Label("交手次数"), 1, rowIndex);
            gridPane.add(new Label(String.valueOf(egtList.size())), 0, rowIndex);
            gridPane.add(new Label(String.valueOf(egtList.size())), 2, rowIndex);
            rowIndex++;

            gridPane.add(new Label("胜利"), 1, rowIndex);
            gridPane.add(new Label(String.valueOf(thisWinMatches)), 0, rowIndex);
            gridPane.add(new Label(String.valueOf(egtList.size() - thisWinMatches)),
                    2, rowIndex);
            rowIndex++;

            gridPane.add(new Label("总胜局数"), 1, rowIndex);
            gridPane.add(new Label(String.valueOf(thisWinFrames)), 0, rowIndex);
            gridPane.add(new Label(String.valueOf(oppoWinFrames)),
                    2, rowIndex);
            rowIndex++;

            gridPane.add(new Separator(), 0, rowIndex++, 3, 1);
            for (Map.Entry<Integer, int[]> entry : playerOppoWinsByTotalFrames.entrySet()) {
                gridPane.add(
                        new Label(String.format("%d局%d胜制", 
                                entry.getKey(), entry.getKey() / 2 + 1)),
                        1, rowIndex);
                gridPane.add(new Label(String.valueOf(entry.getValue()[0])), 0, rowIndex);
                gridPane.add(new Label(String.valueOf(entry.getValue()[1])), 2, rowIndex);
                rowIndex++;
            }

            rightPane.getChildren().add(gridPane);
        }
    }

    public static class MatchRecord extends RecordTree {
        final EntireGameTitle egt;

        MatchRecord(EntireGameTitle egt) {
            super(egt.toString());
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
            if (p1p2Wins[0] > p1p2Wins[1]) {
                winLost = new String[]{"WIN", "LOST"};
            } else {
                winLost = new String[]{"LOST", "WIN"};
            }
            page.add(new Label(winLost[0]), 1, rowIndex);
            page.add(new Label(winLost[1]), 5, rowIndex);
            rowIndex++;

            page.add(new Label(egt.player1Name), 1, rowIndex);
            page.add(new Label(String.valueOf(p1p2Wins[0])), 2, rowIndex);
            page.add(new Label(String.format("(%d)", egt.totalFrames)), 3, rowIndex);
            page.add(new Label(String.valueOf(p1p2Wins[1])), 4, rowIndex);
            page.add(new Label(egt.player2Name), 5, rowIndex);
            rowIndex++;

            int[][] playersTotalBasics = matchRec.totalBasicStats();
//            System.out.println(Arrays.deepToString(playersTotalBasics));
            page.add(new Label("进攻次数"), 0, rowIndex);
            page.add(new Label(String.valueOf(playersTotalBasics[0][0])), 1, rowIndex);
            page.add(new Label(String.valueOf(playersTotalBasics[1][0])), 5, rowIndex);
            rowIndex++;

            page.add(new Label("进攻成功次数"), 0, rowIndex);
            page.add(new Label(String.valueOf(playersTotalBasics[0][1])), 1, rowIndex);
            page.add(new Label(String.valueOf(playersTotalBasics[1][1])), 5, rowIndex);
            rowIndex++;

            page.add(new Label("进攻成功率"), 0, rowIndex);
            page.add(new Label(
                            playersTotalBasics[0][1] == 0 ? "0%" :
                                    String.format("%.1f%%",
                                            playersTotalBasics[0][1] * 100.0 /
                                                    playersTotalBasics[0][0])),
                    1, rowIndex);
            page.add(new Label(
                            playersTotalBasics[1][1] == 0 ? "0%" :
                                    String.format("%.1f%%",
                                            playersTotalBasics[1][1] * 100.0 /
                                                    playersTotalBasics[1][0])),
                    5, rowIndex);
            rowIndex++;

            page.add(new Label("长台进攻次数"), 0, rowIndex);
            page.add(new Label(String.valueOf(playersTotalBasics[0][2])), 1, rowIndex);
            page.add(new Label(String.valueOf(playersTotalBasics[1][2])), 5, rowIndex);
            rowIndex++;

            page.add(new Label("长台进攻成功次数"), 0, rowIndex);
            page.add(new Label(String.valueOf(playersTotalBasics[0][3])), 1, rowIndex);
            page.add(new Label(String.valueOf(playersTotalBasics[1][3])), 5, rowIndex);
            rowIndex++;

            page.add(new Label("长台进攻成功率"), 0, rowIndex);
            page.add(new Label(
                            playersTotalBasics[0][3] == 0 ? "0%" :
                                    String.format("%.1f%%",
                                            playersTotalBasics[0][3] * 100.0 /
                                                    playersTotalBasics[0][2])),
                    1, rowIndex);
            page.add(new Label(
                            playersTotalBasics[1][3] == 0 ? "0%" :
                                    String.format("%.1f%%",
                                            playersTotalBasics[1][3] * 100.0 /
                                                    playersTotalBasics[1][2])),
                    5, rowIndex);
            rowIndex++;

            page.add(new Label("防守次数"), 0, rowIndex);
            page.add(new Label(String.valueOf(playersTotalBasics[0][4])), 1, rowIndex);
            page.add(new Label(String.valueOf(playersTotalBasics[1][4])), 5, rowIndex);
            rowIndex++;

            page.add(new Label("防守成功次数"), 0, rowIndex);
            page.add(new Label(String.valueOf(playersTotalBasics[0][5])), 1, rowIndex);
            page.add(new Label(String.valueOf(playersTotalBasics[1][5])), 5, rowIndex);
            rowIndex++;

            page.add(new Label("防守成功率"), 0, rowIndex);
            page.add(new Label(
                            playersTotalBasics[0][5] == 0 ? "0%" :
                                    String.format("%.1f%%",
                                            playersTotalBasics[0][5] * 100.0 /
                                                    playersTotalBasics[0][4])),
                    1, rowIndex);
            page.add(new Label(
                            playersTotalBasics[1][5] == 0 ? "0%" :
                                    String.format("%.1f%%",
                                            playersTotalBasics[1][5] * 100.0 /
                                                    playersTotalBasics[1][4])),
                    5, rowIndex);
            rowIndex++;

            if (egt.gameType.snookerLike) {
                int[][] totalSnookerScores = ((EntireGameRecord.Snooker) matchRec).totalScores();
                page.add(new Label("总得分"), 0, rowIndex);
                page.add(new Label(String.valueOf(totalSnookerScores[0][0])), 1, rowIndex);
                page.add(new Label(String.valueOf(totalSnookerScores[1][0])), 5, rowIndex);
                rowIndex++;

                page.add(new Label("最高单杆"), 0, rowIndex);
                page.add(new Label(String.valueOf(totalSnookerScores[0][1])), 1, rowIndex);
                page.add(new Label(String.valueOf(totalSnookerScores[1][1])), 5, rowIndex);
                rowIndex++;

                page.add(new Label("50+"), 0, rowIndex);
                page.add(new Label(String.valueOf(totalSnookerScores[0][2])), 1, rowIndex);
                page.add(new Label(String.valueOf(totalSnookerScores[1][2])), 5, rowIndex);
                rowIndex++;

                page.add(new Label("100+"), 0, rowIndex);
                page.add(new Label(String.valueOf(totalSnookerScores[0][3])), 1, rowIndex);
                page.add(new Label(String.valueOf(totalSnookerScores[1][3])), 5, rowIndex);
                rowIndex++;

                page.add(new Label("147"), 0, rowIndex);
                page.add(new Label(String.valueOf(totalSnookerScores[0][4])), 1, rowIndex);
                page.add(new Label(String.valueOf(totalSnookerScores[1][4])), 5, rowIndex);
                rowIndex++;
            } else if (egt.gameType == GameType.CHINESE_EIGHT || 
                    egt.gameType == GameType.SIDE_POCKET) {
                int[][] numberedBreaks = ((EntireGameRecord.NumberedBall) matchRec).totalScores();
                page.add(new Label("炸清"), 0, rowIndex);
                page.add(new Label(String.valueOf(numberedBreaks[0][0])), 1, rowIndex);
                page.add(new Label(String.valueOf(numberedBreaks[1][0])), 5, rowIndex);
                rowIndex++;

                page.add(new Label("接清"), 0, rowIndex);
                page.add(new Label(String.valueOf(numberedBreaks[0][1])), 1, rowIndex);
                page.add(new Label(String.valueOf(numberedBreaks[1][1])), 5, rowIndex);
                rowIndex++;

                page.add(new Label("单杆最高球数"), 0, rowIndex);
                page.add(new Label(String.valueOf(numberedBreaks[0][2])), 1, rowIndex);
                page.add(new Label(String.valueOf(numberedBreaks[1][2])), 5, rowIndex);
                rowIndex++;
            }

            page.add(new Separator(), 0, rowIndex, 6, 1);
            rowIndex++;

            // 分局显示
            for (Map.Entry<Integer, PlayerFrameRecord[]> entry :
                    matchRec.getFrameRecords().entrySet()) {
                PlayerFrameRecord p1r = entry.getValue()[0];
                PlayerFrameRecord p2r = entry.getValue()[1];
                page.add(new Label(
                                Util.secondsToString(matchRec.getFrameDurations().get(entry.getKey()))),
                        3, rowIndex);

                Label p1ScoreLabel = new Label();
                Label p2ScoreLabel = new Label();
                if (egt.gameType.snookerLike) {
                    PlayerFrameRecord.Snooker p1sr = (PlayerFrameRecord.Snooker) p1r;
                    PlayerFrameRecord.Snooker p2sr = (PlayerFrameRecord.Snooker) p2r;
                    p1ScoreLabel.setText(String.valueOf(p1sr.snookerScores[0]));
                    p2ScoreLabel.setText(String.valueOf(p2sr.snookerScores[0]));
                }

                if (p1r.winnerName.equals(egt.player1Name)) {
                    p2ScoreLabel.setDisable(true);
                    page.add(new Label("⚫"), 2, rowIndex);
                } else {
                    p1ScoreLabel.setDisable(true);
                    page.add(new Label("⚫"), 4, rowIndex);
                }
                page.add(p1ScoreLabel, 1, rowIndex);
                page.add(p2ScoreLabel, 5, rowIndex);

                rowIndex++;
            }

            rightPane.getChildren().add(page);
        }
    }
}
