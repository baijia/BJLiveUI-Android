package com.baijiayun.live.ui.speakerlist;

import android.text.TextUtils;

import com.baijiayun.bjyrtcengine.BJYRtcEventObserver;
import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.speakerlist.item.RemoteItem;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.context.LiveRoom;
import com.baijiayun.livecore.models.LPInteractionAwardModel;
import com.baijiayun.livecore.models.LPMediaModel;
import com.baijiayun.livecore.models.LPPresenterLossRateModel;
import com.baijiayun.livecore.models.LPUserModel;
import com.baijiayun.livecore.models.imodels.IMediaControlModel;
import com.baijiayun.livecore.models.imodels.IMediaModel;
import com.baijiayun.livecore.models.imodels.IUserModel;
import com.baijiayun.livecore.utils.LimitedQueue;

import java.util.HashMap;
import java.util.List;

import io.reactivex.android.schedulers.AndroidSchedulers;

/**
 * Created by Shubo on 2019-07-25.
 */
public class SpeakersPresenter implements SpeakersContract.Presenter {

    private LiveRoom liveRoom;
    private SpeakersContract.View view;
    private LiveRoomRouterListener roomRouterListener;
    private HashMap<String, Integer> awardRecord;
    private LimitedQueue<Double> presenterUpLinkLossRateQueue;
    private boolean isPresenterVideoOn;

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
                        awardRecord.putAll(awardModel.value.record);
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

        int packetLossDuration = liveRoom.getPartnerConfig().packetLossDuration;
        presenterUpLinkLossRateQueue = new LimitedQueue<>(packetLossDuration);
        //保存主讲上行丢包
        liveRoom.getObservableOfLPPresenterLossRate()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableHelper.DisposingObserver<LPPresenterLossRateModel>() {
                    @Override
                    public void onNext(LPPresenterLossRateModel lpPresenterLossRateModel) {
                        isPresenterVideoOn = lpPresenterLossRateModel.type == 1;
                        presenterUpLinkLossRateQueue.add((double) lpPresenterLossRateModel.rate);
                    }
                });

        liveRoom.getPlayer().getObservableOfDownLinkLossRate().toObservable()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new DisposableHelper.DisposingObserver<BJYRtcEventObserver.RemoteStreamStats>() {
                    @Override
                    public void onNext(BJYRtcEventObserver.RemoteStreamStats remoteStreamStats) {
                        if (liveRoom.getPresenterUser() != null && remoteStreamStats.uid.equals(liveRoom.getPresenterUser().getUserId())) {
                            double lossRate = isPresenterVideoOn ? remoteStreamStats.receivedVideoLostRate : remoteStreamStats.receivedAudioLossRate;
                            if (lossRate > presenterUpLinkLossRateQueue.getAverage() * 2) {
                                view.notifyNetworkStatus(remoteStreamStats.uid, lossRate);
                                return;
                            }
                        }
                        if (liveRoom == null || liveRoom.getPlayer() == null) return;
                        double lossRate = liveRoom.getPlayer().isVideoPlaying(remoteStreamStats.uid) ? remoteStreamStats.receivedVideoLostRate : remoteStreamStats.receivedAudioLossRate;
                        view.notifyNetworkStatus(remoteStreamStats.uid, lossRate);
                    }
                });

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
    public void requestAward(String number) {
        //noinspection ConstantConditions
        int count = awardRecord.get(number) != null ? awardRecord.get(number) : 0;
        awardRecord.put(number, ++count);
        liveRoom.requestAward(number, awardRecord);
    }

    @Override
    public int getAwardCount(String number) {
        //noinspection ConstantConditions
        return awardRecord.get(number) != null ? awardRecord.get(number) : 0;
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
