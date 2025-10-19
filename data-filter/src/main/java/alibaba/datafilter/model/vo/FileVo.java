package alibaba.datafilter.model.vo;

import alibaba.datafilter.model.em.FileStatus;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/19
 * 说明:
 */
@Data
@AllArgsConstructor
public class FileVo {
    /**
     * 主键ID
     */
    private Long id;

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
    private FileStatus status;

    /**
     * 处理失败时的错误信息
     */
    private String errorMessage;


    /**
     * 文件上传时间
     */
    private Date uploadedAt;


}
