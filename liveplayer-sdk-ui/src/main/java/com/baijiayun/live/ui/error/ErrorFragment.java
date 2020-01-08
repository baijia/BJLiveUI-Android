package com.baijiayun.live.ui.error;

import android.os.Bundle;
import android.text.TextUtils;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.base.BaseFragment;

/**
 * Created by Shubo on 2017/5/10.
 */

public class ErrorFragment extends BaseFragment {

    private LiveRoomRouterListener routerListener;
    private boolean checkUnique = true;
    public final static int ERROR_HANDLE_RECONNECT = 0;
    public final static int ERROR_HANDLE_REENTER = 1;
    public final static int ERROR_HANDLE_NOTHING = 2;
    public final static int ERROR_HANDLE_CONFILICT = 3;

    public static ErrorFragment newInstance(String title, String content, int handleWay, boolean shouldShowTechSupport) {
        return ErrorFragment.newInstance(title, content, handleWay, shouldShowTechSupport, true);
    }

    public static ErrorFragment newInstance(String title, String content, int handleWay, boolean shouldShowTechSupport, boolean shouldShowTechContact) {
        Bundle args = new Bundle();
        args.putString("title", title);
        args.putString("content", content);
        args.putInt("handleWay", handleWay);
        args.putBoolean("shouldShowTechSupport", shouldShowTechSupport);
        args.putBoolean("shouldShowTechContact", shouldShowTechContact);
        ErrorFragment fragment = new ErrorFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public static ErrorFragment newInstance(boolean checkUnique, int handleWay, boolean shouldShowTechSupport, boolean shouldShowTechContact) {
        Bundle args = new Bundle();
        args.putInt("handleWay", handleWay);
        args.putBoolean("checkUnique", checkUnique);
        args.putBoolean("shouldShowTechSupport", shouldShowTechSupport);
        args.putBoolean("shouldShowTechContact", shouldShowTechContact);
        ErrorFragment fragment = new ErrorFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_error;
    }

    @Override
    protected void init(Bundle savedInstanceState) {
        super.init(savedInstanceState);
        $.id(R.id.fragment_error_back).clicked(v -> {
            if (!checkUnique) {
                routerListener.getLiveRoom().quitRoom();
            }
            getActivity().finish();
        });
        this.checkUnique = getArguments().getBoolean("checkUnique", true);
        if (!this.checkUnique) {
            $.id(R.id.fragment_error_title).text(getString(R.string.live_teacher_in));
            $.id(R.id.fragment_error_reason).text(getString(R.string.live_login_conflict_tip));
            $.id(R.id.fragment_error_retry).text(getString(R.string.live_enter_room));
        } else {
            $.id(R.id.fragment_error_title).text(getArguments().getString("title"));
            $.id(R.id.fragment_error_reason).text(getArguments().getString("content"));
        }
        if (getArguments().getBoolean("shouldShowTechSupport", false)) {
            $.id(R.id.tv_logo).visible();
            $.id(R.id.tv_logo).view().setAlpha(0.3f);
        } else {
            $.id(R.id.tv_logo).gone();
        }
        if (TextUtils.isEmpty(routerListener.getLiveRoom().getCustomerSupportDefaultExceptionMessage())
                || !getArguments().getBoolean("shouldShowTechContact", true)) {
            $.id(R.id.fragment_error_suggestion).gone();
        } else {
            $.id(R.id.fragment_error_suggestion).visible();
            $.id(R.id.fragment_error_suggestion).text(routerListener.getLiveRoom().getCustomerSupportDefaultExceptionMessage());
        }
        $.id(R.id.fragment_error_retry).clicked(v -> {
            if (routerListener != null) {
                int handleWay = getArguments().getInt("handleWay");
                if (handleWay == ERROR_HANDLE_RECONNECT) {
                    routerListener.doReEnterRoom(false);
                } else if (handleWay == ERROR_HANDLE_REENTER) {
                    routerListener.doReEnterRoom(false);
                } else if (handleWay == ERROR_HANDLE_CONFILICT) {
                    routerListener.doReEnterRoom(checkUnique);
                } else {
                    routerListener.doHandleErrorNothing();
                }
            }
        });
        view.setOnClickListener(v -> {

        });
    }

    public void setRouterListener(LiveRoomRouterListener routerListener) {
        this.routerListener = routerListener;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        routerListener = null;
    }
}
