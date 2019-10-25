package com.baijiayun.live.ui.toolbox.announcement.modelui;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseFragment;

/**
 * 公告：空白提示
 * panzq
 * 20190708
 */
public class BlankTipsFragment extends BaseFragment implements BlankTipsContract.View{

    private BlankTipsContract.Presenter mPresenter;

    @Override
    public int getLayoutId() {
        return R.layout.fragment_announcement_blank_tips;
    }


    @Override
    public void setPresenter(BlankTipsContract.Presenter presenter) {
        this.mPresenter = presenter;
    }
}
