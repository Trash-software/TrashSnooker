package trashsoftware.trashSnooker.fxml.drawing;

public interface GameLoop {
    
    void start();
    
    void stop();
    
    void beginNewAnimation();

    double lastAnimationFrameMs();

    long msSinceAnimationBegun();
}
