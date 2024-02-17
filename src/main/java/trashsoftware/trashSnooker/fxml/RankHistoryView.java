package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.util.StringConverter;
import trashsoftware.trashSnooker.core.career.*;
import trashsoftware.trashSnooker.core.metrics.GameRule;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class RankHistoryView extends ChildInitializable {

    @FXML
    Label nameLabel;
    @FXML
    ComboBox<GameRule> rankTypeBox;
    @FXML
    LineChart<String, Number> scoreChart;
    @FXML
    NumberAxis scoreAxis;
    @FXML
    CategoryAxis matchAxis;
    @FXML
    LineChart<String, Number> rankChart;
    @FXML
    NumberAxis rankAxis;
    @FXML
    CategoryAxis matchAxis2;

    private Career career;

    private ResourceBundle strings;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.strings = resourceBundle;

        rankTypeBox.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            load();
        }));
        rankTypeBox.getItems().addAll(
                GameRule.SNOOKER,
                GameRule.CHINESE_EIGHT,
                GameRule.AMERICAN_NINE
        );

        scoreAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number object) {
                int index = object.intValue();
                if (index > 0) return "";
                ChampionshipScore.Rank rank = ChampionshipScore.Rank.values()[-index];
                return rank.ranked ? rank.getShown() : "";
            }

            @Override
            public Number fromString(String string) {
                int index = Arrays.stream(ChampionshipScore.Rank.values())
                        .map(ChampionshipScore.Rank::getShown).toList().indexOf(string);
                return -index;
            }
        });
        
        rankAxis.setTickLabelFormatter(new StringConverter<>() {
            @Override
            public String toString(Number object) {
                return String.valueOf(Math.abs(object.intValue()));
            }

            @Override
            public Number fromString(String string) {
                return Integer.parseInt(string);
            }
        });

        scoreAxis.setUpperBound(1);
        scoreAxis.setLowerBound(-ChampionshipScore.Rank.getAllRanked().length);
        scoreAxis.setAutoRanging(false);
        scoreAxis.setTickUnit(1.0);
        
        rankAxis.setForceZeroInRange(false);
        rankAxis.setTickUnit(1.0);
    }

    public void setup(Career career, GameRule initType) {
        this.career = career;

        nameLabel.setText(career.getPlayerPerson().getName());
        rankTypeBox.getSelectionModel().select(initType);
    }

    private void load() {
        List<ChampionshipScore> championshipScores = career.getChampionshipScores();
        GameRule type = rankTypeBox.getValue();

        List<ChampionshipScore> thisType = new ArrayList<>();
        for (ChampionshipScore cs : championshipScores) {
            var data = cs.data;
            if (data.getType() == type) {
                thisType.add(cs);
            }
        }

        drawScores(thisType);
        drawRanks();
    }

    private void drawScores(List<ChampionshipScore> scores) {
        scoreChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        for (ChampionshipScore cs : scores) {
            ChampionshipData data = cs.data;
            int year = cs.getYear();
            ChampionshipData.WithYear withYear = data.getWithYear(year);

            for (ChampionshipScore.Rank rank : cs.ranks) {
                if (rank.ranked) {
                    int ranker = -rank.ordinal();
                    series.getData().add(new XYChart.Data<>(withYear.fullName(), ranker));
                    break;
                }
            }
        }

        scoreChart.getData().add(series);
    }

    private void drawRanks() {
        rankChart.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();

        var careerRanks = career.getHistoryRank(rankTypeBox.getValue());
        if (careerRanks.isEmpty()) return;

        CareerRanker firstVal = careerRanks.get(careerRanks.firstKey());
        if (firstVal instanceof CareerRanker.ByAwards) {
            int worstRank = 0;
            int bestRank = CareerManager.getInstance().getNCareers();
            
            for (var entry : careerRanks.entrySet()) {
                CareerRanker.ByAwards byAwards = (CareerRanker.ByAwards) entry.getValue();
                int rank = byAwards.getRankFrom1();
                series.getData().add(new XYChart.Data<>(entry.getKey().fullName(), -rank));
                if (rank > worstRank) worstRank = rank;
                if (rank < bestRank) bestRank = rank;
            }
            if (worstRank - bestRank < 10) {
                int midRank;
                if (bestRank < 6) {
                    midRank = 5;
                } else {
                    midRank = (worstRank + bestRank) / 2;
                }
                rankAxis.setAutoRanging(false);
                rankAxis.setLowerBound(-midRank - 5);
                rankAxis.setUpperBound(-midRank + 5);
            } else {
                rankAxis.setAutoRanging(true);
            }
        } else if (firstVal instanceof CareerRanker.ByTier) {
            rankAxis.setLowerBound(0);
            rankAxis.setUpperBound(12);
            rankAxis.setAutoRanging(false);
            for (var entry : careerRanks.entrySet()) {
                CareerRanker.ByTier byTier = (CareerRanker.ByTier) entry.getValue();
                series.getData().add(new XYChart.Data<>(entry.getKey().fullName(), byTier.getTier()));
            }
        }

        rankChart.getData().add(series);
    }
}
