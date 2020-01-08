package com.baijiayun.live.ui.speakerlist.item;

import android.app.Activity;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;

import com.afollestad.materialdialogs.MaterialDialog;
import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.speakerlist.SpeakersContract;
import com.baijiayun.live.ui.utils.QueryPlus;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.context.LiveRoom;
import com.baijiayun.livecore.models.imodels.IUserModel;
import com.baijiayun.livecore.wrapper.LPRecorder;
import com.baijiayun.livecore.wrapper.impl.LPCameraView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Shubo on 2019-07-25.
 */
public class LocalItem extends BaseSwitchItem implements Playable, LifecycleObserver {

    private QueryPlus $;
    private Activity context;
    private LiveRoom liveRoom;
    private ViewGroup rootView;
    private LPRecorder recorder;
    private LPCameraView cameraView;
    private RelativeLayout container;
    private FrameLayout videoContainer;
    private boolean shouldStreamVideo, shouldStreamAudio;

    public LocalItem(ViewGroup rootView, SpeakersContract.Presenter presenter) {
        super(presenter);
        this.rootView = rootView;
        this.context = (Activity) rootView.getContext();
        liveRoom = presenter.getRouterListener().getLiveRoom();
        recorder = liveRoom.getRecorder();
        initView();
    }

    private void initView() {
        container = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.item_view_speaker_local, rootView, false);
        $ = QueryPlus.with(container);
        videoContainer = (FrameLayout) $.id(R.id.item_local_speaker_avatar_container).view();
        refreshNameTable();
        registerClickEvent($.contentView());
    }

    public void refreshNameTable() {
        IUserModel currentUser = liveRoom.getCurrentUser();
        if (currentUser.getType() == LPConstants.LPUserType.Teacher) {
            String teacherLabel = liveRoom.getCustomizeTeacherLabel();
            teacherLabel = TextUtils.isEmpty(teacherLabel) ? context.getString(R.string.live_teacher_hint) : "(" + teacherLabel + ")";
            $.id(R.id.item_local_speaker_name).text(liveRoom.getCurrentUser().getName() + teacherLabel);
        } else if (currentUser.getType() == LPConstants.LPUserType.Assistant) {
            String assistantLabel = liveRoom.getCustomizeAssistantLabel();
            assistantLabel = TextUtils.isEmpty(assistantLabel) ? "(助教)" : "(" + assistantLabel + ")";
            if (liveRoom.getPresenterUser() != null && liveRoom.getPresenterUser().getUserId().equals(currentUser.getUserId())) {
                assistantLabel = "(主讲)";
            }
            $.id(R.id.item_local_speaker_name).text(liveRoom.getCurrentUser().getName() + assistantLabel);
        } else {
            $.id(R.id.item_local_speaker_name).text(liveRoom.getCurrentUser().getName());
        }
    }

    private boolean attachVideoOnResume, attachAudioOnResume;

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        if (attachAudioOnResume) {
            streamAudio();
            attachAudioOnResume = false;
        }
        if (attachVideoOnResume) {
            streamVideo();
            attachVideoOnResume = false;
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        attachAudioOnResume = isAudioStreaming();
        attachVideoOnResume = isVideoStreaming();
        stopStreaming();
    }

    @Override
    protected void showOptionDialog() {
        List<String> options = new ArrayList<>();
        options.add(getString(R.string.live_full_screen));
        if (liveRoom.getPartnerConfig().isEnableSwitchPresenter == 1 && liveRoom.getCurrentUser().getType() == LPConstants.LPUserType.Teacher
                && liveRoom.getPresenterUser() != null && !liveRoom.getPresenterUser().getUserId().equals(liveRoom.getCurrentUser().getUserId()))
            options.add(getString(R.string.live_set_to_presenter));
        options.add(getString(R.string.live_recorder_switch_camera));
        if (recorder.isBeautyFilterOn()) {
            options.add(getString(R.string.live_recorder_pretty_filter_off));
        } else {
            options.add(getString(R.string.live_recorder_pretty_filter_on));
        }
        options.add(getString(R.string.live_close_video));
        if (context.isFinishing()) return;
        new MaterialDialog.Builder(context)
                .items(options)
                .itemsCallback((materialDialog, view, i, charSequence) -> {
                    if (context.isFinishing() || context.isDestroyed()) return;
                    if (getString(R.string.live_close_video).equals(charSequence.toString())) {
                        presenter.getRouterListener().detachLocalVideo();
                    } else if (getString(R.string.live_full_screen).equals(charSequence.toString())) {
                        presenter.getRouterListener().getFullScreenItem().switchBackToList();
                        switchToFullScreen();
                    } else if (getString(R.string.live_set_to_presenter).equals(charSequence.toString())) {
                        liveRoom.getSpeakQueueVM().requestSwitchPresenter(liveRoom.getCurrentUser().getUserId());
                    } else if (getString(R.string.live_recorder_pretty_filter_on).equals(charSequence.toString())) {
                        recorder.openBeautyFilter();
                    } else if (getString(R.string.live_recorder_pretty_filter_off).equals(charSequence.toString())) {
                        recorder.closeBeautyFilter();
                    } else if (getString(R.string.live_recorder_switch_camera).equals(charSequence.toString())) {
                        recorder.switchCamera();
                    }
                    materialDialog.dismiss();
                })
                .show();
    }

    private String getString(@StringRes int resId) {
        return context.getString(resId);
    }

    @Override
    public boolean hasVideo() {
        return shouldStreamVideo;
    }

    @Override
    public boolean hasAudio() {
        return shouldStreamAudio;
    }

    public void setShouldStreamAudio(boolean shouldStreamAudio) {
        this.shouldStreamAudio = shouldStreamAudio;
    }

    public void setShouldStreamVideo(boolean shouldStreamVideo) {
        this.shouldStreamVideo = shouldStreamVideo;
    }

    @Override
    public boolean isVideoStreaming() {
        return recorder.isVideoAttached();
    }

    @Override
    public boolean isAudioStreaming() {
        return recorder.isAudioAttached();
    }

    @Override
    public boolean isStreaming() {
        return recorder.isPublishing();
    }

    private void streamAudio() {
        if (!recorder.isPublishing())
            recorder.publish();
        recorder.attachAudio();
    }

    private void streamVideo() {
        if (cameraView == null) {
            cameraView = new LPCameraView(context);
            cameraView.setZOrderMediaOverlay(true);
        }
        videoContainer.removeAllViews();
        videoContainer.addView(cameraView);
        recorder.setPreview(cameraView);
        if (!recorder.isPublishing())
            recorder.publish();
        recorder.attachVideo();
    }

    @Override
    public void stopStreaming() {
        videoContainer.removeAllViews();
        recorder.stopPublishing();
    }

    @Override
    public void refreshPlayable() {
        if (shouldStreamVideo || shouldStreamAudio) {
            if (shouldStreamVideo) {
                streamVideo();
            } else {
                recorder.detachVideo();
            }
            if (shouldStreamAudio) {
                streamAudio();
            } else {
                recorder.detachAudio();
            }
        } else {
            stopStreaming();
        }
    }

    @Override
    public void switchBackToList() {
        super.switchBackToList();
        cameraView.setZOrderMediaOverlay(true);
        invalidVideo();
    }

    public void invalidVideo() {
        recorder.invalidVideo();
    }

    @Override
    public void switchToFullScreen() {
        super.switchToFullScreen();
        cameraView.setZOrderMediaOverlay(false);
        invalidVideo();
    }

    @Override
    public IUserModel getUser() {
        return liveRoom.getCurrentUser();
    }

    @Override
    public void notifyAwardChange(int count) {
        if (count > 0) {
            $.id(R.id.item_local_speaker_award_count_tv).visibility(View.VISIBLE);
            $.id(R.id.item_local_speaker_award_count_tv).text(String.valueOf(count));
        }
    }

    @Override
    public String getIdentity() {
        return liveRoom.getCurrentUser().getUserId();
    }

    @Override
    public SpeakItemType getItemType() {
        return SpeakItemType.Record;
    }

    @Override
    public View getView() {
        return container;
    }
}
