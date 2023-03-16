package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import trashsoftware.trashSnooker.core.career.Career;
import trashsoftware.trashSnooker.core.career.ChampionshipScore;
import trashsoftware.trashSnooker.core.career.championship.Championship;
import trashsoftware.trashSnooker.util.Util;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

public class CongratView implements Initializable {
    @FXML
    Label playerNameLabel, sentenceLabel;
    
    private ResourceBundle strings;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.strings = resourceBundle;
    }
    
    public void setup(ChampionshipScore.Rank rank, Career human, Championship championship) {
        String champStr = championship.fullName();
        String congratStr;
        
        if (rank.ordinal() <= ChampionshipScore.Rank.TOP_64.ordinal()) {
            congratStr = Util.formatSentence(
                    strings.getString("congratSentence"),
                    Map.of("rank", rank.getShown(),
                            "match", champStr)
            );
        } else {
            congratStr = Util.formatSentence(
                    strings.getString("notCongratSentence"),
                    Map.of("rank", rank.getShown(),
                            "match", champStr)
            );
        }
        
        String probKey = Util.toLowerCamelCase("CONGRAT_" + rank.name());
        
        String nameText;
        if (strings.containsKey(probKey)) {
            nameText = String.format(strings.getString(probKey), human.getPlayerPerson().getName());
        } else {
            nameText = human.getPlayerPerson().getName();
        }
        
        playerNameLabel.setText(nameText);
        sentenceLabel.setText(congratStr);
    }
}
