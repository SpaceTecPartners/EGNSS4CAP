package com.ezartech.ezar.videooverlay;

import java.lang.reflect.Method;

/**
 * Created by Zirk on 3/7/2017.
 */

public class FrameBufferHolder {
    static Method method;

    private byte[] data;
    private Object bufferHolder; //android camera

    static {
        try {
            Class cls = Class.forName("android.hardware.Camera");
            Class[] parameterTypes = new Class[]{byte[].class};
            method = cls.getDeclaredMethod("addCallbackBuffer", parameterTypes);
        } catch (Exception ex) {
            //do nothing
        }
    }

    public FrameBufferHolder(byte[] data, Object holder) {
        this.data = data;
        this.bufferHolder = holder;
    }

    public byte[] getFrame() {
        return data;
    }

    public void free() {
        try {
            method.invoke(bufferHolder,data);
        } catch(Exception ex) {
            //do nothing
        }
    }
}
