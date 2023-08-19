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
        if (nextX < radius + table.leftX) return table.leftCushion.getVector();
        if (nextX >= table.rightX - radius) return table.rightCushion.getVector();
        if (nextY < radius + table.topY) return table.topLeftCushion.getVector();  // topLeft和topRight的vector一样的
        if (nextY >= table.botY - radius) return table.botRightCushion.getVector();
            
        return null;
    }
}
