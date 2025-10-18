package alibaba.datafilter.service.impl;

import alibaba.datafilter.common.utils.AliOssUtil;
import alibaba.datafilter.common.utils.FileTypeUtils;
import com.aliyuncs.exceptions.ClientException;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import alibaba.datafilter.model.domain.KnowledgeFile;
import alibaba.datafilter.service.KnowledgeFileService;
import alibaba.datafilter.mapper.KnowledgeFileMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static alibaba.datafilter.common.content.RedisConstant.TEMP_USER_ID;

/**
* @author windows
* @description 针对表【knowledge_file(知识库文件信息表)】的数据库操作Service实现
* @createDate 2025-10-18 17:14:21
*/
@Service
@Slf4j
public class KnowledgeFileServiceImpl extends ServiceImpl<KnowledgeFileMapper, KnowledgeFile>
    implements KnowledgeFileService{
    private final AliOssUtil aliOssUtil;

    public KnowledgeFileServiceImpl(AliOssUtil aliOssUtil ) {
        this.aliOssUtil = aliOssUtil;
    }

    @Override
//    保证原子
    @Transactional
    public ResponseEntity<String> uploadFile(MultipartFile[] files) {
        List<KnowledgeFile> knowledgeFiles =new ArrayList<>();

//      TODO  用户id需要在线程中获取
        Integer userId=TEMP_USER_ID;
//        遍历文件
        for (MultipartFile file : files) {

            String url="";
            String originalFilename = file.getOriginalFilename();
            long size = file.getSize();
//           判断文件的类型是否符合 检查文件的大小,不超过10mb,并且文件不能为空
            if (FileTypeUtils.isSupportedDocument(file)&& size <= 10 * 1024 * 1024 && size != 0) {
//                允许的文件类型，开始上传
//                获取文件字节数组
                try {
                    byte[] content = file.getBytes();
                    assert originalFilename != null;
                    url = aliOssUtil.uploadDocument(content, originalFilename, userId);
                } catch (IOException e) {
                    log.error("文件处理错误：{}",e.getMessage());
                } catch (ClientException e) {
                    log.error("文件上传错误：{}",e.getMessage());
                }
                KnowledgeFile knowledgeFile = KnowledgeFile.builder()
                        .userId(userId)
                        .fileName(originalFilename)
                        .fileSize(size)
                        .fileType(file.getContentType())
                        .ossKey(url)
                        .build();
                knowledgeFiles.add(knowledgeFile);

            }else{
                log.error("文件类型错误：{}",originalFilename);
            }

        }
//        需要判断集合是否为空
        if (knowledgeFiles.isEmpty()) {
            return ResponseEntity.badRequest().body("请上传支持的文件类型");
        }
        if (saveBatch(knowledgeFiles)) {
            return ResponseEntity.ok("上传成功");
        }
        return ResponseEntity.badRequest().body("上传失败");
    }
}




