package com.baijiayun.live.ui.menu.rightmenu;

import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;

/**
 * Created by Shubo on 2017/2/15.
 */

public interface RightMenuContract {

    int STUDENT_SPEAK_APPLY_NONE = 0;
    int STUDENT_SPEAK_APPLY_APPLYING = 1;
    int STUDENT_SPEAK_APPLY_SPEAKING = 2;

    interface View extends BaseView<Presenter> {

        void showSpeakClosedByTeacher(boolean isSmallGroup);

        void showSpeakClosedByServer();

        void showForceSpeakDenyByServer();

        void showDrawingStatus(boolean isEnable);

        void showSpeakApplyCountDown(int countDownTime, int total);

        void showSpeakApplyAgreed(boolean isEnableDrawing);

        void showSpeakApplyDisagreed();

        void showSpeakApplyCanceled();

        void showTeacherRightMenu();

        void showStudentRightMenu();

        void showForbiddenHand();

        void showNotForbiddenHand();

        void hidePPTDrawBtn();

        void showPPTDrawBtn();

        void showHandUpError();

        void showHandUpForbid();

        void showCantDraw();

        void showCantDrawCauseClassNotStart();

        void showWaitingTeacherAgree();

        void showAutoSpeak(boolean isDrawingEnable);

        void showForceSpeak(boolean isDrawingEnable);

        void hideUserList();

        void hideSpeakApply();

        void showHandUpTimeout();

        void setAudition();

        void showDrawDeny();

        void hideTimer();
    }

    interface Presenter extends BasePresenter {
        void visitOnlineUser();

        void changeDrawing();

        void managePPT();

        void speakApply();

        void onSpeakInvite(int confirm);

        boolean isWaitingRecordOpen();

        void showTimer();
    }
}
