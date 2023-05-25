package trashsoftware.trashSnooker.core.training;

public interface Training {
    
    TrainType getTrainType();

    /**
     * @return 挑战模式内容，如果是自由训练则为null
     */
    Challenge getChallenge();
}
