package trashsoftware.trashSnooker.core;

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
    
    boolean hitWall() {
        if (nextX < radius + values.leftX ||
                nextX >= values.rightX - radius) {
            return true;
        }
        return nextY < radius + values.topY ||
                nextY >= values.botY - radius;
    }
}
