package trashsoftware.trashSnooker.fxml.widgets;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Orientation;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import trashsoftware.trashSnooker.core.career.*;
import trashsoftware.trashSnooker.core.career.championship.SnookerBreakScore;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.fxml.CareerView;
import trashsoftware.trashSnooker.res.ResourcesLoader;
import trashsoftware.trashSnooker.util.DataLoader;

import java.io.IOException;
import java.util.*;

public class TournamentItemView extends ScrollPane {

    @FXML
    GridPane rootPane;

    LabelTable<CareerView.AwardItem> awardPerkTable;
    LabelTable<HistoryChamp> champsTable;

    private final ResourceBundle strings;
    private ChampionshipData data;

    public TournamentItemView() {
        this(App.getStrings());
    }

    public TournamentItemView(ResourceBundle strings) {
        super();

        this.strings = strings;

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                "tournamentItemView.fxml"), strings);
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void setData(ChampionshipData data) {
        this.data = data;

        setup();
    }

    private void setup() {
        awardPerkTable = new LabelTable<>();

        champsTable = new LabelTable<>();

        int row = 0;
        Label nameLabel = new Label(data.getName());
        nameLabel.setFont(new Font(App.FONT.getName(), 18));
        rootPane.add(nameLabel, 0, row++);

        HBox tourTypeBox = new HBox();
        tourTypeBox.setSpacing(10.0);
        tourTypeBox.getChildren().add(new Label(strings.getString("isRanked") + (data.isRanked() ?
                        strings.getString("yes") :
                        strings.getString("no"))));
        tourTypeBox.getChildren().add(new Label(strings.getString("isProfessional") + (data.isProfessionalOnly() ? 
                strings.getString("yes") : 
                strings.getString("no"))));

        rootPane.add(tourTypeBox, 0, row++);

        if (!data.getDescription().isEmpty()) {
            Label des = new Label(data.getDescription());
            des.setWrapText(true);
            rootPane.add(des, 0, row++);
        }
        if (!data.getSponsor().isEmpty()) {
            Label sponsor = new Label(strings.getString("sponsor") + ": " + data.getSponsor());
            sponsor.setWrapText(true);
            rootPane.add(sponsor, 0, row++);
        }
        
        LabelTable<ChampionshipData> positionsTable = new LabelTable<>();
        LabelTableColumn<ChampionshipData, Integer> col1 = 
                new LabelTableColumn<>(positionsTable, strings.getString("mainPlaces"), 
                        params -> new ReadOnlyObjectWrapper<>(params.getMainPlaces()));
        LabelTableColumn<ChampionshipData, Integer> col2 =
                new LabelTableColumn<>(positionsTable, strings.getString("seedPlaces"),
                        params -> new ReadOnlyObjectWrapper<>(params.getSeedPlaces()));
        LabelTableColumn<ChampionshipData, Integer> col3 =
                new LabelTableColumn<>(positionsTable, strings.getString("preMatchPlaces"),
                        params -> new ReadOnlyObjectWrapper<>(Arrays.stream(params.getPreMatchNewAdded()).sum()));
        
        positionsTable.addColumns(col1, col2, col3);
        positionsTable.addItem(data);

        rootPane.add(positionsTable, 0, row++);
        
        rootPane.add(awardPerkTable, 0, row++);

        // 赛事奖金表
        LabelTableColumn<CareerView.AwardItem, String> titleCol =
                new LabelTableColumn<>(awardPerkTable, "", param ->
                        new ReadOnlyStringWrapper(param.rank.getShown()));
        LabelTableColumn<CareerView.AwardItem, Integer> awardCol =
                new LabelTableColumn<>(awardPerkTable,
                        ResourcesLoader.getInstance().createMoneyIcon(), 
                        param ->
                        new ReadOnlyObjectWrapper<>(param.data.getAwardByRank(param.rank)));
        LabelTableColumn<CareerView.AwardItem, Integer> perkCol =
                new LabelTableColumn<>(awardPerkTable,
                        ResourcesLoader.getInstance().createExpIcon(), 
                        param ->
                        new ReadOnlyObjectWrapper<>(param.data.getExpByRank(param.rank)));
        LabelTableColumn<CareerView.AwardItem, String> framesCol =
                new LabelTableColumn<>(awardPerkTable, strings.getString("totalFrames"), param ->
                        new ReadOnlyStringWrapper(param.winnerStage == null ? 
                                "" : 
                                String.valueOf(param.data.getNFramesOfStage(param.winnerStage))));

        awardPerkTable.addColumns(titleCol, awardCol, perkCol, framesCol);

        ChampionshipScore.Rank[] ranks = data.getRanksOfLosers();
        awardPerkTable.addItem(new CareerView.AwardItem(data, ChampionshipScore.Rank.CHAMPION, ChampionshipStage.FINAL));
        for (int i = 0; i < ranks.length; i++) {
            ChampionshipScore.Rank rank = ranks[i];
            ChampionshipStage stage = i == ranks.length - 1 ? null : data.getStages()[i + 1];
            awardPerkTable.addItem(new CareerView.AwardItem(data, rank, stage));
        }
        
        // 赛事报名费表
        LabelTable<ChampionshipData> feesTable = new LabelTable<>();
        LabelTableColumn<ChampionshipData, Integer> feesCol1 =
                new LabelTableColumn<>(feesTable, strings.getString("registryFee"),
                        params -> new ReadOnlyObjectWrapper<>(params.getRegistryFee()));
        LabelTableColumn<ChampionshipData, Integer> feesCol2 =
                new LabelTableColumn<>(feesTable, strings.getString("flightFee"),
                        params -> new ReadOnlyObjectWrapper<>(params.getFlightFee()));
        LabelTableColumn<ChampionshipData, Integer> feesCol3 =
                new LabelTableColumn<>(feesTable, strings.getString("hotelFee"),
                        params -> new ReadOnlyObjectWrapper<>(params.getHotelFee()));

        feesTable.addColumns(feesCol1, feesCol2, feesCol3);
        feesTable.addItem(data);
        
        rootPane.add(feesTable, 0, row++);
        rootPane.add(new Separator(Orientation.HORIZONTAL), 0, row++, 1, 1);
        
        // 历届冠军
        LabelTableColumn<HistoryChamp, Integer> yearCol =
                new LabelTableColumn<>(champsTable, strings.getString("historyChampions"), param ->
                        new ReadOnlyObjectWrapper<>(param.year));
        LabelTableColumn<HistoryChamp, String> personCol =
                new LabelTableColumn<>(champsTable, "", param ->
                        new ReadOnlyStringWrapper(param.career.getPlayerPerson().getName()));

        champsTable.addColumns(yearCol, personCol);
        rootPane.add(champsTable, 0, row++);
        
        TournamentStats stats = CareerManager.getInstance().tournamentHistory(data);

        NavigableMap<Integer, Career> historyChamps = stats.getHistoricalChampions();
        
        for (int year : historyChamps.descendingKeySet()) {
            champsTable.addItem(new HistoryChamp(year, historyChamps.get(year)));
        }
        
        if (data.getType().snookerLike()) {
            LabelTable<SnookerBreakScore> sbsTable = new LabelTable<>();
            Set<SnookerBreakScore> bestBreaks = stats.getHighestBreak();
            if (bestBreaks == null || bestBreaks.isEmpty()) {
                sbsTable.addColumn(
                        new LabelTableColumn<>(sbsTable, strings.getString("tournamentHighest"),
                                param -> new ReadOnlyStringWrapper("--"))
                );
                sbsTable.addItem(null);
            } else {
                sbsTable.addColumns(
                        new LabelTableColumn<>(sbsTable, strings.getString("tournamentHighest"), 
                                param -> new ReadOnlyStringWrapper(DataLoader.getInstance().getPlayerPerson(param.playerId).getName())),
                        new LabelTableColumn<>(sbsTable,
                                param -> new ReadOnlyObjectWrapper<>(param.score)),
                        new LabelTableColumn<>(sbsTable,
                                param -> new ReadOnlyStringWrapper(param.getYearElseEmpty() + param.stage.getShown()))
                );
                sbsTable.addItems(bestBreaks);
            }
            rootPane.add(sbsTable, 0, row++);
        }
    }
    
    public static class HistoryChamp {
        final int year;
        final Career career;
        
        HistoryChamp(int year, Career career) {
            this.year = year;
            this.career = career;
        }
    }
}
