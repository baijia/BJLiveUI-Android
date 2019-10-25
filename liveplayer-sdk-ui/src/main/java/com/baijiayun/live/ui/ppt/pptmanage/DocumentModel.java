package com.baijiayun.live.ui.ppt.pptmanage;

import com.baijiayun.livecore.models.LPDocumentModel;

/**
 * Created by Shubo on 2017/5/2.
 */

class DocumentModel extends LPDocumentModel implements IDocumentModel {

    DocumentModel(LPDocumentModel lpDocumentModel) {
        this.id = lpDocumentModel.id;
        this.ext = lpDocumentModel.ext;
        this.number = lpDocumentModel.number;
        this.name = lpDocumentModel.name;
        this.pageInfoModel = lpDocumentModel.pageInfoModel;
    }

    @Override
    public String getFileName() {
        return name;
    }

    @Override
    public String getFileExt() {
        return ext;
    }

    @Override
    public int getUploadPercent() {
        return 0;
    }

    @Override
    public int getStatus() {
        return 0;
    }

}
