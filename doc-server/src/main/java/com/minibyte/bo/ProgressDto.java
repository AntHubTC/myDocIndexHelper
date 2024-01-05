package com.minibyte.bo;

import lombok.Data;

import java.util.Objects;

/**
 * @author tangcheng_cd
 * @version 1.0
 * @className ProgressDto
 * @description
 * @date 2024/1/4 17:56
 **/
@Data
public class ProgressDto {
    /**
     * 流水号
     */
    private String serial;
    /**
     * 状态 1进行中 -1失败 2完成
     */
    private Integer status;
    /**
     * 进度数字
     */
    private Integer progress;
    /**
     * 进度描述信息
     */
    private String descr;

    public boolean isSuccess() {
        return Objects.equals(2, status);
    }
}
