package trashsoftware.trashSnooker.fxml;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;
import trashsoftware.trashSnooker.core.career.achievement.AchCat;
import trashsoftware.trashSnooker.core.career.achievement.AchCompletion;
import trashsoftware.trashSnooker.core.career.achievement.AchManager;
import trashsoftware.trashSnooker.core.career.achievement.Achievement;
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

    private final Map<AchCat, List<AchievementBundle>> completed = new HashMap<>();
    private final Map<AchCat, int[]> catCompletions = new HashMap<>();  // 值是[总数, 完成数]

    private Stage stage;
    private ResourceBundle strings;

    public void setup(Stage stage) {
        this.stage = stage;

        fillCatTable();
        
        totalCountLabel.setText(String.format(strings.getString("totalCount"), 
                AchManager.getInstance().getNCompletedAchievements()));
    }

    @Override
    public Stage getStage() {
        return stage;
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
            VBox vBox = new VBox();
            Label bigLabel = new Label(cell.getValue().bundle.achievement.title());
            bigLabel.setFont(titleFont);
            
            vBox.getChildren().addAll(bigLabel, new Label(cell.getValue().bundle.achievement.description()));
            
            return new ReadOnlyObjectWrapper<>(vBox);
        });
        
        dateCol.setCellValueFactory(cell -> {
            VBox vBox = new VBox();
            Label r1 = new Label(cell.getValue().firstRow());
            Label r2 = new Label(cell.getValue().secondRow());
            vBox.getChildren().addAll(r1, r2);
            
            return new ReadOnlyObjectWrapper<>(vBox);
        });
    }

    private void fillCatTable() {
        completed.clear();
        catCompletions.clear();

        Map<Achievement, AchCompletion> allCompleted = AchManager.getInstance().getCompletedAchievements();

        for (AchCat cat : AchCat.values()) {
            Achievement[] achievements = cat.getAll();
            List<AchievementBundle> bundles = new ArrayList<>();
            int comp = 0;
            for (Achievement achievement : achievements) {
                AchCompletion completion = allCompleted.get(achievement);
                if (completion != null) comp++;
                bundles.add(new AchievementBundle(achievement, completion));
            }
            completed.put(cat, bundles);
            catCompletions.put(cat, new int[]{achievements.length, comp});
            
            achCatTable.getItems().add(new CatItem(cat));
        }
    }

    private void refreshAchTable() {
        achTable.getItems().clear();
        CatItem selected = achCatTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            boolean showIncomplete = selected.cat != AchCat.GENERAL_HIDDEN;
            for (AchievementBundle bundle : completed.get(selected.cat)) {
                if (!showIncomplete && bundle.completion == null) continue;
                
                AchItem item = new AchItem(bundle);
                achTable.getItems().add(item);
            }
        }
    }

    public class AchItem {
        AchievementBundle bundle;

        AchItem(AchievementBundle bundle) {
            this.bundle = bundle;
        }
        
        public String firstRow() {
            if (bundle.achievement.isComplete(bundle.completion)) {
                // 这里已经保证不是null了，不信 @see Achievement#isComplete
                return String.format(strings.getString("firstCompleteDate"),
                        Util.SHOWING_DATE_FORMAT.format(bundle.completion.getFirstCompletion()));
            } else {
                return strings.getString("incomplete");
            }
        }
        
        public String secondRow() {
            if (bundle.achievement.countLikeRepeatable()) {
                if (bundle.completion == null) {
                    return "";
                } else {
                    return String.format(strings.getString("completeTimes"),
                            bundle.completion.getTimes());
                }
            } if (bundle.achievement.isRecordLike()) {
                if (bundle.completion == null) {
                    return "";
                } else {
                    return String.format(strings.getString("highestRecord"),
                            bundle.completion.getTimes());
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
            if (cat == AchCat.GENERAL_HIDDEN) {
                return String.valueOf(numbers[1]);
            } else {
                return numbers[1] + "/" + numbers[0];
            }
        }
    }
}
