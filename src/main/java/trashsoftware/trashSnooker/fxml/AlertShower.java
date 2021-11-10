package trashsoftware.trashSnooker.fxml;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.stage.Window;

import java.util.Optional;

public class AlertShower {
    
    public static boolean askConfirmation(Window owner, String content, String header) {
        return askConfirmation(owner, content, header, "是", "否");
    }

    public static boolean askConfirmation(Window owner, String content, String header,
                                          String positiveText, String negativeText) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, content,
                new ButtonType(positiveText, ButtonBar.ButtonData.YES),
                new ButtonType(negativeText, ButtonBar.ButtonData.NO));
        alert.initOwner(owner);
        alert.setHeaderText(header);

        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get().getButtonData() == ButtonBar.ButtonData.YES;
    }

    public static void showInfo(Window owner, String content, String header) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION, content);
        alert.initOwner(owner);
        alert.setHeaderText(header);

        alert.showAndWait();
    }
}
