package trashsoftware.trashSnooker.fxml.alert;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.Window;
import trashsoftware.trashSnooker.fxml.App;

import java.io.IOException;

public class AlertShower {

    public static void showInfo(Window owner, String content, String header) {
        showInfo(owner, content, header, -1);
    }

    public static void showInfo(Window owner, String content, String header, long autoCloseMs) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Alert.class.getResource("alert.fxml"),
                    App.getStrings()
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            Stage newStage = new Stage();
            newStage.initOwner(owner);
            newStage.initModality(Modality.WINDOW_MODAL);
            newStage.setResizable(false);

            Scene scene = App.createScene(root);
            newStage.setScene(scene);

            Alert view = loader.getController();
            view.setupInfo(newStage, header, content);

            if (autoCloseMs > 0) {
                Thread autoClose = new Thread(() -> {
                    try {
                        Thread.sleep(autoCloseMs);
                        Platform.runLater(() -> {
                            if (view.isActive()) {  // 确保它不会被手动关了之后又自动关了，虽然daemon好像已经有这个作用了
                                view.yesButton.fire();
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                });
                autoClose.setDaemon(true);
                autoClose.start();
            }

            newStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void showSingleButtonWindow(Window owner, String content, String header, 
                                              String buttonText,
                                              Runnable callback,
                                              Node additionalContent) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Alert.class.getResource("alert.fxml"),
                    App.getStrings()
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            Stage newStage = new Stage();
            newStage.initOwner(owner);
            newStage.initModality(Modality.WINDOW_MODAL);
            newStage.setResizable(false);

            Scene scene = App.createScene(root);
            newStage.setScene(scene);

            Alert view = loader.getController();
            view.functionalWindow(newStage, header, content, buttonText, callback);

            if (additionalContent != null) {
                view.setupAdditional(additionalContent);
            }
            
            newStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void askConfirmation(Window owner, String content, String header,
                                       Runnable positiveCallback, Runnable negativeCallback) {
        askConfirmation(owner, content, header, 
                App.getStrings().getString("yes"), 
                App.getStrings().getString("no"),
                positiveCallback, negativeCallback);
    }

    public static void askConfirmation(Window owner, String content, String header,
                                       String positiveText, String negativeText,
                                       Runnable positiveCallback, Runnable negativeCallback) {
        askConfirmation(owner, content, header, positiveText, negativeText, positiveCallback, negativeCallback, null);
    }

    public static void askConfirmation(Window owner, String content, String header,
                                       String positiveText, String negativeText,
                                       Runnable positiveCallback, Runnable negativeCallback,
                                       Node additionalContent) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Alert.class.getResource("alert.fxml"),
                    App.getStrings()
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            Stage newStage = new Stage();
            newStage.initOwner(owner);
            newStage.initModality(Modality.WINDOW_MODAL);
            newStage.setResizable(false);

            Scene scene = App.createScene(root);
            newStage.setScene(scene);

            Alert view = loader.getController();
            view.setupConfirm(newStage, header, content,
                    positiveText, negativeText,
                    positiveCallback, negativeCallback);
            
            if (additionalContent != null) {
                view.setupAdditional(additionalContent);
            }

            newStage.showAndWait();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
