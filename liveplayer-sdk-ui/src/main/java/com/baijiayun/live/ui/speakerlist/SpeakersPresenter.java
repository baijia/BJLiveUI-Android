package com.baijiayun.live.ui.speakerlist;

import android.text.TextUtils;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.speakerlist.item.RemoteItem;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.context.LiveRoom;
import com.baijiayun.livecore.models.LPAwardUserInfo;
import com.baijiayun.livecore.models.LPInteractionAwardModel;
import com.baijiayun.livecore.models.LPMediaModel;
import com.baijiayun.livecore.models.LPUserModel;
import com.baijiayun.livecore.models.imodels.IMediaControlModel;
import com.baijiayun.livecore.models.imodels.IMediaModel;
import com.baijiayun.livecore.models.imodels.IUserModel;
import com.baijiayun.livecore.utils.LPLogger;
import com.baijiayun.livecore.utils.LimitedQueue;

import java.util.HashMap;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

/**
 * Created by Shubo on 2019-07-25.
 */
public class SpeakersPresenter implements SpeakersContract.Presenter {

    private static final String TAG = "SpeakersPresenter";

    private LiveRoom liveRoom;
    private SpeakersContract.View view;
    private LiveRoomRouterListener roomRouterListener;
    private HashMap<String, LPAwardUserInfo> awardRecord;
    private LimitedQueue<Double> upLinkLossRateQueue, downLinkLossRateQueue;
    private boolean isPresenterVideoOn;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    public SpeakersPresenter(SpeakersContract.View view) {
        this.view = view;
        awardRecord = new HashMap<>();
    }

    @Override
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {
        roomRouterListener = liveRoomRouterListener;
        liveRoom = liveRoomRouterListener.getLiveRoom();
    }

    @Override
    public void subscribe() {
        liveRoom.getSpeakQueueVM().getObservableOfActiveUsers().observeOn(AndroidSchedulers.mainThread()).subscribe(new DisposableHelper.DisposingObserver<List<IMediaModel>>() {
            @Override
            public void onNext(List<IMediaModel> iMediaModels) {
                for (IMediaModel mediaModel : iMediaModels) {
                    view.notifyRemotePlayableChanged(mediaModel);
                    if (!mediaModel.hasExtraStreams()) {
                        continue;
                    }
                    for (IMediaModel extMediaModel : mediaModel.getExtraStreams()) {
                        if (extMediaModel.getMediaSourceType() == LPConstants.MediaSourceType.ExtCamera ||
                                extMediaModel.getMediaSourceType() == LPConstants.MediaSourceType.ExtScreenShare) {
                            view.notifyRemotePlayableChanged(extMediaModel);
                        }
                    }
                }
            }
        });

        liveRoom.getSpeakQueueVM().getObservableOfMediaPublish().observeOn(AndroidSchedulers.mainThread()).subscribe(new DisposableHelper.DisposingObserver<IMediaModel>() {
            @Override
            public void onNext(IMediaModel iMediaModel) {
                view.notifyRemotePlayableChanged(iMediaModel);
            }
        });

        // 老师播放媒体文件和屏幕分享自动全屏
        liveRoom.getObservableOfPlayMedia().mergeWith(liveRoom.getObservableOfShareDesktop())
                .filter(aBoolean -> liveRoom.getCurrentUser() != null && liveRoom.getCurrentUser().getType() != LPConstants.LPUserType.Teacher)
                .filter(aBoolean -> aBoolean && liveRoom.getPresenterUser() != null && liveRoom.getTeacherUser() != null && TextUtils.equals(liveRoom.getPresenterUser().getUserId(), liveRoom.getTeacherUser().getUserId()))
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableHelper.DisposingObserver<Boolean>() {
                    @Override
                    public void onNext(Boolean aBoolean) {
                        view.notifyPresenterDesktopShareAndMedia(aBoolean);
                    }
                });

        liveRoom.getSpeakQueueVM().getObservableOfPresenterChange().toObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableHelper.DisposingObserver<String>() {
                    @Override
                    public void onNext(String s) {
                        IMediaModel defaultMediaModel = null;
                        for (IMediaModel mediaModel : liveRoom.getSpeakQueueVM().getSpeakQueueList()) {
                            if (mediaModel.getUser().getUserId().equals(s)) {
                                defaultMediaModel = mediaModel;
                                break;
                            }
                        }
                        if (defaultMediaModel == null) {
                            defaultMediaModel = new LPMediaModel();
                        }
                        if (defaultMediaModel.getUser() == null) {
                            IUserModel userModel = liveRoom.getOnlineUserVM().getUserById(s);
                            if (userModel == null) {
                                LPUserModel fakeUser = new LPUserModel();
                                fakeUser.userId = s;
                                userModel = fakeUser;
                            }
                            ((LPMediaModel) defaultMediaModel).user = (LPUserModel) userModel;
                        }
                        view.notifyPresenterChanged(s, defaultMediaModel);
                    }
                });

        liveRoom.getObservableOfAward().observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableHelper.DisposingObserver<LPInteractionAwardModel>() {
                    @Override
                    public void onNext(LPInteractionAwardModel awardModel) {
                        awardRecord.clear();
                        awardRecord.putAll(awardModel.value.getRecordAward());
                        view.notifyAward(awardModel);
                    }
                });

        liveRoom.getObservableOfClassEnd().mergeWith(liveRoom.getObservableOfClassSwitch())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableHelper.DisposingObserver<Integer>() {
                    @Override
                    public void onNext(Integer integer) {
                        awardRecord.clear();
                    }
                });

        /**
         * ATTENTION 主讲丢包广播发送太频繁，已去掉。声网暂无丢包回调，AVSDK暂无上行丢包
         * 丢包逻辑如下
         * 统计平均10s的丢包情况。时间间隔和丢包等级均走配置
         * 无下行数据时，直接根据上行丢包显示到自己窗口
         * 如果拉流的丢包率大于自己上行丢包的2倍，认为是拉流端的丢包过高导致，这个时候需在拉流学员对应的窗口显示文案提示
         * 关于两倍的逻辑，解释如下
         * 学生A的下行丢包=学生A的上行丢包（现在没这个数据，所以拿学生B的上行丢包等价了） + 学生B拉学生A产生的下行丢包
         */

        int packetLossDuration = liveRoom.getPartnerConfig().packetLossDuration;
        //上行丢包队列
//        upLinkLossRateQueue = new LimitedQueue<>(packetLossDuration);
        //下行丢包队列
        downLinkLossRateQueue = new LimitedQueue<>(packetLossDuration);
        //保存主讲上行丢包.注意：广播太频繁已停发
//        liveRoom.getObservableOfLPPresenterLossRate()
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new DisposableHelper.DisposingObserver<LPPresenterLossRateModel>() {
//                    @Override
//                    public void onNext(LPPresenterLossRateModel lpPresenterLossRateModel) {
//                        isPresenterVideoOn = lpPresenterLossRateModel.type == 1;
//                        upLinkLossRateQueue.add((double) lpPresenterLossRateModel.rate);
//                    }
//                });

        compositeDisposable.add(liveRoom.getPlayer().getObservableOfDownLinkLossRate()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(remoteStreamStats -> {
                    if (liveRoom == null || liveRoom.getPlayer() == null) return;
                    //广播太频繁已停发
//                    if (liveRoom.getPresenterUser() != null && remoteStreamStats.uid.equals(liveRoom.getPresenterUser().getUserId())) {
//                        double lossRate = isPresenterVideoOn ? remoteStreamStats.receivedVideoLostRate : remoteStreamStats.receivedAudioLossRate;
//                        if (lossRate > upLinkLossRateQueue.getAverage() * 2) {
//                            view.notifyNetworkStatus(remoteStreamStats.uid, lossRate);
//                            return;
//                        }
//                    }
                    downLinkLossRateQueue.add(liveRoom.getPlayer().isVideoPlaying(remoteStreamStats.uid) ? remoteStreamStats.receivedVideoLostRate : remoteStreamStats.receivedAudioLossRate);
                    //AVSDK暂无上行丢包，直接在对应窗口显示丢包
                    view.notifyNetworkStatus(remoteStreamStats.uid, downLinkLossRateQueue.getAverage());
                }));

        if (liveRoom.isTeacherOrAssistant()) {
            subscribeTeacherEvent();
        } else {
            subscribeStudentEvent();
        }

        liveRoom.getSpeakQueueVM().requestActiveUsers();
    }

    private void subscribeTeacherEvent() {
        liveRoom.getSpeakQueueVM().getObservableOfSpeakApply().observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableHelper.DisposingObserver<IMediaModel>() {
                    @Override
                    public void onNext(IMediaModel iMediaModel) {
                        view.showSpeakApply(iMediaModel);
                    }
                });

        liveRoom.getSpeakQueueVM().getObservableOfSpeakResponse().observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableHelper.DisposingObserver<IMediaControlModel>() {
                    @Override
                    public void onNext(IMediaControlModel iMediaControlModel) {
                        view.removeSpeakApply(iMediaControlModel.getUser().getUserId());
                    }
                });
    }

    private void subscribeStudentEvent() {
    }

    @Override
    public void agreeSpeakApply(String userId) {
        liveRoom.getSpeakQueueVM().agreeSpeakApply(userId);
    }

    @Override
    public void disagreeSpeakApply(String userId) {
        liveRoom.getSpeakQueueVM().disagreeSpeakApply(userId);
    }

    @Override
    public LiveRoomRouterListener getRouterListener() {
        return roomRouterListener;
    }

    @Override
    public void requestAward(IUserModel userModel) {
        LPAwardUserInfo awardUserInfo = awardRecord.get(userModel.getNumber());
        if(awardUserInfo == null){
            awardUserInfo = new LPAwardUserInfo(userModel.getName(), userModel.getType().getType(), 0);
        }
        awardUserInfo.count++;
        awardRecord.put(userModel.getNumber(), awardUserInfo);
        liveRoom.requestAward(userModel.getNumber(), awardRecord);
    }

    @Override
    public int getAwardCount(String number) {
        return awardRecord.get(number) != null ? awardRecord.get(number).count : 0;
    }

    @Override
    public void handleUserCloseAction(RemoteItem remoteItem) {
        view.notifyUserCloseAction(remoteItem);
    }

    @Override
    public void closeSpeaking(String userId) {
        liveRoom.getSpeakQueueVM().closeOtherSpeak(userId);
    }

    @Override
    public void unSubscribe() {
        DisposableHelper.dispose();
        compositeDisposable.clear();
    }

    @Override
    public void destroy() {
        awardRecord.clear();
    }

    public void attachVideo() {
        if (roomRouterListener.checkCameraPermission()) {
            view.notifyLocalPlayableChanged(true, liveRoom.getRecorder().isAudioAttached());
        }
    }

    public void attachVideoForce() {
        view.notifyLocalPlayableChanged(true, liveRoom.getRecorder().isAudioAttached());
    }

    public void detachVideo() {
        view.notifyLocalPlayableChanged(false, liveRoom.getRecorder().isAudioAttached());
    }

    @Override
    public void localShowAwardAnimation(String userNumber) {
        if (roomRouterListener.getLiveRoom().getCurrentUser().getNumber().equals(userNumber)) {
            roomRouterListener.showAwardAnimation(roomRouterListener.getLiveRoom().getCurrentUser().getName());
        }
    }
}
