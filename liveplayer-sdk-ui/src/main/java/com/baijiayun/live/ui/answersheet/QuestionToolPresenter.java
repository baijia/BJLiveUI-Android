package com.baijiayun.live.ui.answersheet;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.livecore.models.LPAnswerSheetOptionModel;
import com.baijiayun.livecore.models.LPAnswerSheetModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * Created by yangjingming on 2018/6/5.
 */

public class QuestionToolPresenter implements QuestionToolContract.Presenter{
    @Override
    public void removeQuestionTool(boolean isEnded) {
        roomRouterListener.answerEnd(isEnded);
    }

    private LiveRoomRouterListener roomRouterListener;
    private List<LPAnswerSheetOptionModel> options = new ArrayList<>();
    private long countDownTime, currentTime;
    private QuestionToolContract.View view;
    private Disposable countDownSubscription;
    private List<String> checkedOptions = new ArrayList<>();

    @Override
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {
        roomRouterListener = liveRoomRouterListener;
    }

    @Override
    public void subscribe() {
        if (countDownSubscription == null){
            countDownSubscription = Observable.interval(0,1000, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Consumer<Long>() {
                        @Override
                        public void accept(Long aLong) {
                            currentTime = countDownTime - aLong;
                            if (currentTime < 0){
                                SimpleDateFormat formatter = new SimpleDateFormat("mm: ss");
                                String ms = formatter.format(0);
//                                roomRouterListener.answerEnd(true);
                                view.timeDown(ms);
                            }else{
                                SimpleDateFormat formatter = new SimpleDateFormat("mm: ss");
                                String ms = formatter.format(currentTime * 1000);
                                view.timeDown(ms);
                            }
                        }
                    });
        }

    }

    @Override
    public void unSubscribe() {
        if (countDownSubscription != null && !countDownSubscription.isDisposed()) {
            countDownSubscription.dispose();
            countDownSubscription = null;
        }
    }

    public void setLpQuestionToolModel(LPAnswerSheetModel lpAnswerSheetModel){
        options.clear();
        options.addAll(lpAnswerSheetModel.options);
        countDownTime = lpAnswerSheetModel.countDownTime;
    }

    @Override
    public void destroy() {
        roomRouterListener = null;
        view = null;
    }

    public void setView(QuestionToolContract.View view){
        this.view = view;
    }

    @Override
    public List<LPAnswerSheetOptionModel> getOptions() {
        return options;
    }

    @Override
    public void addCheckedOption(int index) {
        if (!checkedOptions.contains(String.valueOf(index)))
            checkedOptions.add(String.valueOf(index));
    }

    @Override
    public void deleteCheckedOption(int index) {
        if (checkedOptions.contains(String.valueOf(index)))
            checkedOptions.remove(String.valueOf(index));
    }

    @Override
    public boolean submitAnswers() {
        return roomRouterListener.getLiveRoom().submitAnswerSheet(checkedOptions);
    }


}
