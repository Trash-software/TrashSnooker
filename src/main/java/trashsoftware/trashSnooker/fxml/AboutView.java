package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import trashsoftware.trashSnooker.res.ResourcesLoader;
import trashsoftware.trashSnooker.util.DataLoader;

import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;

public class AboutView implements Initializable {
    @FXML
    ImageView iconView;
    @FXML
    Label versionLabel;
    @FXML
    Label developTeamLabel;
    @FXML
    Label developerNameLabel, developerContactLabel;
    @FXML
    Label artDesignerNameLabel, artDesignerContactLabel;
    @FXML
    Label designConsultantNameLabel, designConsultantContactLabel;
    @FXML
    Label closedBetaPlayersLabel;
    
    public static final Map<String, String[]> DEVELOPER_NAME = Map.of(
            "zh", new String[]{"张博涵"},
            "en", new String[]{"Bohan Zhang"}
    );

    public static final Map<String, String[]> ART_DESIGNER_NAMES = Map.of(
            "zh", new String[]{"KR", "奥特曼"},
            "en", new String[]{"KR", "Autumn"}
    );

    public static final Map<String, String[]> CONSULTANTS_NAME = Map.of(
            "zh", new String[]{"No Shell"},
            "en", new String[]{"No Shell"}
    );

    public static final Map<String, String[]> CLOSED_BETA_USERS_NAME = Map.of(
            "zh", new String[]{"Purple Fat", "Atom张", "陈哥"},
            "en", new String[]{"Purple Fat", "Atom Zhang", "Cheng Brother"}
    );
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        versionLabel.setText(String.format("%s-%s", App.VERSION_NAME, App.CLASSIFIER));
        
        developTeamLabel.setText(resourceBundle.getString("developTeam") + " @" + resourceBundle.getString("studioName"));
        
        setLabelForMultipleNames(DEVELOPER_NAME, developerNameLabel);
        setLabelForMultipleNames(ART_DESIGNER_NAMES, artDesignerNameLabel);
        setLabelForMultipleNames(CONSULTANTS_NAME, designConsultantNameLabel);
        setLabelForMultipleNames(CLOSED_BETA_USERS_NAME, closedBetaPlayersLabel);
        
//        developerContactLabel.setText("2676147693@qq.com");
        
        iconView.setImage(ResourcesLoader.getInstance().getIcon());
    }
    
    private void setLabelForMultipleNames(Map<String, String[]> content, Label label) {
        String[] arr = DataLoader.getObjectOfLocale(content);
        String string = String.join("\n", arr);
        label.setText(string);
    }
}
