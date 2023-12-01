package trashsoftware.trashSnooker.fxml.inventoryPages;

import javafx.fxml.FXML;
import org.jetbrains.annotations.NotNull;
import trashsoftware.trashSnooker.core.CueSelection;
import trashsoftware.trashSnooker.core.career.CareerManager;
import trashsoftware.trashSnooker.core.career.CareerSave;
import trashsoftware.trashSnooker.core.career.HumanCareer;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.cue.CueBrand;
import trashsoftware.trashSnooker.core.cue.CueTip;
import trashsoftware.trashSnooker.core.cue.CueTipBrand;
import trashsoftware.trashSnooker.fxml.App;
import trashsoftware.trashSnooker.fxml.alert.AlertShower;
import trashsoftware.trashSnooker.fxml.widgets.FixedCueList;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.Util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.ResourceBundle;

public class StorePage extends AbsInvPage {

    @FXML
    FixedCueList cueList;

    private final HumanCareer humanCareer;

    public StorePage() {
        this(App.getStrings());
    }

    public StorePage(ResourceBundle strings) {
        super("storePage.fxml", strings);

        humanCareer = CareerManager.getInstance().getHumanPlayerCareer();

        reload();
    }

    private void fill() {
        cueList.clear();
        List<Cue> haves = inventoryManager.getAllCues();
        List<CueSelection.CueAndBrand> notHaves = getNotHavingCues(haves);

        notHaves.sort(Comparator.comparingInt(x -> x.brand.getPrice()));
        for (CueSelection.CueAndBrand cab : notHaves) {
            cab.initInstanceForViewing();
            cueList.addCue(cab,
                    900,
                    strings.getString("buy") + " - " + cab.brand.getPrice(),
                    () -> askBuyCue(cab));
        }
    }

    @NotNull
    private static List<CueSelection.CueAndBrand> getNotHavingCues(List<Cue> haves) {
        List<CueSelection.CueAndBrand> notHaves = new ArrayList<>();
        OUT_LOOP:
        for (CueBrand cueBrand : DataLoader.getInstance().getCues().values()) {
            if (cueBrand.isAvailable()) {
                for (Cue have : haves) {
                    if (have.getBrand().getCueId().equals(cueBrand.getCueId())) {
                        continue OUT_LOOP;
                    }
                }
                // todo: 预览版instance
                CueSelection.CueAndBrand cab = new CueSelection.CueAndBrand(cueBrand);
                notHaves.add(cab);
            }
        }
        return notHaves;
    }

    void askBuyCue(CueSelection.CueAndBrand cab) {
        int curMoney = humanCareer.getMoney();
        int price = cab.brand.getPrice();
        int moneyAfterBuy = curMoney - price;
        if (moneyAfterBuy < 0) {
            AlertShower.showInfo(
                    inventoryView.getStage(),
                    strings.getString("noMoney"),
                    strings.getString("cannotPurchase")
            );
        } else {
            AlertShower.askConfirmation(
                    inventoryView.getStage(),
                    String.format(strings.getString("balanceAfterPurchase"),
                            Util.moneyToReadable(curMoney),
                            Util.moneyToReadable(price),
                            Util.moneyToReadable(moneyAfterBuy)
                    ),
                    String.format(strings.getString("confirmPurchase")),
                    () -> buyCue(cab.brand, price),
                    null
            );
        }
    }

    void buyCue(CueBrand cueBrand, int price) {
        CareerSave save = CareerManager.getInstance().getCareerSave();
        CueTip tip = CueTip.createByCue(cueBrand,
                CueTipBrand.getById("stdTip"),
                save);
        Cue newCue = Cue.createForCareer(cueBrand, tip, save);
        humanCareer.buyCue(newCue.getInstanceId(), price);

        inventoryManager.addTip(tip);
        inventoryManager.addCue(newCue);
        inventoryManager.saveToDisk();

        CareerManager.getInstance().saveToDisk();

        System.out.println("Cue bought!");

        AlertShower.showInfo(inventoryView.getStage(),
                strings.getString("purchaseCompleteDes") + "\n" +
                strings.getString("purchaseCueDes"),
                strings.getString("purchaseComplete"));

        // 注意：如果有正在进行中的比赛，新杆需在下场比赛开始时才会生效
        inventoryView.updateView();
    }

    @Override
    public void reload() {
        fill();
    }
}
