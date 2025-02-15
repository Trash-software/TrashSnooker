package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.stage.Stage;
import trashsoftware.trashSnooker.fxml.alert.AlertShower;
import trashsoftware.trashSnooker.fxml.drawing.PredictionQuality;
import trashsoftware.trashSnooker.util.config.ConfigLoader;
import trashsoftware.trashSnooker.util.Util;

import java.net.URL;
import java.util.*;

public class SettingsView extends ChildInitializable {
    private final List<ComboBox<?>> allBoxes = new ArrayList<>();
    private final List<Slider> allSliders = new ArrayList<>();
    private final Map<ComboBox<?>, Integer> lastSavedSelections = new HashMap<>();
    private final Map<Slider, Double> lastSavedValues = new HashMap<>();
    @FXML
    ComboBox<Double> aimLingBox, aiStrengthBox;
    @FXML
    ComboBox<YesNo> autoChangeBreakCueBox;
    @FXML
    ComboBox<MouseDragMethod> mouseDragMethodBox;
    @FXML
    ComboBox<LocaleName> languageBox;
    @FXML
    ComboBox<Integer> frameRateBox, prodFrameRateBox;
    @FXML
    ComboBox<Resolution> resolutionComboBox;
    @FXML
    ComboBox<Display> displayBox;
    @FXML
    ComboBox<SystemZoom> systemZoomComboBox;
    @FXML
    ComboBox<AntiAliasing> antiAliasingComboBox;
    @FXML
    Slider effectSoundSlider;
    @FXML
    Label effectSoundLabel;
    @FXML
    ComboBox<PredictionQuality> performanceBox;
    @FXML
    ComboBox<Integer> aiThreadNumBox;
    @FXML
    Label cpuActualThreadNumLabel;
    @FXML
    Button confirmBtn;
    private Stage stage;
    private ConfigLoader configLoader;
    private ResourceBundle strings;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.strings = resources;

        allBoxes.addAll(List.of(
                aimLingBox,
                aiStrengthBox,
                autoChangeBreakCueBox,
                mouseDragMethodBox,
                frameRateBox,
                prodFrameRateBox,
                languageBox,
                resolutionComboBox,
                systemZoomComboBox,
                aiThreadNumBox,
                performanceBox,
                antiAliasingComboBox,
                displayBox));
        
        allSliders.add(effectSoundSlider);

        configLoader = ConfigLoader.getInstance();
        setupBoxes();
        setupSliders();

        storeSelectionsToMap();
        addGeneralChangeListeners();
    }

    private void setupBoxes() {
        aimLingBox.getItems().addAll(
                0.0, 0.15, 0.4, 0.75, 1.0, 1.5, 2.0, 3.0, 5.0, 10.0
        );
        aiStrengthBox.getItems().addAll(
                0.15, 0.4, 0.75, 1.0, 1.5, 2.0, 3.0, 5.0, 10.0
        );
        aimLingBox.getSelectionModel().select(configLoader.getDouble("fastGameAiming", 1.0));
        aiStrengthBox.getSelectionModel().select(configLoader.getDouble("fastGameAiStrength", 1.0));
        
        autoChangeBreakCueBox.getItems().addAll(YesNo.values());
        autoChangeBreakCueBox.getSelectionModel().select(YesNo.fromBoolean(
                configLoader.getBoolean("autoChangeBreakCue", false)
        ));
        
        mouseDragMethodBox.getItems().addAll(MouseDragMethod.values());
        mouseDragMethodBox.getSelectionModel().select(MouseDragMethod.fromKey(
                configLoader.getString("mouseDragMethod", "movement")
        ));

        setupLanguageBox();
        setupScreenParams();

        int cpuThreads = Runtime.getRuntime().availableProcessors();
        for (int i = 1; i <= cpuThreads * 2; i++) {
            aiThreadNumBox.getItems().add(i);
        }
        cpuActualThreadNumLabel.setText("/" + cpuThreads);
        int curThreads = configLoader.getInt("nThreads", cpuThreads / 4);
        aiThreadNumBox.getSelectionModel().select(Integer.valueOf(curThreads));

        frameRateBox.getItems().addAll(
                20, 24, 30, 40, 50, 60, 90, 120, 144, 165, 200, 240, 300, 400, 500
        );
        frameRateBox.getSelectionModel().select(Integer.valueOf(configLoader.getFrameRate()));

        prodFrameRateBox.getItems().addAll(
                30, 50, 60, 90, 120, 160, 240
        );
        prodFrameRateBox.getSelectionModel().select(Integer.valueOf(configLoader.getProductionFrameRate()));

        performanceBox.getItems().addAll(PredictionQuality.values());
        performanceBox.getSelectionModel().select(PredictionQuality.fromKey(configLoader.getString("performance",
                "veryHigh")));

        antiAliasingComboBox.getItems().addAll(AntiAliasing.values());
        antiAliasingComboBox.getSelectionModel().select(AntiAliasing.fromKey(configLoader.getString("antiAliasing",
                "disabled")));

        displayBox.getItems().addAll(Display.values());
        displayBox.getSelectionModel().select(Display.fromKey(configLoader.getString("display",
                "windowed")));
    }
    
    private void setupSliders() {
        effectSoundSlider.valueProperty().addListener((observable, oldValue, newValue) -> 
                effectSoundLabel.setText(String.format("%.0f", (double) newValue)));
        
        effectSoundSlider.setValue(100 * configLoader.getDouble("effectVolume", 1.0));
    }

    private void setupLanguageBox() {
        Locale selected = configLoader.getLocale();
        for (Locale locale : App.getAllSupportedLocales()) {
            LocaleName localeName = new LocaleName(locale);
            languageBox.getItems().add(localeName);
            if (locale.equals(selected)) {
                languageBox.getSelectionModel().select(localeName);
            }
        }
    }

    private void setupScreenParams() {
        resolutionComboBox.getItems().addAll(Resolution.values());
        systemZoomComboBox.getItems().addAll(SystemZoom.values());

        double[] screen = configLoader.getResolution();
        int w = (int) screen[0];
        int h = (int) screen[1];
        double z = screen[2];

        boolean rSel = false;
        for (Resolution r : Resolution.values()) {
            if (r.width == w && r.height == h) {
                resolutionComboBox.getSelectionModel().select(r);
                rSel = true;
                break;
            }
        }
        if (!rSel) {
            resolutionComboBox.getSelectionModel().select(Resolution.RES_1080P);
        }

        boolean zSel = false;
        for (SystemZoom zoom : SystemZoom.values()) {
            if (zoom.ratio == z) {
                systemZoomComboBox.getSelectionModel().select(zoom);
                zSel = true;
                break;
            }
        }
        if (!zSel) {
            systemZoomComboBox.getSelectionModel().select(SystemZoom.SZ_100);
        }
    }

    private boolean anyHasChanged() {
        for (ComboBox<?> box : allBoxes) {
            if (hasChanged(box)) return true;
        }
        return false;
    }

    private void addGeneralChangeListeners() {
        for (ComboBox<?> box : allBoxes) {
            box.getSelectionModel().selectedIndexProperty().addListener((observable, oldValue, newValue) -> {
                Integer lastSaved = lastSavedSelections.get(box);
                if (!Objects.equals(newValue, lastSaved)) {
                    confirmBtn.setDisable(false);
                } else if (!anyHasChanged()) {
                    confirmBtn.setDisable(true);
                }
            });
        }
        for (Slider slider : allSliders) {
            slider.valueProperty().addListener((observable, oldValue, newValue) -> {
                Double lastSaved = lastSavedValues.get(slider);
                if (!Objects.equals(newValue, lastSaved)) {
                    confirmBtn.setDisable(false);
                } else if (!anyHasChanged()) {
                    confirmBtn.setDisable(true);
                }
            });
        }
    }

    private void storeSelectionsToMap() {
        for (ComboBox<?> box : allBoxes) {
            lastSavedSelections.put(box, box.getSelectionModel().getSelectedIndex());
        }
        for (Slider slider : allSliders) {
            lastSavedValues.put(slider, slider.getValue());
        }
    }

    private boolean hasChanged(ComboBox<?> box) {
        Integer lastIndex = lastSavedSelections.get(box);
        return box.getSelectionModel().getSelectedIndex() != lastIndex;  // null也是changed
    }

    private boolean hasChanged(Slider slider) {
        Double lastValue = lastSavedValues.get(slider);
        return slider.getValue() != lastValue;  // null也是changed
    }

//    @Override
//    public Stage getStage() {
//        return stage;
//    }

    void setup(Stage stage) {
        this.stage = stage;
    }

    @FXML
    void confirmAction() {
        if (hasChanged(languageBox)) {
            Locale locale = languageBox.getValue().locale;
            if (locale != null) {
                configLoader.put("locale", locale.getLanguage() + "_" + locale.getCountry());
            }
            App.reloadStrings();
        }

        if (hasChanged(resolutionComboBox)) {
            Resolution resolution = resolutionComboBox.getValue();
            if (resolution != null) {
                configLoader.put("resolution", resolution.width + "x" + resolution.height);
            }
            App.resolutionChanged();
        }

        if (hasChanged(systemZoomComboBox)) {
            SystemZoom zoom = systemZoomComboBox.getValue();
            if (zoom != null) {
                configLoader.put("systemZoom", zoom.ratio);
            }
        }

        if (hasChanged(frameRateBox)) {
            configLoader.put("frameRate", frameRateBox.getValue());
        }
        
        if (hasChanged(prodFrameRateBox)) {
            configLoader.put("productionFrameRate", prodFrameRateBox.getValue());
        }

        if (hasChanged(aiThreadNumBox)) {
            Integer nThreads = aiThreadNumBox.getValue();
            if (nThreads != null) {
                configLoader.put("nThreads", nThreads);
            }
        }

        if (hasChanged(aimLingBox)) {
            configLoader.put("fastGameAiming", aimLingBox.getSelectionModel().getSelectedItem());
        }
        if (hasChanged(aiStrengthBox)) {
            configLoader.put("fastGameAiStrength", aiStrengthBox.getSelectionModel().getSelectedItem());
        }
        if (hasChanged(autoChangeBreakCueBox)) {
            configLoader.put("autoChangeBreakCue", autoChangeBreakCueBox.getSelectionModel().getSelectedItem().toBoolean());
        }
        if (hasChanged(mouseDragMethodBox)) {
            configLoader.put("mouseDragMethod", mouseDragMethodBox.getSelectionModel().getSelectedItem().toKey());
        }

        if (hasChanged(performanceBox)) {
            configLoader.put("performance", performanceBox.getValue().toKey());
        }

        if (hasChanged(antiAliasingComboBox)) {
            configLoader.put("antiAliasing", antiAliasingComboBox.getValue().toKey());
        }

        if (hasChanged(displayBox)) {
            configLoader.put("display", displayBox.getValue().toKey());
        }
        
        if (hasChanged(effectSoundSlider)) {
            configLoader.put("effectVolume", effectSoundSlider.getValue() / 100.0);
        }

        configLoader.save();
        super.backAction();
    }

    @FXML
    void cancelAction() {
        if (anyHasChanged()) {
            AlertShower.askConfirmation(
                    stage,
                    strings.getString("confirmDiscardChanges"),
                    strings.getString("pleaseConfirm"),
                    super::backAction,
                    null
            );
        } else {
            super.backAction();
        }
    }

    @Override
    public void backAction() {
        cancelAction();
    }
    
    public enum YesNo {
        YES,
        NO;
        
        public static YesNo fromBoolean(Boolean value) {
            if (value == null || !value) {
                return NO;
            }
            return YES;
        }
        
        public boolean toBoolean() {
            return this == YES;
        }

        @Override
        public String toString() {
            String key = this == YES ? "yes" : "no";
            return App.getStrings().getString(key);
        }
    }
    
    public enum MouseDragMethod {
        POSITION("mouseDragAbsolute"),
        MOVEMENT("mouseDragRelative");
        
        private final String stringKey;
        
        MouseDragMethod(String stringKey) {
            this.stringKey = stringKey;
        }

        @Override
        public String toString() {
            return App.getStrings().getString(stringKey);
        }
        
        static MouseDragMethod fromKey(String key) {
            try {
                return valueOf(Util.toAllCapsUnderscoreCase(key));
            } catch (IllegalArgumentException e) {
                return MOVEMENT;
            }
        }
        
        String toKey() {
            return Util.toLowerCamelCase(name());
        }
    }

    public enum Resolution {
        RES_720P(1280, 720),
        RES_1360_768(1360, 768),
        RES_1366_768(1366, 768),
        RES_1536_864(1536, 864) {
            @Override
            String extraDescription() {
                return App.getStrings().getString("minimumResolution");
            }
        },
        RES_1280_960(1280, 960),
        RES_1280_1024(1280, 1024),
        RES_1680_1050(1680, 1050),
        RES_1440_1080(1440, 1080),
        RES_1080P(1920, 1080),
        RES_2160_1080(2160, 1080),
        RES_QWXGA(2048, 1152),
        RES_UXGA(1600, 1200),
        RES_FHD_PLUS(2160, 1440),
        RES_2K(2560, 1440),
        RES_2400_1600(2400, 1600),
        RES_WQXGA(2560, 1600),
        RES_3K(2880, 1620),
        RES_2880_1800(2880, 1800),
        RES_3200_1800(3200, 1800),
        RES_2880_2160(2880, 2160),
        RES_3240_2160(3240, 2160),
        RES_4K(3840, 2160),
        RES_4320_2160(4320, 2160);

        final int width;
        final int height;

        Resolution(int width, int height) {
            this.width = width;
            this.height = height;
        }

        String extraDescription() {
            return "";
        }

        @Override
        public String toString() {
            return width + "x" + height + extraDescription();
        }
    }

    public enum SystemZoom {
        SZ_100(1.0),
        SZ_125(1.25),
        SZ_150(1.5),
        SZ_175(1.75);

        private final double ratio;

        SystemZoom(double ratio) {
            this.ratio = ratio;
        }

        @Override
        public String toString() {
            return (int) (ratio * 100) + "%";
        }
    }

    public static class LocaleName {
        final Locale locale;

        LocaleName(Locale locale) {
            this.locale = locale;
        }

        @Override
        public String toString() {
            return locale.getDisplayLanguage(locale);
        }
    }

    public enum AntiAliasing {
        DISABLED,
        BALANCED;

        @Override
        public String toString() {
            return App.getStrings().getString(Util.toLowerCamelCase("ANTI_ALIASING_" + name()));
        }

        static AntiAliasing fromKey(String key) {
            try {
                return valueOf(Util.toAllCapsUnderscoreCase(key));
            } catch (IllegalArgumentException e) {
                return DISABLED;
            }
        }

        String toKey() {
            return Util.toLowerCamelCase(name());
        }
    }

    public enum Display {
        WINDOWED,
        FULL_SCREEN;

        @Override
        public String toString() {
            return App.getStrings().getString(Util.toLowerCamelCase("DISPLAY_" + name()));
        }

        static Display fromKey(String key) {
            try {
                return valueOf(Util.toAllCapsUnderscoreCase(key));
            } catch (IllegalArgumentException e) {
                return WINDOWED;
            }
        }

        String toKey() {
            return Util.toLowerCamelCase(name());
        }
    }
}
