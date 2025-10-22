package alibaba.datafilter.service.impl;

import alibaba.datafilter.common.utils.AliOssUtil;
import alibaba.datafilter.common.utils.FileTypeUtils;
import alibaba.datafilter.model.domain.Collection;
import alibaba.datafilter.model.domain.CollectionFiles;
import alibaba.datafilter.model.em.FileStatus;
import alibaba.datafilter.model.vo.FileVo;
import alibaba.datafilter.service.CollectionFilesService;
import alibaba.datafilter.service.CollectionService;
import cn.hutool.core.bean.BeanUtil;
import com.aliyuncs.exceptions.ClientException;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import alibaba.datafilter.model.domain.KnowledgeFile;
import alibaba.datafilter.service.KnowledgeFileService;
import alibaba.datafilter.mapper.KnowledgeFileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static alibaba.datafilter.common.content.RedisConstant.TEMP_USER_ID;

/**
* @author windows
* @description 针对表【knowledge_file(知识库文件信息表)】的数据库操作Service实现
* @createDate 2025-10-18 17:14:21
*/
@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeFileServiceImpl extends ServiceImpl<KnowledgeFileMapper, KnowledgeFile>
    implements KnowledgeFileService{
    private final AliOssUtil aliOssUtil;
    private final CollectionFilesService collectionFilesService;
    private final CollectionService collectionService;
    private final Function<String, MilvusVectorStore> dynamicVectorStoreFactory;
    private final KnowledgeFileMapper knowledgeFileMapper;


//    TODO 之后可以考虑加一个用户文件上限
    @Override
//    保证原子
    @Transactional
    public ResponseEntity<?> uploadFile(MultipartFile[] files) {
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
                FileStatus status=FileStatus.COMPLETED;
                String errorMessage = "";

                try {
                    byte[] content = file.getBytes();
                    assert originalFilename != null;
                    url = aliOssUtil.uploadDocument(content, originalFilename, userId);
                } catch (IOException e) {
                    status=FileStatus.FAILED;
                    errorMessage=e.getMessage().substring(0, 100);
                    log.error("文件处理错误：{}",e.getMessage());
                } catch (ClientException e) {
                    status=FileStatus.FAILED;
                    errorMessage=e.getMessage().substring(0, 100);
                    log.error("文件上传错误：{}",e.getMessage());
                }
                KnowledgeFile knowledgeFile = KnowledgeFile.builder()
                        .userId(userId)
                        .fileName(originalFilename)
                        .fileSize(size)
                        .fileType(file.getContentType())
                        .ossKey(url)
                        .status(status)
                        .errorMessage(errorMessage)
                        .build();
                knowledgeFiles.add(knowledgeFile);

            }else{
                log.error("文件类型错误：{}",originalFilename);
            }

        }
//        需要判断集合是否为空
        if (knowledgeFiles.isEmpty()) {
            return ResponseEntity.badRequest().body("请上传文件并且上传正确的文件类型");
        }
        knowledgeFileMapper.saveBatchAutoStatus(knowledgeFiles);
//            获取ID
        return ResponseEntity.ok(knowledgeFiles.stream().map(KnowledgeFile::getId).toList());
    }

    @Override
    public ResponseEntity<List<FileVo>> getFileList() {
        List<FileVo> fileList = getFileList(List.of());
        return ResponseEntity.ok(fileList);
    }

    @Override
    public List<FileVo> getFileListByIds(List<Long> ids) {
        return getFileList(ids);
    }

    @Override
    @Transactional
    public ResponseEntity<String> deleteFiles(Long[] ids) {
        List<KnowledgeFile> list = lambdaQuery()
                .in(KnowledgeFile::getId, List.of(ids))
                .eq(KnowledgeFile::getUserId, TEMP_USER_ID)
                .list();
//        获取要删除的文件id
        List<Long> fileIds = list.stream().map(KnowledgeFile::getId).toList();

//        获取文件列表，准备删除，
//        删除文件之前先获取文件关联的知识库，先将知识库中的数据删除，再删除关联信息，最后删除文件
        List<CollectionFiles> collectionFiles = collectionFilesService.lambdaQuery()
                .in(CollectionFiles::getFileId, fileIds)
                .list();
//        需要获取关联知识库的id
        collectionFiles.forEach(c->{
            Long fileId = c.getFileId();
            Integer collectionId = c.getCollectionId();
//            获取集合名
            String collectionName = collectionService.lambdaQuery()
                    .eq(Collection::getId, collectionId)
                    .one().getCollectionName();
            MilvusVectorStore vectorStore = dynamicVectorStoreFactory.apply(collectionName);
            vectorStore.delete("file_id == '" + fileId + "'");
//            ragUtils.deleteDocumentsByFileId(vectorStore, fileId.toString());
        });
        return removeBatchByIds(fileIds)?ResponseEntity.ok("删除成功") : ResponseEntity.badRequest().body("删除失败");

    }

    private List<FileVo> getFileList(List<Long> ids) {
//        TODO 之后要从线程中获取用户的id

        List<KnowledgeFile> list = lambdaQuery()
                .eq(KnowledgeFile::getUserId, TEMP_USER_ID)
                .in(!ids.isEmpty(),KnowledgeFile::getId, ids)
                .list();
        return BeanUtil.copyToList(list, FileVo.class);
    }


}




