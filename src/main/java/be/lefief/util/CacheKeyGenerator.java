package be.lefief.util;

import org.springframework.cache.interceptor.KeyGenerator;

import java.lang.reflect.Method;
import java.util.UUID;

public class CacheKeyGenerator implements KeyGenerator {
    @Override
    public Object generate(Object o, Method method, Object... objects) {
        if (objects[0] instanceof UUID) return o.toString();
        else return o;
    }

}
