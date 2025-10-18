package alibaba.datafilter.utils;

import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentTransformer;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 一个基于字符的文本分割器，它会尝试按顺序使用一系列分隔符进行分割。
 * 类似于 LangChain 的 RecursiveCharacterTextSplitter。
 */
public class CharacterTextSplitter implements DocumentTransformer {

    private final int chunkSize;
    private final int chunkOverlap;
    private final List<String> separators;

    /**
     * @param chunkSize    每个分块的最大字符数
     * @param chunkOverlap 分块之间的重叠字符数
     * @param separators   用于分割文本的分隔符列表，按优先级排序（例如：["\n\n", "\n", "。", " "])
     */
    public CharacterTextSplitter(int chunkSize, int chunkOverlap, List<String> separators) {
        Assert.isTrue(chunkSize > 0, "Chunk size must be greater than 0");
        Assert.isTrue(chunkOverlap >= 0, "Chunk overlap must be greater than or equal to 0");
        Assert.isTrue(chunkSize > chunkOverlap, "Chunk size must be greater than chunk overlap");
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.separators = separators;
    }

    @Override
    public List<Document> apply(List<Document> documents) {
        return documents.stream()
                .flatMap(document -> splitDocument(document).stream())
                .collect(Collectors.toList());
    }

    private List<Document> splitDocument(Document document) {
        List<String> chunks = splitText(document.getText());
        List<Document> splitDocs = new ArrayList<>();
        for (String chunk : chunks) {
            // 创建新的Document，并继承原始的元数据
            Document newDoc = new Document(chunk, document.getMetadata());
            splitDocs.add(newDoc);
        }
        return splitDocs;
    }

    public List<String> splitText(String text) {
        // 首先尝试使用第一个分隔符进行分割
        String separator = separators.get(0);
        List<String> splits;
        if (separator.isEmpty()) {
            // 如果分隔符是空字符串，按字符分割
            splits = new ArrayList<>();
            for (int i = 0; i < text.length(); i++) {
                splits.add(String.valueOf(text.charAt(i)));
            }
        } else {
            splits = List.of(text.split(separator));
        }

        // 用于存储当前正在合并的块
        List<String> goodSplits = new ArrayList<>();
        StringBuilder currentChunk = new StringBuilder();

        for (String s : splits) {
            // 如果当前块加上新的片段（和分隔符）没有超过大小限制
            if (currentChunk.length() + s.length() + separator.length() <= chunkSize) {
                currentChunk.append(s).append(separator);
            } else {
                // 如果超过了，将当前块存起来
                if (!currentChunk.isEmpty()) {
                    goodSplits.add(currentChunk.toString().trim());
                }
                // 如果单个片段本身就超过了大小，需要进一步递归分割
                if (s.length() > chunkSize) {
                    List<String> subChunks = recursivelySplit(s, 1); // 使用下一个分隔符
                    goodSplits.addAll(subChunks);
                    currentChunk = new StringBuilder(); // 递归分割后重置当前块
                } else {
                    currentChunk = new StringBuilder(s + separator);
                }
            }
        }
        // 添加最后一个块
        if (!currentChunk.isEmpty()) {
            goodSplits.add(currentChunk.toString().trim());
        }

        // 合并小的分块并处理重叠
        return mergeSplits(goodSplits);
    }

    // 递归分割过长的片段
    private List<String> recursivelySplit(String text, int separatorIndex) {
        if (separatorIndex >= separators.size()) {
            // 如果所有分隔符都用完了，但文本仍然太长，就硬切
            List<String> chunks = new ArrayList<>();
            for (int i = 0; i < text.length(); i += chunkSize) {
                chunks.add(text.substring(i, Math.min(text.length(), i + chunkSize)));
            }
            return chunks;
        }

        CharacterTextSplitter subSplitter = new CharacterTextSplitter(chunkSize, chunkOverlap, separators.subList(separatorIndex, separators.size()));
        return subSplitter.splitText(text);
    }
    
    // 合并分块并处理重叠
    private List<String> mergeSplits(List<String> splits) {
        List<String> mergedChunks = new ArrayList<>();
        String currentChunk = "";
        for (String split : splits) {
            if (currentChunk.length() + split.length() <= chunkSize) {
                currentChunk += split;
            } else {
                mergedChunks.add(currentChunk);
                // 创建重叠部分
                int overlapStart = Math.max(0, currentChunk.length() - chunkOverlap);
                String overlap = currentChunk.substring(overlapStart);
                currentChunk = overlap + split;
            }
        }
        if (!currentChunk.isEmpty()) {
            mergedChunks.add(currentChunk);
        }
        return mergedChunks;
    }
}