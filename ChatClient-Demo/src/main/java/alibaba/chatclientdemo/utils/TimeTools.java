package alibaba.chatclientdemo.utils;

import com.alibaba.cloud.ai.toolcalling.time.GetTimeByZoneIdService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class TimeTools {
    private final GetTimeByZoneIdService timeService;

    public TimeTools(GetTimeByZoneIdService timeService) {
        this.timeService = timeService;
    }

    @Tool(description = "Get the time of a specified city.")
    public String getCityTime(@ToolParam(description = "Time zone id, such as Asia/Shanghai")
                              String timeZoneId) {

        return timeService.apply(new GetTimeByZoneIdService.Request(timeZoneId)).description();
    }
}