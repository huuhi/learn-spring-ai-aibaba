package alibaba.datafilter.common.utils;

import alibaba.datafilter.common.properties.AliOssProperties;
import com.aliyun.oss.*;
import com.aliyun.oss.common.auth.CredentialsProviderFactory;
import com.aliyun.oss.common.auth.EnvironmentVariableCredentialsProvider;
import com.aliyun.oss.common.comm.SignVersion;
import com.aliyun.oss.model.OSSObject;
import com.aliyuncs.exceptions.ClientException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class AliOssUtil {

    private final AliOssProperties aliOssProperties;

    public AliOssUtil(AliOssProperties aliOssProperties) {
        this.aliOssProperties = aliOssProperties;
    }

    /**
     * 图片上传
     *
     * @param content 文件字节数组
     * @param originalFilename 原始文件名
     * @return 文件访问路径
     */
    public String uploadImage(byte[] content, String originalFilename) throws ClientException {
        String endpoint = aliOssProperties.getEndpoint();
        String bucketName = aliOssProperties.getBucketName();
        String region = aliOssProperties.getRegion();
        String dir=aliOssProperties.getImageDir();

        log.info("这里！！！  endpoint:{}, bucketName:{}, region:{}", endpoint, bucketName, region);


        // 从环境变量中获取访问凭证。运行本代码示例之前，请确保已设置环境变量OSS_ACCESS_KEY_ID和OSS_ACCESS_KEY_SECRET。
        EnvironmentVariableCredentialsProvider credentialsProvider = CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();

        // 填写Object完整路径，例如202406/1.png。Object完整路径中不能包含Bucket名称。
        //获取当前系统日期的字符串,格式为 yyyy/MM
        //生成一个新的不重复的文件名
        String newFileName = UUID.randomUUID() + originalFilename.substring(originalFilename.lastIndexOf("."));
        String objectName = dir + "/" + newFileName;
        log.info("objectName: {}", objectName);

        // 创建OSSClient实例。
        return getUrl(content, endpoint, bucketName, region, credentialsProvider, objectName);
    }
    
    /**
     * 文档上传
     *
     * @param content 文件字节数组
     * @param originalFilename 原始文件名
     * @param userId 用户ID，用于创建用户专属文件夹
     * @return 文件访问路径
     */
    public String uploadDocument(byte[] content, String originalFilename, Integer userId) throws ClientException {
        String endpoint = aliOssProperties.getEndpoint();
        String bucketName = aliOssProperties.getBucketName();
        String region = aliOssProperties.getRegion();
        String dir = aliOssProperties.getFileDir();

        log.info("文档上传: endpoint:{}, bucketName:{}, region:{}", endpoint, bucketName, region);

        // 从环境变量中获取访问凭证。运行本代码示例之前，请确保已设置环境变量OSS_ACCESS_KEY_ID和OSS_ACCESS_KEY_SECRET。
        EnvironmentVariableCredentialsProvider credentialsProvider = CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();

        // 生成一个新的不重复的文件名
        String newFileName = UUID.randomUUID() + originalFilename.substring(originalFilename.lastIndexOf("."));
        // 构造包含用户ID的文件路径
        String objectName = dir + "/user_" + userId + "/" + newFileName;
        log.info("文档上传路径: {}", objectName);

        // 创建OSSClient实例。
        return getUrl(content, endpoint, bucketName, region, credentialsProvider, objectName);
    }
    
    /**
     * 上传文件并返回访问URL
     *
     * @param content 文件字节数组
     * @param endpoint OSS端点
     * @param bucketName 存储桶名称
     * @param region 区域
     * @param credentialsProvider 凭证提供者
     * @param objectName 对象名称
     * @return 文件访问路径
     */
    private String getUrl(byte[] content, String endpoint, String bucketName, String region, EnvironmentVariableCredentialsProvider credentialsProvider, String objectName) {
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
        OSS ossClient = OSSClientBuilder.create()
                .endpoint(endpoint)
                .credentialsProvider(credentialsProvider)
                .clientConfiguration(clientBuilderConfiguration)
                .region(region)
                .build();

        try {
            ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(content));
        } finally {
            ossClient.shutdown();
        }

        return endpoint.split("//")[0] + "//" + bucketName + "." + endpoint.split("//")[1] + "/" + objectName;
    }
    
    /**
     * 从OSS下载文件内容
     *
     * @param objectName OSS中的对象名称
     * @return 文件字节数组
     */
    public byte[] downloadDocument(String objectName) throws ClientException {
        String endpoint = aliOssProperties.getEndpoint();
        String bucketName = aliOssProperties.getBucketName();
        String region = aliOssProperties.getRegion();

        // 从环境变量中获取访问凭证
        EnvironmentVariableCredentialsProvider credentialsProvider = CredentialsProviderFactory.newEnvironmentVariableCredentialsProvider();

        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
        OSS ossClient = OSSClientBuilder.create()
                .endpoint(endpoint)
                .credentialsProvider(credentialsProvider)
                .clientConfiguration(clientBuilderConfiguration)
                .region(region)
                .build();

        try {
            OSSObject ossObject = ossClient.getObject(bucketName, objectName);
            return ossObject.getObjectContent().readAllBytes();
        } catch (IOException e) {
            log.error("下载文件失败: {}", e.getMessage());
            throw new RuntimeException("下载文件失败", e);
        } finally {
            ossClient.shutdown();
        }
    }
}