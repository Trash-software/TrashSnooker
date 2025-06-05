package trashsoftware.trashSnooker.fxml.drawing;

import trashsoftware.trashSnooker.core.cue.TexturedCueBrand;
import trashsoftware.trashSnooker.core.person.PlayerHand;

import java.util.ArrayList;
import java.util.List;

public class CueExtensionModel3D {
    
    final PlayerHand.CueExtension type;
    final List<TexturedCueBrand.Segment> segments = new ArrayList<>();
    
    CueExtensionModel3D(PlayerHand.CueExtension type) {
        this.type = type;
    }

    public List<TexturedCueBrand.Segment> getSegments() {
        return segments;
    }
}
