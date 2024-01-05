package com.minibyte.service;

import cn.hutool.core.collection.CollUtil;
import com.minibyte.bo.ProgressDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 进度管理器
 **/
@Slf4j
@Component
public class ProgressManager implements InitializingBean {
    public Map<String, Queue<ProgressDto>> progressMap;
    public List<IProgressEventListener> listeners;

    @Override
    public void afterPropertiesSet() {
        this.listeners = new CopyOnWriteArrayList<>();
        this.progressMap = new ConcurrentHashMap<>();
    }

    public void ing(String serial, int progress, String descr) {
        ProgressDto progressDto = new ProgressDto();
        progressDto.setSerial(serial);
        progressDto.setProgress(progress);
        progressDto.setStatus(1);
        progressDto.setDescr(descr);


        addProgress(serial, progressDto);
    }

    public void err(String serial, String message) {
        ProgressDto progressDto = new ProgressDto();
        progressDto.setSerial(serial);
        progressDto.setProgress(null);
        progressDto.setStatus(-1);
        progressDto.setDescr(message);

        addProgress(serial, progressDto);
    }

    public void succ(String serial) {
        ProgressDto progressDto = new ProgressDto();
        progressDto.setSerial(serial);
        progressDto.setProgress(100);
        progressDto.setStatus(2);
        progressDto.setDescr("ok");

        addProgress(serial, progressDto);
    }

    public void removeProgress(String serial) {
        progressMap.remove(serial);
    }

    // 新增进度
    private synchronized void addProgress(String serial, ProgressDto progressDto) {
        Queue<ProgressDto> progressDtos = progressMap.get(serial);
        if (CollUtil.isEmpty(progressDtos)) {
            progressDtos = new LinkedList<>();
            progressMap.put(serial, progressDtos);
        }
        while (progressDtos.size() >= 3) {
            progressDtos.remove();
        }
        progressDtos.offer(progressDto);

        notifyListener(serial);
    }

    // 通知监听者
    private void notifyListener(String serial) {
        List<IProgressEventListener> removeListeners = new ArrayList<>();
        for (IProgressEventListener listener : this.listeners) {
            if (listener.isTargetListener(serial)) {
                if (consumerProgress(listener)) {
                    removeListeners.add(listener);
                }
            }
        }
        // 每个的serial监听者只用一次，只要通知后就移除掉
        removeListeners.forEach(this.listeners::remove);
    }

    // 键入监听者
    public void addListener(IProgressEventListener progressEventListener) {
        // 如果监听着刚加入进度就有消息产生了，那么直接通知取进度信息
        if (consumerProgress(progressEventListener)) {
            return;
        }
        // 如果无消息消费，加入监听队列
        this.listeners.add(progressEventListener);
    }

    // 消费进度信息
    private boolean consumerProgress(IProgressEventListener progressEventListener) {
        Queue<ProgressDto> progressDtos = progressMap.get(progressEventListener.getSerial());
        if (CollUtil.isNotEmpty(progressDtos)) {
            // 进度就完毕了，那么直接通知取进度信息
            ProgressDto headProgress = progressDtos.element();
            if (null != headProgress && headProgress.isSuccess()) {
                // 通知
                progressEventListener.progressChange(headProgress);
                // 移除进度信息，防止泄露
                removeProgress(progressEventListener.getSerial());
            } else {
                ProgressDto progressDto = progressDtos.poll();
                progressEventListener.progressChange(progressDto);
            }
            return true;
        }
        return false;
    }
}
