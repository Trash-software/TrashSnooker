package trashsoftware.trashSnooker.fxml.settingsPages;

import javafx.fxml.FXMLLoader;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Slider;
import javafx.scene.layout.GridPane;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.fxml.SettingsView;
import trashsoftware.trashSnooker.util.config.ConfigLoader;

import java.io.IOException;
import java.util.List;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.function.Function;

public abstract class AbsSettingsPage extends GridPane {
    
    protected ResourceBundle strings;
    protected SettingsView parent;
    
    public AbsSettingsPage(String path) {
        this(path, App.getStrings());
    }
    
    public AbsSettingsPage(String path, ResourceBundle strings) {
        super();

        this.strings = strings;
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource(
                path), strings);
        fxmlLoader.setRoot(this);
        fxmlLoader.setController(this);

        try {
            fxmlLoader.load();
        } catch (IOException exception) {
            throw new RuntimeException(exception);
        }
    }

    public void setParent(SettingsView parent) {
        this.parent = parent;
    }

    public abstract void setupItems(List<ComboBox<?>> allBoxes, List<Slider> allSliders);
    
    public abstract void initSelections(ConfigLoader configLoader);
    
    public abstract void saveIfChanged(Function<Control, Boolean> hasChanged, ConfigLoader configLoader);
}
