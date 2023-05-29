package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import trashsoftware.trashSnooker.fxml.alert.AlertShower;
import trashsoftware.trashSnooker.util.ConfigLoader;

import java.net.URL;
import java.util.*;

public class SettingsView extends ChildInitializable {
    private final List<ComboBox<?>> allBoxes = new ArrayList<>();
    private final Map<ComboBox<?>, Integer> lastSavedSelections = new HashMap<>();
    @FXML
    ComboBox<Double> aimLingBox, aiStrengthBox;
    @FXML
    ComboBox<LocaleName> languageBox;
    @FXML
    ComboBox<Integer> frameRateBox;
    @FXML
    ComboBox<Resolution> resolutionComboBox;
    @FXML
    ComboBox<SystemZoom> systemZoomComboBox;
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

        allBoxes.addAll(List.of(aimLingBox,
                aiStrengthBox,
                frameRateBox,
                languageBox,
                resolutionComboBox,
                systemZoomComboBox,
                aiThreadNumBox));

        configLoader = ConfigLoader.getInstance();
        setupBoxes();

        storeSelectionsToMap();
        addGeneralChangeListeners();
    }

    private void setupBoxes() {
        setupDifficultyBox(aimLingBox);
        setupDifficultyBox(aiStrengthBox);
        aimLingBox.getSelectionModel().select(configLoader.getDouble("fastGameAiming", 1.0));
        aiStrengthBox.getSelectionModel().select(configLoader.getDouble("fastGameAiStrength", 1.0));

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
                20, 24, 30, 40, 50, 60, 90, 120, 144, 180, 240, 300, 400, 500
        );
        frameRateBox.getSelectionModel().select(Integer.valueOf(configLoader.getFrameRate()));
    }

    private void setupDifficultyBox(ComboBox<Double> box) {
        box.getItems().addAll(
                0.15, 0.4, 0.75, 1.0, 1.5, 2.0, 3.0, 5.0, 10.0
        );
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
    }

    private void storeSelectionsToMap() {
        for (ComboBox<?> box : allBoxes) {
            lastSavedSelections.put(box, box.getSelectionModel().getSelectedIndex());
        }
    }

    private boolean hasChanged(ComboBox<?> box) {
        Integer lastIndex = lastSavedSelections.get(box);
        return box.getSelectionModel().getSelectedIndex() != lastIndex;  // null也是changed
    }

    @Override
    public Stage getStage() {
        return stage;
    }

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
}
