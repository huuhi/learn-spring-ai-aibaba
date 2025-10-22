package alibaba.datafilter.service.impl;

import alibaba.datafilter.common.utils.AliOssUtil;
import alibaba.datafilter.mapper.CollectionFilesMapper;
import alibaba.datafilter.model.domain.CollectionFiles;
import alibaba.datafilter.model.dto.UploadFileConfigDTO;
import alibaba.datafilter.model.em.FileStatus;
import alibaba.datafilter.model.vo.FileVo;
import alibaba.datafilter.service.CollectionFilesService;
import alibaba.datafilter.utils.CharacterTextSplitter;
import alibaba.datafilter.utils.RagUtils;
import com.aliyuncs.exceptions.ClientException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.milvus.MilvusVectorStore;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;



/**
 * @author 胡志坚
 * @version 1.0
 * 创造日期 2025/10/19
 * 说明:
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AsyncFileProcessingService {
    private final AliOssUtil aliOssUtil;
    private final CollectionFilesService collectionFilesService;
    private final RagUtils ragUtils;
    private final CollectionFilesMapper collectionFilesMapper;
    /**
     * 异步处理OSS文件
     * @param fileVo 文件信息
     * @param vectorStore 向量存储
     * @param sourceDescription 源描述
     * @param uploadFileConfig 上传文件配置
     * @param collectionFilesBuilder 集合文件构建器
     * @param language 知识库的语言
     */
    @Async
    public void processingTypeFromOss(FileVo fileVo, MilvusVectorStore vectorStore, String sourceDescription, UploadFileConfigDTO uploadFileConfig, CollectionFiles.CollectionFilesBuilder collectionFilesBuilder, String language) {
        log.info("开始处理OSS文件：fileName:{},fileSize:{}", fileVo.getFileName(), fileVo.getFileSize());
        CollectionFiles collectionFiles;
        try {
            // 从OSS下载文件到临时文件
            Path tempFile = Files.createTempFile("upload_", "-" + fileVo.getFileName());

            // 从OSS下载文件内容
            String ossKey = extractOssKeyFromUrl(fileVo.getOssKey());
            byte[] fileContent = aliOssUtil.downloadDocument(ossKey);
            Files.write(tempFile, fileContent);

            List<Document> documents;
            String fileName = fileVo.getFileName();

            if (fileName.toLowerCase().endsWith(".pdf")) {
                // 使用pdf读取器
                PagePdfDocumentReader pdfReader = new PagePdfDocumentReader(tempFile.toUri().toString());
                documents = pdfReader.get();
                log.info("使用PD处理文件：{}", fileName);
            } else {
                // 使用Tika处理其他类型文件
                TikaDocumentReader tikaReader = new TikaDocumentReader(tempFile.toUri().toString());
                documents = tikaReader.get();
                log.info("使用tika处理文件：{}", fileName);
            }
//            查看是否需要转换
//            documents=ragUtils.transfer(documents,language);
            // 2. 创建自定义的TextSplitter实例
            CharacterTextSplitter textSplitter = new CharacterTextSplitter(uploadFileConfig.getChunkSize(), uploadFileConfig.getChunkOverlap(), Arrays.stream(uploadFileConfig.getSeparators()).toList());

            // 3. 应用分块 (现在textSplitter.apply会返回已经处理好的所有小块)
            List<Document> splitDocuments = textSplitter.apply(documents);
            log.info("原始文档数:{},处理之后:{}", documents.size(), splitDocuments.size());
            log.debug("查看是否需要转换语言,知识库语言:{}",language);
            splitDocuments=ragUtils.transfer(splitDocuments,language);


            log.debug("预览处理块：{}",splitDocuments.stream().limit(10).toList());

            List<Document> finalDocuments = getDocuments(sourceDescription, splitDocuments, fileName,fileVo.getId());
            vectorStore.add(finalDocuments);
            Files.deleteIfExists(tempFile);
            log.info("文件处理完毕：fileName:{},原始文档数:{},分割后文档数:{}",
                    fileName, documents.size(), finalDocuments.size());
            
            // 处理成功，更新状态
            collectionFiles = collectionFilesBuilder.status(FileStatus.COMPLETED).build();
        } catch (IOException | ClientException e) {
            // 处理失败，更新状态
            collectionFiles = collectionFilesBuilder.status(FileStatus.FAILED).build();
            log.error("文件处理失败：fileName:{},error:{}", fileVo.getFileName(), e.getMessage());
        } catch (Exception e) {
            // 其他异常处理
            collectionFiles = collectionFilesBuilder.status(FileStatus.FAILED).build();
            log.error("文件处理过程中发生未知错误：fileName:{},error:{}", fileVo.getFileName(), e.getMessage());
        }
        
        // 保存处理结果
        if (collectionFiles != null) {
            collectionFilesMapper.saveAutoStatus(collectionFiles);
        }
    }
    @NotNull
    public static List<Document> getDocuments(String sourceDescription, List<Document> splitDocuments, String fileName, String id) {
        List<Document> finalDocuments = new ArrayList<>();
        for (Document split : splitDocuments) {
            Map<String, Object> newMetadata = new HashMap<>(split.getMetadata());
            newMetadata.put("source_description", sourceDescription);
            newMetadata.put("file_name", fileName);
            newMetadata.put("file_id",id);
            Document enrichedDoc = new Document(
                    split.getId(),
                    Objects.requireNonNull(split.getText()),
                    newMetadata
            );
            finalDocuments.add(enrichedDoc);
        }
        return finalDocuments;
    }
    
    /**
     * 从完整URL中提取OSS对象键
     * @param ossUrl 完整的OSS URL
     * @return OSS对象键
     */
    private String extractOssKeyFromUrl(String ossUrl) {
        try {
            URL url = new URL(ossUrl);
            String path = url.getPath();
            // 移除开头的斜杠
            if (path.startsWith("/")) {
                path = path.substring(1);
            }
            return path;
        } catch (MalformedURLException e) {
            // 如果不是有效的URL，假设它已经是对象键
            log.warn("OSS URL格式不正确，使用原始值: {}", ossUrl);
            return ossUrl;
        }
    }



}