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
import java.util.Date;
import java.util.ResourceBundle;
import java.util.function.Consumer;

public class CueSelectionView implements Initializable {

    @FXML
    FixedCueList cueList;

    private ResourceBundle strings;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.strings = resourceBundle;
    }

    /**
     * @see CueSelectionView#showCueSelectionView(CueSelection, Stage, Runnable, Consumer, HumanCareer)
     */
    public static void showCueSelectionView(CueSelection cueSelection,
                                            Stage parentStage,
                                            Runnable beforeSelectionChanged,
                                            Consumer<CueSelection.CueAndBrand> afterSelectionChanged) {
        showCueSelectionView(cueSelection,
                parentStage,
                beforeSelectionChanged,
                afterSelectionChanged,
                null);
    }

    /**
     * @param cueSelection           selection list
     * @param parentStage            parent window
     * @param beforeSelectionChanged run at the "select" button is fired, but before the selection
     *                               changed
     * @param afterSelectionChanged  run after the selection changed, the argument is the newly
     *                               selected cue and brand
     * @param humanCareer            human career if this is invoked in a career game, 
     *                               otherwise null
     */
    public static void showCueSelectionView(CueSelection cueSelection,
                                            Stage parentStage,
                                            Runnable beforeSelectionChanged,
                                            Consumer<CueSelection.CueAndBrand> afterSelectionChanged,
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
                            Consumer<CueSelection.CueAndBrand> afterSelectionChanged,
                            HumanCareer humanCareer,
                            Stage stage) {
        cueList.clear();
        cueList.setDisplayComparator((a, b) -> {
            // 1. 最近选的在前
            int selTimeCmp = -a.getCueInstance().getLastSelectTime().compareTo(b.getCueInstance().getLastSelectTime());
            if (selTimeCmp != 0) return selTimeCmp;
            if (humanCareer != null) {
                // 2. 如果是生涯模式，则把最近买的放在前
                int purchaseTimeCmp = -a.getCueInstance().getCreationTime().compareTo(b.getCueInstance().getCreationTime());
                if (purchaseTimeCmp != 0) return purchaseTimeCmp;
            }
            // 3. 贵的在前
            return -Integer.compare(a.brand.getPrice(), b.brand.getPrice());
        });
        for (CueSelection.CueAndBrand cue : cueSelection.getAvailableCues()) {
            Runnable buttonCallback = () -> {
                if (beforeSelectionChanged != null) {
                    beforeSelectionChanged.run();
                }
                cueSelection.select(cue);
                if (afterSelectionChanged != null) {
                    afterSelectionChanged.accept(cue);
                }
                stage.close();
            };

            if (humanCareer == null) {
                cueList.addCue(cue,
                        780,
                        strings.getString("select"),
                        cue == cueSelection.getSelected() ?
                                null : buttonCallback,
                        false
                );
            } else {
                cueList.addCue(cue,
                        780,
                        humanCareer,
                        () -> reloadList(cueSelection,
                                beforeSelectionChanged,
                                afterSelectionChanged,
                                humanCareer,
                                stage),
                        strings.getString("select"),
                        cue == cueSelection.getSelected() ?
                                null : buttonCallback,
                        false
                );
            }
        }
        cueList.display();
    }
}
