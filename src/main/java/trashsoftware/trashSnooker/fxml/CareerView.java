package trashsoftware.trashSnooker.fxml;

import javafx.beans.property.ReadOnlyIntegerWrapper;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.CareerRank;
import trashsoftware.trashSnooker.core.career.ChampionshipData;
import trashsoftware.trashSnooker.core.career.ChampionshipScore;
import trashsoftware.trashSnooker.core.career.championship.Championship;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.fxml.widgets.AbilityShower;
import trashsoftware.trashSnooker.fxml.widgets.LabelTable;
import trashsoftware.trashSnooker.fxml.widgets.LabelTableColumn;
import trashsoftware.trashSnooker.fxml.widgets.PerkManager;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;

import java.io.IOException;
import java.net.URL;
import java.util.*;

public class CareerView implements Initializable {

    @FXML
    TableView<CareerRank> rankingTable;
    @FXML
    TableColumn<CareerRank, Integer> rankCol, rankedAwardCol, totalAwardCol;
    @FXML
    TableColumn<CareerRank, String> rankNameCol;

    @FXML
    AbilityShower abilityShower;
    @FXML
    LabelTable<PlayerAward> allAwardsTable;
    @FXML
    Label myRankLabel;
    @FXML
    Label currentDateLabel;
    @FXML
    Label availPerksLabel;
    @FXML
    Button confirmAddPerkBtn, clearPerkBtn;
    @FXML
    Label nextChampionshipLabel, champInProgLabel, champInProgStageLabel;
    @FXML
    CheckBox joinChampBox;
//    @FXML
//    Label champAwardsLabel1, champAwardsLabel2;
    @FXML
    LabelTable<AwardItem> champAwardsTable;
    @FXML
    ComboBox<GameRule> rankTypeBox;
    @FXML
    Pane champInProgBox, nextChampInfoBox;

    CareerManager careerManager;
    private PerkManager perkManager;
    private Stage selfStage;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        careerManager = CareerManager.getInstance();

        PlayerPerson pp = careerManager.getHumanPlayerCareer().getPlayerPerson();
        perkManager = new PerkManager(this, 
                careerManager.getHumanPlayerCareer().getAvailablePerks(), 
                PlayerPerson.ReadableAbility.fromPlayerPerson(pp));
        abilityShower.setup(perkManager, pp.isCustom());
        
        initTypeBox();
        initTable();
        initAwardsTable();

        refreshGui();
    }

    public void setSelfStage(Stage selfStage) {
        this.selfStage = selfStage;
    }
    
    private void refreshPersonalAwardsTable() {
        allAwardsTable.clearItems();
        List<ChampionshipScore> cs = new ArrayList<>(careerManager.getHumanPlayerCareer().getChampionshipScores());
        Collections.reverse(cs);
        for (ChampionshipScore score : cs) {
            allAwardsTable.addItem(new PlayerAward(score));
        }
    }
    
    private void initAwardsTable() {
        LabelTableColumn<PlayerAward, String> champCol = 
                new LabelTableColumn<>(allAwardsTable, "赛事", param -> 
                        new ReadOnlyObjectWrapper<>(param.score.getYear() + " " + param.score.data.getName()));
        LabelTableColumn<PlayerAward, String> scoreCol =
                new LabelTableColumn<>(allAwardsTable, "成绩", param -> {
                    StringBuilder builder = new StringBuilder();
                    for (ChampionshipScore.Rank rank : param.score.ranks) {
                        builder.append(rank.getShown()).append(' ');
                    }
                    return new ReadOnlyStringWrapper(builder.toString());
                });
        LabelTableColumn<PlayerAward, Integer> awardMoneyCol =
                new LabelTableColumn<>(allAwardsTable, "奖金", param -> {
                    int money = 0;
                    for (ChampionshipScore.Rank rank : param.score.ranks) {
                        money += param.score.data.getAwardByRank(rank);
                    }
                    return new ReadOnlyObjectWrapper<>(money);
                });
        
        allAwardsTable.addColumns(champCol, scoreCol, awardMoneyCol);

        refreshPersonalAwardsTable();
        
        // 赛事奖金表
        LabelTableColumn<AwardItem, String> titleCol = 
                new LabelTableColumn<>(champAwardsTable, "", param -> 
                        new ReadOnlyStringWrapper(param.rank.getShown()));
        LabelTableColumn<AwardItem, Integer> awardCol =
                new LabelTableColumn<>(champAwardsTable, "奖金", param ->
                        new ReadOnlyObjectWrapper<>(param.data.getAwardByRank(param.rank)));
        LabelTableColumn<AwardItem, Integer> perkCol =
                new LabelTableColumn<>(champAwardsTable, "点数", param ->
                        new ReadOnlyObjectWrapper<>(param.data.getPerkByRank(param.rank)));
        
        champAwardsTable.addColumns(titleCol, awardCol, perkCol);
    }

    private void initTypeBox() {
        rankTypeBox.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            refreshRanks();
        }));
        
        rankTypeBox.getItems().addAll(GameRule.values());
        rankTypeBox.getSelectionModel().select(GameRule.SNOOKER);
    }
    
    private void initTable() {
        rankCol.setCellValueFactory(new PropertyValueFactory<>("rankFrom1"));
        rankNameCol.setCellValueFactory(param ->
                new ReadOnlyStringWrapper(param.getValue().career.getPlayerPerson().getName()));
        rankedAwardCol.setCellValueFactory(new PropertyValueFactory<>("recentAwards"));
        totalAwardCol.setCellValueFactory(new PropertyValueFactory<>("totalAwards"));
    }
    
    public void refreshGui() {
        refreshRanks();
        refreshPersonalAwardsTable();
        currentDateLabel.setText(String.format("%d/%d/%d", 
                careerManager.getTimestamp().get(Calendar.YEAR),
                careerManager.getTimestamp().get(Calendar.MONTH) + 1,
                careerManager.getTimestamp().get(Calendar.DAY_OF_MONTH)));

        Championship inProgress = careerManager.getChampionshipInProgress();
//        System.out.println(inProgress);
        ChampionshipData data;
        if (inProgress == null) {
            champInProgBox.setVisible(false);
            champInProgBox.setManaged(false);

            nextChampInfoBox.setVisible(true);
            nextChampInfoBox.setManaged(true);
            ChampionshipData.WithYear nextData = careerManager.nextChampionshipData();
            data = nextData.data;
            
            if (careerManager.humanPlayerQualifiedToJoinSnooker(data)) {
                joinChampBox.setDisable(false);
                joinChampBox.setSelected(true);
            } else {
                joinChampBox.setDisable(true);
                joinChampBox.setSelected(false);
            }
            nextChampionshipLabel.setText(nextData.fullName());
        } else {
            champInProgBox.setVisible(true);
            champInProgBox.setManaged(true);
            
            nextChampInfoBox.setVisible(false);
            nextChampInfoBox.setManaged(false);
            
            champInProgLabel.setText(inProgress.fullName());
            champInProgStageLabel.setText(inProgress.getCurrentStage().shown);
            
            data = inProgress.getData();
        }
        
        // 更新赛事奖金表
        champAwardsTable.clearItems();
        champAwardsTable.getColumns().get(0).setTitle(data.isRanked() ? "排名赛" : "非排名赛");
        ChampionshipScore.Rank[] ranks = data.getRanksOfLosers();
        champAwardsTable.addItem(new AwardItem(data, ChampionshipScore.Rank.CHAMPION));
        for (ChampionshipScore.Rank rank : ranks) {
            champAwardsTable.addItem(new AwardItem(data, rank));
        }
    }

    private void refreshRanks() {
        rankingTable.getItems().clear();
        rankingTable.getItems().addAll(careerManager.getRanking(rankTypeBox.getValue()));

        CareerRank myRank = careerManager.humanPlayerSnookerRanking();
        myRankLabel.setText(String.format("%d  %s  %d",
                myRank.getRankFrom1(),
                myRank.career.getPlayerPerson().getName(),
                myRank.getRecentAwards()));

        availPerksLabel.setText(String.valueOf(myRank.career.getAvailablePerks()));
    }

    @FXML
    public void clearUsedPerks() {
        perkManager.clearSelections();
        abilityShower.noticePerksReset();

        noticePerksChanged();
    }

    @FXML
    public void applyPerks() {
        int used = perkManager.applyPerks();
        careerManager.getHumanPlayerCareer().usePerk(used);

//        DataLoader.getInstance().updatePlayer(perkManager.getAbility().toPlayerPerson());
        careerManager.reloadHumanPlayerPerson();

        noticePerksChanged();

        abilityShower.noticePerksReset();
    }

    @FXML
    public void nextChamp() {
        Championship championship = careerManager.startNextChampionship();
        championship.startChampionship(joinChampBox.isSelected());
        
        refreshGui();
        
        showChampDrawView();
    }

    @FXML
    public void continueChampInProg() {
        showChampDrawView();
    }
    
    private void showChampDrawView() {
        try {
            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(selfStage);
            
            stage.setTitle(CareerManager.getInstance().getChampionshipInProgress().fullName());

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("champDrawView.fxml")
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            ChampDrawView view = loader.getController();
            view.setup(this, stage);
//            mainView.setup(parentStage, stage);

            Scene scene = new Scene(root);

//            Scene scene = new Scene(root, -1, -1, false, SceneAntialiasing.BALANCED);
//            scene.getStylesheets().add(getClass().getResource("/trashsoftware/trashSnooker/css/font.css").toExternalForm());
            stage.setScene(scene);

            stage.show();
        } catch (IOException e) {
            EventLogger.log(e);
        }
    }

    public void noticePerksChanged() {
        availPerksLabel.setText(perkManager.getAvailPerks() + "");
    }
    
    public enum CareerAwardTime {
        RANKED("排名"),
        TOTAL("生涯总奖金");
        
        private final String shown;
        
        CareerAwardTime(String shown) {
            this.shown = shown;
        }

        @Override
        public String toString() {
            return shown;
        }
    }
    
    public static class PlayerAward {
        final ChampionshipScore score;
        
        PlayerAward(ChampionshipScore score) {
            this.score = score;
        }
    }
    
    public static class AwardItem {
        final ChampionshipData data;
        final ChampionshipScore.Rank rank;
        
        AwardItem(ChampionshipData data, ChampionshipScore.Rank rank) {
            this.data = data;
            this.rank = rank;
        }
    }
}
