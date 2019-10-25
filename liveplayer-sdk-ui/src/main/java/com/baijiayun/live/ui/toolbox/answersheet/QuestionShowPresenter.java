package com.baijiayun.live.ui.toolbox.answersheet;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.livecore.models.LPAnswerModel;

public class QuestionShowPresenter implements QuestionShowContract.Presenter{
    private LiveRoomRouterListener roomRouterListener;
    private QuestionShowContract.View view;
    private LPAnswerModel lpAnswerModel;

    @Override
    public void removeQuestionShow() {
        roomRouterListener.removeAnswer();
    }

    @Override
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {
        roomRouterListener = liveRoomRouterListener;
    }

    @Override
    public void subscribe() {
        view.onShowAnswer(lpAnswerModel);
    }

    @Override
    public void unSubscribe() {

    }
    public void setView(QuestionShowContract.View view){
        this.view = view;
    }
    @Override
    public void destroy() {
        roomRouterListener = null;
        view = null;
    }

    public void setLpQuestionToolModel(LPAnswerModel lpAnswerModel) {
        this.lpAnswerModel = lpAnswerModel;
    }
}
