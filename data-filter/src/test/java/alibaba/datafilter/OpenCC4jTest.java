package alibaba.datafilter;

import cn.hutool.captcha.generator.RandomGenerator;
import cn.hutool.core.util.RandomUtil;
import com.github.houbb.opencc4j.util.ZhConverterUtil;
import org.junit.jupiter.api.Test;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/18
 * 说明:
 */
public class OpenCC4jTest {
    String text= """
后汉书卷一上\\r\\n　光武帝纪第一　上
　　世祖光武皇帝讳秀，字文叔，[一]南阳蔡阳人，[二]高祖九世之孙也，出自景帝生长沙定王发。[三]发生舂陵节侯买，[四]买生郁林太守外，[五]外生钜鹿都尉回，[六]回生南顿令钦，[七]钦生光武。
            """;

    @Test
    public void test() {
//        String text = "三国演义";
        if(ZhConverterUtil.isSimple(text.substring(0,20))){
            text = ZhConverterUtil.toTraditional(text);
        }
        System.out.println(text);
    }

    @Test
    public void test2() {
        System.out.println(RandomUtil.randomString(30));
    }
}
