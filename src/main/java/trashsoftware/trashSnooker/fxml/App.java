package trashsoftware.trashSnooker.fxml;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
//import trashsoftware.configLoader.ConfigLoader;
import trashsoftware.trashSnooker.util.ConfigLoader;
import trashsoftware.trashSnooker.util.EventLogger;

import java.io.File;
import java.util.ResourceBundle;

public class App extends Application {
    
    public static final String VERSION_NAME = "0.1";
    public static final int VERSION = 12;

    private static final String CONFIG = "user" + File.separator + "config.cfg";
    public static final String FONT_STYLE = "";
//    public static final String FONT_STYLE = "-fx-font-family: 'serif'";
    
    private static ResourceBundle strings;
    
    public static void startApp() {
        launch();
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            strings = ResourceBundle.getBundle(
                    "trashsoftware.trashSnooker.bundles.Strings", 
                    ConfigLoader.getInstance().getLocale());
            
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("entryView.fxml"),
                    strings
            );
            primaryStage.setTitle(strings.getString("appName"));
            Parent parent = loader.load();
            
            EntryView entryView = loader.getController();
            entryView.setup(primaryStage);
            
            Scene scene = new Scene(parent);
            primaryStage.setScene(scene);
            
            primaryStage.show();
        } catch (Exception e) {
            EventLogger.log(e);
        }
    }

    public static ResourceBundle getStrings() {
        return strings;
    }
}
