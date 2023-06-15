package trashsoftware.trashSnooker.core;

import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.phy.Phy;

public class CueBackPredictObject extends ObjectOnTable {
    
    final double eachMove;
    
    public CueBackPredictObject(GameValues gameValues, double radius, double eachMove) {
        super(gameValues, radius);
        
        this.eachMove = eachMove;
    }

    @Override
    protected void normalMove(Phy phy) {
        setX(nextX);
        setY(nextY);
        distance += eachMove;
    }
    
    double[] hitWall() {
        if (nextX < radius + table.leftX) return Ball.LEFT_CUSHION_VEC;
        if (nextX >= table.rightX - radius) return Ball.RIGHT_CUSHION_VEC;
        if (nextY < radius + table.topY) return Ball.TOP_CUSHION_VEC;
        if (nextY >= table.botY - radius) return Ball.BOT_CUSHION_VEC;
            
        return null;
    }
}
