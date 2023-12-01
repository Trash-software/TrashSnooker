package trashsoftware.trashSnooker.fxml;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.stage.Stage;
import trashsoftware.trashSnooker.core.CueSelection;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.HumanCareer;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.cue.CueTip;
import trashsoftware.trashSnooker.core.cue.CueTipBrand;
import trashsoftware.trashSnooker.fxml.alert.AlertShower;
import trashsoftware.trashSnooker.fxml.widgets.FixedTipList;
import trashsoftware.trashSnooker.util.Util;

import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

public class BuyTipView implements Initializable {
    
    @FXML
    FixedTipList tipList;
    
    private ResourceBundle strings;
    private Stage thisStage;
    private HumanCareer humanCareer;
    private Cue cue;
    private Runnable changeTipCallback;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        this.strings = resourceBundle;
    }
    
    public void setup(Stage thisStage, 
                      HumanCareer humanCareer, 
                      Cue cue, 
                      Runnable changeTipCallback) {
        this.thisStage = thisStage;
        this.humanCareer = humanCareer;
        this.cue = cue;
        this.changeTipCallback = changeTipCallback;
    }
    
    public void fillTipBrands() {
        List<CueTipBrand> brands = CueTipBrand.listAll();
        
        brands.sort(Comparator.comparingInt(CueTipBrand::price));
        for (CueTipBrand tipBrand : brands) {
            tipList.addTip(tipBrand, cue.getBrand(), () -> confirmBuy(tipBrand));
        }
    }
    
    void confirmBuy(CueTipBrand tipBrand) {
        AlertShower.askConfirmation(
                thisStage,
                String.format(strings.getString("changeTipPrice"),
                        Util.moneyToReadable(humanCareer.getMoney() - tipBrand.price())),
                String.format(strings.getString("changeTipConfirm"),
                        Util.moneyToReadable(tipBrand.price())),
                () -> {
                    CueTip newTip = CueTip.createByCue(cue.getBrand(),
                            tipBrand,
                            CareerManager.getInstance().getCareerSave());
                    humanCareer.buyCueTip(newTip.getInstanceId(), tipBrand.price());
                    humanCareer.getInventory().installTip(
                            newTip,
                            cue);
                    Platform.runLater(changeTipCallback);
                    thisStage.close();
                },
                null
        );
    }
}
