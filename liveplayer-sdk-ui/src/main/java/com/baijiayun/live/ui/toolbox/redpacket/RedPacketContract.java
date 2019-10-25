package com.baijiayun.live.ui.toolbox.redpacket;

import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;
import com.baijiayun.live.ui.toolbox.redpacket.widget.MoveModel;

public interface RedPacketContract {

    //红包开始倒计时
    public static int TYPE_REDPACKET_START_COUNTDOWN = 1;
    //红包雨运行中
    public static int TYPE_REDPACKET_RUNNING = 2;
    //显示排行榜
    public static int TYPE_REDPACKET_RANKING_LIST = 3;
    //推出红包
    public static int TYPE_REDPACKET_EXIT = 4;
    //一个都没抢到
    public static int TYE_REDPACKET_NOT_ROB = 5;
    //抢到积分提示
    public static int TYPE_REDPACKET_ROB = 6;

    interface View extends BaseView<Presenter> {

        /**
         * 更新红包倒计时时间
         * @param timeStart     距离开始抽红包时间
         */
        void upDateRedPacketTime(long timeStart);

        /**
         * 抽红包开始倒计时 ->切换到红包雨效果
         */
        void switchRedPacketStart(int type);

        /**
         * 红包雨效果 -> 排行榜
         */
        void switchRedPacketRankingList(RedPacketTopModel[] list);

        void showRedPacketScoreAmount(int mScoreAmount);

        int getCurrStateType();

        void setRobEnable(boolean robEnable);
    }

    interface Presenter extends BasePresenter {

        void release();

        void robRedPacket(MoveModel model);

        void updateRedPacket();

        int getScoreAmount();

        boolean getRedPacketing();

        void switchState(int type);

        void exit();
    }

}
