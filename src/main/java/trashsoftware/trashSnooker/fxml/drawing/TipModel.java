package trashsoftware.trashSnooker.fxml.drawing;

import javafx.scene.Group;
import trashsoftware.trashSnooker.core.cue.CueTip;

public abstract class TipModel extends Group {
    
    protected double scale = 1.0;
    
    public static TipModel create(CueTip cueTip) {
        if (cueTip.isRest) {
            return new RestTipModel(cueTip.getBrand().id());
        } else {
            return new CueTipModel(cueTip);
        }
    }
    
    public void setScale(double scale) {
        this.scale = scale;
        
        build();
    }
    
    protected abstract void build();
}
