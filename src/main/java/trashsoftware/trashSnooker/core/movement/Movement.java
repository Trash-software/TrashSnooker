package trashsoftware.trashSnooker.core.movement;

import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.metrics.Cushion;

import java.util.*;

public class Movement {

    private final Map<Ball, List<MovementFrame>> movementMap = new HashMap<>();  // 必须是hashmap，详见ball.compareTo()
    private final Map<Ball, MovementFrame> startingPositions = new HashMap<>();
    private final Ball anyBall;
    private boolean congested = false;
    private transient Trace whiteTrace;  // 仅用于游戏，不用于录像
    private transient Trace targetTrace;  // 仅用于游戏，不用于录像
    private transient Ball whiteFirstCollide;  // 仅用于录像，不用于游戏

    public Movement(Ball[] allBalls) {
        anyBall = allBalls[0];
        for (Ball ball : allBalls) {
            List<MovementFrame> positionList = new ArrayList<>();
            MovementFrame frame = new MovementFrame(ball.getX(), ball.getY(), 
                    ball.getAxisX(), ball.getAxisY(), ball.getAxisZ(), ball.getFrameDegChange(),
                    ball.isPotted(), MovementFrame.NORMAL, 0.0);
            positionList.add(frame);
            
            movementMap.put(ball, positionList);
            startingPositions.put(ball, frame);
        }
    }
    
    public void setupReplay() {
        Ball cueBall = null;
        for (Ball ball : movementMap.keySet()) {
            if (ball.isWhite()) {
                cueBall = ball;
                break;
            }
        }
        if (cueBall == null) throw new RuntimeException("No cue ball in this movement");
        
        // 实则是找到第一颗动的非白球
        // 少发性bug:白球在同一录像帧接触两颗目标球
        int nFrames = getNFrames();
        
        OUT_LOOP:
        for (int i = 0; i < nFrames - 1; i++) {
            for (Map.Entry<Ball, List<MovementFrame>> entry : movementMap.entrySet()) {
                if (entry.getKey().isWhite()) continue;  // 不管白球
                MovementFrame curFrame = entry.getValue().get(i);
                if (curFrame.potted) continue;
                MovementFrame nextFrame = entry.getValue().get(i + 1);
                
                if (nextFrame.x != curFrame.x || nextFrame.y != curFrame.y) {
                    // 这球动了
                    whiteFirstCollide = entry.getKey();
                    break OUT_LOOP;
                }
            }
        }
    }

    /**
     * 仅有Game应调用，Replay不调用
     */
    public void startTrace() {
        whiteTrace = new Trace();
        targetTrace = new Trace();
    }

    public Trace getWhiteTrace() {
        return whiteTrace;
    }

    public Trace getTargetTrace() {
        return targetTrace;
    }

    public void setCongested() {
        this.congested = true;
    }

    public boolean isCongested() {
        return congested;
    }
    
    public boolean isInRange(int index) {
        return index < movementMap.get(anyBall).size();
    }
    
    public int getNFrames() {
        return movementMap.get(anyBall).size();
    }
    
    public void addFrame(Ball ball, MovementFrame frame) {
        movementMap.get(ball).add(frame);
    }

    public Map<Ball, List<MovementFrame>> getMovementMap() {
        return movementMap;
    }

    public Map<Ball, MovementFrame> getStartingPositions() {
        return startingPositions;
    }
    
    public Map<Ball, MovementFrame> getEndingPositions() {
        Map<Ball, MovementFrame> pos = new HashMap<>();
        for (Map.Entry<Ball, List<MovementFrame>> entry : movementMap.entrySet()) {
            pos.put(entry.getKey(), entry.getValue().get(entry.getValue().size() - 1));
        }
        return pos;
    }

    @Override
    public String toString() {
        return "Movement{balls=" + movementMap.size() + "," +
                "frames=" + new ArrayList<>(movementMap.values()).get(0).size() + "}";
    }
    
    public Ball getWhiteFirstCollide() {
        if (whiteTrace != null) {
            return whiteTrace.getFirstCollision();
        } else {
            return whiteFirstCollide;
        }
    }
    
    public static class Trace {
        private final List<Cushion> cushionBefore = new ArrayList<>();
        private final List<Cushion> cushionAfter = new ArrayList<>();
        private final List<Ball> collisions = new ArrayList<>();  // 只计白球的
        private double distanceMoved;
        
        public void hitCushion(Cushion cushion) {
            if (collisions.isEmpty()) {
                cushionBefore.add(cushion);
            } else {
                cushionAfter.add(cushion);
            }
        }
        
        public Ball getFirstCollision() {
            return collisions.isEmpty() ? null : collisions.get(0);
        }

        public void setDistanceMoved(double distanceMoved) {
            this.distanceMoved = distanceMoved;
        }

        /**
         * 注意！这里是白球的总里程，包括碰目标球之前的里程
         */
        public double getDistanceMoved() {
            return distanceMoved;
        }

        public void hitBall(Ball ball) {
            collisions.add(ball);
        }

        public List<Ball> getCollisions() {
            return collisions;
        }

        public List<Cushion> getCushionBefore() {
            return cushionBefore;
        }

        public List<Cushion> getCushionAfter() {
            return cushionAfter;
        }
    }
}
