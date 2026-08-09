package trashsoftware.trashSnooker.util;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import trashsoftware.trashSnooker.core.metrics.TableMetrics;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.*;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class JsonUtil {
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
    
    public static Map<String, Integer> jsonToIntMap(JSONObject json) {
        Map<String, Integer> map = new TreeMap<>();
        for (String key : json.keySet()) {
            map.put(key, json.getInt(key));
        }
        return map;
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

    @Retention(RetentionPolicy.RUNTIME) // This is crucial
    @Target({ElementType.FIELD, ElementType.RECORD_COMPONENT}) // Targets fields specifically
    public @interface Optional {
        String value() default "";
    }
    
    private static Object recordValueToJsonValue(Class<?> type, Object value) {
        if (isSupportedType(type)) {
            return value;
        } else if (value == null) {
            return JSONObject.NULL;
        } else if (type.isEnum()) {
            Enum<?> e = (Enum<?>) value;
            return e.name();
        } else if (type.isArray()) {
            int length = Array.getLength(value);
            Class<?> contentType = type.componentType();
            JSONArray result = new JSONArray();
            for (int i = 0; i < length; i++) {
                Object item = recordValueToJsonValue(contentType, Array.get(value, i));
                result.put(item);
            }
            return result;
        } else {
            throw new JSONException("Unsupported field type: " + type.getName());
        }
    }

    public static <T extends Record> JSONObject recordToJson(T record) throws JSONException {
        JSONObject json = new JSONObject();
        for (RecordComponent component : record.getClass().getRecordComponents()) {
            Class<?> type = component.getType();
            Object value;
            try {
                Method accessor = component.getAccessor();
                value = accessor.invoke(record);
            } catch (IllegalArgumentException | InvocationTargetException |
                     IllegalAccessException e) {
                throw new JSONException("Failed to access record component: " + component.getName(), e);
            }
            Object valueToWrite = recordValueToJsonValue(type, value);
            try {
                json.put(component.getName(), valueToWrite);
            } catch (JSONException je) {
                throw new JSONException("Error when writing '" + component.getName() + "' because " +
                        "its value is " + value, je);
            }
        }

        return json;
    }

    public static <T extends Record> T jsonToRecord(Class<T> recordClass, JSONObject json) {
        RecordComponent[] components = recordClass.getRecordComponents();
        Object[] args = new Object[components.length];

        for (int i = 0; i < components.length; i++) {
            RecordComponent component = components[i];
            Class<?> type = component.getType();
            String name = component.getName();

            boolean missAble = component.isAnnotationPresent(Optional.class);

            if (!json.has(name)) {
                if (missAble) {
                    continue;
                } else {
                    throw new IllegalArgumentException("Missing field in JSON: " + name);
                }
            }

//            if (!isSupportedType(type)) {
//                throw new IllegalArgumentException("Unsupported field type: " + type.getName());
//            }

            Object value = json.get(name);
            args[i] = convertValue(value, type);
        }

        try {
            Constructor<T> constructor = recordClass.getDeclaredConstructor(Arrays.stream(components)
                    .map(RecordComponent::getType).toArray(Class[]::new));
            return constructor.newInstance(args);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create record instance from JSON.", e);
        }
    }

    private static boolean isSupportedType(Class<?> type) {
        return type.isPrimitive() || type.equals(String.class)
                || type.equals(Boolean.class) || type.equals(Byte.class)
                || type.equals(Short.class) || type.equals(Integer.class)
                || type.equals(Long.class) || type.equals(Float.class)
                || type.equals(Double.class);
    }

    private static Object convertValue(Object value, Class<?> targetType) {
        if (targetType.isPrimitive()) {
            if (targetType == int.class) return ((Number) value).intValue();
            if (targetType == long.class) return ((Number) value).longValue();
            if (targetType == double.class) return ((Number) value).doubleValue();
            if (targetType == float.class) return ((Number) value).floatValue();
            if (targetType == boolean.class)
                return value instanceof Boolean ? value : Boolean.parseBoolean(value.toString());
            if (targetType == byte.class) return ((Number) value).byteValue();
            if (targetType == short.class) return ((Number) value).shortValue();
            if (targetType == char.class) return ((String) value).charAt(0);
        } else if (JSONObject.NULL.equals(value)) {
            return null;
        } else if (targetType == String.class) {
            return value.toString();
        } else if (Number.class.isAssignableFrom(targetType) || targetType == Boolean.class) {
            return value; // Already boxed
        } else if (targetType.isEnum()) {
            String name = value.toString();
            Object[] enumConstants = targetType.getEnumConstants();
            for (Object constant : enumConstants) {
                if (((Enum<?>) constant).name().equals(name)) {
                    return constant;
                }
            }
            throw new IllegalArgumentException("Enum type '" + targetType + "' does not have constant '" + name + "'.");
        } else if (targetType.isArray()) {
            Class<?> contentType = targetType.componentType();
            if (value instanceof JSONArray ja) {
                int length = ja.length();
                Object array = Array.newInstance(contentType, length);
                for (int i = 0; i < length; i++) {
                    Array.set(array, i, convertValue(ja.get(i), contentType));
                }
                return array;
            } else {
                throw new IllegalArgumentException("Array type must associate with json array: " + targetType.getName());
            }
        }
        throw new IllegalArgumentException("Unsupported target type: " + targetType.getName());
    }


    record A(String name, 
             double[][] values, 
             double[] values2,
             TableMetrics.PocketName pn) {
        @Override
        public String toString() {
            return "A{" +
                    "name='" + name + '\'' +
                    ", values=" + Arrays.toString(values) +
                    ", values2=" + Arrays.toString(values2) +
                    ", pn=" + pn +
                    '}';
        }
    }

    public static void main(String[] args) {
        A a = new A("ASSD", new double[][]{{2, 3, 4.5}, {3.12}}, null, TableMetrics.PocketName.BOT_LEFT);
        JSONObject json = recordToJson(a);
        System.out.println(json);
        A back = jsonToRecord(A.class, json);
        System.out.println(back);
    }
}
