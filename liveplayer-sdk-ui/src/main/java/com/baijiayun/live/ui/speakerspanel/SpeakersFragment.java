package com.baijiayun.live.ui.speakerspanel;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseFragment;
import com.baijiayun.live.ui.utils.DisplayUtils;
import com.baijiayun.live.ui.utils.QueryPlus;
import com.baijiayun.live.ui.utils.RxUtils;
import com.baijiayun.live.ui.utils.VideoDefinitionUtil;
import com.baijiayun.live.ui.viewsupport.BJTouchHorizontalScrollView;
import com.baijiayun.bjyrtcengine.BJYRtcEventObserver;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.models.imodels.IMediaModel;
import com.baijiayun.livecore.models.imodels.IUserModel;
import com.baijiayun.livecore.ppt.photoview.OnDoubleTapListener;
import com.baijiayun.livecore.ppt.photoview.OnViewTapListener;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

import static com.baijiayun.live.ui.speakerspanel.SpeakersContract.PPT_TAG;
import static com.baijiayun.live.ui.speakerspanel.SpeakersContract.RECORD_TAG;
import static com.baijiayun.live.ui.speakerspanel.SpeakersContract.VIEW_TYPE_APPLY;
import static com.baijiayun.live.ui.speakerspanel.SpeakersContract.VIEW_TYPE_PPT;
import static com.baijiayun.live.ui.speakerspanel.SpeakersContract.VIEW_TYPE_PRESENTER;
import static com.baijiayun.live.ui.speakerspanel.SpeakersContract.VIEW_TYPE_RECORD;
import static com.baijiayun.live.ui.speakerspanel.SpeakersContract.VIEW_TYPE_SPEAKER;
import static com.baijiayun.live.ui.speakerspanel.SpeakersContract.VIEW_TYPE_VIDEO_PLAY;

/**
 * Created by Shubo on 2017/6/5.
 */

public class SpeakersFragment extends BaseFragment implements SpeakersContract.View, BJTouchHorizontalScrollView.PPTModeCheckListener {

    private SpeakersContract.Presenter presenter;
    private LinearLayout container;
    private RecorderView recorderView;
    private ViewGroup.LayoutParams lpItem;
    private TextView speakerRequest;
    private BJTouchHorizontalScrollView scrollView;
    private static final int SHRINK_THRESHOLD = 3;
    private boolean disableSpeakQueuePlaceholder;
    private CompositeDisposable timerList = new CompositeDisposable();

    @Override
    public int getLayoutId() {
        return R.layout.fragment_speakers;
    }

    @Override
    public void setPresenter(SpeakersContract.Presenter presenter) {
        super.setBasePresenter(presenter);
        this.presenter = presenter;
    }

    @Override
    protected void init(Bundle savedInstanceState) {
        super.init(savedInstanceState);
        container = (LinearLayout) $.id(R.id.fragment_speakers_container).view();
        speakerRequest = (TextView) $.id(R.id.fragment_speakers_new_request_toast).view();
        scrollView = (BJTouchHorizontalScrollView) $.id(R.id.fragment_speakers_scroll_view).view();
        scrollView.setListener(this);
        lpItem = new ViewGroup.LayoutParams(DisplayUtils.dip2px(getActivity(), 100),
                DisplayUtils.dip2px(getActivity(), 76));
        scrollView.getViewTreeObserver().addOnScrollChangedListener(() -> {
            if (scrollView.getScrollX() == scrollView.getChildAt(0).getMeasuredWidth() - scrollView.getMeasuredWidth()) {
                if (speakerRequest.getVisibility() == View.VISIBLE) {
                    speakerRequest.setVisibility(View.GONE);
                }
            }
        });

    }

    @Override
    public boolean isPPTDrawing() {
        if (presenter == null)
            return false;
        return presenter.getPPTFragment() != null && presenter.getPPTFragment().isEditable();
    }

    private boolean attachVideoOnResume = false;
    private boolean attachAudioOnResume = false;

    @Override
    public void onResume() {
        super.onResume();
        if (attachAudioOnResume) {
            attachAudioOnResume = false;
            if (presenter.getRecorder() != null && !presenter.getRecorder().isPublishing())
                presenter.getRecorder().publish();
            if (presenter.getRecorder() != null && !presenter.getRecorder().isAudioAttached())
                presenter.getRecorder().attachAudio();
        }
        Disposable timer = Observable.timer(300, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) {
                        if (attachVideoOnResume) {
                            attachVideoOnResume = false;
                            if (presenter.getRecorder() == null) return;
                            if (!presenter.getRecorder().isPublishing())
                                presenter.getRecorder().publish();
                            if (!presenter.getRecorder().isVideoAttached())
                                presenter.getRecorder().attachVideo();
                        }
                    }
                });
        timerList.add(timer);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (presenter.getRecorder() != null && presenter.getRecorder().isVideoAttached()) {
            presenter.getRecorder().detachVideo();
            attachVideoOnResume = true;
        }
        if (presenter.getRecorder() != null && presenter.getRecorder().isAudioAttached()) {
            presenter.getRecorder().detachAudio();
            attachAudioOnResume = true;
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void notifyItemChanged(int position, IMediaModel iMediaModel) {
        if (container == null) return;
        int viewType = presenter.getItemViewType(position);
        if (viewType == VIEW_TYPE_SPEAKER) {
            container.removeViewAt(position);
            container.addView(generateSpeakView(presenter.getSpeakModel(position)), position);
            presenter.getPlayer().playAudio(presenter.getItem(position));
        } else if (viewType == VIEW_TYPE_PRESENTER) {
            IMediaModel model;
            if (iMediaModel == null)
                model = presenter.getSpeakModel(position);
            else {
                model = iMediaModel;
            }
            if (model == null) return;
            if (model.getUser() == null) return;

            String userId = TextUtils.isEmpty(model.getUser().getUserId()) ? model.getUser().getUserId() : model.getUser().getUserId();


            if (model.isVideoOn() && presenter.isAutoPlay()) {

                VideoView videoView;
                videoView = new VideoView(getContext(), presenter.isNeedLoading(userId));
                videoView.setName(appendName(model.getUser(), true));

                container.removeViewAt(position);
                container.addView(videoView, position, lpItem);

                final GestureDetector gestureDetector = new GestureDetector(getActivity(),
                        new ClickGestureDetector(userId));
                videoView.setOnTouchListener((v, event) -> {
                    gestureDetector.onTouchEvent(event);
                    return true;
                });

                presenter.getPlayer().playAVClose(presenter.getItem(position));
                presenter.getPlayer().playVideo(presenter.getItem(position), videoView.getLpVideoView());
                updateAwardTv(videoView, model);
            } else {
                presenter.getPlayer().playAVClose(presenter.getItem(position));
                if (model.isAudioOn())
                    presenter.getPlayer().playAudio(presenter.getItem(position));
                if (model.getUser() == null) return;
                if (position < container.getChildCount())
                    container.removeViewAt(position);
                container.addView(generateSpeakView(model), position);
            }
        } else if (viewType == VIEW_TYPE_VIDEO_PLAY) {
            IMediaModel model = presenter.getSpeakModel(position);
            container.removeViewAt(position);
            VideoView videoView = new VideoView(getActivity(), presenter.isNeedLoading(model.getUser().getUserId()));
            videoView.setName(appendName(model.getUser(), false));

            container.addView(videoView, position, lpItem);
            updateAwardTv(videoView, model);
            videoView.setAwardCount(presenter.getRewardCount(model.getUser().getNumber()));

            final GestureDetector gestureDetector = new GestureDetector(getActivity(), new ClickGestureDetector(model.getUser().getUserId()));
            videoView.setOnTouchListener((v, event) -> {
                gestureDetector.onTouchEvent(event);
                return true;
            });
            presenter.getPlayer().playAVClose(presenter.getItem(position));
            presenter.getPlayer().playVideo(presenter.getItem(position), videoView.getLpVideoView());
        }
        if (container.getChildCount() > 0) {
            presenter.notifyHavingSpeakers();
        }
    }

    //当前发言视图集合
    private List<View> speakersView = new ArrayList<>();

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void notifyItemInserted(final int position) {
        if (container == null) return;
        switch (presenter.getItemViewType(position)) {
            case VIEW_TYPE_PPT:
                break;
            case VIEW_TYPE_PRESENTER:
                IMediaModel presenterSpeakModel = presenter.getSpeakModel(position);
                if (presenterSpeakModel == null) return;

                String userId = TextUtils.isEmpty(presenterSpeakModel.getMediaId()) ? presenterSpeakModel.getUser().getUserId() : presenterSpeakModel.getMediaId();

                if (presenterSpeakModel.isVideoOn()) {
                    VideoView videoView;
                    videoView = new VideoView(getContext(), presenter.isNeedLoading(userId));
                    videoView.getLpVideoView().setZOrderMediaOverlay(true);
                    videoView.setName(appendName(presenterSpeakModel.getUser(), true));
                    presenter.getPlayer().playVideo(presenter.getItem(position), videoView.getLpVideoView());
                    updateAwardTv(videoView, presenterSpeakModel);
                    container.addView(videoView, position, lpItem);

                    final GestureDetector gestureDetector = new GestureDetector(getActivity(), new ClickGestureDetector(userId));
                    videoView.setOnTouchListener((v, event) -> {
                        gestureDetector.onTouchEvent(event);
                        return true;
                    });
                    videoView.setTag(userId);
                    speakersView.add(videoView);
                } else {
                    View speakerAudioView = generateSpeakView(presenterSpeakModel);
                    speakersView.add(speakerAudioView);
                    container.addView(speakerAudioView, position);
                    if (presenterSpeakModel.isAudioOn()) // 新主讲打开音频
                        presenter.getPlayer().playAudio(presenter.getItem(position));
                }
                break;
            case VIEW_TYPE_RECORD:
                if (recorderView == null) {
                    recorderView = new RecorderView(getActivity());
                    recorderView.setName(appendName(presenter.getCurrentUser(), false));
                    presenter.getRecorder().setPreview(recorderView.getCameraView());
                    recorderView.getCameraView().setZOrderMediaOverlay(true);
                } else {
                    container.removeView(recorderView);
                    presenter.getRecorder().setPreview(recorderView.getCameraView());
                }
                recorderView.setAwardCount(presenter.getRewardCount(presenter.getCurrentUser().getNumber()));
                if (presenter.isTeacherOrAssistant()) {
                    recorderView.setAwardTvVisibility(View.GONE);
                }
                container.addView(recorderView, position, lpItem);
                recorderView.getCameraView().setZOrderMediaOverlay(true);

                final GestureDetector gestureDetector1 = new GestureDetector(getActivity(), new ClickGestureDetector(RECORD_TAG));
                recorderView.setOnTouchListener((v, event) -> {
                    gestureDetector1.onTouchEvent(event);
                    return true;
                });

                if (!presenter.getRecorder().isPublishing())
                    presenter.getRecorder().publish();
                if (!presenter.getRecorder().isVideoAttached()) {
                    Disposable timer = Observable.timer(1, TimeUnit.SECONDS).observeOn(AndroidSchedulers.mainThread())
                            .subscribe(aLong -> presenter.getRecorder().attachVideo());
                    timerList.add(timer);
                }
                break;
            case VIEW_TYPE_VIDEO_PLAY:
                IMediaModel model = presenter.getSpeakModel(position);
                VideoView videoView;
                videoView = new VideoView(getContext(), presenter.isNeedLoading(model.getUser().getUserId()));
                videoView.setName(appendName(model.getUser(), false));
                presenter.getPlayer().playVideo(presenter.getItem(position), videoView.getLpVideoView());
                updateAwardTv(videoView, model);
                videoView.setAwardCount(presenter.getRewardCount(model.getUser().getNumber()));

                container.addView(videoView, position, lpItem);
                final GestureDetector gestureDetector = new GestureDetector(getActivity(), new ClickGestureDetector(model.getUser().getUserId()));
                videoView.setOnTouchListener((v, event) -> {
                    gestureDetector.onTouchEvent(event);
                    return true;
                });
                videoView.setTag(model.getUser().getUserId());
                speakersView.add(videoView);
                break;
            case VIEW_TYPE_SPEAKER:
                IMediaModel speakerModel = presenter.getSpeakModel(position);
                if (speakerModel == null || speakerModel.getUser() == null) return;
                View view = generateSpeakView(speakerModel);
                addView(view, position);
                if (speakerModel.isAudioOn())
                    presenter.getPlayer().playAudio(presenter.getItem(position));
                speakersView.add(view);
                break;
            case VIEW_TYPE_APPLY:
                View applyView = generateApplyView(presenter.getApplyModel(position));
                addView(applyView, position);
                final ViewGroup.LayoutParams params = applyView.getLayoutParams();
                WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
                if ((position + 1) * params.width > wm.getDefaultDisplay().getWidth())
                    speakerRequest.setVisibility(View.VISIBLE);
                speakerRequest.setOnClickListener(v -> {
                    speakerRequest.setAnimation(createAnimation());
                    speakerRequest.startAnimation(createAnimation());
                    speakerRequest.setVisibility(View.GONE);
                    scrollView.smoothScrollTo((position + 1) * params.width, 0);
                });
                break;
            default:
                break;
        }
        presenter.changeBackgroundContainerSize(container.getChildCount() >= SHRINK_THRESHOLD);
        if (getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
                && container.getChildCount() > 0) {
            setBackGroundVisible(true);
        }
        if (container.getChildCount() > 0) {
            presenter.notifyHavingSpeakers();
        }
    }

    private Animation createAnimation() {
        TranslateAnimation animation = new TranslateAnimation(0, 2 * speakerRequest.getWidth(), 0, 0);
        animation.setDuration(500);
        return animation;
    }

    @Override
    public View notifyItemDeleted(int position) {
        View removedView;
        if (container == null) return null;
        if (container.getChildCount() <= position) return null;
        if (position >= 0) {
            removedView = container.getChildAt(position);
            container.removeViewAt(position);
        } else {
            removedView = container.getChildAt(container.getChildCount() - 1);
            container.removeViewAt(container.getChildCount() - 1);
        }
        if (presenter.getItemViewType(position) == VIEW_TYPE_RECORD) {
            if (presenter.getRecorder().isVideoAttached() && !presenter.isStopPublish()) {
                presenter.getRecorder().detachVideo();
            }
            presenter.setIsStopPublish(false);
        } else {
            if (presenter.getPlayer() != null)
                presenter.getPlayer().playAVClose(presenter.getItem(position));
        }
        if (speakerRequest.getVisibility() == View.VISIBLE) {
            speakerRequest.setVisibility(View.GONE);
        }
        presenter.changeBackgroundContainerSize(container.getChildCount() >= SHRINK_THRESHOLD);
        if (getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT
                && container.getChildCount() == 0) {
            setBackGroundVisible(false);
        }
        if (container.getChildCount() == 0) {
            presenter.notifyNoSpeakers();
        }
        speakersView.remove(removedView);
        return removedView;
    }

    private void addView(View view, int position) {
        if (position > container.getChildCount())
            container.addView(view);
        else
            container.addView(view, position);
    }

    @Override
    public void deletedViewAt(int position) {
        if (container == null || container.getChildCount() <= position || position < 0) return;
        container.removeViewAt(position);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void updateView(int position, View videoView) {
        if (container == null) return;
        int viewType = presenter.getItemViewType(position);
        if (viewType == VIEW_TYPE_PRESENTER) {
            IMediaModel model;
            model = presenter.getSpeakModel(position);
            if (model == null) return;
            if (model.getUser() == null) return;

            if (model.isVideoOn() && presenter.isAutoPlay()) {
                String userId = TextUtils.isEmpty(model.getMediaId()) ? model.getUser().getUserId() : model.getMediaId();

                if (!(videoView instanceof VideoView))
                    videoView = new VideoView(getContext(), presenter.isNeedLoading(userId));
                ((VideoView) videoView).setName(appendName(model.getUser(), true));

                container.removeViewAt(position);
                container.addView(videoView, position, lpItem);

                final GestureDetector gestureDetector = new GestureDetector(getActivity(),
                        new ClickGestureDetector(userId));
                videoView.setOnTouchListener((v, event) -> {
                    gestureDetector.onTouchEvent(event);
                    return true;
                });

                presenter.getPlayer().playAVClose(presenter.getItem(position));
                presenter.getPlayer().playVideo(presenter.getItem(position), ((VideoView) videoView).getLpVideoView());
//                if (presenter.isUseWebRTC() && model.isAudioOn())
//                    presenter.getPlayer().playAudio(presenter.getItem(position));
                updateAwardTv((VideoView) videoView, model);
            } else {
                if (model.isAudioOn())
                    presenter.getPlayer().playAudio(presenter.getItem(position));
                if (model.getUser() == null) return;
                container.removeViewAt(position);
                container.addView(generateSpeakView(model), position);
            }
        } else if (viewType == VIEW_TYPE_VIDEO_PLAY) {
            IMediaModel model = presenter.getSpeakModel(position);

            if (!(videoView instanceof VideoView))
                videoView = new VideoView(getActivity(), presenter.isNeedLoading(model.getUser().getUserId()));
            ((VideoView) videoView).setName(appendName(model.getUser(), false));

            container.removeViewAt(position);
            container.addView(videoView, position, lpItem);

            final GestureDetector gestureDetector = new GestureDetector(getActivity(), new ClickGestureDetector(model.getUser().getUserId()));
            videoView.setOnTouchListener((v, event) -> {
                gestureDetector.onTouchEvent(event);
                return true;
            });
            presenter.getPlayer().playAVClose(presenter.getItem(position));
            presenter.getPlayer().playVideo(presenter.getItem(position), ((VideoView) videoView).getLpVideoView());
//            if (presenter.isUseWebRTC() && model.isAudioOn())
//                presenter.getPlayer().playAudio(presenter.getItem(position));
            updateAwardTv((VideoView) videoView, model);
        }
        if (container.getChildCount() > 0) {
            presenter.notifyHavingSpeakers();
        }
    }

    /**
     * 更新音视频窗口的网络状态
     *
     * @param remoteStreamStats
     */
    @Override
    public void updateNetworkTips(BJYRtcEventObserver.RemoteStreamStats remoteStreamStats) {
        if (getContext() == null) {
            return;
        }
        //拉流是主讲
        if (presenter.getLiveRoom().getPresenterUser() != null && remoteStreamStats.uid.equals(presenter.getLiveRoom().getPresenterUser().getUserId())) {
            double lossRate = presenter.isPresenterVideoOn() ? remoteStreamStats.receivedVideoLostRate : remoteStreamStats.receivedAudioLossRate;
            if (lossRate > presenter.getPresenterUpLinkLossRate() * 2) {
                if (recorderView != null) {
                    updateNetworkTips(recorderView.getNetworkTextView(), lossRate);
                }
                return;
            }
        }

        for (View view : speakersView) {
            String userId = (String) view.getTag();
            String keyUserId = remoteStreamStats.uid;
            double lossRate = remoteStreamStats.receivedVideoLostRate;
            //视频播放窗口
            if (keyUserId.equals(userId) && view instanceof VideoView) {
                updateNetworkTips(((VideoView) view).getVideoViewNetworkTextView(), lossRate);
                break;
            }
            //音频播放窗口
            userId = (String) view.getTag(R.id.lp_speaker_audio_type_tag);
            lossRate = remoteStreamStats.receivedAudioLossRate;
            if (keyUserId.equals(userId)) {
                updateNetworkTips(view.findViewById(R.id.item_speak_speaker_network), lossRate);
                break;
            }
        }
    }

    private void updateNetworkTips(TextView textView, double lossRate) {
        List<Integer> lossRateLevelList = presenter.getLiveRoom().getPartnerConfig().packetLossRate.packetLossRateLevel;
        if (lossRate < lossRateLevelList.get(0)) {
            textView.setText("");
        } else if (lossRate < lossRateLevelList.get(1)) {
            textView.setText(getString(R.string.live_network_tips_level_1));
            textView.setTextColor(getContext().getResources().getColor(R.color.live_low_network_tips_middle));
        } else if (lossRate < lossRateLevelList.get(2)) {
            textView.setText(getString(R.string.live_network_tips_level_2));
            textView.setTextColor(getContext().getResources().getColor(R.color.live_low_network_tips_middle));
        } else {
            textView.setText(getString(R.string.live_network_tips_level_3));
            textView.setTextColor(getContext().getResources().getColor(R.color.live_low_network_tips_terrible));
        }
    }

    @Override
    public View removeViewAt(int position) {
        View view = container.getChildAt(position);
        container.removeView(view);
        return view;
    }

    public void pptResume() {
        presenter.getPPTFragment().setOnViewTapListener(new OnViewTapListener() {
            @Override
            public void onViewTap(View view, float x, float y) {
                showOptionDialog();
            }
        });
        presenter.getPPTFragment().setDoubleTapScaleEnable(false);
        presenter.getPPTFragment().setOnDoubleTapListener(new OnDoubleTapListener() {
            @Override
            public void onDoubleTapConfirmed() {
                presenter.setFullScreenTag(PPT_TAG);
                if (presenter.getPPTFragment() != null)
                    presenter.getPPTFragment().setDoubleTapScaleEnable(true);
            }
        });
    }

    @Override
    public void notifyViewAdded(View view, int position) {
        if (view == null || view.getParent() != null) return;
        if (view instanceof VideoView) {
            ((VideoView) view).getLpVideoView().setZOrderMediaOverlay(true);
            ((VideoView) view).setAwardTvVisibility(View.VISIBLE);
            IMediaModel model = presenter.getSpeakModel(position);
            if (model == null) return;
            if (model.getUser() != null && (model.getUser().getType() == LPConstants.LPUserType.Teacher || model.getUser().getType() == LPConstants.LPUserType.Assistant)) {
                ((VideoView) view).setAwardTvVisibility(View.GONE);
            }
        }
        if (view instanceof RecorderView) {
            ((RecorderView) (view)).setZOrderMediaOverlay(true);
            ((RecorderView) view).setAwardTvVisibility(View.VISIBLE);
            if (presenter.isTeacherOrAssistant()) {
                ((RecorderView) view).setAwardTvVisibility(View.GONE);
            }
        }
        if (position > container.getChildCount())
            container.addView(view, lpItem);
        else
            container.addView(view, position, lpItem);
        if (presenter.getPPTFragment() == view) {
            presenter.getPPTFragment().onStart();
            presenter.getPPTFragment().setDoubleTapScaleEnable(false);
            presenter.getPPTFragment().setOnViewTapListener(new OnViewTapListener() {
                @Override
                public void onViewTap(View view, float x, float y) {
                    showOptionDialog(PPT_TAG);
                }
            });
            presenter.getPPTFragment().setOnDoubleTapListener(new OnDoubleTapListener() {
                @Override
                public void onDoubleTapConfirmed() {
                    presenter.setFullScreenTag(PPT_TAG);
                }
            });
        } else if (view instanceof RecorderView) {
            presenter.getRecorder().invalidVideo();
        }
    }

    @Override
    public void showMaxVideoExceed() {
        showToast(getString(R.string.live_speakers_max_video_exceed));
    }

    @Override
    public void disableSpeakQueuePlaceholder() {
        disableSpeakQueuePlaceholder = true;
        $.id(R.id.fragment_speakers_scroll_view).backgroundDrawable(null);
    }

    @Override
    public View getChildAt(int position) {
        if (container == null || position >= container.getChildCount()) return null;
        return container.getChildAt(position);
    }

    @Override
    public void showOptionDialog() {
        showOptionDialog(PPT_TAG);
    }

    @Override
    public void stopLoadingAnimation(final int position) {
        if (container == null || position >= container.getChildCount() || container.getChildCount() <= 0)
            return;
        if (getActivity() == null) return;
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (position < 0) {
                    for (int i = 0; i < container.getChildCount(); i++) {
                        View view = container.getChildAt(i);
                        if (view instanceof VideoView)
                            ((VideoView) view).stopRotate();
                    }
                    return;
                }
                View view = container.getChildAt(position);
                if (view instanceof VideoView)
                    ((VideoView) view).stopRotate();
            }
        });
    }

    @Override
    public void showToast(String s) {
        super.showToast(s);
    }

    @Override
    public void notifyAwardCountChange(int position, int awardCount) {
        View view = container.getChildAt(position);
        if (view instanceof VideoView) {
            ((VideoView) view).setAwardCount(awardCount);
        }
        if (view instanceof RecorderView) {
            ((RecorderView) view).setAwardCount(awardCount);
        }
    }

    private View generateSpeakView(final IMediaModel model) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.item_speak_speaker, container, false);
        QueryPlus q = QueryPlus.with(view);
        String userId = model.getUser().getUserId();
        if (presenter.getItemViewType(userId) == VIEW_TYPE_PRESENTER) {
            String teacherLable = presenter.getLiveRoom().getCustomizeTeacherLabel();
            teacherLable = TextUtils.isEmpty(teacherLable) ? getString(R.string.live_teacher_hint) : "(" + teacherLable + ")";
            q.id(R.id.item_speak_speaker_name).text(model.getUser().getName() + (model.getUser().getType() == LPConstants.LPUserType.Teacher ? teacherLable : getString(R.string.live_presenter_hint)));
        } else
            q.id(R.id.item_speak_speaker_name).text(model.getUser().getName());
        if (!TextUtils.isEmpty(model.getUser().getAvatar())) {
            String avatar = model.getUser().getAvatar().startsWith("//") ? "https:" + model.getUser().getAvatar() : model.getUser().getAvatar();
            q.id(R.id.item_speak_speaker_avatar).image(getActivity(), avatar);
        }
        q.id(R.id.item_speak_speaker_video_label).visibility(model.isVideoOn() ? View.VISIBLE : View.GONE);
        q.contentView().setOnClickListener(v -> showOptionDialog(userId));
        view.setTag(R.id.lp_speaker_audio_type_tag, model.getUser().getUserId());
        return view;
    }

    @SuppressLint("CheckResult")
    private View generateApplyView(final IUserModel model) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.item_speak_apply, container, false);
        QueryPlus q = QueryPlus.with(view);
        q.id(R.id.item_speak_apply_avatar).image(getActivity(), model.getAvatar());
        q.id(R.id.item_speak_apply_name).text(model.getName() + getContext().getString(R.string.live_media_speak_applying));
        q.id(R.id.item_speak_apply_agree).clicked().subscribe(integer -> presenter.agreeSpeakApply(model.getUserId()));
        q.id(R.id.item_speak_apply_disagree).clicked().subscribe(integer -> presenter.disagreeSpeakApply(model.getUserId()));
        return view;
    }

    private class ClickGestureDetector extends GestureDetector.SimpleOnGestureListener {

        private String tag;

        ClickGestureDetector(String tag) {
            this.tag = tag;
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (!clickableCheck()) {
                return super.onSingleTapConfirmed(e);
            }
            if (!presenter.isFullScreen(tag)) {
                showOptionDialog(tag);
            } else {
                // clear screen
                if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    presenter.clearScreen();
                }
            }
            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            if (!clickableCheck()) {
                return super.onDoubleTap(e);
            }
            if (!presenter.isFullScreen(tag)) {
                presenter.setFullScreenTag(tag);
            }
            return super.onDoubleTap(e);
        }
    }

    private void showOptionDialog(final String tag) {
        if (TextUtils.isEmpty(tag)) return;
        List<String> options = new ArrayList<>();
        int viewType = presenter.getItemViewType(tag);
        if (viewType == -1) return;
        switch (viewType) {
            case VIEW_TYPE_PPT:
                options.add(getString(R.string.live_full_screen));
                break;
            case VIEW_TYPE_PRESENTER:
                IMediaModel presenterModel = presenter.getSpeakModel(tag);
                if (presenterModel == null || presenterModel.getUser() == null) return; // 主讲人音视频未开启
                if (presenterModel.isVideoOn() && presenter.isAutoPlay()) {
                    options.add(getString(R.string.live_full_screen));
                    if (presenter.isEnableSwitchPresenter() && presenter.isCurrentTeacher() && presenterModel.getUser().getType() == LPConstants.LPUserType.Assistant) {
                        options.add(getString(R.string.live_unset_presenter));
                    }
                    if (presenterModel.getVideoDefinitions().size() > 1)
                        options.add(getString(R.string.live_switch_definitions));
                    options.add(getString(R.string.live_close_video));

                } else if (presenterModel.isVideoOn() && !presenter.isAutoPlay()) {
                    options.add(getString(R.string.live_open_video));
                } else if (presenterModel.isAudioOn()) {
                    if (presenter.isEnableSwitchPresenter() && presenter.isCurrentTeacher() && presenterModel.getUser().getType() == LPConstants.LPUserType.Assistant) {
                        options.add(getString(R.string.live_unset_presenter));
                    }
                }
                break;
            case VIEW_TYPE_RECORD:
                options.add(getString(R.string.live_full_screen));
                if (presenter.isEnableSwitchPresenter() && presenter.isCurrentTeacher() && presenter.getPresenter() != null && !presenter.getPresenter().getUserId().equals(tag))
                    options.add(getString(R.string.live_set_to_presenter));
                options.add(getString(R.string.live_recorder_switch_camera));
                if (presenter.getRecorder().isBeautyFilterOn()) {
                    options.add(getString(R.string.live_recorder_pretty_filter_off));
                } else {
                    options.add(getString(R.string.live_recorder_pretty_filter_on));
                }
                options.add(getString(R.string.live_close_video));
                break;
            case VIEW_TYPE_VIDEO_PLAY:
                options.add(getString(R.string.live_full_screen));
                IMediaModel iMediaModel = presenter.getSpeakModel(tag);
                if (iMediaModel == null || iMediaModel.getUser() == null) return;
                if (presenter.isEnableSwitchPresenter() && presenter.isCurrentTeacher() && (iMediaModel.getUser().getType() == LPConstants.LPUserType.Teacher || iMediaModel.getUser().getType() == LPConstants.LPUserType.Assistant))
                    options.add(getString(R.string.live_set_to_presenter));
                if (iMediaModel.getUser().getType() == LPConstants.LPUserType.Student
                        && presenter.isTeacherOrAssistant()) {
                    if (!presenter.isEnableGrantDrawing()) {
                        if (presenter.isHasDrawingAuth(tag))
                            options.add(getString(R.string.live_unset_auth_drawing));
                        else
                            options.add(getString(R.string.live_set_auth_drawing));
                    }
                }
                if (iMediaModel.getVideoDefinitions().size() > 1)
                    options.add(getString(R.string.live_switch_definitions));
                if (presenter.isTeacherOrAssistant() && iMediaModel.getUser().getType() != LPConstants.LPUserType.Assistant
                        && iMediaModel.getUser().getType() != LPConstants.LPUserType.Teacher) {
                    options.add(getString(R.string.live_award));
                }
                options.add(getString(R.string.live_close_video));
                if (presenter.isTeacherOrAssistant() && presenter.isMultiClass() && iMediaModel.getUser().getType() != LPConstants.LPUserType.Teacher)
                    options.add(getString(R.string.live_close_speaking));
                break;
            case VIEW_TYPE_SPEAKER:
                IMediaModel model = presenter.getSpeakModel(tag);
                if (model != null && model.getUser() != null) {
                    if (model.isVideoOn())
                        options.add(getString(R.string.live_open_video));
                    if (presenter.isEnableSwitchPresenter() && presenter.isCurrentTeacher() && (model.getUser().getType() == LPConstants.LPUserType.Teacher || model.getUser().getType() == LPConstants.LPUserType.Assistant))
                        options.add(getString(R.string.live_set_to_presenter));
                    if (model.getUser().getType() == LPConstants.LPUserType.Student
                            && presenter.isTeacherOrAssistant()) {
                        if (!presenter.isEnableGrantDrawing()) {
                            if (presenter.isHasDrawingAuth(tag))
                                options.add(getString(R.string.live_unset_auth_drawing));
                            else
                                options.add(getString(R.string.live_set_auth_drawing));
                        }
                    }
                    if (presenter.isTeacherOrAssistant() && presenter.isMultiClass())
                        options.add(getString(R.string.live_close_speaking));
                }
                break;
            default:
                break;
        }
        if (options.size() <= 0) return;
        Activity activity = getActivity();
        if (activity == null) return;
        if (activity.isFinishing()) return;
        new MaterialDialog.Builder(activity)
                .items(options)
                .itemsCallback((materialDialog, view, i, charSequence) -> {
                    if (getActivity() == null) return;
                    if (getString(R.string.live_close_video).equals(charSequence.toString())) {
                        presenter.closeVideo(tag);
                    } else if (getString(R.string.live_close_speaking).equals(charSequence.toString())) {
                        presenter.closeSpeaking(tag);
                    } else if (getString(R.string.live_open_video).equals(charSequence.toString())) {
                        presenter.playVideo(tag);
                    } else if (getString(R.string.live_full_screen).equals(charSequence.toString())) {
                        presenter.setFullScreenTag(tag);
                    } else if (getString(R.string.live_recorder_switch_camera).equals(charSequence.toString())) {
                        presenter.switchCamera();
                    } else if (getString(R.string.live_recorder_pretty_filter_off).equals(charSequence.toString())
                            || getString(R.string.live_recorder_pretty_filter_on).equals(charSequence.toString())) {
                        presenter.switchPrettyFilter();
                    } else if (getString(R.string.live_switch_definitions).equals(charSequence.toString())) {
                        showVideoDefinitionSwitchDialog(tag);
                    } else if (getString(R.string.live_set_to_presenter).equals(charSequence.toString())) {
                        presenter.requestPresenterChange(tag, true);
                    } else if (getString(R.string.live_unset_presenter).equals(charSequence.toString())) {
                        presenter.requestPresenterChange(tag, false);
                    } else if (getString(R.string.live_set_auth_drawing).equals(charSequence.toString())) {
                        presenter.requestStudentDrawingAuth(tag, true);
                    } else if (getString(R.string.live_unset_auth_drawing).equals(charSequence.toString())) {
                        presenter.requestStudentDrawingAuth(tag, false);
                    } else if (getString(R.string.live_award).equals(charSequence.toString())) {
                        presenter.requestAward(presenter.getSpeakModel(tag).getUser().getNumber());
                    }
                    materialDialog.dismiss();
                })
                .show();
    }

    private void showVideoDefinitionSwitchDialog(final String tag) {
        IMediaModel iMediaModel = presenter.getSpeakModel(tag);
        if (iMediaModel == null || iMediaModel.getVideoDefinitions().size() <= 1) return;
        List<String> options = new ArrayList<>();
        for (LPConstants.VideoDefinition definition : iMediaModel.getVideoDefinitions()) {
            options.add(VideoDefinitionUtil.getVideoDefinitionLabelFromType(definition));
        }
        if (getActivity() == null) return;
        new MaterialDialog.Builder(getActivity())
                .items(options)
                .itemsCallback((dialog, itemView, position, text) -> {
                    presenter.switchVideoDefinition(tag, VideoDefinitionUtil.getVideoDefinitionTypeFromLabel(text.toString()));
                    dialog.dismiss();
                })
                .show();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            setBackGroundVisible(true);
        } else {
            if (container.getChildCount() >= 3) {
                setBackGroundVisible(true);
            } else {
                setBackGroundVisible(false);
            }
        }
    }

    private Disposable subscriptionOfClickable;

    private boolean clickableCheck() {
        if (presenter == null || (subscriptionOfClickable != null && !subscriptionOfClickable.isDisposed())) {
            return false;
        }
        subscriptionOfClickable = Observable.timer(1, TimeUnit.SECONDS).subscribe(new Consumer<Long>() {
            @Override
            public void accept(Long aLong) {
                RxUtils.dispose(subscriptionOfClickable);
            }
        });
        return true;
    }

    public void setBackGroundVisible(boolean visible) {
        if (disableSpeakQueuePlaceholder) {
            $.id(R.id.fragment_speakers_scroll_view).backgroundDrawable(null);
            return;
        }
        if (visible) {
            if (container.getChildCount() == 0)
                return;
            $.id(R.id.fragment_speakers_scroll_view).backgroundDrawable(ContextCompat.getDrawable(getActivity(), R.color.live_text_color_light));
        } else {
            $.id(R.id.fragment_speakers_scroll_view).backgroundDrawable(null);
        }
    }

    private String appendName(IUserModel userModel, boolean isPresenter) {
        String name = userModel.getName();
        if (userModel.getType() == LPConstants.LPUserType.Teacher) {
            String teacherLable = presenter.getLiveRoom().getCustomizeTeacherLabel();
            name += TextUtils.isEmpty(teacherLable) ? getString(R.string.live_teacher_hint) : "(" + teacherLable + ")";
        } else if (userModel.getType() == LPConstants.LPUserType.Assistant) {
            if (!isPresenter) {
                String assisLabel = presenter.getLiveRoom().getCustomizeAssistantLabel();
                name += TextUtils.isEmpty(assisLabel) ? "" : "(" + assisLabel + ")";
            } else
                name += getString(R.string.live_presenter_hint);
        }
        return name;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter = null;
        timerList.clear();
        speakersView.clear();
    }

    private void updateAwardTv(VideoView videoView, IMediaModel model) {
        if (presenter.isTeacherOrAssistant() && model.getUser().getType() != LPConstants.LPUserType.Assistant && model.getUser().getType() != LPConstants.LPUserType.Teacher) {
            videoView.setAwardTvVisibility(View.VISIBLE);
        } else {
            videoView.setAwardTvVisibility(View.GONE);
        }
    }
}
