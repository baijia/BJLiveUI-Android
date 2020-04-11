package com.baijiayun.live.ui.toolbox.timer;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TimePicker;

import com.baijiayun.live.ui.LiveRoomTripleActivity;
import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseDialogFragment;
import com.baijiayun.live.ui.base.BaseFragment;
import com.baijiayun.live.ui.utils.QueryPlus;
import com.baijiayun.livecore.models.LPBJTimerModel;

/**
 * 计时器
 */
public class TimerFragment extends BaseDialogFragment implements TimerContract.View{
    private TimerContract.Presenter presenter;
    private QueryPlus $;
    private Context context;
    private boolean isPublish;
    private TextView etMin;
    private TextView etSecond;
    private CheckedTextView tvPublish;
    private CheckedTextView tvCountDown;
    private CheckedTextView tvCountUp;
    private boolean canEditable = true;
    private long duration;

    @Override
    public void setPresenter(TimerContract.Presenter presenter) {
        setBasePresenter(presenter);
        this.presenter = presenter;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        this.context = context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @SuppressLint("SetTextI18n")
    @Override
    protected void init(Bundle savedInstanceState ,Bundle arguments) {
        $ = QueryPlus.with(contentView);
        hideBackground();
        tvPublish = (CheckedTextView) $.id(R.id.tv_publish).view();
        etMin = (TextView) $.id(R.id.dialog_timer_et_min).view();
        etSecond = (TextView) $.id(R.id.dialog_timer_et_second).view();
        tvCountDown = (CheckedTextView) $.id(R.id.dialog_timer_tv_count_down).view();
        tvCountUp = (CheckedTextView) $.id(R.id.dialog_timer_tv_count_up).view();

        etMin.setOnClickListener(v -> {
            if(canEditable) new TimePickerDialog(context, AlertDialog.THEME_HOLO_LIGHT , (view, hourOfDay, minute) -> {
                etMin.setText(hourOfDay > 9 ? hourOfDay + "" : "0" + hourOfDay);
                etSecond.setText(minute > 9 ? minute + "" : "0" + minute);
            }, Integer.parseInt(etMin.getText().toString()), Integer.parseInt(etSecond.getText().toString()), true).show();
        });
        etSecond.setOnClickListener(v -> {
            if(canEditable) new TimePickerDialog(context,AlertDialog.THEME_HOLO_LIGHT , (view, hourOfDay, minute) -> {
                etMin.setText(hourOfDay > 9 ? hourOfDay + "" : "0" + hourOfDay);
                etSecond.setText(minute > 9 ? minute + "" : "0" + minute);
        }, Integer.parseInt(etMin.getText().toString()), Integer.parseInt(etSecond.getText().toString()), true).show();
        });

        $.id(R.id.tv_publish).clicked(v -> {
            hideInput(context, contentView);
            if (!isPublish) {
                //启动
                publish(getTimerSeconds());
            } else {
                //暂停
                pause();
            }
            setTabClickable(canEditable);
        });
        $.id(R.id.dialog_end).clicked(v -> {
            presenter.requestTimerEnd();
            reset();
        });
        tvCountDown.setOnClickListener(v -> {
            tvCountUp.setChecked(false);
            tvCountDown.setChecked(true);
        });
        tvCountUp.setOnClickListener(v -> {
            tvCountUp.setChecked(true);
            tvCountDown.setChecked(false);
        });
    }

    private void reset() {
        setTimer(0);
        setButtonState(TimerPresenter.start_timer);
    }

    private boolean isCountDown() {
        CheckedTextView textView = (CheckedTextView) $.id(R.id.dialog_timer_tv_count_down).view();
        return textView.isChecked();
    }

    private void setTabClickable(boolean clickable) {
        $.id(R.id.dialog_timer_tv_count_down).view().setEnabled(clickable);
        $.id(R.id.dialog_timer_tv_count_up).view().setEnabled(clickable);
    }

    private void publish(long duration) {
        if (!isLegal()) {
            showToast(getString(R.string.timer_error_tip,isCountDown()?getString(R.string.timer_countdown):getString(R.string.timer_countup)));
            return;
        }
        if (getString(R.string.timer_start).equals(tvPublish.getText().toString())) {
            this.duration = duration;
        }

        presenter.requestTimerStart(0,this.duration,isCountDown());

        onDestroyView();
    }

    private void pause() {
        setButtonState(TimerPresenter.continue_timer);
        presenter.requestTimerPause(0, duration, isCountDown());
    }

    private boolean isLegal() {
        return getTimerSeconds() > 0;
    }

    private long getTimerSeconds() {
        long min = 0, second = 0;
        long seconds = 0;
        try {
            min = Long.parseLong(etMin.getText().toString());
            second = Long.parseLong(etSecond.getText().toString());
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        seconds = min * 60 + second;
        return seconds;
    }

    public static void hideInput(Context context, View view) {
        InputMethodManager inputMethodManager = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
    @Override
    public void setTimer(long remainSeconds) {
        long min = remainSeconds / 60;
        long second = remainSeconds % 60;
        etMin.setText(String.valueOf(min >= 10 ? min : ("0" + min)));
        etSecond.setText(String.valueOf(second >= 10 ? second : ("0" + second)));
    }

    private void showEditable(boolean canEditable) {
        etMin.setClickable(canEditable);
        etSecond.setClickable(canEditable);
        tvCountDown.setEnabled(canEditable);
        tvCountUp.setEnabled(canEditable);
    }

    @Override
    public void showViewState(boolean isCountDown) {
        tvCountDown.setChecked(isCountDown);
        tvCountUp.setChecked(!isCountDown);
    }

    @Override
    public void hideButton() {
        $.id(R.id.dialog_close).visibility(View.GONE);
        $.id(R.id.ll_tab).visibility(View.GONE);
        $.id(R.id.tv_publish).visibility(View.GONE);
        showEditable(false);
    }

    @Override
    public void showTimerEnd() {
        reset();
    }

    @Override
    public void showTimerPause(boolean isPause) {
        $.id(R.id.dialog_base_title).text(getString(isPause ? R.string.timer_pause_tip : R.string.timer));
    }

    @Override
    public void setButtonState(String state) {
        if (TextUtils.isEmpty(state)){
            return;
        }

        if (state.equals(TimerPresenter.pause_timer)){
            isPublish = true;
            canEditable = false;
            showTimerPause(false);
            tvPublish.setText("暂停计时");
        }else if (state.equals(TimerPresenter.continue_timer)){
            isPublish = false;
            canEditable = false;
            showTimerPause(true);
            tvPublish.setText("开始计时");
        }else if (state.equals(TimerPresenter.start_timer)){
            isPublish = false;
            canEditable = true;
            showTimerPause(false);
            tvPublish.setText("开始计时");
        }
        showEditable(canEditable);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    @Override
    public int getLayoutId() {
        return R.layout.bjy_fragment_timer;
    }
}
