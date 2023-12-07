package trashsoftware.trashSnooker.fxml;

import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.SequentialTransition;
import javafx.animation.Timeline;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.HumanCareer;
import trashsoftware.trashSnooker.core.career.achievement.*;
import trashsoftware.trashSnooker.res.ResourcesLoader;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.Util;

import java.net.URL;
import java.util.*;

public class AchievementsView extends ChildInitializable {

    @FXML
    Label totalCountLabel;
    @FXML
    TableView<CatItem> achCatTable;
    @FXML
    TableView<AchItem> achTable;
    @FXML
    TableColumn<AchItem, VBox> textCol;
    @FXML
    TableColumn<AchItem, VBox> dateCol;
    @FXML
    TableColumn<AchItem, ImageView> iconCol;
    @FXML
    TableColumn<AchItem, VBox> awdCol;

    private final Map<AchCat, List<AchievementBundle>> completed = new HashMap<>();
    private final Map<AchCat, int[]> catCompletions = new HashMap<>();  // 值是[总数, 完成数]

    private Stage stage;
    private CareerView careerView;
    private ResourceBundle strings;
    private final ResourcesLoader resourcesLoader = ResourcesLoader.getInstance();
    private HumanCareer humanCareer;

    private transient final Deque<Integer> popupsToShow = new ArrayDeque<>();
    private boolean popupShowing = false;

    public void setup(Stage stage, CareerView careerView) {
        this.stage = stage;
        this.careerView = careerView;

        humanCareer = CareerManager.getInstance().getHumanPlayerCareer();

        fillCatTable();

        totalCountLabel.setText(String.format(strings.getString("totalCount"),
                AchManager.getInstance().getNCompletedAchievements()));
    }

//    @Override
//    public Stage getStage() {
//        return stage;
//    }

    @Override
    public void backAction() {
        careerView.refreshGui();

        super.backAction();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.strings = resourceBundle;

        setupTable();
    }

    private void setupTable() {
        TableColumn<CatItem, String> catNameCol = new TableColumn<>();
        catNameCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().cat.shown()));
        TableColumn<CatItem, String> catCompletionCol = new TableColumn<>();
        catCompletionCol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().completion()));

        achCatTable.getColumns().add(catNameCol);
        achCatTable.getColumns().add(catCompletionCol);
        achCatTable.getSelectionModel().selectedItemProperty().addListener(e -> refreshAchTable());

        Font titleFont = Font.font(App.FONT.getFamily(), FontWeight.BLACK, 16.0);
        textCol.setCellValueFactory(cell -> {
            AchievementBundle bundle = cell.getValue().bundle;

            VBox vBox = new VBox();
            Label bigLabel = new Label();
            bigLabel.setFont(titleFont);

            if (bundle.completion != null) {
                bigLabel.setText(bundle.completion.getTitle());
            } else {
                bigLabel.setText(bundle.achievement.title());
            }

            String des = bundle.completion == null ?
                    bundle.achievement.getDescriptionOfLevel(0) :
                    bundle.completion.getDescription();

            vBox.getChildren().addAll(bigLabel, new Label(des));

            return new ReadOnlyObjectWrapper<>(vBox);
        });

        dateCol.setCellValueFactory(cell -> {
            VBox vBox = new VBox();
            Label r1 = new Label(cell.getValue().firstRow());
            Label r2 = new Label(cell.getValue().secondRow());
            vBox.getChildren().addAll(r1, r2);

            return new ReadOnlyObjectWrapper<>(vBox);
        });

        iconCol.setCellValueFactory(cell -> {
            Image image = cell.getValue().getImage();
            if (image == null) return null;
            ImageView imageView = new ImageView(image);
            imageView.setFitWidth(32.0);
            imageView.setFitHeight(32.0);
            return new ReadOnlyObjectWrapper<>(imageView);
        });

//        awdCol.setCellFactory(new Callback<TableColumn<AchItem, VBox>, TableCell<AchItem, VBox>>() {
//            @Override
//            public TableCell<AchItem, VBox> call(TableColumn<AchItem, VBox> param) {
//                TableCell<AchItem, VBox> cell = new TableCell<>();
//                VBox vBox = param.getCellData()
//                if (vBox == null) return null;
//            }
//        });

        awdCol.setCellValueFactory(cell -> {
            VBox vBox = cell.getValue().getUnreceivedAwards();
            if (vBox == null) return null;
            return new ReadOnlyObjectWrapper<>(vBox);
        });
    }

    private void fillCatTable() {
        completed.clear();
        catCompletions.clear();

        Map<Achievement, AchCompletion> allCompleted = AchManager.getInstance().getRecordedAchievements();

        for (AchCat cat : AchCat.values()) {
            Achievement[] achievements = cat.getAll();
            List<AchievementBundle> bundles = new ArrayList<>();
            int showing = 0;
            int comp = 0;
            for (Achievement achievement : achievements) {
                AchCompletion completion = allCompleted.get(achievement);
                if (!achievement.isHidden()) showing += achievement.getNLevels();
                comp += achievement.getNCompleted(completion);
//                if (achievement.isComplete(completion)) comp++;
                bundles.add(new AchievementBundle(achievement, completion));
            }
            completed.put(cat, bundles);
            catCompletions.put(cat, new int[]{showing, comp});

            if (comp > 0) {
                achCatTable.getItems().add(new CatItem(cat));
            }
        }
    }

    private void refreshAchTable() {
        achTable.getItems().clear();
        CatItem selected = achCatTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            boolean showIncomplete = selected.cat != AchCat.GENERAL_HIDDEN;
            for (AchievementBundle bundle : completed.get(selected.cat)) {
                if ((!showIncomplete || bundle.achievement.isHidden()) && bundle.completion == null)
                    continue;

                if (bundle.achievement.getType() == Achievement.Type.COLLECTIVE) {
                    if (bundle.completion != null) {
                        AchCompletion.Collective collective = (AchCompletion.Collective) bundle.completion;
                        for (String pid : collective.getKeys()) {
                            AchievementBundle subBundle = new AchievementBundle(
                                    bundle.achievement,
                                    collective.getIndividual(pid)
                            );
                            AchItem item = new AchItem(subBundle);
                            achTable.getItems().add(item);
                        }
                    } else {
                        EventLogger.error("Collective completion is null, should not enter this branch");
                    }
                } else {
                    AchItem item = new AchItem(bundle);
                    achTable.getItems().add(item);
                }
            }
        }
    }

    private void showMoneyReceived() {
        if (popupsToShow.isEmpty()) return;
        if (!App.isMainWindowShowing()) return;

        int money = popupsToShow.removeFirst();
        popupShowing = true;

        Stage stage = new Stage();

        VBox baseRoot = new VBox();
        baseRoot.setAlignment(Pos.CENTER);
        HBox root = new HBox();

        Insets insetsOut = new Insets(2.0);
        Insets insets = new Insets(4.0);
        CornerRadii cornerRadii = new CornerRadii(5.0);
        baseRoot.setBackground(new Background(new BackgroundFill(Color.valueOf("#FFFCF7"),
                cornerRadii, null)));
        root.setBorder(new Border(new BorderStroke(Color.BURLYWOOD,
                BorderStrokeStyle.SOLID, cornerRadii, BorderWidths.DEFAULT)));
        baseRoot.setPadding(insetsOut);
        baseRoot.setStyle(App.FONT_STYLE);

        root.setSpacing(10.0);
        root.setPadding(insets);

        ImageView imageView = new ImageView();
        resourcesLoader.setIconImage(resourcesLoader.getMoneyImg(), imageView);

        Label label = new Label("+" + money);

        root.getChildren().addAll(imageView, label);
        baseRoot.getChildren().add(root);

        Scene scene = new Scene(baseRoot);
        scene.setFill(Color.TRANSPARENT);

        stage.setScene(scene);
        stage.initStyle(StageStyle.TRANSPARENT);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setAlwaysOnTop(true);

        baseRoot.setOpacity(0.0);

        Timeline shower = new Timeline();
        KeyFrame showing = new KeyFrame(Duration.millis(500),
                new KeyValue(baseRoot.opacityProperty(), 0.8));
        shower.getKeyFrames().add(showing);

        Timeline keeper = new Timeline();
        keeper.getKeyFrames().add(new KeyFrame(Duration.millis(2000)));

        Timeline fader = new Timeline();
        KeyFrame fading = new KeyFrame(Duration.millis(500),
                new KeyValue(baseRoot.opacityProperty(), 0.0));
        fader.getKeyFrames().add(fading);

        SequentialTransition st = new SequentialTransition(shower, keeper, fader);
        st.setOnFinished(event -> {
            stage.hide();
            if (!popupsToShow.isEmpty()) {
                showMoneyReceived();
            } else {
                popupShowing = false;
            }
        });

        stage.show();
        CareerAchManager.showToPosition(stage);

        st.play();
    }

    public class AchItem {
        AchievementBundle bundle;

        AchItem(AchievementBundle bundle) {
            this.bundle = bundle;
        }

        public Image getImage() {
            if (bundle.completion != null) {
                if (bundle.completion.getNCompleted() > 0) {
                    return bundle.completion.getImage();
                }
            }
            return null;
        }

        public VBox getUnreceivedAwards() {
            VBox box = new VBox();
            box.setAlignment(Pos.CENTER);
            
            fillBoxContent(box);
            return box;
        }
        
        private void fillBoxContent(VBox box) {
            if (bundle.completion != null) {
                SortedMap<Integer, Integer> notReceived = bundle.completion.getUnreceivedAwards();
                if (!notReceived.isEmpty()) {
                    int firstLevelIndex = notReceived.firstKey();
                    int money = notReceived.get(firstLevelIndex);

                    Button button = new Button("+" + money);
                    ImageView iv = new ImageView();
                    resourcesLoader.setIconImage(resourcesLoader.getMoneyImg(), iv);

                    button.setOnAction(e -> {
                        if (bundle.completion.receiveAward(firstLevelIndex)) {
                            popupsToShow.addLast(money);
                            if (!popupShowing) showMoneyReceived();

                            humanCareer.earnAchievementAward(bundle.achievement, firstLevelIndex, money);
                            box.getChildren().clear();
                            fillBoxContent(box);
                        } else {
                            System.err.println("Cannot receive award!");
                        }
                    });

                    button.setGraphic(iv);
                    box.getChildren().add(button);
                }
            }
        }

        public String firstRow() {
            if (bundle.achievement.isFullyComplete(bundle.completion)) {
                // 这里已经保证不是null了，不信 @see Achievement#isFullyComplete
                Date firstCompletion = bundle.completion.getFirstCompletion();
                if (firstCompletion != null) {
                    return String.format(strings.getString("firstCompleteDate"),
                            Util.SHOWING_DATE_FORMAT.format(firstCompletion));
                } else {
                    System.err.println(bundle.achievement + " date is null");
                    return "";
                }
            } else if (bundle.completion != null) {
                return String.format("%d/%d",
                        bundle.completion.getNCompleted(),
                        bundle.achievement.getNLevels());
            } else {
                return strings.getString("incomplete");
            }
        }

        public String secondRow() {
            if (bundle.achievement.getType() == Achievement.Type.CUMULATIVE) {
                if (bundle.completion == null) {
                    return "";
                } else {
                    return String.format(strings.getString("completeTimes"),
                            bundle.completion.getTimes());
                }
            } else if (bundle.achievement.getType() == Achievement.Type.HIGH_RECORD) {
                if (bundle.completion == null) {
                    return "";
                } else {
                    return String.format(strings.getString("highestRecord"),
                            bundle.completion.getTimes());
                }
            } else if (bundle.achievement.getType() == Achievement.Type.COLLECTIVE) {
                if (bundle.completion instanceof AchCompletion.Sub) {
                    return String.format(strings.getString("completeTimes"),
                            bundle.completion.getTimes());
                } else {
                    return "";
                }
            } else {
                return "";
            }
        }
    }

    private static class AchievementBundle {
        final Achievement achievement;
        final AchCompletion completion;  // nullable

        AchievementBundle(Achievement achievement, AchCompletion completion) {
            this.achievement = achievement;
            this.completion = completion;
        }
    }

    public class CatItem {
        AchCat cat;

        CatItem(AchCat cat) {
            this.cat = cat;
        }

        public String completion() {
            int[] numbers = catCompletions.get(cat);
            if (cat == AchCat.GENERAL_HIDDEN || cat == AchCat.UNIQUE_DEFEATS) {
                return String.valueOf(numbers[1]);
            } else {
                return numbers[1] + "/" + numbers[0];
            }
        }
    }
}
