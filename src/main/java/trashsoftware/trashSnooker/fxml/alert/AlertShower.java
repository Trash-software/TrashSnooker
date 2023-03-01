package trashsoftware.trashSnooker.fxml.alert;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Window;
import trashsoftware.trashSnooker.fxml.App;

import java.io.IOException;

public class AlertShower {

    public static void showInfo(Window owner, String content, String header) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Alert.class.getResource("alert.fxml")
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            Stage newStage = new Stage();
            newStage.initOwner(owner);

            Scene scene = new Scene(root);
            newStage.setScene(scene);

            Alert view = loader.getController();
            view.setupInfo(newStage, header, content);

            newStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void askConfirmation(Window owner, String content, String header,
                                       Runnable positiveCallback, Runnable negativeCallback) {
        askConfirmation(owner, content, header, "是", "否",
                positiveCallback, negativeCallback);
    }

    public static void askConfirmation(Window owner, String content, String header,
                                       String positiveText, String negativeText,
                                       Runnable positiveCallback, Runnable negativeCallback) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Alert.class.getResource("alert.fxml")
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            Stage newStage = new Stage();
            newStage.initOwner(owner);

            Scene scene = new Scene(root);
            newStage.setScene(scene);

            Alert view = loader.getController();
            view.setupConfirm(newStage, header, content,
                    positiveText, negativeText,
                    positiveCallback, negativeCallback);

            newStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
