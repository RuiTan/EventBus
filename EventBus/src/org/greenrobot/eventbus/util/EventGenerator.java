package org.greenrobot.eventbus.util;

import net.sf.cglib.beans.BeanGenerator;

import java.lang.reflect.Field;
import java.util.concurrent.ConcurrentHashMap;

public class EventGenerator {

    // DefaultEvent用于标识所有由参数列表动态生成的事件类
    public static abstract class DefaultEvent{}

    private static final String DEFAULT_SEPARATOR = ";";
    private static final String PARAMETER_NAME_PREFIX = "arg";

    // 事件类的缓存，key为参数列表的类型名拼接而成，可以唯一标识一组参数类型
    private final ConcurrentHashMap<String, Class<?>> eventCache;


    private EventGenerator(){
        eventCache = new ConcurrentHashMap<>();
    }

    private BeanGenerator getBeanGenerator() {
        BeanGenerator beanGenerator = new BeanGenerator();
        // 所有动态生成类都写死为DefaultEvent的子类
        beanGenerator.setSuperclass(DefaultEvent.class);
        return beanGenerator;
    }

    private volatile static EventGenerator singleInstance;

    public static EventGenerator getInstance() {
        if (singleInstance == null) {
            synchronized (EventGenerator.class) {
                if (singleInstance == null) {
                    singleInstance = new EventGenerator();
                }
            }
        }
        return singleInstance;
    }

    /**
     * 根据参数列表的类型列表构建对应事件类的唯一标识符，根据每个参数类型名进行拼接
     * @param parameters 参数列表的类型列表
     * @return 对应事件类的唯一标识符
     */
    public String generateKeyByParameterClasses(Class<?>[] parameters) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Class<?> c : parameters) {
            // 这里的getBoxType是必要的，否则会出现跟EventBus一样不支持基本类型的问题
            stringBuilder.append(TypeUtil.getBoxType(c).getName()).append(DEFAULT_SEPARATOR);
        }
        return stringBuilder.toString();
    }

    /**
     * 根据参数列表构建对应事件类的唯一标识符，根据每个参数的类型名进行拼接
     * @param args 参数列表的类型列表
     * @return 对应事件类的唯一标识符
     */
    public String generateKeyByParameters(Object... args) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Object o : args) {
            stringBuilder.append(TypeUtil.getBoxType(o.getClass()).getName()).append(DEFAULT_SEPARATOR);
        }
        return stringBuilder.toString();
    }

    /**
     * 根据参数列表来动态生成事件类
     * @param parameters 参数列表
     * @return 事件类
     */
    public Class<?> generateEvent(Object... parameters) {
        Class<?>[] classes = new Class[parameters.length];
        int k = 0;
        for (Object o : parameters) {
            classes[k++] = o.getClass();
        }
        return generateEvent(classes);
    }

    /**
     * 根据参数列表来动态生成事件类
     * @param parameterClasses 参数列表的类型列表
     * @return 事件类
     */
    public Class<?> generateEvent(Class<?>[] parameterClasses) {
        BeanGenerator generator = getBeanGenerator();
        int k = 0;
        for (Class<?> c : parameterClasses) {
            generator.addProperty(PARAMETER_NAME_PREFIX + k++, c);
        }
        String key = generateKeyByParameterClasses(parameterClasses);
        // 保证缓存有效性，需要同步
        synchronized (this) {
            if (eventCache.containsKey(key)) {
                return eventCache.get(key);
            }else {
                Class<?> c = (Class<?>) generator.createClass();
                eventCache.put(key, c);
                return c;
            }
        }
    }

    /**
     * 解封装event，将事件拆解成参数列表
     * @param event 事件
     * @return 参数列表
     */
    public Object[] eventToParameters(Object event) throws IllegalAccessException {
        Field[] fields = event.getClass().getDeclaredFields();
        Object[] params = new Object[fields.length];
        int k = 0;
        for (Field f : fields) {
            f.setAccessible(true);
            params[k++] = f.get(event);
        }
        return params;
    }

    /**
     * 根据参数列表获取动态生成的事件类实例
     * @param parameters 参数列表
     * @return 事件类实例
     */
    public Object generateEventInstance(Object... parameters) {
        String key = generateKeyByParameters(parameters);
        Class<?> c = eventCache.get(key);
        if (c == null) {
            c = generateEvent(parameters);
        }
        try {
            Object o = c.newInstance();
            Field[] fields = c.getDeclaredFields();
            int k = 0;
            for (Field f : fields) {
                f.setAccessible(true);
                f.set(o, parameters[k++]);
            }
            return o;
        } catch (InstantiationException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
