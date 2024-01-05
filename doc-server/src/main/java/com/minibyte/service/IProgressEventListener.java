package com.minibyte.service;

import com.minibyte.bo.ProgressDto;

import java.util.EventListener;

/**
 * 进度监听者
 **/
public interface IProgressEventListener extends EventListener {
    void progressChange(ProgressDto progressDto);

    boolean isTargetListener(String serial);

    String getSerial();
}
