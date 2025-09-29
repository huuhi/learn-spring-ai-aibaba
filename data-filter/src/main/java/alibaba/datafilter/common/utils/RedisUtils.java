package alibaba.datafilter.common.utils;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class RedisUtils {
    
//    private static final log log = logFactory.getlog(RedisUtils.class);

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
    public void set(String key, String value, Long ttl) {
        try {
            stringRedisTemplate.opsForValue().set(key, value, ttl, TimeUnit.MINUTES);
            log.debug("Redis set key: {}, value: {}, ttl: {} minutes", key, value, ttl);
        } catch (Exception e) {
            log.error("Redis set operation failed for key: {}", key, e);
        }
    }
    
    public Boolean exists(String key) {
        try {
            Boolean result = stringRedisTemplate.hasKey(key);
            log.debug("Redis exists check for key: {}, result: {}", key, result);
            return result;
        } catch (Exception e) {
            log.error("Redis exists operation failed for key: {}", key, e);
            return false;
        }
    }
    
    public String get(String key) {
        try {
            String result = stringRedisTemplate.opsForValue().get(key);
            log.debug("Redis get operation for key: {}, result: {}", key, result);
            return result;
        } catch (Exception e) {
            log.error("Redis get operation failed for key: {}", key, e);
            return null;
        }
    }

    public void delete(String s) {
        stringRedisTemplate.delete(s);
    }
}
