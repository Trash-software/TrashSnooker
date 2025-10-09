package trashsoftware.trashSnooker.fxml.settingsPages;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.fxml.SettingsView;
import trashsoftware.trashSnooker.util.config.ConfigLoader;

import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Function;

public class GeneralPage extends AbsSettingsPage {

    @FXML
    ComboBox<SettingsView.LocaleName> languageBox;
    @FXML
    ComboBox<Integer> aiThreadNumBox;
    @FXML
    Label cpuActualThreadNumLabel;
    
    public GeneralPage() {
        this(App.getStrings());
    }
    
    public GeneralPage(ResourceBundle strings) {
        super("generalPage.fxml", strings);
    }

    @Override
    public void setupItems(List<ComboBox<?>> allBoxes, List<Slider> allSliders) {
        allBoxes.add(languageBox);
        allBoxes.add(aiThreadNumBox);
    }

    @Override
    public void initSelections(ConfigLoader configLoader) {
        int cpuThreads = Runtime.getRuntime().availableProcessors();
        for (int i = 1; i <= cpuThreads * 2; i++) {aiThreadNumBox.getItems().add(i);
        }
        cpuActualThreadNumLabel.setText("/" + cpuThreads);
        int curThreads = configLoader.getInt("nThreads", cpuThreads / 4);
        aiThreadNumBox.getSelectionModel().select(Integer.valueOf(curThreads));

        Locale selected = configLoader.getLocale();
        for (Locale locale : App.getAllSupportedLocales()) {
            SettingsView.LocaleName localeName = new SettingsView.LocaleName(locale);
            languageBox.getItems().add(localeName);
            if (locale.equals(selected)) {
                languageBox.getSelectionModel().select(localeName);
            }
        }
    }

    @Override
    public void saveIfChanged(Function<Control, Boolean> hasChanged, ConfigLoader configLoader) {
        if (hasChanged.apply(languageBox)) {
            Locale locale = languageBox.getValue().locale;
            if (locale != null) {
                configLoader.put("locale", locale.getLanguage() + "_" + locale.getCountry());
            }
            App.reloadStrings();
        }

        if (hasChanged.apply(aiThreadNumBox)) {
            Integer nThreads = aiThreadNumBox.getValue();
            if (nThreads != null) {
                configLoader.put("nThreads", nThreads);
            }
        }
    }
}
