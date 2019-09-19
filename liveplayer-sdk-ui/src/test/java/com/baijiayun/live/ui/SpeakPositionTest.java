package com.baijiayun.live.ui;

import android.text.TextUtils;

import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.ppt.MyPPTView;
import com.baijiayun.live.ui.speakerlist.ItemPositionHelper;
import com.baijiayun.live.ui.speakerlist.item.ApplyItem;
import com.baijiayun.live.ui.speakerlist.item.LocalItem;
import com.baijiayun.live.ui.speakerlist.item.RemoteItem;
import com.baijiayun.live.ui.speakerlist.item.SpeakItem;
import com.baijiayun.live.ui.speakerlist.item.SpeakItemType;
import com.baijiayun.livecore.context.LPConstants;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.List;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

/**
 * Unit Test of {@link ItemPositionHelper}
 * Created by Shubo on 2019-08-01.
 */

@RunWith(PowerMockRunner.class)
@PrepareForTest(TextUtils.class)
public class SpeakPositionTest {

    private ItemPositionHelper positionHelper;

    private Random random;

    public SpeakPositionTest() {
        random = new Random();
    }

    @Before
    public void setUp() throws Exception {
        mockStatic(TextUtils.class);

        positionHelper = new ItemPositionHelper();
        LiveRoomRouterListener routerListener = mock(LiveRoomRouterListener.class);

        MyPPTView pptItem = mock(MyPPTView.class);
        when(pptItem.getItemType()).thenReturn(SpeakItemType.PPT);
        when(pptItem.getIdentity()).thenReturn("ppt");
        when(routerListener.getPPTView()).thenReturn(pptItem);
        positionHelper.setRouterListener(routerListener);
    }

    @After
    public void checkLast() throws Exception {
        listCheckAssertion();
    }

    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(1, positionHelper.getSpeakItems().size());
    }

    @Test
    public void addition_testSpeaker() throws Exception {
        RemoteItem speakItem = mock(RemoteItem.class);
        mockRandomRemoteItem(speakItem);

        List<ItemPositionHelper.ItemAction> actions = positionHelper.processItemActions(speakItem);
        assertEquals(1, actions.size());
        assertEquals(ItemPositionHelper.ActionType.ADD, actions.get(0).getAction());
        assertEquals(2, positionHelper.getSpeakItems().size());

        when(speakItem.hasVideo()).thenReturn(false);
        when(speakItem.getItemType()).thenReturn(SpeakItemType.Audio);
        positionHelper.processItemActions(speakItem);
        assertEquals(2, actions.size());
        assertEquals(ItemPositionHelper.ActionType.REMOVE, actions.get(0).getAction());
        assertEquals(ItemPositionHelper.ActionType.ADD, actions.get(1).getAction());
        assertEquals(2, positionHelper.getSpeakItems().size());

        mockRemoveItem(speakItem);
        positionHelper.processItemActions(speakItem);
        assertEquals(1, actions.size());
        assertEquals(ItemPositionHelper.ActionType.REMOVE, actions.get(0).getAction());
        assertEquals(1, positionHelper.getSpeakItems().size());


        mock10RemoteItem();
        int count = positionHelper.getSpeakItems().size();
        if (count <= 3)
            return;
        SpeakItem item = positionHelper.getSpeakItems().get(count - 2);
        if (item instanceof RemoteItem && item.getItemType() != SpeakItemType.Presenter) {
            closeVideo((RemoteItem) item);
            closeAudio((RemoteItem) item);
            positionHelper.processItemActions(item);
            assertEquals(count - 1, positionHelper.getSpeakItems().size());
        }

        RemoteItem remoteItem = mockRandomRemoteItem();
        openVideo(remoteItem);
        openAudio(remoteItem);
        positionHelper.processItemActions(remoteItem);
        closeVideo(remoteItem);
        List<ItemPositionHelper.ItemAction> actionList = positionHelper.processItemActions(remoteItem);
        assertEquals(2, actionList.size());
        assertEquals(ItemPositionHelper.ActionType.ADD, actionList.get(1).getAction());

        count = positionHelper.getSpeakItems().size();
        assertEquals(count - 1, actionList.get(1).getValue());

        closeAudio(remoteItem);
        positionHelper.processItemActions(remoteItem);
        assertEquals(count - 1, positionHelper.getSpeakItems().size());
    }

    @Test
    public void addition_addSpeakers() {
        mock10RemoteItem();
    }

    @Test
    public void addition_testPresenter() {
        mock10RemoteItem();

        RemoteItem presenter = mockPresenterItem();
        positionHelper.processItemActions(presenter);

        assertSame(presenter, positionHelper.getSpeakItems().get(1));

        mockRemoveItem(presenter);
        positionHelper.processItemActions(presenter);

        assertSame(presenter, positionHelper.getSpeakItems().get(1));

        closeAudio(presenter);
        positionHelper.processItemActions(presenter);
        assertSame(presenter, positionHelper.getSpeakItems().get(1));

        closeVideo(presenter);
        positionHelper.processItemActions(presenter);
        assertSame(presenter, positionHelper.getSpeakItems().get(1));

        openAudio(presenter);
        positionHelper.processItemActions(presenter);
        assertSame(presenter, positionHelper.getSpeakItems().get(1));

        openVideo(presenter);
        positionHelper.processItemActions(presenter);
        assertSame(presenter, positionHelper.getSpeakItems().get(1));
    }

    @Test
    public void addition_testApply() {
        mock10RemoteItem();
        ApplyItem applyItem = mock(ApplyItem.class);
        when(applyItem.getIdentity()).thenReturn("apply001");

        int count = positionHelper.getSpeakItems().size();

        positionHelper.processItemActions(applyItem);
        assertEquals(count + 1, positionHelper.getSpeakItems().size());

        positionHelper.processItemActions(applyItem);
        assertEquals(count, positionHelper.getSpeakItems().size());
    }

    @Test
    public void addition_testPresenterChange() {
        mock10RemoteItem();
        RemoteItem presenter = mockPresenterItem();
        positionHelper.processItemActions(presenter);

        printAllSpeakItemInfo();
        listCheckAssertion();

        SpeakItem newPresenter = positionHelper.getSpeakItems().get(positionHelper.getLastPositionOfItemType(SpeakItemType.Video) -1);

        when(TextUtils.isEmpty(anyString())).thenReturn(false);

        List<ItemPositionHelper.ItemAction> actionList = positionHelper.processPresenterChangeItemActions(newPresenter);

        SpeakItemType tpye = presenter.hasVideo() ? SpeakItemType.Video : SpeakItemType.Audio;
        when(presenter.getItemType()).thenReturn(tpye);
        when(newPresenter.getItemType()).thenReturn(SpeakItemType.Presenter);

        assertEquals(4, actionList.size());

        printAllSpeakItemInfo();
        listCheckAssertion();

        positionHelper.processPresenterChangeItemActions(null);
        positionHelper.processPresenterChangeItemActions(mockPresenterItem());

        printAllSpeakItemInfo();
    }

    @Test
    public void addition_testRecorderChange() {
        mock10RemoteItem();

        int size = positionHelper.getSpeakItems().size();
        LocalItem localItem = mockLocalItem();
        when(localItem.hasVideo()).thenReturn(true);
        positionHelper.processItemActions(localItem);
        assertEquals(size + 1, positionHelper.getSpeakItems().size());

        when(localItem.hasVideo()).thenReturn(false);
        positionHelper.processItemActions(localItem);
        assertEquals(size, positionHelper.getSpeakItems().size());

        RemoteItem presenter = mockPresenterItem();
        positionHelper.processItemActions(presenter);
        size = positionHelper.getSpeakItems().size();

        when(localItem.hasVideo()).thenReturn(true);
        positionHelper.processItemActions(localItem);
        assertEquals(size + 1, positionHelper.getSpeakItems().size());

        printAllSpeakItemInfo();

        when(localItem.hasVideo()).thenReturn(false);
        positionHelper.processItemActions(localItem);
        assertEquals(size, positionHelper.getSpeakItems().size());

    }

    private void mock10RemoteItem() {
        for (int i = 0; i < 10; i++) {
            positionHelper.processItemActions(mockRandomRemoteItem());
        }
    }

    private void mockRemoveItem(RemoteItem remoteItem) {
        when(remoteItem.hasVideo()).thenReturn(false);
        when(remoteItem.hasAudio()).thenReturn(false);
    }

    private void openVideo(RemoteItem remoteItem) {
        when(remoteItem.hasVideo()).thenReturn(true);
        if (remoteItem.getItemType() != SpeakItemType.Presenter)
            when(remoteItem.getItemType()).thenReturn(SpeakItemType.Video);
    }

    private void closeVideo(RemoteItem remoteItem) {
        when(remoteItem.hasVideo()).thenReturn(false);
        if (remoteItem.getItemType() != SpeakItemType.Presenter)
            when(remoteItem.getItemType()).thenReturn(SpeakItemType.Audio);
    }

    private void openAudio(RemoteItem remoteItem) {
        when(remoteItem.hasAudio()).thenReturn(true);
    }

    private void closeAudio(RemoteItem remoteItem) {
        when(remoteItem.hasAudio()).thenReturn(false);
    }

    private LocalItem mockLocalItem() {
        LocalItem localItem = mock(LocalItem.class);
        when(localItem.getIdentity()).thenReturn("record");
        when(localItem.getItemType()).thenReturn(SpeakItemType.Record);
        return localItem;
    }

    private RemoteItem mockPresenterItem() {
        RemoteItem remoteItem = mock(RemoteItem.class);
        when(remoteItem.getIdentity()).thenReturn(String.valueOf(++userId));

        boolean isVideoOn = random.nextBoolean();
        boolean isAudioOn = random.nextBoolean();
        when(remoteItem.hasVideo()).thenReturn(isVideoOn);
        when(remoteItem.hasAudio()).thenReturn(isAudioOn);
        when(remoteItem.isVideoStreaming()).thenReturn(isVideoOn);
        when(remoteItem.isAudioStreaming()).thenReturn(isAudioOn);
        when(remoteItem.getItemType()).thenReturn(SpeakItemType.Presenter);
        when(remoteItem.getMediaSourceType()).thenReturn(LPConstants.MediaSourceType.MainCamera);

        return remoteItem;
    }

    private void mockRandomRemoteItem(RemoteItem remoteItem) {
        when(remoteItem.getIdentity()).thenReturn(String.valueOf(random.nextInt()));

        boolean isVideoOn = random.nextBoolean();
        boolean isAudioOn = true;
        when(remoteItem.hasVideo()).thenReturn(isVideoOn);
        when(remoteItem.hasAudio()).thenReturn(true);
        when(remoteItem.isVideoStreaming()).thenReturn(isVideoOn);
        when(remoteItem.isAudioStreaming()).thenReturn(true);
        when(remoteItem.getItemType()).thenReturn(isVideoOn ? SpeakItemType.Video : SpeakItemType.Audio);
    }

    private static int userId = 0;

    private RemoteItem mockRandomRemoteItem() {
        RemoteItem remoteItem = mock(RemoteItem.class);
        when(remoteItem.getIdentity()).thenReturn(String.valueOf(++userId));

        boolean isVideoOn = random.nextBoolean();
        boolean isAudioOn = random.nextBoolean();
        when(remoteItem.hasVideo()).thenReturn(isVideoOn);
        when(remoteItem.hasAudio()).thenReturn(isAudioOn);
        when(remoteItem.getItemType()).thenReturn(isVideoOn ? SpeakItemType.Video : SpeakItemType.Audio);

        return remoteItem;
    }

    //序列正确性检查
    private void listCheckAssertion() {
        List<SpeakItem> speakItems = positionHelper.getSpeakItems();

        for (SpeakItem item : speakItems) {
//            printSpeakItemInfo(item);
            if (item != speakItems.get(speakItems.size() - 1)) {
                assertTrue(item.getItemType().ordinal() <= speakItems.get(speakItems.indexOf(item) + 1).getItemType().ordinal());
            }
        }
    }

    private void printAllSpeakItemInfo() {
        List<SpeakItem> speakItems = positionHelper.getSpeakItems();

        for (SpeakItem item : speakItems) {
            printSpeakItemInfo(item);
        }
    }

    private void printSpeakItemInfo(SpeakItem item) {
        if (item instanceof RemoteItem) {
            System.out.println(item.getIdentity() + " " + item.getItemType() + " " + ((RemoteItem) item).hasVideo() + " " + ((RemoteItem) item).hasAudio());
        } else {
            System.out.println(item.getIdentity() + " " + item.getItemType());
        }
    }
}
