package alibaba.datafilter.model.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serial;
import java.io.Serializable;
import java.util.Date;

import lombok.Builder;
import lombok.Data;

/**
 * 
 * @TableName user
 */
@TableName(value ="user")
@Data
@Builder
public class User implements Serializable {
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
    private String email;

    /**
     * 
     */
    private String password;

    /**
     * 
     */
    private String avatar;

    /**
     * 
     */
    private Date createAt;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}