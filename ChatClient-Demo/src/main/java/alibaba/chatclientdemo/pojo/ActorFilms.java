package alibaba.chatclientdemo.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/9/20
 * 说明:
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ActorFilms {
    private String actor;
    private List<String> movies;
}
