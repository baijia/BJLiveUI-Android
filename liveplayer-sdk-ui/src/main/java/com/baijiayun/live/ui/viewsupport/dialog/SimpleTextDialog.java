package com.baijiayun.live.ui.viewsupport.dialog;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.utils.RxUtils;
import com.baijiayun.livecore.context.LPError;
import com.baijiayun.livecore.utils.LPLogger;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * 单条文本提示
 * 201907128
 * panzq
 */
public class SimpleTextDialog extends Dialog {

    private TextView mTvDialogTitle;
    private TextView mTvDialogInfo;
    private Button mBtnDialogOk;

    private OnOkClickListener mOnOkClickListener;

    private LPError error;

    private Disposable mQuitDisposable;
    private final int TIME_JUMP_ENDLINK = 5;//倒计跳转时间/s

    public SimpleTextDialog(@NonNull Context context, LPError error) {
        super(context, R.style.DialogStyle);
        this.error = error;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_simple_text);

        initView();
        initListener();
    }

    private void initListener() {
        mBtnDialogOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                jump();
            }
        });
    }

    private void jump() {
        RxUtils.dispose(mQuitDisposable);
        mQuitDisposable = null;
        if (mOnOkClickListener == null)
            return;
        mOnOkClickListener.onJump(error.getAuditionEndLink());
    }

    private void initView() {
        mTvDialogTitle = findViewById(R.id.tv_dialog_title);
        mTvDialogInfo = findViewById(R.id.tv_dialog_info);
        mBtnDialogOk= findViewById(R.id.btn_dialog_ok);

        mTvDialogInfo.setText(error.getMessage());

        mQuitDisposable = Observable.interval(0, 1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        if (aLong >= TIME_JUMP_ENDLINK) {
                            jump();
                        } else {
                            mBtnDialogOk.setText(getContext().getResources().getString(R.string.live_quiz_dialog_confirm)
                                    + "(" + (TIME_JUMP_ENDLINK - aLong) + ")" );
                        }
                    }
                });
    }

    public void setOnOkClickListener(OnOkClickListener listener) {
        this.mOnOkClickListener = listener;
    }

    public interface OnOkClickListener {
        void onJump(String url);
    }
}
