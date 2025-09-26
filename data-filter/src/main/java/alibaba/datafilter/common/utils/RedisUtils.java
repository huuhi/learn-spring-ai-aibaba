package alibaba.datafilter.common.utils;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/26
 * 说明:
 */
@Component
public class RedisUtils {

    private final StringRedisTemplate stringRedisTemplate;

    public RedisUtils(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     *
     * @param key 键
     * @param value 值
     * @param ttl 过期时间，默认单位:分钟
     */
    public void set(String key, String value,Long ttl) {
        stringRedisTemplate.opsForValue().set(key, value,ttl, TimeUnit.SECONDS);
    }
    public Boolean exists(String key) {
        return stringRedisTemplate.hasKey(key);
    }
    public String get(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

}
