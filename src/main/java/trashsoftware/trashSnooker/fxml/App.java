package trashsoftware.trashSnooker.fxml;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.text.Font;
import javafx.stage.Stage;
//import trashsoftware.configLoader.ConfigLoader;
import trashsoftware.trashSnooker.util.ConfigLoader;
import trashsoftware.trashSnooker.util.EventLogger;

import java.io.File;
import java.util.ResourceBundle;

@SuppressWarnings("all")
public class App extends Application {
    
    public static final String VERSION_NAME = "0.3.0";
    public static final int VERSION_CODE = 25;

    private static final String CONFIG = "user" + File.separator + "config.cfg";
    public static final String CLASSIFIER = "win";
    public static final String FONT_STYLE = CLASSIFIER.equals("mac") ? 
            "-fx-font-family: 'serif'" : 
            "";
    public static final Font FONT = CLASSIFIER.equals("mac") ?
            new Font("sansserif", 12) :
            null;
    
    private static ResourceBundle strings;
    public static final boolean PRINT_DEBUG = true;
    
    public static void startApp() {
        launch();
    }

    @Override
    public void start(Stage primaryStage) {
        ConfigLoader cl = ConfigLoader.getInstance();
        if (cl.getLastVersion() != VERSION_CODE) {
            System.out.println("Just updated from version " + cl.getLastVersion() + "!");
            cl.save();
        }
        
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
            
            parent.setStyle(FONT_STYLE);
            
            EntryView entryView = loader.getController();
            entryView.setup(primaryStage);
            
            Scene scene = new Scene(parent);
            primaryStage.setScene(scene);
            
            primaryStage.show();
        } catch (Exception e) {
            EventLogger.crash(e);
        }
    }

    public static ResourceBundle getStrings() {
        return strings;
    }
}
