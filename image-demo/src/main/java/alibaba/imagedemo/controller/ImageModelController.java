package alibaba.imagedemo.controller;

import org.springframework.ai.image.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
public class ImageModelController {
  private final ImageModel imageModel;

  ImageModelController(ImageModel imageModel) {
    this.imageModel = imageModel;
  }

  @RequestMapping("/image")
  public String image(String input) {
    ImageOptions options = ImageOptionsBuilder.builder()
        .model("qwen-image")
        .build();

    ImagePrompt imagePrompt = new ImagePrompt(input, options);
    ImageResponse response = imageModel.call(imagePrompt);
    String imageUrl = response.getResult().getOutput().getUrl();

    return "redirect:" + imageUrl;
  }
    @GetMapping("/image/multipleConditions")
    public ResponseEntity<?> multipleConditions(
            @RequestParam(value = "subject", defaultValue = "一只会编程的猫") String subject,
            @RequestParam(value = "environment", defaultValue = "办公室") String environment,
            @RequestParam(value = "height", defaultValue = "1024") Integer height,
            @RequestParam(value = "width", defaultValue = "1024") Integer width,
            @RequestParam(value = "style", defaultValue = "生动") String style) {

        String prompt = String.format(
                "一个%s，置身于%s的环境中，使用%s的艺术风格，高清4K画质，细节精致",
                subject, environment, style
        );

        ImageOptions options = ImageOptionsBuilder.builder()
                .height(height)
                .width(width)
                .build();

        try {
            ImageResponse response = imageModel.call(new ImagePrompt(prompt, options));
            if (response.getResult() == null) {
                return ResponseEntity.ok(response.getMetadata().getCreated());
            }
            return ResponseEntity.ok(response.getResult().getOutput().getUrl());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "error", "图像生成失败",
                            "message", e.getMessage(),
                            "timestamp", LocalDateTime.now()
                    ));
        }
    }
}