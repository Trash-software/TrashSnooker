package trashsoftware.trashSnooker.fxml;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
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
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.career.*;
import trashsoftware.trashSnooker.core.career.championship.Championship;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.fxml.widgets.AbilityShower;
import trashsoftware.trashSnooker.fxml.widgets.LabelTable;
import trashsoftware.trashSnooker.fxml.widgets.LabelTableColumn;
import trashsoftware.trashSnooker.fxml.widgets.PerkManager;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.Util;

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
    Label levelLabel, levelExpLabel;
    @FXML
    ProgressBar levelExpBar;
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
    ComboBox<ChampionshipData.Selection> rankMethodBox;
    @FXML
    Pane champInProgBox, nextChampInfoBox;
    @FXML
    LabelTable<PlayerAward> selectedPlayerInfoTable;
    @FXML
    Label selectedPlayerAchievements;
    @FXML
    Button skipChampBtn;

    CareerManager careerManager;
    private PerkManager perkManager;
    private Stage selfStage;
    private ResourceBundle strings;
    
    private ChampionshipData activeOrNext;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.strings = resourceBundle;
        
        careerManager = CareerManager.getInstance();

        PlayerPerson pp = careerManager.getHumanPlayerCareer().getPlayerPerson();
        perkManager = new PerkManager(this,
                careerManager.getHumanPlayerCareer().getAvailablePerks(),
                PlayerPerson.ReadableAbility.fromPlayerPerson(pp));
        abilityShower.setup(perkManager, pp.isCustom());

        joinChampBox.selectedProperty().addListener(((observable, oldValue, newValue) -> 
                skipChampBtn.setDisable(newValue)));

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

    private void showHideSelectedPanel(boolean show) {
        selectedPlayerInfoTable.setVisible(show);
        selectedPlayerInfoTable.setManaged(show);
        selectedPlayerAchievements.setVisible(show);
        selectedPlayerAchievements.setManaged(show);
    }

    private void refreshSelectedPlayerTable(CareerRank selected) {
        selectedPlayerInfoTable.clearItems();

        boolean currentlyVisible = selectedPlayerInfoTable.isVisible();

        if (selected == null) {
            showHideSelectedPanel(false);
        } else {
            showHideSelectedPanel(true);
            List<ChampionshipScore> cs = new ArrayList<>(selected.getCareer().getChampionshipScores());
            Collections.reverse(cs);

            for (ChampionshipScore score : cs) {
                selectedPlayerInfoTable.addItem(new PlayerAward(score));
            }

            selectedPlayerAchievements.setText(getAchievements(cs, rankTypeBox.getValue()));

            if (selfStage != null && (currentlyVisible != selectedPlayerInfoTable.isVisible()))
                selfStage.sizeToScene();
        }
    }

    private String getAchievements(List<ChampionshipScore> csList, GameRule gameRule) {
        int rankedChampions = 0;

        ChampionshipScore.Rank bestRankedRank = null;
        for (ChampionshipScore cs : csList) {
            if (cs.data.getType() == gameRule && cs.data.isRanked()) {
                if (Util.arrayContains(cs.ranks, ChampionshipScore.Rank.CHAMPION)) {
                    rankedChampions++;
                }
                for (ChampionshipScore.Rank r : cs.ranks) {
                    if (bestRankedRank == null || r.ordinal() < bestRankedRank.ordinal()) {
                        bestRankedRank = r;
                    }
                }
            }
        }

        StringBuilder builder = new StringBuilder();
        builder.append(gameRule.toString()).append('\n');
        builder.append(strings.getString("bestRankedScore"))
                .append(bestRankedRank == null ? strings.getString("none") : bestRankedRank.getShown())
                .append('\n');
        builder.append(strings.getString("numRankedChamps")).append(rankedChampions);

        switch (gameRule) {
            case SNOOKER:
                List<ChampionshipData> threeBigData = ChampDataManager.getInstance().getSnookerThreeBig();
                Map<ChampionshipData, Integer> threeBigAch = new HashMap<>();
                for (ChampionshipScore cs : csList) {
                    if (threeBigData.contains(cs.data)) {
                        if (Util.arrayContains(cs.ranks, ChampionshipScore.Rank.CHAMPION)) {
                            threeBigAch.merge(cs.data, 1, Integer::sum);
                        }
                    }
                }
                builder.append("\n")
                        .append(strings.getString("numSnookerThreeChamps"))
                        .append("\n");
                for (ChampionshipData cd : threeBigData) {
                    builder.append(cd.getName())
                            .append(": ")
                            .append(threeBigAch.getOrDefault(cd, 0))
                            .append('\n');
                }
                break;
        }

        return builder.toString();
    }

    private void setupAwdTable(LabelTable<PlayerAward> table) {
        LabelTableColumn<PlayerAward, String> champCol =
                new LabelTableColumn<>(table, strings.getString("gameEvent"), param ->
                        new ReadOnlyObjectWrapper<>(param.score.getYear() + " " + param.score.data.getName()));
        LabelTableColumn<PlayerAward, String> scoreCol =
                new LabelTableColumn<>(table, strings.getString("achievement"), param -> {
                    StringBuilder builder = new StringBuilder();
                    for (ChampionshipScore.Rank rank : param.score.ranks) {
                        builder.append(rank.getShown()).append(' ');
                    }
                    return new ReadOnlyStringWrapper(builder.toString());
                });
        LabelTableColumn<PlayerAward, Integer> awardMoneyCol =
                new LabelTableColumn<>(table, strings.getString("awards"), param -> {
                    int money = 0;
                    for (ChampionshipScore.Rank rank : param.score.ranks) {
                        money += param.score.data.getAwardByRank(rank);
                    }
                    return new ReadOnlyObjectWrapper<>(money);
                });

        table.addColumns(champCol, scoreCol, awardMoneyCol);
    }

    private void initAwardsTable() {
        // 玩家自己的奖金表
        setupAwdTable(allAwardsTable);
        refreshPersonalAwardsTable();

        // 点选的球员的奖金表
        setupAwdTable(selectedPlayerInfoTable);
        refreshSelectedPlayerTable(null);

        // 赛事奖金表
        LabelTableColumn<AwardItem, String> titleCol =
                new LabelTableColumn<>(champAwardsTable, "", param ->
                        new ReadOnlyStringWrapper(param.rank.getShown()));
        LabelTableColumn<AwardItem, Integer> awardCol =
                new LabelTableColumn<>(champAwardsTable, strings.getString("awards"), param ->
                        new ReadOnlyObjectWrapper<>(param.data.getAwardByRank(param.rank)));
        LabelTableColumn<AwardItem, Integer> perkCol =
                new LabelTableColumn<>(champAwardsTable, "exp", param ->
                        new ReadOnlyObjectWrapper<>(param.data.getExpByRank(param.rank)));

        champAwardsTable.addColumns(titleCol, awardCol, perkCol);
    }

    private void initTypeBox() {
        rankTypeBox.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            refreshRanks();
        }));
        rankTypeBox.getItems().addAll(GameRule.values());

        rankMethodBox.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            refreshRanks();
        }));
        rankMethodBox.getItems().addAll(
                ChampionshipData.Selection.REGULAR,
                ChampionshipData.Selection.SINGLE_SEASON
        );

        rankMethodBox.getSelectionModel().select(ChampionshipData.Selection.REGULAR);
        rankTypeBox.getSelectionModel().select(GameRule.SNOOKER);
    }

    private void initTable() {
        rankCol.setCellValueFactory(new PropertyValueFactory<>("rankFrom1"));
        rankNameCol.setCellValueFactory(param ->
                new ReadOnlyStringWrapper(param.getValue().career.getPlayerPerson().getName()));
        rankedAwardCol.setCellValueFactory(new PropertyValueFactory<>("shownAwards"));
        totalAwardCol.setCellValueFactory(new PropertyValueFactory<>("totalAwards"));

        rankingTable.getSelectionModel().selectedItemProperty()
                .addListener(((observable, oldValue, newValue) -> refreshSelectedPlayerTable(newValue)));
    }

    public void refreshGui() {
        Career myCareer = careerManager.getHumanPlayerCareer();
        levelLabel.setText("Lv." + myCareer.getLevel());
        int curExp = myCareer.getExpInThisLevel();
        int expToNext = careerManager.getExpNeededToLevelUp(myCareer.getLevel());
        levelExpBar.setProgress((double) curExp / expToNext);
        levelExpLabel.setText(String.format("%d/%d", curExp, expToNext));

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

            if (careerManager.humanPlayerQualifiedToJoinSnooker(data, data.getSelection())) {
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
            champInProgStageLabel.setText(inProgress.getCurrentStage().getShown());

            data = inProgress.getData();
        }
        activeOrNext = data;

        // 更新赛事奖金表
        champAwardsTable.clearItems();
        champAwardsTable.getColumns().get(0).setTitle(data.isRanked() ? 
                strings.getString("rankedGame") : 
                strings.getString("nonRankedGame"));
        ChampionshipScore.Rank[] ranks = data.getRanksOfLosers();
        champAwardsTable.addItem(new AwardItem(data, ChampionshipScore.Rank.CHAMPION, ChampionshipStage.FINAL));
        for (int i = 0; i < ranks.length; i++) {
            ChampionshipScore.Rank rank = ranks[i];
            ChampionshipStage stage = i == ranks.length - 1 ? null : data.getStages()[i + 1];
            champAwardsTable.addItem(new AwardItem(data, rank, stage));
        }

        // 更新加点界面
        perkManager.synchronizePerks();
        notifyPerksChanged();
        abilityShower.notifyPerksReset();
    }

    private void refreshRanks() {
        if (rankTypeBox.getValue() == null || rankMethodBox.getValue() == null) return;

        rankingTable.getItems().clear();
        rankingTable.getItems().addAll(careerManager.getRanking(rankTypeBox.getValue(), rankMethodBox.getValue()));

        CareerRank myRank = careerManager.humanPlayerRanking(rankTypeBox.getValue(), rankMethodBox.getValue());
        myRankLabel.setText(String.format("%d  %s  %d  %d",
                myRank.getRankFrom1(),
                myRank.career.getPlayerPerson().getName(),
                myRank.getShownAwards(),
                myRank.getTotalAwards()));
    }

    @FXML
    public void clearUsedPerks() {
        perkManager.clearSelections();
        abilityShower.notifyPerksReset();

        notifyPerksChanged();
    }

    @FXML
    public void applyPerks() {
        int used = perkManager.applyPerks();
        careerManager.getHumanPlayerCareer().usePerk(used);

//        DataLoader.getInstance().updatePlayer(perkManager.getAbility().toPlayerPerson());
        careerManager.reloadHumanPlayerPerson();
        careerManager.saveToDisk();

        notifyPerksChanged();

        abilityShower.notifyPerksReset();
    }
    
    @FXML
    public void seeToursListAction() {
        try {
            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(selfStage);

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("tournamentsViewer.fxml"),
                    strings
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            Scene scene = new Scene(root);

//            Scene scene = new Scene(root, -1, -1, false, SceneAntialiasing.BALANCED);
//            scene.getStylesheets().add(getClass().getResource("/trashsoftware/trashSnooker/css/font.css").toExternalForm());
            stage.setScene(scene);
            
            TournamentsViewer viewer = loader.getController();
            viewer.initialSelection(activeOrNext);

            stage.show();
        } catch (IOException e) {
            EventLogger.log(e);
        }
    }

    @FXML
    public void nextChamp() {
        Championship championship = careerManager.startNextChampionship();
        championship.startChampionship(joinChampBox.isSelected());

        refreshGui();

        showChampDrawView();
    }

    @FXML
    public void skipNextChamp() {
        if (!joinChampBox.isSelected()) {
            Championship championship = careerManager.startNextChampionship();
            championship.startChampionship(false);
            
            while (!championship.isFinished()) {
                championship.startNextRound();
            }
            
            refreshGui();
        }
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
                    getClass().getResource("champDrawView.fxml"),
                    strings
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

    public void notifyPerksChanged() {
        availPerksLabel.setText(perkManager.getAvailPerks() + "");
    }

    public enum CareerAwardTime {
        RANKED("rankRank"),
        TOTAL("rankTotalAwards");

        private final String key;

        CareerAwardTime(String key) {
            this.key = key;
        }

        @Override
        public String toString() {
            return App.getStrings().getString(key);
        }
    }

    public static class PlayerAward {
        final ChampionshipScore score;

        PlayerAward(ChampionshipScore score) {
            this.score = score;
        }
    }

    public static class AwardItem {
        public final ChampionshipData data;
        public final ChampionshipScore.Rank rank;
        public final ChampionshipStage winnerStage;

        public AwardItem(ChampionshipData data, ChampionshipScore.Rank rank, ChampionshipStage winnerStage) {
            this.data = data;
            this.rank = rank;
            this.winnerStage = winnerStage;
        }
    }
}
