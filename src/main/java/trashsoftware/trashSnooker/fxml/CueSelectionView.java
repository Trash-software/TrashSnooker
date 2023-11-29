package trashsoftware.trashSnooker.fxml;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import trashsoftware.trashSnooker.core.CueSelection;
import trashsoftware.trashSnooker.core.career.HumanCareer;
import trashsoftware.trashSnooker.fxml.widgets.FixedCueList;
import trashsoftware.trashSnooker.util.EventLogger;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class CueSelectionView implements Initializable {
    
    @FXML
    FixedCueList cueList;
    
    private ResourceBundle strings;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.strings = resourceBundle;
    }
    
    public static void showCueSelectionView(CueSelection cueSelection,
                                            Stage parentStage,
                                            Runnable beforeSelectionChanged,
                                            Runnable afterSelectionChanged) {
        showCueSelectionView(cueSelection, 
                parentStage, 
                beforeSelectionChanged, 
                afterSelectionChanged, 
                null);
    }

    public static void showCueSelectionView(CueSelection cueSelection,
                                            Stage parentStage,
                                            Runnable beforeSelectionChanged,
                                            Runnable afterSelectionChanged,
                                            HumanCareer humanCareer) {
        ResourceBundle strings = App.getStrings();
        try {
            FXMLLoader loader = new FXMLLoader(
                    CueSelectionView.class.getResource("cueSelectionView.fxml"),
                    strings
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            Stage stage = new Stage();
            stage.initOwner(parentStage);
            stage.initModality(Modality.WINDOW_MODAL);

            Scene scene = App.createScene(root);
            stage.setScene(scene);

            stage.show();

            CueSelectionView view = loader.getController();
            view.reloadList(cueSelection,
                    beforeSelectionChanged,
                    afterSelectionChanged,
                    humanCareer,
                    stage);
            
            stage.sizeToScene();
        } catch (IOException e) {
            EventLogger.error(e);
        }
    }
    
    private void reloadList(CueSelection cueSelection,
                            Runnable beforeSelectionChanged,
                            Runnable afterSelectionChanged,
                            HumanCareer humanCareer,
                            Stage stage) {
        cueList.clear();
        for (CueSelection.CueAndBrand cue : cueSelection.getAvailableCues()) {
            Runnable buttonCallback = () -> {
                if (beforeSelectionChanged != null) {
                    beforeSelectionChanged.run();
                }
                cueSelection.select(cue);
                if (afterSelectionChanged != null) {
                    afterSelectionChanged.run();
                }
                stage.close();
            };

            if (humanCareer == null) {
                cueList.addCue(cue,
                        640,
                        strings.getString("select"),
                        cue == cueSelection.getSelected() ?
                                null : buttonCallback
                );
            } else {
                cueList.addCue(cue,
                        640,
                        humanCareer,
                        () -> reloadList(cueSelection,
                                beforeSelectionChanged,
                                afterSelectionChanged,
                                humanCareer,
                                stage),
                        strings.getString("select"),
                        cue == cueSelection.getSelected() ?
                                null : buttonCallback
                );
            }
        }
    }
}
