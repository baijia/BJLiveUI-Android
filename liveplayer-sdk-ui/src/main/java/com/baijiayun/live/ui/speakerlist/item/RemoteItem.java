package com.baijiayun.live.ui.speakerlist.item;

import android.app.Activity;
import android.support.annotation.ColorInt;
import android.support.annotation.StringRes;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.speakerlist.SpeakersContract;
import com.baijiayun.live.ui.utils.QueryPlus;
import com.baijiayun.live.ui.utils.VideoDefinitionUtil;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.context.LiveRoom;
import com.baijiayun.livecore.models.imodels.IMediaModel;
import com.baijiayun.livecore.models.imodels.IUserModel;
import com.baijiayun.livecore.wrapper.LPPlayer;
import com.baijiayun.livecore.wrapper.impl.LPVideoView;
import com.baijiayun.livecore.wrapper.listener.LPPlayerListener;
import com.squareup.picasso.Picasso;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by Shubo on 2019-07-25.
 */
public class RemoteItem extends BaseSwitchItem implements Playable {

    private QueryPlus $;
    private Activity context;
    private LPPlayer player;
    private LiveRoom liveRoom;
    private LPVideoView videoView;
    private IMediaModel mediaModel;
    private SpeakItemType itemType;
    private ViewGroup itemContainer;
    private RelativeLayout container;
    private ImageView avatarImageView;
    private FrameLayout videoContainer;
    private Animation loadingViewAnimation;
    private LoadingListener loadingListener;

    private boolean isVideoPlaying, isAudioPlaying, isVideoClosedByUser;
    private static FrameLayout.LayoutParams layoutParams =
            new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

    public RemoteItem(ViewGroup itemContainer, IMediaModel mediaModel, SpeakersContract.Presenter presenter) {
        super(presenter);
        this.mediaModel = mediaModel;
        this.itemContainer = itemContainer;
        this.context = (Activity) itemContainer.getContext();
        liveRoom = presenter.getRouterListener().getLiveRoom();
        player = liveRoom.getPlayer();
        refreshItemType();
        initView();
    }

    public void refreshItemType() {
        if (liveRoom.getPresenterUser() != null && mediaModel.getUser().getUserId().equals(liveRoom.getPresenterUser().getUserId())) {
            itemType = SpeakItemType.Presenter;
        } else {
            if (mediaModel.isVideoOn() && !isVideoClosedByUser) {
                itemType = SpeakItemType.Video;
            } else {
                itemType = SpeakItemType.Audio;
            }
        }
    }

    private void initView() {
        container = (RelativeLayout) LayoutInflater.from(context).inflate(R.layout.item_view_speaker_remote, itemContainer, false);
        $ = QueryPlus.with(container);
        videoContainer = (FrameLayout) $.id(R.id.item_speak_speaker_avatar_container).view();
        avatarImageView = new ImageView(context);
        avatarImageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        refreshNameTable();
        if (!TextUtils.isEmpty(mediaModel.getUser().getAvatar())) {
            String avatar = mediaModel.getUser().getAvatar().startsWith("//") ? "https:" + mediaModel.getUser().getAvatar() : mediaModel.getUser().getAvatar();
            Picasso.with(context).load(avatar).into(avatarImageView);
            videoContainer.addView(avatarImageView);
        }
        $.id(R.id.item_speak_speaker_video_label).visibility(mediaModel.isVideoOn() ? View.VISIBLE : View.GONE);
        container.setTag(R.id.lp_speaker_audio_type_tag, mediaModel.getUser().getUserId());
        registerClickEvent($.contentView());
    }

    public void refreshNameTable() {
        if (itemType == SpeakItemType.Presenter) {
            String teacherLabel = liveRoom.getCustomizeTeacherLabel();
            teacherLabel = TextUtils.isEmpty(teacherLabel) ? context.getString(R.string.live_teacher_hint) : "(" + teacherLabel + ")";
            $.id(R.id.item_speak_speaker_name).text(mediaModel.getUser().getName() + (mediaModel.getUser().getType() == LPConstants.LPUserType.Teacher ? teacherLabel : context.getString(R.string.live_presenter_hint)));
        } else if (mediaModel.getUser().getType() == LPConstants.LPUserType.Teacher) {
            String teacherLabel = liveRoom.getCustomizeTeacherLabel();
            teacherLabel = TextUtils.isEmpty(teacherLabel) ? context.getString(R.string.live_teacher_hint) : "(" + teacherLabel + ")";
            $.id(R.id.item_speak_speaker_name).text(mediaModel.getUser().getName() + teacherLabel);
        } else if (mediaModel.getUser().getType() == LPConstants.LPUserType.Assistant) {
            String assistantLabel = liveRoom.getCustomizeAssistantLabel();
            assistantLabel = TextUtils.isEmpty(assistantLabel) ? "" : "(" + assistantLabel + ")";
            $.id(R.id.item_speak_speaker_name).text(mediaModel.getUser().getName() + (TextUtils.isEmpty(assistantLabel) ? "" : assistantLabel));
        } else
            $.id(R.id.item_speak_speaker_name).text(mediaModel.getUser().getName());
    }

    @Override
    public void refreshPlayable() {
        if (mediaModel.isVideoOn() && !isVideoClosedByUser) {
            stopStreaming();
            streamVideo();
        } else if (mediaModel.isAudioOn()) {
            stopStreaming();
            streamAudio();
        } else
            stopStreaming();
    }

    @Override
    public IUserModel getUser() {
        return mediaModel.getUser();
    }

    @Override
    public void notifyAwardChange(int count) {
        if (count > 0) {
            $.id(R.id.item_speak_speaker_award_count_tv).visibility(View.VISIBLE);
            $.id(R.id.item_speak_speaker_award_count_tv).text(String.valueOf(count));
        }
    }

    @Override
    public boolean hasVideo() {
        return mediaModel.isVideoOn();
    }

    @Override
    public boolean hasAudio() {
        return mediaModel.isAudioOn();
    }

    @Override
    public boolean isVideoStreaming() {
        return isVideoPlaying;
    }

    @Override
    public boolean isAudioStreaming() {
        return isAudioPlaying;
    }

    @Override
    public boolean isStreaming() {
        return isAudioPlaying || isVideoPlaying;
    }

    private void streamAudio() {
        player.playAudio(mediaModel.getMediaId());
        isAudioPlaying = true;
    }

    private void streamVideo() {
        if (videoView == null) {
            videoView = new LPVideoView(context);
            videoView.setAspectRatio(LPConstants.LPAspectRatio.Fit);
            videoView.setZOrderMediaOverlay(true);
        }
        videoContainer.removeAllViews();
        videoContainer.addView(videoView, layoutParams);

        showLoading();

        player.playVideo(mediaModel.getMediaId(), videoView);
        isAudioPlaying = true;
        isVideoPlaying = true;
        $.id(R.id.item_speak_speaker_video_label).visibility(View.GONE);
    }

    private void showLoading() {
        if (loadingListener == null) {
            loadingListener = new LoadingListener(this);
            player.addPlayerListener(loadingListener);
        }
        $.id(R.id.item_speak_speaker_loading_container).visible();
        if (loadingViewAnimation == null) {
            loadingViewAnimation = AnimationUtils.loadAnimation(context, R.anim.live_video_loading);
            loadingViewAnimation.setInterpolator(new LinearInterpolator());
        }
        $.id(R.id.item_speak_speaker_loading_img).view().startAnimation(loadingViewAnimation);
    }

    private void hideLoading() {
        $.id(R.id.item_speak_speaker_loading_container).gone();
        if (loadingViewAnimation != null) {
            loadingViewAnimation.cancel();
        }
        $.id(R.id.item_speak_speaker_loading_img).view().clearAnimation();
    }

    @Override
    public void switchToFullScreen() {
        super.switchToFullScreen();
        if (videoView != null)
            videoView.setZOrderMediaOverlay(false);
    }

    @Override
    public void switchBackToList() {
        super.switchBackToList();
        if (videoView != null)
            videoView.setZOrderMediaOverlay(true);
    }

    @Override
    public void stopStreaming() {
        hideLoading();
        player.playAVClose(mediaModel.getMediaId());
        isVideoPlaying = false;
        isAudioPlaying = false;
        videoContainer.removeAllViews();
        videoContainer.addView(avatarImageView, layoutParams);

        $.id(R.id.item_speak_speaker_video_label).visibility(hasVideo() ? View.VISIBLE : View.GONE);
    }

    @Override
    public String getIdentity() {
        if (mediaModel.getMediaSourceType() == LPConstants.MediaSourceType.ExtCamera || mediaModel.getMediaSourceType() == LPConstants.MediaSourceType.ExtScreenShare) {
            return mediaModel.getUser().getUserId() + "_1";
        }
        return mediaModel.getUser().getUserId();
    }

    @Override
    public SpeakItemType getItemType() {
        return itemType;
    }

    @Override
    public View getView() {
        return container;
    }

    public void setMediaModel(IMediaModel mediaModel) {
        this.mediaModel = mediaModel;
        refreshItemType();
    }

    public void updateNetworkState(String status, @ColorInt int color) {
        $.id(R.id.item_speak_speaker_network).text(status);
        ((TextView) $.id(R.id.item_speak_speaker_network).view()).setTextColor(color);
    }

    public boolean isVideoClosedByUser() {
        return isVideoClosedByUser;
    }

    @Override
    protected void showOptionDialog() {
        List<String> options = new ArrayList<>();
        switch (itemType) {
            case Presenter:
                if (!mediaModel.isVideoOn() && !mediaModel.isAudioOn()) return; // 主讲人音视频未开启
                if (isVideoPlaying) {
                    options.add(getString(R.string.live_full_screen));
                    if (mediaModel.getVideoDefinitions().size() > 1)
                        options.add(getString(R.string.live_switch_definitions));
                    options.add(getString(R.string.live_close_video));

                } else if (mediaModel.isVideoOn() && !isVideoPlaying) {
                    options.add(getString(R.string.live_open_video));
                }
                if (canCurrentUserSetPresenter() && mediaModel.getUser().getType() == LPConstants.LPUserType.Assistant) {
                    options.add(getString(R.string.live_unset_presenter));
                }
                break;
            case Video:
                options.add(getString(R.string.live_full_screen));
                if (canCurrentUserSetPresenter() && isThisTeacherOrAssistant())
                    options.add(getString(R.string.live_set_to_presenter));
                if (mediaModel.getUser().getType() == LPConstants.LPUserType.Student && liveRoom.isTeacherOrAssistant()) {
                    if (liveRoom.getPartnerConfig().liveDisableGrantStudentBrush != 1) {
                        if (liveRoom.getSpeakQueueVM().getStudentsDrawingAuthList().contains(mediaModel.getUser().getNumber()))
                            options.add(getString(R.string.live_unset_auth_drawing));
                        else
                            options.add(getString(R.string.live_set_auth_drawing));
                    }
                }
                if (mediaModel.getVideoDefinitions().size() > 1)
                    options.add(getString(R.string.live_switch_definitions));
                if (liveRoom.isTeacherOrAssistant() && mediaModel.getUser().getType() == LPConstants.LPUserType.Student) {
                    options.add(getString(R.string.live_award));
                }
                options.add(getString(R.string.live_close_video));
                if (liveRoom.isTeacherOrAssistant() && liveRoom.getRoomType() == LPConstants.LPRoomType.Multi && mediaModel.getUser().getType() != LPConstants.LPUserType.Teacher)
                    options.add(getString(R.string.live_close_speaking));
                break;
            case Audio:
                if (mediaModel.isVideoOn())
                    options.add(getString(R.string.live_open_video));
                if (canCurrentUserSetPresenter() && (mediaModel.getUser().getType() == LPConstants.LPUserType.Teacher || mediaModel.getUser().getType() == LPConstants.LPUserType.Assistant))
                    options.add(getString(R.string.live_set_to_presenter));
                if (mediaModel.getUser().getType() == LPConstants.LPUserType.Student && liveRoom.isTeacherOrAssistant()) {
                    if (liveRoom.getPartnerConfig().liveDisableGrantStudentBrush != 1) {
                        if (liveRoom.getSpeakQueueVM().getStudentsDrawingAuthList().contains(mediaModel.getUser().getNumber()))
                            options.add(getString(R.string.live_unset_auth_drawing));
                        else
                            options.add(getString(R.string.live_set_auth_drawing));
                    }
                }
                if (liveRoom.isTeacherOrAssistant() && liveRoom.getRoomType() == LPConstants.LPRoomType.Multi)
                    options.add(getString(R.string.live_close_speaking));
                break;
            default:
                break;
        }
        if (options.size() <= 0) return;
        if (context.isFinishing()) return;
        new MaterialDialog.Builder(context)
                .items(options)
                .itemsCallback((materialDialog, view, i, charSequence) -> {
                    if (context.isFinishing() || context.isDestroyed()) return;
                    if (getString(R.string.live_close_video).equals(charSequence.toString())) {
                        stopStreaming();
                        streamAudio();
                        isVideoClosedByUser = true;
                        presenter.handleUserCloseAction(this);
                    } else if (getString(R.string.live_close_speaking).equals(charSequence.toString())) {
                        presenter.closeSpeaking(mediaModel.getUser().getUserId());
                    } else if (getString(R.string.live_open_video).equals(charSequence.toString())) {
                        stopStreaming();
                        streamVideo();
                        isVideoClosedByUser = false;
                        presenter.handleUserCloseAction(this);
                    } else if (getString(R.string.live_full_screen).equals(charSequence.toString())) {
                        presenter.getRouterListener().getFullScreenItem().switchBackToList();
                        switchToFullScreen();
                    } else if (getString(R.string.live_switch_definitions).equals(charSequence.toString())) {
                        showVideoDefinitionSwitchDialog();
                    } else if (getString(R.string.live_set_to_presenter).equals(charSequence.toString())) {
                        liveRoom.getSpeakQueueVM().requestSwitchPresenter(mediaModel.getUser().getUserId());
                    } else if (getString(R.string.live_unset_presenter).equals(charSequence.toString())) {
                        liveRoom.getSpeakQueueVM().requestSwitchPresenter(liveRoom.getCurrentUser().getUserId());
                    } else if (getString(R.string.live_set_auth_drawing).equals(charSequence.toString())) {
                        liveRoom.getSpeakQueueVM().requestStudentDrawingAuthChange(true, mediaModel.getUser().getNumber());
                    } else if (getString(R.string.live_unset_auth_drawing).equals(charSequence.toString())) {
                        liveRoom.getSpeakQueueVM().requestStudentDrawingAuthChange(false, mediaModel.getUser().getNumber());
                    } else if (getString(R.string.live_award).equals(charSequence.toString())) {
                        presenter.requestAward(mediaModel.getUser().getNumber());
                    }
                    materialDialog.dismiss();
                })
                .show();
    }

    private void showVideoDefinitionSwitchDialog() {
        if (mediaModel.getVideoDefinitions().size() <= 1) return;
        List<String> options = new ArrayList<>();
        for (LPConstants.VideoDefinition definition : mediaModel.getVideoDefinitions()) {
            options.add(VideoDefinitionUtil.getVideoDefinitionLabelFromType(definition));
        }
        if (context.isDestroyed() || context.isFinishing()) return;
        new MaterialDialog.Builder(context)
                .items(options)
                .itemsCallback((dialog, itemView, position, text) -> {
                    if (position == -1) return;
                    LPConstants.VideoDefinition definition = VideoDefinitionUtil.getVideoDefinitionTypeFromLabel(text.toString());
                    if (definition == null) return;
                    liveRoom.getPlayer().changeVideoDefinition(mediaModel.getUser().getUserId(), definition);
                    dialog.dismiss();
                })
                .show();
    }

    private String getString(@StringRes int resId) {
        return context.getString(resId);
    }

    private boolean canCurrentUserSetPresenter() {
        return liveRoom.getPartnerConfig().isEnableSwitchPresenter == 1 && liveRoom.getCurrentUser().getType() == LPConstants.LPUserType.Teacher;
    }

    public LPConstants.MediaSourceType getMediaSourceType() {
        return mediaModel.getMediaSourceType();
    }

    private boolean isThisTeacherOrAssistant() {
        return mediaModel.getUser().getType() == LPConstants.LPUserType.Teacher || mediaModel.getUser().getType() == LPConstants.LPUserType.Assistant;
    }

    public void hideWaterMark() {
        if (videoView != null)
            videoView.setWaterMarkVisibility(View.INVISIBLE);
    }

    public void showWaterMark() {
        if (videoView != null) {
            videoView.setWaterMarkVisibility(View.VISIBLE);
        }
    }

    private static class LoadingListener implements LPPlayerListener {

        private WeakReference<RemoteItem> remoteItemWeakReference;

        LoadingListener(RemoteItem remoteItem) {
            remoteItemWeakReference = new WeakReference<>(remoteItem);
        }

        @Override
        public void onReadyToPlay(String mediaId) {
            // webrtc辅助摄像头屏幕分享mediaID为uid_3 大班课里辅助摄像头/辅助摄像头屏幕分享都识别为uid_1
            if (mediaId.endsWith("_3"))
                mediaId = mediaId.replace("_3", "_1");
            RemoteItem remoteItem = remoteItemWeakReference.get();
            if (remoteItem == null || !remoteItem.getIdentity().equals(mediaId)) return;
            remoteItem.hideLoading();
        }

        @Override
        public void onPlayAudioSuccess(String mediaId) {

        }

        @Override
        public void onPlayVideoSuccess(String mediaId) {

        }

        @Override
        public void onPlayClose(String mediaId) {

        }
    }
}
