package com.baijiayun.live.ui.topbar;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.activity.LiveRoomActivity;
import com.baijiayun.live.ui.base.BaseFragment;

/**
 * Created by Shubo on 2017/2/13.
 */

public class TopBarFragment extends BaseFragment implements TopBarContract.View {

    private TopBarContract.Presenter presenter;

    @Override
    public int getLayoutId() {
        return R.layout.bjy_fragment_topbar;
    }

    @Override
    public void init(Bundle savedInstanceState) {
        $.id(R.id.fragment_top_bar_back).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (LiveRoomActivity.getExitListener() != null) {
                    getActivity().finish();
                    return;
                }
                new MaterialDialog.Builder(getActivity())
                        .title(getString(R.string.live_exit_hint_title))
                        .content(getString(R.string.live_exit_hint_content))
                        .contentColor(ContextCompat.getColor(getContext(), R.color.live_text_color_light))
                        .positiveColor(ContextCompat.getColor(getContext(), R.color.live_blue))
                        .positiveText(getString(R.string.live_exit_hint_confirm))
                        .negativeColor(ContextCompat.getColor(getContext(), R.color.live_text_color))
                        .negativeText(getString(R.string.live_cancel))
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                                if (getActivity() != null)
                                    getActivity().finish();
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
        $.id(R.id.fragment_top_bar_share).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.navigateToShare();
            }
        });

    }

    @Override
    public void setPresenter(TopBarContract.Presenter presenter) {
        super.setBasePresenter(presenter);
        this.presenter = presenter;
    }

    @Override
    public void showHideShare(boolean show) {
        if (show) $.id(R.id.fragment_top_bar_share).visible();
        else $.id(R.id.fragment_top_bar_share).gone();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        $ = null;
        presenter = null;
    }
}
