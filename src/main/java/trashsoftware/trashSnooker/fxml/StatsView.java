package trashsoftware.trashSnooker.fxml;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TableView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import trashsoftware.trashSnooker.core.GameType;
import trashsoftware.trashSnooker.util.PersonRecord;

import java.io.File;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

public class StatsView implements Initializable {
    @FXML
    TreeView<RecordTree> treeView;

    @FXML
    TableView<RecordItem> tableView;

    @FXML
    GridPane resultPane;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initTree();
    }

    private void initTree() {
        TreeItem<RecordTree> root = new TreeItem<>(new RecordTree("记录"));
        File[] files = PersonRecord.listRecordFiles();
        for (File file : files) {
            root.getChildren().add(new PersonTreeItem(file));
        }
        treeView.setRoot(root);
        treeView.getSelectionModel().selectedItemProperty()
                .addListener(((observable, oldValue, newValue) -> {
                    if (newValue != null) {
                        RecordTree value = newValue.getValue();
                        if (value instanceof GameTypeTree) {
                            setRightBox((GameTypeTree) value);
                        }
                    }
                }));
    }

    private void setRightBox(GameTypeTree gameTypeTree) {
        resultPane.getChildren().clear();
        Map<String, Integer> intMap =
                gameTypeTree.personRecord.getIntRecords().get(gameTypeTree.gameType);
        int potAttempts = intMap.get("potAttempts");
        int potSuccesses = intMap.get("potSuccesses");
        resultPane.add(new Label("进攻次数"), 0, 0);
        resultPane.add(new Label(String.valueOf(potAttempts)), 1, 0);
        resultPane.add(new Label("进攻成功次数"), 0, 1);
        resultPane.add(new Label(String.valueOf(potSuccesses)), 1, 1);
        resultPane.add(new Label("进攻成功率"), 0, 2);
        resultPane.add(new Label(String.format("%.1f%%", potSuccesses * 100.0 / potAttempts)), 1, 2);

        int longPotAttempts = intMap.get("longPotAttempts");
        int longPotSuccesses = intMap.get("longPotSuccesses");
        resultPane.add(new Label("长台进攻次数"), 0, 3);
        resultPane.add(new Label(String.valueOf(longPotAttempts)), 1, 3);
        resultPane.add(new Label("长台进攻成功次数"), 0, 4);
        resultPane.add(new Label(String.valueOf(longPotSuccesses)), 1, 4);
        resultPane.add(new Label("长台进攻成功率"), 0, 5);
        resultPane.add(new Label(String.format("%.1f%%", longPotSuccesses * 100.0 / longPotAttempts)), 1, 5);
    }

    public static class PersonTreeItem extends TreeItem<RecordTree> {
        private final File file;
        private final String name;

        private boolean firstTimeChildren = true;

        PersonTreeItem(File file) {
            this.file = file;
            String name = file.getName();
            this.name = name.substring(0, name.lastIndexOf('.'));
            setValue(new RecordTree(this.name));
        }

        @Override
        public ObservableList<TreeItem<RecordTree>> getChildren() {
//            if (children == null) {
//                children = buildChildren(name);
//            }
//            return children;

            if (firstTimeChildren) {
                firstTimeChildren = false;
                super.getChildren().setAll(buildChildren(name));
            }
            return super.getChildren();
        }

        private static ObservableList<TreeItem<RecordTree>> buildChildren(String name) {
            ObservableList<TreeItem<RecordTree>> children = FXCollections.observableArrayList();
            PersonRecord personRecord = PersonRecord.loadRecord(name);
            Set<GameType> gameTypeSet = personRecord.getIntRecords().keySet();
            for (GameType gameType : gameTypeSet) {
                children.add(new TreeItem<>(new GameTypeTree(personRecord, gameType)));
            }
            return children;
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
        private final PersonRecord personRecord;
        private final GameType gameType;

        GameTypeTree(PersonRecord personRecord, GameType gameType) {
            super(getString(gameType));
            this.personRecord = personRecord;
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
    }

    public static class RecordTree {
        protected final String shown;

        RecordTree(String shown) {
            this.shown = shown;
        }

        @Override
        public String toString() {
            return shown;
        }
    }

    public static class PersonRecordTree extends RecordTree {
        private final String name;

        PersonRecordTree(String name) {
            super(name);
            this.name = name;
        }
    }

    public static class RecordItem {

    }
}
