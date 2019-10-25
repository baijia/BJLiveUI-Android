package com.baijiayun.live.ui.toolbox.questionanswer;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.livecore.context.LPError;
import com.baijiayun.livecore.models.LPQuestionPullResItem;
import com.baijiayun.livecore.utils.LPRxUtils;


import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.Disposable;

public class QuestionAnswerPresenter implements QuestionAnswerContract.Presenter{

    private LiveRoomRouterListener liveRoomRouterListener;
    private Disposable disposableOfQuestionQueue, disposableOfQuestionForbid;
    private QuestionAnswerContract.View view;
    private List<LPQuestionPullResItem> questionList = new ArrayList<>();
    private boolean isLoading = false, isForbidQuestion = false;

    public QuestionAnswerPresenter(QuestionAnswerContract.View view) {
        this.view = view;
    }

    @Override
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {
        this.liveRoomRouterListener = liveRoomRouterListener;
    }

    @Override
    public void subscribe() {

        disposableOfQuestionQueue = liveRoomRouterListener.getLiveRoom().getObservableOfQuestionQueue()
                .subscribe(priorityQueue -> {
                    isLoading = false;
                    questionList.clear();
                    questionList.addAll(priorityQueue);
                    if (questionList.isEmpty()) {
                        view.showEmpty(true);
                    }else {
                        view.showEmpty(false);
                    }
                    view.notifyDataChange();
                });

        disposableOfQuestionForbid = liveRoomRouterListener.getLiveRoom().getObservableOfQuestionForbidStatus()
                .subscribe(aBoolean -> {
                    if (isForbidQuestion != aBoolean)
                        view.forbidQuestion(aBoolean);
                    isForbidQuestion = aBoolean;
                }
                );

        liveRoomRouterListener.getLiveRoom().loadMoreQuestions();
    }

    @Override
    public void unSubscribe() {
        LPRxUtils.dispose(disposableOfQuestionQueue);
        LPRxUtils.dispose(disposableOfQuestionForbid);
    }

    @Override
    public void destroy() {
        liveRoomRouterListener = null;
        view = null;
    }

    @Override
    public void sendQuestion(String content) {
        LPError error = liveRoomRouterListener.getLiveRoom().sendQuestion(content);
        if (error != null) {
            view.showToast(error.getMessage());
        } else {
            view.sendSuccess();
            view.showToast("发送成功");
        }
    }

    @Override
    public int getCount() {
        return isLoading ? questionList.size() + 1 : questionList.size();
    }

    @Override
    public LPQuestionPullResItem getQuestion(int position) {
        if (position >= questionList.size())
            return null;
        return questionList.get(position);
    }

    @Override
    public void loadMore() {
        isLoading = true;
        LPError error = liveRoomRouterListener.getLiveRoom().loadMoreQuestions();
        if (error != null) {
            isLoading = false;
            view.notifyDataChange();
        }
    }

    @Override
    public boolean isHasMoreQuestions() {
        return liveRoomRouterListener.getLiveRoom().isHasMoreQuestions();
    }

    @Override
    public boolean isLoading() {
        return isLoading;
    }

    @Override
    public void closeFragment() {
        liveRoomRouterListener.showQuestionAnswer(false);
    }
}
