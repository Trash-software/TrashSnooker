package trashsoftware.trashSnooker.fxml.statsViews;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.fxml.ChildInitializable;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.db.DBAccess;
import trashsoftware.trashSnooker.util.db.EntireGameTitle;

import java.net.URL;
import java.util.*;

public class StatsView extends ChildInitializable {
    @FXML
    TreeView<RecordTree> treeView;

    @FXML
    TableView<RecordItem> tableView;

    @FXML
    VBox rightPane;

    private ResourceBundle strings;
    private Stage stage;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.strings = resources;

        initTree();
    }

//    @Override
//    public Stage getStage() {
//        return stage;
//    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void initTree() {
        TreeItem<RecordTree> root = new TreeItem<>(new RecordTree(strings.getString("recordMenu"), strings));
        DBAccess db = DBAccess.getInstance();

        TreeItem<RecordTree> humanRoot = new TreeItem<>(new PlayerTypeTree(false, strings));
        TreeItem<RecordTree> aiRoot = new TreeItem<>(new PlayerTypeTree(true, strings));
        root.getChildren().add(humanRoot);
        root.getChildren().add(aiRoot);

        List[] humanComputerIds = db.listPlayerIdsHumanComputer();
        for (Object s : humanComputerIds[0]) {
            PlayerAi paiHuman = new PlayerAi((String) s, false);
            humanRoot.getChildren().add(new PersonTreeItem(paiHuman, strings));
        }
        for (Object s : humanComputerIds[1]) {
            PlayerAi paiAi = new PlayerAi((String) s, true);
            aiRoot.getChildren().add(new PersonTreeItem(paiAi, strings));
        }

        TreeItem<RecordTree> matchesRoot = new TreeItem<>(new RecordTree(strings.getString("statsMatches"), strings));

        for (GameRule gameRule : GameRule.values()) {
            List<EntireGameTitle> matchesOfType = db.getAllMatches(gameRule);
            TreeItem<RecordTree> typeRoot = new TreeItem<>(new AllMatchesTree(gameRule, strings));

            for (EntireGameTitle egt : matchesOfType) {
                MatchRecord mr = new MatchRecord(egt, strings);
                typeRoot.getChildren().add(new TreeItem<>(mr));
            }

            matchesRoot.getChildren().add(typeRoot);
        }
        root.getChildren().add(matchesRoot);

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
        private final ResourceBundle strings;
        private boolean firstTimeChildren = true;

        PersonTreeItem(PlayerAi playerAi, ResourceBundle strings) {
            this.playerAi = playerAi;
            this.strings = strings;

            setValue(new RecordTree(this.playerAi.toString(), strings));
        }

        private static ObservableList<TreeItem<RecordTree>> buildChildren(PlayerAi playerAi, ResourceBundle strings) {
            ObservableList<TreeItem<RecordTree>> children = FXCollections.observableArrayList();
            for (GameRule gameRule : GameRule.values()) {
                TreeItem<RecordTree> typeItem = new TreeItem<>(new GameTypeTree(playerAi, gameRule, strings));
                typeItem.getChildren().add(new RecordSorting(gameRule, playerAi, "time", strings));
                typeItem.getChildren().add(new RecordSorting(gameRule, playerAi, "opponent", strings));
                children.add(typeItem);
            }
            return children;
        }

        @Override
        public ObservableList<TreeItem<RecordTree>> getChildren() {
            if (firstTimeChildren) {
                firstTimeChildren = false;
                super.getChildren().setAll(buildChildren(playerAi, strings));
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

    public static class AllMatchesTree extends RecordTree {

        AllMatchesTree(GameRule gameRule, ResourceBundle strings) {
            super(GameRule.toReadable(gameRule), strings);
        }

        @Override
        void setRightPane(Pane rightPane) {
            rightPane.getChildren().clear();
        }
    }

    public static class RecordSorting extends TreeItem<RecordTree> {
        private final String shown;
        private final PlayerAi playerAi;
        private final GameRule gameRule;
        private final ResourceBundle strings;
        private boolean firstTimeChildren = true;

        RecordSorting(GameRule gameRule, PlayerAi playerAi, String shown, ResourceBundle strings) {
            this.strings = strings;
            this.shown = shown;
            this.playerAi = playerAi;
            this.gameRule = gameRule;

            setValue(new RecordTree(getShowingStr(), strings));
        }

        private static ObservableList<TreeItem<RecordTree>> buildChildren(
                GameRule gameRule, PlayerAi playerAi, String type, ResourceBundle strings) {
            ObservableList<TreeItem<RecordTree>> children = FXCollections.observableArrayList();
            DBAccess dbAccess = DBAccess.getInstance();
            List<EntireGameTitle> gameRecords = dbAccess.getAllPveMatches(gameRule, playerAi.playerId, playerAi.isAi);
            if ("time".equals(type)) {
                // 按照比赛
                for (EntireGameTitle egt : gameRecords) {
                    children.add(new TreeItem<>(new MatchRecord(egt, strings)));
                }
            } else {
                // 按照对手
                Map<PlayerAi, List<EntireGameTitle>> opponentMap = new HashMap<>();
                for (EntireGameTitle egt : gameRecords) {
                    boolean oppoIsP2 = egt.player1Id.equals(playerAi.playerId);
                    String oppoName = oppoIsP2 ?
                            egt.player2Id : egt.player1Id;
                    boolean oppoIsAi = oppoIsP2 ?
                            egt.player2isAi : egt.player1isAi;

                    PlayerAi oppo = new PlayerAi(oppoName, oppoIsAi);
                    List<EntireGameTitle> list =
                            opponentMap.get(oppo);
                    if (list == null) {
                        list = new ArrayList<>();
                        opponentMap.put(oppo, list);
                        children.add(new TreeItem<>(
                                new OpponentRecord(playerAi, oppo, list, strings)));
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
                super.getChildren().setAll(buildChildren(gameRule, playerAi, shown, strings));
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

    public static class PlayerAi {
        final String playerId;
        final boolean isAi;
        final String shown;

        PlayerAi(String playerId, boolean isAi) {
            this.playerId = playerId;
            this.isAi = isAi;
            this.shown = DataLoader.getInstance().getPlayerPerson(playerId).getName() + (isAi ? "(电脑)" : "(玩家)");
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            PlayerAi playerAi = (PlayerAi) o;
            return isAi == playerAi.isAi && Objects.equals(playerId, playerAi.playerId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(playerId, isAi);
        }

        @Override
        public String toString() {
            return shown;
        }
    }
}
