package trashsoftware.trashSnooker.core.career;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class DataGenerator {

    public static void main(String[] args) {
        generateExpPerLevel();
    }
    
    public static void generateExpPerLevel() {
        double exp = 100;
        double ratio = 1.08;
        
        StringBuilder builder = new StringBuilder();
        int limit = 80;
        for (int i = 1; i < limit; i++) {
            builder.append(i)
                    .append(", ")
                    .append((int) Math.round(exp))
                    .append("\n");
            exp *= ratio;
        }
        
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("data/level.dat"))) {
            bw.write(builder.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }
}
