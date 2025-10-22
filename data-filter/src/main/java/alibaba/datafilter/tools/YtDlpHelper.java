package alibaba.datafilter.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
@Component
@Slf4j
public class YtDlpHelper {

    // --- 可配置的参数 ---
    private static final String YT_DLP_EXECUTABLE_PATH = "yt-dlp"; // 如果yt-dlp在系统PATH中，直接用名字即可
    private static final long TIMEOUT_SECONDS = 120; // 命令执行的超时时间，单位秒

    /**
     * 获取指定URL视频的字幕内容。
     *
     * @param videoUrl       视频的URL，例如 "<a href="https://www.bilibili.com/video/BV1eDndzEEWV/">...</a>"
     * @param browser        用于获取cookies的浏览器，例如 "edge", "chrome", "firefox"
     * @param subtitleLang   想要获取的字幕语言代码，例如 "ai-zh"
     * @return 字幕文件的完整内容（字符串形式）。如果失败或没有找到字幕，则返回 null。
     */
    @Tool(name = "get_subtitles",description = "获取指定视频的字幕")
    public  String getSubtitles(@ToolParam(description = "有效的视频url,比如:https://www.bilibili.com/video/视频编号") String videoUrl,@ToolParam(description = "保存cookie的浏览器,比如:chrome, firefox, edge") String browser,@ToolParam(description = "字幕语言代码,比如：ai-zh") String subtitleLang) {
        List<String> command = new ArrayList<>();
        command.add(YT_DLP_EXECUTABLE_PATH);
        command.add("--cookies-from-browser");
        command.add(browser);
        command.add("--write-subs");
        command.add("--sub-lang");
        command.add(subtitleLang);
        command.add("--skip-download");
        command.add("--print");
        command.add("requested_subtitles");
        command.add(videoUrl);

        log.debug("完整的命令：{}", String.join(" ", command));

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();

            // --- 核心改动在这里 ---
            // 1. 判断操作系统
            String os = System.getProperty("os.name").toLowerCase();
            Charset charset;
            if (os.contains("win")) {
                // 2. 如果是 Windows，就使用 GBK 解码
                log.info("Win,使用GDK：{}",os);
                charset = Charset.forName("GBK");
            } else {
                log.info("非Win,使用UTF-8：{}",os);
                // 3. 如果是其他系统（Linux, macOS等），使用 UTF-8
                System.out.println("Detected non-Windows OS, using UTF-8 charset.");
                charset = StandardCharsets.UTF_8;
            }

            String result;
            // 4. 动态选择的 charset 来读取流
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), charset))) {
                result = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }

            boolean finished = process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                log.error("超时！");
                return null;
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                if (!result.trim().isEmpty() && !result.contains("There are no subtitles for the requested languages")) {
                    log.info("成功获取字幕");
                    // 这里可以加入我们之前讨论的解析 'data' 字段的逻辑
                    return result;
                } else {
                    log.info("执行失败！");
                    return null;
                }
            } else {
                log.warn("获取字幕失败");
                return null;
            }

        } catch (IOException | InterruptedException e) {
            log.error("执行命令时出错：{}", e.getMessage());
            return null;
        }
    }
//    获取字幕列表


    /**
     * 获取指定URL视频的所有可用字幕列表。
     *
     * @param videoUrl 视频的URL
     * @param browser  用于获取cookies的浏览器 ("edge", "chrome", etc.)
     * @return 一个 Map，键是字幕语言代码(e.g., "ai-zh")，值是字幕名称(e.g., "AI生成")。
     *         如果失败或没有字幕，返回一个空的Map。
     */
    @Tool(name = "get_",description = "获取指定视频的字幕语言代码列表(包括弹幕)")
    public  Map<String, String> getAvailableSubtitles(@ToolParam(description = "有效的视频url") String videoUrl,@ToolParam(description = "保存cookie的浏览器") String browser) {
        List<String> command = new ArrayList<>();
        command.add(YT_DLP_EXECUTABLE_PATH);
        command.add("--cookies-from-browser");
        command.add(browser);
        command.add("--list-subs");
        command.add(videoUrl);

        log.debug("获取字幕列表完整的命令：{}", String.join(" ", command));

        Map<String, String> subtitlesMap = new HashMap<>();

        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            Charset charset = System.getProperty("os.name").toLowerCase().contains("win")
                    ? Charset.forName("GBK")
                    : StandardCharsets.UTF_8;

            String fullOutput;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), charset))) {
                fullOutput = reader.lines().collect(Collectors.joining(System.lineSeparator()));
            }

            if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                process.destroyForcibly();
                log.error("获取字幕列表超时!");
                return subtitlesMap;
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("成功获取字幕列表");

                // --- 核心优化在这里 ---
                // 1. 定位字幕列表的开始位置
                String header = "Language Formats"; // 这是字幕列表的标题行
                int startIndex = fullOutput.indexOf(header);

                if (startIndex != -1) {
                    // 2. 只截取标题行之后的部分进行解析
                    String subtitleBlock = fullOutput.substring(startIndex + header.length()).trim();
                    System.out.println(subtitleBlock);

                    String[] split = subtitleBlock.split("\\s+");
                    for (String s : split) {
                        System.out.println(s);
                    }
                    System.out.println(split.length);
                    for (int i = 0; i < split.length; i+=2) {
                        String languageCode = split[i];
                        String Formats = split[i + 1];
                        subtitlesMap.put(languageCode, Formats);
                    }
                } else {
                    log.warn("没有找到字幕");
                }
            } else {
                log.warn("啥也没有找到");
            }

        } catch (IOException | InterruptedException e) {
            log.error("获取字幕列表时出错：{}", e.getMessage());
        }

        return subtitlesMap;
    }
}