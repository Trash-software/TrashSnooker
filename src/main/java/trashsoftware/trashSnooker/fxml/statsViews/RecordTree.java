package trashsoftware.trashSnooker.fxml.statsViews;

import javafx.scene.layout.Pane;

import java.util.ResourceBundle;

public class RecordTree {
    protected final ResourceBundle strings;
    protected final String shown;

    RecordTree(String shown, ResourceBundle strings) {
        this.shown = shown;
        this.strings = strings;
    }

    void setRightPane(Pane rightPane) {
        rightPane.getChildren().clear();
    }

    @Override
    public String toString() {
        return shown;
    }
}
