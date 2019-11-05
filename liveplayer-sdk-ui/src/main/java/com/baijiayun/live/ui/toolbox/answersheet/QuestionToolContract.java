package com.baijiayun.live.ui.toolbox.answersheet;

import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;
import com.baijiayun.livecore.models.LPAnswerSheetOptionModel;

import java.util.List;


/**
 * Created by yangjingming on 2018/6/5.
 */

public interface QuestionToolContract {
    interface View extends BaseView<Presenter>{
        void timeDown(String down);
    }

    interface Presenter extends BasePresenter{
        boolean isJudgement();

        List<LPAnswerSheetOptionModel> getOptions();

        void addCheckedOption(int index);

        void deleteCheckedOption(int index);

        boolean isItemChecked(int index);

        boolean submitAnswers();

        void removeQuestionTool(boolean isEnded);

        String getDesc();
    }
}
