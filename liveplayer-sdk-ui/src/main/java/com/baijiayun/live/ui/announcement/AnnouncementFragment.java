package com.baijiayun.live.ui.announcement;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.announcement.modelui.BlankTipsFragment;
import com.baijiayun.live.ui.announcement.modelui.BlankTipsPresenter;
import com.baijiayun.live.ui.announcement.modelui.DoubleAnnContract;
import com.baijiayun.live.ui.announcement.modelui.DoubleAnnFramgent;
import com.baijiayun.live.ui.announcement.modelui.DoubleAnnPresenter;
import com.baijiayun.live.ui.announcement.modelui.EditAnnContract;
import com.baijiayun.live.ui.announcement.modelui.EditAnnFragment;
import com.baijiayun.live.ui.announcement.modelui.EditAnnPresenter;
import com.baijiayun.live.ui.announcement.modelui.IAnnouncementUI;
import com.baijiayun.live.ui.announcement.modelui.NoticeInfo;
import com.baijiayun.live.ui.base.BaseDialogFragment;
import com.baijiayun.live.ui.base.BaseFragment;
import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;
import com.baijiayun.live.ui.utils.QueryPlus;
import com.baijiayun.livecore.models.imodels.IAnnouncementModel;

import java.util.function.DoublePredicate;

/**
 * Created by Shubo on 2017/4/19.
 */

public class AnnouncementFragment extends BaseDialogFragment implements AnnouncementContract.View {

    private AnnouncementContract.Presenter mPresenter;
    private QueryPlus $;
    private boolean isTeacherView = true;
    private TextWatcher textWatcher;

    private BaseFragment mCurrFragment;
    private BasePresenter mCurrPresenter;

    private IAnnouncementModel mIAnnModel;

    public static AnnouncementFragment newInstance() {

        Bundle args = new Bundle();

        AnnouncementFragment fragment = new AnnouncementFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void setPresenter(AnnouncementContract.Presenter presenter) {
        super.setBasePresenter(presenter);
        this.mPresenter = presenter;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_announcement;
    }

    @Override
    protected void init(Bundle savedInstanceState, Bundle arguments) {
        super.title(getString(R.string.live_announcement)).editText("");
        hideBackground();
        $ = QueryPlus.with(contentView);

        //初始化默认UI
        mCurrFragment = new BlankTipsFragment();
        bindVP((BlankTipsFragment)mCurrFragment, new BlankTipsPresenter());
        showFragment(mCurrFragment);

        $.id(R.id.tv_announcement_edit_button).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (mCurrFragment instanceof EditAnnFragment) {
                    //当前为编辑页面
                    NoticeInfo info = ((IAnnouncementUI)mCurrPresenter).getNotice();
                    if (info == null)
                        return;

                    if (!TextUtils.isEmpty(info.link) && TextUtils.isEmpty(info.content))
                        return;

                    if (info.link == null)
                        info.link = "";
                    if (info.content == null)
                        info.content = "";

                    mPresenter.saveAnnouncement(info.content, info.link);
                    mPresenter.switchUI();
                } else{
                    //跳转到编辑页面
                    if (mCurrFragment != null)
                        removeFragment(mCurrFragment);
                    mCurrFragment = null;
                    mCurrPresenter = null;

                    mCurrFragment = new EditAnnFragment();
                    ((EditAnnFragment) mCurrFragment).setOnAnnEditListener(mAnnEditListener);
                    mCurrPresenter = new EditAnnPresenter((EditAnnFragment)mCurrFragment, mPresenter.isGrouping(), mIAnnModel);
                    bindVP((EditAnnFragment)mCurrFragment, mCurrPresenter);
                    showFragment(mCurrFragment);

                    $.id(R.id.tv_announcement_edit_button).text(getResources().getString(R.string.string_notice_save_send));
                }
            }
        });
    }

    private void showFragment(Fragment fragment) {

        FragmentTransaction transaction = this.getChildFragmentManager().beginTransaction();
        transaction.add(R.id.fl_announcement, fragment).commit();
    }

    protected void removeFragment(Fragment fragment) {
        if (fragment == null) return;
        FragmentTransaction transaction = this.getChildFragmentManager().beginTransaction();
        transaction.remove(fragment);
        transaction.commitAllowingStateLoss();
    }

    private <V extends BaseView, P extends BasePresenter> void bindVP(V view, P presenter) {
        presenter.setRouter(mPresenter.getRouter());
        view.setPresenter(presenter);
    }

    @Override
    public void editButtonEnable(boolean enable, @StringRes int stringRes) {
        if (enable) {
            //显示
            $.id(R.id.tv_announcement_edit_button).visibility(View.VISIBLE);
            $.id(R.id.tv_announcement_edit_button).text(getResources().getString(stringRes));
        } else {
            //隐藏
            $.id(R.id.tv_announcement_edit_button).visibility(View.GONE);
        }
    }

    @Override
    public void showBlankTips() {

        removeCurrFragment();
        mIAnnModel = null;

        //初始化默认UI
        mCurrFragment = new BlankTipsFragment();
        mCurrPresenter = new BlankTipsPresenter();
        bindVP((BlankTipsFragment)mCurrFragment, mCurrPresenter);
        showFragment(mCurrFragment);
    }

    private void removeCurrFragment() {
        if (mCurrFragment != null)
            removeFragment(mCurrFragment);
        mCurrFragment = null;
        mCurrPresenter = null;
    }

    @Override
    public void showCurrUI(int type, int groupId) {

        removeCurrFragment();

        mCurrFragment = new DoubleAnnFramgent();
        if (type == AnnouncementContract.TYPE_UI_TEACHERORASSISTANT) {
            //老师
            mCurrPresenter = new DoubleAnnPresenter((DoubleAnnFramgent)mCurrFragment, AnnouncementContract.TYPE_UI_TEACHERORASSISTANT, groupId);
        } else if (type == AnnouncementContract.TYPE_UI_GROUPTEACHERORASSISTANT) {
            //分组老师
            mCurrPresenter = new DoubleAnnPresenter((DoubleAnnFramgent)mCurrFragment, AnnouncementContract.TYPE_UI_GROUPTEACHERORASSISTANT, groupId);
        } else {
            //学生
            mCurrPresenter = new DoubleAnnPresenter((DoubleAnnFramgent)mCurrFragment, AnnouncementContract.TYPE_UI_STUDENT, groupId);

        }
        bindVP((DoubleAnnFramgent) mCurrFragment, mCurrPresenter);
        showFragment(mCurrFragment);
    }

    @Override
    public void setNoticeInfo(IAnnouncementModel iAnnouncementModel) {

        if (!(mCurrFragment instanceof DoubleAnnFramgent)) {
            mPresenter.switchUI();
        }

        if (mCurrPresenter == null && !(mCurrPresenter instanceof IAnnouncementUI))
            return;

        if (String.valueOf(mPresenter.getRouter().getLiveRoom().getGroupId()).equals(iAnnouncementModel.getGroup())) {
            mIAnnModel = iAnnouncementModel;
        }
        ((IAnnouncementUI) mCurrPresenter).setNoticeInfo(iAnnouncementModel);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        EditText editText = (EditText) $.id(R.id.dialog_announcement_et).view();
//        EditText editUrl = (EditText) $.id(R.id.dialog_announcement_url_et).view();
//        editText.removeTextChangedListener(textWatcher);
//        editUrl.removeTextChangedListener(textWatcher);
//        textWatcher = null;
//        presenter = null;
    }

    private EditAnnContract.OnAnnEditListener mAnnEditListener = new EditAnnContract.OnAnnEditListener() {
        @Override
        public void cannel() {
            mPresenter.switchUI();
        }

        @Override
        public void onError() {

        }

        @Override
        public void Success() {

        }
    };
}
