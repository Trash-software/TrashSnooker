package trashsoftware.trashSnooker.fxml;

import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import trashsoftware.trashSnooker.recorder.BriefReplayItem;
import trashsoftware.trashSnooker.recorder.GameReplay;
import trashsoftware.trashSnooker.recorder.NaiveGameReplay;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class ReplayView implements Initializable {
    
    @FXML
    TableView<BriefReplayItem> replayTable;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        TableColumn<BriefReplayItem, ?> replayCol = replayTable.getColumns().get(0);
        TableColumn<BriefReplayItem, ?> p1 = replayTable.getColumns().get(1);
        TableColumn<BriefReplayItem, ?> p2 = replayTable.getColumns().get(2);
        
        replayCol.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        p1.setCellValueFactory(new PropertyValueFactory<>("p1Name"));
        p2.setCellValueFactory(new PropertyValueFactory<>("p2Name"));
        
        fill();
    }
    
    @FXML
    void playAction() throws IOException {
        BriefReplayItem selected = replayTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            GameReplay replay = GameReplay.loadReplay(selected);
            
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("gameView.fxml")
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            Stage stage = new Stage();
//            stage.initOwner(this.stage);
            stage.initModality(Modality.WINDOW_MODAL);

            Scene scene = new Scene(root);
            stage.setScene(scene);

            GameView gameView = loader.getController();
            gameView.setupReplay(stage, replay);

            stage.show();
        }
    }
    
    private void fill() {
        File[] replays = GameReplay.listReplays();
        if (replays != null) {
            for (File f : replays) {
                try {
                    BriefReplayItem item = new BriefReplayItem(f);
                    replayTable.getItems().add(item);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
