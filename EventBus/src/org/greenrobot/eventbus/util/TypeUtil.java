package org.greenrobot.eventbus.util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class TypeUtil {
    private static final Map<Class<?>, Class<?>> map = new HashMap<>();
    static {
        map.put(float.class, Float.class);
        map.put(double.class, Double.class);
        map.put(char.class, Character.class);
        map.put(byte.class, Byte.class);
        map.put(short.class, Short.class);
        map.put(int.class, Integer.class);
        map.put(long.class, Long.class);
        map.put(boolean.class, Boolean.class);
    }

    public static Class<?> getBoxType(Class<?> primitiveType) {
        if (primitiveType.isPrimitive()) {
            return map.get(primitiveType);
        }
        return primitiveType;
    }

    public static Object getBoxInstance(Object primitiveValue) throws InstantiationException, IllegalAccessException, NoSuchFieldException {
        if (primitiveValue.getClass().isPrimitive()) {
            Class<?> c = map.get(primitiveValue.getClass());
            Object o = c.newInstance();
            Field value = c.getDeclaredField("value");
            value.setAccessible(true);
            value.set(o, primitiveValue);
            return o;
        }
        return primitiveValue;
    }


}
