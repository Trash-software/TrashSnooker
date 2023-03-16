package trashsoftware.trashSnooker.core;

import trashsoftware.trashSnooker.core.table.Table;

public interface GameHolder {
    
    Table getTable();
    
    Ball getBallByValue(int value);
    
    Ball getCueBall();
    
    Ball[] getAllBalls();
}
