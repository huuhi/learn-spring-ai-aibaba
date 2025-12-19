package alibaba.datafilter.model.vo;
import lombok.Builder;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/20
 * 说明:
 */
@Data
@Builder
public class ConversationVO implements Serializable {
    private String id;

//    private Integer userId;

    /**
     *
     */
    private String title;

    /**
     *
     */
    private String updateTime;

    @Serial
    private static final long serialVersionUID = 1L;
}
