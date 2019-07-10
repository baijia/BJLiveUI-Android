package com.baijiayun.live.ui.utils;

import android.support.annotation.Nullable;

/**
 * Created by Shubo on 2017/2/13.
 */

public class Precondition {

    public static <T> T checkNotNull(T reference) {
        if (reference == null) {
            throw new NullPointerException();
        }
        return reference;
    }

    public static <T> T checkNotNull(T reference, @Nullable Object errorMessage) {
        if (reference == null) {
            throw new NullPointerException(String.valueOf(errorMessage));
        }
        return reference;
    }
}
