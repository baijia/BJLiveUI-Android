package com.baijiayun.live.ui.chat;

import com.baijiahulian.common.networkv2.BJProgressCallback;
import com.baijiahulian.common.networkv2.BJResponse;
import com.baijiahulian.common.networkv2.HttpException;
import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.utils.RxUtils;
import com.baijiayun.live.ui.utils.Precondition;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.models.LPMessageTranslateModel;
import com.baijiayun.livecore.models.LPShortResult;
import com.baijiayun.livecore.models.LPUploadDocModel;
import com.baijiayun.livecore.models.imodels.IMessageModel;
import com.baijiayun.livecore.models.imodels.IUserModel;
import com.baijiayun.livecore.utils.LPChatMessageParser;
import com.baijiayun.livecore.utils.LPJsonUtils;
import com.baijiayun.livecore.utils.LPLogger;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

import static com.baijiayun.live.ui.utils.Precondition.checkNotNull;

/**
 * Created by Shubo on 2017/2/23.
 */

public class ChatPresenter implements ChatContract.Presenter {

    private LiveRoomRouterListener routerListener;
    private ChatContract.View view;
    private Disposable subscriptionOfDataChange, subscriptionOfMessageReceived, subscribeOfTranslateMessage,subscribeOfIsSelfChatForbid;
    private LinkedBlockingQueue<UploadingImageModel> imageMessageUploadingQueue;
    private ConcurrentHashMap<String, List<IMessageModel>> privateChatMessagePool;
    private ConcurrentHashMap<String, LPMessageTranslateModel> translateMessageModels;
    private int receivedNewMessageNumber = 0;
    private boolean filter;
    private List<IMessageModel> privateChatMessageFilterList;
    private List<IMessageModel> chatMessageFilterList;
    private boolean isSelfForbidden;

    public ChatPresenter(ChatContract.View view) {
        this.view = view;
        privateChatMessagePool = new ConcurrentHashMap<>();
        imageMessageUploadingQueue = new LinkedBlockingQueue<>();
        translateMessageModels = new ConcurrentHashMap<>();
        privateChatMessageFilterList = new ArrayList<>();
        chatMessageFilterList = new ArrayList<>();
    }

    @Override
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {
        this.routerListener = liveRoomRouterListener;
    }

    @Override
    public void subscribe() {
        Precondition.checkNotNull(routerListener);
        chatMessageFilterList = getFilterMessageList(routerListener.getLiveRoom().getChatVM().getMessageList());
        view.notifyDataChanged();
        subscriptionOfDataChange = routerListener.getLiveRoom().getChatVM().getObservableOfNotifyDataChange()
                .onBackpressureBuffer(1000)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<List<IMessageModel>>() {
                    @Override
                    public void accept(List<IMessageModel> iMessageModels) {
                        chatMessageFilterList = getFilterMessageList(iMessageModels);
                        view.notifyDataChanged();
                    }
                });
        subscriptionOfMessageReceived = routerListener.getLiveRoom().getChatVM().getObservableOfReceiveMessage()
                .onBackpressureBuffer()
                .doOnNext(iMessageModel -> {
                    if (!iMessageModel.getFrom().getUserId().equals(routerListener.getLiveRoom().getCurrentUser().getUserId()))
                        receivedNewMessageNumber++;
                    if (iMessageModel.isPrivateChat() && iMessageModel.getToUser() != null) {
                        String userNumber = iMessageModel.getFrom().getNumber().equals(routerListener.getLiveRoom().getCurrentUser().getNumber()) ?
                                iMessageModel.getToUser().getNumber() : iMessageModel.getFrom().getNumber();
                        List<IMessageModel> messageList = privateChatMessagePool.get(userNumber);
                        if (messageList == null) {
                            messageList = new ArrayList<>();
                            privateChatMessagePool.put(userNumber, messageList);
                        }
                        messageList.add(iMessageModel);
                        if (routerListener.isPrivateChat()) {
                            privateChatMessageFilterList = getFilterMessageList(privateChatMessagePool.get(routerListener.getPrivateChatUser().getNumber()));
                        }
                    }
                })
                .filter(iMessageModel -> {
                    if (routerListener.isPrivateChat()) return true;
                    if ("-1".equals(iMessageModel.getTo())) return false;
                    if (iMessageModel.getToUser() == null) return false;
                    IUserModel currentPrivateChatUser = routerListener.getPrivateChatUser();
                    if (currentPrivateChatUser == null) return true;
                    return iMessageModel.getToUser().getNumber().equals(currentPrivateChatUser.getNumber())
                            || iMessageModel.getFrom().getNumber().equals(currentPrivateChatUser.getNumber());
                })
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(iMessageModel -> {
                    if (iMessageModel.getMessageType() == LPConstants.MessageType.Image
                            && iMessageModel.getFrom().getUserId().equals(routerListener.getLiveRoom().getCurrentUser().getUserId())) {
                        view.notifyItemChange(getCount() - imageMessageUploadingQueue.size() - 1);
                    }
                    view.notifyItemInserted(getCount() - 1);
                });
        subscribeOfTranslateMessage = routerListener.getLiveRoom().getChatVM().getObservableOfReceiveTranslateMessage()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(translateModel -> {
                    translateMessageModels.put(translateModel.messageId, translateModel);
                    view.notifyItemTranslateMessage();
                });
        subscribeOfIsSelfChatForbid = routerListener.getLiveRoom().getObservableOfIsSelfChatForbid()
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aBoolean -> isSelfForbidden = aBoolean);
    }

    @Override
    public void unSubscribe() {
        RxUtils.dispose(subscriptionOfDataChange);
        RxUtils.dispose(subscriptionOfMessageReceived);
        RxUtils.dispose(subscribeOfTranslateMessage);
        RxUtils.dispose(subscribeOfIsSelfChatForbid);
    }

    /**
     * 从所有消息过滤出老师/助教的消息
     *
     * @param allMessages 所有消息
     * @return
     */
    private List<IMessageModel> getFilterMessageList(List<IMessageModel> allMessages) {
        List<IMessageModel> messageModelList = new ArrayList<>();
        if (allMessages == null) {
            return messageModelList;
        }
        int size = allMessages.size();
        for (int i = 0; i < size; i++) {
            IMessageModel iMessageModel = allMessages.get(i);
            if (iMessageModel.getFrom().getType() == LPConstants.LPUserType.Teacher ||
                    iMessageModel.getFrom().getType() == LPConstants.LPUserType.Assistant) {
                messageModelList.add(iMessageModel);
            }
        }
        return messageModelList;
    }

    @Override
    public int getCount() {
        if (routerListener.isPrivateChat()) {
            List<IMessageModel> list = privateChatMessagePool.get(routerListener.getPrivateChatUser().getNumber());
            if (filter) {
                list = privateChatMessageFilterList;
            }
            return list == null ? 0 : list.size() + imageMessageUploadingQueue.size();
        } else {
            if (filter) {
                return chatMessageFilterList.size() + imageMessageUploadingQueue.size();
            }
            return routerListener.getLiveRoom().getChatVM().getMessageCount() + imageMessageUploadingQueue.size();
        }
    }

    @Override
    public IMessageModel getMessage(int position) {
        Precondition.checkNotNull(routerListener);
        if (routerListener.isPrivateChat()) {
            List<IMessageModel> list = privateChatMessagePool.get(routerListener.getPrivateChatUser().getNumber());
            if (filter) {
                list = privateChatMessageFilterList;
            }
            int messageCount = list == null ? 0 : list.size();
            if (position < messageCount) {
                return list.get(position);
            } else {
                return (IMessageModel) imageMessageUploadingQueue.toArray()[position - messageCount];
            }
        } else {
            int messageCount = routerListener.getLiveRoom().getChatVM().getMessageCount();
            if (filter) {
                messageCount = chatMessageFilterList.size();
            }
            if (position < messageCount) {
                if (filter) {
                    return chatMessageFilterList.get(position);
                }
                return routerListener.getLiveRoom().getChatVM().getMessage(position);
            } else {
                return (IMessageModel) imageMessageUploadingQueue.toArray()[position - messageCount];
            }
        }
    }

    @Override
    public String getTranslateResult(int position) {
        IMessageModel iMessageModel = getMessage(position);
        LPMessageTranslateModel lpMessageTranslateModel = translateMessageModels.get(iMessageModel.getFrom().getUserId() + iMessageModel.getTime().getTime());
        String result;
        if (lpMessageTranslateModel != null) {
            if (lpMessageTranslateModel.code == 0) {
                result = lpMessageTranslateModel.result;
            } else {
                result = Locale.getDefault().getCountry().equalsIgnoreCase("cn") ? "翻译失败" : "Translate Fail!";
            }
        } else {
            result = "";
        }
        return result;
    }

    @Override
    public void translateMessage(String message, String messageId, String fromLanguage, String toLanguage) {
        routerListener.getLiveRoom().getChatVM().sendTranslateMessage(message, messageId
                , String.valueOf(routerListener.getLiveRoom().getRoomId()), routerListener.getLiveRoom().getCurrentUser().getUserId(), fromLanguage, toLanguage);
    }

    @Override
    public void showBigPic(int position) {
        Precondition.checkNotNull(routerListener);
        routerListener.showBigChatPic(getMessage(position).getUrl());
    }

    @Override
    public void reUploadImage(int position) {
        continueUploadQueue();
    }

    @Override
    public void endPrivateChat() {
        routerListener.onPrivateChatUserChange(null);
        view.notifyDataChanged();
    }

    @Override
    public boolean isForbiddenByTeacher() {
        if (routerListener.isTeacherOrAssistant() || routerListener.isGroupTeacherOrAssistant()) {
            return false;
        }
        return isSelfForbidden;
    }

    @Override
    public IUserModel getCurrentUser() {
        return routerListener.getLiveRoom().getCurrentUser();
    }

    @Override
    public boolean isPrivateChatMode() {
        return routerListener.isPrivateChat();
    }

    @Override
    public void showPrivateChat(IUserModel userModel) {
        routerListener.onPrivateChatUserChange(userModel);
//        routerListener.navigateToMessageInput();
    }

    @Override
    public boolean isLiveCanWhisper() {
        return routerListener.getLiveRoom().getChatVM().isLiveCanWhisper();
    }

    @Override
    public void changeNewMessageReminder(boolean isNeededShow) {
        if (isNeededShow && receivedNewMessageNumber > 0)
            routerListener.changeNewChatMessageReminder(true, receivedNewMessageNumber);
        else {
            receivedNewMessageNumber = 0;
            routerListener.changeNewChatMessageReminder(false, 0);
        }
    }

    @Override
    public boolean needScrollToBottom() {
        return receivedNewMessageNumber > 0;
    }

    @Override
    public boolean isEnableTranslate() {
        return routerListener.getLiveRoom().getPartnerConfig().isEnableChatTranslation();
    }

    /**
     * 过滤只看老师/助教消息
     * true:只看老师/助教 false:全部
     */
    @Override
    public void setFilter(boolean filter) {
        this.filter = filter;
        view.notifyDataChanged();
        view.showFilterChat(filter);
    }

    public boolean getFilter() {
        return filter;
    }

    public void onPrivateChatUserChange() {
        Precondition.checkNotNull(routerListener);
        Precondition.checkNotNull(view);
        if (routerListener.isPrivateChat()) {
            view.showHavingPrivateChat(routerListener.getPrivateChatUser());
        } else {
            view.showNoPrivateChat();
        }
        view.notifyDataChanged();
    }

    @Override
    public void destroy() {
        view = null;
        routerListener = null;
        imageMessageUploadingQueue.clear();
        imageMessageUploadingQueue = null;
    }

    // add Uploading Image to queue
    public void sendImageMessage(String path) {
        UploadingImageModel model = new UploadingImageModel(path, routerListener.getLiveRoom().getCurrentUser(), routerListener.getPrivateChatUser());
        imageMessageUploadingQueue.offer(model);
        continueUploadQueue();
    }


    private void continueUploadQueue() {
        final UploadingImageModel model = imageMessageUploadingQueue.peek();
        if (model == null) return;
        view.notifyItemInserted(routerListener.getLiveRoom().getChatVM().getMessageCount() + imageMessageUploadingQueue.size() - 1);
        routerListener.getLiveRoom().getDocListVM().uploadImageWithProgress(model.getUrl(), this, new BJProgressCallback() {
            @Override
            public void onProgress(long l, long l1) {
                LPLogger.d(l + "/" + l1);
            }

            @Override
            public void onFailure(HttpException e) {
                model.setStatus(UploadingImageModel.STATUS_UPLOAD_FAILED);
                view.notifyDataChanged();
            }

            @Override
            public void onResponse(BJResponse bjResponse) {
                LPShortResult shortResult;
                try {
                    shortResult = LPJsonUtils.parseString(bjResponse.getResponse().body().string(), LPShortResult.class);
                    LPUploadDocModel uploadModel = LPJsonUtils.parseJsonObject((JsonObject) shortResult.data, LPUploadDocModel.class);
                    String imageContent = LPChatMessageParser.toImageMessage(uploadModel.url);
                    routerListener.getLiveRoom().getChatVM().sendImageMessageToUser(model.getToUser(), imageContent, uploadModel.width, uploadModel.height);
                    imageMessageUploadingQueue.poll();
                    continueUploadQueue();
                } catch (Exception e) {
                    model.setStatus(UploadingImageModel.STATUS_UPLOAD_FAILED);
                    view.notifyDataChanged();
                    e.printStackTrace();
                }
            }
        });
    }
}
