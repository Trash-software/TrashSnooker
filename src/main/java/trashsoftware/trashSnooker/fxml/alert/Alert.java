package trashsoftware.trashSnooker.fxml.alert;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;

public class Alert implements Initializable {
    protected Stage stage;
    @FXML
    Label headerText, contentText;
    @FXML
    Button yesButton, noButton;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {

    }

    public void setupInfo(Stage stage,
                          String header, String content) {
        this.stage = stage;

        this.headerText.setText(header);
        this.contentText.setText(content);
        this.noButton.setVisible(false);
        this.noButton.setManaged(false);
        this.yesButton.setOnAction(e -> stage.close());
    }

    public void setupConfirm(Stage stage, String header, String content,
                             String yesText, String noText,
                             Runnable yes, Runnable no) {
        this.stage = stage;

        this.headerText.setText(header);
        this.contentText.setText(content);
        this.yesButton.setText(yesText);
        this.yesButton.setOnAction(e -> {
            stage.close();
            if (yes != null) Platform.runLater(yes);
        });
        this.noButton.setText(noText);
        this.noButton.setOnAction(e -> {
            stage.close();
            if (no != null) Platform.runLater(no);
        });
    }
}
