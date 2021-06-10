package trashsoftware.trashSnooker.util;

public class Util {

    public static <T> boolean arrayContains(T[] array, T value) {
        for (T ele : array) if (ele == value) return true;
        return false;
    }

    public static boolean arrayContains(int[] array, int value) {
        for (int ele : array) if (ele == value) return true;
        return false;
    }
}
