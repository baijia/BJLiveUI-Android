package com.baijiayun.live.ui.speakerlist;

import android.arch.lifecycle.LifecycleObserver;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.LinearLayout;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.base.BaseFragment;
import com.baijiayun.live.ui.speakerlist.item.ApplyItem;
import com.baijiayun.live.ui.speakerlist.item.LocalItem;
import com.baijiayun.live.ui.speakerlist.item.Playable;
import com.baijiayun.live.ui.speakerlist.item.RemoteItem;
import com.baijiayun.live.ui.speakerlist.item.SpeakItem;
import com.baijiayun.live.ui.speakerlist.item.Switchable;
import com.baijiayun.live.ui.utils.DisplayUtils;
import com.baijiayun.live.ui.viewsupport.BJTouchHorizontalScrollView;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.models.LPAwardUserInfo;
import com.baijiayun.livecore.models.LPInteractionAwardModel;
import com.baijiayun.livecore.models.imodels.IMediaModel;
import com.baijiayun.livecore.utils.CommonUtils;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Created by Shubo on 2019-07-25.
 */
public class SpeakersFragment extends BaseFragment implements SpeakersContract.View {

    private static final int SHRINK_THRESHOLD = 3;

    private SpeakersContract.Presenter presenter;

    private LiveRoomRouterListener routerListener;

    private ItemPositionHelper positionHelper;

    private LinearLayout container;

    private boolean disableSpeakQueuePlaceholder = false;

    private BJTouchHorizontalScrollView scrollView;

    public SpeakersFragment() {
        positionHelper = new ItemPositionHelper();
    }

    @Override
    public int getLayoutId() {
        return R.layout.bjy_fragment_speakers;
    }

    @Override
    public void setPresenter(SpeakersContract.Presenter presenter) {
        super.setBasePresenter(presenter);
        this.presenter = presenter;
        routerListener = presenter.getRouterListener();
        positionHelper.setRouterListener(routerListener);
    }

    @Override
    protected void init(Bundle savedInstanceState) {
        super.init(savedInstanceState);
        container = (LinearLayout) $.id(R.id.fragment_speakers_container).view();
        if (disableSpeakQueuePlaceholder)
            $.id(R.id.fragment_speakers_scroll_view).backgroundDrawable(null);
        scrollView = (BJTouchHorizontalScrollView) $.id(R.id.fragment_speakers_scroll_view).view();
        scrollView.setListener(() -> routerListener.getPPTView().getPPTEditMode() == LPConstants.PPTEditMode.ShapeMode);
        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (scrollView.getScrollX() == scrollView.getChildAt(0).getMeasuredWidth() - scrollView.getMeasuredWidth()) {
                if ($ != null)
                    $.id(R.id.fragment_speakers_new_request_toast).gone();
            }
        });
        $.id(R.id.fragment_speakers_new_request_toast).clicked(v -> {
            $.id(R.id.fragment_speakers_new_request_toast).gone();
            scrollView.fullScroll(View.FOCUS_RIGHT);
        });
        if (routerListener.getCurrentScreenOrientation() == Configuration.ORIENTATION_PORTRAIT) {
            setBackGroundVisible(true);
        } else {
            if (container.getChildCount() > SHRINK_THRESHOLD) {
                setBackGroundVisible(true);
            } else {
                setBackGroundVisible(false);
            }
        }
    }

    @Override
    public void notifyRemotePlayableChanged(IMediaModel iMediaModel) {
        SpeakItem item;
        if (iMediaModel.getMediaSourceType() == LPConstants.MediaSourceType.ExtCamera ||
                iMediaModel.getMediaSourceType() == LPConstants.MediaSourceType.ExtScreenShare) {
            item = positionHelper.getSpeakItemByIdentity(iMediaModel.getUser().getUserId() + "_1");
        } else {
            item = positionHelper.getSpeakItemByIdentity(iMediaModel.getUser().getUserId());
        }

        //防止信令丢失后助教出现奔溃问题（一般获取不到ApplyItem）
        if (item instanceof ApplyItem) {
            removeSpeakApply(item.getIdentity());
            item = null;
        }

        if (item == null) {
            item = createRemotePlayableItem(iMediaModel);
        }
        RemoteItem remoteItem = (RemoteItem) item;
        remoteItem.setMediaModel(iMediaModel);
        boolean isSpeakClosed = !remoteItem.hasAudio() && !remoteItem.hasVideo();
        if (!remoteItem.isVideoClosedByUser() || isSpeakClosed) {
            takeItemActions(positionHelper.processItemActions(item));
        }
        remoteItem.refreshPlayable();
    }

    @Override
    public void notifyLocalPlayableChanged(boolean isVideoOn, boolean isAudioOn) {
        SpeakItem item = positionHelper.getSpeakItemByIdentity(routerListener.getLiveRoom().getCurrentUser().getUserId());
        List<ItemPositionHelper.ItemAction> itemActions = null;
        if (item == null) {
            if (isVideoOn || isAudioOn) {
                item = createLocalPlayableItem();
                ((LocalItem) item).setShouldStreamVideo(isVideoOn);
                ((LocalItem) item).setShouldStreamAudio(isAudioOn);
                itemActions = positionHelper.processItemActions(item);
            }
        } else if (item instanceof LocalItem) {
            ((LocalItem) item).setShouldStreamVideo(isVideoOn);
            ((LocalItem) item).setShouldStreamAudio(isAudioOn);
            itemActions = positionHelper.processItemActions(item);
        }
        takeItemActions(itemActions);
        if (item instanceof LocalItem)
            ((LocalItem) item).refreshPlayable();
    }

    @Override
    public void notifyPresenterChanged(String userId, IMediaModel defaultMediaModel) {
        List<ItemPositionHelper.ItemAction> actionList;
        if (TextUtils.isEmpty(userId)) {
            actionList = positionHelper.processPresenterChangeItemActions(null);
        } else {
            SpeakItem speakItem = positionHelper.getSpeakItemByIdentity(userId);
            if (speakItem == null && !routerListener.getLiveRoom().getCurrentUser().getUserId().equals(userId)) {
                speakItem = createRemotePlayableItem(defaultMediaModel);
                actionList = positionHelper.processPresenterChangeItemActions(speakItem);
            } else if (speakItem == null && routerListener.getLiveRoom().getCurrentUser().getUserId().equals(userId)) {
                actionList = positionHelper.processUnActiveLocalPresenterItemActions();
            } else {
                actionList = positionHelper.processPresenterChangeItemActions(speakItem);
            }
        }
        takeItemActions(actionList);

        for (ItemPositionHelper.ItemAction action : actionList) {
            if (action.action == ItemPositionHelper.ActionType.ADD && action.speakItem instanceof LocalItem) {
                ((LocalItem) action.speakItem).invalidVideo();
            }
        }
    }

    @Override
    public void notifyUserCloseAction(RemoteItem remoteItem) {
        List<ItemPositionHelper.ItemAction> actionList = positionHelper.processUserCloseAction(remoteItem);
        takeItemActions(actionList);
    }

    private void takeItemActions(@Nullable List<ItemPositionHelper.ItemAction> actions) {
        if (actions == null) return;
        if (getContext() == null) return;
        for (ItemPositionHelper.ItemAction action : actions) {
            switch (action.action) {
                case ADD:
                    if (action.speakItem.getView() != null) {
                        final ViewParent parent = action.speakItem.getView().getParent();
                        if (parent != null) {
                            ((ViewGroup) parent).removeView(action.speakItem.getView());
                        }
                    }
                    //fix java.lang.IndexOutOfBoundsException: index=2 count=1
                    if(action.value <= container.getChildCount()){
                        container.addView(action.speakItem.getView(), action.value);
                    }
                    break;
                case REMOVE:
                    container.removeView(action.speakItem.getView());
                    if(action.speakItem instanceof LifecycleObserver){
                        getLifecycle().removeObserver((LifecycleObserver) action.speakItem);
                    }
                    break;
                case FULLSCREEN:
                    ((Switchable) action.speakItem).switchToFullScreen();
                    break;
            }
        }
        if (container.getChildCount() >= 1) {
            routerListener.showHavingSpeakers();
        } else {
            routerListener.showNoSpeakers();
        }
        routerListener.changeBackgroundContainerSize(container.getChildCount() > SHRINK_THRESHOLD);
    }

    @Override
    public void notifyPresenterDesktopShareAndMedia(boolean isDesktopShareAndMedia) {
        if (!isDesktopShareAndMedia) return;
        Switchable fullScreenItem = routerListener.getFullScreenItem();
        if (!fullScreenItem.getIdentity().equals(routerListener.getLiveRoom().getTeacherUser().getUserId())) {
            SpeakItem speakItem = positionHelper.getSpeakItemByIdentity(routerListener.getLiveRoom().getTeacherUser().getUserId());
            if (speakItem instanceof RemoteItem) {
                if (((RemoteItem) speakItem).isVideoClosedByUser()) return;
                fullScreenItem.switchBackToList();
                ((RemoteItem) speakItem).switchToFullScreen();
            }
        }
    }

    @Override
    public void showSpeakApply(IMediaModel iMediaModel) {
        SpeakItem item = positionHelper.getSpeakItemByIdentity(iMediaModel.getMediaId());
        if (item == null) {
            item = createApplyItem(iMediaModel);
            positionHelper.processItemActions(item);
            container.addView(item.getView());
        }
        showNewSpeakApplyHint();
    }

    private void showNewSpeakApplyHint() {
        int childCount = container.getChildCount();
        if (childCount == 0)
            return;
        int itemWidth = container.getChildAt(0).getWidth();
        if (scrollView.getWidth() < itemWidth * childCount) {
            $.id(R.id.fragment_speakers_new_request_toast).visible();
        }
    }

    @Override
    public void removeSpeakApply(String Identity) {
        SpeakItem item = positionHelper.getSpeakItemByIdentity(Identity);
        if (item != null) {
            positionHelper.processItemActions(item);
            container.removeView(item.getView());
        }
        if (positionHelper.getApplyCount() == 0) {
            $.id(R.id.fragment_speakers_new_request_toast).gone();
        }
    }

    @Override
    public void notifyAward(LPInteractionAwardModel awardModel) {
        if (awardModel.isFromCache && awardModel.value.getRecordAward() != null) {
            for (Map.Entry<String, LPAwardUserInfo> entry : awardModel.value.getRecordAward().entrySet()) {
                Playable playable = positionHelper.getPlayableItemByUserNumber(entry.getKey());
                if (playable == null) continue;
                playable.notifyAwardChange(entry.getValue().count);
            }
        } else {
            Playable playable = positionHelper.getPlayableItemByUserNumber(awardModel.value.to);
            //关闭摄像头，老师点赞本地依然需要显示
            if (playable == null) {
                presenter.localShowAwardAnimation(awardModel.value.to);
                return;
            }
            if(awardModel.value.getRecordAward().get(awardModel.value.to) != null){
                playable.notifyAwardChange(awardModel.value.getRecordAward().get(awardModel.value.to).count);
                routerListener.showAwardAnimation(CommonUtils.getEncodePhoneNumber(playable.getUser().getName()));
            }
        }
    }

    @Override
    public void notifyNetworkStatus(String identity, double lossRate) {
        SpeakItem item = positionHelper.getSpeakItemByIdentity(identity);
        if (!(item instanceof RemoteItem)) return;
        if (getContext() == null) return;
        RemoteItem remoteItem = (RemoteItem) item;
        List<Integer> lossRateLevelList = routerListener.getLiveRoom().getPartnerConfig().packetLossRate.packetLossRateLevel;
        if (lossRate < lossRateLevelList.get(0)) {
            remoteItem.updateNetworkState("", getContext().getResources().getColor(R.color.live_low_network_tips_middle));
        } else if (lossRate < lossRateLevelList.get(1)) {
            remoteItem.updateNetworkState(getString(R.string.live_network_tips_level_1), getContext().getResources().getColor(R.color.live_low_network_tips_middle));
        } else if (lossRate < lossRateLevelList.get(2)) {
            remoteItem.updateNetworkState(getString(R.string.live_network_tips_level_2), getContext().getResources().getColor(R.color.live_low_network_tips_middle));
        } else {
            remoteItem.updateNetworkState(getString(R.string.live_network_tips_level_3), getContext().getResources().getColor(R.color.live_low_network_tips_terrible));
        }
    }

    @NonNull
    private SpeakItem createApplyItem(IMediaModel iMediaModel) {
        return new ApplyItem(container, iMediaModel.getUser(), presenter);
    }

    private SpeakItem createRemotePlayableItem(IMediaModel iMediaModel) {
        RemoteItem remoteItem = new RemoteItem(container, iMediaModel, presenter);
        remoteItem.notifyAwardChange(presenter.getAwardCount(iMediaModel.getUser().getNumber()));
        return remoteItem;
    }

    private SpeakItem createLocalPlayableItem() {
        LocalItem localItem = new LocalItem(container, presenter);
        localItem.notifyAwardChange(presenter.getAwardCount(routerListener.getLiveRoom().getCurrentUser().getNumber()));
        getLifecycle().addObserver(localItem);
        return localItem;
    }

    public void switchBackToList(Switchable switchable) {
        if (getContext() == null) return;
        int index = positionHelper.getItemSwitchBackPosition(switchable);
        container.addView(switchable.getView(), index, new LinearLayout.LayoutParams(DisplayUtils.dip2px(getContext(), 100), DisplayUtils.dip2px(getContext(), 76)));
    }

    public void setDisableSpeakQueuePlaceholder(boolean disableSpeakQueuePlaceholder) {
        this.disableSpeakQueuePlaceholder = disableSpeakQueuePlaceholder;
    }

    public void setBackGroundVisible(boolean visible) {
        if (disableSpeakQueuePlaceholder) {
            $.id(R.id.fragment_speakers_scroll_view).backgroundDrawable(null);
            return;
        }
        if (visible) {
            if (container.getChildCount() == 0)
                return;
            if (getActivity() != null && !getActivity().isFinishing())
                $.id(R.id.fragment_speakers_scroll_view).backgroundDrawable(ContextCompat.getDrawable(getActivity(), R.color.live_text_color_light));
        } else {
            $.id(R.id.fragment_speakers_scroll_view).backgroundDrawable(null);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setBackGroundVisible(true);
        } else {
            if (container.getChildCount() > SHRINK_THRESHOLD) {
                setBackGroundVisible(true);
            } else {
                setBackGroundVisible(false);
            }
        }
    }

    public void unBindItems() {
        if (positionHelper != null) {
            final List<SpeakItem> speakItems = positionHelper.getSpeakItems();
            for (SpeakItem speakItem : speakItems) {
                if (speakItem instanceof LocalItem) {
                    LocalItem localItem = (LocalItem) speakItem;
                    getLifecycle().removeObserver(localItem);
                }
            }
        }
    }
}
