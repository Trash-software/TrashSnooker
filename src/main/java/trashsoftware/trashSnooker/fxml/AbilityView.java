package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import trashsoftware.trashSnooker.core.PlayerPerson;

import java.net.URL;
import java.util.ResourceBundle;

public class AbilityView implements Initializable {

    @FXML
    Label nameLabel, categoryLabel;

    @FXML
    Label aimingLabel, cuePrecisionLabel, powerLabel, spinLabel, powerControlLabel,
            spinControlLabel;

    @FXML
    ProgressBar aimingBar, cuePrecisionBar, powerBar, spinBar, powerControlBar,
            spinControlBar;

    private PlayerPerson playerPerson;

    public static String numToString(double d) {
        return d == (long) d ? String.format("%d", (long) d) : String.format("%.1f", d);
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    public void setup(PlayerPerson playerPerson) {
        this.playerPerson = playerPerson;

        nameLabel.setText(playerPerson.getName());

        if ("Professional".equals(playerPerson.category)) {
            categoryLabel.setText("职业选手");
        } else if ("Amateur".equals(playerPerson.category)) {
            categoryLabel.setText("业余选手");
        } else if ("Noob".equals(playerPerson.category)) {
            categoryLabel.setText("菜鸡");
        }

        aimingLabel.setText(numToString(playerPerson.getPrecisionPercentage()));
        aimingBar.setProgress(playerPerson.getPrecisionPercentage() / 100.0);

        double cuePrecision =
                Math.max(0, 
                        1.0 - (playerPerson.getCuePointMuSigmaXY()[0] + 
                                playerPerson.getCuePointMuSigmaXY()[1]) / 40.0);
        
        cuePrecisionLabel.setText(numToString(cuePrecision * 100.0));
        cuePrecisionBar.setProgress(cuePrecision);

        powerLabel.setText(String.format("%s/%s",
                numToString(playerPerson.getControllablePowerPercentage()),
                numToString(playerPerson.getMaxPowerPercentage())));
        powerBar.setProgress(playerPerson.getControllablePowerPercentage() / 100.0);

        aimingLabel.setText(numToString(playerPerson.getPrecisionPercentage()));
        aimingBar.setProgress(playerPerson.getPrecisionPercentage() / 100.0);
        
        powerControlLabel.setText(numToString(playerPerson.getPowerControl()));
        powerControlBar.setProgress(playerPerson.getPowerControl() / 100.0);
        
        spinLabel.setText(numToString(playerPerson.getMaxSpinPercentage()));
        spinBar.setProgress(playerPerson.getMaxSpinPercentage() / 100.0);
        
        double spinControl = Math.max(0,
                1.0 - (playerPerson.getCuePointMuSigmaXY()[0] +
                        playerPerson.getCuePointMuSigmaXY()[1] + 
                        playerPerson.getCuePointMuSigmaXY()[2] +
                        playerPerson.getCuePointMuSigmaXY()[3]) / 80.0);
        spinControlLabel.setText(numToString(spinControl * 100.0));
        spinControlBar.setProgress(spinControl);
    }
}
