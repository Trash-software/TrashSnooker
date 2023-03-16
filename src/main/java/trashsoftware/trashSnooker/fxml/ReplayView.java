package trashsoftware.trashSnooker.fxml;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.stage.Modality;
import javafx.stage.Stage;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.recorder.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;

public class ReplayView implements Initializable {

    private final Map<Long, TreeItem<Item>> entireGameItems = new TreeMap<>();
    private final List<BriefReplayItem> replayList = new ArrayList<>();
    @FXML
    TreeTableView<Item> replayTable;
    @FXML
    TreeTableColumn<Item, String> replayCol, typeCol, p1Col, p2Col, beginTimeCol, durationCol,
            nCuesCol, resultCol;
    TreeItem<Item> root;

    private Stage stage;
    private ResourceBundle strings;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.strings = resourceBundle;

        root = new TreeItem<>(new RootItem());
        replayTable.setRoot(root);

        replayCol.setCellValueFactory(p ->
                new ReadOnlyStringWrapper(p.getValue().getValue().title()));
        typeCol.setCellValueFactory(p ->
                new ReadOnlyStringWrapper(p.getValue().getValue().typeString()));
        p1Col.setCellValueFactory(p ->
                new ReadOnlyStringWrapper(p.getValue().getValue().p1Name()));
        p2Col.setCellValueFactory(p ->
                new ReadOnlyStringWrapper(p.getValue().getValue().p2Name()));
        beginTimeCol.setCellValueFactory(p ->
                new ReadOnlyStringWrapper(p.getValue().getValue().beginTime()));
        durationCol.setCellValueFactory(p ->
                new ReadOnlyStringWrapper(p.getValue().getValue().duration()));
        nCuesCol.setCellValueFactory(p ->
                new ReadOnlyStringWrapper(p.getValue().getValue().nCues()));
        resultCol.setCellValueFactory(p ->
                new ReadOnlyStringWrapper(p.getValue().getValue().result()));

        clickListener();
        fill();
//        naiveFill();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void clickListener() {
        replayTable.setRowFactory(briefReplayItemTableView -> {
            TreeTableRow<Item> row = new TreeTableRow<>();
            row.setOnMouseClicked(mouseEvent -> {
                if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
                    playReplay(row.getItem(), 1);
                }
            });
            return row;
        });
    }

    @FXML
    void playAction() {
        Item selected = replayTable.getSelectionModel().getSelectedItem().getValue();
        if (selected != null) {
            playReplay(selected, 1);
        }
    }
    
    private Stage playReplay(GameReplay replay) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("gameView.fxml"),
                strings
        );
        Parent root = loader.load();
        root.setStyle(App.FONT_STYLE);

        Stage stage = new Stage();
        stage.initOwner(this.stage);
        stage.initModality(Modality.WINDOW_MODAL);

        Scene scene = new Scene(root);
        stage.setScene(scene);

        GameView gameView = loader.getController();
        gameView.setupReplay(stage, replay);

        stage.show();
        return stage;
    }

    private void playReplay(Item item, int count) {
        if (item instanceof FrameItem) {
            try {
                // fixme: 这真是个无法理解的神奇bug
                // fixme: stage和replay就是非要都第二次加载时才显球，哪个少加载一次都不行
                GameReplay replay = GameReplay.loadReplay(item.getValue());

                Stage stage = playReplay(replay);
                while (count > 0) {
                    stage.close();
                    replay = GameReplay.loadReplay(item.getValue());
                    stage = playReplay(replay);
                    count--;
                }
            } catch (VersionException ve) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("不能播放旧版本的录像");
                alert.setContentText(String.format(
                        "当前游戏版本: %d.%d, 录像版本: %d.%d",
                        GameRecorder.RECORD_PRIMARY_VERSION,
                        GameRecorder.RECORD_SECONDARY_VERSION,
                        item.getValue().primaryVersion,
                        item.getValue().secondaryVersion));
                alert.show();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void fill() {
        replayList.clear();
        FillReplayService service = new FillReplayService();

        service.setOnSucceeded(e -> {
            for (BriefReplayItem item : replayList) {
                long time = item.gameBeginTime;
                TreeItem<Item> gameItemWrapper = entireGameItems.get(time);
                if (gameItemWrapper == null) {
                    gameItemWrapper = new TreeItem<>(new GameItem(time, item));
                    ((GameItem) gameItemWrapper.getValue()).setChildrenList(gameItemWrapper.getChildren());
                    entireGameItems.put(time, gameItemWrapper);
                    root.getChildren().add(gameItemWrapper);
                }
                gameItemWrapper.getChildren().add(new TreeItem<>(new FrameItem(item)));
            }
            root.setExpanded(true);
        });
        service.setOnFailed(e -> {
            e.getSource().getException().printStackTrace();
        });

        service.start();
    }

    public abstract static class Item {
        Item() {
        }

        public String title() {
            return "";
        }

        public BriefReplayItem getValue() {
            return null;
        }

        public String typeString() {
            String res;
            if (getValue() == null) res = "";
            else res = GameRule.toReadable(getValue().gameValues.rule);
            return res;
        }

        public String beginTime() {
            return "";
        }

        public String duration() {
            return "";
        }

        public String p1Name() {
            return "";
        }

        public String p2Name() {
            return "";
        }

        public String nCues() {
            return "";
        }

        public String result() {
            return "";
        }
    }

    public static class RootItem extends Item {
        RootItem() {
            super();
        }

        @Override
        public String title() {
            return "回放";
        }
    }

    public static class GameItem extends Item {
        final long gameBeginTime;
        final BriefReplayItem value;
        ObservableList<TreeItem<Item>> childrenList;

        GameItem(long gameBeginTime,
                 BriefReplayItem value  // value.value这一场中的某一局，一般是第一局
        ) {
            super();
            this.gameBeginTime = gameBeginTime;
            this.value = value;
        }

        public void setChildrenList(ObservableList<TreeItem<Item>> childrenList) {
            this.childrenList = childrenList;
        }

        public BriefReplayItem getValue() {
            return value;
        }

        @Override
        public String title() {
            return String.format("%d局%d胜比赛",
                    value.totalFrames, value.totalFrames / 2 + 1);
        }

        @Override
        public String beginTime() {
            return getValue().getGameBeginTimeString();
        }

        @Override
        public String p1Name() {
            return getValue().getP1Name();
        }

        @Override
        public String p2Name() {
            return getValue().getP2Name();
        }

        @Override
        public String result() {
            FrameItem last = (FrameItem) childrenList.get(childrenList.size() - 1).getValue();
            BriefReplayItem brief = last.getValue();
            int p1w = brief.p1Wins;
            int p2w = brief.p2Wins;
            if (brief.frameWinnerNumber == 1) p1w++;
            else if (brief.frameWinnerNumber == 2) p2w++;
            return String.format("%s %d : %d %s",
                    p1Name(), p1w, p2w, p2Name());
        }

        @Override
        public String toString() {
            return "GameTreeItem{" +
                    "gameBeginTime=" + gameBeginTime +
                    '}';
        }
    }

    public static class FrameItem extends Item {
        final BriefReplayItem value;

        FrameItem(BriefReplayItem value) {
            super();

            this.value = value;
        }

        public BriefReplayItem getValue() {
            return value;
        }

        @Override
        public String title() {
            return String.format("第%d局", getValue().p1Wins + getValue().p2Wins + 1);
        }

        @Override
        public String beginTime() {
            return getValue().getFrameBeginTimeString();
        }

        @Override
        public String duration() {
            return getValue().getDuration();
        }

        @Override
        public String nCues() {
            return getValue().getNCues();
        }

        @Override
        public String result() {
            if (getValue().frameWinnerNumber == 0) return "平局或中止";
            return String.format("%s 胜",
                    (getValue().frameWinnerNumber == 1 ? getValue().getP1Name() : getValue().getP2Name()));
        }

        @Override
        public String toString() {
            return "FrameItem{" +
                    '}';
        }
    }

    private class FillReplayService extends Service<Void> {

        @Override
        protected Task<Void> createTask() {
            return new Task<>() {
                @Override
                protected Void call() throws Exception {
                    File[] replays = GameReplay.listReplays();
                    if (replays != null) {
                        for (int i = replays.length - 1; i >= 0; i--) {
                            File f = replays[i];
                            try {
                                BriefReplayItem item = new BriefReplayItem(f);
                                replayList.add(item);
                            } catch (VersionException ve) {
                                System.err.printf("Record version: %d.%d\n",
                                        ve.recordPrimaryVersion, ve.recordSecondaryVersion);
                            } catch (RecordException re) {
                                System.err.println(re.getMessage());
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    return null;
                }
            };
        }
    }
}
