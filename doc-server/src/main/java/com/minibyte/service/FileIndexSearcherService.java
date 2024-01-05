package com.minibyte.service;

import cn.hutool.core.util.StrUtil;
import com.minibyte.bo.pojo.app.Condition;
import com.minibyte.bo.pojo.app.FileDocDto;
import com.minibyte.bo.pojo.app.SqlSearchDto;
import com.minibyte.common.enums.CONFIG_ITEM;
import com.minibyte.common.enums.SQL_FILE_IDX_FILED;
import com.minibyte.common.exception.MBBizException;
import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.analysis.cn.smart.SmartChineseAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.uhighlight.DefaultPassageFormatter;
import org.apache.lucene.search.uhighlight.UnifiedHighlighter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 文件索引检索服务
 *
 * @author:
 * @date:
 * @description:
 */
@Service
@Slf4j
public class FileIndexSearcherService {

    @Resource
    private ConfigService configService;

    public void run() throws Exception {

    }

    public static void main(String[] args) {
        List<Condition> conditions = new ArrayList<>();
        Condition condition = new Condition();
        condition.setType(4);
        condition.setValue("select");
        conditions.add(condition);

        SqlSearchDto sqlSearchDto = new SqlSearchDto();
        sqlSearchDto.setConditions(conditions);
        try {
            FileIndexSearcherService fileIndexSearcherService = new FileIndexSearcherService();
            fileIndexSearcherService.search(sqlSearchDto);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public List<FileDocDto> search(SqlSearchDto sqlSearchDto) {
        int hitNum = Integer.parseInt(Optional.ofNullable(configService.getConfigValue(CONFIG_ITEM.HITS_NUM)).orElse("40"));

        List<FileDocDto> resList = new ArrayList<>();
        try {
            // 设置索引存储路径
            Directory indexDir = FSDirectory.open(new File("index-dir").toPath());

            // 创建索引读取器
            IndexReader indexReader = DirectoryReader.open(indexDir);
            IndexSearcher indexSearcher = new IndexSearcher(indexReader);
            /*
            // 单条件搜索
            QueryParser queryParser = new QueryParser("content", new StandardAnalyzer());
            Query query = queryParser.parse("hello");
            */
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            for (Condition condition : sqlSearchDto.getConditions()) {
                if (StrUtil.isBlank(condition.getValue())) {
                   continue;
                }
                QueryParser queryParser = new QueryParser(getFieldByType(condition.getType()), new SmartChineseAnalyzer());
                Query query = queryParser.parse(condition.getValue());
                builder.add(query, BooleanClause.Occur.MUST);
            }
            Query query = builder.build();

            TopDocs topDocs = indexSearcher.search(query, hitNum);
            log.info("命中目标:{}个", topDocs.totalHits);
            UnifiedHighlighter highlighter = new UnifiedHighlighter(indexSearcher, new SmartChineseAnalyzer());
            highlighter.setFormatter(new DefaultPassageFormatter());

            String[] contentHightFragments = highlighter.highlight("content", query, topDocs, 40); // 获取高亮显示结果
            for (int i = 0; i < topDocs.scoreDocs.length; i++) {
                ScoreDoc scoreDoc = topDocs.scoreDocs[i];
                Document doc = indexReader.document(scoreDoc.doc);

                FileDocDto fileDocDto = new FileDocDto();
                fileDocDto.setFileName(doc.get(SQL_FILE_IDX_FILED.fileName));
                fileDocDto.setFilePath(doc.get(SQL_FILE_IDX_FILED.filePath));
                fileDocDto.setContent(contentHightFragments[i]);
                resList.add(fileDocDto);
            }

            // 关闭索引读取器
            indexReader.close();
        } catch (IOException e) {
            log.error("IO异常", e);
            throw new MBBizException("IO异常");
        } catch (ParseException e) {
            log.error("转换异常", e);
            throw new MBBizException("转换异常");
        }
        return resList;
    }


    private String getFieldByType(Integer type) {
        switch (type) {
            case 1:
                return "not supported SQL_FILE_IDX_FILED.sqlName";
            case 2:
                return SQL_FILE_IDX_FILED.fileName;
            case 3:
                return "not supported SQL_FILE_IDX_FILED.detail";
            case 4:
                return SQL_FILE_IDX_FILED.content;
            default:
                throw new IllegalArgumentException("Invalid condition type: " + type);
        }
    }
}
