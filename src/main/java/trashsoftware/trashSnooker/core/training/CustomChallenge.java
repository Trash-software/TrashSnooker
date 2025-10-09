package trashsoftware.trashSnooker.core.training;

import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.Algebra;
import trashsoftware.trashSnooker.core.metrics.GameRule;
import trashsoftware.trashSnooker.core.metrics.GameValues;
import trashsoftware.trashSnooker.core.metrics.TableMetrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

public class CustomChallenge extends Challenge {
    
    private final List<BallSchema> ballSchemas = new ArrayList<>();
    protected SingleBallRepeater singleBallRepeater;
    
    private CustomChallenge(GameRule rule, TrainType trainType) {
        super(rule, trainType);
    }
    
    public static CustomChallenge fromJson(GameRule rule, JSONObject schema, TrainType trainType) {
        CustomChallenge customChallenge = new CustomChallenge(rule, trainType);
        JSONArray balls = schema.getJSONArray("balls");
        for (int i = 0; i < balls.length(); i++) {
            JSONObject ballObj = balls.getJSONObject(i);
            BallSchema ballSchema;
            if (ballObj.optBoolean("random", false)) {
                ballSchema = new RandomBallSchema(
                        ballObj.getInt("value"),
                        ballObj.getDouble("xLow"),
                        ballObj.getDouble("xHigh"),
                        ballObj.getDouble("yLow"),
                        ballObj.getDouble("yHigh"),
                        Reference.valueOf(ballObj.getString("ref").toUpperCase(Locale.ROOT))
                );
            } else {
                ballSchema = new FixedBallSchema(
                        ballObj.getInt("value"),
                        ballObj.getDouble("x"),
                        ballObj.getDouble("y"),
                        Reference.valueOf(ballObj.getString("ref").toUpperCase(Locale.ROOT))
                );
            }
            customChallenge.ballSchemas.add(ballSchema);
        }
        
        return customChallenge;
    }

    public void setSingleBallRepeater(SingleBallRepeater singleBallRepeater) {
        this.singleBallRepeater = singleBallRepeater;
    }

    public List<BallSchema> getBallSchemas() {
        return ballSchemas;
    }

    public SingleBallRepeater getSingleBallRepeater() {
        return singleBallRepeater;
    }
    
    public boolean isSingleBallRepeat() {
        return singleBallRepeater != null;
    }

    public abstract static class BallSchema {
        public final int value;
        public final Reference reference;
        
        BallSchema(int value, Reference reference) {
            this.value = value;
            this.reference = reference;
        }

        public abstract double[] getLocation(GameValues values);
        
        protected double[] getLocation(GameValues values, double x, double y) {
            if (reference == Reference.ABSOLUTE) return new double[]{x, y};
            TableMetrics metrics = values.table;

            if (reference == Reference.UNIT_TRUE) {
                double leftX = metrics.leftX;
                double rightX = metrics.rightX;
                double topY = metrics.topY;
                double botY = metrics.botY;
                return new double[]{
                        Algebra.shiftRange(0, 1, leftX, rightX, x),
                        Algebra.shiftRange(0, 1, topY, botY, y)
                };
            } else if (reference == Reference.UNIT) {
                double ballR = values.ball.ballRadius;
                double leftX = metrics.leftX + ballR;  // 我们不希望把球放库上面了
                double rightX = metrics.rightX - ballR;
                double topY = metrics.topY + ballR;
                double botY = metrics.botY - ballR;
                return new double[]{
                        Algebra.shiftRange(0, 1, leftX, rightX, x),
                        Algebra.shiftRange(0, 1, topY, botY, y)
                };
            } else {
                throw new IllegalArgumentException("No such reference: " + reference);
            }
        }
    }

    public static class FixedBallSchema extends BallSchema {
        
        protected final double x;
        protected final double y;

        FixedBallSchema(int value, double x, double y, Reference reference) {
            super(value, reference);
            this.x = x;
            this.y = y;
        }
        
        @Override
        public double[] getLocation(GameValues values) {
            return getLocation(values, x, y);
        }
    }
    
    public static class RandomBallSchema extends BallSchema {
        
        final double xLow;
        final double xHigh;
        final double yLow;
        final double yHigh;
        
        Random random = new Random();

        RandomBallSchema(int value, double xLow, double xHigh, double yLow, double yHigh, Reference reference) {
            super(value, reference);
            
            this.xLow = xLow;
            this.xHigh = xHigh;
            this.yLow = yLow;
            this.yHigh = yHigh;
        }

        @Override
        public double[] getLocation(GameValues values) {
            double x = xLow == xHigh ? xLow : random.nextDouble(xLow, xHigh);
            double y = yLow == yHigh ? yLow : random.nextDouble(yLow, yHigh);
            return getLocation(values, x, y);
        }
    }
    
    public enum Reference {
        UNIT,
        UNIT_TRUE,
        ABSOLUTE
    }
}
