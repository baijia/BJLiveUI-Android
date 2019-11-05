package com.baijiayun.live.ui.toolbox.answersheet;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.livecore.models.LPAnswerModel;
import com.baijiayun.livecore.models.LPAnswerSheetOptionModel;

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

    @Override
    public String getDesc() {
        return lpAnswerModel.getDescription();
    }

    private LiveRoomRouterListener roomRouterListener;
    private List<LPAnswerSheetOptionModel> options = new ArrayList<>();
    private long countDownTime, currentTime;
    private QuestionToolContract.View view;
    private Disposable countDownSubscription;
    private LPAnswerModel lpAnswerModel;
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

    public void setLpQuestionToolModel(LPAnswerModel lpAnswerModel){
        this.lpAnswerModel = lpAnswerModel;
        options.clear();
        options.addAll(lpAnswerModel.options);
        countDownTime = lpAnswerModel.duration;
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
    public boolean isJudgement() {
        return lpAnswerModel.isJudgement();
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
    public boolean isItemChecked(int index) {
        return checkedOptions.contains(String.valueOf(index));
    }

    private void checkOptions() {
        if (lpAnswerModel == null || lpAnswerModel.options == null || lpAnswerModel.options.isEmpty()) {
            return;
        }
        List<LPAnswerSheetOptionModel> options = lpAnswerModel.options;
        for (int i = 0; i < options.size(); i++) {
            LPAnswerSheetOptionModel lpAnswerSheetOptionModel = options.get(i);
            if (checkedOptions.contains(String.valueOf(i + 1))) {
                lpAnswerSheetOptionModel.isActive = true;
            }
        }
    }

    @Override
    public boolean submitAnswers() {
        if (checkedOptions.isEmpty()) {
            return false;
        }
        checkOptions();
        roomRouterListener.setQuestionAnswerCahce(lpAnswerModel);
        return roomRouterListener.getLiveRoom().getToolBoxVM().submitAnswers(lpAnswerModel);
    }
}
