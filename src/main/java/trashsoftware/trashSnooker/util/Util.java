package trashsoftware.trashSnooker.util;

import java.sql.Timestamp;

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
            return String.format("%d:%d", sec / 60, sec % 60);
        } else {
            return String.format("%d:%s", sec / 3600, secondsToString(sec % 3600));
        }
    }
}
