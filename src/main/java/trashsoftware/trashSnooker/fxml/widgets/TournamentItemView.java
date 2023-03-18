package trashsoftware.trashSnooker.fxml.widgets;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import trashsoftware.trashSnooker.core.career.*;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.fxml.CareerView;

import java.io.IOException;
import java.util.*;

public class TournamentItemView extends ScrollPane {

    @FXML
    GridPane rootPane;

    LabelTable<CareerView.AwardItem> awardPerkTable;
    LabelTable<HistoryChamp> champsTable;

    private ResourceBundle strings;
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
        rootPane.add(new Label(data.getName()), 0, row++);

        HBox tourTypeBox = new HBox();
        tourTypeBox.setSpacing(10.0);
        tourTypeBox.getChildren().add(new Label(strings.getString("isRanked") + (data.isRanked() ?
                        strings.getString("yes") :
                        strings.getString("no"))));
        tourTypeBox.getChildren().add(new Label(strings.getString("isProfessional") + (data.isProfessionalOnly() ? 
                strings.getString("yes") : 
                strings.getString("no"))));

        rootPane.add(tourTypeBox, 0, row++);

        if (data.getDescription().length() > 0) {
            Label des = new Label(data.getDescription());
            des.setWrapText(true);
            rootPane.add(des, 0, row++);
        }
        if (data.getSponsor().length() > 0) {
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
        rootPane.add(champsTable, 0, row++);

        // 赛事奖金表
        LabelTableColumn<CareerView.AwardItem, String> titleCol =
                new LabelTableColumn<>(awardPerkTable, "", param ->
                        new ReadOnlyStringWrapper(param.rank.getShown()));
        LabelTableColumn<CareerView.AwardItem, Integer> awardCol =
                new LabelTableColumn<>(awardPerkTable, strings.getString("awards"), param ->
                        new ReadOnlyObjectWrapper<>(param.data.getAwardByRank(param.rank)));
        LabelTableColumn<CareerView.AwardItem, Integer> perkCol =
                new LabelTableColumn<>(awardPerkTable, "exp", param ->
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
        
        // 历届冠军
        LabelTableColumn<HistoryChamp, Integer> yearCol =
                new LabelTableColumn<>(champsTable, strings.getString("historyChampions"), param ->
                        new ReadOnlyObjectWrapper<>(param.year));
        LabelTableColumn<HistoryChamp, String> personCol =
                new LabelTableColumn<>(champsTable, "", param ->
                        new ReadOnlyStringWrapper(param.career.getPlayerPerson().getName()));

        champsTable.addColumns(yearCol, personCol);
        
        NavigableMap<Integer, Career> historyChamps = CareerManager.getInstance().historicalChampions(data);
        
        for (int year : historyChamps.descendingKeySet()) {
            champsTable.addItem(new HistoryChamp(year, historyChamps.get(year)));
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
