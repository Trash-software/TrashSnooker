package trashsoftware.trashSnooker.core;

import trashsoftware.trashSnooker.core.phy.Phy;

public class CueBackPredictor {
    
    private final static double INTERVAL = 1.0;  // 每个预测的间隔，毫米
    
    private final double[] unitVec;
    private final double dx;
    private final double dy;
    private final double cueRadius;
    private final double x;
    private final double y;
    private final double maxDistance;
    private final Game<?, ?> game;
    
    CueBackPredictor(Game<?, ?> game, double cursorPointingX, double cursorPointingY,
                     double cueWidth, double maxDistance, double whiteX, double whiteY) {
        this.game = game;
        this.x = whiteX;
        this.y = whiteY;
        this.cueRadius = cueWidth / 2;
        this.maxDistance = maxDistance;
        
        unitVec = Algebra.unitVector(cursorPointingX, cursorPointingY);
        
        dx = -unitVec[0] * INTERVAL;
        dy = -unitVec[1] * INTERVAL;
    }
    
    public Result predict() {
        Phy phy = game.entireGame.predictPhy;
        CueBackPredictObject predictor = 
                new CueBackPredictObject(game.gameValues, cueRadius, INTERVAL);
        
        predictor.setX(x);
        predictor.setY(y);
        predictor.setVx(dx);
        predictor.setVy(dy);
        
        while (predictor.distance < maxDistance) {
            // 检测后方障碍球
            predictor.prepareMove(phy);
            Result res = checkBalls(predictor);
            if (res != null) return res;
            
            if (predictor.willPot(phy)) {
                return new CushionObstacle(predictor.distance + game.gameValues.table.midHoleDiameter,
                        game.gameValues.table.cushionHeight, 0.0);
            }
            
            // 检测袋口区域
            ObjectOnTable.CushionHitResult holeAreaResult = predictor.tryHitHoleArea(phy);
            if (holeAreaResult != null && holeAreaResult.result() != 0) {
                // 袋口区域
                Result res2 = checkBalls(predictor);
                if (res2 != null) return res2;
                if (holeAreaResult.result() == 2) return new CushionObstacle(predictor.distance, 
                        game.gameValues.table.cushionHeight, 0.0);
                continue;
            }
            // 检测裤边
            double[] cushionHit = predictor.hitWall();
            if (cushionHit != null) {
                double angle = Algebra.thetaBetweenVectors(unitVec, cushionHit) - Algebra.HALF_PI;
                return new CushionObstacle(predictor.distance, 
                        game.gameValues.table.cushionHeight, 
                        angle);
            }
            
            predictor.normalMove(phy);
        }
        return null;
    }
    
    private Result checkBalls(CueBackPredictObject predictor) {
        double threshold = cueRadius + game.gameValues.ball.ballRadius;
        for (Ball ball : game.getAllBalls()) {
            if (!ball.isNotOnTable() && !ball.isWhite()) {
                if (Algebra.distanceToPoint(predictor.x, predictor.y, 
                        ball.x, ball.y) < threshold) {
                    return new BallObstacle(predictor.distance + game.gameValues.ball.ballRadius, ball);
                }
            }
        }
        return null;
    }
    
    public abstract static class Result {
        public final double distance;  // 白球球心与障碍物的距离
        
        Result(double distance) {
            this.distance = distance;
        }
    }
    
    public static class BallObstacle extends Result {
        public final Ball obstacle;

        BallObstacle(double distance, Ball obstacle) {
            super(distance);
            
            this.obstacle = obstacle;
        }
    }
    
    public static class CushionObstacle extends Result {
        public final double height;
        public final double relativeAngle;

        CushionObstacle(double distance, double height, double relativeAngleRad) {
            super(distance);
            
            this.height = height;
            this.relativeAngle = relativeAngleRad;
        }
    }
}
