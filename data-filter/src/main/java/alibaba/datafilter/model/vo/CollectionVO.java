package alibaba.datafilter.model.vo;

import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/25
 * 说明:
 */
@Data
public class CollectionVO implements Serializable {
    private Integer id;

    /**
     *
     */
    private String name;

    private String collectionName;

    /**
     *
     */
    private String description;



    private String language;

    private Boolean isSystem;
    @Serial
    private static final long serialVersionUID = 1L;
}
