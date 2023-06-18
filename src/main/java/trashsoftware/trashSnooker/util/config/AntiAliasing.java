package trashsoftware.trashSnooker.util.config;

import javafx.scene.SceneAntialiasing;

public enum AntiAliasing {
    DISABLED(SceneAntialiasing.DISABLED, false),
    BALANCED(SceneAntialiasing.BALANCED, true);
    
    public final SceneAntialiasing threeDAA;
    public final boolean canvasAA;
    
    AntiAliasing(SceneAntialiasing threeDAA, boolean canvasAA) {
        this.threeDAA = threeDAA;
        this.canvasAA = canvasAA;
    }
}
