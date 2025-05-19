package trashsoftware.trashSnooker.util;

import org.json.JSONArray;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.Values;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;

public class Util {

    public static final DateFormat TIME_FORMAT_SEC =
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static final DateFormat SHOWING_DATE_FORMAT = 
            new SimpleDateFormat("yyyy/MM/dd HH:mm");

    public static <T> boolean arrayContains(T[] array, T value) {
        for (T ele : array) if (ele == value) return true;
        return false;
    }

    public static <T> boolean arrayContainsEqual(T[] array, T value) {
        for (T ele : array) if (Objects.equals(ele, value)) return true;
        return false;
    }

    public static boolean arrayContains(int[] array, int value) {
        for (int ele : array) if (ele == value) return true;
        return false;
    }
    
    public static <T> T findInArray(T[] array, Function<T, Boolean> predicate) {
        for (T obj : array) {
            if (predicate.apply(obj)) return obj;
        }
        return null;
    }
    
    public static <T> void randomShrinkTo(NavigableSet<T> set, int n) {
        int currentSize = set.size();
        if (currentSize <= n) return;
        
        List<T> keys = new ArrayList<>(set);
        Collections.shuffle(keys);

        for (int i = 0; i < currentSize - n; i++) {
            set.remove(keys.get(i));
        }
    }
    
    public static String entireBeginTimeNoQuote(Timestamp timestamp) {
        String str = timestamp.toString();
        int msDotIndex = str.lastIndexOf('.');
        String noMs = str.substring(0, msDotIndex);
        String ms = str.substring(msDotIndex + 1);
        while (ms.startsWith("0")) {
            ms = ms.substring(1);
        }
        return noMs + "." + ms;
    }
    
    public static String entireBeginTimeToFileName(Timestamp timestamp) {
        String ebt = entireBeginTimeNoQuote(timestamp);
        return ebt.replace(':', '_');
    }
    
    public static String fromSqlFmtToEbtFileName(String sql) {
        String noQuote = sql.substring(1, sql.length() - 1);
        return noQuote.replace(':', '_');
    }

    public static String timeStampFmt(Timestamp timestamp) {
        return "'" + entireBeginTimeNoQuote(timestamp) + "'";
    }

    public static String secondsToString(int sec) {
        if (sec < 3600) {
            return String.format("%02d:%02d", sec / 60, sec % 60);
        } else {
            return String.format("%d:%s", sec / 3600, secondsToString(sec % 3600));
        }
    }
    
    public static String decodeStringFromArr(byte[] arr, int startIndex, int maxLen) {
        int stop = startIndex;
        int mustStop = startIndex + maxLen;
        while (stop < mustStop) {
            if (arr[stop] == 0) break;
            stop++;
        }
        return new String(arr, startIndex, stop - startIndex);
    }

    public static boolean deleteFile(File file) {
        if (file == null || !file.exists()) return true;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children == null) return true;
            boolean suc = true;
            for (File c : children) {
                if (!deleteFile(c)) suc = false;
            }
            return file.delete() && suc;
        } else {
            return file.delete();
        }
    }
    
    public static String byteArrayToHex(byte[] b) {
        StringBuilder builder = new StringBuilder();
        for (byte b1 : b) {
            builder.append(decimalToHex(b1 & 0xff, 2));
        }
        return builder.toString();
    }
    
    public static String generateHex(int digits) {
        Random random = new Random();
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < digits; i++) {
            int rnd = random.nextInt(16);
            builder.append(decimalToHex(rnd));
        }
        return builder.toString();
    }
    
    private static char decimalToHex(int dec) {
        if (dec < 10) return (char) ('0' + dec);
        else return (char) ('A' + (dec - 10));
    }

    public static String decimalToHex(int dec, int digits) {
        int max = (1 << (digits << 2));
        if (dec >= max) throw new ArithmeticException("Too big to convert to hex");

        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < digits; i++) {
            int d = dec & 15;
            char c;
            if (d < 10) c = (char) ('0' + d);
            else c = (char) ('A' + (d - 10));
            builder.append(c);
            dec >>= 4;
        }
        builder.reverse();
        return builder.toString();
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

    @SafeVarargs
    public static <K, V> Map<K, V> mergeMaps(Map<K, V>... maps) {
        Map<K, V> res = new HashMap<>();
        for (Map<K, V> map : maps) {
            res.putAll(map);
        }
        return res;
    }

    public static void intToBytesN(long num, byte[] arr, int index, int nBytes) {
        for (int i = 0; i < nBytes; i++)
            arr[index + i] =
                    (byte) ((num >>> ((nBytes - 1 - i) << 3)) & 0xff);
    }

    public static long bytesToIntN(byte[] arr, int index, int nBytes) {
        long result = 0;
        for (int i = 0; i < nBytes; i++)
            result =
                    (arr[index + i] & 0xffL) << ((nBytes - 1 - i) << 3) | result;
        return result;
    }

    public static void int32ToBytes(int n, byte[] arr, int index) {
        intToBytesN(n, arr, index, 4);
    }

    public static int bytesToInt32(byte[] arr, int index) {
        return (int) bytesToIntN(arr, index, 4);
    }

    /**
     * Convert a long into an 8-byte array in big-endian.
     *
     * @param l the long.
     */
    public static void longToBytes(long l, byte[] arr, int index) {
        intToBytesN(l, arr, index, 8);
//        for (int i = 0; i < 8; i++) arr[index + i] = (byte) ((l >> ((7 - i) << 3)) & 0xff);
    }

    /**
     * Convert a 8-byte array into signed long in big-endian.
     *
     * @param b byte array.
     * @return signed long.
     */
    public static long bytesToLong(byte[] b, int index) {
        return bytesToIntN(b, index, 8);
//        long result = 0;
//        for (int i = 0; i < 8; i++) result = (b[index + i] & 0xffL) << ((7 - i) << 3) | result;
//        return result;
    }

    public static void doubleToBytes(double d, byte[] arr, int index) {
        long bits = Double.doubleToLongBits(d);
        longToBytes(bits, arr, index);
    }

    public static double bytesToDouble(byte[] b, int index) {
        return Double.longBitsToDouble(bytesToLong(b, index));
    }

    public static void floatToBytes(float d, byte[] arr, int index) {
        int bits = Float.floatToIntBits(d);
        int32ToBytes(bits, arr, index);
    }

    public static float bytesToFloat(byte[] b, int index) {
        return Float.intBitsToFloat(bytesToInt32(b, index));
    }

    public static int indexOf(char c, char[] arr) {
        for (int i = 0; i < arr.length; i++) if (c == arr[i]) return i;
        return -1;
    }

    public static int indexOf(byte c, byte[] arr) {
        for (int i = 0; i < arr.length; i++) if (c == arr[i]) return i;
        return -1;
    }

    public static <T> int indexOf(T c, T[] arr) {
        for (int i = 0; i < arr.length; i++) if (Objects.equals(c, arr[i])) return i;
        return -1;
    }

    public static String timeToReadable(long ms) {
        long s = Math.round(ms / 1000.0);
        if (s < 60) {
            return String.format("0:%02d", s);
        } else if (s < 3600) {
            return String.format("%d:%02d", s / 60, s % 60);
        } else {
            long h = s / 3600;
            long mm_ss = s % 3600;
            return String.format("%d:%02d:%02d", h, mm_ss / 60, mm_ss % 60);
        }
    }

    public static double powerMultiplierOfCuePoint(double unitX, double unitY) {
        double dt = Math.hypot(Math.abs(unitX), Math.abs(unitY));  // 0-1之间
        double gap = 1 - Values.CUE_POINT_MULTIPLIER;
        return (1 - dt) * gap + Values.CUE_POINT_MULTIPLIER;
    }

    public static JSONObject readJson(File file) {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                builder.append(line);
            }
            return new JSONObject(builder.toString());
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void writeJson(JSONObject jsonObject, File file) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(jsonObject.toString(2));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    public static boolean isAllCap(String s) {
        for (char c : s.toCharArray()) {
            if (!Character.isUpperCase(c)) return false;
        }
        return true;
    }
    
    private static String[] separateWords(String s) {
        String[] words;

        if (s.contains("_")) {
            words = s.split("_");
        } else if (s.contains(" ")) {
            words = s.split(" ");
        } else if (isAllCap(s)) {
            words = new String[]{s};
        } else {
            // probably upper camel, or a single word
            List<String> strings = new ArrayList<>();
            StringBuilder wordBuilder = new StringBuilder();
            for (char c : s.toCharArray()) {
                if (Character.isUpperCase(c)) {
                    if (wordBuilder.length() > 0) {
                        strings.add(wordBuilder.toString());
                        wordBuilder.setLength(0);
                    }
                } else if (Character.isDigit(c)) {
                    if (wordBuilder.length() > 0) {
                        if (!Character.isDigit(wordBuilder.charAt(0))) {
                            strings.add(wordBuilder.toString());
                            wordBuilder.setLength(0);
                        }
                    }
                } else {
                    // 是小写字母
                    if (wordBuilder.length() > 0) {
                        if (Character.isDigit(wordBuilder.charAt(0))) {
                            strings.add(wordBuilder.toString());
                            wordBuilder.setLength(0);
                        }
                    }
                }
                wordBuilder.append(c);
            }
            if (wordBuilder.length() > 0) {
                strings.add(wordBuilder.toString());
            }
            words = strings.toArray(new String[0]);
        }
        return words;
    }
    
    public static String toAllCapsUnderscoreCase(String s) {
        String[] words = separateWords(s);

        String[] upper = new String[words.length];
        for (int i = 0 ; i < words.length; i++) {
            upper[i] = words[i].toUpperCase();
        }
        
        return String.join("_", upper);
    }

    public static String toLowerCamelCase(String s) {
        String[] words = separateWords(s);

        StringBuilder builder = new StringBuilder()
                .append(words[0].toLowerCase(Locale.ROOT));
        for (int i = 1; i < words.length; i++) {
            char first = Character.toUpperCase(words[i].charAt(0));
            String rest = words[i].substring(1);
            builder.append(first)
                    .append(rest.toLowerCase(Locale.ROOT));
        }
        return builder.toString();
    }

    /**
     * 高级点的替换，可以把"{xxx}"替换为map内的key"xxx"对应的值
     */
    public static String formatSentence(String sen, Map<String, String> replacements) {
        for (Map.Entry<String, String> rep : replacements.entrySet()) {
            sen = sen.replace('{' + rep.getKey() + '}', rep.getValue());
        }
        return sen;
    }
    
    public static String moneyToReadable(int money) {
        return String.format("%,d", money);
    }

    public static String moneyToReadable(int money, boolean forceSign) {
        String s = String.format("%,d", money);
        if (money > 0) s = "+" + s;
        return s;
    }
    
    public static JSONArray arrayToJson(double[] array) {
        JSONArray json = new JSONArray();
        for (double d : array) {
            json.put(d);
        }
        return json;
    }

    public static JSONArray arrayToJson(int[] array) {
        JSONArray json = new JSONArray();
        for (int d : array) {
            json.put(d);
        }
        return json;
    }
    
    public static double[] jsonToDoubleArray(JSONArray jsonArray) {
        double[] result = new double[jsonArray.length()];
        for (int i = 0; i < result.length; i++) {
            result[i] = jsonArray.getDouble(i);
        }
        return result;
    }

    public static int[] jsonToIntArray(JSONArray jsonArray) {
        int[] result = new int[jsonArray.length()];
        for (int i = 0; i < result.length; i++) {
            result[i] = jsonArray.getInt(i);
        }
        return result;
    }

    public static JSONObject stringMapToJson(Map<?, String> map) {
        JSONObject json = new JSONObject();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            json.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return json;
    }
    
    public static JSONObject mapToJson(Map<?, ? extends Number> map) {
        JSONObject json = new JSONObject();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            json.put(String.valueOf(entry.getKey()), entry.getValue());
        }
        return json;
    }

    public static long readInt4Little(InputStream stream) throws IOException {
        byte[] buf = stream.readNBytes(4);
        return (buf[0] & 0xffL) | ((buf[1] & 0xffL) << 8) | ((buf[2] & 0xffL) << 16) | ((buf[3] & 0xffL) << 24);
    }

    public static void writeInt4Little(OutputStream stream, long value) throws IOException {
        stream.write(new byte[]{(byte) value, (byte) (value >> 8), (byte) (value >> 16), (byte) (value >> 24)});
    }

    public static int readInt2Little(InputStream stream) throws IOException {
        byte[] buf = stream.readNBytes(2);
        return (buf[0] & 0xff) | ((buf[1] & 0xff) << 8);
    }

    public static void writeInt2Little(OutputStream stream, int value) throws IOException {
        stream.write(new byte[]{(byte) value, (byte) (value >> 8)});
    }

    public static String readString(InputStream stream, int len) throws IOException {
        return new String(stream.readNBytes(len));
    }

    public static void writeString(OutputStream stream, String s) throws IOException {
        stream.write(s.getBytes(StandardCharsets.UTF_8));
    }

    public static class IntList extends ArrayList<Integer> {

    }
}
