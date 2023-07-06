package trashsoftware.trashSnooker.recorder;

import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.Game;
import trashsoftware.trashSnooker.core.career.championship.MetaMatchInfo;
import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.core.movement.MovementFrame;
import trashsoftware.trashSnooker.util.Util;

import java.io.IOException;
import java.util.List;

public class NaiveActualRecorder extends ActualRecorder {
    
    public static final int CUE_RECORD_LENGTH = 84;

    public NaiveActualRecorder(Game<?, ?> game, MetaMatchInfo metaMatchInfo) {
        super(game, metaMatchInfo);
    }

    @Override
    public void writeCue(CueRecord cueRecord,
                         Movement movement,
                         TargetRecord thisTarget,
                         TargetRecord nextTarget,
                         CueAnimationRec animationRec) throws IOException {
        writeCueRecord(cueRecord, thisTarget, nextTarget);
        writeMovement(movement);
        writeCueAnimation(animationRec);
    }

    @Override
    public void writeBallInHand() throws IOException {
        recordOneBallPos(game.getCueBall());
    }

    private void writeCueRecord(CueRecord cueRecord,
                                TargetRecord thisTarget, TargetRecord nextTarget) throws IOException {
        byte[] buf = new byte[CUE_RECORD_LENGTH];
        buf[0] = (byte) cueRecord.cuePlayer.getPlayerNumber();
        buf[1] = (byte) (cueRecord.isBreaking ? 1 : 0);
        buf[2] = (byte) thisTarget.playerNum;
        buf[3] = (byte) thisTarget.targetRep;
        buf[4] = (byte) (thisTarget.isSnookerFreeBall ? 1 : 0);
        buf[5] = (byte) nextTarget.playerNum;
        buf[6] = (byte) nextTarget.targetRep;
        buf[7] = (byte) (nextTarget.isSnookerFreeBall ? 1 : 0);

        Util.doubleToBytes(cueRecord.selectedPower, buf, 8);
        Util.doubleToBytes(cueRecord.actualPower, buf, 16);
        Util.doubleToBytes(cueRecord.aimUnitX, buf, 24);
        Util.doubleToBytes(cueRecord.aimUnitY, buf, 32);
        Util.doubleToBytes(cueRecord.intendedVerPoint, buf, 40);
        Util.doubleToBytes(cueRecord.intendedHorPoint, buf, 48);
        Util.doubleToBytes(cueRecord.actualVerPoint, buf, 56);
        Util.doubleToBytes(cueRecord.actualHorPoint, buf, 64);
        Util.doubleToBytes(cueRecord.cueAngle, buf, 72);

        buf[80] = (byte) cueRecord.playStage.ordinal();
        buf[81] = (byte) cueRecord.hand.ordinal();

        outputStream.write(buf);
    }
    
    private void writeCueAnimation(CueAnimationRec animationRec) throws IOException {
        totalBeforeCueMs += animationRec.getBeforeCueMs();
        
        // 暂时没啥能干的
    }

    private void writeMovement(Movement movement) throws IOException {
        int maxMovementSteps = 0;
        int steps = -1;
        for (Ball ball : game.getAllBalls()) {
            // 顺序很重要
            List<MovementFrame> frames = movement.getMovementMap().get(ball);
            if (frames.size() > maxMovementSteps) {
                maxMovementSteps = frames.size();
            }
            if (steps == -1) {
                steps = frames.size();  // 记录有多少帧
                byte[] stepsBytes = new byte[4];
                Util.int32ToBytes(steps, stepsBytes, 0);
                outputStream.write(stepsBytes);
            }
            if (frames.size() != steps)
                throw new RuntimeException();

            outputStream.write(ball.getValue());

            byte[] buf = new byte[58];
            for (MovementFrame frame : frames) {
                buf[0] = (byte) (frame.potted ? 1 : 0);
                buf[1] = (byte) frame.movementType;
                Util.doubleToBytes(frame.x, buf, 2);
                Util.doubleToBytes(frame.y, buf, 10);
                Util.doubleToBytes(frame.movementValue, buf, 18);
                Util.doubleToBytes(frame.xAxis, buf, 26);
                Util.doubleToBytes(frame.yAxis, buf, 34);
                Util.doubleToBytes(frame.zAxis, buf, 42);
                Util.doubleToBytes(frame.frameDegChange, buf, 50);
                outputStream.write(buf);
            }
        }
        nMovementFrames += maxMovementSteps;
    }

    @Override
    public void recordPositions() throws IOException {
        for (Ball b : game.getAllBalls()) {
            recordOneBallPos(b);
        }
    }

    private void recordOneBallPos(Ball b) throws IOException {
        outputStream.write(b.getValue());
        outputStream.write(b.isPotted() ? 1 : 0);
        byte[] buffer = new byte[8];
        Util.doubleToBytes(b.getX(), buffer, 0);
        outputStream.write(buffer);
        Util.doubleToBytes(b.getY(), buffer, 0);
        outputStream.write(buffer);
    }

    @Override
    protected byte recorderType() {
        return 0;
    }
}
