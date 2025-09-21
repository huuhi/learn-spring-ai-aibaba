package alibaba.chatclientdemo.utils;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/20
 * 说明:
 */
public class WeatherTools {

    @Tool(description = "根据城市名称获取当前天气")
    public String getWeather(@ToolParam(description = "城市名称") String cityName) {
        return "雨天";
    }
}
