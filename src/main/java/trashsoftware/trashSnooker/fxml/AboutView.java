package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import trashsoftware.trashSnooker.util.DataLoader;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

public class AboutView implements Initializable {
    @FXML
    Label versionLabel;
    @FXML
    Label developerNameLabel, developerContactLabel;
    @FXML
    Label designConsultantNameLabel, designConsultantContactLabel;
    @FXML
    Label closedBetaPlayersLabel;
    
    public static final Map<String, String[]> DEVELOPER_NAME = Map.of(
            "zh", new String[]{"张博涵 @垃圾软件工作室"},
            "en", new String[]{"Bohan Zhang @Trash software studio"}
    );

    public static final Map<String, String[]> CONSULTANTS_NAME = Map.of(
            "zh", new String[]{"李可冉", "吴浩博"},
            "en", new String[]{"Keran Li", "Haobo Wu"}
    );

    public static final Map<String, String[]> CLOSED_BETA_USERS_NAME = Map.of(
            "zh", new String[]{"Atom张", "陈哥"},
            "en", new String[]{"Atom Zhang", "Cheng Brother"}
    );
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        versionLabel.setText(String.format("%s-%s", App.VERSION_NAME, App.CLASSIFIER));
        
        setLabelForMultipleNames(DEVELOPER_NAME, developerNameLabel);
        setLabelForMultipleNames(CONSULTANTS_NAME, designConsultantNameLabel);
        setLabelForMultipleNames(CLOSED_BETA_USERS_NAME, closedBetaPlayersLabel);
    }
    
    private void setLabelForMultipleNames(Map<String, String[]> content, Label label) {
        String[] arr = DataLoader.getObjectOfLocale(content);
        String string = String.join("\n", arr);
        label.setText(string);
    }
}
