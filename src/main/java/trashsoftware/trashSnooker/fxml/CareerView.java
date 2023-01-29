package trashsoftware.trashSnooker.fxml;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.CareerRank;
import trashsoftware.trashSnooker.core.career.ChampionshipData;
import trashsoftware.trashSnooker.core.career.championship.Championship;
import trashsoftware.trashSnooker.fxml.widgets.AbilityShower;
import trashsoftware.trashSnooker.fxml.widgets.PerkManager;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;

import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.ResourceBundle;

public class CareerView implements Initializable {

    @FXML
    TableView<CareerRank> snookerRankingTable;
    @FXML
    TableColumn<CareerRank, Integer> snookerRankCol, snookerAwardCol;
    @FXML
    TableColumn<CareerRank, String> snookerRankNameCol;

    @FXML
    AbilityShower abilityShower;
    @FXML
    Label myRankLabel;
    @FXML
    Label currentDateLabel;
    @FXML
    Label availPerksLabel;
    @FXML
    Button confirmAddPerkBtn, clearPerkBtn;
    @FXML
    Label nextChampionshipLabel, champInProgLabel, champInProgStageLabel;
    @FXML
    CheckBox joinChampBox;

    @FXML
    Pane champInProgBox, nextChampInfoBox;

    CareerManager careerManager;
    private PerkManager perkManager;
    private Stage selfStage;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        careerManager = CareerManager.getInstance();

        abilityShower.setup(careerManager.getHumanPlayerCareer().getPlayerPerson(), true);
        perkManager = new PerkManager(this, careerManager.getHumanPlayerCareer().getAvailablePerks());
        abilityShower.setPerkManager(perkManager);
        initTable();

        refreshGui();
    }

    public void setSelfStage(Stage selfStage) {
        this.selfStage = selfStage;
    }

    private void initTable() {
        snookerRankCol.setCellValueFactory(new PropertyValueFactory<>("rankFrom1"));
        snookerRankNameCol.setCellValueFactory(param ->
                new ReadOnlyStringWrapper(param.getValue().career.getPlayerPerson().getName()));
        snookerAwardCol.setCellValueFactory(new PropertyValueFactory<>("recentAwards"));
    }
    
    public void refreshGui() {
        refreshRanks();
        currentDateLabel.setText(String.format("%d/%d/%d", 
                careerManager.getTimestamp().get(Calendar.YEAR),
                careerManager.getTimestamp().get(Calendar.MONTH) + 1,
                careerManager.getTimestamp().get(Calendar.DAY_OF_MONTH)));

        Championship inProgress = careerManager.getChampionshipInProgress();
//        System.out.println(inProgress);
        if (inProgress == null) {
            champInProgBox.setVisible(false);
            champInProgBox.setManaged(false);

            nextChampInfoBox.setVisible(true);
            nextChampInfoBox.setManaged(true);
            ChampionshipData.WithYear nextData = careerManager.nextChampionshipData();
            if (careerManager.humanPlayerQualifiedToJoinSnooker(nextData.data)) {
                joinChampBox.setDisable(false);
                joinChampBox.setSelected(true);
            } else {
                joinChampBox.setDisable(true);
                joinChampBox.setSelected(false);
            }
            nextChampionshipLabel.setText(nextData.fullName());
        } else {
            champInProgBox.setVisible(true);
            champInProgBox.setManaged(true);
            
            nextChampInfoBox.setVisible(false);
            nextChampInfoBox.setManaged(false);
            
            champInProgLabel.setText(inProgress.fullName());
            champInProgStageLabel.setText(inProgress.getCurrentStage().shown);
        }
    }

    private void refreshRanks() {
        snookerRankingTable.getItems().clear();
        snookerRankingTable.getItems().addAll(careerManager.getSnookerRanking());

        CareerRank myRank = careerManager.humanPlayerSnookerRanking();
        myRankLabel.setText(String.format("%d  %s  %d",
                myRank.getRankFrom1(),
                myRank.career.getPlayerPerson().getName(),
                myRank.recentAwards));

        availPerksLabel.setText(String.valueOf(myRank.career.getAvailablePerks()));
    }

    @FXML
    public void clearUsedPerks() {
        perkManager.clearSelections();
        abilityShower.noticePerksReset();

        noticePerksChanged();
    }

    @FXML
    public void applyPerks() {
        int used = perkManager.applyPerks();
        careerManager.getHumanPlayerCareer().usePerk(used);

        DataLoader.getInstance().updatePlayer(perkManager.getAbility().toPlayerPerson());
        careerManager.reloadHumanPlayerPerson();

        noticePerksChanged();

        abilityShower.noticePerksReset();
    }

    @FXML
    public void nextChamp() {
        Championship championship = careerManager.startNextChampionship();
        championship.startChampionship(joinChampBox.isSelected());
        
        refreshGui();
        
        showChampDrawView();
    }

    @FXML
    public void continueChampInProg() {
        showChampDrawView();
    }
    
    private void showChampDrawView() {
        try {
            Stage stage = new Stage();
            stage.initModality(Modality.WINDOW_MODAL);
            stage.initOwner(selfStage);

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("champDrawView.fxml")
            );
            Parent root = loader.load();
            root.setStyle(App.FONT_STYLE);

            ChampDrawView view = loader.getController();
            view.setup(this, stage);
//            mainView.setup(parentStage, stage);

            Scene scene = new Scene(root);

//            Scene scene = new Scene(root, -1, -1, false, SceneAntialiasing.BALANCED);
//            scene.getStylesheets().add(getClass().getResource("/trashsoftware/trashSnooker/css/font.css").toExternalForm());
            stage.setScene(scene);

            stage.show();
        } catch (IOException e) {
            EventLogger.log(e);
        }
    }

    public void noticePerksChanged() {
        availPerksLabel.setText(perkManager.getAvailPerks() + "");
    }
}
