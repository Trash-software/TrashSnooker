package trashsoftware.trashSnooker.fxml.drawing;

public interface GameLoop {
    
    void start();
    
    void stop();
    
    void beginNewAnimation();

    double lastAnimationFrameMs();

    long msSinceAnimationBegun();

    /**
     * @return 时间轴上的参考时间，不一定要在某个具体时间开始
     */
    long currentTimeMillis();
}
