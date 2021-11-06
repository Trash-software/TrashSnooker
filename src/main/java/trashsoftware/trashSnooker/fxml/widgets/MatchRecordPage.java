package trashsoftware.trashSnooker.fxml.widgets;

import javafx.fxml.FXMLLoader;

import java.io.IOException;

public class MatchRecordPage extends StatsPage {

    public MatchRecordPage() {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                "matchRecordPage.fxml"));
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }
}
