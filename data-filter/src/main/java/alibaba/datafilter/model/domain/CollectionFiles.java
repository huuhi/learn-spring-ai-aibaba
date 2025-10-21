package alibaba.datafilter.model.domain;

import alibaba.datafilter.model.em.FileStatus;
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
 * @TableName collection_files
 */
@TableName(value ="collection_files")
@Data
@Builder
public class CollectionFiles implements Serializable {
    /**
     * 
     */
    @TableId
    private Integer id;

    /**
     * 
     */
    private Integer collectionId;

    /**
     * 
     */
    private Long fileId;

    /**
     * 
     */
    private Date createdAt;

    /**
     * PENDING, PROCESSING, COMPLETED, FAILED
     */
    @TableField(value = "status")
    private FileStatus status;

    @Serial
    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}