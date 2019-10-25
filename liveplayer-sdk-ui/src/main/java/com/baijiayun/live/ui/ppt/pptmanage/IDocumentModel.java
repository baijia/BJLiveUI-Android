package com.baijiayun.live.ui.ppt.pptmanage;

/**
 * Created by Shubo on 2017/5/2.
 */

interface IDocumentModel {
    String getFileName();

    String getFileExt();

    int getUploadPercent();

    int getStatus();
}
