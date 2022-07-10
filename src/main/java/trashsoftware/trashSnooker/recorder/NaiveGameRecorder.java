package trashsoftware.trashSnooker.recorder;

import trashsoftware.trashSnooker.core.Ball;
import trashsoftware.trashSnooker.core.EntireGame;
import trashsoftware.trashSnooker.core.Game;
import trashsoftware.trashSnooker.core.movement.Movement;
import trashsoftware.trashSnooker.core.movement.MovementFrame;
import trashsoftware.trashSnooker.core.scoreResult.ScoreResult;
import trashsoftware.trashSnooker.util.Util;

import java.io.IOException;
import java.util.Deque;

public class NaiveGameRecorder extends GameRecorder {

    public NaiveGameRecorder(Game game, EntireGame entireGame) {
        super(game, entireGame);
    }

    @Override
    protected void writeCue(CueRecord cueRecord, Movement movement) throws IOException {
        writeCueRecord(cueRecord);
        writeMovement(movement);
    }

    @Override
    protected void writeBallInHand() throws IOException {
        recordOneBallPos(game.getCueBall());
    }

    private void writeCueRecord(CueRecord cueRecord) throws IOException {
        byte[] buf = new byte[80];
        buf[0] = (byte) cueRecord.cuePlayer.getPlayerNumber();
        buf[1] = (byte) (cueRecord.isBreaking ? 1 : 0);
        buf[2] = (byte) cueRecord.targetRep;
        buf[3] = (byte) (cueRecord.isSnookerFreeBall ? 1 : 0); 

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
            Deque<MovementFrame> frames = movement.getImmutableMap().get(ball);
            if (steps == -1) {
                steps = frames.size();  // 记录有多少帧
                byte[] stepsBytes = new byte[4];
                Util.int32ToBytes(steps, stepsBytes, 0);
                outputStream.write(stepsBytes);
            }
            if (frames.size() != steps) 
                throw new RuntimeException();

            outputStream.write(ball.getValue());

            byte[] buf = new byte[17];
            for (MovementFrame frame : frames) {
                buf[0] = (byte) (frame.potted ? 1 : 0);
                Util.doubleToBytes(frame.x, buf, 1);
                Util.doubleToBytes(frame.y, buf, 9);
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
