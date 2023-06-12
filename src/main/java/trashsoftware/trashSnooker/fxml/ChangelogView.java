package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import trashsoftware.trashSnooker.util.EventLogger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.ResourceBundle;

public class ChangelogView implements Initializable {
    @FXML
    Label changelogText;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        fillChangelog();
    }

    private void fillChangelog() {
        changelogText.setText(loadChangelog());
    }

    /**
     * @return the changelog text stored in resources directory.
     */
    private String loadChangelog() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(
                Objects.requireNonNull(
                        getClass().getResourceAsStream("/trashsoftware/trashSnooker/doc/changelog.txt")),
                StandardCharsets.UTF_8))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        } catch (IOException | NullPointerException e) {
            EventLogger.error(e);
        }
        return "";
    }
}
