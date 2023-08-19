package trashsoftware.trashSnooker.core.metrics;

import trashsoftware.trashSnooker.core.Algebra;

import java.util.Arrays;

public abstract class Cushion {
    
    public static abstract class LinearCushion extends Cushion {
        protected final double[][] position;
        protected final double[] vector;
        protected final double[] normal;
        
        LinearCushion(double[][] position) {
            this.position = position;
            //todo: check
            this.vector = Algebra.unitVector(position[1][0] - position[0][0], position[1][1]- position[0][1]);
            this.normal = Algebra.normalVector(vector);
        }

        public double[][] getPosition() {
            return position;
        }

        public double[] getVector() {
            return vector;
        }

        public double[] getNormal() {
            return normal;
        }
    }
    
    public static class EdgeCushion extends LinearCushion {

        private final String des;
        
        public EdgeCushion(String des, double[][] vec) {
            super(vec);
            
            this.des = des;
        }

        public String getDes() {
            return des;
        }
        
        public boolean isEndCushion() {
            return vector[0] == 0.0;
        }

        @Override
        public String toString() {
            return "Cushion " + des;
        }
    }
    
    public static class CushionArc extends Cushion {
        private final double[] center;
        
        public CushionArc(double[] center) {
            this.center = center;
        }

        public double[] getCenter() {
            return center;
        }

        @Override
        public String toString() {
            return "Pocket arc @" + Arrays.toString(center);
        }
    }
    
    public static class CushionLine extends LinearCushion {

        public CushionLine(double[][] pos) {
            super(pos);
        }

        @Override
        public String toString() {
            return "Pocket Line @" + Arrays.deepToString(position);
        }
    }
}
