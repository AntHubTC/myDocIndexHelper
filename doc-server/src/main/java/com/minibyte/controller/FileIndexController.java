package com.minibyte.controller;

import com.minibyte.bo.ProgressDto;
import com.minibyte.bo.pojo.app.FileDocDto;
import com.minibyte.bo.pojo.app.SqlSearchDto;
import com.minibyte.common.MBResponse;
import com.minibyte.service.FileIndexSearcherService;
import com.minibyte.service.FileIndexWriterService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.annotation.Resource;
import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("fileIndex")
public class FileIndexController {

    @Resource
    private FileIndexSearcherService fileIndexSearcherService;
    @Resource
    private FileIndexWriterService fileIndexWriterService;

    @PostMapping("search")
    @ResponseBody
    public MBResponse<List<FileDocDto>> search(@RequestBody SqlSearchDto sqlSearchDto) {
        List<FileDocDto> list = fileIndexSearcherService.search(sqlSearchDto);
        return MBResponse.ofSuccess(list);
    }

    // 构建索引数据，返回索引流水号
    @PutMapping("index")
    @ResponseBody
    public MBResponse<String> index() {
        String serial = UUID.randomUUID().toString();
        fileIndexWriterService.index(serial);
        return MBResponse.ofSuccess(serial);
    }

    // 通过索引流水号查询索引进度
    @GetMapping("index/{serial}")
    @ResponseBody
    public DeferredResult<MBResponse<ProgressDto>> indexProgress(@PathVariable("serial") String serial) {
        DeferredResult<MBResponse<ProgressDto>> deferredResult = new DeferredResult<>(-1L);
        fileIndexWriterService.indexProgressserial(serial, deferredResult);
        return deferredResult;
    }
}
