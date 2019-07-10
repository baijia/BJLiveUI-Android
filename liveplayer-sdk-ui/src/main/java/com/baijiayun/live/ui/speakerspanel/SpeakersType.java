package com.baijiayun.live.ui.speakerspanel;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 * Created by yangjingming on 2018/3/22.
 */

public enum SpeakersType {
    @Expose(serialize = true, deserialize = true)
    @SerializedName("0")
    /* 主讲人 */PPT(0),
    @SerializedName("0")
    /* 主讲人 */Presenter(1),
    @Expose(serialize = true, deserialize = true)
    @SerializedName("2")
    /* 自己视频 */Record(2),
    @Expose(serialize = true, deserialize = true)
    @SerializedName("3")
    /* 视频发言 */VideoPlay(3),
    @Expose(serialize = true, deserialize = true)
    @SerializedName("4")
    /* 未开视频发言 */Speaking(4),
    @SerializedName("5")
    /* 请求发言用户 */Applying(5);

    private int type;

    SpeakersType(int type) {
        this.type = type;
    }

    public static SpeakersType from(int type) {
        switch (type) {
            case 0:
                return PPT;
            case 1:
                return Presenter;
            case 2:
                return Record;
            case 3:
                return VideoPlay;
            case 4:
                return Speaking;
            default:
                return Applying;
        }
    }

    public int getType() {
        return type;
    }
}
