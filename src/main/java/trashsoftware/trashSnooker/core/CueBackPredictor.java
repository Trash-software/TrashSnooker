package trashsoftware.trashSnooker.core;

public class CueBackPredictor {
    
    private final static double INTERVAL = 1.0;  // 每个预测的间隔，毫米
    
    private final double dx;
    private final double dy;
    private final double cueRadius;
    private final double x;
    private final double y;
    private final double maxDistance;
    private final Game game;
    
    CueBackPredictor(Game game, double cursorPointingX, double cursorPointingY,
                     double cueWidth, double maxDistance, double whiteX, double whiteY) {
        this.game = game;
        this.x = whiteX;
        this.y = whiteY;
        this.cueRadius = cueWidth / 2;
        this.maxDistance = maxDistance;
        
        double[] unitVec = Algebra.unitVector(cursorPointingX, cursorPointingY);
        
        dx = -unitVec[0] * INTERVAL;
        dy = -unitVec[1] * INTERVAL;
    }
    
    public Result predict() {
        Phy phy = Phy.PREDICT;
        CueBackPredictObject predictor = 
                new CueBackPredictObject(game.gameValues, cueRadius, INTERVAL);
        
        predictor.setX(x);
        predictor.setY(y);
        predictor.setVx(dx);
        predictor.setVy(dy);
        
        while (predictor.distance < maxDistance) {
            // 检测后方障碍球
            predictor.prepareMove();
            Result res = checkBalls(predictor);
            if (res != null) return res;
            
            if (predictor.willPot(phy)) {
                return new Result(predictor.distance + game.gameValues.midHoleDiameter,
                        game.gameValues.cushionHeight);
            }
            
            // 检测袋口区域
            int holeAreaResult = predictor.tryHitHoleArea(phy);
            if (holeAreaResult != 0) {
                // 袋口区域
                Result res2 = checkBalls(predictor);
                if (res2 != null) return res2;
                if (holeAreaResult == 2) return new Result(predictor.distance, 
                        game.gameValues.cushionHeight);
                continue;
            }
            // 检测裤边
            if (predictor.hitWall()) {
                return new Result(predictor.distance, game.gameValues.cushionHeight);
            }
            
            predictor.normalMove(phy);
        }
        return null;
    }
    
    private Result checkBalls(CueBackPredictObject predictor) {
        double threshold = cueRadius + game.gameValues.ballRadius;
        for (Ball ball : game.getAllBalls()) {
            if (!ball.isPotted() && !ball.isWhite()) {
                if (Algebra.distanceToPoint(predictor.x, predictor.y, 
                        ball.x, ball.y) < threshold) {
                    return new Result(predictor.distance + game.gameValues.ballRadius,
                            game.gameValues.ballDiameter, ball);
                }
            }
        }
        return null;
    }
    
    public static class Result {
        public final double distance;  // 白球球心与障碍物的距离
        public final double height;
        public final Ball obstacle;
        
        Result(double distance, double height, Ball obstacle) {
            this.distance = distance;
            this.height = height;
            this.obstacle = obstacle;
        }

        Result(double distance, double height) {
            this(distance, height, null);
        }

        @Override
        public String toString() {
            return "Result{" +
                    "distance=" + distance +
                    ", height=" + height +
                    '}';
        }
    }
}
