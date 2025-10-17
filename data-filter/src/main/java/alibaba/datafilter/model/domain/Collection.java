package alibaba.datafilter.model.domain;


import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;

import lombok.Builder;
import lombok.Data;

/**
 * 
 * @TableName collection
 */
@TableName(value ="collection")
@Data
@Builder
public class Collection implements Serializable {
    /**
     * 
     */
    @TableId
    private Integer id;

    /**
     * 
     */
    private String name;

    /**
     * 
     */
    private String description;

    /**
     * 
     */
    private Integer userId;

    private Boolean isSystem;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}