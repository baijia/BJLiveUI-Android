package com.baijiayun.live.ui.toolbox.questionanswer;

import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;
import com.baijiayun.livecore.models.LPQuestionPullResItem;

interface QuestionAnswerContract {

    interface View extends BaseView<Presenter>{
        void notifyDataChange();

        void showToast(String content);

        void sendSuccess();

        void showEmpty(boolean isEmpty);

        void forbidQuestion(boolean isForbid);
    }

    interface Presenter extends BasePresenter{

        void sendQuestion(String content);

        int getCount();

        LPQuestionPullResItem getQuestion(int position);

        void loadMore();

        boolean isHasMoreQuestions();

        boolean isLoading();

        void closeFragment();
    }
}
