package trashsoftware.trashSnooker.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

public class PinyinDict {

    private static final Map<Character, char[]> TONE_DICT_A = Map.of(
            'ā', new char[]{'a', 1},
            'á', new char[]{'a', 2},
            'ǎ', new char[]{'a', 3},
            'à', new char[]{'a', 4}
    );
    private static final Map<Character, char[]> TONE_DICT_O = Map.of(
            'ō', new char[]{'o', 1},
            'ó', new char[]{'o', 2},
            'ǒ', new char[]{'o', 3},
            'ò', new char[]{'o', 4}
    );
    private static final Map<Character, char[]> TONE_DICT_E = Map.of(
            'ē', new char[]{'e', 1},
            'é', new char[]{'e', 2},
            'ě', new char[]{'e', 3},
            'è', new char[]{'e', 4}
    );
    private static final Map<Character, char[]> TONE_DICT_I = Map.of(
            'ī', new char[]{'i', 1},
            'í', new char[]{'i', 2},
            'ǐ', new char[]{'i', 3},
            'ì', new char[]{'i', 4}
    );
    private static final Map<Character, char[]> TONE_DICT_U = Map.of(
            'ū', new char[]{'u', 1},
            'ú', new char[]{'u', 2},
            'ǔ', new char[]{'u', 3},
            'ù', new char[]{'u', 4}
    );
    private static final Map<Character, char[]> TONE_DICT_V = Map.of(
            'ü', new char[]{'v', 0},
            'ǖ', new char[]{'v', 1},
            'ǘ', new char[]{'v', 2},
            'ǚ', new char[]{'v', 3},
            'ǜ', new char[]{'v', 4}
    );

    public static final Map<Character, char[]> TONE_DICT = Util.mergeMaps(
            TONE_DICT_A, TONE_DICT_O, TONE_DICT_E, TONE_DICT_I, TONE_DICT_U, TONE_DICT_V
    );
    
    private static PinyinDict instance;
    
    protected Map<Character, String> pinyin;

    protected PinyinDict() {
        pinyin = getChsPinyinDict();

//        makeRevPinyinDict();
//
//        int min = 65536;
//        int max = 0;
//        for (char c : pinyin.keySet()) {
//            if (c < min) min = c;
//            if (c > max) max = c;
//        }
//        System.out.println(pinyin.size() + " " + min + " " + max);
    }

    public static Map<Character, String> getChsPinyinDict() {
        try (BufferedReader pinBr = new BufferedReader(
                new InputStreamReader(Objects.requireNonNull(
                        PinyinDict.class.getResourceAsStream("pinyin.txt"))))) {
            Map<Character, String> result = new TreeMap<>();
            String line;
            while ((line = pinBr.readLine()) != null) {
                String[] split = line.split(",");
                for (String s : split) {
                    s = s.strip();
                    if (s.length() >= 2) {
                        char chs = s.charAt(0);
                        String pinyin = s.substring(1);
//                        int tone = 0;
                        StringBuilder builder = new StringBuilder();
                        for (char c : pinyin.toCharArray()) {
                            char[] replace = TONE_DICT.get(c);
                            if (replace == null) {
                                builder.append(c);
                            } else {
                                builder.append(replace[0]);
//                                tone = replace[1];
                            }
                        }
//                        String py = tone == 0 ?
//                                builder.toString() : builder.append(tone).toString();
                        result.put(chs, builder.toString());
                    }
                }
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static PinyinDict getInstance() {
        if (instance == null) {
            instance = new PinyinDict();
        }
        return instance;
    }

    public String getPinyinByChs(char ch) {
        return pinyin.get(ch);
    }
    
    public boolean needTranslate(String s) {
        for (char c : s.toCharArray()) {
            if (c <= 127) continue;  // ASCII的，直接跳过
            if (pinyin.containsKey(c)) return true;
        }
        return false;
    }

    public String translateChineseName(String chineseName) {
        return translateChineseName(chineseName, NameConvention.LAST_FULL_INIT);
    }
    
    public String translateChineseName(String chineseName, NameConvention nameConvention) {
        String last = null;
        List<String> first = new ArrayList<>();
        StringBuilder unTrans = new StringBuilder();
        for (int i = 0; i < chineseName.length(); i++) {
            char c = chineseName.charAt(i);
            String py = getPinyinByChs(c);
            
            if (py == null) {
                unTrans.append(c);
            } else {
                if (unTrans.length() > 0) {
                    if (last == null) last = unTrans.toString();  // 这里的i已经不是0了
                    else first.add(unTrans.toString());
                    unTrans.setLength(0);
                }
                if (i == 0) last = py;
                else first.add(py);
            }
        }
        
        return nameConvention.format(last, first);
    }
    
    private static String firstUpper(String word) {
        char first = Character.toUpperCase(word.charAt(0));
        return first + word.substring(1);
    }
    
    public enum NameConvention {
        FIRST_LAST {
            @Override
            String format(String last, List<String> first) {
                return firstUpper(String.join("", first)) + " " + firstUpper(last);
            }
        },
        LAST_FIRST_INIT {
            @Override
            String format(String last, List<String> first) {
                if (first.isEmpty()) return firstUpper(last);
                return firstUpper(last) + " " + Character.toUpperCase(first.get(0).charAt(0)) + ".";
            }
        },
        LAST_FULL_INIT {
            @Override
            String format(String last, List<String> first) {
                if (first.isEmpty()) return firstUpper(last);
                StringBuilder builder = new StringBuilder();
                builder.append(firstUpper(last))
                        .append(' ');
                for (String f : first) {
                    builder.append(Character.toUpperCase(f.charAt(0))).append('.');
                }
                return builder.toString();
            }
        },
        LAST {
            @Override
            String format(String last, List<String> first) {
                return firstUpper(last);
            }
        },
        LAST_FIRST {
            @Override
            String format(String last, List<String> first) {
                return firstUpper(last) + " " + firstUpper(String.join("", first));
            }
        };
        
        abstract String format(String last, List<String> first);
    }
}
