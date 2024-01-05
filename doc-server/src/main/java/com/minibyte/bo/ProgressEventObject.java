package com.minibyte.bo;

import java.util.EventObject;

/**
 * @author tangcheng_cd
 * @version 1.0
 * @className ProgressEventObject
 * @description
 * @date 2024/1/4 17:31
 **/
public class ProgressEventObject extends EventObject {
    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public ProgressEventObject(Object source) {
        super(source);
    }
}
