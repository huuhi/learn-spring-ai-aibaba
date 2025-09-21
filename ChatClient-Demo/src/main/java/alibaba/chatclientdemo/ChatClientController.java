package alibaba.chatclientdemo;

import alibaba.chatclientdemo.pojo.ActorFilms;
import alibaba.chatclientdemo.utils.TimeTools;
import alibaba.chatclientdemo.utils.WeatherTools;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/20
 * 说明:
 */
@RestController
@Slf4j
public class ChatClientController {

    private  final ChatClient chatClient;

    public ChatClientController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

//    一个简单的AI聊天接口
    @PostMapping("/chat")
    public  String chat(@RequestParam(defaultValue = "Hello，你觉得现在的国际形势如何？不要输出敏感词！")String query){
        return chatClient.prompt()
                .user(query)
                .call()
                .content();
    }
    @PostMapping("/getActorAndFilms")
    public void getActorAndFilms(){
//        转换
//        ActorFilms actorFilms = chatClient.prompt()
//                .user("生成一个随机演员的电影列表")
//                .call()
//                .entity(ActorFilms.class);
//        重载方法
        List<ActorFilms> actorFilmsList = chatClient.prompt()
                .user("生成多个随机演员的电影列表，演员的需要和电影列表对应！")
                .call()
                .entity(new ParameterizedTypeReference<List<ActorFilms>>() {
                });

        assert actorFilmsList != null;
        actorFilmsList.forEach(actorFilms -> {
            log.info("演员：{}",actorFilms.getActor());
            log.info("电影列表：{}",actorFilms.getMovies());
        });
    }

//    流式返回
    @PostMapping("/stream")
    public Flux<String> stream(@RequestParam(defaultValue = "Hello，吃了没")String query){
        return chatClient
                .prompt()
//                设置选项，指定调用的模型
                .options(ChatOptions.builder().model("qwen3-max-preview").build())
                .user(query)
                .advisors()
                .stream()
                .content();
    }

    @PostMapping("/role_chat")
    public String role_chat(@RequestParam(defaultValue = "Hello，最近怎么样？")String query,
                           @RequestParam(defaultValue = " helpful assistant") String role){
        return chatClient.prompt("你是{role_name}，你要以{role_name}的说话方式回答问题")
                .system(sp->sp.param("role_name",role))
                .user(query)
                .call()
                .content();
    }

    @Autowired
    private TimeTools timeTools;
    @Autowired
    private WeatherTools weatherTools;

    @PostMapping("/tool-test")
    public String toolTest(@RequestParam(defaultValue = "现在几点了？") String query) {
        return chatClient.prompt()
                .user(query)
                .tools(timeTools)
                .call()
                .content();
    }
    @PostMapping("/getWeather")
    public String getWeather(@RequestParam(defaultValue = "北京") String cityName) {
        return chatClient.prompt()
                .user("现在北京的天气如何？")
                .tools(weatherTools)
                .call()
                .content();
    }




}
