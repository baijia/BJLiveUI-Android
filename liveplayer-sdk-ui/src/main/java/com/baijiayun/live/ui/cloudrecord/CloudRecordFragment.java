package com.baijiayun.live.ui.cloudrecord;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseFragment;

/**
 * Created by wangkangfei on 17/5/3.
 */

public class CloudRecordFragment extends BaseFragment implements CloudRecordContract.View {

    private CloudRecordContract.Presenter presenter;

    @Override
    public int getLayoutId() {
        return R.layout.bjy_fragment_right_top_menu;
    }

    @Override
    public void setPresenter(CloudRecordContract.Presenter presenter) {
        super.setBasePresenter(presenter);
        this.presenter = presenter;
    }

    @Override
    protected void init(Bundle savedInstanceState) {
        super.init(savedInstanceState);
        $.contentView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!presenter.canOperateCloudRecord()) {
                    showToast(getString(R.string.live_room_cloud_record_permission_forbid));
                    return;
                }
                new MaterialDialog.Builder(getActivity())
                        .title(getString(R.string.live_cloud_record_setting))
                        .content(getString(R.string.live_cloud_record_setting_content))
                        .positiveColor(ContextCompat.getColor(getContext(), R.color.live_red))
                        .positiveText(getString(R.string.live_cloud_record_setting_end))
                        .negativeColor(ContextCompat.getColor(getContext(), R.color.live_text_color))
                        .negativeText(getString(R.string.live_cancel))
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                                presenter.cancelCloudRecord();
                            }
                        })
                        .onNegative(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                                materialDialog.dismiss();
                            }
                        })
                        .build()
                        .show();
            }
        });
    }
}
