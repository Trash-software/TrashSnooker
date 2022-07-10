package trashsoftware.trashSnooker.core.movement;

import trashsoftware.trashSnooker.core.Ball;

import java.util.*;

public class Movement {

    private final Map<Ball, Deque<MovementFrame>> movementMap = new HashMap<>();  // 必须是hashmap，详见ball.compareTo()
    private final Map<Ball, Deque<MovementFrame>> immutableMap = new HashMap<>();
    private final Map<Ball, MovementFrame> startingPositions = new HashMap<>();

    public Movement(Ball[] allBalls) {
        for (Ball ball : allBalls) {
            ArrayDeque<MovementFrame> positionList = new ArrayDeque<>();
            MovementFrame frame = new MovementFrame(ball.getX(), ball.getY(), ball.isPotted());
            positionList.addFirst(frame);

            ArrayDeque<MovementFrame> positionListImm = new ArrayDeque<>();
            positionListImm.addFirst(frame);
            movementMap.put(ball, positionList);
            immutableMap.put(ball, positionListImm);
            startingPositions.put(ball, frame);
        }
    }
    
    public void addFrame(Ball ball, MovementFrame frame) {
        movementMap.get(ball).addLast(frame);
        immutableMap.get(ball).addLast(frame);
    }

    public Map<Ball, Deque<MovementFrame>> getMovementMap() {
        return movementMap;
    }

    public Map<Ball, Deque<MovementFrame>> getImmutableMap() {
        return immutableMap;
    }

    public Map<Ball, MovementFrame> getStartingPositions() {
        return startingPositions;
    }

    @Override
    public String toString() {
        return "Movement{balls=" + movementMap.size() + "," +
                "frames=" + new ArrayList<>(movementMap.values()).get(0).size() + "}";
    }
}
