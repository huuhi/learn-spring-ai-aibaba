package alibaba.datafilter;

import com.github.houbb.opencc4j.util.ZhConverterUtil;
import org.junit.jupiter.api.Test;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/18
 * 说明:
 */
public class OpenCC4jTest {

    @Test
    public void test() {
        String text = "三国演义";
        String convertedText = ZhConverterUtil.toTraditional(text);
        System.out.println(convertedText);
    }

}
