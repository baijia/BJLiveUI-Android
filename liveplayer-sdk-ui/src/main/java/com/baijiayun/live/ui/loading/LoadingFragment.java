package com.baijiayun.live.ui.loading;

import android.animation.ObjectAnimator;
import android.os.Bundle;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ProgressBar;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseFragment;
import com.baijiayun.livecore.LiveSDK;
import com.baijiayun.livecore.context.LPError;
import com.baijiayun.livecore.context.LiveRoom;

import static com.baijiayun.live.ui.utils.Precondition.checkNotNull;

/**
 * Created by Shubo on 2017/2/14.
 */

public class LoadingFragment extends BaseFragment implements LoadingContract.View {

    private LoadingContract.Presenter presenter;
    private ProgressBar progressBar;
    private LiveRoom liveRoom;

    public static LoadingFragment newInstance(boolean checkUnique) {
        Bundle args = new Bundle();
        args.putBoolean("check_unique",checkUnique);
//        args.putBoolean("show_tech_support",showTechSupport);
        LoadingFragment fragment = new LoadingFragment();
        fragment.setArguments(args);
        return fragment;
    }
    @Override
    public void setPresenter(LoadingContract.Presenter presenter) {
        super.setBasePresenter(presenter);
        this.presenter = presenter;
    }

    @Override
    public int getLayoutId() {
        return R.layout.bjy_fragment_loading;
    }

    @Override
    public void init(Bundle savedInstanceState) {
        checkNotNull(presenter);
        progressBar = (ProgressBar) $.id(R.id.fragment_loading_pb).view();
        $.id(R.id.fragment_loading_back).clicked(lis -> getActivity().finish());
        Bundle args = getArguments();
        if (args != null) {
            LiveSDK.checkTeacherUnique = args.getBoolean("check_unique",true);
        } else {
            LiveSDK.checkTeacherUnique = false;
        }

        if (presenter.isJoinCode()) {
            liveRoom = LiveSDK.enterRoom(getActivity(), presenter.getCode(), presenter.getName(), null, presenter.getAvatar(), presenter.getLaunchListener());
        } else {
            liveRoom = LiveSDK.enterRoom(getActivity(), presenter.getRoomId(), presenter.getUser().getGroup(),presenter.getUser().getNumber(),
                    presenter.getUser().getName(), presenter.getUser().getType(), presenter.getUser().getAvatar(),
                    presenter.getSign(), presenter.getLaunchListener());
        }
        presenter.setLiveRoom(liveRoom);
    }

    private ObjectAnimator animator;

    @Override
    public void showLoadingSteps(int currentStep, int totalSteps) {
        int start = progressBar.getProgress();
        int end = currentStep * 100 / totalSteps;
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
        animator = ObjectAnimator.ofInt(progressBar, "progress", start, end);
        animator.setDuration(400);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();

        if(currentStep == 2){
            int hideBJYSupportMessage = liveRoom.getPartnerConfig() == null ? 1 : liveRoom.getPartnerConfig().hideBJYSupportMessage;
            setTechSupportVisibility(hideBJYSupportMessage == 0);
            presenter.setShouldShowTecSupport(hideBJYSupportMessage == 0);
        }
    }

    public void setTechSupportVisibility(boolean shouldShow) {
        if (shouldShow) {
            $.id(R.id.tv_fragment_loading_tech_support).visible();
        } else {
            $.id(R.id.tv_fragment_loading_tech_support).invisible();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }
    }
}
