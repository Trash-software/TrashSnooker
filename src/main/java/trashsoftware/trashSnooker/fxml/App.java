package trashsoftware.trashSnooker.fxml;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
//import trashsoftware.configLoader.ConfigLoader;
import trashsoftware.trashSnooker.fxml.GameView;
import trashsoftware.trashSnooker.fxml.MainView;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.Recorder;
import trashsoftware.trashSnooker.util.db.DBAccess;

import java.io.File;
import java.io.IOException;

public class App extends Application {
    
    public static final int VERSION = 1;

    private static final String CONFIG = "user" + File.separator + "config.cfg";
    public static final String FONT_STYLE = "-fx-font-family: 'serif'";
    
    public static void startApp() {
        launch();
    }

    @Override
    public void start(Stage primaryStage) {
        try {
//            ConfigLoader.startLoader(CONFIG);
            Recorder.loadAll();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("mainView.fxml")
            );
            Parent root = loader.load();
            root.setStyle(FONT_STYLE);

            MainView mainView = loader.getController();
            mainView.setStage(primaryStage);

            Scene scene = new Scene(root);
//            scene.getStylesheets().add(getClass().getResource("/trashsoftware/trashSnooker/css/font.css").toExternalForm());
            primaryStage.setScene(scene);

            primaryStage.setOnHidden(e -> {
//                Recorder.save();
//                ConfigLoader.stopLoader();
                DBAccess.closeDB();
            });

            primaryStage.show();
        } catch (Exception e) {
            EventLogger.log(e);
        }
    }
}
