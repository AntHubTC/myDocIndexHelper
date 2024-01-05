package com.minibyte.service;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import com.minibyte.bo.ProgressDto;
import com.minibyte.bo.pojo.app.UpdateDocDto;
import com.minibyte.common.MBResponse;
import com.minibyte.common.enums.SQL_FILE_IDX_FILED;
import com.minibyte.common.exception.MBBizException;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.*;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Resource;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 文件索引服务
 *
 *  https://blog.csdn.net/weixin_42633131/article/details/82873731/
 *
 * @author: tangcheng_cd
 * @date: 2023/4/14
 * @description:
 */
@Slf4j
@Service
public class FileIndexWriterService {
    public static final String INDEX_DIR = "index-dir";

    @Resource
    private ProgressManager progressManager;

    public static void main(String[] args) throws Exception {
        long start = System.currentTimeMillis();
        new FileIndexWriterService().run(null);
//        new FileIndexWriterService().list();
        long end = System.currentTimeMillis();
        System.out.println(end-start);
    }

    @Async
    public void index(String serial) {
        progressManager.ing(serial, 0, "开始索引中....");
        try {
            run(serial);
        } catch (Exception e) {
            progressManager.err(serial, e.getMessage());
        } finally {
            progressManager.succ(serial);
        }
    }

    public void list() throws Exception {
        // 索引目录对象
        Directory directory = FSDirectory.open(new File("index-dir").toPath());
        // 索引读取工具
        IndexReader reader = DirectoryReader.open(directory);
        for (int i = 0; i < reader.maxDoc(); i++) {
            Document document = reader.document(i);
            System.out.println(" fileName:" + document.get(SQL_FILE_IDX_FILED.fileName));
            System.out.println(" filePath:" + document.get(SQL_FILE_IDX_FILED.filePath));
            System.out.println(" content:" + document.get(SQL_FILE_IDX_FILED.content));
            System.out.println();
        }
    }

    public void run (String serial) throws Exception{
        progressManager.ing(serial, 0, "查找要检索的文档....");
        List<File> files = FileUtil.loopFiles("D:\\githubRepository\\AntHubTC.github.io", pathname -> pathname.getName().endsWith(".md") );
        if (CollUtil.isEmpty(files)) {
            return;
        }

        progressManager.ing(serial, 1, "准备开始索引....");
        File indexDirFile = new File(INDEX_DIR);
        ensureExistIndexLab(indexDirFile);

        // 设置索引存储路径
        try (Directory indexDir = FSDirectory.open(indexDirFile.toPath())) {
            // 创建索引配置对象，使用标准分词器
            // IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
            IndexWriterConfig config = new IndexWriterConfig(new SmartChineseAnalyzer());
            // 设置是否清空索引库中的数据
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
            // 创建索引写入器
            IndexWriter indexWriter = new IndexWriter(indexDir, config);

            // 创建索引读取器
            IndexReader indexReader = DirectoryReader.open(indexDir);

            // 读取本地文件，并将内容创建成Document对象
            Collection<Document> addDocs = new ArrayList<>();
            Collection<UpdateDocDto> updateDocs = new ArrayList<>();

            for (int i = 0; i < files.size(); i++) {
                File file = files.get(i);

                int progress = (int)(((float)(i + 1) / files.size()) * 100);
                progressManager.ing(serial, progress, "准备索引文件:" + file.getAbsolutePath());

                if (checkFile(file)) {
                    collectFileDocs(file, indexReader, addDocs, updateDocs);
                }
            }
            progressManager.ing(serial, 100, "正在将索引添加到索引库中");
            // 将Document对象添加到索引中
            if (CollUtil.isNotEmpty(addDocs)) {
                indexWriter.addDocuments(addDocs);
            }
            for (UpdateDocDto updateDoc : updateDocs) {
                indexWriter.updateDocument(updateDoc.getTerm(), updateDoc.getDocument());
            }

            if (CollUtil.isNotEmpty(addDocs) || CollUtil.isNotEmpty(updateDocs)) {
                // 提交
                indexWriter.commit();
            }
            // 关闭索引写入器
            indexWriter.close();
            // 关闭索引读取器
            indexReader.close();
        }
    }

    /**
     * 确保索引库存在
     * @param indexDirFile
     * @throws Exception
     */
    private void ensureExistIndexLab(File indexDirFile) throws Exception {
        if (ArrayUtil.isNotEmpty(indexDirFile.list())) {
            return;
        }
        try (Directory indexDir = FSDirectory.open(indexDirFile.toPath())) {
            IndexWriterConfig config = new IndexWriterConfig(new SmartChineseAnalyzer());
            // 设置是否清空索引库中的数据
            config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            // 创建索引写入器
            IndexWriter indexWriter = new IndexWriter(indexDir, config);
            // 关闭索引写入器
            indexWriter.close();
        }
    }

    private boolean checkFile(File file) {
        if (file.length() > 8 * 1024 * 1024 * 200) {
            log.warn("文件超过200M，不能进行索引! 文件：{}", file.toPath());
            return false;
        }
        return true;
    }

    private static final String NAME_PREFIX = "-- @name";
    private static final String DETAIL_PREFIX = "-- @detail";
    private void collectFileDocs(File file, IndexReader indexReader, Collection<Document> addDocs, Collection<UpdateDocDto> updateDocs) throws Exception {
        log.info("正在索引文件:{}", file.toString());
        List<String> lines = FileUtil.readLines(file, StandardCharsets.UTF_8);

        String fileSizeHash = getFileHash(Collections.singletonList(file.getAbsolutePath() + file.length()));
        log.debug("fileHash:{}", fileSizeHash);
        String fileContentHash = getFileHash(lines);
        log.debug("fileHash2:{}", fileContentHash);

        // 创建文档对象
        // String content = CollUtil.join(lines, "\r\n");
        // Document document1 = new Document();
        // document1.add(new TextField(SQL_FILE_IDX_FILED.fileName, file.getName(), Field.Store.YES));
        // document1.add(new StringField(SQL_FILE_IDX_FILED.filePath, file.getPath(), Field.Store.YES));
        // document1.add(new StringField(SQL_FILE_IDX_FILED.fileSizeHash, fileSizeHash, Field.Store.YES));
        // document1.add(new StringField(SQL_FILE_IDX_FILED.fileContentHash, fileContentHash, Field.Store.YES));
        // document1.add(new TextField(SQL_FILE_IDX_FILED.content, content, Field.Store.YES));

        for (int i = 0; i < lines.size(); i++) {
            // 因为索引上下多个关联行，所以跳过几行防止重复索引
//            String idxContent = getIdxContent(i, lines);
            String idxContent = "";
            {
                int indexRow = 7;
                if (lines.size() <= indexRow) {
                    idxContent = CollUtil.join(lines, "\r\n");
                    i = lines.size();
                } else if (i + indexRow >= lines.size()) {
                    idxContent = CollUtil.join(CollUtil.sub(lines, i, lines.size()), "\r\n");
                    i = lines.size();
                } else {
                    idxContent = CollUtil.join(CollUtil.sub(lines, i, i + indexRow), "\r\n");
                    i += indexRow;
                }
            }

            Document document1 = new Document();
            document1.add(new TextField(SQL_FILE_IDX_FILED.fileName, file.getName(), Field.Store.YES));
            document1.add(new StringField(SQL_FILE_IDX_FILED.filePath, file.getPath(), Field.Store.YES));
            document1.add(new StringField(SQL_FILE_IDX_FILED.fileSizeHash, fileSizeHash, Field.Store.YES));
            document1.add(new StringField(SQL_FILE_IDX_FILED.fileContentHash, fileContentHash, Field.Store.YES));
            document1.add(new TextField(SQL_FILE_IDX_FILED.content, idxContent, Field.Store.YES));

            // 通过文件路径搜索，查看索引中是否存在了这个文件；
            // https://blog.csdn.net/yelllowcong/article/details/78698506
//        QueryParser queryParser = new QueryParser(SQL_FILE_IDX_FILED.filePath, new SimpleAnalyzer());
//        Query query = queryParser.parse(file.getPath());
//        IndexSearcher indexSearcher = new IndexSearcher(indexReader);
            Query query = new TermQuery(new Term(SQL_FILE_IDX_FILED.filePath, file.getPath()));
            IndexSearcher indexSearcher = new IndexSearcher(indexReader);
            TopDocs topDocs = indexSearcher.search(query, 1);
            boolean existFile = topDocs.totalHits.value > 0;

            if (existFile) {
                int docId = topDocs.scoreDocs[0].doc;
                Document doc = indexReader.document(docId);
                String oldFileSizeHash = doc.get(SQL_FILE_IDX_FILED.fileSizeHash);
                String oldFileContentHash = doc.get(SQL_FILE_IDX_FILED.fileContentHash);
                // 只要有一个不匹配就要进行更新
                if (!fileSizeHash.equals(oldFileSizeHash) || !fileContentHash.equals(oldFileContentHash)) {
                    updateDocs.add(new UpdateDocDto(new Term(SQL_FILE_IDX_FILED.filePath, file.getPath()), document1));
                }
            } else {
                addDocs.add(document1);
            }
        }
    }

    private String getIdxContent(int i, List<String> lines) {
        int indexLineCount = 7;
        int indexHalfCount = indexLineCount / 2;
        if (lines.size() < indexLineCount) {
            // 文档小于7行
            return CollUtil.join(lines, "\r\n");
        }
        // 索引出上3行到下3行信息，总共7行
        int startCur;
        if (i < indexHalfCount) {
            // 文档开头
            startCur = 0;
        } else if (i + indexHalfCount > lines.size() - 1) {
            // 文档结尾
            startCur = lines.size() -1 -indexLineCount;
        } else {
            startCur = i - indexHalfCount;
        }

        return CollUtil.join(CollUtil.sub(lines, startCur, startCur + 7), "\r\n");
    }

    private String getFileHash(List<String> lines) {
        StringBuilder hashBuilder = new StringBuilder();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            for (String line : lines) {
                md.update(line.getBytes(StandardCharsets.UTF_8));
            }
            for (byte b : md.digest()) {
                hashBuilder.append(String.format("%02x", b));
            }
        } catch (NoSuchAlgorithmException e) {
            log.error("系统异常", e);
            throw new MBBizException("无此算法类型");
        }
        return hashBuilder.toString();
    }

    public String getFileHash(File file) {
        StringBuilder hashBuilder = new StringBuilder();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            try (FileInputStream fis = new FileInputStream(file)) {
                FileChannel ch = fis.getChannel();
                MappedByteBuffer buffer = ch.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
                md.update(buffer);
            }
            for (byte b : md.digest()) {
                hashBuilder.append(String.format("%02x", b));
            }
        } catch (NoSuchAlgorithmException e) {
            log.error("系统异常", e);
            throw new MBBizException("无此算法类型");
        } catch (IOException e) {
            log.error("系统异常", e);
            throw new MBBizException("IO Exception");
        }
        return hashBuilder.toString();
    }

    @Async
    public void indexProgressserial(String serial, DeferredResult<MBResponse<ProgressDto>> deferredResult) {
        progressManager.addListener(new ProgressEventListener(serial, deferredResult));
    }
}
