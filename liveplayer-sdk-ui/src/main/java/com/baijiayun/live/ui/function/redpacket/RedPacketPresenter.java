package com.baijiayun.live.ui.function.redpacket;

import android.annotation.SuppressLint;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.function.redpacket.widget.MoveModel;
import com.baijiayun.live.ui.utils.RxUtils;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.models.LPRedPacketModel;
import com.baijiayun.livecore.models.LPShortResult;
import com.baijiayun.livecore.utils.LPJsonUtils;
import com.baijiayun.livecore.utils.LPLogger;
import com.google.gson.JsonObject;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

/**
 * 红包雨效果
 * @author panzq
 * @date    20190514
 */
public class RedPacketPresenter implements RedPacketContract.Presenter {

    private final String TAG = RedPacketPresenter.class.getName();

    private final long TIME_RED_PACKET_START = 3;//红包启动倒计时时间/s
    private final int TIME_ROB_RED_PACKET_INTERVAL = 1000;//抢红包时间间隔/ms

    private LiveRoomRouterListener mRouter;
    private RedPacketContract.View mView;

    private Disposable mRedPacketDisposable;
    private Disposable mTimeDisposable;
    private Disposable mRedPacketRain;
    private Disposable mRedResultDisposable;

    private boolean isRedPacketing = false;

    private LPRedPacketModel mLPRedPacketModel;
    private long mTimeRob = 0;

    private Disposable mRobDisposable, mListDisposable, mRedPacketPHB;
    private int mScoreAmount = 0;

    private RedPacketTopList mTopList;
    long mSleepTime;

    boolean isStudent = true;

    private LPRedPacketModel model;
    public RedPacketPresenter(RedPacketContract.View view, LPRedPacketModel model) {
        this.mView = view;
        this.model = model;
    }


    @Override
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {
        mRouter = liveRoomRouterListener;
    }

    @SuppressLint("CheckResult")
    @Override
    public void subscribe() {

        isStudent = mRouter.getLiveRoom().getCurrentUser().getType() == LPConstants.LPUserType.Student
                || mRouter.getLiveRoom().getCurrentUser().getType() == LPConstants.LPUserType.Visitor;

        mView.setRobEnable(isStudent);
        startRedPacket(model);
    }

    @Override
    public void robRedPacket(MoveModel model) {

        long time = System.currentTimeMillis();
        if (mTimeRob == 0) {
            mTimeRob = time;
        } else if (time - mTimeRob < TIME_ROB_RED_PACKET_INTERVAL) {
            return;
        }

        if (mLPRedPacketModel == null)
            return;

        //请求
        RxUtils.dispose(mRobDisposable);
        mRobDisposable = mRouter.getLiveRoom().requestCloudRobRedPacket(Integer.valueOf(mLPRedPacketModel.id))
                .observeOn(AndroidSchedulers.mainThread())
            .subscribe(new Consumer<LPShortResult>() {
                @Override
                public void accept(LPShortResult lpShortResult) throws Exception {

                    if (model == null)
                        return;

                    if (!model.isRob)
                        return;

                    RobRedPacketModel robModel = LPJsonUtils.parseJsonObject((JsonObject) lpShortResult.data, RobRedPacketModel.class);
                    if (robModel.status != 0)
                        return;

                    if (robModel != null && robModel.status == 0) {
                        //抢成功
                        mScoreAmount += robModel.score_amount;

                        model.scoreAmount = robModel.score_amount;
                        model.isOpen = true;
//                        mView.showRedPacketScoreAmount(model);
                    } else {
                        //抢失败
                        model.scoreAmount = -1;
                        model.isOpen = true;
                    }
                }
            });
    }


    /**
     * 启动
     * @param lpRedPacketModel
     */
    private void startRedPacket(LPRedPacketModel lpRedPacketModel) {

        if (isRedPacketing) {
            LPLogger.d(TAG, "startRedPacket : runing " + isRedPacketing);
            return;
        }

        release();
        isRedPacketing = true;
        mTimeRob = 0;
        mTopList = null;
        mLPRedPacketModel = lpRedPacketModel;
        mView.switchRedPacketStart(RedPacketContract.TYPE_REDPACKET_START_COUNTDOWN);
        mTimeDisposable = Observable.interval(0, 1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        LPLogger.d(TAG, "startRedPacket : time : " + aLong);
                        long timeStart = TIME_RED_PACKET_START - aLong;
                        if (timeStart <= 0) {
                            //倒计时完成，开始抽红包
                            RxUtils.dispose(mTimeDisposable);
                            mScoreAmount = 0;

                            startRedPacketRain(lpRedPacketModel);
                        } else {

//                            long showText = timeStart - 2;
//                            //倒计时完成后，多增加2S等待时间
//                            if (showText <= 0)
//                                return;
                            mView.upDateRedPacketTime(timeStart);
                        }

                    }
                });
    }

    private void startRedPacketRain(LPRedPacketModel lpRedPacketModel) {

        mView.switchRedPacketStart(RedPacketContract.TYPE_REDPACKET_RUNNING);
        mRedPacketRain = Observable.interval(0, 1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        LPLogger.d(TAG, "startRedPackRain : time : " + aLong);
                        if (aLong >= lpRedPacketModel.duration) {
                            //请求数据
                            getRedPacketList(lpRedPacketModel.id);
                        }
                    }
                });
    }

    /**
     * 获取红包排名列表
     */
    private void getRedPacketList(String id) {

        //切换到排行榜
//        RxUtils.dispose(mRedPacketRain);
//        mListDisposable = mRouter.getLiveRoom().requestCloudRedPacketRankList(Integer.valueOf(id))
//                .subscribe(new Consumer<LPShortResult>() {
//                    @Override
//                    public void accept(LPShortResult lpShortResult) throws Exception {
//
//                        RedPacketTopList topList = LPJsonUtils.parseJsonObject((JsonObject) lpShortResult.data, RedPacketTopList.class);
//                        showTopList(topList);
//                    }
//                });

        //切换到排行榜
        RxUtils.dispose(mRedPacketRain);
        mListDisposable = Observable.interval(0, 1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {
                        mRedPacketPHB = mRouter.getLiveRoom().requestCloudRedPacketRankList(Integer.valueOf(id))
                                .subscribe(new ExConsumer());
                    }
                });
        showTopList();
    }

    private void showTopList() {

        mRedResultDisposable = Observable.interval(0, 1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {

                        LPLogger.d(TAG, "showTopList : mRedResultDisposable" );

                        if (mTopList == null) {
                            mSleepTime = aLong;
                            return;
                        }

                        isRedPacketing = false;
                        if ((aLong - mSleepTime) <= 1) {
                            //老师/助教直接跳转排行榜
                            if (!isStudent) {
                                switchState(RedPacketContract.TYPE_REDPACKET_RANKING_LIST);
                                return;
                            }

                            if (mScoreAmount <= 0) {
                                //显示未抢到提示
                                mView.switchRedPacketStart(RedPacketContract.TYE_REDPACKET_NOT_ROB);
                            } else {
                                //显示抢到总个数
                                mView.switchRedPacketStart(RedPacketContract.TYPE_REDPACKET_ROB);
                            }
                        } else if ((aLong - mSleepTime) == 5){
                            if (mView.getCurrStateType() != RedPacketContract.TYPE_REDPACKET_RANKING_LIST) {
                                mView.switchRedPacketStart(RedPacketContract.TYPE_REDPACKET_RANKING_LIST);
                                mView.switchRedPacketRankingList(mTopList.list);
                                mScoreAmount = 0;
                            }
                        }
                    }
                });
    }


    private void showTopList(RedPacketTopList topList) {

        mRedResultDisposable = Observable.interval(0, 1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) throws Exception {

                        mTopList = topList;

//                        else if (aLong >= 9){
//                            RxUtils.dispose(mRedResultDisposable);
//                            mView.switchRedPacketStart(RedPacketContract.TYPE_REDPACKET_EXIT);
//                            updateRedPacket();
//                        }
                    }
                });
    }

    @Override
    public void unSubscribe() {

        RxUtils.dispose(mRedPacketDisposable);
        RxUtils.dispose(mTimeDisposable);
        RxUtils.dispose(mRedPacketRain);
        RxUtils.dispose(mRedResultDisposable);
        RxUtils.dispose(mRobDisposable);
        RxUtils.dispose(mRedPacketPHB);
        RxUtils.dispose(mListDisposable);


        mRedPacketDisposable = null;
        mTimeDisposable = null;
        mRedPacketRain = null;
        mRedResultDisposable = null;
        mRobDisposable = null;
        mRedPacketPHB = null;
        mListDisposable = null;
    }

    @Override
    public void exit() {
        release();
        mRouter.switchRedPacketUI(false, null);
    }

    @Override
    public void release() {
        RxUtils.dispose(mRedPacketDisposable);
        RxUtils.dispose(mTimeDisposable);
        RxUtils.dispose(mRedPacketRain);
        RxUtils.dispose(mRedResultDisposable);
        RxUtils.dispose(mRobDisposable);
        RxUtils.dispose(mRedPacketPHB);
        RxUtils.dispose(mListDisposable);


        mRedPacketDisposable = null;
        mTimeDisposable = null;
        mRedPacketRain = null;
        mRedResultDisposable = null;
        mRobDisposable = null;
        mRedPacketPHB = null;
        mListDisposable = null;

        isRedPacketing = false;
        mTimeRob = 0;
        mLPRedPacketModel = null;

        mView.switchRedPacketStart(RedPacketContract.TYPE_REDPACKET_EXIT);
    }

    @Override
    public void updateRedPacket() {
        mRouter.updateRedPacket();
    }

    @Override
    public void destroy() {

    }

    @Override
    public int getScoreAmount() {
        return mScoreAmount;
    }

    @Override
    public boolean getRedPacketing() {
        return isRedPacketing;
    }

    @Override
    public void switchState(int type) {

        if (type == RedPacketContract.TYPE_REDPACKET_RANKING_LIST) {
            //显示排行榜
            if (mTopList == null)
                return;
            mView.switchRedPacketStart(RedPacketContract.TYPE_REDPACKET_RANKING_LIST);
            mView.switchRedPacketRankingList(mTopList.list);
            mScoreAmount = 0;
        }
    }

    class ExConsumer implements Consumer<LPShortResult> {

        @Override
        public void accept(LPShortResult lpShortResult) throws Exception {
            if (lpShortResult == null && !(lpShortResult.data instanceof JsonObject))
                return;

            LPLogger.d(TAG, "ExConsumer : accept" );

            RedPacketTopList list = LPJsonUtils.parseJsonObject((JsonObject) lpShortResult.data, RedPacketTopList.class);
            mTopList = list;
        }
    }
}
