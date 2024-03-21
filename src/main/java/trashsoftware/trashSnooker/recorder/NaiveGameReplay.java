package trashsoftware.trashSnooker.recorder;

import trashsoftware.trashSnooker.core.*;
import trashsoftware.trashSnooker.core.cue.Cue;
import trashsoftware.trashSnooker.core.cue.CueBrand;
import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.core.movement.MovementFrame;
import trashsoftware.trashSnooker.core.numberedGames.PoolBall;
import trashsoftware.trashSnooker.core.snooker.SnookerBall;
import trashsoftware.trashSnooker.util.DataLoader;
import trashsoftware.trashSnooker.util.EventLogger;
import trashsoftware.trashSnooker.util.Util;

import java.io.IOException;

public class NaiveGameReplay extends GameReplay {
    protected NaiveGameReplay(BriefReplayItem item)
            throws IOException, VersionException {
        super(item);
    }

    private Ball loadOneBallPos(Ball b) throws IOException {
        byte[] buf = new byte[18];
        if (inputStream.read(buf) != buf.length) {
            throw new IOException();
        }
        int val = buf[0] & 0xff;
        boolean potted = buf[1] == 1;
        double x = Util.bytesToDouble(buf, 2);
        double y = Util.bytesToDouble(buf, 10);

        if (b == null) {
            switch (gameValues.rule) {
                case CHINESE_EIGHT, LIS_EIGHT, AMERICAN_NINE -> {
                    b = new PoolBall(val, potted, gameValues);
                    b.setX(x);
                    b.setY(y);
                }
                case SNOOKER, MINI_SNOOKER, SNOOKER_TEN -> {
                    b = new SnookerBall(val, new double[]{x, y}, gameValues);
                    b.setPotted(potted);
                }
                default -> throw new RuntimeException("No such ball");
            }

        } else {
            b.setX(x);
            b.setY(y);
            b.setPotted(potted);
        }
        return b;
    }

    @Override
    protected void loadBallPositions() throws IOException {
        if (balls == null) balls = new Ball[gameValues.nBalls()];

        for (int i = 0; i < balls.length; i++) {
            Ball b = loadOneBallPos(balls[i]);
            if (balls[i] == null) {
                balls[i] = b;
                valueBallMap.put(b.getValue(), b);
                if (b.isWhite()) cueBall = b;
            }
        }
    }

    private void loadCueAnimation() throws IOException {
        byte[] buf = new byte[NaiveActualRecorder.CUE_ANIMATION_BUF_LEN];
        if (inputStream.read(buf) != buf.length) throw new IOException();
        
        String cueInsId = new String(buf, 8, NaiveActualRecorder.CUE_ANIMATION_BUF_LEN - 8);
        cueInsId = cueInsId.replace("\0", "");
        String[] spl = cueInsId.split(Cue.BRAND_SEPARATOR);
        String cueBrandId;
        if (spl.length > 0) {
            cueBrandId = spl[0];
        } else {
            cueBrandId = "";
        }
        
        CueBrand cueBrand = DataLoader.getInstance().getCueById(cueBrandId);
        if (cueBrand == null) {
            EventLogger.warning("Replay cue is null: " + cueBrandId);
            cueBrand = DataLoader.getInstance().getStdBreakCueBrand();  // 随便选一个让它不是null
        }
        InGamePlayer cuing = getCuingIgp();
        cuing.getCueSelection().selectByBrand(cueBrand);
        
        animationRec = new CueAnimationRec(
                cuing.getCueSelection().getSelected().getNonNullInstance(CueSelection.InstanceType.TEMP_VIEW),
                Util.bytesToInt32(buf, 0),
                Util.bytesToInt32(buf, 4)
        );
    }

    @Override
    protected void loadNextRecordAndMovement() throws IOException {
        readCueRecordAndTargets();
        loadCueAnimation();  // must after readCueRecordAndTargets()
        currentMovement = getNextMovement();
    }

    @Override
    protected void loadBallInHand() {
        try {
            loadOneBallPos(cueBall);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Movement getNextMovement() throws IOException {
        byte[] stepsBuf = new byte[4];
        if (inputStream.read(stepsBuf) != stepsBuf.length) throw new IOException();

        int steps = Util.bytesToInt32(stepsBuf, 0);

        byte[] posBuf = new byte[58];
        byte[] ballValueBuf = new byte[1];

        Movement movement = new Movement(balls);
        for (int ballIndex = 0; ballIndex < gameValues.nBalls(); ballIndex++) {
            if (inputStream.read(ballValueBuf) != ballValueBuf.length) {
                throw new IOException();
            }
            int expectedBall = ballValueBuf[0] & 0xff;
            Ball ball = balls[ballIndex];
            if (ball.getValue() != expectedBall) {
                throw new RuntimeException(String.format("Expected %d, got %d\n",
                        expectedBall, ball.getValue()));
            }
            for (int s = 0; s < steps; s++) {
                if (inputStream.read(posBuf) != posBuf.length) throw new IOException();
                boolean potted = posBuf[0] == 1;
                int movementType = posBuf[1] & 0xff;
                double x = Util.bytesToDouble(posBuf, 2);
                double y = Util.bytesToDouble(posBuf, 10);
                double movementValue = Util.bytesToDouble(posBuf, 18);
                double axisX = Util.bytesToDouble(posBuf, 26);
                double axisY = Util.bytesToDouble(posBuf, 34);
                double axisZ = Util.bytesToDouble(posBuf, 42);
                double rotateDeg = Util.bytesToDouble(posBuf, 50);

                MovementFrame frame = new MovementFrame(x, y,
                        axisX, axisY, axisZ, rotateDeg, potted, movementType, movementValue);
                movement.addFrame(ball, frame);
            }
        }

        if (steps >= Game.CONGESTION_LIMIT) {
            movement.setCongested();
        }
        movement.setupReplay();

        return movement;
    }

    private void readCueRecordAndTargets() throws IOException {
        byte[] buf = new byte[NaiveActualRecorder.CUE_RECORD_LENGTH];
        if (inputStream.read(buf) != buf.length) throw new IOException();

        currentCueRecord = new CueRecord(
                buf[0] == 1 ? p1 : p2,
                buf[1] == 1,
                Util.bytesToDouble(buf, 8),
                Util.bytesToDouble(buf, 16),
                Util.bytesToDouble(buf, 24),
                Util.bytesToDouble(buf, 32),
                Util.bytesToDouble(buf, 40),
                Util.bytesToDouble(buf, 48),
                Util.bytesToDouble(buf, 56),
                Util.bytesToDouble(buf, 64),
                Util.bytesToDouble(buf, 72),
                GamePlayStage.values()[buf[80] & 0xff],
                PlayerPerson.Hand.values()[buf[81] & 0xff]
        );

        thisTarget = new TargetRecord(
                buf[2] & 0xff,
                buf[3] & 0xff,
                buf[4] == 1
        );
        nextTarget = new TargetRecord(
                buf[5] & 0xff,
                buf[6] & 0xff,
                buf[7] == 1
        );
    }
}
