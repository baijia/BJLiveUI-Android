package com.baijiayun.live.ui.toolbox.timer;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseFragment;
import com.baijiayun.livecore.models.LPBJTimerModel;

/**
 * @author lzd
 */
public class TimerShowyFragment extends BaseFragment implements TimerContract.View {
    private TimerContract.Presenter presenter;
    private Context context;

    @Override
    public int getLayoutId() {return R.layout.bjy_fragment_timer_showy;}

    @Override
    protected void init(Bundle savedInstanceState) {
        super.init(savedInstanceState);
    }

    @Override
    public void showViewState(boolean isCounting) {
        $.id(R.id.dialog_timer_showy_bg).backgroundDrawable(context.getResources().getDrawable(
                isCounting ? R.drawable.shape_timer_showy_bg_nomal : R.drawable.shape_timer_showy_bg_end));
    }

    @Override
    public void hideButton() {
    }

    @Override
    public void showTimerEnd() {
        //计时结束
        $.id(R.id.dialog_timer_showy_bg).backgroundDrawable(context.getResources().getDrawable(R.drawable.shape_timer_showy_bg_end));
    }

    @Override
    public void showTimerPause(boolean isPause) {
        //暂停
    }

    @Override
    public void setButtonState(String state) {
        //传入 start ，将背景变为正常
        $.id(R.id.dialog_timer_showy_bg).backgroundDrawable(context.getResources().getDrawable(R.drawable.shape_timer_showy_bg_nomal));
    }

    @Override
    public void setTimer(long remainSeconds) {
        long min = remainSeconds / 60;
        long second = remainSeconds % 60;
        String sb = (min >= 10 ? min : ("0" + min)) +
                ":" +
                (second >= 10 ? second : ("0" + second));
        $.id(R.id.dialog_timer_showy_time).text(sb);
    }

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
    public void onDestroyView(){
        super.onDestroyView();
        presenter.requestTimerEnd();
        presenter.destroy();
        presenter = null;
    }
}
