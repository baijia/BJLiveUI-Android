package com.baijiahulian.live.ui.speakerspanel;

import android.text.TextUtils;
import android.view.View;

import com.baijiahulian.live.ui.activity.LiveRoomRouterListener;
import com.baijiahulian.live.ui.ppt.MyPPTView;
import com.baijiahulian.live.ui.utils.RxUtils;
import com.baijiahulian.livecore.context.LPConstants;
import com.baijiahulian.livecore.context.LPError;
import com.baijiahulian.livecore.launch.LPEnterRoomNative;
import com.baijiahulian.livecore.models.LPInteractionAwardModel;
import com.baijiahulian.livecore.models.LPMediaModel;
import com.baijiahulian.livecore.models.LPUserModel;
import com.baijiahulian.livecore.models.LPVideoSizeModel;
import com.baijiahulian.livecore.models.imodels.IMediaControlModel;
import com.baijiahulian.livecore.models.imodels.IMediaModel;
import com.baijiahulian.livecore.models.imodels.IUserInModel;
import com.baijiahulian.livecore.models.imodels.IUserModel;
import com.baijiahulian.livecore.models.roomresponse.LPResRoomDocAllModel;
import com.baijiahulian.livecore.utils.LPBackPressureBufferedSubscriber;
import com.baijiahulian.livecore.utils.LPErrorPrintSubscriber;
import com.baijiahulian.livecore.utils.LPSubscribeObjectWithLastValue;
import com.baijiahulian.livecore.wrapper.LPPlayer;
import com.baijiahulian.livecore.wrapper.LPRecorder;
import com.baijiahulian.livecore.wrapper.listener.LPOnPlayReadyListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Func1;
import rx.functions.Func2;

import static com.baijiahulian.live.ui.speakerspanel.SpeakersContract.PPT_TAG;
import static com.baijiahulian.live.ui.speakerspanel.SpeakersContract.RECORD_TAG;
import static com.baijiahulian.live.ui.speakerspanel.SpeakersContract.VIEW_TYPE_APPLY;
import static com.baijiahulian.live.ui.speakerspanel.SpeakersContract.VIEW_TYPE_PPT;
import static com.baijiahulian.live.ui.speakerspanel.SpeakersContract.VIEW_TYPE_PRESENTER;
import static com.baijiahulian.live.ui.speakerspanel.SpeakersContract.VIEW_TYPE_RECORD;
import static com.baijiahulian.live.ui.speakerspanel.SpeakersContract.VIEW_TYPE_SPEAKER;
import static com.baijiahulian.live.ui.speakerspanel.SpeakersContract.VIEW_TYPE_VIDEO_PLAY;
import static com.baijiahulian.live.ui.utils.Precondition.checkNotNull;

/**
 * 发言者列表遵从{[PPT]---[主讲人视频|头像]---[自己视频]---[其他人视频]---[其他发言用户音频]---[请求发言用户]}的顺序
 * 如果全屏或没有对应的项目则不在此列表中
 * 如果老师不是主讲人并切没有被全屏则在其他人视频里
 * currentFullScreenTag 为当前全屏的tag VideoView为UserId
 * Created by Shubo on 2017/6/5.
 */

public class SpeakerPresenter implements SpeakersContract.Presenter {

    private final boolean disableSpeakQueuePlaceholder;
    private LiveRoomRouterListener routerListener;
    private SpeakersContract.View view;
    private LPSubscribeObjectWithLastValue<String> fullScreenKVO;
    private static final int MAX_VIDEO_COUNT = 6;
    private boolean autoPlayPresenterVideo = true, isInit = false;
    private LPOnPlayReadyListener lpOnPlayReadyListener;

    // 显示视频或发言用户的分段Map, key为SpeakerType， value为List，保存每个SpeakType下相应的tag或者userId;
    private LinkedHashMap<SpeakersType, ArrayList<String>> displayMap;
    private IUserModel tempUserIn;
    private HashMap<String, Integer> awardRecord = new HashMap<>();

    private Subscription subscriptionOfMediaNew, subscriptionOfMediaChange, subscriptionOfMediaClose,
            subscriptionSpeakApply, subscriptionSpeakResponse, subscriptionOfActiveUser, subscriptionOfFullScreen,
            subscriptionOfUserOut, subscriptionOfPresenterChange, subscriptionOfShareDesktopAndPlayMedia,
            subscriptionOfVideoSizeChange, subscriptionOfUserIn, subscriptionOfAutoFullScreenTeacher
            , subscriptionOfSwtichFullScreenTeacher, subscriptionOfAward, subscriptionOfClassEnd;

    public SpeakerPresenter(SpeakersContract.View view, boolean disableSpeakQueuePlaceholder) {
        this.view = view;
        this.disableSpeakQueuePlaceholder = disableSpeakQueuePlaceholder;
        fullScreenKVO = new LPSubscribeObjectWithLastValue<>(PPT_TAG);
        initDisplayMap();

    }

    @Override
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {
        routerListener = liveRoomRouterListener;
    }

    private int indexOfUserId(String userId) {
        if (TextUtils.isEmpty(userId))
            return -1;
        ArrayList<String> totalList = getList();
        int count = 0;
        for (String value : totalList) {
            if (userId.equals(value))
                return count;
            else
                count++;
        }
        return -1;
    }

    private ArrayList<String> getList() {
        Set<SpeakersType> sets = displayMap.keySet();
        ArrayList<String> totalList = new ArrayList<>();
        for (SpeakersType set : sets) {
            if (!displayMap.get(set).isEmpty())
                totalList.addAll(displayMap.get(set));
        }
        return totalList;
    }

    private SpeakersType getSpeakersType(String userId) {
        Set<SpeakersType> sets = displayMap.keySet();
        for (SpeakersType set : sets) {
            if (!displayMap.get(set).isEmpty()) {
                if (displayMap.get(set).contains(userId))
                    return set;
            }
        }
        return null;
    }

    @Override
    public void setPPTToFullScreen() {
        if (!PPT_TAG.equals(fullScreenKVO.getParameter())) {
            // if full screen shows someone's video, we will switch it with PPT,
            // we need all video and speaking in the list;
            View ppt = view.removeViewAt(indexOfUserId(PPT_TAG));
            View fullScreenView = routerListener.removeFullScreenView();
            String fullId = fullScreenKVO.getParameter();
            displayMap.get(SpeakersType.PPT).remove(PPT_TAG);
            if (!displayMap.get(SpeakersType.Presenter).isEmpty()) {
                // if last presenter is in the list (full screen not shows presenter's video)
                if (fullId.equals(RECORD_TAG)) {// full screen shows record
                    displayMap.get(SpeakersType.Record).clear();
                    displayMap.get(SpeakersType.Record).add(RECORD_TAG);
                } else {// full screen shows other's video
                    displayMap.get(SpeakersType.VideoPlay).add(fullId);
                }
                view.notifyViewAdded(fullScreenView, indexOfUserId(fullId));
            } else {
                // if full screen shows last presenter's video, then we switch it to presenter position;
                if (!routerListener.isTeacherOrAssistant()) {
                    displayMap.get(SpeakersType.Presenter).add(fullId);
                } else {
                    if (RECORD_TAG.equals(fullId)) {
                        displayMap.get(SpeakersType.Record).add(RECORD_TAG);
                    } else if (routerListener.getLiveRoom().getPresenterUser() != null && routerListener.getLiveRoom().getPresenterUser().getUserId().equals(fullId)) {
                        displayMap.get(SpeakersType.Presenter).add(fullId);
                    } else {
                        displayMap.get(SpeakersType.VideoPlay).add(fullId);
                        if (fullScreenView instanceof VideoView && getSpeakModel(fullId) != null)
                            ((VideoView) fullScreenView).setName(getSpeakModel(fullId).getUser().getName());
                    }
                }
                view.notifyViewAdded(fullScreenView, indexOfUserId(fullId));
            }
            routerListener.setFullScreenView(ppt);// set full screen to PPT
            fullScreenKVO.setParameterWithoutNotify(PPT_TAG);
        }
    }


    private void initDisplayMap() {
        displayMap = new LinkedHashMap<>();
        displayMap.put(SpeakersType.PPT, new ArrayList<String>(1));
        displayMap.put(SpeakersType.Presenter, new ArrayList<String>(1));
        displayMap.put(SpeakersType.Record, new ArrayList<String>(1));
        displayMap.put(SpeakersType.VideoPlay, new ArrayList<String>());
        displayMap.put(SpeakersType.Speaking, new ArrayList<String>());
        displayMap.put(SpeakersType.Applying, new ArrayList<String>());
    }

    private void initView() {

        lpOnPlayReadyListener = new LPOnPlayReadyListener() {
            @Override
            public void ready(int userId) {
                if (userId < 0) {
                    view.stopLoadingAnimation(-1);
                    return;
                }
                int pos = indexOfUserId(String.valueOf(userId));
                try {
                    if (pos >= 0)
                        view.stopLoadingAnimation(pos);
                    else if (fullScreenKVO.getParameter() != null && fullScreenKVO.getParameter().equals(String.valueOf(userId))) {
                        if (routerListener.getBackgroundContainer().getChildCount() > 0 && routerListener.getBackgroundContainer().getChildAt(0) instanceof VideoView)
                            ((VideoView) routerListener.getBackgroundContainer().getChildAt(0)).stopRotate();
                            ((VideoView) routerListener.getBackgroundContainer().getChildAt(0)).setAwardLayoutVisibility(View.GONE);
                    } else {
                        view.stopLoadingAnimation(-1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    view.stopLoadingAnimation(-1);
                }
            }
        };

        routerListener.getLiveRoom().getPlayer().setOnPlayReadyListener(lpOnPlayReadyListener);

        if (disableSpeakQueuePlaceholder)
            view.disableSpeakQueuePlaceholder();

        if (!PPT_TAG.equals(fullScreenKVO.getParameter())) {//
            displayMap.get(SpeakersType.PPT).add(PPT_TAG);
        }

        if (displayMap.get(SpeakersType.Presenter).isEmpty() && routerListener.getLiveRoom().getPresenterUser() != null &&
                !routerListener.getLiveRoom().getPresenterUser().getUserId().equals(routerListener.getLiveRoom().getCurrentUser().getUserId())) {
            displayMap.get(SpeakersType.Presenter).add(routerListener.getLiveRoom().getPresenterUser().getUserId());
        }

        if (routerListener.getLiveRoom().getRecorder().isVideoAttached() && displayMap.get(SpeakersType.Record).isEmpty()) {
            displayMap.get(SpeakersType.Record).add(RECORD_TAG);
        }

        for (IMediaModel model : routerListener.getLiveRoom().getSpeakQueueVM().getSpeakQueueList()) {
            if (routerListener.getLiveRoom().getPresenterUser() == null) {
                if (model.isVideoOn())
                    displayMap.get(SpeakersType.VideoPlay).add(model.getUser().getUserId());
                else
                    displayMap.get(SpeakersType.Speaking).add(model.getUser().getUserId());
            } else {
                if (!model.getUser().getUserId().equals(routerListener.getLiveRoom().getPresenterUser().getUserId()) && model.isVideoOn())
                    displayMap.get(SpeakersType.VideoPlay).add(model.getUser().getUserId());
                else if (!model.getUser().getUserId().equals(routerListener.getLiveRoom().getPresenterUser().getUserId()) && !model.isVideoOn())
                    displayMap.get(SpeakersType.Speaking).add(model.getUser().getUserId());
            }
        }

        for (IUserModel model : routerListener.getLiveRoom().getSpeakQueueVM().getApplyList()) {
            // add other Speaking users
            displayMap.get(SpeakersType.Speaking).add(model.getUserId());
        }
        // init view
        for (int i = 0; i < getList().size(); i++) {
            view.notifyItemInserted(i, null);
            if (getItemViewType(i) != VIEW_TYPE_SPEAKER)
                continue;
            if (getSpeakModel(i).isVideoOn())
                playVideo(getSpeakModel(i).getUser().getUserId());
        }
    }

    @Override
    public void subscribe() {

        LPErrorPrintSubscriber<List<IMediaModel>> activeUserSubscriber = new LPErrorPrintSubscriber<List<IMediaModel>>() {
            @Override
            public void call(List<IMediaModel> iMediaModels) {
                initView();
                isInit = true;
            }
        };
        subscriptionOfActiveUser = routerListener.getLiveRoom().getSpeakQueueVM().getObservableOfActiveUsers().observeOn(AndroidSchedulers.mainThread()).subscribe(activeUserSubscriber);
        routerListener.getLiveRoom().getSpeakQueueVM().requestActiveUsers();


        subscriptionOfMediaNew = routerListener.getLiveRoom().getSpeakQueueVM().getObservableOfMediaNew()
                .subscribe(new LPErrorPrintSubscriber<IMediaModel>() {
                    @Override
                    public void call(IMediaModel iMediaModel) {
                        if (!iMediaModel.isVideoOn() && !iMediaModel.isAudioOn())
                            return; // 兼容iOS下课发media_publish
                        if (routerListener.getLiveRoom().getPresenterUser() != null
                                && iMediaModel.getUser().getUserId().equals(routerListener.getLiveRoom().getPresenterUser().getUserId())) {
                            //if presenter's info comes
                            if (getSpeakersType(iMediaModel.getUser().getUserId()) == SpeakersType.Presenter) {
                                //if presenter already exists in the list, update it.
                                int presenterPosition = indexOfUserId(iMediaModel.getUser().getUserId());
                                if (view.getChildAt(presenterPosition) != null) {
                                    if (iMediaModel.isVideoOn())
                                        view.notifyItemChanged(presenterPosition, iMediaModel);
                                } else { // if presenter has its position in the view, update view
                                    view.notifyItemInserted(presenterPosition, null);
                                }
                            } else {
                                //if presenter is not in the list, add new presenter
//                                if (fullScreenKVO.getParameter() != null && iMediaModel.getUser().getUserId().equals(fullScreenKVO.getParameter())){
//
//                                }
                                if (!displayMap.get(SpeakersType.Presenter).isEmpty()) {
                                    int pos = indexOfUserId(displayMap.get(SpeakersType.Presenter).get(0));
                                    view.notifyItemDeleted(pos);
                                    displayMap.get(SpeakersType.Presenter).clear();
                                }
                                displayMap.get(SpeakersType.Presenter).add(iMediaModel.getUser().getUserId());
                                int presenterPosition = indexOfUserId(iMediaModel.getUser().getUserId());
                                view.notifyItemInserted(presenterPosition, null);
                            }
                            printSections();
                            return;
                        }
                        // otherwise add new speaking
                        displayMap.get(SpeakersType.Speaking).add(iMediaModel.getUser().getUserId());
                        int presenterPosition = indexOfUserId(iMediaModel.getUser().getUserId());

                        if (routerListener.getLiveRoom().getCurrentUser().getType() == LPConstants.LPUserType.Teacher) {
                            view.notifyItemInserted(presenterPosition, null);
                        } else {
                            if (view.getChildAt(presenterPosition) != null)
                                view.notifyItemChanged(presenterPosition, iMediaModel);
                            else
                                view.notifyItemInserted(presenterPosition, null);
                        }
                        if (iMediaModel.isVideoOn())
                            playVideo(iMediaModel.getUser().getUserId());
                        printSections();
                    }
                });

//        subscriptionOfDebugVideo = routerListener.getLiveRoom().getPublishSubjectOfDebugVideo()
//                .observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new LPErrorPrintSubscriber<String>() {
//                    @Override
//                    public void call(String s) {
//                        if ("isVideoOn".equals(s)){
//                            displayMap.get(SpeakersType.Record).clear();
//                            displayMap.get(SpeakersType.Record).add(RECORD_TAG);
//                            view.notifyItemInserted(indexOfUserId(RECORD_TAG));
//                        }else if ("isVideoOff".equals(s)){
//                            if (!displayMap.get(SpeakersType.Record).isEmpty()) {
//                                view.notifyItemDeleted(indexOfUserId(RECORD_TAG));
//                                displayMap.get(SpeakersType.Record).clear();
//                            }
//                        }
//                    }
//                });

        subscriptionOfMediaChange = routerListener.getLiveRoom().getSpeakQueueVM().getObservableOfMediaChange()
                .subscribe(new LPErrorPrintSubscriber<IMediaModel>() {
                    @Override
                    public void call(IMediaModel iMediaModel) {
                        if (fullScreenKVO.getParameter() != null && fullScreenKVO.getParameter().equals(iMediaModel.getUser().getUserId())) {
                            // if full screen video need change
                            if (!iMediaModel.isVideoOn()) {
                                //we need close full screen video
                                fullScreenKVO.setParameter(null);
                                if (routerListener.getLiveRoom().getPresenterUser() != null
                                        && iMediaModel.getUser().getUserId().equals(routerListener.getLiveRoom().getPresenterUser().getUserId())) {
                                    displayMap.get(SpeakersType.Presenter).clear();
                                    displayMap.get(SpeakersType.Presenter).add(iMediaModel.getUser().getUserId());
                                    view.notifyItemInserted(indexOfUserId(iMediaModel.getUser().getUserId()), null);
                                } else {
                                    routerListener.getLiveRoom().getPlayer().replay(iMediaModel.getUser().getUserId());
                                    displayMap.get(SpeakersType.Speaking).add(iMediaModel.getUser().getUserId());
                                    view.notifyItemInserted(indexOfUserId(iMediaModel.getUser().getUserId()), null);
                                }
                            } else {
                                if (iMediaModel.skipRelease() != 1)
                                    routerListener.getLiveRoom().getPlayer().replay(iMediaModel.getUser().getUserId());
                            }
                            return;
                        }
                        int position = indexOfUserId(iMediaModel.getUser().getUserId());
                        if (position == -1) { // 未在speaker列表
                            return;
                        }
                        SpeakersType speakersType = getSpeakersType(iMediaModel.getUser().getUserId());
                        if (speakersType == SpeakersType.Presenter) {
                            view.notifyItemChanged(position, iMediaModel);
                            if (iMediaModel.isVideoOn() && routerListener.getLiveRoom().getDocListVM().getDocList() != null && routerListener.getLiveRoom().getDocListVM().getDocList().size() == 1)
                                fullScreenKVO.setParameter(iMediaModel.getUser().getUserId());
                            return;
                        }
                        if (speakersType == SpeakersType.VideoPlay) {  // 视频打开用户
                            if (!iMediaModel.isVideoOn()) { // 通知视频关闭
                                View removedView = view.notifyItemDeleted(position);
                                // core 处理了playAVCLose
                                displayMap.get(SpeakersType.VideoPlay).remove(iMediaModel.getUser().getUserId());
                                displayMap.get(SpeakersType.Speaking).add(iMediaModel.getUser().getUserId());
                                view.notifyItemInserted(indexOfUserId(iMediaModel.getUser().getUserId()), removedView);
                            } else { // 重播
                                if (iMediaModel.skipRelease() != 1)
                                    view.notifyItemChanged(position, iMediaModel);
                            }
                        } else if (speakersType == SpeakersType.Speaking) { // 视频未打开用户
//                            view.notifyItemChanged(position, iMediaModel);
                            if (iMediaModel.isVideoOn())
                                playVideo(iMediaModel.getUser().getUserId());
                            else
                                getPlayer().playAudio(iMediaModel.getUser().getUserId());
                        } else {
                            throw new RuntimeException("position > _displayApplySection");
                        }
                        printSections();
                    }
                });

        subscriptionOfMediaClose = routerListener.getLiveRoom().getSpeakQueueVM().getObservableOfMediaClose()
                .subscribe(new LPErrorPrintSubscriber<IMediaModel>() {
                    @Override
                    public void call(IMediaModel iMediaModel) {
                        if (iMediaModel.getUser().getUserId() != null && iMediaModel.getUser().getUserId().equals(fullScreenKVO.getParameter())) {
                            // full screen user
                            fullScreenKVO.setParameter(null);
                            if (routerListener.getLiveRoom().getPresenterUser() != null
                                    && iMediaModel.getUser().getUserId().equals(routerListener.getLiveRoom().getPresenterUser().getUserId())) {
                                displayMap.get(SpeakersType.Presenter).clear();
                                displayMap.get(SpeakersType.Presenter).add(0, iMediaModel.getUser().getUserId());
                                view.notifyItemInserted(indexOfUserId(iMediaModel.getUser().getUserId()), null);
                            }
                            return;
                        }
                        int position = indexOfUserId(iMediaModel.getUser().getUserId());
                        if (position == -1) {
                            return;
                        }
                        SpeakersType speakersType = getSpeakersType(iMediaModel.getUser().getUserId());
                        if (speakersType == SpeakersType.Presenter) {
                            view.notifyItemChanged(position, iMediaModel);
                        } else if (speakersType == SpeakersType.Speaking) {
                            view.notifyItemDeleted(position);
                            displayMap.get(SpeakersType.Speaking).remove(iMediaModel.getUser().getUserId());
                        } else if (speakersType == SpeakersType.VideoPlay) {
                            view.notifyItemDeleted(position);
//                            getPlayer().playAVClose(iMediaModel.getUser().getUserId()); core 处理了 AVClose
                            displayMap.get(SpeakersType.VideoPlay).remove(iMediaModel.getUser().getUserId());
                        }
                    }
                });

        subscriptionOfUserIn = routerListener.getLiveRoom().getObservableOfUserIn().observeOn(AndroidSchedulers.mainThread())
                .subscribe(new LPErrorPrintSubscriber<IUserInModel>() {
                    @Override
                    public void call(IUserInModel iUserInModel) {
                        tempUserIn = iUserInModel.getUser();
                    }
                });

        subscriptionOfPresenterChange = routerListener.getLiveRoom().getSpeakQueueVM().getObservableOfPresenterChange()
                .filter(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String s) {
//                        return (!displayMap.get(SpeakersType.Presenter).isEmpty() || !fullScreenKVO.getParameter().equals(PPT_TAG));
                        return isInit;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new LPErrorPrintSubscriber<String>() {
                    @Override
                    public void call(String newPresenter) {
                        if (TextUtils.isEmpty(newPresenter)) {
                            // presenter is null
                            return;
                        }
                        setPPTToFullScreen();

                        if (routerListener.isTeacherOrAssistant()) {
                            if (displayMap.get(SpeakersType.Presenter).isEmpty()) {
                                if (newPresenter.equals(routerListener.getLiveRoom().getCurrentUser().getUserId()))
                                    return;
                                int switchIndex = indexOfUserId(newPresenter);
                                if (switchIndex < 0) {
                                    displayMap.get(SpeakersType.Presenter).add(newPresenter);
                                    view.notifyItemInserted(0, null);
                                    return;
                                }
                                View removedView = null;
                                if (displayMap.get(SpeakersType.VideoPlay).contains(newPresenter)) {
                                    removedView = view.notifyItemDeleted(switchIndex);
                                    displayMap.get(SpeakersType.VideoPlay).remove(newPresenter);

                                } else if (displayMap.get(SpeakersType.Speaking).contains(newPresenter)) {
                                    removedView = view.notifyItemDeleted(switchIndex);
                                    displayMap.get(SpeakersType.Speaking).remove(newPresenter);
                                }
                                displayMap.get(SpeakersType.Presenter).add(newPresenter);
                                view.notifyItemInserted(0, removedView);
                            } else {
                                String lastPresenter = displayMap.get(SpeakersType.Presenter).get(0);
                                if (newPresenter.equals(lastPresenter)) return;

                                int switchIndex = indexOfUserId(newPresenter);
                                View removedView = null;
                                if (switchIndex >= 0) {
                                    if (displayMap.get(SpeakersType.VideoPlay).contains(newPresenter)) {
                                        removedView = view.notifyItemDeleted(switchIndex);
                                        displayMap.get(SpeakersType.VideoPlay).remove(newPresenter);

                                    } else if (displayMap.get(SpeakersType.Speaking).contains(newPresenter)) {
                                        removedView = view.notifyItemDeleted(switchIndex);
                                        displayMap.get(SpeakersType.Speaking).remove(newPresenter);
                                    }
                                }
                                View removePresenterView = null;
                                if (newPresenter.equals(routerListener.getLiveRoom().getCurrentUser().getUserId())) {
                                    removePresenterView = view.notifyItemDeleted(0);
                                    displayMap.get(SpeakersType.Presenter).clear();
                                    view.showToast("您成为了主讲");
                                } else {
                                    displayMap.get(SpeakersType.Presenter).clear();
                                    displayMap.get(SpeakersType.Presenter).add(newPresenter);
                                    removePresenterView = view.notifyItemDeleted(0);
                                    view.notifyItemInserted(0, removedView);
                                    view.showToast(getSpeakModel(newPresenter).getUser().getName() + "成为了主讲");
                                }

                                IMediaModel lastPresenterModel = getSpeakModel(lastPresenter);
                                if (lastPresenterModel != null && (lastPresenterModel.isVideoOn() || lastPresenterModel.isAudioOn())) {
                                    if (lastPresenterModel.isVideoOn()) {
                                        displayMap.get(SpeakersType.VideoPlay).add(lastPresenter);
                                        view.notifyItemInserted(indexOfUserId(lastPresenter), removePresenterView);
                                    } else if (lastPresenterModel.isAudioOn()) {
                                        displayMap.get(SpeakersType.Speaking).add(lastPresenter);
                                        view.notifyItemInserted(indexOfUserId(lastPresenter), removePresenterView);
                                    }
                                }
                            }
                            return;
                        }

                        if (displayMap.get(SpeakersType.Presenter).isEmpty()) return;

                        if (!displayMap.get(SpeakersType.Presenter).isEmpty()) { // presenter is not empty
                            String lastPresenter = displayMap.get(SpeakersType.Presenter).get(0);
                            if (TextUtils.isEmpty(lastPresenter)) {
                                return;
                            }

                            if (indexOfUserId(newPresenter) < 0) {
                                //new presenter has no position in the list
                                displayMap.get(SpeakersType.Presenter).clear();
                                displayMap.get(SpeakersType.Presenter).add(newPresenter);// add new presenter to the displayMap

                                View removedPresenterView = view.notifyItemDeleted(0);
                                view.notifyItemInserted(0, null); // update presenter view, new presenter position ~= 0;

                                IMediaModel lastModel = getSpeakModel(lastPresenter);
                                if (lastModel != null && (lastModel.isAudioOn() || lastModel.isVideoOn())) {
                                    // if new presenter's camera or mic is on, we will insert a new position for him
                                    if (lastModel.isVideoOn()) {
                                        if (!displayMap.get(SpeakersType.VideoPlay).contains(lastPresenter)) {
                                            displayMap.get(SpeakersType.VideoPlay).add(lastPresenter);
                                            view.notifyItemInserted(indexOfUserId(lastPresenter), removedPresenterView);
                                        }
                                    } else if (!lastModel.isVideoOn()) {
                                        if (!displayMap.get(SpeakersType.Speaking).contains(lastPresenter)) {
                                            displayMap.get(SpeakersType.Speaking).add(lastPresenter);
                                            view.notifyItemInserted(indexOfUserId(lastPresenter), removedPresenterView);
                                        }
                                    }
                                }
                            } else {
                                // new presenter was at list
                                int switchIndex = indexOfUserId(newPresenter); //  find out new presenter's last position

                                // switch presenter in displayMap
                                displayMap.get(SpeakersType.Presenter).clear();
                                displayMap.get(SpeakersType.Presenter).add(newPresenter);

//                                View removePresenterView = view.notifyItemDeleted(0);
//                                view.notifyItemInserted(0, null);

                                IMediaModel lastSpeakModel = getSpeakModel(lastPresenter);

                                if (lastSpeakModel != null && (lastSpeakModel.isVideoOn() || lastSpeakModel.isAudioOn())) {
                                    // if last presenter's camera or mic is on, it will replace new presenter's position in the list
                                    if (lastSpeakModel.isVideoOn()) {
                                        if (displayMap.get(SpeakersType.VideoPlay).contains(newPresenter)) {
                                            int newPresenterIndex = displayMap.get(SpeakersType.VideoPlay).indexOf(newPresenter);
                                            displayMap.get(SpeakersType.VideoPlay).set(newPresenterIndex, lastPresenter);
                                            View removeView = view.notifyItemDeleted(switchIndex);

                                            View removePresenterView = view.notifyItemDeleted(0);
                                            view.notifyItemInserted(0, removeView);
                                            view.notifyItemInserted(switchIndex, removePresenterView);
                                        } else if (displayMap.get(SpeakersType.Speaking).contains(newPresenter)) {
                                            View removePresenterView = view.notifyItemDeleted(switchIndex);
                                            displayMap.get(SpeakersType.Speaking).remove(newPresenter);
                                            displayMap.get(SpeakersType.VideoPlay).add(lastPresenter);
                                            view.notifyItemInserted(indexOfUserId(lastPresenter), null);

                                            view.notifyItemDeleted(0);
                                            view.notifyItemInserted(0, removePresenterView);
                                        }
                                    } else {
                                        if (displayMap.get(SpeakersType.Speaking).contains(newPresenter)) {
                                            int newPresenterIndex = displayMap.get(SpeakersType.Speaking).indexOf(newPresenter);
                                            displayMap.get(SpeakersType.Speaking).set(newPresenterIndex, lastPresenter);
                                            view.notifyItemChanged(switchIndex, null);

                                            view.notifyItemDeleted(0);
                                            view.notifyItemInserted(0, null);
                                        } else if (displayMap.get(SpeakersType.VideoPlay).contains(newPresenter)) {
                                            View removePresenterView = view.notifyItemDeleted(switchIndex);
                                            displayMap.get(SpeakersType.VideoPlay).remove(newPresenter);
                                            displayMap.get(SpeakersType.Speaking).add(lastPresenter);
                                            view.notifyItemInserted(indexOfUserId(lastPresenter), null);

                                            view.notifyItemDeleted(0);
                                            view.notifyItemInserted(0, removePresenterView);
                                        } else {
                                            throw new RuntimeException("new presenter not in the speakList");
                                        }
                                    }

                                } else {
                                    // last presenter's camera and mic is off, simply delete new presenter's last position
                                    if (switchIndex != 0 && getSpeakersType(newPresenter) != SpeakersType.Record)
                                        view.notifyItemDeleted(switchIndex);
                                    if (displayMap.get(SpeakersType.VideoPlay).contains(newPresenter)) {
                                        displayMap.get(SpeakersType.VideoPlay).remove(newPresenter);
//                                        getPlayer().playAVClose(newPresenter);
                                    } else if (displayMap.get(SpeakersType.Speaking).contains(newPresenter))
                                        displayMap.get(SpeakersType.Speaking).remove(newPresenter);

                                    view.notifyItemDeleted(0);
                                    view.notifyItemInserted(0, null);
                                }
                            }
                        } else {
                            displayMap.get(SpeakersType.Presenter).add(newPresenter);
                            view.notifyItemInserted(indexOfUserId(newPresenter), null);
                        }
                        view.showToast(getSpeakModel(newPresenter).getUser().getName() + "成为了主讲");
                        printSections();
                    }
                });

        subscriptionOfVideoSizeChange = routerListener.getLiveRoom().getPlayer().getObservableOfVideoSizeChange()
                .onBackpressureBuffer()
                .observeOn(AndroidSchedulers.mainThread())
                .filter(new Func1<LPVideoSizeModel, Boolean>() {
                    @Override
                    public Boolean call(LPVideoSizeModel lpVideoSizeModel) {
                        return routerListener.getLiveRoom().getPresenterUser() != null &&
                                lpVideoSizeModel.userId.equals(routerListener.getLiveRoom().getPresenterUser().getUserId());
                    }
                })
                .subscribe(new LPBackPressureBufferedSubscriber<LPVideoSizeModel>() {
                    @Override
                    public void call(LPVideoSizeModel lpVideoSizeModel) {
                        if (lpVideoSizeModel.userId.equals(fullScreenKVO.getParameter())) {
                            routerListener.resizeFullScreenWaterMark(lpVideoSizeModel.height, lpVideoSizeModel.width);
                        } else if (indexOfUserId(lpVideoSizeModel.userId) != -1) {
                            view.notifyPresenterVideoSizeChange(indexOfUserId(lpVideoSizeModel.userId),
                                    lpVideoSizeModel.height, lpVideoSizeModel.width);
                        }
                    }
                });

        // 主讲人退出教室
        subscriptionOfUserOut = routerListener.getLiveRoom()
                .getObservableOfUserOut()
                .observeOn(AndroidSchedulers.mainThread())
                .filter(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String s) {
                        return routerListener.getLiveRoom().getPresenterUser() != null &&
                                routerListener.getLiveRoom().getPresenterUser().getUserId().equals(s); // 主讲人退出教室
                    }
                })
                .subscribe(new LPErrorPrintSubscriber<String>() {
                    @Override
                    public void call(String s) {
                        if (s.equals(fullScreenKVO.getParameter())) {
//                            fullScreenKVO.setParameter(null);
                            printSections();
                            return;
                        }
                        if (indexOfUserId(s) < 0) return;
                        if (getSpeakersType(s) == SpeakersType.Presenter) {
                            view.notifyItemDeleted(indexOfUserId(s));
                            getPlayer().playAVClose(s);
                            displayMap.get(SpeakersType.Presenter).clear();
                        }
                        printSections();
                    }
                });

        subscriptionOfShareDesktopAndPlayMedia = routerListener.getLiveRoom().
                getObservableOfPlayMedia().mergeWith(routerListener.getLiveRoom().
                getObservableOfShareDesktop())
                .filter(new Func1<Boolean, Boolean>() {
                    @Override
                    public Boolean call(Boolean aBoolean) {
                        // 不是老师都自动全屏
                        return routerListener.getLiveRoom().getCurrentUser() != null
                                && routerListener.getLiveRoom().getCurrentUser().getType() != LPConstants.LPUserType.Teacher;
                    }
                })
                .filter(new Func1<Boolean, Boolean>() {
                    @Override
                    public Boolean call(Boolean aBoolean) {
                        return aBoolean &&
                                routerListener.getLiveRoom().getPresenterUser() != null &&
                                routerListener.getLiveRoom().getTeacherUser() != null &&
                                routerListener.getLiveRoom().getPresenterUser().getUserId().equals(
                                        routerListener.getLiveRoom().getTeacherUser().getUserId());
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new LPErrorPrintSubscriber<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        if (routerListener.getLiveRoom().getTeacherUser() == null)
                            return;
                        String teacherId = routerListener.getLiveRoom().getTeacherUser().getUserId();
                        if (!fullScreenKVO.getParameter().equals(teacherId)
                                && getSpeakModel(teacherId) != null
                                && getSpeakModel(teacherId).isVideoOn() && autoPlayPresenterVideo) {
                            fullScreenKVO.setParameter(teacherId);
                        }
                    }
                });

        subscriptionOfSwtichFullScreenTeacher = Observable.zip(routerListener.getLiveRoom().getObservableOfClassStart(),
                routerListener.getLiveRoom().getSpeakQueueVM().getObservableOfMediaNew(), new Func2<Void, IMediaModel, IMediaModel>() {
                    @Override
                    public IMediaModel call(Void v, IMediaModel iMediaModel) {
                        return iMediaModel;
                    }
                })
                .filter(new Func1<IMediaModel, Boolean>() {
                    @Override
                    public Boolean call(IMediaModel iMediaModel) {
                        return iMediaModel.getUser().getType() == LPConstants.LPUserType.Teacher && iMediaModel.isVideoOn() && routerListener.getLiveRoom().getTeacherUser() != null;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new LPErrorPrintSubscriber<IMediaModel>() {
                    @Override
                    public void call(IMediaModel iMediaModel) {
                        if (routerListener.getLiveRoom().getDocListVM().getDocList() != null &&
                                routerListener.getLiveRoom().getDocListVM().getDocList().size() == 1 && fullScreenKVO.getParameter() != null &&
                                !fullScreenKVO.getParameter().equals(routerListener.getLiveRoom().getTeacherUser().getUserId())) {
                            String teacherId = routerListener.getLiveRoom().getTeacherUser().getUserId();

                            if (!TextUtils.isEmpty(teacherId) && getSpeakModel(teacherId) != null) {

//                                fullScreenKVO.setParameter(teacherId);
                            }

                        }
                    }
                });


        subscriptionOfAutoFullScreenTeacher = Observable
                .zip(routerListener.getLiveRoom().getSpeakQueueVM().getObservableOfActiveUsers(),
                        routerListener.getLiveRoom().getDocListVM().getObservableOfDocAll(), new Func2<List<IMediaModel>, LPResRoomDocAllModel, Boolean>() {
                            @Override
                            public Boolean call(List<IMediaModel> iMediaModels, LPResRoomDocAllModel lpResRoomDocAllModel) {
                                boolean needFullScreenTeacherVideo = false;
                                for (IMediaModel mediaModel : iMediaModels) {
                                    if (mediaModel.getUser().getType() == LPConstants.LPUserType.Teacher &&
                                            mediaModel.isVideoOn()) {
                                        needFullScreenTeacherVideo = true;
                                    }
                                }
                                return lpResRoomDocAllModel.docList.size() == 1 && needFullScreenTeacherVideo;
                            }
                        })
                .filter(new Func1<Boolean, Boolean>() {
                    @Override
                    public Boolean call(Boolean aBoolean) {
                        return aBoolean;
                    }
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new LPErrorPrintSubscriber<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        String teacherId = routerListener.getLiveRoom().getTeacherUser().getUserId();

                        if (!TextUtils.isEmpty(teacherId) && !teacherId.equals(fullScreenKVO.getParameter())
                                && getSpeakModel(teacherId) != null
                                && getSpeakModel(teacherId).isVideoOn()) {
                            fullScreenKVO.setParameter(teacherId);
                        }
                    }
                });

        subscriptionOfFullScreen = fullScreenKVO.newObservableOfParameterChanged()
                .filter(new Func1<String, Boolean>() {
                    @Override
                    public Boolean call(String s) {
                        return s == null || !s.equals(fullScreenKVO.getLastParameter());
                    }
                }).subscribe(new LPBackPressureBufferedSubscriber<String>() {
                    @Override
                    public void call(String s) {
                        if (TextUtils.isEmpty(s)) {
                            // full screen ppt
                            fullScreenKVO.setParameter(PPT_TAG);
                        } else {
                            String lastTag = fullScreenKVO.getLastParameter();
                            String tag = fullScreenKVO.getParameter();

                            View view1 = routerListener.removeFullScreenView();
                            View view2 = view.removeViewAt(indexOfUserId(tag));

                            if (view1 == null || view2 == null) return;
                            routerListener.setFullScreenView(view2);

                            if (PPT_TAG.equals(tag)) {
                                displayMap.get(SpeakersType.PPT).clear();
                            } else if (routerListener.getLiveRoom().getPresenterUser() != null
                                    && tag.equals(routerListener.getLiveRoom().getPresenterUser().getUserId())) {
                                displayMap.get(SpeakersType.Presenter).clear();
                            } else if (RECORD_TAG.equals(tag)) {
                                displayMap.get(SpeakersType.Record).clear();
                            } else if (tag != null) { // video
                                displayMap.get(SpeakersType.VideoPlay).remove(tag);
                            }

                            if (!TextUtils.isEmpty(lastTag)) {
                                if (PPT_TAG.equals(lastTag)) {
                                    displayMap.get(SpeakersType.PPT).clear();
                                    displayMap.get(SpeakersType.PPT).add(lastTag);
                                } else if (routerListener.getLiveRoom().getPresenterUser() != null
                                        && lastTag.equals(routerListener.getLiveRoom().getPresenterUser().getUserId())) {
                                    displayMap.get(SpeakersType.Presenter).clear();
                                    displayMap.get(SpeakersType.Presenter).add(lastTag);
                                } else if (RECORD_TAG.equals(lastTag)) {
                                    displayMap.get(SpeakersType.Record).clear();
                                    displayMap.get(SpeakersType.Record).add(lastTag);
                                } else { // video
                                    displayMap.get(SpeakersType.VideoPlay).add(lastTag);
                                }
                                view.notifyViewAdded(view1, indexOfUserId(lastTag));
                            }

                        }
                        printSections();
                    }
                });

        if (routerListener.isTeacherOrAssistant()) {

            subscriptionSpeakApply = routerListener.getLiveRoom().getSpeakQueueVM().getObservableOfSpeakApply()
                    .onBackpressureBuffer(1000)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new LPErrorPrintSubscriber<IMediaModel>() {
                        @Override
                        public void call(IMediaModel iMediaModel) {
                            try {
                                displayMap.get(SpeakersType.Applying).add(iMediaModel.getUser().getUserId());
                                view.notifyItemInserted(indexOfUserId(iMediaModel.getUser().getUserId()), null);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    });

            subscriptionSpeakResponse = routerListener.getLiveRoom().getSpeakQueueVM().getObservableOfSpeakResponse()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new LPErrorPrintSubscriber<IMediaControlModel>() {
                        @Override
                        public void call(IMediaControlModel iMediaControlModel) {
                            int position = indexOfUserId(iMediaControlModel.getUser().getUserId());
                            if (position < getList().size() && position != -1) {
                                view.notifyItemDeleted(position);
                                displayMap.get(SpeakersType.Applying).remove(iMediaControlModel.getUser().getUserId());
                            } else {
                                throw new RuntimeException("position > displayList.size()");
                            }
                        }
                    });
        }
        subscriptionOfAward = routerListener.getLiveRoom()
                .getObservableOfAward()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new LPErrorPrintSubscriber<LPInteractionAwardModel>() {
                    @Override
                    public void call(LPInteractionAwardModel awardModel) {
                        awardRecord.putAll(awardModel.value.record);
                        if(awardModel.isFromCache && awardModel.value.record != null){
                            for(Map.Entry<String,Integer> entry : awardModel.value.record.entrySet()){
                                IMediaModel iMediaModel = getSpeakModelWithUserNumber(entry.getKey());
                                notifyAward(iMediaModel, entry.getValue(), false);
                            }
                        } else{
                            IMediaModel iMediaModel = getSpeakModelWithUserNumber(awardModel.value.to);
                            notifyAward(iMediaModel, awardModel.value.record.get(awardModel.value.to), true);
                        }
                    }
                });

        subscriptionOfClassEnd = routerListener.getLiveRoom()
                .getObservableOfClassEnd()
                .mergeWith(routerListener.getLiveRoom().getObservableOfClassSwitch())
                .subscribe(new LPErrorPrintSubscriber<Void>() {
                    @Override
                    public void call(Void aVoid) {
                        awardRecord.clear();
                    }
                });
    }

    private void notifyAward(IMediaModel iMediaModel, int awardCount, boolean isShowAwardAnimation){
        if(iMediaModel != null){
            int index = indexOfUserId(iMediaModel.getUser().getUserId());
            if(index == -1 && iMediaModel.getUser().getNumber().equals(getCurrentUser().getNumber())){
                index = indexOfUserId(RECORD_TAG);
            }
            view.notifyAwardCountChange(index, awardCount);
            if(isShowAwardAnimation){
                routerListener.showAwardAnimation(iMediaModel.getUser().getName());
            }
        }
    }

    @Override
    public void unSubscribe() {
        RxUtils.unSubscribe(subscriptionOfMediaNew);
        RxUtils.unSubscribe(subscriptionOfMediaChange);
        RxUtils.unSubscribe(subscriptionOfMediaClose);
        RxUtils.unSubscribe(subscriptionSpeakApply);
        RxUtils.unSubscribe(subscriptionSpeakResponse);
        RxUtils.unSubscribe(subscriptionOfActiveUser);
        RxUtils.unSubscribe(subscriptionOfFullScreen);
        RxUtils.unSubscribe(subscriptionOfUserIn);
        RxUtils.unSubscribe(subscriptionOfUserOut);
        RxUtils.unSubscribe(subscriptionOfPresenterChange);
        RxUtils.unSubscribe(subscriptionOfVideoSizeChange);
        RxUtils.unSubscribe(subscriptionOfShareDesktopAndPlayMedia);
        RxUtils.unSubscribe(subscriptionOfAutoFullScreenTeacher);
        RxUtils.unSubscribe(subscriptionOfAward);
        RxUtils.unSubscribe(subscriptionOfSwtichFullScreenTeacher);
        RxUtils.unSubscribe(subscriptionOfClassEnd);
    }

    @Override
    public void destroy() {
        view = null;
        routerListener = null;
        displayMap.clear();
    }

    @Override
    public IUserModel getPresenter() {
        return routerListener.getLiveRoom().getPresenterUser();
    }

    @Override
    public void requestPresenterChange(String userId, boolean isSet) {
        if (!clickableCheck()) {
            view.showToast("请勿频繁切换");
            return;
        }
        if (RECORD_TAG.equals(userId) || !isSet)
            routerListener.getLiveRoom().getSpeakQueueVM().requestSwitchPresenter(routerListener.getLiveRoom().getCurrentUser().getUserId());
        else
            routerListener.getLiveRoom().getSpeakQueueVM().requestSwitchPresenter(userId);
    }

    @Override
    public boolean isHasDrawingAuth(String userId) {
        IMediaModel model = getSpeakModel(userId);
        if (model == null) return false;
        return routerListener.getLiveRoom().getSpeakQueueVM().getStudentsDrawingAuthList().contains(model.getUser().getNumber());
    }

    @Override
    public void requestStudentDrawingAuth(String userId, boolean isAddAuth) {
        IMediaModel model = getSpeakModel(userId);
        if (model == null) return;
        LPError lpError = routerListener.getLiveRoom().getSpeakQueueVM().requestStudentDrawingAuthChange(isAddAuth, model.getUser().getNumber());
//        if (lpError == null && isAddAuth){
//            view.showToast("授权画笔成功");
//        }else if (lpError == null && !isAddAuth){
//            view.showToast("取消画笔成功");
//        }else if ( isAddAuth){
//            view.showToast("授权画笔失败");
//        }else if ( !isAddAuth){
//            view.showToast("取消画笔失败");
//        }
    }

    @Override
    public boolean isEnableGrantDrawing() {
        return routerListener.getLiveRoom().getPartnerConfig().liveDisableGrantStudentBrush == 0;
    }

    @Override
    public boolean isEnableSwitchPresenter() {
        return routerListener.getLiveRoom().getPartnerConfig().isEnableSwitchPresenter == 1;
    }

    @Override
    public int getItemViewType(int position) {

        if (position < 0 || position >= getList().size())
            return -1;
        SpeakersType speakersType = getSpeakersType(getList().get(position));
        if (speakersType == SpeakersType.PPT)
            return VIEW_TYPE_PPT;
        else if (speakersType == SpeakersType.Presenter)
            return VIEW_TYPE_PRESENTER;
        else if (speakersType == SpeakersType.Record)
            return VIEW_TYPE_RECORD;
        else if (speakersType == SpeakersType.VideoPlay)
            return VIEW_TYPE_VIDEO_PLAY;
        else if (speakersType == SpeakersType.Speaking)
            return VIEW_TYPE_SPEAKER;
        else
            return VIEW_TYPE_APPLY;
    }

    @Override
    public int getItemViewType(String userId) {
        return getItemViewType(indexOfUserId(userId));
    }

    @Override
    public LPRecorder getRecorder() {
        return routerListener.getLiveRoom().getRecorder();
    }

    @Override
    public LPPlayer getPlayer() {
        return routerListener.getLiveRoom().getPlayer();
    }

    @Override
    public IUserModel getApplyModel(int position) {
        String userId = getList().get(position);
        for (IUserModel model : routerListener.getLiveRoom().getSpeakQueueVM().getApplyList()) {
            if (model.getUserId().equals(userId)) {
                return model;
            }
        }
        return null;
    }

    @Override
    public IMediaModel getSpeakModel(String userId) {
        if (TextUtils.isEmpty(userId)) return null;
        for (IMediaModel model : routerListener.getLiveRoom().getSpeakQueueVM().getSpeakQueueList()) {
            if (model.getUser().getUserId().equals(userId)) {
                return model;
            }
        }
        // presenter mismatching
        if (routerListener.getLiveRoom().getPresenterUser() != null &&
                userId.equals(routerListener.getLiveRoom().getPresenterUser().getUserId())) {
            LPMediaModel model = new LPMediaModel();
            model.user = (LPUserModel) routerListener.getLiveRoom().getPresenterUser();
            return model;
        }

        if (tempUserIn != null && tempUserIn.getUserId().equals(userId)) {
            LPMediaModel model = new LPMediaModel();
            model.user = (LPUserModel) tempUserIn;
            return model;
        }
        // mismatching
        LPMediaModel model = new LPMediaModel();
        model.user = (LPUserModel) routerListener.getLiveRoom().getOnlineUserVM().getUserById(userId);
        return model;
    }

    private IMediaModel getSpeakModelWithUserNumber(String userNumber) {
        if (TextUtils.isEmpty(userNumber)) return null;
        if(userNumber.equals(routerListener.getLiveRoom().getCurrentUser().getNumber())){
            LPMediaModel model = new LPMediaModel();
            model.user = (LPUserModel) routerListener.getLiveRoom().getCurrentUser();
            return model;
        }
        for (IMediaModel model : routerListener.getLiveRoom().getSpeakQueueVM().getSpeakQueueList()) {
            if (model.getUser().getNumber().equals(userNumber)) {
                return model;
            }
        }
        // presenter mismatching
        if (routerListener.getLiveRoom().getPresenterUser() != null &&
                userNumber.equals(routerListener.getLiveRoom().getPresenterUser().getNumber())) {
            LPMediaModel model = new LPMediaModel();
            model.user = (LPUserModel) routerListener.getLiveRoom().getPresenterUser();
            return model;
        }

        if (tempUserIn != null && tempUserIn.getNumber().equals(userNumber)) {
            LPMediaModel model = new LPMediaModel();
            model.user = (LPUserModel) tempUserIn;
            return model;
        }
        return null;
    }

    @Override
    public IMediaModel getSpeakModel(int position) {
        String userId = getList().get(position);
        return getSpeakModel(userId);
    }

    @Override
    public void playVideo(String userId) {
        int position = indexOfUserId(userId);

        if (position == -1) return;
        if (getSpeakModel(position) == null || !getSpeakModel(position).isVideoOn()) return;

        if (displayMap.get(SpeakersType.Presenter).contains(userId)) {
            autoPlayPresenterVideo = true;
            view.notifyItemChanged(position, null);
            return;
        }

        IMediaModel model = getSpeakModel(fullScreenKVO.getParameter());
        boolean isFullScreenStudentVideo = model.getUser() != null && model.getUser().getType() == LPConstants.LPUserType.Student;

        if (displayMap.get(SpeakersType.Presenter).size() + displayMap.get(SpeakersType.VideoPlay).size() + (isFullScreenStudentVideo ? 1 : 0) >= MAX_VIDEO_COUNT) {
            view.showMaxVideoExceed();
            return;
        }
        if (displayMap.get(SpeakersType.Speaking).contains(userId)) {
            view.notifyItemDeleted(position);
            displayMap.get(SpeakersType.Speaking).remove(userId);
        }

        if (displayMap.get(SpeakersType.VideoPlay).contains(userId)) {
            view.notifyItemChanged(position, null);
            return;
        }
        displayMap.get(SpeakersType.VideoPlay).add(userId);
        view.notifyItemInserted(indexOfUserId(userId), null);
    }

    @Override
    public void closeVideo(String tag) {
        if (TextUtils.isEmpty(tag)) return;
        if (tag.equals(RECORD_TAG)) {
            if (routerListener.getLiveRoom().getRecorder().isVideoAttached()) {
                routerListener.detachLocalVideo();
                if (routerListener.getLiveRoom().getRecorder().isPublishing()) {
                    routerListener.getLiveRoom().getRecorder().stopPublishing();
                }
            }
            return;
        } else if (tag.equals(PPT_TAG)) {
            throw new RuntimeException("close PPT? wtf");
        }
        int position = indexOfUserId(tag);

        // 在dialog操作过程中 数据发生了变化
        if (position == -1) return;
        IMediaModel model = getSpeakModel(position);
        if (model == null) return;

        if (displayMap.get(SpeakersType.Presenter).contains(tag)) { // presenter
            autoPlayPresenterVideo = false;
            view.notifyItemChanged(position, null);
            return;
        }

        routerListener.getLiveRoom().getPlayer().playAVClose(tag);
        routerListener.getLiveRoom().getPlayer().playAudio(tag);
        if (displayMap.get(SpeakersType.VideoPlay).contains(tag)) {
            view.notifyItemDeleted(position);
            displayMap.get(SpeakersType.VideoPlay).remove(tag);

            displayMap.get(SpeakersType.Speaking).add(tag);
            view.notifyItemInserted(indexOfUserId(tag), null);
        }
    }

    @Override
    public void switchVideoDefinition(String userId, LPConstants.VideoDefinition definition) {
        int position = indexOfUserId(userId);
        if (position == -1) return;
        if (definition == null) return;
        routerListener.getLiveRoom().getPlayer().changeVideoDefinition(userId, definition);
    }

    @Override
    public void closeSpeaking(String userId) {
        int position = indexOfUserId(userId);
        if (position == -1) return;
        routerListener.getLiveRoom().getSpeakQueueVM().closeOtherSpeak(userId);
    }

    @Override
    public boolean isTeacherOrAssistant() {
        return routerListener.isTeacherOrAssistant();
    }

    @Override
    public boolean isCurrentTeacher() {
        return routerListener.isCurrentUserTeacher();
    }


    @Override
    public void changeBackgroundContainerSize(boolean isShrink) {
        routerListener.changeBackgroundContainerSize(isShrink);
    }

    @Override
    public void setFullScreenTag(String tag) {
        fullScreenKVO.setParameter(tag);
    }

    @Override
    public MyPPTView getPPTFragment() {
        return routerListener.getPPTView();
    }

    @Override
    public boolean isFullScreen(String tag) {
        checkNotNull(tag);
        return tag.equals(fullScreenKVO.getParameter());
    }

    @Override
    public void switchCamera() {
        routerListener.getLiveRoom().getRecorder().switchCamera();
    }

    @Override
    public void switchPrettyFilter() {
        if (getRecorder().isBeautyFilterOn()) {
            getRecorder().closeBeautyFilter();
        } else {
            getRecorder().openBeautyFilter();
        }
    }

    private boolean isScreenCleared = false;

    @Override
    public void clearScreen() {
        isScreenCleared = !isScreenCleared;
        if (isScreenCleared) routerListener.clearScreen();
        else routerListener.unClearScreen();
    }

    @Override
    public boolean isAutoPlay() {
        return autoPlayPresenterVideo;
    }

    @Override
    public LPEnterRoomNative.LPWaterMark getWaterMark() {
        return routerListener.getLiveRoom().getPartnerConfig().waterMark;
    }


    @Override
    public String getItem(int position) {
        if (position < getList().size())
            return getList().get(position);
        else
            throw new RuntimeException("position > displayList.size() in getItem");
    }

    @Override
    public int getCount() {
        return getList().size();
    }

    @Override
    public void agreeSpeakApply(String userId) {
        int position = indexOfUserId(userId);
        if (position == -1) {
            throw new RuntimeException("invalid userId:" + userId + " in agreeSpeakApply");
        } else {
            routerListener.getLiveRoom().getSpeakQueueVM().agreeSpeakApply(userId);
        }
    }

    @Override
    public void disagreeSpeakApply(String userId) {
        int position = indexOfUserId(userId);
        if (position == -1) {
            throw new RuntimeException("invalid userId:" + userId + " in disagreeSpeakApply");
        } else {
            routerListener.getLiveRoom().getSpeakQueueVM().disagreeSpeakApply(userId);
        }
    }

    public void attachVideo() {
        if (routerListener.checkCameraPermission()) {
            if (displayMap.get(SpeakersType.Record).isEmpty()) {
                displayMap.get(SpeakersType.Record).add(RECORD_TAG);
                view.notifyItemInserted(indexOfUserId(RECORD_TAG), null);
            } else {
                view.notifyItemChanged(indexOfUserId(RECORD_TAG), null);
            }
        }
        printSections();
    }

    @Override
    public boolean isStopPublish() {
        return isStopPublish;
    }

    @Override
    public void setIsStopPublish(boolean b) {
        isStopPublish = b;
    }

    @Override
    public boolean isMultiClass() {
        return routerListener.getRoomType() == LPConstants.LPRoomType.Multi;
    }

    @Override
    public void notifyNoSpeakers() {
        routerListener.showNoSpeakers();
    }

    @Override
    public void notifyHavingSpeakers() {
        routerListener.showHavingSpeakers();
    }


    private boolean isStopPublish = false;

    public void detachVideo() {
        if (RECORD_TAG.equals(fullScreenKVO.getParameter())) {
            if (getRecorder().isVideoAttached())
                getRecorder().detachVideo();
            if (getRecorder().isPublishing())
                getRecorder().stopPublishing();
            fullScreenKVO.setParameter(null);
            return;
        }
        if (displayMap == null) return;
        if (!displayMap.get(SpeakersType.Record).isEmpty()) {
            view.notifyItemDeleted(indexOfUserId(RECORD_TAG));
            displayMap.get(SpeakersType.Record).clear();
        }
        printSections();
    }

    public boolean isPPTInSpeakersList() {
        return displayMap != null && !displayMap.get(SpeakersType.PPT).isEmpty();
    }

    public void showOptionDialog() {
        view.showOptionDialog();
    }

    private void printSections() {
//        LPLogger.i("section: " + _displayPresenterSection + " " + _displayRecordSection + " " + _displayVideoSection + " " +
//                _displaySpeakerSection + " " + _displayApplySection);
    }

    private Subscription subscriptionOfClickable;

    private boolean clickableCheck() {
        if (subscriptionOfClickable != null && !subscriptionOfClickable.isUnsubscribed()) {
            return false;
        }
        subscriptionOfClickable = Observable.timer(2, TimeUnit.SECONDS).subscribe(new LPErrorPrintSubscriber<Long>() {
            @Override
            public void call(Long aLong) {
                RxUtils.unSubscribe(subscriptionOfClickable);
            }
        });
        return true;
    }

    @Override
    public void requestAward(String userNumber) {
        awardRecord.put(userNumber, (awardRecord.get(userNumber) == null) ? 1 : awardRecord.get(userNumber) + 1);
        routerListener.getLiveRoom().requestAward(userNumber, awardRecord);
    }

    @Override
    public int getRewardCount(String userNumber) {
        if(TextUtils.isEmpty(userNumber)){
            return 0;
        }
        return awardRecord.get(userNumber) == null ? 0 : awardRecord.get(userNumber);
    }

    @Override
    public IUserModel getCurrentUser() {
        return routerListener.getLiveRoom().getCurrentUser();
    }

}
