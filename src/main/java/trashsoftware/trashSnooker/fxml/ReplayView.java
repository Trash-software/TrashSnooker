package trashsoftware.trashSnooker.fxml;

import javafx.application.Platform;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ObservableList;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.jetbrains.annotations.NotNull;
import trashsoftware.trashSnooker.core.career.championship.MetaMatchInfo;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.recorder.*;
import trashsoftware.trashSnooker.res.ResourcesLoader;
import trashsoftware.trashSnooker.util.EventLogger;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Comparator;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.TreeMap;

public class ReplayView extends ChildInitializable {

    private final Map<Long, TreeItem<Item>> entireGameItems = new TreeMap<>();
    @FXML
    Button playBtn, exportBtn;
    @FXML
    TreeTableView<Item> replayTable;
    @FXML
    TreeTableColumn<Item, String> replayCol, eventCol, typeCol, p1Col, p2Col, beginTimeCol, durationCol,
            nCuesCol, resultCol;
    TreeItem<Item> root;

    private Stage stage;
    private ResourceBundle strings;

    private boolean interrupted;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.strings = resourceBundle;

        root = new TreeItem<>(new RootItem());
        replayTable.setRoot(root);

        replayCol.setCellValueFactory(p ->
                new ReadOnlyStringWrapper(p.getValue().getValue().title()));
        eventCol.setCellValueFactory(p ->
                new ReadOnlyStringWrapper(p.getValue().getValue().eventString()));
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

        for (TreeTableColumn<Item, ?> col : replayTable.getColumns()) {
            col.setSortable(false);
        }

        clickListener();
        selectionListener();
//        naiveFill();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    private void selectionListener() {
        replayTable.getSelectionModel().selectedItemProperty().addListener((observableValue, itemTreeItem, t1) -> {
            boolean disable = !(t1.getValue() instanceof FrameItem);
            playBtn.setDisable(disable);
            exportBtn.setDisable(disable);
        });
    }

    private void clickListener() {
        replayTable.setRowFactory(briefReplayItemTableView -> {
            TreeTableRow<Item> row = new TreeTableRow<>();
            row.setOnMouseClicked(mouseEvent -> {
                if (mouseEvent.getButton() == MouseButton.PRIMARY && mouseEvent.getClickCount() == 2) {
                    playReplay(getReplay(row.getItem()));
                }
            });
            return row;
        });
    }

    @FXML
    void playAction() {
        Item selected = replayTable.getSelectionModel().getSelectedItem().getValue();
        if (selected != null) {
            playReplay(getReplay(selected));
        }
    }

    @FXML
    void exportVideoAction() throws IOException {
        Item selected = replayTable.getSelectionModel().getSelectedItem().getValue();

        GameReplay replay = getReplay(selected);
        if (replay != null) {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("videoExportView.fxml"),
                    strings
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            Stage stage = new Stage();
            stage.getIcons().add(ResourcesLoader.getInstance().getIcon());
            stage.initOwner(this.stage);
            stage.initModality(Modality.WINDOW_MODAL);

            Scene scene = App.createScene(root);
            stage.setScene(scene);

            VideoExportView view = loader.getController();
            view.setup(replay, stage);

            stage.show();
        }
    }

    @Override
    public void backAction() {
        interrupted = true;

        super.backAction();
    }

    private void playReplay(GameReplay replay) {
        if (replay != null) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("gameView.fxml"),
                        strings
                );
                Parent root = loader.load();
                root.setStyle(App.FONT_STYLE);

                Stage stage = new Stage();
                stage.initOwner(this.stage);
                stage.initModality(Modality.WINDOW_MODAL);

                Scene scene = App.createScene(root);
                stage.setScene(scene);

                GameView gameView = loader.getController();
                gameView.setupReplay(stage, replay);
                gameView.startAnimation();

                stage.show();

                App.scaleGameStage(stage, gameView);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private GameReplay getReplay(Item item) {
        if (item instanceof FrameItem) {
            try {
                return GameReplay.loadReplay(item.getValue());
            } catch (VersionException ve) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setHeaderText("不能播放旧版本的录像");
                alert.setContentText(String.format(
                        "当前游戏版本: %d.%d, 录像版本: %d.%d",
                        ActualRecorder.RECORD_PRIMARY_VERSION,
                        ActualRecorder.RECORD_SECONDARY_VERSION,
                        item.getValue().primaryVersion,
                        item.getValue().secondaryVersion));
                alert.show();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return null;
    }

//    private void playReplay(Item item) {
//        GameReplay replay = getReplay(item);
//        if (replay != null) {
//            try {
//                // fixme: 这真是个无法理解的神奇bug
//                // fixme: stage和replay就是非要都第二次加载时才显球，哪个少加载一次都不行
//                // fixed: 莫名其妙修好了，可能是BallModel的问题
//                playReplay(replay);
//            } catch (IOException e) {  // 这里已经不可能version exception了
//                throw new RuntimeException(e);
//            }
//        }
//    }

//    public void fill() {
//        root.getChildren().clear();
//
//        long begin = System.currentTimeMillis();
//        FillReplayService service = new FillReplayService();
//
//        service.setOnSucceeded(e -> {
//            System.out.println("Records listed in " + (System.currentTimeMillis() - begin) + " ms");
//        });
//        service.setOnFailed(e -> {
//            e.getSource().getException().printStackTrace();
//        });
//
//        service.start();
//    }

    public void naiveFill() {
        root.getChildren().clear();
        Thread thread = new Thread(() -> {
            long begin = System.currentTimeMillis();
            File[] replays = GameReplay.listReplays();
            if (replays != null) {
                for (int i = replays.length - 1; i >= 0; i--) {
                    if (interrupted) return;
                    File f = replays[i];
                    if (!f.getName().endsWith(".replay")) continue;
                    try {
                        BriefReplayItem item = new BriefReplayItem(f);

                        Platform.runLater(() -> {
                            long time = item.gameBeginTime;
                            TreeItem<Item> gameItemWrapper = entireGameItems.get(time);
                            if (gameItemWrapper == null) {
                                gameItemWrapper = new TreeItem<>(new MatchItem(time, item));
                                ((MatchItem) gameItemWrapper.getValue()).setChildrenList(gameItemWrapper.getChildren());

                                entireGameItems.put(time, gameItemWrapper);
                                root.getChildren().add(gameItemWrapper);
                                root.getChildren().sort(Comparator.comparing(TreeItem::getValue));

                                if (root.getChildren().size() == 1) {
                                    root.setExpanded(true);
                                }
                            }
                            gameItemWrapper.getChildren().add(new TreeItem<>(new FrameItem(item)));
                            gameItemWrapper.getChildren().sort(Comparator.comparing(TreeItem::getValue));
                            replayTable.refresh();  // 这里是为了让match的比分刷新
                        });

                        Thread.sleep(1);  // 猜猜这是为啥
                    } catch (VersionException ve) {
                        System.err.printf("Record version: %d.%d\n",
                                ve.recordPrimaryVersion, ve.recordSecondaryVersion);
                    } catch (ReplayException | IOException re) {
                        System.err.println("replay error: " + re.getClass() + ": " + re.getMessage());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    } catch (Exception e) {
                        EventLogger.error(e);
                    }
                }
            }
            System.out.println("Records listed in " + (System.currentTimeMillis() - begin) + " ms");
        });
        thread.setDaemon(true);
        thread.start();
    }

//    @Override
//    public Stage getStage() {
//        return stage;
//    }

    public abstract static class Item implements Comparable<Item> {
        Item() {
        }

        public String title() {
            return "";
        }

        public BriefReplayItem getValue() {
            return null;
        }

        public String eventString() {
            return "";
        }

        public String typeString() {
            String res;
            if (getValue() == null) res = "";
//            else res = GameRule.toReadable(getValue().gameValues.rule);
            else res = getValue().getGameTypeName();
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

        @Override
        public int compareTo(@NotNull ReplayView.Item o) {
            return 0;
        }
    }

    public static class MatchItem extends Item {
        final long gameBeginTime;
        final BriefReplayItem value;
        ObservableList<TreeItem<Item>> childrenList;

        MatchItem(long gameBeginTime,
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
        public String eventString() {
            MetaMatchInfo metaMatchInfo = value.getMetaMatchInfo();
            if (metaMatchInfo != null) {
                return metaMatchInfo.normalReadable();
            }
            return "";
        }

        @Override
        public int compareTo(@NotNull ReplayView.Item o) {
            if (o instanceof MatchItem) {
                return -Long.compare(this.gameBeginTime, ((MatchItem) o).gameBeginTime);
            } else return 0;
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
            int p1w = 0;
            int p2w = 0;
            for (TreeItem<Item> item : childrenList) {
                FrameItem frameItem = (FrameItem) item.getValue();
                if (frameItem.getValue().frameWinnerNumber == 1) p1w++;
                else p2w++;
            }
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
        public int compareTo(@NotNull ReplayView.Item o) {
            if (o instanceof FrameItem) {
                return -Long.compare(this.value.frameBeginTime, ((FrameItem) o).value.frameBeginTime);
            } else return 0;
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
            return String.valueOf(getValue().getNCues());
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

//    private class FillReplayService extends Service<Void> {
//
//        @Override
//        protected Task<Void> createTask() {
//            return new Task<>() {
//                @Override
//                protected Void call() {
//                    File[] replays = GameReplay.listReplays();
//                    if (replays != null) {
//                        for (int i = replays.length - 1; i >= 0; i--) {
//                            File f = replays[i];
//                            try {
//                                BriefReplayItem item = new BriefReplayItem(f);
//
//                                Platform.runLater(() -> {
//                                    long time = item.gameBeginTime;
//                                    TreeItem<Item> gameItemWrapper = entireGameItems.get(time);
//                                    if (gameItemWrapper == null) {
//                                        gameItemWrapper = new TreeItem<>(new MatchItem(time, item));
//                                        ((MatchItem) gameItemWrapper.getValue()).setChildrenList(gameItemWrapper.getChildren());
//
//                                        entireGameItems.put(time, gameItemWrapper);
//                                        root.getChildren().add(gameItemWrapper);
//                                        root.getChildren().sort(Comparator.comparing(TreeItem::getValue));
//
//                                        if (root.getChildren().size() == 1) {
//                                            root.setExpanded(true);
//                                        }
//                                    }
//                                    gameItemWrapper.getChildren().add(new TreeItem<>(new FrameItem(item)));
//                                    gameItemWrapper.getChildren().sort(Comparator.comparing(TreeItem::getValue));
//                                    replayTable.refresh();  // 这里是为了让match的比分刷新
//                                });
//
//
////                                replayList.add(item);
//                            } catch (VersionException ve) {
//                                System.err.printf("Record version: %d.%d\n",
//                                        ve.recordPrimaryVersion, ve.recordSecondaryVersion);
//                            } catch (ReplayException | IOException re) {
//                                System.err.println("replay error: " + re.getClass() + ": " + re.getMessage());
//                            }
//                        }
//                    }
//                    return null;
//                }
//            };
//        }
//    }
}
