package trashsoftware.trashSnooker.fxml;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.text.Font;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import trashsoftware.trashSnooker.util.ConfigLoader;
import trashsoftware.trashSnooker.util.EventLogger;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

@SuppressWarnings("all")
public class App extends Application {

    public static final String VERSION_NAME = "0.4.5";
    public static final int VERSION_CODE = 39;
    public static final String CLASSIFIER = "win";
    public static final String FONT_STYLE = CLASSIFIER.equals("mac") ?
            "-fx-font-family: 'serif'" :
            "";
    public static final Font FONT = CLASSIFIER.equals("mac") ?
            new Font("sansserif", 12) :
            Font.getDefault();
    public static final boolean PRINT_DEBUG = false;
    private static final String CONFIG = "user" + File.separator + "config.cfg";
    private static ResourceBundle strings;

    public static void startApp() {
        launch();
    }

    public static List<Locale> getAllSupportedLocales() {
//        for (Locale locale : Locale.getAvailableLocales()) {
//            try {
//                
//            }
//        }
        return List.of(Locale.CHINA, Locale.US);
    }

    public static Scene createScene(Parent root) {
        SceneAntialiasing antialiasing = switch (ConfigLoader.getInstance().getString("antiAliasing")) {
            case "balanced" -> SceneAntialiasing.BALANCED;
            case "disabled" -> SceneAntialiasing.DISABLED;
            default -> SceneAntialiasing.DISABLED;
        };

        return new Scene(root, -1, -1, false, antialiasing);
    }

    public static void scaleWindow(Stage stage) {
        Scene scene = stage.getScene();
        Parent root = scene.getRoot();

        double sw = scene.getWidth();
        double sh = scene.getHeight();

        double[] systemRes = ConfigLoader.getSystemResolution();

        if (systemRes[1] >= 864) return;
        double rw = systemRes[0] / sw;
        double rh = systemRes[1] / sh;

        double windowScaleRatio = 1 / Math.min(rw, rh);

        Scale scaleTrans = new Scale(windowScaleRatio, windowScaleRatio);
        root.getTransforms().add(scaleTrans);

        stage.setX(0);
        stage.setY(0);
    }

    public static void resolutionChanged() {
//        scaleRatio = 0.0;
    }

    public static void scaleGameStage(Stage stage) {
        scaleGameStage(stage, 1.0);
    }

    public static void scaleGameStage(Stage stage, double scaleMul) {
        boolean fullScreen = switch (ConfigLoader.getInstance().getString("display")) {
            case "fullScreen" -> true;
            default -> false;
        };

        if (fullScreen) {
            Scene scene = stage.getScene();
            Parent root = scene.getRoot();

            double sw = scene.getWidth();
            double sh = scene.getHeight();

            double[] systemRes = ConfigLoader.getSystemResolution();
            double rw = systemRes[0] / sw;
            double rh = systemRes[1] / sh;
            double scaleRatio = rh * scaleMul;
//            System.out.println(rw + "x" + rh + ": " + sw + "x" + sh);

            Scale scaleTrans = new Scale(scaleRatio, scaleRatio);
            root.getTransforms().add(scaleTrans);
            stage.setFullScreenExitHint("");

            // 你他妈两只手都用上试试？
            stage.setFullScreenExitKeyCombination(new KeyCodeCombination(
                    KeyCode.ESCAPE,
                    KeyCombination.ModifierValue.DOWN,
                    KeyCombination.ModifierValue.DOWN,
                    KeyCombination.ModifierValue.DOWN,
                    KeyCombination.ModifierValue.DOWN,
                    KeyCombination.ModifierValue.DOWN
            ));

            stage.setFullScreen(true);
        }

        stage.widthProperty().addListener(((observableValue, aBoolean, t1) -> {
            stage.setX(0);
            stage.setY(0);
        }));
    }

    public static ResourceBundle getStrings() {
        return strings;
    }

    public static void reloadStrings() {
        strings = ResourceBundle.getBundle(
                "trashsoftware.trashSnooker.bundles.Strings",
                ConfigLoader.getInstance().getLocale());
    }

    @Override
    public void start(Stage primaryStage) {
        ConfigLoader cl = ConfigLoader.getInstance();
        if (cl.getLastVersion() != VERSION_CODE) {
            System.out.println("Just updated from version " + cl.getLastVersion() + "!");
            cl.save();
        }

        try {
            reloadStrings();

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("entryView.fxml"),
                    strings
            );
            primaryStage.setTitle(strings.getString("appName"));
            Parent parent = loader.load();

            parent.setStyle(FONT_STYLE);
            Scene scene = new Scene(parent);
            primaryStage.setScene(scene);

            EntryView entryView = loader.getController();
            entryView.setup(primaryStage);

            primaryStage.show();
        } catch (Exception e) {
            EventLogger.crash(e);
        }
    }
}
