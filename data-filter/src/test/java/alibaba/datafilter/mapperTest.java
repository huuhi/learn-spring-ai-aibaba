package alibaba.datafilter;

import alibaba.datafilter.mapper.UserMapper;
import alibaba.datafilter.model.domain.User;
import cn.hutool.core.lang.Assert;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/21
 * 说明:
 */

public class mapperTest {
    @Resource
    private UserMapper userMapper;

    @Test
    public void testSelect() {
        User u = userMapper.findByUsername("小小怪mTT2Sl");
        System.out.println(u);
    }
}
