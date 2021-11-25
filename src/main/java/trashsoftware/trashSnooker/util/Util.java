package trashsoftware.trashSnooker.util;

import java.sql.Timestamp;
import java.util.Random;

public class Util {

    public static <T> boolean arrayContains(T[] array, T value) {
        for (T ele : array) if (ele == value) return true;
        return false;
    }

    public static boolean arrayContains(int[] array, int value) {
        for (int ele : array) if (ele == value) return true;
        return false;
    }
    
    public static String timeStampFmt(Timestamp timestamp) {
        String str = timestamp.toString();
        int msDotIndex = str.lastIndexOf('.');
        String noMs = str.substring(0, msDotIndex);
        String ms = str.substring(msDotIndex + 1);
        while (ms.startsWith("0")) {
            ms = ms.substring(1);
        }
        return "'" + noMs + "." + ms + "'";
    }
    
    public static String secondsToString(int sec) {
        if (sec < 3600) {
            return String.format("%02d:%02d", sec / 60, sec % 60);
        } else {
            return String.format("%d:%s", sec / 3600, secondsToString(sec % 3600));
        }
    }
    
    private static <T> void swap(T[] array, int i, int j) {
        T temp = array[i];
        array[i] = array[j];
        array[j] = temp;
    }
    
    public static <T> void shuffleArray(T[] array) {
        Random random = new Random();
        for (int i = array.length; i > 0; i--) {
            int index = random.nextInt(i);
            swap(array, index, i - 1);
        }
    }
    
    public static <T> void reverseArray(T[] array) {
        int mid = array.length / 2;
        for (int i = 0; i < mid; i++) {
            T temp = array[i];
            array[i] = array[array.length - i - 1];
            array[array.length - i - 1] = temp;
        }
    }
}
