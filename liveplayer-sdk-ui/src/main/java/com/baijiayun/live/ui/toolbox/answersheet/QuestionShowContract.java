package com.baijiayun.live.ui.toolbox.answersheet;

import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;
import com.baijiayun.livecore.models.LPAnswerModel;


/**
 * Created by yangjingming on 2018/6/5.
 */

public interface QuestionShowContract {
    interface View extends BaseView<Presenter>{
        void onShowAnswer(LPAnswerModel lpAnswerModel);
    }

    interface Presenter extends BasePresenter{
        void removeQuestionShow();
    }
}
