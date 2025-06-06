package trashsoftware.trashSnooker.fxml;

import javafx.application.Application;
import javafx.beans.binding.DoubleBinding;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.SceneAntialiasing;
import javafx.scene.canvas.Canvas;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.*;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.res.ResourcesLoader;
import trashsoftware.trashSnooker.util.config.ConfigLoader;
import trashsoftware.trashSnooker.util.EventLogger;

import java.io.File;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

@SuppressWarnings("all")
public class App extends Application {

    public static final String VERSION_NAME = "0.7 beta 3";
    public static final int VERSION_CODE = 59;
    public static final String CLASSIFIER = "win";
    public static final String FONT_STYLE = CLASSIFIER.equals("mac") ?
            "-fx-font-family: 'serif'" :
            "";
    public static final Font FONT = CLASSIFIER.equals("mac") ?
            new Font("sansserif", 12) :
            new Font(Font.getDefault().getName(), 12);
    public static final boolean PRINT_DEBUG = false;
    private static final String CONFIG = "user" + File.separator + "config.cfg";
    private static ResourceBundle strings;
    private static ResourceBundle achievementStrings;

    private static VBox appRoot;
    private static Stage fullScreenStage;
    private static boolean mainWindowShowing;

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
        SceneAntialiasing antialiasing = ConfigLoader.getInstance().getAntiAliasing().threeDAA;
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

        boolean wasMax = stage.isMaximized();
        stage.sizeToScene();
        if (wasMax) {
            stage.setMaximized(false);
            stage.setMaximized(true);
        }
    }

    public static void resolutionChanged() {
//        scaleRatio = 0.0;
    }

    public static void scaleGameStage(Stage stage, GameView gameView) {
        scaleGameStage(stage, gameView, 1.0);
    }

    public static void scaleGameStage(Stage stage, GameView gameView, double scaleMul) {
        stage.getIcons().add(ResourcesLoader.getInstance().getIcon());
        boolean fullScreen = switch (ConfigLoader.getInstance().getString("display")) {
            case "fullScreen" -> true;
            default -> false;
        };

        // 防止杆把父节点挤大
        Pane contentPane = gameView.getContentPane();
        Rectangle clipBound = new Rectangle(contentPane.getWidth(), contentPane.getHeight());
        clipBound.widthProperty().bind(stage.widthProperty());
        clipBound.heightProperty().bind(stage.heightProperty());
        System.out.println(clipBound.getWidth() + " " + clipBound.getHeight());
        contentPane.setClip(clipBound);

        if (fullScreen) {
            Scene scene = stage.getScene();
            Parent root = scene.getRoot();

            double sw = scene.getWidth();
            double sh = scene.getHeight();

            double[] systemRes = ConfigLoader.getSystemResolution();
            double rw = systemRes[0] / sw;
            double rh = systemRes[1] / sh;
//            double scaleRatio = rh * scaleMul;
            double scaleRatio = Math.min(rw, rh) * scaleMul;
//            System.out.println(rw + "x" + rh + ": " + sw + "x" + sh);

            Scale scaleTrans = new Scale(scaleRatio, scaleRatio);
            root.getTransforms().add(scaleTrans);
            stage.setFullScreenExitHint("");
            
            fullScreenStage = stage;

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
//            stage.initStyle(StageStyle.UNDECORATED);
//            double[] systemResolution = ConfigLoader.getSystemResolution();
//            stage.setWidth(systemResolution[0]);
//            stage.setHeight(systemResolution[1]);
        } else {
            bindGamePaneToWindow(stage, gameView);
        }

//        stage.widthProperty().addListener(((observableValue, aBoolean, t1) -> {
//            stage.setX(0);
//            stage.setY(0);
//        }));

//        gameView.setupAfterShow();
    }
    
    private static double heightExcept(VBox vBox, Node excepted) {
        double h = 0;
        Insets insets = vBox.getInsets();
        if (insets != null) h += insets.getTop() + insets.getBottom();
        h += Math.max(0, vBox.getChildren().size() - 1) * vBox.getSpacing();
        for (Node child : vBox.getChildren()) {
            if (child instanceof VBox childVbox) {
                h += heightExcept(childVbox, excepted);
            } else if (child != excepted) {
                // 暂没有考虑其他V型容器
                if (child instanceof Region region) {
                    h += region.getHeight();
                } else if (child instanceof Canvas canvas) {
                    h += canvas.getHeight();
                } else {
                    System.err.println("Child " + child + " not instance of region");
                }
            }
        }
        return h;
    }
    
    private static void bindGamePaneToWindow(Stage stage, GameView gameView) {
        GameValues gameValues = gameView.gameValues;
        
        final double powerSliderH = gameView.powerSliderPane.getHeight();
        double otherLeftHeights = gameView.leftToolbarPane.getHeight() - powerSliderH;
        
        Insets mainInsets = gameView.mainFramePane.getInsets();
        double systemZoom = ConfigLoader.getInstance().getSystemZoom();
        
        double deadWidth = (gameView.leftVBox.getWidth() + 
                gameView.mainFramePane.getSpacing() +
                mainInsets.getLeft() + mainInsets.getRight()) * systemZoom;
        double deadHeight = (gameView.menuBar.getHeight() +
                gameView.bottomPane.getHeight() +
                mainInsets.getTop() + mainInsets.getBottom() + 30) * systemZoom;
        
        double leftRemHeight = heightExcept(gameView.leftVBox, gameView.powerSliderPane);
        double sliderOrigHeight = gameView.powerSlider.getHeight();
        double sliderMarginHeight = gameView.powerSliderPane.getHeight() - sliderOrigHeight;
        
        final double gamePaneOrigWidth = gameView.gamePane.getWidth();
        double tableAspect = gameValues.table.outerWidth / gameValues.table.outerHeight;
        
        Scene scene = stage.getScene();

        stage.widthProperty().addListener((observable, oldValue, newValue) -> {
            double tableAreaW = newValue.doubleValue() - deadWidth;
            double ratio = tableAreaW / gamePaneOrigWidth;
            
            double tableAreaH = tableAreaW / tableAspect;
            
            stage.setHeight(tableAreaH + deadHeight);
//            System.out.println("GP size: " + gameView.gamePane.getWidth() + ", " + gameView.gamePane.getHeight());

            double newScale = tableAreaW / gameValues.table.outerWidth;
            gameView.changeScale(newScale);
//            System.out.println(scene.getWidth() + ", " + gameView.contentPane.getWidth());
        });
        
        stage.heightProperty().addListener((observable, oldValue, newValue) -> {
            double hRemForSlider = newValue.doubleValue() - leftRemHeight - sliderMarginHeight;
            double sliderNewH = Math.max(160, Math.min(sliderOrigHeight, hRemForSlider));
            gameView.powerSlider.setPrefHeight(sliderNewH);
            gameView.updatePowerSlider();
        });
    }

    public static ResourceBundle getStrings() {
        return strings;
    }

    public static ResourceBundle getAchievementStrings() {
        return achievementStrings;
    }

    public static void reloadStrings() {
        Locale locale = ConfigLoader.getInstance().getLocale();
        strings = ResourceBundle.getBundle(
                "trashsoftware.trashSnooker.bundles.Strings",
                locale);
        achievementStrings = ResourceBundle.getBundle(
                "trashsoftware.trashSnooker.bundles.Achievements",
                locale);
    }

    public static Stage getFullScreenStage() {
        return fullScreenStage != null && fullScreenStage.isShowing() ? fullScreenStage : null;
    }
    
    public static void focusFullScreenStage() {
        if (fullScreenStage != null && fullScreenStage.isShowing()) {
            fullScreenStage.requestFocus();
        }
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
            primaryStage.getIcons().add(ResourcesLoader.getInstance().getIcon());
            Parent parent = loader.load();
            
            appRoot = new VBox();
            appRoot.setAlignment(Pos.CENTER);
            appRoot.setStyle(FONT_STYLE);
            
            appRoot.getChildren().add(parent);
            
//            Scene scene = new Scene(appRoot);
            Scene scene = createScene(appRoot);
            primaryStage.setScene(scene);

            EntryView entryView = loader.getController();
            entryView.setup(primaryStage);
            
            mainWindowShowing = true;

            primaryStage.show();
        } catch (Exception e) {
            EventLogger.crash(e);
        }
    }

    public static boolean isMainWindowShowing() {
        return mainWindowShowing;
    }

    static void setMainWindowShowing(boolean mainWindowShowing) {
        App.mainWindowShowing = mainWindowShowing;
    }

    public static Pane getAppRoot() {
        return appRoot;
    }

    public static void setRoot(Node root) {
        appRoot.getChildren().clear();
        appRoot.getChildren().add(root);
        appRoot.setVgrow(root, Priority.ALWAYS);
        
        Stage stage = (Stage) root.getScene().getWindow();
        if (!stage.isMaximized()) {
            stage.sizeToScene();
        }
    }
}
