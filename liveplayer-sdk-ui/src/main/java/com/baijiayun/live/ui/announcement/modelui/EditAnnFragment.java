package com.baijiayun.live.ui.announcement.modelui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseFragment;
import com.baijiayun.livecore.models.imodels.IAnnouncementModel;
import com.baijiayun.livecore.utils.LPLogger;

/**
 * 修改公告/通知
 * panzq
 * 20190708
 */
public class EditAnnFragment extends BaseFragment implements EditAnnContract.View {

    private EditAnnContract.Presenter mPresenter;

    private EditAnnContract.OnAnnEditListener mOnAnnEditListener;
    private boolean islMaxCount = false;

    @Override
    public int getLayoutId() {
        return R.layout.fragment_announcement_edit;
    }

    @Override
    public void setPresenter(EditAnnContract.Presenter presenter) {
        setBasePresenter(presenter);
        this.mPresenter = presenter;
    }

    @Override
    protected void init(Bundle savedInstanceState) {
        super.init(savedInstanceState);


        $.id(R.id.tv_announcement_edit_cancel).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //取消编辑
                if (mOnAnnEditListener == null)
                    return;
                mOnAnnEditListener.cannel();
            }
        });

        ((EditText)$.id(R.id.et_announcement_edit_info).view()).addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                int detailLength = editable.length();
                $.id(R.id.tv_announcement_edit_info_count).text(detailLength + "/140");
            }
        });
    }

    @Override
    public void initInfo(IAnnouncementModel iAnnModel) {
        if (iAnnModel == null)
            return;

        if (iAnnModel.getSGroup() != null) {
            $.id(R.id.et_announcement_edit_info).text("" + iAnnModel.getSGroup().content);
            $.id(R.id.et_announcement_edit_url).text("" + iAnnModel.getSGroup().link);
        } else {
            $.id(R.id.et_announcement_edit_info).text("" + iAnnModel.getContent());
            $.id(R.id.et_announcement_edit_url).text("" + iAnnModel.getLink());
        }
    }

    public void setOnAnnEditListener(EditAnnContract.OnAnnEditListener listener) {
        this.mOnAnnEditListener = listener;
    }

    @Override
    public NoticeInfo getNoticeInfo() {

        NoticeInfo info = new NoticeInfo();

        info.content = ((EditText)$.id(R.id.et_announcement_edit_info).view()).getText().toString();
        info.link = ((EditText)$.id(R.id.et_announcement_edit_url).view()).getText().toString();

        return info;
    }
}
