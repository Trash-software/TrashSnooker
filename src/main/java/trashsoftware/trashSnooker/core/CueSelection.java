package trashsoftware.trashSnooker.core;

import javafx.scene.control.Button;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.cue.CueBrand;
import trashsoftware.trashSnooker.fxml.FastGameView;

import java.util.ArrayList;
import java.util.List;

public class CueSelection {
    private final List<CueAndBrand> available = new ArrayList<>();
    private final Button btn;
    private CueAndBrand selected;

    public CueSelection(Button textBtn) {
        this.btn = textBtn;
    }

    public void select(CueAndBrand cue) {
        selected = cue;
        if (btn != null) {
            btn.setText(selected.brand.getName());
        }
    }

    public void selectByBrand(CueBrand brand) {
        for (CueAndBrand cab : available) {
            if (brand.cueId.equals(cab.brand.cueId)) {
                select(cab);
            }
        }
    }

    public void selectByCue(Cue cue) {
        for (CueAndBrand cab : available) {
            if (cab.instance != null && cue.getInstanceId().equals(cab.instance.getInstanceId())) {
                select(cab);
            }
        }
    }

    public List<CueAndBrand> getAvailableCues() {
        return available;
    }

    public CueAndBrand getSelected() {
        return selected;
    }

    public static class CueAndBrand {
        public final CueBrand brand;
        private Cue instance;  // fast game选杆界面为null

        CueAndBrand(CueBrand brand, Cue instance) {
            this.brand = brand;
            this.instance = instance;
        }

        CueAndBrand(CueBrand brand) {
            this(brand, null);
        }

        public Cue getCueInstance() {
            return instance;
        }

        public Cue getNonNullInstance() {
            if (instance == null) {
                instance = Cue.createForFastGame(brand);
            }
            return instance;
        }
    }
}
