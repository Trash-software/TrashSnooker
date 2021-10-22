package trashsoftware.trashSnooker.core.movement;

import trashsoftware.trashSnooker.core.Ball;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

public class Movement {

    private final Map<Ball, Deque<MovementFrame>> movementMap = new HashMap<>();
    private final Map<Ball, MovementFrame> startingPositions = new HashMap<>();

    public Movement(Ball[] allBalls) {
        for (Ball ball : allBalls) {
            ArrayDeque<MovementFrame> positionList = new ArrayDeque<>();
            MovementFrame frame = new MovementFrame(ball.getX(), ball.getY(), ball.isPotted());
            positionList.addFirst(frame);
            movementMap.put(ball, positionList);
            startingPositions.put(ball, frame);
        }
    }

    public Map<Ball, Deque<MovementFrame>> getMovementMap() {
        return movementMap;
    }

    public Map<Ball, MovementFrame> getStartingPositions() {
        return startingPositions;
    }
}
