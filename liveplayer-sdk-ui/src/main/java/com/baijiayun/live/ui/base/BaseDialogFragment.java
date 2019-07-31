package com.baijiayun.live.ui.base;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.utils.Precondition;
import com.baijiayun.live.ui.utils.QueryPlus;
import com.baijiayun.livecore.utils.DisplayUtils;


/**
 * Created by Shubo on 2017/2/15.
 */

public abstract class BaseDialogFragment extends DialogFragment {

    private BasePresenter basePresenter;
    protected View contentView;
    private boolean isEditing;
    private QueryPlus $;
    private int contentBackgroundColor = -1;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View baseView = inflater.inflate(R.layout.dialog_base, container);
        contentView = inflater.inflate(getLayoutId(), null);
        $ = QueryPlus.with(baseView);
        ((FrameLayout) $.id(R.id.dialog_base_content).view()).addView(contentView);
        init(savedInstanceState, getArguments());
        if (basePresenter != null)
            basePresenter.subscribe();
        return baseView;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), R.style.LiveBaseDialogFragment);
        Precondition.checkNotNull(dialog.getWindow());
        dialog.setCanceledOnTouchOutside(true);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
        dialog.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        dialog.getWindow().getDecorView().setSystemUiVisibility(getActivity().getWindow().getDecorView().getSystemUiVisibility());
        // 处理dialog沉浸式状态栏
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                //Clear the not focusable flag from the window
                if (getDialog() == null) return;
                getDialog().getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE);
                //Update the WindowManager with the new attributes (no nicer way I know of to do this)..
                WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
                wm.updateViewLayout(getDialog().getWindow().getDecorView(), getDialog().getWindow().getAttributes());
            }
        });
        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Window window = getDialog().getWindow();
        Precondition.checkNotNull(window);
        if (contentBackgroundColor == -1)
            contentBackgroundColor = ContextCompat.getColor(getContext(), R.color.live_white);
        window.setBackgroundDrawable(new ColorDrawable(contentBackgroundColor));
        WindowManager.LayoutParams windowParams = window.getAttributes();
        windowParams.dimAmount = 0.50f;
        windowParams.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        windowParams.windowAnimations = R.style.LiveBaseDialogAnim;
        setWindowParams(windowParams);
        window.setAttributes(windowParams);
    }

    protected void setWindowParams(WindowManager.LayoutParams windowParams) {
        int longEdge = Math.max(DisplayUtils.getScreenHeightPixels(getContext()), DisplayUtils.getScreenWidthPixels(getContext()));
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            //竖屏
            windowParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            windowParams.height = longEdge / 2;
        } else {
            //横屏
            windowParams.width = longEdge / 2;
            windowParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        }
        windowParams.gravity = Gravity.BOTTOM | GravityCompat.END;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        Window window = getDialog().getWindow();
        Precondition.checkNotNull(window);
        WindowManager.LayoutParams windowParams = window.getAttributes();
        resetWindowParams(windowParams);
        window.setAttributes(windowParams);
    }

    protected void resetWindowParams(WindowManager.LayoutParams windowParams) {
        setWindowParams(windowParams);
    }

    protected abstract int getLayoutId();

    protected abstract void init(Bundle savedInstanceState, Bundle arguments);

    public void setBasePresenter(BasePresenter presenter) {
        basePresenter = presenter;
    }

    public BaseDialogFragment title(String title) {
        $.id(R.id.dialog_base_title).text(title);
        return this;
    }

    public BaseDialogFragment hideBackground() {
        $.id(R.id.dialog_base_title_container).gone();
        return this;
    }

    public BaseDialogFragment contentBackgroundColor(int color) {
        contentBackgroundColor = color;
        return this;
    }

    public void setTitleTextColor(int color) {
        ((TextView) $.id(R.id.dialog_base_title).view()).setTextColor(getResources().getColor(color));
    }

    public void setTitleHeight(int height) {
        ViewGroup.LayoutParams layoutParams = ($.id(R.id.dialog_base_title_container).view()).getLayoutParams();
        layoutParams.height = height;
        ($.id(R.id.dialog_base_title_container).view()).setLayoutParams(layoutParams);
    }

    public void showCross() {
        $.id(R.id.dialog_base_edit).visibility(View.GONE);
        $.id(R.id.dialog_close).visibility(View.VISIBLE);
        $.id(R.id.dialog_close).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismissAllowingStateLoss();
            }
        });
    }

    public void setTitleBarColor(int color) {
        $.id(R.id.dialog_base_title_container).view().setBackgroundColor(getResources().getColor(color));
    }

    public BaseDialogFragment editable(boolean editable) {
        $.id(R.id.dialog_base_edit).visibility(editable ? View.VISIBLE : View.GONE);
        $.id(R.id.dialog_base_edit).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isEditing) {
                    disableEdit();
                } else {
                    enableEdit();
                }
            }
        });
        return this;
    }


    public BaseDialogFragment editText(String text) {
        $.id(R.id.dialog_base_edit).text(text);
        return this;
    }

    public BaseDialogFragment editColor(int color) {
        ((TextView) $.id(R.id.dialog_base_edit).view()).setTextColor(color);
        return this;
    }

    public BaseDialogFragment editClick(View.OnClickListener listener) {
        $.id(R.id.dialog_base_edit).clicked(listener);
        return this;
    }

    protected void hideTitleBar() {
        $.id(R.id.dialog_base_title_container).gone();
    }

    protected boolean isEditing() {
        return isEditing;
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        return super.onCreateAnimation(transit, enter, nextAnim);
    }

    @CallSuper
    protected void disableEdit() {
        isEditing = false;
        $.id(R.id.dialog_base_edit).text(getString(R.string.live_edit));
    }

    @CallSuper
    protected void enableEdit() {
        isEditing = true;
        $.id(R.id.dialog_base_edit).text(getString(R.string.live_cancel));
    }

    protected void showToast(String msg) {
        Toast.makeText(getContext(), msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (basePresenter != null) {
            basePresenter.unSubscribe();
            basePresenter.destroy();
            basePresenter = null;
        }
        $ = null;
    }

    public boolean isQueryPlusNull() {
        return $ == null;
    }
}
