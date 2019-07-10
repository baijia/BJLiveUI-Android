package com.baijiayun.live.ui.utils;

import com.baijiayun.livecore.models.LPDataModel;

import java.io.Serializable;

/**
 * Created by Shubo on 2016/10/24.
 */

public abstract class LPShareModel extends LPDataModel implements Serializable{

    public abstract String getShareIconText();

    public abstract int getShareIconRes();

    public abstract boolean hasCorner();

    public abstract String getCornerText();

    public abstract int getShareType();
}
