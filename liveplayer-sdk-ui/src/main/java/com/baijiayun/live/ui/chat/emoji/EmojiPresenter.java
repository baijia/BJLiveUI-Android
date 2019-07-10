package com.baijiayun.live.ui.chat.emoji;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.livecore.models.imodels.IExpressionModel;

import java.util.List;

/**
 * Created by Shubo on 2017/5/6.
 */

public class EmojiPresenter implements EmojiContract.Presenter {

    private LiveRoomRouterListener routerListener;
    private EmojiContract.View view;
    private int PAGE_SIZE;
    private List<IExpressionModel> emojiList;
    private int currentPageFirstItem;

    EmojiPresenter(EmojiContract.View view) {
        this.view = view;
        PAGE_SIZE = view.getRowCount() * view.getSpanCount();
    }

    @Override
    public void setRouter(LiveRoomRouterListener liveRoomRouterListener) {
        routerListener = liveRoomRouterListener;
        emojiList = routerListener.getLiveRoom().getChatVM().getExpressions();
    }

    @Override
    public void subscribe() {
    }

    @Override
    public void unSubscribe() {

    }

    @Override
    public void destroy() {
        routerListener = null;
        view = null;
    }

    // page = [0,int.max]
    // position = [0, ROW_COUNT * SPAN_PER_ROW -1]
    @Override
    public IExpressionModel getItem(int page, int position) {
        return emojiList.get(page * PAGE_SIZE + position);
    }

    // page = [0,int.max]
    @Override
    public int getCount(int page) {
        return emojiList.size() < PAGE_SIZE * (page + 1) ? emojiList.size() % PAGE_SIZE : PAGE_SIZE;
    }

    @Override
    public int getPageCount() {
        return emojiList.size() % PAGE_SIZE == 0 ? emojiList.size() / PAGE_SIZE : emojiList.size() / PAGE_SIZE + 1;
    }

    @Override
    public void onSizeChanged() {
        PAGE_SIZE = view.getRowCount() * view.getSpanCount();
    }

    @Override
    public int getPageOfCurrentFirstItem() {
        return currentPageFirstItem / PAGE_SIZE;
    }

    @Override
    public void onPageSelected(int page) {
        currentPageFirstItem = page * PAGE_SIZE + 1;
    }
}
