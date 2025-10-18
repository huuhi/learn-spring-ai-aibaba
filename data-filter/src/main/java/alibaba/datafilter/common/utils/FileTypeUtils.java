package alibaba.datafilter.common.utils;

import org.apache.tika.Tika;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 文件类型工具类
 * 用于判断上传的文件是否为文档类型，可被TikaDocumentReader处理
 */
public class FileTypeUtils {
    
    // 支持的文档类型MIME列表
    private static final Set<String> SUPPORTED_MIME_TYPES = new HashSet<>(Arrays.asList(
        // 文本文件
        "text/plain",
        "text/html",
        "text/csv",
        "text/markdown",
        
        // Microsoft Office文档
        "application/msword",  // .doc
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",  // .docx
        "application/vnd.ms-excel",  // .xls
        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",  // .xlsx
        "application/vnd.ms-powerpoint",  // .ppt
        "application/vnd.openxmlformats-officedocument.presentationml.presentation",  // .pptx
        
        // OpenDocument格式
        "application/vnd.oasis.opendocument.text",  // .odt
        "application/vnd.oasis.opendocument.spreadsheet",  // .ods
        "application/vnd.oasis.opendocument.presentation",  // .odp
        
        // PDF
        "application/pdf",
        
        // 电子书格式
        "application/epub+zip",  // .epub
        
        // 其他文档格式
        "application/rtf"  // .rtf
    ));
    
    // 支持的文件扩展名列表
    private static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<>(Arrays.asList(
        ".txt", ".md", ".markdown", ".html", ".htm", ".csv",
        ".doc", ".docx", ".xls", ".xlsx", ".ppt", ".pptx",
        ".odt", ".ods", ".odp", ".pdf", ".epub", ".rtf"
    ));
    
    private static final Tika tika = new Tika();
    
    /**
     * 判断文件是否为支持的文档类型
     * 
     * @param file MultipartFile对象
     * @return 如果是支持的文档类型返回true，否则返回false
     */
    public static boolean isSupportedDocument(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }
        
        // 方法1: 通过文件扩展名判断
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null) {
            String extension = getFileExtension(originalFilename).toLowerCase();
            if (SUPPORTED_EXTENSIONS.contains(extension)) {
                return true;
            }
        }
        
        // 方法2: 通过MIME类型判断
        try {
            String mimeType = tika.detect(file.getInputStream());
            return SUPPORTED_MIME_TYPES.contains(mimeType);
        } catch (IOException e) {
            // 如果检测失败，默认认为不是支持的文档类型
            return false;
        }
    }
    
    /**
     * 获取文件扩展名
     * 
     * @param filename 文件名
     * @return 文件扩展名，格式为".ext"
     */
    private static String getFileExtension(String filename) {
        if (filename == null || filename.lastIndexOf('.') == -1) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.')).toLowerCase();
    }
    
    /**
     * 获取支持的文档类型列表
     * 
     * @return 支持的文档类型列表
     */
    public static Set<String> getSupportedExtensions() {
        return new HashSet<>(SUPPORTED_EXTENSIONS);
    }
}