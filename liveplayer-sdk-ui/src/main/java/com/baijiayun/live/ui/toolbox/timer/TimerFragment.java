package com.baijiayun.live.ui.toolbox.timer;

import android.content.Context;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.Toast;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseFragment;


public class TimerFragment extends BaseFragment implements TimerContract.View{
    private TimerContract.Presenter presenter;
    private Context context;
    private boolean isPublish;
    private boolean isEnd;
    private EditText etMinHigh;
    private EditText etMinLow;
    private EditText etSecondHigh;
    private EditText etSecondLow;
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
    protected void init(Bundle savedInstanceState) {
        super.init(savedInstanceState);
        tvPublish = (CheckedTextView) $.id(R.id.tv_publish).view();
        etMinHigh = (EditText) $.id(R.id.et_min_high).view();
        etMinLow = (EditText) $.id(R.id.et_min_low).view();
        etSecondHigh = (EditText) $.id(R.id.et_second_high).view();
        etSecondLow = (EditText) $.id(R.id.et_second_low).view();
        tvCountDown = (CheckedTextView) $.id(R.id.tv_countdown).view();
        tvCountUp = (CheckedTextView) $.id(R.id.tv_countup).view();
        etMinHigh.addTextChangedListener(textWatcher);
        etMinLow.addTextChangedListener(textWatcher);
        etSecondHigh.addTextChangedListener(textWatcher);
        etSecondLow.addTextChangedListener(textWatcher);
        $.id(R.id.tv_publish).clicked(v -> {
            hideInput(context, view);
            if (isEnd) {
                reset();
            } else {
                if (!isPublish) {
                    publish(getTimerSeconds());
                } else {
                    pause();
                }
            }
            setTabClickable(canEditable);
        });
        $.id(R.id.dialog_close).clicked(v -> {
            presenter.requestTimerEnd();
            presenter.closeTimer();
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
        showEditable(true);
        canEditable = true;
        isEnd = false;
        isPublish = false;
        setTimer(duration);
        showViewState(true);
        tvPublish.setText(getString(R.string.timer_start));
    }

    private boolean isCountDown() {
        CheckedTextView textView = (CheckedTextView) $.id(R.id.tv_countdown).view();
        return textView.isChecked();
    }

    private void setTabClickable(boolean clickable) {
        $.id(R.id.tv_countdown).view().setEnabled(clickable);
        $.id(R.id.tv_countup).view().setEnabled(clickable);
    }

    private void publish(long duration) {
        if (!isLegal()) {
            Toast.makeText(context,getString(R.string.timer_error_tip,isCountDown()?getString(R.string.timer_countdown):getString(R.string.timer_countup)),Toast.LENGTH_SHORT).show();
            return;
        }
        if (getString(R.string.timer_start).equals(tvPublish.getText().toString())) {
            this.duration = duration;
        }
        long current = isCountDown() ? duration : this.duration - duration;
        if (current == 0) {
            current = this.duration;
        }
        presenter.requestTimerStart(current,this.duration,isCountDown());
        isPublish = true;
        tvPublish.setText(getString(R.string.timer_pause));
        showEditable(false);
        canEditable = false;
    }

    private void pause() {
        tvPublish.setText(getString(R.string.timer_continue));
        long current = isCountDown() ? getTimerSeconds() : duration - getTimerSeconds();
        presenter.requestTimerPause(current, duration, isCountDown());
        isPublish = false;
        showEditable(false);
        canEditable = false;
    }
    private TextWatcher textWatcher = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (!canEditable || isPublish) {
                return;
            }
            if (TextUtils.isEmpty(s)) {
                tvPublish.setChecked(false);
                tvPublish.setEnabled(false);
            } else {
                boolean isEmpty = TextUtils.isEmpty(etMinHigh.getText()) || TextUtils.isEmpty(etMinLow.getText())
                        || TextUtils.isEmpty(etSecondHigh.getText()) || TextUtils.isEmpty(etMinLow.getText()) || !isLegal();
                tvPublish.setChecked(!isEmpty);
                tvPublish.setEnabled(!isEmpty);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {
        }
    };

    private boolean isLegal() {
        return getTimerSeconds() > 0;
    }

    private long getTimerSeconds() {
        long min = 0, second = 0;
        long seconds = 0;
        try {
            min = Long.parseLong(etMinHigh.getText().toString()) * 10 +
                    Long.parseLong(etMinLow.getText().toString());
            second = Long.parseLong(etSecondHigh.getText().toString()) * 10 +
                    Long.parseLong(etSecondLow.getText().toString());
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
        etMinHigh.setText(String.valueOf(min / 10));
        etMinLow.setText(String.valueOf(min % 10));
        etSecondHigh.setText(String.valueOf(second / 10));
        etSecondLow.setText(String.valueOf(second % 10));
    }

    private void showEditable(boolean canEditable) {
        etMinHigh.setFocusable(canEditable);
        etMinLow.setFocusable(canEditable);
        etSecondHigh.setFocusable(canEditable);
        etSecondLow.setFocusable(canEditable);
        etMinHigh.setFocusableInTouchMode(canEditable);
        etMinLow.setFocusableInTouchMode(canEditable);
        etSecondHigh.setFocusableInTouchMode(canEditable);
        etSecondLow.setFocusableInTouchMode(canEditable);
        etMinHigh.setCursorVisible(canEditable);
        etMinLow.setCursorVisible(canEditable);
        etSecondHigh.setCursorVisible(canEditable);
        etSecondLow.setCursorVisible(canEditable);
    }

    @Override
    public void showViewState(boolean enable) {
        etMinHigh.setEnabled(enable);
        etMinLow.setEnabled(enable);
        etSecondHigh.setEnabled(enable);
        etSecondLow.setEnabled(enable);
        $.id(R.id.tv_risk).view().setEnabled(enable);
    }

    @Override
    public void hideButton() {
        $.id(R.id.dialog_close).visibility(View.GONE);
        $.id(R.id.ll_tab).visibility(View.GONE);
        $.id(R.id.tv_publish).visibility(View.GONE);
        $.id(R.id.space).visible();
        showEditable(false);
    }

    @Override
    public void showTimerEnd() {
        isEnd = true;
        tvPublish.setText(getString(R.string.timer_restart));
    }

    @Override
    public void showTimerPause(boolean isPause) {
        $.id(R.id.dialog_base_title).text(getString(isPause ? R.string.timer_pause_tip : R.string.timer));
    }

    @Override
    public void onDestroyView() {
        if (presenter != null) {
            presenter.requestTimerEnd();
        }
        etMinHigh.removeTextChangedListener(textWatcher);
        etMinLow.removeTextChangedListener(textWatcher);
        etSecondHigh.removeTextChangedListener(textWatcher);
        etSecondLow.removeTextChangedListener(textWatcher);
        super.onDestroyView();
    }

    @Override
    public int getLayoutId() {
        return R.layout.fragment_timer;
    }
}
