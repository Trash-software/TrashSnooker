package trashsoftware.trashSnooker.fxml;

import javafx.application.Platform;
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

        TreeItem<RecordTree> humanRoot = new TreeItem<>(new PlayerTypeTree(false));
        TreeItem<RecordTree> aiRoot = new TreeItem<>(new PlayerTypeTree(true));
        root.getChildren().add(humanRoot);
        root.getChildren().add(aiRoot);

        List<String> names = db.listAllPlayerNames();
        for (String name : names) {
            PlayerAi paiHuman = new PlayerAi(name, false);
            PlayerAi paiAi = new PlayerAi(name, true);
            humanRoot.getChildren().add(new PersonTreeItem(paiHuman));
            aiRoot.getChildren().add(new PersonTreeItem(paiAi));
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
        private final PlayerAi playerAi;
        private boolean firstTimeChildren = true;

        PersonTreeItem(PlayerAi playerAi) {
            this.playerAi = playerAi;

            setValue(new RecordTree(this.playerAi.toString()));
        }

        private static ObservableList<TreeItem<RecordTree>> buildChildren(PlayerAi playerAi) {
            ObservableList<TreeItem<RecordTree>> children = FXCollections.observableArrayList();
            for (GameType gameType : GameType.values()) {
                TreeItem<RecordTree> typeItem = new TreeItem<>(new GameTypeTree(playerAi, gameType));
                typeItem.getChildren().add(new RecordSorting(gameType, playerAi, "time"));
                typeItem.getChildren().add(new RecordSorting(gameType, playerAi, "opponent"));
                children.add(typeItem);
            }
            return children;
        }

        @Override
        public ObservableList<TreeItem<RecordTree>> getChildren() {
            if (firstTimeChildren) {
                firstTimeChildren = false;
                super.getChildren().setAll(buildChildren(playerAi));
            }
            return super.getChildren();
        }

        @Override
        public boolean isLeaf() {
            return false;
        }

        @Override
        public String toString() {
            return playerAi.toString();
        }
    }

    public static class GameTypeTree extends RecordTree {
        private final PlayerAi pai;
        private final GameType gameType;

        GameTypeTree(PlayerAi pai, GameType gameType) {
            super(GameType.toReadable(gameType));
            this.pai = pai;
            this.gameType = gameType;
        }

        @Override
        void setRightPane(Pane rightPane) {
            rightPane.getChildren().clear();

            final GridPane resultPane = new GridPane();
            resultPane.setVgap(10.0);
            resultPane.setHgap(20.0);
            resultPane.setAlignment(Pos.CENTER);

            DBAccess db = DBAccess.getInstance();
            int[] potRecords = db.getBasicPotStatusAll(gameType, pai.playerName, pai.isAi);

            int rowIndex = 0;

            int potAttempts = potRecords[0];
            int potSuccesses = potRecords[1];
            resultPane.add(new Label("进攻次数"), 0, rowIndex);
            resultPane.add(new Label(String.valueOf(potAttempts)), 1, rowIndex++);
            resultPane.add(new Label("进攻成功次数"), 0, rowIndex);
            resultPane.add(new Label(String.valueOf(potSuccesses)), 1, rowIndex);
//            resultPane.add(new Label("进攻成功率"), 0, rowIndex);
            resultPane.add(new Label(
                    potAttempts == 0 ? "0%" :
                            String.format("%.1f%%", potSuccesses * 100.0 / potAttempts)), 2, rowIndex++);

            int longPotAttempts = potRecords[2];
            int longPotSuccesses = potRecords[3];
            resultPane.add(new Label("长台进攻次数"), 0, rowIndex);
            resultPane.add(new Label(String.valueOf(longPotAttempts)), 1, rowIndex++);
            resultPane.add(new Label("长台进攻成功次数"), 0, rowIndex);
            resultPane.add(new Label(String.valueOf(longPotSuccesses)), 1, rowIndex);
//            resultPane.add(new Label("长台进攻成功率"), 0, rowIndex);
            resultPane.add(new Label(
                    longPotAttempts == 0 ? "0%" :
                            String.format("%.1f%%",
                                    longPotSuccesses * 100.0 / longPotAttempts)), 2, rowIndex++);

            int defAttempts = potRecords[4];
            int defSuccesses = potRecords[5];
            resultPane.add(new Label("防守次数"), 0, rowIndex);
            resultPane.add(new Label(String.valueOf(defAttempts)), 1, rowIndex++);
            resultPane.add(new Label("防守成功次数"), 0, rowIndex);
            resultPane.add(new Label(String.valueOf(defSuccesses)), 1, rowIndex);
//            resultPane.add(new Label("防守成功率"), 0, rowIndex);
            resultPane.add(new Label(
                    defAttempts == 0 ? "0%" :
                            String.format("%.1f%%",
                                    defSuccesses * 100.0 / defAttempts)), 2, rowIndex++);

            if (gameType.snookerLike) {
                int[] breaksScores = db.getSnookerBreaksTotal(gameType, pai.playerName, pai.isAi);
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
                int[] breaksScores = db.getNumberedBallGamesTotal(gameType, pai.playerName, pai.isAi);
                resultPane.add(new Label("开球次数"), 0, rowIndex);
                resultPane.add(new Label(String.valueOf(breaksScores[0])), 1, rowIndex++);
                resultPane.add(new Label("开球进球次数"), 0, rowIndex);
                resultPane.add(new Label(String.valueOf(breaksScores[1])), 1, rowIndex++);
                resultPane.add(new Label("开球成功率"), 0, rowIndex);
                resultPane.add(new Label(String.format("%.1f%%",
                        breaksScores[1] * 100.0 / breaksScores[0])), 
                        1, rowIndex++);
                resultPane.add(new Label("炸清"), 0, rowIndex);
                resultPane.add(new Label(String.valueOf(breaksScores[2])), 1, rowIndex++);
                resultPane.add(new Label("接清"), 0, rowIndex);
                resultPane.add(new Label(String.valueOf(breaksScores[3])), 1, rowIndex++);
                resultPane.add(new Label("最高单杆球数"), 0, rowIndex);
                resultPane.add(new Label(String.valueOf(breaksScores[4])), 1, rowIndex++);
            }
            resultPane.add(new Separator(), 0, rowIndex++, 3, 1);
            final Button gameStatsButton = new Button("对局统计");
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
                    DBAccess.getInstance().getAllPveMatches(gameType, pai.playerName, pai.isAi);
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
            for (EntireGameRecord egr : entireRecords) {
                boolean thisIsP1 = egr.getTitle().player1Name.equals(pai.playerName);
                int[] playerWinsInThisMatchSize =
                        playerWinsByTotalFrames.computeIfAbsent(
                                egr.getTitle().totalFrames, k -> new int[2]);
                playerWinsInThisMatchSize[1]++;
                int[] p1p2wins = egr.getP1P2WinsCount();
                totalFrames += egr.getTitle().totalFrames;
                if (thisIsP1) {
                    if (p1p2wins[0] > p1p2wins[1]) {
                        thisWinMatches++;
                        playerWinsInThisMatchSize[0]++;
                    }
                    thisWinFrames += p1p2wins[0];
                } else {
                    if (p1p2wins[1] > p1p2wins[0]) {
                        thisWinMatches++;
                        playerWinsInThisMatchSize[0]++;
                    }
                    thisWinFrames += p1p2wins[1];
                }
            }

            final int thisWinFramesFinal = thisWinFrames;
            final int totalFramesFinal = totalFrames;
            final int thisWinMatchesFinal = thisWinMatches;

            System.out.println("db time: " + (System.currentTimeMillis() - st));

            Platform.runLater(() -> {
                indicator.setManaged(false);
                indicator.setVisible(false);
                button.setManaged(false);
                button.setVisible(false);

                int rowIndex = startRowIndex;

                resultPane.add(new Label("总场次"), 0, rowIndex);
                resultPane.add(new Label(String.valueOf(allMatches.size())), 1, rowIndex++);

                resultPane.add(new Label("总胜场"), 0, rowIndex);
                resultPane.add(new Label(String.valueOf(thisWinMatchesFinal)), 1, rowIndex);

                resultPane.add(new Label(
                                String.format("%.1f%%",
                                        (double) thisWinMatchesFinal / allMatches.size() * 100)),
                        2, rowIndex++);

                resultPane.add(new Label("总局数"), 0, rowIndex);
                resultPane.add(new Label(String.valueOf(totalFramesFinal)), 1, rowIndex++);

                resultPane.add(new Label("总胜局数"), 0, rowIndex);
                resultPane.add(new Label(String.valueOf(thisWinFramesFinal)), 1, rowIndex);

                resultPane.add(new Label(
                                String.format("%.1f%%",
                                        (double) thisWinFramesFinal / totalFramesFinal * 100)),
                        2, rowIndex++);

                resultPane.add(new Separator(), 0, rowIndex++, 3, 1);

                // 每种局长的胜率
                for (Map.Entry<Integer, int[]> entry : playerWinsByTotalFrames.entrySet()) {
                    int pWins = entry.getValue()[0];
                    int total = entry.getValue()[1];
                    double pRate = (double) pWins / total * 100;
                    resultPane.add(
                            new Label(String.format("%d局%d胜制",
                                    entry.getKey(), entry.getKey() / 2 + 1)),
                            0, rowIndex);
                    resultPane.add(new Label(String.format("%d场%d胜", total, pWins)), 1, rowIndex);
                    resultPane.add(new Label(String.format("%.1f%%", pRate)), 2, rowIndex);
                    rowIndex++;
                }
            });
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

    public static class PlayerTypeTree extends RecordTree {

        final boolean isAi;
        boolean expand = false;

        PlayerTypeTree(boolean isAi) {
            super(isAi ? "电脑" : "玩家");

            this.isAi = isAi;
        }

        @Override
        void setRightPane(Pane rightPane) {
            rightPane.getChildren().clear();
            if (!expand) {
                VBox vBox = new VBox();
                Button button = new Button("查询记录");
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

                appendToPane(GameType.SNOOKER, gridPane);
                appendToPane(GameType.MINI_SNOOKER, gridPane);
                appendToPane(GameType.CHINESE_EIGHT, gridPane);
                appendToPane(GameType.SIDE_POCKET, gridPane);

                rightPane.getChildren().add(gridPane);
            }
        }

        private void appendToPane(GameType gameType, GridPane gridPane) {
            List<EntireGameTitle> egtList = DBAccess.getInstance().getAllPveMatches(gameType);
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
            gridPane.add(new Label(GameType.toReadable(gameType)), 2, rowIndex++);

            String thisShown = isAi ? "电脑" : "玩家";
            String oppoShown = isAi ? "玩家" : "电脑";

            gridPane.add(new Label(thisShown), 0, rowIndex);
            gridPane.add(new Label(oppoShown), 4, rowIndex);
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
            gridPane.add(new Separator(), 0, rowIndex, 5, 1);
        }
    }

    public static class RecordSorting extends TreeItem<RecordTree> {
        private final String shown;
        private final PlayerAi playerAi;
        private final GameType gameType;
        private boolean firstTimeChildren = true;

        RecordSorting(GameType gameType, PlayerAi playerAi, String shown) {
            this.shown = shown;
            this.playerAi = playerAi;
            this.gameType = gameType;

            setValue(new RecordTree(getShowingStr()));
        }

        private static ObservableList<TreeItem<RecordTree>> buildChildren(
                GameType gameType, PlayerAi playerAi, String type) {
            ObservableList<TreeItem<RecordTree>> children = FXCollections.observableArrayList();
            DBAccess dbAccess = DBAccess.getInstance();
            List<EntireGameTitle> gameRecords = dbAccess.getAllPveMatches(gameType, playerAi.playerName, playerAi.isAi);
            if ("time".equals(type)) {
                // 按照比赛
                for (EntireGameTitle egt : gameRecords) {
                    children.add(new TreeItem<>(new MatchRecord(egt)));
                }
            } else {
                // 按照对手
                Map<PlayerAi, List<EntireGameTitle>> opponentMap = new HashMap<>();
                for (EntireGameTitle egt : gameRecords) {
                    boolean oppoIsP2 = egt.player1Name.equals(playerAi.playerName);
                    String oppoName = oppoIsP2 ?
                            egt.player2Name : egt.player1Name;
                    boolean oppoIsAi = oppoIsP2 ?
                            egt.player2isAi : egt.player1isAi;

                    PlayerAi oppo = new PlayerAi(oppoName, oppoIsAi);
                    List<EntireGameTitle> list =
                            opponentMap.get(oppo);
                    if (list == null) {
                        list = new ArrayList<>();
                        opponentMap.put(oppo, list);
                        children.add(new TreeItem<>(
                                new OpponentRecord(playerAi, oppo, list)));
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
                super.getChildren().setAll(buildChildren(gameType, playerAi, shown));
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
        final PlayerAi thisPlayer;
        final List<EntireGameTitle> egtList;

        OpponentRecord(PlayerAi thisPlayer, PlayerAi opponent, List<EntireGameTitle> egtList) {
            super(opponent.toString());
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
                boolean thisIsP1 = egr.getTitle().player1Name.equals(thisPlayer.playerName);
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
            gridPane.add(new Label(thisPlayer.playerName), 0, rowIndex);
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

            String p1Ai = egt.player1isAi ? "电脑" : "玩家";
            String p2Ai = egt.player2isAi ? "电脑" : "玩家";
            page.add(new Label(egt.player1Name + "\n" + p1Ai), 1, rowIndex);
            page.add(new Label(String.valueOf(p1p2Wins[0])), 2, rowIndex);
            page.add(new Label(String.format("(%d)", egt.totalFrames)), 3, rowIndex);
            page.add(new Label(String.valueOf(p1p2Wins[1])), 4, rowIndex);
            page.add(new Label(egt.player2Name + "\n" + p2Ai), 5, rowIndex);
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
                page.add(new Label("开球次数"), 0, rowIndex);
                page.add(new Label(String.valueOf(numberedBreaks[0][0])), 1, rowIndex);
                page.add(new Label(String.valueOf(numberedBreaks[1][0])), 5, rowIndex);
                rowIndex++;

                page.add(new Label("开球进球次数"), 0, rowIndex);
                page.add(new Label(String.valueOf(numberedBreaks[0][1])), 1, rowIndex);
                page.add(new Label(String.valueOf(numberedBreaks[1][1])), 5, rowIndex);
                rowIndex++;

                page.add(new Label("开球成功率"), 0, rowIndex);
                page.add(new Label(String.format("%.1f%%", 
                                numberedBreaks[0][1] * 100.0 / numberedBreaks[0][0])), 
                        1, rowIndex);
                page.add(new Label(String.format("%.1f%%",
                                numberedBreaks[1][1] * 100.0 / numberedBreaks[1][0])),
                        5, rowIndex);
                rowIndex++;
                
                page.add(new Label("炸清"), 0, rowIndex);
                page.add(new Label(String.valueOf(numberedBreaks[0][2])), 1, rowIndex);
                page.add(new Label(String.valueOf(numberedBreaks[1][2])), 5, rowIndex);
                rowIndex++;

                page.add(new Label("接清"), 0, rowIndex);
                page.add(new Label(String.valueOf(numberedBreaks[0][3])), 1, rowIndex);
                page.add(new Label(String.valueOf(numberedBreaks[1][3])), 5, rowIndex);
                rowIndex++;

                page.add(new Label("单杆最高球数"), 0, rowIndex);
                page.add(new Label(String.valueOf(numberedBreaks[0][4])), 1, rowIndex);
                page.add(new Label(String.valueOf(numberedBreaks[1][4])), 5, rowIndex);
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

    public static class PlayerAi {
        final String playerName;
        final boolean isAi;
        final String shown;

        PlayerAi(String playerName, boolean isAi) {
            this.playerName = playerName;
            this.isAi = isAi;
            this.shown = playerName + (isAi ? "(电脑)" : "(玩家)");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PlayerAi playerAi = (PlayerAi) o;
            return isAi == playerAi.isAi && Objects.equals(playerName, playerAi.playerName);
        }

        @Override
        public int hashCode() {
            return Objects.hash(playerName, isAi);
        }

        @Override
        public String toString() {
            return shown;
        }
    }
}
