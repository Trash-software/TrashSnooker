package trashsoftware.trashSnooker.fxml;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import trashsoftware.trashSnooker.core.PlayerPerson;
import trashsoftware.trashSnooker.core.career.*;
import trashsoftware.trashSnooker.core.career.achievement.AchManager;
import trashsoftware.trashSnooker.core.career.achievement.CareerAchManager;
import trashsoftware.trashSnooker.core.career.awardItems.AwardMaterial;
import trashsoftware.trashSnooker.core.career.championship.Championship;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.fxml.alert.Alert;
import trashsoftware.trashSnooker.fxml.alert.AlertShower;
import trashsoftware.trashSnooker.fxml.widgets.AbilityShower;
import trashsoftware.trashSnooker.fxml.widgets.LabelTable;
import trashsoftware.trashSnooker.fxml.widgets.LabelTableColumn;
import trashsoftware.trashSnooker.fxml.widgets.PerkManager;
import trashsoftware.trashSnooker.res.ResourcesLoader;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.Util;
import trashsoftware.trashSnooker.util.db.DBAccess;

import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;

public class CareerView extends ChildInitializable {
    public static final Color EARN_MONEY_COLOR = Color.GREEN;
    public static final Color SPEND_MONEY_COLOR = Color.RED.darker();
    @FXML
    GridPane basePane;
    @FXML
    TableView<RankedCareer> rankingTable;
    @FXML
    TableColumn<RankedCareer, Integer> rankCol;
    @FXML
    TableColumn<RankedCareer, String> rankNameCol, rankFirstDesCol, rankSecondDesCol;
    @FXML
    Label levelLabel, levelExpLabel, moneyLabel, achievementsLabel;
    @FXML
    Button levelUpBtn;
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
    Label registryFeeLabel, travelFeeLabel,
            otherFeeLabel1, otherFeeLabel2,
            totalFeeLabel, totalFeeLabel2;
    @FXML
    HBox feesBoxChecked, feesBoxUnchecked;
    @FXML
    CheckBox joinChampBox;
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
    VBox selectedPlayerAchBox;
    @FXML
    Label selectedPlayerAchievements, selectedPlayerGameTypesLabel;
    @FXML
    Button skipChampBtn;
    @FXML
    ImageView expImage, moneyImage, inventoryImage, storeImage, achIconImage, lineChartImg;
    CareerManager careerManager;
    private PerkManager perkManager;
    private Stage selfStage;
    private EntryView parent;
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
        abilityShower.setExtraField(createPerksBox());

        joinChampBox.selectedProperty().addListener(((observable, oldValue, newValue) ->
                refreshFeesTexts(newValue)));

        initTypeBox();
        rankingTable.getSelectionModel().selectedItemProperty()
                .addListener(((observable, oldValue, newValue) -> refreshSelectedPlayerTable(newValue)));
        initAwardsTable();
        setupImages();
    }

    private HBox createPerksBox() {
        HBox box = new HBox();
        box.setSpacing(10.0);
        box.setAlignment(Pos.CENTER);
        availPerksLabel = new Label();
        clearPerkBtn = new Button(strings.getString("restorePerks"));
        clearPerkBtn.setOnAction(e -> clearUsedPerks());
        confirmAddPerkBtn = new Button(strings.getString("applyPerks"));
        confirmAddPerkBtn.setDisable(true);
        confirmAddPerkBtn.setOnAction(e -> applyPerksAction());
        
        box.getChildren().addAll(availPerksLabel, clearPerkBtn, confirmAddPerkBtn);
        return box;
    }

//    @Override
//    public Stage getStage() {
//        return selfStage;
//    }

    public void setup(EntryView parent, Stage selfStage) {
        this.selfStage = selfStage;
        this.parent = parent;

        // 检测一些新出的成就
        careerManager.getHumanPlayerCareer().checkScoreAchievements();
        careerManager.checkRankingAchievements();
        DBAccess.getInstance().checkAchievements();
        AchManager.getInstance().showAchievementPopup();

        refreshGui();
    }

    private void refreshFeesTexts(boolean join) {
        skipChampBtn.setDisable(join);
        feesBoxChecked.setVisible(join);
        feesBoxChecked.setManaged(join);
        feesBoxUnchecked.setVisible(!join);
        feesBoxUnchecked.setManaged(!join);
    }

    private void setupImages() {
        ResourcesLoader rl = ResourcesLoader.getInstance();

        rl.setIconImage(rl.getExpImg(), expImage);
        rl.setIconImage(rl.getMoneyImg(), moneyImage);
        rl.setIconImage(rl.getInventoryIcon(), inventoryImage, 1.0, 1.25);
        rl.setIconImage(rl.getStoreIcon(), storeImage, 1.0, 1.25);
        rl.setIconImage(rl.getAwardIcon(), achIconImage, 1.0, 1.25);
        rl.setIconImage(rl.getLineIcon(), lineChartImg, 1.0, 1.25);
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
        if (show) {
            basePane.getColumnConstraints().get(0).setPercentWidth(40);
            basePane.getColumnConstraints().get(1).setPercentWidth(32);
            basePane.getColumnConstraints().get(2).setPercentWidth(28);
        } else {
            basePane.getColumnConstraints().get(0).setPercentWidth(50.0);
            basePane.getColumnConstraints().get(1).setPercentWidth(50.0);
            basePane.getColumnConstraints().get(2).setPercentWidth(0.0);
        }

        selectedPlayerInfoTable.setVisible(show);
        selectedPlayerInfoTable.setManaged(show);
        selectedPlayerAchBox.setVisible(show);
        selectedPlayerAchBox.setManaged(show);
    }

    private void refreshSelectedPlayerTable(RankedCareer selected) {
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

            selectedPlayerGameTypesLabel.setText(
                    selected.getCareer().getPlayerPerson()
                            .getParticipateGames()
                            .keySet()
                            .stream()
                            .map(GameRule::toString)
                            .collect(Collectors.joining(", "))
            );

            selectedPlayerAchievements.setText(
                    getAchievements(cs, rankTypeBox.getValue()));

            if (selfStage != null
                    && !selfStage.isMaximized()
                    && (currentlyVisible != selectedPlayerInfoTable.isVisible()))
                selfStage.sizeToScene();
        }
    }

    private String getAchievements(List<ChampionshipScore> csList, GameRule gameRule) {
        int rankedChampions = 0;
        int totalChampions = 0;

        ChampionshipScore.Rank bestAnyRank = null;
        ChampionshipScore.Rank bestRankedRank = null;
        for (ChampionshipScore cs : csList) {
            if (cs.data.getType() == gameRule) {
                if (Util.arrayContains(cs.ranks, ChampionshipScore.Rank.CHAMPION)) {
                    totalChampions++;
                    if (cs.data.isRanked()) {
                        rankedChampions++;
                    }
                }
                for (ChampionshipScore.Rank r : cs.ranks) {
                    if (bestAnyRank == null || r.ordinal() < bestAnyRank.ordinal()) {
                        bestAnyRank = r;
                    }
                    if (cs.data.isRanked()) {
                        if (bestRankedRank == null || r.ordinal() < bestRankedRank.ordinal()) {
                            bestRankedRank = r;
                        }
                    }
                }
            }
        }

        StringBuilder builder = new StringBuilder();

        builder.append(String.format(strings.getString("achievementsOf"), gameRule.toString()))
                .append('\n');
        builder.append(strings.getString("bestAnyScore"))
                .append(bestAnyRank == null ? strings.getString("none") : bestAnyRank.getShown())
                .append('\n');
        builder.append(strings.getString("bestRankedScore"))
                .append(bestRankedRank == null ? strings.getString("none") : bestRankedRank.getShown())
                .append('\n');
        builder.append(strings.getString("numAnyChamps")).append(totalChampions).append('\n');
        builder.append(strings.getString("numRankedChamps")).append(rankedChampions);

        switch (gameRule) {
            case SNOOKER:
                List<ChampionshipData> threeBigData = ChampDataManager.getInstance().getSnookerTripleCrown();
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
                new LabelTableColumn<>(table,
                        ResourcesLoader.getInstance().createMoneyIcon(),
                        param -> {
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
                new LabelTableColumn<>(champAwardsTable,
                        param ->
                                new ReadOnlyStringWrapper(param.rank.getShown()));
        LabelTableColumn<AwardItem, Integer> awardCol =
                new LabelTableColumn<>(champAwardsTable,
                        ResourcesLoader.getInstance().createMoneyIcon(),
                        param ->
                                new ReadOnlyObjectWrapper<>(param.data.getAwardByRank(param.rank)));
        LabelTableColumn<AwardItem, Integer> perkCol =
                new LabelTableColumn<>(champAwardsTable,
                        ResourcesLoader.getInstance().createExpIcon(),
                        param ->
                                new ReadOnlyObjectWrapper<>(param.data.getExpByRank(param.rank)));

        champAwardsTable.addColumns(titleCol, awardCol, perkCol);
    }

    private void initTypeBox() {
        rankTypeBox.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            refreshRanks();
        }));
        rankTypeBox.getItems().addAll(
                GameRule.SNOOKER,
                GameRule.CHINESE_EIGHT,
                GameRule.AMERICAN_NINE
        );

        rankMethodBox.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue == ChampionshipData.Selection.ALL_CHAMP) {
                rankFirstDesCol.setText(strings.getString("rankNBigChamps"));
                rankSecondDesCol.setText(strings.getString("rankNChamps"));
            } else {
                rankFirstDesCol.setText(strings.getString("rankAwards"));
                rankSecondDesCol.setText(strings.getString("rankTotalAwards"));
            }
            refreshRanks();
        }));
        rankMethodBox.getItems().addAll(
                ChampionshipData.Selection.REGULAR,
                ChampionshipData.Selection.SINGLE_SEASON,
                ChampionshipData.Selection.ALL_CHAMP
        );

        rankMethodBox.getSelectionModel().select(ChampionshipData.Selection.REGULAR);
        rankTypeBox.getSelectionModel().select(GameRule.SNOOKER);
    }

    /**
     * @param type 1=奖金排行 2=段位排行
     */
    private void setRankTable(int type) {
        rankNameCol.setCellValueFactory(param ->
                new ReadOnlyStringWrapper(param.getValue().career.getPlayerPerson().getName()));

        if (type == 1) {
            rankCol.setText(strings.getString("rankRank"));
            rankFirstDesCol.setText(strings.getString("rankAwards"));
            rankSecondDesCol.setText(strings.getString("rankTotalAwards"));

            rankCol.setCellValueFactory(new PropertyValueFactory<>("rankFrom1"));
            rankFirstDesCol.setCellValueFactory(new PropertyValueFactory<>("shownAwards"));
            rankSecondDesCol.setCellValueFactory(new PropertyValueFactory<>("totalAwards"));
        } else if (type == 2) {
            rankCol.setText(strings.getString("rankTier"));
            rankFirstDesCol.setText(strings.getString("rankTotalWins"));
            rankSecondDesCol.setText(strings.getString("rankWinRate"));

            rankCol.setCellValueFactory(new PropertyValueFactory<>("tier"));
            rankFirstDesCol.setCellValueFactory(new PropertyValueFactory<>("totalWins"));
            rankSecondDesCol.setCellValueFactory(new PropertyValueFactory<>("winRate"));
        }
    }

    private void updateMoneyLabel(int money, int moneyCost) {
        int afterCost = money - moneyCost;
        String moneyStr = Util.moneyToReadable(money);
        if (moneyCost != 0) {
            moneyStr += " - " + moneyCost;
        }
        moneyLabel.setText(moneyStr);
        if (afterCost < 0) {
            moneyLabel.setTextFill(SPEND_MONEY_COLOR);
        } else {
            moneyLabel.setTextFill(Color.BLACK);
        }
    }

    public void refreshGui() {
        HumanCareer myCareer = careerManager.getHumanPlayerCareer();
        levelLabel.setText("Lv." + myCareer.getLevel());
        int curExp = myCareer.getExpInThisLevel();
        int expToNext = careerManager.getExpNeededToLevelUp(myCareer.getLevel());
        levelExpBar.setProgress(Math.min(1.0, (double) curExp / expToNext));
        levelExpLabel.setText(String.format("%d/%d", curExp, expToNext));

        boolean canLevelUp = myCareer.canLevelUp();
        levelUpBtn.setVisible(canLevelUp);
        levelUpBtn.setDisable(!canLevelUp);

        int money = careerManager.getHumanPlayerCareer().getMoney();
        int moneyCost = perkManager.getCost();
        updateMoneyLabel(money, moneyCost);

        int ach = AchManager.getInstance().getNCompletedAchievements();
        achievementsLabel.setText(String.valueOf(ach));

        refreshRanks();
        refreshPersonalAwardsTable();
        currentDateLabel.setText(
                String.format(strings.getString("currentDateFmt"),
                        CareerManager.calendarToString(careerManager.getTimestamp()))
        );

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

            boolean humanQualified = careerManager.humanPlayerQualifiedToJoin(data, data.getSelection());
            if (humanQualified) {
                joinChampBox.setDisable(false);
                joinChampBox.setSelected(true);
            } else {
                joinChampBox.setDisable(true);
                joinChampBox.setSelected(false);
            }
            nextChampionshipLabel.setText(nextData.fullName());
            int registryFee = careerManager.getHumanRegistryFee(data, humanQualified);
            registryFeeLabel.setText(String.valueOf(registryFee));
            int travelFee = data.getFlightFee() + data.getHotelFee();
            travelFeeLabel.setText(String.valueOf(travelFee));

            int fixedFees = 0;
            Map<String, Integer> feesMap = careerManager.getHumanPlayerCareer().calculateFixedFees(nextData);
            if (!feesMap.isEmpty()) {
                StringBuilder builder = new StringBuilder();
                for (Map.Entry<String, Integer> feesItem : feesMap.entrySet()) {
                    fixedFees += feesItem.getValue();
                    String label;
                    if (strings.containsKey(feesItem.getKey())) {
                        label = strings.getString(feesItem.getKey());
                    } else {
                        label = feesItem.getKey();
                    }
                    builder.append(label).append(" ").append(feesItem.getValue()).append(' ');
                }
                String sbs = builder.toString();
                otherFeeLabel1.setText(sbs);
                otherFeeLabel2.setText(sbs);
            } else {
                otherFeeLabel1.setText("");
                otherFeeLabel2.setText("");
            }

            totalFeeLabel.setText(String.valueOf(registryFee + travelFee + fixedFees));
            totalFeeLabel2.setText(String.valueOf(fixedFees));

            refreshFeesTexts(joinChampBox.isSelected());
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
        champAwardsTable.getColumns().getFirst().setTitleText(data.isRanked() ?
                strings.getString("rankedGame") :
                strings.getString("nonRankedGame"));

        fillChampionshipAwardTable(champAwardsTable, data);

        // 更新加点界面
        perkManager.synchronizePerks();
        notifyPerksChanged();
        abilityShower.notifyPerksReset();

        // 如果有还没显示的新奖金，就显示
        // 如果玩家在champDrawView完赛之后直接退了，那也懒得给他显了
        HumanCareer.AwardDistributionHint awd = myCareer.getAndRemoveUnShownAwd();
        if (awd != null) {
            showAwardAlert(awd);
        }
    }
    
    public static void fillChampionshipAwardTable(LabelTable<AwardItem> table, ChampionshipData data) {
        ChampionshipScore.Rank[] ranks = data.getRanksOfLosers();
        table.addItem(new CareerView.AwardItem(data, ChampionshipScore.Rank.CHAMPION, ChampionshipStage.FINAL));
        for (int i = 0; i < ranks.length; i++) {
            ChampionshipScore.Rank rank = ranks[i];
            ChampionshipStage stage = i == ranks.length - 1 ? null : data.getStages()[i + 1];
            table.addItem(new CareerView.AwardItem(data, rank, stage));
        }

        // 额外奖励
        int highestBreakAwd = data.getAwardByRank(ChampionshipScore.Rank.BEST_SINGLE);
        if (highestBreakAwd != 0) {
            table.addItem(new CareerView.AwardItem(data, ChampionshipScore.Rank.BEST_SINGLE, null));
        }
        int maximumAwd = data.getAwardByRank(ChampionshipScore.Rank.MAXIMUM);
        if (maximumAwd != 0) {
            table.addItem(new CareerView.AwardItem(data, ChampionshipScore.Rank.MAXIMUM, null));
        }
        int goldMaximumAwd = data.getAwardByRank(ChampionshipScore.Rank.GOLD_MAXIMUM);
        if (goldMaximumAwd != 0) {
            table.addItem(new CareerView.AwardItem(data, ChampionshipScore.Rank.GOLD_MAXIMUM, null));
        }
    }

    private void showAwardAlert(HumanCareer.AwardDistributionHint awd) {
        FXMLLoader loader = new FXMLLoader(
                Alert.class.getResource("alert.fxml"),
                App.getStrings()
        );
        Parent root;
        try {
            root = loader.load();
        } catch (IOException e) {
            EventLogger.error(e);
            return;
        }
        root.setStyle(App.FONT_STYLE);

        Stage newStage = new Stage();
        newStage.initOwner(selfStage);
        newStage.initModality(Modality.WINDOW_MODAL);
        newStage.setResizable(false);

        Scene scene = new Scene(root);
        newStage.setScene(scene);

        ResourcesLoader rl = ResourcesLoader.getInstance();

        Alert alert = loader.getController();
        alert.setupInfo(newStage,
                strings.getString("awardGained"),
                null);
        GridPane gp = new GridPane();
        gp.setVgap(10.0);
        gp.setHgap(5.0);
        ImageView moneyIv = new ImageView();
        rl.setIconImage(rl.getMoneyImg(), moneyIv);
        gp.addRow(0, moneyIv, new Label(Util.moneyToReadable(awd.money())));
        ImageView expIv = new ImageView();
        rl.setIconImage(rl.getExpImg(), expIv);
        gp.addRow(1, expIv, new Label(String.valueOf(awd.exp())));
        alert.setupAdditional(gp);

        AlertShower.setAutoClose(3000, alert);

        newStage.show();
    }

    private void refreshRanks() {
        if (rankTypeBox.getValue() == null || rankMethodBox.getValue() == null) return;

        GameRule selected = rankTypeBox.getValue();
        int type = switch (selected) {
            case SNOOKER, MINI_SNOOKER, SNOOKER_TEN, AMERICAN_NINE -> 1;
            case CHINESE_EIGHT, LIS_EIGHT -> 2;
        };
        setRankTable(type);

        rankingTable.getItems().clear();
        rankingTable.getItems().addAll(careerManager.getRanking(rankTypeBox.getValue(), rankMethodBox.getValue()));

        RankedCareer myRank = careerManager.humanPlayerRanking(rankTypeBox.getValue(), rankMethodBox.getValue());
        if (myRank == null) {
            myRankLabel.setText("");
        } else {
            if (type == 1) {
                myRankLabel.setText(String.format("%d  %s  %s  %s",
                        myRank.getRankFrom1(),
                        myRank.career.getPlayerPerson().getName(),
                        myRank.getShownAwards(),
                        myRank.getTotalAwards()));
            } else {
                myRankLabel.setText(String.format("%s  %s  %s  %s",
                        myRank.getTier(),
                        myRank.career.getPlayerPerson().getName(),
                        myRank.getTotalWins(),
                        myRank.getWinRate()));
            }
        }
    }

    @FXML
    @Override
    public void backAction() {
        CareerManager.closeInstance();
        CareerAchManager.closeCareerInstance();
        parent.refreshGui();

        super.backAction();
    }

    @FXML
    public void careerSettingsAction() {
        try {
            Stage popup = new Stage();
            popup.initOwner(selfStage);
            popup.initModality(Modality.WINDOW_MODAL);
            popup.getIcons().add(ResourcesLoader.getInstance().getIcon());

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("careerSettingsView.fxml"),
                    strings
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            Scene scene = App.createScene(root);
            popup.setScene(scene);

            CareerSettingsView view = loader.getController();
            view.setup(popup);

            popup.show();
        } catch (IOException e) {
            EventLogger.error(e);
        }
    }

    @FXML
    public void levelUpAction() throws IOException {
        HumanCareer myCareer = careerManager.getHumanPlayerCareer();

        int pastLevel = myCareer.getLevel();
        List<AwardMaterial> awardMaterials = myCareer.levelUp();
        int curLevel = myCareer.getLevel();
        if (awardMaterials != null) {
            int[] perkPool = myCareer.levelUpPerkRange(curLevel);
//            System.out.println("Possible perks: " + Arrays.toString(myCareer.levelUpPerkRange(curLevel)));
            String nPerk = awardMaterials.get(0).toString();

            FXMLLoader loader = new FXMLLoader(
                    Alert.class.getResource("alert.fxml"),
                    App.getStrings()
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            Stage newStage = new Stage();
            newStage.initOwner(selfStage);
            newStage.initStyle(StageStyle.UTILITY);
            newStage.initModality(Modality.WINDOW_MODAL);

            newStage.setOnHidden(e -> refreshGui());

            Scene scene = App.createScene(root);
            newStage.setScene(scene);

            Alert view = loader.getController();

            StringBuilder perkPoolStr = new StringBuilder();
            perkPoolStr.append(strings.getString("perk"))
                    .append(": ")
                    .append(Arrays.stream(perkPool)
                            .mapToObj(String::valueOf)
                            .collect(Collectors.joining("/")));

            view.functionalWindow(
                    newStage,
                    String.format("Lv. %d->%d", pastLevel, curLevel),
                    String.format(strings.getString("levelUpAwardPool"), perkPoolStr),
                    strings.getString("clickToRandomPerk"),
                    () -> {
                        refreshGui();
                        view.setHeaderText(
                                String.format(
                                        strings.getString("levelUpPerkCongrat"),
                                        nPerk
                                ));
                        view.setContentText(
                                strings.getString("levelUpDes")
                        );
                        view.setPositiveButton(
                                strings.getString("close"),
                                view::close
                        );
                    }
            );

            newStage.showAndWait();
        }
    }

    @FXML
    public void clearUsedPerks() {
        perkManager.clearSelections();
        abilityShower.notifyPerksReset();

        notifyPerksChanged();
    }

    @FXML
    public void applyPerksAction() {
        int curMoney = careerManager.getHumanPlayerCareer().getMoney();
        int price = perkManager.getCost();
        int moneyAfterBuy = curMoney - price;
        int perksUse = perkManager.getPerksSelected();
        if (perksUse > 0) {
            AlertShower.askConfirmation(
                    selfStage,
                    String.format(strings.getString("balanceAfterApplyPerk"),
                            Util.moneyToReadable(curMoney),
                            Util.moneyToReadable(price),
                            Util.moneyToReadable(moneyAfterBuy)
                    ),
                    String.format(strings.getString("confirmApplyPerk"),
                            perksUse),
                    this::applyPerks,
                    null
            );
        }
    }
    
    private void applyPerks() {
        PerkManager.UpgradeRec used = perkManager.applyPerks();  // 在getCost之后

        careerManager.getHumanPlayerCareer().recordUpgradeAndUsePerk(used);

//        DataLoader.getInstance().updatePlayer(perkManager.getAbility().toPlayerPerson());
        careerManager.reloadHumanPlayerPerson();
        careerManager.saveToDisk();

        notifyPerksChanged();

        abilityShower.notifyPerksReset();
    }

    @FXML
    public void invoiceAction() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("cashFlowView.fxml"),
                    strings
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            CashFlowView view = loader.getController();
            view.setParent(selfStage.getScene());

            view.setup(selfStage, careerManager.getHumanPlayerCareer());
            
            App.setRoot(root);
        } catch (IOException e) {
            EventLogger.error(e);
        }
    }

    @FXML
    public void achievementsAction() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("achievementsView.fxml"),
                    strings
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            AchievementsView view = loader.getController();
            view.setParent(selfStage.getScene());

            view.setup(selfStage, this);
            App.setRoot(root);
        } catch (IOException e) {
            EventLogger.error(e);
        }
    }

    @FXML
    public void inventoryAction() {
        showInventoryStore(true);
    }

    @FXML
    public void storeAction() {
        showInventoryStore(false);
    }

    @FXML
    public void trainingChallengeAction() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("careerTrainingView.fxml"),
                    strings
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            CareerTrainingView view = loader.getController();
            view.setParent(selfStage.getScene());
            view.setup(selfStage, careerManager.getHumanPlayerCareer(), this);

            App.setRoot(root);
        } catch (IOException e) {
            EventLogger.error(e);
        }
    }

    @FXML
    public void seeToursListAction() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("tournamentsViewer.fxml"),
                    strings
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

//            Scene scene = new Scene(root, -1, -1, false, SceneAntialiasing.BALANCED);
//            scene.getStylesheets().add(getClass().getResource("/trashsoftware/trashSnooker/css/font.css").toExternalForm());

            TournamentsViewer viewer = loader.getController();
            viewer.setParent(selfStage.getScene());
            viewer.initialSelection(activeOrNext);

            App.setRoot(root);
        } catch (IOException e) {
            EventLogger.error(e);
        }
    }
    
    @FXML
    public void showCareerRankHistory() {
        RankedCareer selected = rankingTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            try {
                FXMLLoader loader = new FXMLLoader(
                        getClass().getResource("rankHistoryView.fxml"),
                        strings
                );
                Parent root = loader.load();
                root.setStyle(App.FONT_STYLE);

                RankHistoryView viewer = loader.getController();
                viewer.setParent(selfStage.getScene());
                viewer.setup(selected.getCareer(), rankTypeBox.getValue());

                App.setRoot(root);
            } catch (IOException e) {
                EventLogger.error(e);
            }
        }
    }

    @FXML
    public void nextChamp() {
        HumanCareer humanCareer = careerManager.getHumanPlayerCareer();
        ChampionshipData.WithYear nextData = careerManager.nextChampionshipData();

        humanCareer.updateMoneyChampStart(nextData);  // 扣生活费

        Championship championship = careerManager.startNextChampionship();
        boolean humanJoin = !joinChampBox.isDisabled() && joinChampBox.isSelected();

        championship.startChampionship(joinChampBox.isSelected(), !joinChampBox.isDisabled());

        if (humanJoin) {
            humanCareer.receiveInviteAward(championship);  // 接收邀请金
            humanCareer.payParticipateFees(championship);  // 扣报名费、住宿费、机票
        }

        CareerManager.getInstance().saveToDisk();

        refreshGui();

        showChampDrawView();
    }

    @FXML
    public void skipNextChamp() {
        if (!joinChampBox.isSelected()) {
            careerManager.getHumanPlayerCareer().updateMoneyChampStart(careerManager.nextChampionshipData());  // 扣生活费

            Championship championship = careerManager.startNextChampionship();

            championship.startChampionship(false, !joinChampBox.isDisabled());

            while (!championship.isFinished()) {
                championship.startNextRound();
            }

            CareerManager.getInstance().saveToDisk();

            refreshGui();
        }
    }

    @FXML
    public void continueChampInProg() {
        showChampDrawView();
    }

    private void showInventoryStore(boolean isInventory) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("inventoryView.fxml"),
                    strings
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            InventoryView view = loader.getController();
            view.setStage(selfStage);
            view.setParent(selfStage.getScene());

            view.setup(isInventory);
            App.setRoot(root);
        } catch (IOException e) {
            EventLogger.error(e);
        }
    }

    private void showChampDrawView() {
        try {
            selfStage.setTitle(CareerManager.getInstance().getChampionshipInProgress().fullName());

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("champDrawView.fxml"),
                    strings
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            ChampDrawView view = loader.getController();
            view.setup(this, selfStage);
            view.setParent(selfStage.getScene());
//            mainView.setup(parentStage, stage);

            App.setRoot(root);
//            Scene scene = App.createScene(root);

//            Scene scene = new Scene(root, -1, -1, false, SceneAntialiasing.BALANCED);
//            scene.getStylesheets().add(getClass().getResource("/trashsoftware/trashSnooker/css/font.css").toExternalForm());
//            selfStage.setScene(scene);

//            selfStage.show();
        } catch (IOException e) {
            EventLogger.error(e);
        }
    }

    public void notifyPerksChanged() {
        availPerksLabel.setText(String.format(strings.getString("availPerksFmt"), perkManager.getAvailPerks()));
        int money = careerManager.getHumanPlayerCareer().getMoney();
        int moneyCost = perkManager.getCost();
        updateMoneyLabel(money, moneyCost);

        confirmAddPerkBtn.setDisable(moneyCost > money || perkManager.getPerksSelected() == 0);
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
