package alibaba.chatclientdemo.config;

import alibaba.chatclientdemo.utils.TimeTools;
import alibaba.chatclientdemo.utils.WeatherTools;
import com.alibaba.cloud.ai.toolcalling.time.GetTimeByZoneIdService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

    @Bean
    ChatClient chatClient(ChatClient.Builder builder) {
        return builder
                .defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
    }
    @Bean
    public GetTimeByZoneIdService getTimeByZoneIdService() {
        return new GetTimeByZoneIdService();
    }
    @Bean
    public TimeTools timeTools(GetTimeByZoneIdService service) {
        return new TimeTools(service);
    }
    @Bean
    public WeatherTools weatherTools() {
        return new WeatherTools();
    }
}