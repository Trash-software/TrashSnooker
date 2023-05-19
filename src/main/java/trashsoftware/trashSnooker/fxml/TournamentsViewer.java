package trashsoftware.trashSnooker.fxml;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.ChampDataManager;
import trashsoftware.trashSnooker.core.career.ChampionshipData;
import trashsoftware.trashSnooker.fxml.widgets.TournamentItemView;

import java.net.URL;
import java.util.ResourceBundle;

public class TournamentsViewer extends ChildInitializable {
    
    @FXML
    HBox rootBox;
    @FXML
    TableView<ChampionshipData> dataTable;
    @FXML
    TableColumn<ChampionshipData, String> champNameCol, champDateCol, champRuleCol;
    
    private ChampDataManager champDataManager;
    
    private ResourceBundle strings;
    private ChampionshipData initSel;
    private Stage stage;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.strings = resourceBundle;
        
        this.champDataManager = ChampDataManager.getInstance();
        
        initTable();
        fillTable();
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    public void initialSelection(ChampionshipData initialSelection) {
        this.initSel = initialSelection;
        
        if (initialSelection != null) {
            dataTable.getSelectionModel().select(initialSelection);
        }
    }
    
    private void initTable() {
        champNameCol.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getName()));
        champDateCol.setCellValueFactory(param -> new ReadOnlyStringWrapper(
                param.getValue().getMonth() + "/" + param.getValue().getDay()
        ));
        champRuleCol.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getType().toString()));
        
        dataTable.getSelectionModel().selectedItemProperty().addListener(((observable, oldValue, newValue) -> {
            if (newValue != null) {
                setRightPane(newValue);
            }
        }));
    }
    
    private void fillTable() {
        dataTable.getItems().clear();
        for (ChampionshipData data : champDataManager.getChampionshipData()) {
            dataTable.getItems().add(data);
        }
    }
    
    private void setRightPane(ChampionshipData data) {
        if (rootBox.getChildren().size() > 1) {
            rootBox.getChildren().remove(1);
        }
        
        TournamentItemView tiv = new TournamentItemView();
        tiv.setData(data);
        
        rootBox.getChildren().add(tiv);
    }
}
