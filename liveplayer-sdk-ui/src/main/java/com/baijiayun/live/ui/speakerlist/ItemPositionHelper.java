package com.baijiayun.live.ui.speakerlist;

import android.support.annotation.MainThread;
import android.support.annotation.VisibleForTesting;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.speakerlist.item.ApplyItem;
import com.baijiayun.live.ui.speakerlist.item.LocalItem;
import com.baijiayun.live.ui.speakerlist.item.Playable;
import com.baijiayun.live.ui.speakerlist.item.RemoteItem;
import com.baijiayun.live.ui.speakerlist.item.SpeakItem;
import com.baijiayun.live.ui.speakerlist.item.SpeakItemType;
import com.baijiayun.live.ui.speakerlist.item.Switchable;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.utils.LPLogger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * 解决复杂的发言列表的排序问题
 * 发言者列表遵从{[PPT]---[主讲人视频|头像]---[自己视频]---[其他人视频]---[其他发言用户音频]---[请求发言用户]}的顺序
 * <p>
 * Created by Shubo on 2019-07-31.
 */
public class ItemPositionHelper {

    // 记录位于发言队列的所有元素，包括已经被切换到全屏显示位置的元素
    private List<SpeakItem> speakItems;

    private List<ItemAction> itemActions;

    private Switchable pptItem;

    private LocalItem localItem;

//    private IUserModel currentUser;

    public ItemPositionHelper() {
        speakItems = new ArrayList<>();
        itemActions = new ArrayList<>();
    }

    public void setRouterListener(LiveRoomRouterListener routerListener) {
        pptItem = (Switchable) routerListener.getPPTView();
        speakItems.add(0, pptItem);
//        currentUser = routerListener.getLiveRoom().getCurrentUser();
    }

    public List<ItemAction> processItemActions(SpeakItem speakItem) {
        itemActions.clear();
        if (speakItem instanceof RemoteItem) {
            handleRemoteItem((RemoteItem) speakItem);
        } else if (speakItem instanceof LocalItem) {
            handleLocalItem((LocalItem)speakItem);
        } else if (speakItem instanceof ApplyItem) {
            handleApplyItem(speakItem);
        } else if (speakItem.getItemType() == SpeakItemType.PPT) {
            handlePPTItem(speakItem);
        }
        printAllSpeakItemInfo();
        return itemActions;
    }

    public List<ItemAction> processPresenterChangeItemActions(SpeakItem newPresenter) {
        itemActions.clear();
        SpeakItem oldPresenter = null;
        if (newPresenter == null) { // 老师离开教室，newPresenter为空 切自己为主讲并且未开音视频
            Iterator<SpeakItem> iterator = speakItems.iterator();
            while (iterator.hasNext()) {
                SpeakItem item = iterator.next();
                if (item.getItemType() == SpeakItemType.Presenter) {
                    iterator.remove();
                    itemActions.add(new ItemAction(item, ActionType.REMOVE));
                }
            }
        } else {
            for (SpeakItem item : speakItems) {
                if (item.getItemType() == SpeakItemType.Presenter) {
                    oldPresenter = item;
                    break;
                }
            }
            if (newPresenter instanceof RemoteItem) {
                ((RemoteItem) newPresenter).refreshItemType();
            }
            if (oldPresenter instanceof RemoteItem) {
                ((RemoteItem) oldPresenter).refreshItemType();
            }
            if (oldPresenter == null) { // 老师进入教室 oldPresenter为空
                if (speakItems.indexOf(newPresenter) != 1) {
                    speakItems.remove(newPresenter);
                    int addPosition = getLastPositionOfItemType(SpeakItemType.Presenter);
                    speakItems.add(addPosition, newPresenter);
                    itemActions.add(new ItemAction(newPresenter, ActionType.REMOVE));
                    itemActions.add(new ItemAction(newPresenter, ActionType.ADD, getRealViewPosition(addPosition)));
                    if (newPresenter instanceof RemoteItem && ((RemoteItem) newPresenter).isInFullScreen()) {
                        itemActions.clear();
                    }
                }
            } else {
                if (oldPresenter instanceof Switchable && newPresenter instanceof Switchable) {
                    // 切主讲
                    if (speakItems.remove(oldPresenter)) {
                        if (oldPresenter instanceof Playable) {
                            Playable oldPlayable = (Playable) oldPresenter;
                            itemActions.add(new ItemAction(oldPlayable, ActionType.REMOVE));
                            if (oldPlayable.isVideoStreaming()) {
                                int addPosition = getLastPositionOfItemType(SpeakItemType.Video);
                                speakItems.add(addPosition, oldPlayable);
                                if (!((Switchable) oldPresenter).isInFullScreen())
                                    itemActions.add(new ItemAction(oldPlayable, ActionType.ADD, getRealViewPosition(addPosition)));
                            } else if (oldPlayable.hasAudio() || oldPlayable.hasVideo()) {
                                int addPosition = getLastPositionOfItemType(SpeakItemType.Audio);
                                speakItems.add(addPosition, oldPlayable);
                                itemActions.add(new ItemAction(oldPlayable, ActionType.ADD, getRealViewPosition(addPosition)));
                            } else {
                                itemActions.add(new ItemAction(oldPlayable, ActionType.REMOVE));
                            }
                        }
                    }

                    if (speakItems.remove(newPresenter)) {
                        itemActions.add(new ItemAction(newPresenter, ActionType.REMOVE));
                    }
                    int addPosition = getLastPositionOfItemType(SpeakItemType.Presenter);
                    speakItems.add(addPosition, newPresenter);
                    if (!((Switchable) newPresenter).isInFullScreen())
                        itemActions.add(new ItemAction(newPresenter, ActionType.ADD, getRealViewPosition(addPosition)));
                }
            }
        }

        for (SpeakItem item : speakItems) {
            if (item instanceof RemoteItem) {
                ((RemoteItem) item).refreshNameTable();
                if (item == newPresenter) ((RemoteItem) item).showWaterMark();
                if (item == oldPresenter) ((RemoteItem) item).hideWaterMark();
            } else if (item instanceof LocalItem) ((LocalItem) item).refreshNameTable();
        }

        printAllSpeakItemInfo();
        return itemActions;
    }

    // 本地未上麦用户被设为主讲了
    public List<ItemAction> processUnActiveLocalPresenterItemActions() {
        itemActions.clear();
        SpeakItem oldPresenter = null;
        for (SpeakItem item : speakItems) {
            if (item.getItemType() == SpeakItemType.Presenter) {
                oldPresenter = item;
                break;
            }
        }
        if (oldPresenter instanceof RemoteItem) {
            ((RemoteItem) oldPresenter).refreshItemType();
        }
        return itemActions;
    }

    private void printAllSpeakItemInfo() {
        for (SpeakItem item : speakItems) {
            if (item instanceof Playable) {
                LPLogger.d("ItemPositionHelper", item.getIdentity() + " " + item.getItemType() + " " + ((Playable) item).isVideoStreaming() + " " + ((Playable) item).isAudioStreaming());
            } else {
                LPLogger.d("ItemPositionHelper", item.getIdentity() + " " + item.getItemType());
            }
        }
    }

    private void handlePPTItem(SpeakItem speakItem) {
    }

    private void handleLocalItem(LocalItem localItem) {
        if (speakItems.contains(localItem)) {
            if (!localItem.hasVideo()) {
                if (localItem.isInFullScreen()) {
                    localItem.switchBackToList();
                    pptItem.switchToFullScreen();
                }
                speakItems.remove(localItem);
                itemActions.add(new ItemAction(localItem, ActionType.REMOVE));
            }
        } else {
            if (localItem.hasVideo()) {
                int position = getLastPositionOfItemType(SpeakItemType.Record);
                speakItems.add(position, localItem);
                itemActions.add(new ItemAction(localItem, ActionType.ADD, getRealViewPosition(position)));
            }
        }
    }

    private void handleApplyItem(SpeakItem speakItem) {
        if (speakItems.contains(speakItem)) {
            speakItems.remove(speakItem);
        } else {
            speakItems.add(speakItem);
        }
    }

    @MainThread
    private void handleRemoteItem(RemoteItem remoteItem) {
        if (speakItems.contains(remoteItem)) {
            if (!remoteItem.hasVideo() && !remoteItem.hasAudio()) {
                if (remoteItem.isInFullScreen()) {
                    remoteItem.switchBackToList();
                    pptItem.switchToFullScreen();
                    //ATTENTION 主讲主摄像头或keep_alive=true保留席位
                    if (remoteItem.getItemType() == SpeakItemType.Presenter && remoteItem.getMediaSourceType() == LPConstants.MediaSourceType.MainCamera
                            || remoteItem.isKeepAlive())
                        return;
                    itemActions.add(new ItemAction(remoteItem, ActionType.REMOVE));
                    speakItems.remove(remoteItem);
                } else {
                    if (remoteItem.getItemType() == SpeakItemType.Presenter && remoteItem.getMediaSourceType() == LPConstants.MediaSourceType.MainCamera
                            || remoteItem.isKeepAlive())
                        return;
                    speakItems.remove(remoteItem);
                    itemActions.add(new ItemAction(remoteItem, ActionType.REMOVE));
                }
            } else {
                if (remoteItem.isInFullScreen() && !remoteItem.hasVideo()) {
                    remoteItem.switchBackToList();
                    pptItem.switchToFullScreen();
                }
                if (remoteItem.getItemType() == SpeakItemType.Presenter && remoteItem.getMediaSourceType() == LPConstants.MediaSourceType.MainCamera)
                    return;
                if (remoteItem.hasVideo()) {
                    if (!remoteItem.isVideoStreaming()) {
                        speakItems.remove(remoteItem);
                        int addPosition = getLastPositionOfItemType(remoteItem.getItemType());
                        speakItems.add(addPosition, remoteItem);
                        itemActions.add(new ItemAction(remoteItem, ActionType.REMOVE));
                        itemActions.add(new ItemAction(remoteItem, ActionType.ADD, getRealViewPosition(addPosition)));
                    }
                } else {
                    speakItems.remove(remoteItem);
                    int addPosition = getLastPositionOfItemType(remoteItem.getItemType());
                    speakItems.add(addPosition, remoteItem);
                    itemActions.add(new ItemAction(remoteItem, ActionType.REMOVE));
                    itemActions.add(new ItemAction(remoteItem, ActionType.ADD, getRealViewPosition(addPosition)));
                }
            }
        } else {
            if(!remoteItem.hasVideo() && !remoteItem.hasAudio() && remoteItem.getMediaSourceType() == LPConstants.MediaSourceType.ExtScreenShare){
                // 兼容前端不开辅助摄像头 只开辅助摄像头屏幕分享 再关闭会发两次mp_ext false false
                return;
            }
            if (!remoteItem.hasVideo() && !remoteItem.hasAudio() && remoteItem.getItemType() != SpeakItemType.Presenter)
                return;
            int addPosition = getLastPositionOfItemType(remoteItem.getItemType());
            speakItems.add(addPosition, remoteItem);
            itemActions.add(new ItemAction(remoteItem, ActionType.ADD, getRealViewPosition(addPosition)));
        }
    }

    public List<ItemAction> processUserCloseAction(RemoteItem remoteItem) {
        itemActions.clear();
        SpeakItemType preItemType = remoteItem.getItemType();
        remoteItem.refreshItemType();
        SpeakItemType postItemType = remoteItem.getItemType();
        if (preItemType != postItemType) {
            speakItems.remove(remoteItem);
            int position = getLastPositionOfItemType(postItemType);
            speakItems.add(position, remoteItem);
            itemActions.add(new ItemAction(remoteItem, ActionType.REMOVE));
            itemActions.add(new ItemAction(remoteItem, ActionType.ADD, getRealViewPosition(position)));
        }
        return itemActions;
    }

    public SpeakItem getSpeakItemByIdentity(String identity) {
        for (SpeakItem item : speakItems) {
            if (item.getIdentity().equals(identity))
                return item;
        }
        return null;
    }

    int getApplyCount() {
        int count = 0;
        for (SpeakItem item : speakItems) {
            if (item instanceof ApplyItem)
                count++;
        }
        return count;
    }

    public Playable getPlayableItemByUserNumber(String userNumber) {
        for (SpeakItem item : speakItems) {
            if (item instanceof Playable)
                if (userNumber.equals(((Playable) item).getUser().getNumber())) {
                    return (Playable) item;
                }
        }
        return null;
    }

    public List<SpeakItem> getSpeakItems() {
        return speakItems;
    }

    private int getRealViewPosition(int dataPosition) {
        for (int i = 0; i < dataPosition; i++) {
            SpeakItem item = speakItems.get(i);
            if (item instanceof Switchable && ((Switchable) item).isInFullScreen())
                return dataPosition - 1;
        }
        return dataPosition;
    }

    public int getLastPositionOfItemType(SpeakItemType type) {
        for (SpeakItem item : speakItems) {
            if (item instanceof Switchable)
                if (type.ordinal() >= item.getItemType().ordinal())
                    continue;
            return speakItems.indexOf(item);
        }
        return speakItems.size();
    }

    public int getItemSwitchBackPosition(SpeakItem speakItem) {
        return speakItems.indexOf(speakItem);
    }

    public static class ItemAction {

        ItemAction(SpeakItem speakItem, ActionType action, int value) {
            this.speakItem = speakItem;
            this.action = action;
            this.value = value;
        }

        ItemAction(SpeakItem speakItem, ActionType action) {
            this.speakItem = speakItem;
            this.action = action;
        }

        public SpeakItem speakItem;
        public ActionType action;
        public int value;

        @VisibleForTesting
        public ActionType getAction() {
            return action;
        }

        @VisibleForTesting
        public int getValue() {
            return value;
        }
    }

    public enum ActionType {
        ADD, REMOVE, FULLSCREEN
    }
}
