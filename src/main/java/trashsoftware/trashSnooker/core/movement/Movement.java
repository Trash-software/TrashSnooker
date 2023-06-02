package trashsoftware.trashSnooker.core.movement;

import trashsoftware.trashSnooker.core.Ball;

import java.util.*;

public class Movement {

    private final Map<Ball, List<MovementFrame>> movementMap = new HashMap<>();  // 必须是hashmap，详见ball.compareTo()
//    private final Map<Ball, Deque<MovementFrame>> immutableMap = new HashMap<>();
    private final Map<Ball, MovementFrame> startingPositions = new HashMap<>();
//    private double iterationIndex = 0;
    private final Ball anyBall;
    private boolean congested = false;

    public Movement(Ball[] allBalls) {
        anyBall = allBalls[0];
        for (Ball ball : allBalls) {
            List<MovementFrame> positionList = new ArrayList<>();
            MovementFrame frame = new MovementFrame(ball.getX(), ball.getY(), 
                    ball.getAxisX(), ball.getAxisY(), ball.getAxisZ(), ball.getFrameDegChange(),
                    ball.isPotted(), MovementFrame.NORMAL, 0.0);
            positionList.add(frame);

//            List<MovementFrame> positionListImm = new ArrayDeque<>();
//            positionListImm.addFirst(frame);
            movementMap.put(ball, positionList);
//            immutableMap.put(ball, positionListImm);
            startingPositions.put(ball, frame);
        }
    }

    public void setCongested() {
        this.congested = true;
    }

    public boolean isCongested() {
        return congested;
    }

    //    public boolean hasNext() {
//        return iterationIndex < movementMap.get(anyBall).size();
//    }
//    
//    public int incrementIndex() {
//        return (int) iterationIndex++;
//    }
//    
//    public int incrementIndex(double nFrames) {
//        int cur = (int) iterationIndex;
//        iterationIndex = Math.min(iterationIndex + nFrames, movementMap.get(anyBall).size());
//        return cur;
//    }
    
    public boolean isInRange(int index) {
        return index < movementMap.get(anyBall).size();
    }
    
    public int getNFrames() {
        return movementMap.get(anyBall).size();
    }
    
//    public void reset() {
//        iterationIndex = 0;
//    }
    
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
}
