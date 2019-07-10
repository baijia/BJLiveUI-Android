package com.baijiayun.live.ui.utils;

import android.content.ContentResolver;
import android.database.ContentObserver;
import android.os.Handler;
import android.provider.Settings;

/**
 * Created by wangkangfei on 17/5/6.
 */

public class RotationObserver extends ContentObserver {
    private ContentResolver contentResolver;
    private OnRotationSettingChangedListener listener;

    public RotationObserver(Handler handler, ContentResolver resolver) {
        super(handler);
        this.contentResolver = resolver;
    }

    public void setOnRotationSettingChangedListener(OnRotationSettingChangedListener listener) {
        this.listener = listener;
    }

    @Override
    public void onChange(boolean selfChange) {
        super.onChange(selfChange);
        //注：这里只会监听是否变化了，不能拿到具体的值，还得让上层主动获取
        if (listener != null) {
            listener.onRotationSettingChanged();
        }
    }

    //开启监听
    public void startObserver() {
        contentResolver.registerContentObserver(Settings.System.getUriFor(Settings.System.ACCELEROMETER_ROTATION), true, this);
    }

    //关闭监听
    public void stopObserver() {
        contentResolver.unregisterContentObserver(this);
    }

    public interface OnRotationSettingChangedListener {
        void onRotationSettingChanged();
    }
}
