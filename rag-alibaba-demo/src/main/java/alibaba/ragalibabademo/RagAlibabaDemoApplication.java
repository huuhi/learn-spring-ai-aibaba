package alibaba.ragalibabademo;

import alibaba.ragalibabademo.config.AliKnowledgeBase;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import(AliKnowledgeBase.class)

public class RagAlibabaDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RagAlibabaDemoApplication.class, args);
    }

}
