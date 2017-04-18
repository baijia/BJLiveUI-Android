package com.baijiahulian.live.ui.speakqueue;

import com.baijiahulian.live.ui.activity.LiveRoomRouterListener;
import com.baijiahulian.live.ui.utils.RxUtils;
import com.baijiahulian.livecore.models.imodels.IMediaControlModel;
import com.baijiahulian.livecore.models.imodels.IMediaModel;
import com.baijiahulian.livecore.models.imodels.IUserModel;
import com.baijiahulian.livecore.utils.LPErrorPrintSubscriber;

import java.util.Iterator;
import java.util.List;

import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;

/**
 * Created by Shubo on 2017/4/16.
 */

public class SpeakQueuePresenter implements SpeakQueueContract.Presenter {

    private SpeakQueueContract.View view;
    private LiveRoomRouterListener routerListener;
    private List<IMediaModel> speakList;
    private List<IUserModel> applyList;
    private Subscription subscriptionOfMediaNew, subscriptionOfMediaChange, subscriptionOfMediaClose;
    private Subscription subscriptionSpeakApply, subscriptionSpeakResponse;

    public SpeakQueuePresenter(SpeakQueueContract.View view) {
        this.view = view;
    }

    @Override
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {
        routerListener = liveRoomRouterListener;
    }

    @Override
    public void subscribe() {
        speakList = routerListener.getLiveRoom().getSpeakQueueVM().getSpeakQueueList();
        applyList = routerListener.getLiveRoom().getSpeakQueueVM().getApplyList();
        subscriptionOfMediaNew = routerListener.getLiveRoom().getSpeakQueueVM().getObservableOfMediaNew()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new LPErrorPrintSubscriber<IMediaModel>() {
                    @Override
                    public void call(IMediaModel iMediaModel) {
                        speakList.add(iMediaModel);
                        view.notifyItemInserted(applyList.size() + speakList.size());
                    }
                });
        subscriptionOfMediaChange = routerListener.getLiveRoom().getSpeakQueueVM().getObservableOfMediaChange()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new LPErrorPrintSubscriber<IMediaModel>() {
                    @Override
                    public void call(IMediaModel iMediaModel) {
                        for (int i = 0; i < speakList.size(); i++) {
                            if (speakList.get(i).getUser().getUserId().equals(iMediaModel.getUser().getUserId())) {
                                speakList.set(i, iMediaModel);
                                view.notifyItemChanged(i + applyList.size());
                            }
                        }
                    }
                });
        subscriptionOfMediaClose = routerListener.getLiveRoom().getSpeakQueueVM().getObservableOfMediaClose()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new LPErrorPrintSubscriber<IMediaModel>() {
                    @Override
                    public void call(IMediaModel iMediaModel) {
                        Iterator<IMediaModel> iterator = speakList.iterator();
                        int counter = 0;
                        while (iterator.hasNext()) {
                            IMediaModel model = iterator.next();
                            counter++;
                            if (model.getUser().getUserId().equals(iMediaModel.getUser().getUserId())) {
                                iterator.remove();
                                break;
                            }
                        }
                        view.notifyItemDeleted(counter + applyList.size());
                    }
                });

        if (routerListener.isTeacherOrAssistant()) {
            subscriptionSpeakApply = routerListener.getLiveRoom().getSpeakQueueVM().getObservableOfSpeakApply()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new LPErrorPrintSubscriber<IMediaModel>() {
                        @Override
                        public void call(IMediaModel iMediaModel) {
                            applyList.add(iMediaModel.getUser());
                        }
                    });
            subscriptionSpeakResponse = routerListener.getLiveRoom().getSpeakQueueVM().getObservableOfSpeakResponse()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new LPErrorPrintSubscriber<IMediaControlModel>() {
                        @Override
                        public void call(IMediaControlModel iMediaControlModel) {
                            Iterator<IUserModel> iterator = applyList.iterator();
                            int counter = 0;
                            while (iterator.hasNext()) {
                                IUserModel model = iterator.next();
                                counter++;
                                if (model.getUserId().equals(iMediaControlModel.getUser().getUserId())) {
                                    iterator.remove();
                                    break;
                                }
                            }
                            view.notifyItemDeleted(counter);
                        }
                    });
        }
    }

    @Override
    public void unSubscribe() {
        RxUtils.unSubscribe(subscriptionOfMediaNew);
        RxUtils.unSubscribe(subscriptionOfMediaChange);
        RxUtils.unSubscribe(subscriptionOfMediaClose);
        RxUtils.unSubscribe(subscriptionSpeakApply);
        RxUtils.unSubscribe(subscriptionSpeakResponse);
    }

    @Override
    public void destroy() {
        routerListener = null;
        view = null;
    }

    @Override
    public int getCount() {
        return speakList.size() + applyList.size();
    }

    @Override
    public Object getItem(int position) {
        if (position < applyList.size()) {
            return applyList.get(position);
        } else if (position < applyList.size() + speakList.size()) {
            return speakList.get(position - applyList.size());
        }
        return null;
    }

    @Override
    public void agreeSpeakApply(int position) {
        routerListener.getLiveRoom().getSpeakQueueVM().agreeSpeakApply(applyList.get(position).getUserId());
    }

    @Override
    public void disagreeSpeakApply(int position) {
        routerListener.getLiveRoom().getSpeakQueueVM().disagreeSpeakApply(applyList.get(position).getUserId());
    }

    @Override
    public void closeSpeaking(int position) {
        routerListener.getLiveRoom().getSpeakQueueVM().closeOtherSpeak(speakList.get(position - applyList.size()).getUser().getUserId());
    }

    @Override
    public void openVideo(int position) {
        routerListener.playVideo(speakList.get(position - applyList.size()).getUser().getUserId());
    }

    @Override
    public void closeVideo(int position) {
        String userId = speakList.get(position - applyList.size()).getUser().getUserId();
        routerListener.playVideoClose(userId);
        routerListener.getLiveRoom().getPlayer().playAudio(userId);
    }

    @Override
    public boolean isCurrentVideoPlayingUser(int position) {
        return speakList.get(position - applyList.size()).getUser().getUserId().equals(routerListener.getCurrentVideoPlayingUserId());
    }

    @Override
    public boolean isTeacherOrAssistant() {
        return routerListener.isTeacherOrAssistant();
    }
}