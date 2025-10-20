package alibaba.datafilter.model.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;

import lombok.Builder;
import lombok.Data;

/**
 * 
 * @TableName conversation
 */
@TableName(value ="conversation")
@Data
@Builder
public class Conversation implements Serializable {
    /**
     * 
     */
    @TableId
    private String id;

    /**
     * 
     */
    private Integer userId;

    /**
     * 
     */
    private String title;

    /**
     * 
     */
    private LocalDate createdAt;

    private LocalDate updatedAt;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}