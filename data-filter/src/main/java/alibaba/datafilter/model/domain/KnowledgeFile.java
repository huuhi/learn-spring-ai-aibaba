package alibaba.datafilter.model.domain;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;

import lombok.Builder;
import lombok.Data;

/**
 * 知识库文件信息表
 * @TableName knowledge_file
 */
@TableName(value ="knowledge_file")
@Data
@Builder
public class KnowledgeFile implements Serializable {
    /**
     * 主键ID
     */
    @TableId
    private Long id;

    /**
     * 所属用户ID
     */
    private Integer userId;

    /**
     * 原始文件名
     */
    private String fileName;

    /**
     * 文件大小 (in bytes)
     */
    private Long fileSize;

    /**
     * 文件MIME类型
     */
    private String fileType;

    /**
     * 文件在OSS中的Key或完整URL
     */
    private String ossKey;

    /**
     * 文件处理状态 (PENDING, PROCESSING, COMPLETED, FAILED)
     */
    private String status;

    /**
     * 处理失败时的错误信息
     */
    private String errorMessage;

    /**
     * 成功处理后的文档块数量
     */
    private Integer chunkCount;

    /**
     * 文件上传时间
     */
    private Date uploadedAt;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}