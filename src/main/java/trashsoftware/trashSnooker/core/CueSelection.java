package trashsoftware.trashSnooker.core;

import javafx.scene.control.Button;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.cue.CueBrand;
import trashsoftware.trashSnooker.util.EventLogger;

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
                return;
            }
        }
        EventLogger.warning("Failed to selected by brand: " + brand.cueId + ". \nInventory: ");
        for (CueAndBrand cab : available) {
            EventLogger.warning(cab.brand.cueId);
        }
    }

    public void selectByCue(Cue cue) {
        for (CueAndBrand cab : available) {
            if (cab.instance != null && cue.getInstanceId().equals(cab.instance.getInstanceId())) {
                select(cab);
                break;
            }
        }
    }
    
    public void selectByInstanceId(String instanceId) {
        for (CueAndBrand cab : available) {
            if (cab.instance != null && instanceId.equals(cab.instance.getInstanceId())) {
                select(cab);
                break;
            }
        }
    }

    public List<CueAndBrand> getAvailableCues() {
        return available;
    }

    public CueAndBrand getSelected() {
        return selected;
    }
    
    public boolean hasThisBrand(CueBrand brand) {
        for (CueAndBrand cab : getAvailableCues()) {
            if (cab.brand.getCueId().equals(brand.getCueId())) return true;
        }
        return false;
    }

    public static class CueAndBrand {
        public final CueBrand brand;
        private Cue instance;  // fast game选杆界面为null

        CueAndBrand(CueBrand brand, Cue instance) {
            this.brand = brand;
            this.instance = instance;
        }

        public CueAndBrand(CueBrand brand) {
            this(brand, null);
        }
        
        public CueAndBrand(Cue instance) {
            this(instance.getBrand(), instance);
        }

        public Cue getCueInstance() {
            return instance;
        }
        
        public void initInstanceForViewing() {
            getNonNullInstance(InstanceType.TEMP_VIEW);
        }

        public Cue getNonNullInstance() {
            return getNonNullInstance(InstanceType.REGULAR);
        }

        public Cue getNonNullInstance(InstanceType instanceType) {
            if (instance == null) {
                if (brand.isRest) {
                    instance = Cue.createRest(brand);
                } else {
                    instance = switch (instanceType) {
                        case REGULAR -> Cue.createOneTimeInstance(brand);
                        case TEMP_VIEW -> Cue.createForTempView(brand);
                    };
                }
            }
            return instance;
        }
    }
    
    public enum InstanceType {
        REGULAR,
        TEMP_VIEW
    }
}
