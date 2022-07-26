package trashsoftware.trashSnooker.recorder;

import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.EntireGame;
import trashsoftware.trashSnooker.core.Game;
import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.core.movement.MovementFrame;
import trashsoftware.trashSnooker.util.Util;

import java.io.IOException;
import java.util.List;

public class NaiveGameRecorder extends GameRecorder {

    public NaiveGameRecorder(Game game, EntireGame entireGame) {
        super(game, entireGame);
    }

    @Override
    protected void writeCue(CueRecord cueRecord, 
                            Movement movement,
                            TargetRecord thisTarget,
                            TargetRecord nextTarget) throws IOException {
        writeCueRecord(cueRecord, thisTarget, nextTarget);
        writeMovement(movement);
    }

    @Override
    protected void writeBallInHand() throws IOException {
        recordOneBallPos(game.getCueBall());
    }

    private void writeCueRecord(CueRecord cueRecord, 
                                TargetRecord thisTarget, TargetRecord nextTarget) throws IOException {
        byte[] buf = new byte[80];
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

        outputStream.write(buf);
    }
    
    private void writeMovement(Movement movement) throws IOException {
        int steps = -1;
        for (Ball ball : game.getAllBalls()) {
            // 顺序很重要
            List<MovementFrame> frames = movement.getMovementMap().get(ball);
            if (steps == -1) {
                steps = frames.size();  // 记录有多少帧
                byte[] stepsBytes = new byte[4];
                Util.int32ToBytes(steps, stepsBytes, 0);
                outputStream.write(stepsBytes);
            }
            if (frames.size() != steps) 
                throw new RuntimeException();

            outputStream.write(ball.getValue());

            byte[] buf = new byte[26];
            for (MovementFrame frame : frames) {
                buf[0] = (byte) (frame.potted ? 1 : 0);
                buf[1] = (byte) frame.movementType;
                Util.doubleToBytes(frame.x, buf, 2);
                Util.doubleToBytes(frame.y, buf, 10);
                Util.doubleToBytes(frame.movementValue,  buf,18);
                outputStream.write(buf);
            }
        }
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
