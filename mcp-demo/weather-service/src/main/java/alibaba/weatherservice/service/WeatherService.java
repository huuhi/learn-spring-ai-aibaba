package alibaba.weatherservice.service;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/23
 * 说明:
 */
@Service
public class WeatherService {
    @Tool(description = "根据城市名称获取时间")
    public String getTime(String cityName) {
        LocalDateTime now = LocalDateTime.now();
        return cityName + "现在时间是：" + now.format(DateTimeFormatter.ofPattern("yyyy年MM月dd日 HH:mm:ss"));
    }
    @Tool(description = "根据城市名称获取天气")
    public String getWeather(String cityName) {
        return cityName + "现在天气是：晴天";
    }

}
