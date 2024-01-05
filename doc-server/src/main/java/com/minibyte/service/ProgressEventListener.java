package com.minibyte.service;

import com.minibyte.bo.ProgressDto;
import com.minibyte.common.MBResponse;
import lombok.Getter;
import org.springframework.web.context.request.async.DeferredResult;

/**
 * @author tangcheng_cd
 * @version 1.0
 * @className ProgressEventListener
 * @description
 * @date 2024/1/4 18:17
 **/
@Getter
public class ProgressEventListener implements IProgressEventListener {
    private String serial;
    private DeferredResult<MBResponse<ProgressDto>> deferredResult;

    public ProgressEventListener(String serial, DeferredResult<MBResponse<ProgressDto>> deferredResult) {
        this.serial = serial;
        this.deferredResult = deferredResult;
    }

    @Override
    public void progressChange(ProgressDto progressDto) {
        this.deferredResult.setResult(MBResponse.ofSuccess(progressDto));
    }

    @Override
    public boolean isTargetListener(String serial) {
        return this.serial.equals(serial);
    }
}
