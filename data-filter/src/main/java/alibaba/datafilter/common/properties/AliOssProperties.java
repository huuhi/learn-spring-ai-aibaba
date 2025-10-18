package alibaba.datafilter.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "spring.aliyun")
@Data
public class AliOssProperties {

    private String endpoint;
    private String bucketName;
    private String region;
    private String imageDir;
    private String fileDir;

}