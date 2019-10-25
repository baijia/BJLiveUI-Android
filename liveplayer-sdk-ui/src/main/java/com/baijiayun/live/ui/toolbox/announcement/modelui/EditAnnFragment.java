package com.baijiayun.live.ui.toolbox.announcement.modelui;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseFragment;
import com.baijiayun.livecore.models.imodels.IAnnouncementModel;

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

        String info = "";
        String link = "";
        if (iAnnModel.getSGroup() != null) {
            info = iAnnModel.getSGroup().content;
            link = iAnnModel.getSGroup().link;
        } else {
            info = iAnnModel.getContent();
            link = iAnnModel.getLink();
        }

        if (info == null)
            info = "";
        if (link == null)
            link = null;

        $.id(R.id.et_announcement_edit_info).text(info);
        $.id(R.id.et_announcement_edit_url).text(link);
    }

    public void setOnAnnEditListener(EditAnnContract.OnAnnEditListener listener) {
        this.mOnAnnEditListener = listener;
    }

    @Override
    public void setTitle(int titleType) {
        if (titleType == 1) {
            $.id(R.id.tv_announcement_edit_title).text(getResources().getString(R.string.live_announcement));
        } else {
            $.id(R.id.tv_announcement_edit_title).text(getResources().getString(R.string.string_notice_group_title));
        }
    }

    @Override
    public NoticeInfo getNoticeInfo() {

        NoticeInfo info = new NoticeInfo();


        info.content = ((EditText)$.id(R.id.et_announcement_edit_info).view()).getText().toString();
        info.link = ((EditText)$.id(R.id.et_announcement_edit_url).view()).getText().toString();

        return info;
    }
}
