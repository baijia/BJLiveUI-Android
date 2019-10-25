package com.baijiayun.live.ui.ppt.pptmanage;

import com.baijiayun.livecore.models.LPUploadDocModel;

import java.io.File;

/**
 * Created by Shubo on 2017/5/2.
 */

class DocumentUploadingModel implements IDocumentModel {
    static final int INITIAL = 0;
    static final int UPLOADING = 1;
    static final int UPLOADED = 2;
    static final int WAIT_SIGNAL = 3;
    static final int UPLOAD_FAIL = 4;

    DocumentUploadingModel(String imgPath) {
        this.imgPath = imgPath;
        File file = new File(imgPath);
        fileName = file.getName();
        if (fileName.contains("."))
            fileExt = fileName.substring(fileName.lastIndexOf("."));
        status = INITIAL;
    }

    String imgPath;

    String fileName;

    String fileExt;

    LPUploadDocModel uploadModel;

    int status;

    int uploadPercentage;

    @Override
    public String getFileName() {
        return fileName;
    }

    @Override
    public String getFileExt() {
        return fileExt;
    }

    @Override
    public int getUploadPercent() {
        return 0;
    }

    @Override
    public int getStatus() {
        return status;
    }
}
