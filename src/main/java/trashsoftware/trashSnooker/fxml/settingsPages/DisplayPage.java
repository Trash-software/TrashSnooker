package trashsoftware.trashSnooker.fxml.settingsPages;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.fxml.SettingsView;
import trashsoftware.trashSnooker.fxml.drawing.PredictionQuality;
import trashsoftware.trashSnooker.util.Util;
import trashsoftware.trashSnooker.util.config.ConfigLoader;

import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Function;

public class DisplayPage extends AbsSettingsPage {

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
    
    public DisplayPage() {
        this(App.getStrings());
    }
    
    public DisplayPage(ResourceBundle strings) {
        super("displayPage.fxml", strings);
    }

    @Override
    public void setupItems(List<ComboBox<?>> allBoxes, List<Slider> allSliders) {
        allBoxes.addAll(List.of(
                frameRateBox,
                prodFrameRateBox,
                resolutionComboBox,
                systemZoomComboBox,
                performanceBox,
                antiAliasingComboBox,
                displayBox));

        allSliders.add(effectSoundSlider);
    }

    @Override
    public void initSelections(ConfigLoader configLoader) {
        setupSliders(configLoader);
        setupScreenParams(configLoader);

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

    @Override
    public void saveIfChanged(Function<Control, Boolean> hasChanged, ConfigLoader configLoader) {
        if (hasChanged.apply(resolutionComboBox)) {
            Resolution resolution = resolutionComboBox.getValue();
            if (resolution != null) {
                configLoader.put("resolution", resolution.width + "x" + resolution.height);
            }
            App.resolutionChanged();
        }

        if (hasChanged.apply(systemZoomComboBox)) {
            SystemZoom zoom = systemZoomComboBox.getValue();
            if (zoom != null) {
                configLoader.put("systemZoom", zoom.ratio);
            }
        }

        if (hasChanged.apply(frameRateBox)) {
            configLoader.put("frameRate", frameRateBox.getValue());
        }

        if (hasChanged.apply(prodFrameRateBox)) {
            configLoader.put("productionFrameRate", prodFrameRateBox.getValue());
        }

        if (hasChanged.apply(performanceBox)) {
            configLoader.put("performance", performanceBox.getValue().toKey());
        }

        if (hasChanged.apply(antiAliasingComboBox)) {
            configLoader.put("antiAliasing", antiAliasingComboBox.getValue().toKey());
        }

        if (hasChanged.apply(displayBox)) {
            configLoader.put("display", displayBox.getValue().toKey());
        }

        if (hasChanged.apply(effectSoundSlider)) {
            configLoader.put("effectVolume", effectSoundSlider.getValue() / 100.0);
        }
    }

    private void setupSliders(ConfigLoader configLoader) {
        effectSoundSlider.valueProperty().addListener((observable, oldValue, newValue) ->
                effectSoundLabel.setText(String.format("%.0f", (double) newValue)));

        effectSoundSlider.setValue(100 * configLoader.getDouble("effectVolume", 1.0));
    }

    private void setupScreenParams(ConfigLoader configLoader) {
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
