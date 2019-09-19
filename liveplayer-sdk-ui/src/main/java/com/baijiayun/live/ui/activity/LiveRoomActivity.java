package com.baijiayun.live.ui.activity;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.baijiayun.live.ui.BuildConfig;
import com.baijiayun.live.ui.LiveSDKWithUI;
import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.announcement.AnnouncementFragment;
import com.baijiayun.live.ui.announcement.AnnouncementPresenter;
import com.baijiayun.live.ui.answersheet.QuestionShowFragment;
import com.baijiayun.live.ui.answersheet.QuestionShowPresenter;
import com.baijiayun.live.ui.answersheet.QuestionToolFragment;
import com.baijiayun.live.ui.answersheet.QuestionToolPresenter;
import com.baijiayun.live.ui.base.BasePresenter;
import com.baijiayun.live.ui.base.BaseView;
import com.baijiayun.live.ui.base.DragFragment;
import com.baijiayun.live.ui.chat.ChatFragment;
import com.baijiayun.live.ui.chat.ChatPresenter;
import com.baijiayun.live.ui.chat.MessageSendPresenter;
import com.baijiayun.live.ui.chat.MessageSentFragment;
import com.baijiayun.live.ui.chat.preview.ChatPictureViewFragment;
import com.baijiayun.live.ui.chat.preview.ChatPictureViewPresenter;
import com.baijiayun.live.ui.chat.preview.ChatSavePicDialogFragment;
import com.baijiayun.live.ui.chat.preview.ChatSavePicDialogPresenter;
import com.baijiayun.live.ui.cloudrecord.CloudRecordFragment;
import com.baijiayun.live.ui.cloudrecord.CloudRecordPresenter;
import com.baijiayun.live.ui.error.ErrorFragment;
import com.baijiayun.live.ui.function.redpacket.RedPacketFragment;
import com.baijiayun.live.ui.function.redpacket.RedPacketPresenter;
import com.baijiayun.live.ui.leftmenu.LeftMenuFragment;
import com.baijiayun.live.ui.leftmenu.LeftMenuPresenter;
import com.baijiayun.live.ui.loading.LoadingFragment;
import com.baijiayun.live.ui.loading.LoadingPresenter;
import com.baijiayun.live.ui.more.MoreMenuDialogFragment;
import com.baijiayun.live.ui.more.MoreMenuPresenter;
import com.baijiayun.live.ui.ppt.MyPPTView;
import com.baijiayun.live.ui.ppt.PPTPresenter;
import com.baijiayun.live.ui.ppt.quickswitchppt.QuickSwitchPPTFragment;
import com.baijiayun.live.ui.ppt.quickswitchppt.SwitchPPTFragmentPresenter;
import com.baijiayun.live.ui.pptleftmenu.PPTLeftFragment;
import com.baijiayun.live.ui.pptleftmenu.PPTLeftPresenter;
import com.baijiayun.live.ui.pptmanage.PPTManageFragment;
import com.baijiayun.live.ui.pptmanage.PPTManagePresenter;
import com.baijiayun.live.ui.questionanswer.QuestionAnswerFragment;
import com.baijiayun.live.ui.questionanswer.QuestionAnswerPresenter;
import com.baijiayun.live.ui.quiz.QuizDialogFragment;
import com.baijiayun.live.ui.quiz.QuizDialogPresenter;
import com.baijiayun.live.ui.rightbotmenu.RightBottomMenuFragment;
import com.baijiayun.live.ui.rightbotmenu.RightBottomMenuPresenter;
import com.baijiayun.live.ui.rightmenu.RightMenuFragment;
import com.baijiayun.live.ui.rightmenu.RightMenuPresenter;
import com.baijiayun.live.ui.rollcall.RollCallDialogFragment;
import com.baijiayun.live.ui.rollcall.RollCallDialogPresenter;
import com.baijiayun.live.ui.setting.SettingDialogFragment;
import com.baijiayun.live.ui.setting.SettingPresenter;
import com.baijiayun.live.ui.share.LPShareDialog;
import com.baijiayun.live.ui.speakerlist.SpeakersFragment;
import com.baijiayun.live.ui.speakerlist.SpeakersPresenter;
import com.baijiayun.live.ui.speakerlist.item.Switchable;
import com.baijiayun.live.ui.speakerlist.AwardView;
import com.baijiayun.live.ui.topbar.TopBarFragment;
import com.baijiayun.live.ui.topbar.TopBarPresenter;
import com.baijiayun.live.ui.users.OnlineUserDialogFragment;
import com.baijiayun.live.ui.users.OnlineUserPresenter;
import com.baijiayun.live.ui.utils.DisplayUtils;
import com.baijiayun.live.ui.utils.FileUtil;
import com.baijiayun.live.ui.utils.JsonObjectUtil;
import com.baijiayun.live.ui.utils.RxUtils;
import com.baijiayun.live.ui.viewsupport.dialog.SimpleTextDialog;
import com.baijiayun.livecore.LiveSDK;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.context.LPError;
import com.baijiayun.livecore.context.LiveRoom;
import com.baijiayun.livecore.listener.LPLaunchListener;
import com.baijiayun.livecore.listener.OnPhoneRollCallListener;
import com.baijiayun.livecore.models.LPAnswerModel;
import com.baijiayun.livecore.models.LPJsonModel;
import com.baijiayun.livecore.models.LPRedPacketModel;
import com.baijiayun.livecore.models.LPRoomForbidChatResult;
import com.baijiayun.livecore.models.imodels.IMediaControlModel;
import com.baijiayun.livecore.models.imodels.IMediaModel;
import com.baijiayun.livecore.models.imodels.IUserModel;
import com.baijiayun.livecore.models.roomresponse.LPResRoomMediaControlModel;
import com.baijiayun.livecore.ppt.listener.OnPPTStateListener;
import com.baijiayun.livecore.utils.LPSdkVersionUtils;
import com.baijiayun.livecore.wrapper.model.LPAVMediaModel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;

import static android.R.attr.path;
import static com.baijiayun.live.ui.utils.Precondition.checkNotNull;

public class LiveRoomActivity extends LiveRoomBaseActivity implements LiveRoomRouterListener {
    private RelativeLayout rlContainer;
    private RelativeLayout flBackground;
    private FrameLayout flLeft;
    private FrameLayout flLoading;
    private FrameLayout flError;
    private DrawerLayout dlChat;
    private FrameLayout flPPTLeft;
    private FrameLayout flRightBottom;
    private FrameLayout flRight;
    private FrameLayout flSpeakers;
    private FrameLayout flCloudRecord;
    private DragFragment flQuestionTool;
    private LiveRoom liveRoom;
    private LoadingFragment loadingFragment;
    private TopBarFragment topBarFragment;
    private CloudRecordFragment cloudRecordFragment;
    private MyPPTView lppptView;
    private ChatFragment chatFragment;
    private ChatPresenter chatPresenter;
    private RightBottomMenuFragment rightBottomMenuFragment;
    private LeftMenuFragment leftMenuFragment;
    private RightMenuFragment rightMenuFragment;
    private QuestionToolFragment questionToolFragment;
    private QuestionShowFragment questionShowFragment;
    private WindowManager windowManager;
    private SpeakersFragment speakersFragment;
    private SpeakersPresenter speakersPresenter;
    private RightMenuPresenter rightMenuPresenter;
    private PPTManagePresenter pptManagePresenter;
    private ErrorFragment errorFragment;
    private GlobalPresenter globalPresenter;
    private PPTLeftFragment pptLeftFragment;
    private RollCallDialogPresenter rollCallDialogPresenter;
    private QuizDialogFragment quizFragment;
    private QuizDialogPresenter quizPresenter;
    private MessageSendPresenter messageSendPresenter;
    private AnnouncementFragment announcementFragment;
    private QuestionAnswerFragment questionAnswerFragment;
    private OrientationEventListener orientationEventListener; //处理屏幕旋转时本地视频的方向
    private int oldRotation;

    //试听码dialog提示
    private SimpleTextDialog mAuditionEndDialog;

    //红包
    private RedPacketFragment mRedPacketFragment;
    private RedPacketPresenter mRedPacketPresenter;

    //白板/PPT页展示
    private SwitchPPTFragmentPresenter switchPPTFragmentPresenter;
    private OnPPTStateListener mOnPPTStateListener;

    private Disposable subscriptionOfLoginConflict, subscriptionOfStreamInfo,
            subscriptionOfMarquee;

    private boolean isClearScreen; //是否已经清屏，作用于视频采集和远程视频ui的调整
    private String code, name, avatar;
    private boolean isBackgroundContainerShrink = false; //缩小背景框

    private long roomId;
    private String sign;
    private static String liveHorseLamp; // 跑马灯
    private static int liveHorseLampInterval = 60; // 跑马灯时间间隔
    private IUserModel enterUser;
    private boolean mobileNetworkDialogShown = false;
    private MaterialDialog speakInviteDlg;  //取消用
    private EditText etDebugAecDelay;
    private int aecMode = 0, aecmMode = 0, audioSource = 0, delay;
    private boolean isCommunication = true;
    private TextView tvStreamInfo;
    private LinearLayout messageReminderContainer;
    private List<IMediaModel> userMediaModels;
    private Disposable subscriptionOfIsCloudRecordAllowed;
    private int minVolume;
    private IUserModel privateChatUser;
    private LPError lpError;

    private Disposable subscriptionOfTeacherAbsent;

    private AwardView awardView;

    private CompositeDisposable timerList = new CompositeDisposable();

    private final String TAG = LiveRoomActivity.class.getCanonicalName();

    private Handler mHandler = new Handler();
    private LPAnswerModel lpAnswerModel;

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("code", code);
        outState.putString("name", name);
        outState.putString("avatar", avatar);
        outState.putLong("roomId", roomId);
        outState.putString("sign", sign);
        outState.putSerializable("user", enterUser);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { // 5.0+ 打开硬件加速
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_live_room);

        LiveSDK.AUTO_PLAY_SHARING_SCREEN_AND_MEDIA = true;
        initViews();

        // x86平台的机器不让进教室
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (Build.SUPPORTED_ABIS[0].contains("x86")) {
                showMessage(getString(R.string.live_room_x86_not_supported));
                super.finish();
            }
        } else {
            if (Build.CPU_ABI.contains("x86")) {
                showMessage(getString(R.string.live_room_x86_not_supported));
                super.finish();
            }
        }

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            onConfigurationChanged(getResources().getConfiguration());
        }
        if (savedInstanceState == null) {
            code = getIntent().getStringExtra("code");
            name = getIntent().getStringExtra("name");
            avatar = getIntent().getStringExtra("avatar");
            roomId = getIntent().getLongExtra("roomId", -1);
            sign = getIntent().getStringExtra("sign");
            enterUser = (IUserModel) getIntent().getSerializableExtra("user");
        } else {
            code = savedInstanceState.getString("code");
            name = savedInstanceState.getString("name");
            avatar = savedInstanceState.getString("avatar");
            roomId = savedInstanceState.getLong("roomId", -1);
            sign = savedInstanceState.getString("sign");
            enterUser = (IUserModel) savedInstanceState.getSerializable("user");
        }

        loadingFragment = LoadingFragment.newInstance(true, shouldShowTechSupport);
        LoadingPresenter loadingPresenter;
        if (roomId == -1) {
            loadingPresenter = new LoadingPresenter(loadingFragment, code, name, avatar);
        } else {
            loadingPresenter = new LoadingPresenter(loadingFragment, roomId, sign, enterUser);
        }
        bindVP(loadingFragment, loadingPresenter);
        addFragment(R.id.activity_live_room_loading, loadingFragment);

        windowManager = (WindowManager) this
                .getSystemService(WINDOW_SERVICE);
        oldRotation = windowManager.getDefaultDisplay().getRotation();

        orientationEventListener = new OrientationEventListener(this, SensorManager.SENSOR_DELAY_FASTEST) {
            @Override
            public void onOrientationChanged(int orientation) {
                try {
                    int newRotation = windowManager.getDefaultDisplay().getRotation();
                    if (newRotation != oldRotation) {
                        oldRotation = newRotation;
                        if (liveRoom.getRecorder().isVideoAttached())
                            liveRoom.getRecorder().invalidVideo();
                    }
                } catch (Exception ignored) {
                }
            }
        };
        dlChat.openDrawer(GravityCompat.START);
        checkScreenOrientationInit();
    }

    private boolean isDlchatOpen = false;

    private void initViews() {
        flBackground = findViewById(R.id.activity_live_room_background_container);
        DisplayMetrics displayMetrics = new DisplayMetrics();// 强制设置白版高度
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        int displayWidth = displayMetrics.widthPixels;
        ViewGroup.LayoutParams params = flBackground.getLayoutParams();
        params.height = displayWidth * 3 / 4;
        flBackground.setLayoutParams(params);
        flLeft = findViewById(R.id.activity_live_room_bottom_left);
        flLoading = findViewById(R.id.activity_live_room_loading);
        dlChat = findViewById(R.id.activity_live_room_chat_drawer);
        flPPTLeft = findViewById(R.id.activity_live_room_ppt_left);
        flError = findViewById(R.id.activity_live_room_error);
        flRightBottom = findViewById(R.id.activity_live_room_bottom_right);
        flRight = findViewById(R.id.activity_live_room_right);
        flSpeakers = findViewById(R.id.activity_live_room_speakers_container);
        flCloudRecord = findViewById(R.id.activity_live_room_cloud_record);
        rlContainer = findViewById(R.id.activity_live_room_container);
        flQuestionTool = findViewById(R.id.activity_dialog_question_tool);

        dlChat.addDrawerListener(new DrawerLayout.DrawerListener() {
            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                isDlchatOpen = true;
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                isDlchatOpen = false;
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                if (isDlchatOpen && newState == 0) {
                    dlChat.setTag(true);
                    dlChat.isDrawerOpen(dlChat);

                } else if (!isDlchatOpen && newState == 0) {
                    dlChat.setTag(false);
                    dlChat.isDrawerOpen(dlChat);
                }
            }
        });
        awardView = (AwardView) findViewById(R.id.award_view);
    }

    @Override
    protected void onResume() {
        super.onResume();
        orientationEventListener.enable();

        if (roomLifeCycleListener != null) {
            roomLifeCycleListener.onResume(this, (code, nickName) -> {
                //重新进入教室
                flBackground.removeAllViews();
                removeFragment(topBarFragment);
                removeFragment(cloudRecordFragment);
                removeFragment(leftMenuFragment);
                removeFragment(pptLeftFragment);
                removeFragment(rightMenuFragment);
                removeFragment(rightBottomMenuFragment);
                removeFragment(chatFragment);
                removeFragment(speakersFragment);
                if (errorFragment != null && errorFragment.isAdded())
                    removeFragment(errorFragment);
                removeAllFragment();
                loadingFragment = LoadingFragment.newInstance(LiveSDK.checkTeacherUnique, shouldShowTechSupport);
                LoadingPresenter loadingPresenter = new LoadingPresenter(loadingFragment, code, nickName, null);
                bindVP(loadingFragment, loadingPresenter);
                addFragment(R.id.activity_live_room_loading, loadingFragment);
            });
        }

        if (globalPresenter != null) {
            globalPresenter.switchBackstage(false);
        }
    }


    @Override
    protected void onPause() {
        super.onPause();
        orientationEventListener.disable();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (globalPresenter != null) {
            globalPresenter.switchBackstage(true);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            dlChat.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
            if (flQuestionTool.getVisibility() == View.VISIBLE) {
                flQuestionTool.configurationChanged();
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                flQuestionTool.setLayoutParams(layoutParams);
            }
            if (flSpeakers.getVisibility() == View.GONE) {
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) dlChat.getLayoutParams();
                lp.addRule(RelativeLayout.BELOW, 0);
                lp.addRule(RelativeLayout.BELOW, R.id.activity_live_room_center_anchor);
                dlChat.setLayoutParams(lp);
            }
        } else {
            if (flQuestionTool.getVisibility() == View.VISIBLE) {
                flQuestionTool.configurationChanged();
                RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
                flQuestionTool.setLayoutParams(layoutParams);
            }
            if (isClearScreen)
                unClearScreen();
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            dlChat.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
            if (flSpeakers.getVisibility() == View.GONE) {
                RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) dlChat.getLayoutParams();
                lp.addRule(RelativeLayout.BELOW, 0);
                lp.addRule(RelativeLayout.BELOW, R.id.activity_live_room_background_container);
                dlChat.setLayoutParams(lp);
            }
        }
        onCloudRecordConfigurationChanged(newConfig);
        onBackgroundContainerConfigurationChanged(newConfig);
        onSpeakersContainerConfigurationChanged(newConfig);
        onPPTLeftMenuConfigurationChanged(newConfig);
    }

    private void onCloudRecordConfigurationChanged(Configuration newConfig) {
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) flCloudRecord.getLayoutParams();
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            lp.addRule(RelativeLayout.ABOVE, 0);
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            lp.addRule(RelativeLayout.ABOVE, R.id.activity_live_room_center_anchor);
            lp.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, 0);
        }
        flCloudRecord.setLayoutParams(lp);
    }

    private void onSpeakersContainerConfigurationChanged(Configuration newConfig) {
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) flSpeakers.getLayoutParams();
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            flCloudRecord.setVisibility(View.GONE);
            lp.addRule(RelativeLayout.BELOW, 0);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (liveRoom.getCloudRecordStatus() && liveRoom.isTeacherOrAssistant())
                flCloudRecord.setVisibility(View.VISIBLE);
            else
                flCloudRecord.setVisibility(View.GONE);
            lp.addRule(RelativeLayout.BELOW, R.id.activity_live_room_background_container);
        }
        flSpeakers.setLayoutParams(lp);
    }

    private void onBackgroundContainerConfigurationChanged(Configuration newConfig) {
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) flBackground.getLayoutParams();
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT);
            if (isBackgroundContainerShrink)
                lp.addRule(RelativeLayout.BELOW, R.id.activity_live_room_speakers_container);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            DisplayMetrics displayMetrics = new DisplayMetrics();// 强制设置白版高度
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
            int displayWidth = Math.min(displayMetrics.widthPixels, displayMetrics.heightPixels);
            lp.height = displayWidth * 3 / 4;
            if (isBackgroundContainerShrink)
                lp.addRule(RelativeLayout.BELOW, 0);
        }
        flBackground.setLayoutParams(lp);
    }

    private void onPPTLeftMenuConfigurationChanged(Configuration newConfig) {
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if (lppptView != null && lppptView.isEditable()) {
                flLeft.setVisibility(View.GONE);
                dlChat.setVisibility(View.GONE);
                flRightBottom.setVisibility(View.INVISIBLE);
            }
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            flLeft.setVisibility(View.VISIBLE);
            dlChat.setVisibility(View.VISIBLE);
            flRightBottom.setVisibility(View.VISIBLE);
        }
    }

    //录课中ui清屏调整
    private void onRecordFullScreenConfigurationChanged(boolean isClear) {
        if (!liveRoom.getCloudRecordStatus())
            return;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            flCloudRecord.setVisibility(View.GONE);
            return;
        }
        if (isClear && liveRoom != null && liveRoom.isTeacherOrAssistant()) {
            flCloudRecord.setVisibility(View.VISIBLE);
        } else {
            flCloudRecord.setVisibility(View.GONE);
        }
    }

    @Override
    public LiveRoom getLiveRoom() {
        checkNotNull(liveRoom);
        return liveRoom;
    }

    @Override
    public void setLiveRoom(final LiveRoom liveRoom) {
        this.liveRoom = liveRoom;
        LPSdkVersionUtils.setSdkVersion(LPSdkVersionUtils.MUTIL_CLASS_UI.concat(BuildConfig.VERSION_NAME));
        liveRoom.setOnLiveRoomListener(error -> {
            switch ((int) error.getCode()) {
                case LPError.CODE_ERROR_ROOMSERVER_LOSE_CONNECTION:
                    doReEnterRoom(LiveSDK.checkTeacherUnique);
                    break;
                case LPError.CODE_ERROR_NETWORK_FAILURE:
                    showMessage(error.getMessage());
                    break;
                case LPError.CODE_ERROR_NETWORK_MOBILE:
                    if (!mobileNetworkDialogShown && isForeground) {
                        mobileNetworkDialogShown = true;
                        try {
                            if (isFinishing()) return;
                            new MaterialDialog.Builder(LiveRoomActivity.this)
                                    .content(getString(R.string.live_mobile_network_hint))
                                    .positiveText(getString(R.string.live_mobile_network_confirm))
                                    .positiveColor(ContextCompat.getColor(LiveRoomActivity.this, R.color.live_blue))
                                    .onPositive((materialDialog, dialogAction) -> materialDialog.dismiss())
                                    .canceledOnTouchOutside(true)
                                    .build()
                                    .show();
                        } catch (WindowManager.BadTokenException e) {
                            e.printStackTrace();
                        }
                    } else {
                        showMessage(getString(R.string.live_mobile_network_hint_less));
                    }
                    break;
                case LPError.CODE_ERROR_LOGIN_CONFLICT:
                    break;
                case LPError.CODE_ERROR_OPEN_AUDIO_RECORD_FAILED:
                    if (!TextUtils.isEmpty(error.getMessage()))
                        showMessage(error.getMessage());
                    break;
                case LPError.CODE_ERROR_OPEN_AUDIO_CAMERA_FAILED:
                    if (!TextUtils.isEmpty(error.getMessage()))
                        showMessage(error.getMessage());
                    detachLocalVideo();
                    break;
                case LPError.CODE_WARNING_PLAYER_LAG:
                case LPError.CODE_WARNING_PLAYER_MEDIA_SUBSCRIBE_TIME_OUT:
                    break;
                default:
                    if (!TextUtils.isEmpty(error.getMessage()))
                        showMessage(error.getMessage());
                    break;
            }
        });
    }

    @Override
    public void showMessage(final String message) {
        if (TextUtils.isEmpty(message)) return;

        this.runOnUiThread(() -> {
            if (this.isFinishing()) return;
            Toast toast = Toast.makeText(LiveRoomActivity.this, message, Toast.LENGTH_SHORT);
            toast.setGravity(Gravity.CENTER, 0, 0);
            toast.show();
        });
    }

    @Override
    public void showMessage(@StringRes int strRes) {
        showMessage(getResources().getString(strRes));
    }

    @Override
    public void saveTeacherMediaStatus(IMediaModel model) {
        globalPresenter.setTeacherMedia(model);
    }

    @Override
    public void showError(LPError error) {
        this.lpError = error;
        if (error.getCode() == LPError.CODE_ERROR_LOGIN_KICK_OUT) {
            //被踢出房间后的登录
            showKickOutDlg(error);
        } else if(error.getCode() == LPError.CODE_ERROR_LOGIN_AUDITION) {
            if (mAuditionEndDialog == null) {
                mAuditionEndDialog = new SimpleTextDialog(this, error);
                mAuditionEndDialog.setCancelable(false);
                mAuditionEndDialog.setOnOkClickListener(new SimpleTextDialog.OnOkClickListener() {
                    @Override
                    public void onJump(String url) {
                        mAuditionEndDialog.dismiss();
                        mAuditionEndDialog = null;

                        //跳转link
                        if (!TextUtils.isEmpty(url)) {
                            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                if (!TextUtils.isEmpty(url))
                                    url = "http://" + url;
                            }

                            Intent intent = new Intent();
                            intent.setAction("android.intent.action.VIEW");
                            Uri uri = Uri.parse(url);
                            intent.setData(uri);
                            LiveRoomActivity.this.startActivity(intent);
                        }
                        LiveRoomActivity.this.finish();
                    }
                });
            }
            if (mAuditionEndDialog.isShowing())
                return;
            mAuditionEndDialog.show();

        } else {
            if (errorFragment != null && errorFragment.isAdded()) return;
            if (flError.getChildCount() >= 2) return;
            if (findFragment(flError.getId()) != null) return;
            if (loadingFragment != null && loadingFragment.isAdded()) {
                removeFragment(loadingFragment);
            }
            int errorCode = (int) error.getCode();
            switch (errorCode) {
                case LPError.CODE_ERROR_LOGIN_UNIQUE_CONFLICT:
                    errorFragment = ErrorFragment.newInstance(false, ErrorFragment.ERROR_HANDLE_CONFILICT, shouldShowTechSupport, false);
                    break;
                case LPError.CODE_ERROR_NEW_SMALL_COURSE:
                case LPError.CODE_ERROR_MAX_STUDENT:
                case LPError.CODE_ERROR_ENTER_ROOM_FORBIDDEN:
                    errorFragment = ErrorFragment.newInstance(getString(R.string.live_override_error), error.getMessage(), ErrorFragment.ERROR_HANDLE_REENTER, shouldShowTechSupport, false);
                    break;
                default:
                    errorFragment = ErrorFragment.newInstance(getString(R.string.live_override_error), error.getMessage(), ErrorFragment.ERROR_HANDLE_REENTER, shouldShowTechSupport);
                    break;
            }
            errorFragment.setRouterListener(this);
            flError.setVisibility(View.VISIBLE);
            addFragment(flError.getId(), errorFragment);
        }
    }

    private void showKickOutDlg(LPError error) {
        liveRoom.quitRoom();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog = builder.setMessage(error.getMessage())
                .setPositiveButton(R.string.live_quiz_dialog_confirm, (dialog1, which) -> {
                    dialog1.dismiss();
                    LiveRoomActivity.this.finish();
                }).create();
        dialog.setCancelable(false);
        if (!isFinishing()) {
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.live_blue));
        }
    }

    @Override
    public boolean canStudentDraw() {
        return isTeacherOrAssistant() || lppptView.isCurrentMaxPage();
    }

    @Override
    public boolean isCurrentUserTeacher() {
        return liveRoom.getCurrentUser().getType() == LPConstants.LPUserType.Teacher;
    }

    @Override
    public boolean isVideoManipulated() {
        return globalPresenter.isVideoManipulated();
    }

    @Override
    public void setVideoManipulated(boolean b) {
        globalPresenter.setVideoManipulated(b);
    }

    @Override
    public int getSpeakApplyStatus() {
        return rightMenuPresenter.getSpeakApplyStatus();
    }

    @Override
    public void showMessageClassEnd() {
        showMessage(getString(R.string.live_message_le, getString(R.string.lp_override_class_end)));
        flPPTLeft.setVisibility(View.GONE);
    }

    @Override
    public void showMessageClassStart() {
        showMessage(getString(R.string.live_message_le, getString(R.string.lp_override_class_start)));
    }

    @Override
    public void showMessageForbidAllChat(LPRoomForbidChatResult result) {

        String message = "";
        if (result.type == LPConstants.LPForbidChatType.TYPE_ALL) {
            if (result.isForbid)
                message = getString(R.string.string_live_uisdk_forbid_all_true);
            else
                message = getString(R.string.string_live_uisdk_forbid_all_false);
        } else {
            if (result.isForbid)
                message = getString(R.string.string_live_uisdk_forbid_group_true);
            else
                message = getString(R.string.string_live_uisdk_forbid_group_false);
        }
        showMessage(message);
    }

    @Override
    public void showMessageTeacherOpenAudio() {
        showMessage(getString(R.string.lp_override_role_teacher) + "打开了麦克风");
    }

    @Override
    public void showMessageTeacherOpenVideo() {
        showMessage(getString(R.string.lp_override_role_teacher) + "打开了摄像头");
    }

    @Override
    public void showMessageTeacherOpenAV() {
        showMessage(getString(R.string.lp_override_role_teacher) + "打开了麦克风和摄像头");
    }

    @Override
    public void showMessageTeacherCloseAV() {
        showMessage(getString(R.string.lp_override_role_teacher) + "关闭了麦克风和摄像头");
    }

    @Override
    public void showMessageTeacherCloseAudio() {
        showMessage(getString(R.string.lp_override_role_teacher) + "关闭了麦克风");
    }

    @Override
    public void showMessageTeacherCloseVideo() {
        showMessage(getString(R.string.lp_override_role_teacher) + "关闭了摄像头");
    }

    @Override
    public void showMessageTeacherEnterRoom() {
        showMessage(getString(R.string.lp_override_role_teacher) + "进入了" + getString(R.string.lp_override_classroom));
    }

    @Override
    public void showMessageTeacherExitRoom() {
        showMessage(getString(R.string.lp_override_role_teacher) + "离开了" + getString(R.string.lp_override_classroom));
    }

    @Override
    public boolean getVisibilityOfShareBtn() {
        if (liveRoom != null)
            return shareListener != null && liveRoom.getFeatureConfig().isShareEnable();
        return shareListener != null;
    }

    @Override
    public void changeBackgroundContainerSize(boolean isShrink) {
        if (isShrink == isBackgroundContainerShrink)
            return;
        isBackgroundContainerShrink = isShrink;

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            speakersFragment.setBackGroundVisible(true);
            return;
        }

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) flBackground.getLayoutParams();
        if (isBackgroundContainerShrink) {
            lp.addRule(RelativeLayout.BELOW, R.id.activity_live_room_speakers_container);
            speakersFragment.setBackGroundVisible(true);
        } else {
            lp.addRule(RelativeLayout.BELOW, 0);
            speakersFragment.setBackGroundVisible(false);
        }
        flBackground.setLayoutParams(lp);
        if (lppptView != null) {
            lppptView.onSizeChange();
        }
    }

    @Override
    public void showForceSpeakDlg(IMediaControlModel iMediaControlModel) {
        LPResRoomMediaControlModel lpResRoomMediaControlModel = (LPResRoomMediaControlModel) iMediaControlModel;
        if (lpResRoomMediaControlModel == null) return;
        if (speakersPresenter != null && getLiveRoom().getAutoOpenCameraStatus()) {
            speakersPresenter.attachVideo();
        }
        if (lpResRoomMediaControlModel.isAudioOn())
            attachLocalAudio();
        int tipRes = R.string.live_force_speak_tip_all;
        if (lpResRoomMediaControlModel.isAudioOn() && getLiveRoom().getAutoOpenCameraStatus()) {
            tipRes = R.string.live_force_speak_tip_all;
        } else if (lpResRoomMediaControlModel.isAudioOn()) {
            tipRes = R.string.live_force_speak_tip_audio;
        } else if (getLiveRoom().getAutoOpenCameraStatus()) {
            tipRes = R.string.live_force_speak_tip_video;
        }
        if (!isFinishing()) {
            new MaterialDialog.Builder(this)
                    .content(tipRes)
                    .positiveText(getString(R.string.live_i_got_it))
                    .positiveColor(ContextCompat.getColor(LiveRoomActivity.this, R.color.live_blue))
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                            materialDialog.dismiss();
                        }
                    })
                    .canceledOnTouchOutside(true)
                    .build()
                    .show();
        }

    }

    @Override
    public void showSpeakInviteDlg(int invite) {
        if (invite == 0) { // 取消邀请;
            if (speakInviteDlg != null && speakInviteDlg.isShowing()) {
                speakInviteDlg.dismiss();
            }
            return;
        }
        speakInviteDlg = new MaterialDialog.Builder(this)
                .content(R.string.live_invite_speak_tip)
                .positiveText(getString(R.string.live_agree))
                .negativeText(getString(R.string.live_disagree))
                .positiveColor(ContextCompat.getColor(LiveRoomActivity.this, R.color.live_blue))
                .negativeColor(ContextCompat.getColor(LiveRoomActivity.this, R.color.live_blue))
                .onPositive((materialDialog, dialogAction) -> {
                    rightMenuPresenter.onSpeakInvite(1);
                    materialDialog.dismiss();
                })
                .onNegative((dialog, which) -> {
                    rightMenuPresenter.onSpeakInvite(0);
                    dialog.dismiss();
                })
                .canceledOnTouchOutside(true)
                .build();

        if (!isFinishing())
            speakInviteDlg.show();
    }

    @Override
    public LPConstants.LPRoomType getRoomType() {
        return liveRoom.getRoomType();
    }


    @Override
    public void showDebugBtn() {
        if (leftMenuFragment != null) {
            if (liveRoom.isUseWebRTC()) {
                leftMenuFragment.showDebugBtn(1);
            } else {
                leftMenuFragment.showDebugBtn(2);
            }
        }
    }

    @Override
    public void showCopyLogDebugPanel() {
        if (checkWriteFilePermission()) {
            Toast.makeText(LiveRoomActivity.this, FileUtil.copyFile(getLiveRoom().getAVLogFilePath(), FileUtil.getSDPath()), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void showStreamDebugPanel() {
        View layout = getLayoutInflater().inflate(R.layout.dlg_lp_debug_stream, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dlg = builder.create();
        dlg.setOnCancelListener(dialog -> {

            if (liveRoom.isUseWebRTC()) {
                liveRoom.setOnWebrtcStreamStats(1500, null);
            } else {
                RxUtils.dispose(subscriptionOfStreamInfo);
            }
        });
        dlg.setView(layout);

        Window window = dlg.getWindow();
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.alpha = 0.2f;
        window.setAttributes(lp);
        tvStreamInfo = layout.findViewById(R.id.tv_dlg_debug_stream);

        if (liveRoom.isUseWebRTC()) {
            liveRoom.setOnWebrtcStreamStats(1500, log -> mHandler.post(() -> {
                if (tvStreamInfo != null)
                    tvStreamInfo.setText(log);
            }));
        } else {
            userMediaModels = getLiveRoom().getSpeakQueueVM().getSpeakQueueList();
            disposableOfMediaPublish = getLiveRoom().getSpeakQueueVM().getObservableOfMediaPublish()
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(mediaModel -> userMediaModels = getLiveRoom().getSpeakQueueVM().getSpeakQueueList());

            subscriptionOfStreamInfo = Observable.interval(1500, TimeUnit.MILLISECONDS)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(aLong -> {
                        if (userMediaModels != null && userMediaModels.size() > 0) {
                            StringBuilder builder1 = new StringBuilder();
                            for (int i = 0; i < userMediaModels.size(); i++) {
                                ConcurrentHashMap<String, LPAVMediaModel> map = getLiveRoom().getPlayer().getChmUserStream();
                                if (map != null && map.size() > 0) {
                                    LPAVMediaModel mediaModel = map.get(userMediaModels.get(i).getUser().getUserId());
                                    if (mediaModel != null) {
                                        Map<Object, Object> info = getLiveRoom().getPlayer().getStreamInfo(mediaModel.streamId);
                                        if (info == null || info.get("stream") == null) {
                                            continue;
                                        }
                                        Map<Object, Object> stream = (Map<Object, Object>) info.get("stream");
                                        int videoBytesPerSecond = (int) (stream.get("videoBytesPerSecond") == null ? 0 : stream.get("videoBytesPerSecond"));
                                        int audioBytesPerSecond = (int) (stream.get("audioBytesPerSecond") == null ? 0 : stream.get("audioBytesPerSecond"));
                                        double videoBufferLength = (double) (stream.get("videoBufferLength") == null ? 0d : stream.get("videoBufferLength"));
                                        double audioBufferLength = (double) (stream.get("audioBufferLength") == null ? 0d : stream.get("audioBufferLength"));
                                        int videoLossRate = (int) (stream.get("videoLossRate") == null ? 0 : stream.get("videoLossRate"));
                                        int audioLossRate = (int) (stream.get("audioLossRate") == null ? 0 : stream.get("audioLossRate"));

                                        String tcpOrUdp = "";
                                        switch (mediaModel.userLinkType) {
                                            case TCP:
                                                tcpOrUdp = "TCP";
                                                break;
                                            case UDP:
                                                tcpOrUdp = "UDP";
                                                break;
                                        }
                                        builder1.append("\n" + userMediaModels.get(i).getUser().getName() + "  ↓ ")
                                                .append("\nvideoBytesPerSecond:" + videoBytesPerSecond * 8 / 1024 + "kb/s  audioBytesPerSecond:" + audioBytesPerSecond * 8 / 1024 + "kb/s"
                                                        + "\nvideoBufferLength:" + videoBufferLength + "  audioBufferLength:" + audioBufferLength
                                                        + "\nvideoLossRate:" + videoLossRate + "  audioLossRate:" + audioLossRate
                                                        + "\nLinkType:" + tcpOrUdp
                                                        + "\n=====================");
                                    }
                                }
                                tvStreamInfo.setText(builder1.toString());
                            }
                        } else {
                            tvStreamInfo.setText("没有发言用户");
                        }
                    });
            dlg.show();
        }
    }

    @Override
    public void showHuiyinDebugPanel() {
        MaterialDialog dlg = new MaterialDialog.Builder(this)
                .title("debug面板")
                .customView(R.layout.dlg_lp_debug_panel, true)
                .positiveText("确认")
                .negativeText("取消")
                .onPositive((dialog, which) -> {
                    String s = etDebugAecDelay.getText().toString().trim();
                    if (TextUtils.isEmpty(s)) {
                        delay = 0;
                    } else {
                        delay = Integer.valueOf(s);
                    }
//                        getLiveRoom().getLivePlayer().setAecParameters(aecMode, aecmMode, delay);
//                        getLiveRoom().getLivePlayer().setAudioSource(audioSource);
//                        getLiveRoom().getLivePlayer().setCommunicationMode(isCommunication);
                    dialog.dismiss();
                    Toast.makeText(LiveRoomActivity.this, "aecMode: " + aecMode + "\naecmMode: " + aecmMode + "\ndelay: " + delay + "\naudio source: " + audioSource + "\n 通话模式：" + isCommunication, Toast.LENGTH_LONG).show();
                })
                .onNegative((dialog, which) -> dialog.dismiss())
                .build();
        //debug
        RadioGroup rgAecMode = (RadioGroup) dlg.getCustomView().findViewById(R.id.rg_lp_debug_mode_aec);
        RadioGroup rgAecmMode = (RadioGroup) dlg.getCustomView().findViewById(R.id.rg_lp_debug_mode_aecm);
        RadioGroup rgAudioSourceMode = (RadioGroup) dlg.getCustomView().findViewById(R.id.rg_lp_debug_mode_audio_source);
        RadioGroup rgCommunication = (RadioGroup) dlg.getCustomView().findViewById(R.id.rg_lp_debug_mode_is_communication);
        etDebugAecDelay = (EditText) dlg.getCustomView().findViewById(R.id.et_debug_aec_delay);
        rgAecMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.aec_unchanged) {
                aecMode = 0;
            } else if (checkedId == R.id.aec_default) {
                aecMode = 1;
            } else if (checkedId == R.id.aec_conference) {
                aecMode = 2;
            } else if (checkedId == R.id.aec_aec) {
                aecMode = 3;
            } else if (checkedId == R.id.aec_aecm) {
                aecMode = 4;
            }
        });
        rgAecmMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.aecm_quiet_or_headset) {
                aecmMode = 0;
            } else if (checkedId == R.id.aecm_ear_piece) {
                aecmMode = 1;
            } else if (checkedId == R.id.aecm_loud_ear_piece) {
                aecmMode = 2;
            } else if (checkedId == R.id.aecm_speaker_phone) {
                aecmMode = 3;
            } else if (checkedId == R.id.aecm_loud_speaker_phone) {
                aecmMode = 4;
            }
        });
        rgAudioSourceMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.audio_source_default) {
                audioSource = 0;
            } else if (checkedId == R.id.audio_source_mic) {
                audioSource = 1;
            } else if (checkedId == R.id.audio_source_uplink) {
                audioSource = 2;
            } else if (checkedId == R.id.audio_source_downlink) {
                audioSource = 3;
            } else if (checkedId == R.id.audio_source_voice_call) {
                audioSource = 4;
            } else if (checkedId == R.id.audio_source_camcorder) {
                audioSource = 5;
            } else if (checkedId == R.id.audio_source_recognition) {
                audioSource = 6;
            } else if (checkedId == R.id.audio_source_communication) {
                audioSource = 7;
            }
        });
        rgCommunication.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.communication_on) {
                isCommunication = true;
            } else if (checkedId == R.id.communication_off) {
                isCommunication = false;
            }
        });
        if (!isFinishing())
            dlg.show();
    }

    @Override
    public void enableStudentSpeakMode() {
        // 一对一、小班课上课学生自动举手上麦
        if (liveRoom.getCurrentUser().getType() == LPConstants.LPUserType.Student && !liveRoom.isAudition()) {
            //试听进入不上台
            if (liveRoom.getRoomType() == LPConstants.LPRoomType.Single ||
                    liveRoom.getRoomType() == LPConstants.LPRoomType.SmallGroup) {
                if (liveRoom.isClassStarted()) {
                    liveRoom.getRecorder().publish();
                    if (!liveRoom.getRecorder().isAudioAttached()) {
                        attachLocalAudio();
                    }
                    if (liveRoom.getAutoOpenCameraStatus()) {
                        Disposable timer = Observable.timer(500, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread())
                                .subscribe(new Consumer<Long>() {
                                    @Override
                                    public void accept(Long aLong) {
                                        attachLocalVideo();
                                    }
                                });
                        timerList.add(timer);
                    }
                    rightMenuFragment.showAutoSpeak(getLiveRoom().getPartnerConfig().liveDisableGrantStudentBrush == 1);
                }
            }
        }
    }

    @Override
    public void showClassSwitch() {
        showMessage(getString(R.string.live_room_switch));
        if (errorFragment != null && errorFragment.isAdded()) {
            removeFragment(errorFragment);
        }

        flBackground.removeAllViews();
        flPPTLeft.setVisibility(View.GONE);
        removeFragment(topBarFragment);
        removeFragment(cloudRecordFragment);
        removeFragment(leftMenuFragment);
        removeFragment(pptLeftFragment);
        removeFragment(rightMenuFragment);
        removeFragment(rightBottomMenuFragment);
        removeFragment(chatFragment);
        removeFragment(speakersFragment);

        if (loadingFragment != null && loadingFragment.isAdded())
            removeFragment(loadingFragment);

        if (announcementFragment != null && announcementFragment.isAdded()) {
            removeFragment(announcementFragment);
        }

        flBackground.removeAllViews();
        removeAllFragment();

        getSupportFragmentManager().executePendingTransactions();

        liveRoom.switchRoom(new LPLaunchListener() {
            @Override
            public void onLaunchSteps(int i, int i1) {
            }

            @Override
            public void onLaunchError(LPError lpError) {
                showError(lpError);
            }

            @Override
            public void onLaunchSuccess(LiveRoom liveRoom) {
                navigateToMain();
            }
        });
    }

    @Override
    public void onPrivateChatUserChange(IUserModel iUserModel) {
        this.privateChatUser = iUserModel;
        if (messageSendPresenter != null)
            messageSendPresenter.onPrivateChatUserChange();
        if (chatPresenter != null)
            chatPresenter.onPrivateChatUserChange();
    }

    @Override
    public IUserModel getPrivateChatUser() {
        return privateChatUser;
    }

    @Override
    public Switchable getFullScreenItem() {
        return fullScreenItem;
    }

    private Switchable fullScreenItem;

    @Override
    public void setFullScreenItem(Switchable screenItem) {
        View view = screenItem.getView();
        if (view == lppptView && lppptView != null && lppptView.isEditable() && rightMenuPresenter != null)
            rightMenuPresenter.changeDrawing();
        fullScreenItem = screenItem;
        flBackground.addView(fullScreenItem.getView(), new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    @Override
    public void switchBackToList(Switchable switchable) {
        speakersFragment.switchBackToList(switchable);
    }

    @Override
    public MyPPTView getPPTView() {
        checkNotNull(lppptView);
        return lppptView;
    }

    private RollCallDialogFragment rollCallDialogFragment;

    public void showRollCallDlg(int time, OnPhoneRollCallListener.RollCall rollCallListener) {
        rollCallDialogFragment = new RollCallDialogFragment();
        rollCallDialogFragment.setCancelable(false);
        rollCallDialogPresenter = new RollCallDialogPresenter(rollCallDialogFragment);
        rollCallDialogPresenter.setRollCallInfo(time, rollCallListener);
        bindVP(rollCallDialogFragment, rollCallDialogPresenter);
        showDialogFragment(rollCallDialogFragment);
    }

    @Override
    public void dismissRollCallDlg() {
        if (rollCallDialogPresenter != null) {
            rollCallDialogPresenter.timeOut();
        }
        if (tempDialogFragment instanceof RollCallDialogFragment)
            tempDialogFragment = null;
        removeFragment(rollCallDialogFragment);
    }

    @Override
    public void onQuizStartArrived(final LPJsonModel jsonModel) {
        dismissQuizDlg();
        quizFragment = new QuizDialogFragment();
        Bundle args = new Bundle();
        int forceJoin = 0;
        if (JsonObjectUtil.isJsonNull(jsonModel.data, "force_join")) {
            forceJoin = 0;
        } else {
            forceJoin = JsonObjectUtil.getAsInt(jsonModel.data, "force_join");
        }
        args.putBoolean(QuizDialogFragment.KEY_FORCE_JOIN, forceJoin == 1);
        quizFragment.setArguments(args);
        quizFragment.setCancelable(false);
        quizPresenter = new QuizDialogPresenter(quizFragment);
        quizFragment.onStartArrived(jsonModel);
        bindVP(quizFragment, quizPresenter);
        showDialogFragment(quizFragment);
    }

    @Override
    public void onQuizEndArrived(LPJsonModel jsonModel) {
        if (quizFragment == null) {
            return;
        }
        quizFragment.onEndArrived(jsonModel);
    }

    @Override
    public void onQuizSolutionArrived(final LPJsonModel jsonModel) {
        dismissQuizDlg();
        quizFragment = new QuizDialogFragment();
        Bundle args = new Bundle();
        args.putBoolean(QuizDialogFragment.KEY_FORCE_JOIN, false);
        quizFragment.setArguments(args);
        quizPresenter = new QuizDialogPresenter(quizFragment);
        quizFragment.onSolutionArrived(jsonModel);
        bindVP(quizFragment, quizPresenter);
        showDialogFragment(quizFragment);
    }

    @Override
    public void onQuizRes(final LPJsonModel jsonModel) {
        dismissQuizDlg();
        quizFragment = new QuizDialogFragment();
        Bundle args = new Bundle();
        int forceJoin = 0;
        if (JsonObjectUtil.isJsonNull(jsonModel.data, "force_join")) {
            forceJoin = 0;
        } else {
            forceJoin = JsonObjectUtil.getAsInt(jsonModel.data, "force_join");
        }
        args.putBoolean(QuizDialogFragment.KEY_FORCE_JOIN, forceJoin == 1);
        quizFragment.setArguments(args);
        quizFragment.setCancelable(false);
        quizPresenter = new QuizDialogPresenter(quizFragment);
        quizFragment.onQuizResArrived(jsonModel);
        bindVP(quizFragment, quizPresenter);
        showDialogFragment(quizFragment);
    }

    @Override
    public void dismissQuizDlg() {
        if (quizFragment != null && quizFragment.isAdded() && quizFragment.isVisible()) {
            quizFragment.dismissAllowingStateLoss();
        }
    }

    private static final int REQUEST_CODE_PERMISSION_CAMERA_TEACHER = 0;
    private static final int REQUEST_CODE_PERMISSION_CAMERA = 1;
    private static final int REQUEST_CODE_PERMISSION_MIC = 2;
    private static final int REQUEST_CODE_PERMISSION_WRITE = 3;
    private static final int REQUEST_CODE_PERMISSION_CAMERA_MIC = 4;

    @Override
    public boolean checkCameraPermission() {
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(LiveRoomActivity.this, Manifest.permission.CAMERA)) {
            return true;
        } else {
            ActivityCompat.requestPermissions(LiveRoomActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_PERMISSION_CAMERA);
        }
        return false;
    }

    private boolean checkCameraAndMicPermission() {
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(LiveRoomActivity.this, Manifest.permission.CAMERA) &&
                PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(LiveRoomActivity.this, Manifest.permission.RECORD_AUDIO)) {
            return true;
        } else {
            ActivityCompat.requestPermissions(LiveRoomActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_PERMISSION_CAMERA_MIC);
        }
        return false;
    }

    @Override
    public boolean checkTeacherCameraPermission(LiveRoom liveRoom) {
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(LiveRoomActivity.this, Manifest.permission.CAMERA)) {
            return true;
        } else {
            this.liveRoom = liveRoom;
            ActivityCompat.requestPermissions(LiveRoomActivity.this, new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_PERMISSION_CAMERA_TEACHER);
        }
        return false;
    }

    private boolean checkMicPermission() {
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(LiveRoomActivity.this, Manifest.permission.RECORD_AUDIO)) {
            return true;
        } else {
            ActivityCompat.requestPermissions(LiveRoomActivity.this, new String[]{Manifest.permission.RECORD_AUDIO}, REQUEST_CODE_PERMISSION_MIC);
        }
        return false;
    }

    private boolean checkWriteFilePermission() {
        if (PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(LiveRoomActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            return true;
        } else {
            ActivityCompat.requestPermissions(LiveRoomActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_CODE_PERMISSION_WRITE);
        }
        return false;
    }

    @Override
    public void attachLocalAudio() {
        if (checkMicPermission()) {
            liveRoom.getRecorder().attachAudio();
        }
    }

    private void showSystemSettingDialog(int type) {
        if (isFinishing()) return;
        new MaterialDialog.Builder(this)
                .title(getString(R.string.live_sweet_hint))
                .content(mapType2String(type))
                .positiveText(getString(R.string.live_quiz_dialog_confirm))
                .positiveColor(ContextCompat.getColor(LiveRoomActivity.this, R.color.live_blue))
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                        materialDialog.dismiss();
                    }
                })
                .canceledOnTouchOutside(true)
                .build()
                .show();
    }

    private String mapType2String(int type) {
        switch (type) {
            case REQUEST_CODE_PERMISSION_CAMERA:
                return getString(R.string.live_no_camera_permission);
            case REQUEST_CODE_PERMISSION_MIC:
                return getString(R.string.live_no_mic_permission);
            case REQUEST_CODE_PERMISSION_WRITE:
                return getString(R.string.live_no_write_permission);
            case REQUEST_CODE_PERMISSION_CAMERA_MIC:
                return getString(R.string.live_no_camera_mic_permission);
            default:
                return "";
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case REQUEST_CODE_PERMISSION_CAMERA_TEACHER:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    setLiveRoom(liveRoom);
                    navigateToMain();
                } else if (grantResults.length > 0) {
                    showMessage("拒绝了相机授权,不能进入房间");
                    finish();
                }
                break;
            case REQUEST_CODE_PERMISSION_CAMERA:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    speakersPresenter.attachVideo();
                } else if (grantResults.length > 0) {
                    showSystemSettingDialog(REQUEST_CODE_PERMISSION_CAMERA);
                }
                break;
            case REQUEST_CODE_PERMISSION_MIC:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    liveRoom.getRecorder().attachAudio();
                } else if (grantResults.length > 0) {
                    showSystemSettingDialog(REQUEST_CODE_PERMISSION_MIC);
                }
                break;
            case REQUEST_CODE_PERMISSION_WRITE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(LiveRoomActivity.this, FileUtil.copyFile(getLiveRoom().getAVLogFilePath(), FileUtil.getSDPath()), Toast.LENGTH_SHORT).show();
                } else if (grantResults.length > 0) {
                    showSystemSettingDialog(REQUEST_CODE_PERMISSION_WRITE);
                }
                break;
            case REQUEST_CODE_PERMISSION_CAMERA_MIC:
                //0 1 2;0是全部授权，1：相机 2：音频
                if (grantResults.length > 0) {
                    int denyPermissions = 0;
                    for (int i = 0; i < grantResults.length; i++) {
                        if (PackageManager.PERMISSION_GRANTED != grantResults[i]) {
                            denyPermissions += i + 1;
                        }
                    }
                    if (denyPermissions != 0) {
                        if (denyPermissions == 1) {
                            showSystemSettingDialog(REQUEST_CODE_PERMISSION_CAMERA);
                            liveRoom.getRecorder().attachAudio();
                        } else if (denyPermissions == 2) {
                            showSystemSettingDialog(REQUEST_CODE_PERMISSION_MIC);
                            speakersPresenter.attachVideoForce();
                        } else {
                            showSystemSettingDialog(REQUEST_CODE_PERMISSION_CAMERA_MIC);
                        }
                    } else {
                        liveRoom.getRecorder().attachAudio();
                        speakersPresenter.attachVideoForce();
                    }
                }
                break;
            default:
                break;
        }
    }

    private void showNetError(LPError error) {
        if (errorFragment != null && errorFragment.isAdded()) return;
        if (flError.getChildCount() >= 2) return;
        if (loadingFragment != null && loadingFragment.isAdded()) {
            removeFragment(loadingFragment);
        }
        errorFragment = ErrorFragment.newInstance("好像断网了", error.getMessage(), ErrorFragment.ERROR_HANDLE_RECONNECT, shouldShowTechSupport);
        errorFragment.setRouterListener(this);
        flError.setVisibility(View.VISIBLE);
        addFragment(R.id.activity_live_room_error, errorFragment);
        if (Build.VERSION.SDK_INT < 24) {
            getSupportFragmentManager().executePendingTransactions();
        }
    }

    @Override
    public void doReEnterRoom(boolean checkUnique) {
        if (errorFragment != null && errorFragment.isAdded()) {
            removeFragment(errorFragment);
        }

        if (lppptView != null) {
            lppptView.onDestroy();
            lppptView.removeAllViews();
        }

        RxUtils.dispose(subscriptionOfMarquee);
        RxUtils.dispose(subscriptionOfLoginConflict);
        RxUtils.dispose(subscriptionOfStreamInfo);
        RxUtils.dispose(subscriptionOfIsCloudRecordAllowed);
        RxUtils.dispose(subscriptionOfTeacherAbsent);
        RxUtils.dispose(disposableOfMediaPublish);

        removeFragment(topBarFragment);
        removeFragment(cloudRecordFragment);
        removeFragment(leftMenuFragment);
        removeFragment(pptLeftFragment);
        removeFragment(rightMenuFragment);
        removeFragment(rightBottomMenuFragment);
        removeFragment(chatFragment);
        removeFragment(speakersFragment);
        if (messageSentFragment != null && messageSentFragment.isAdded()) {
            removeFragment(messageSentFragment);
        }

        if (loadingFragment != null && loadingFragment.isAdded())
            removeFragment(loadingFragment);

        if (announcementFragment != null && announcementFragment.isAdded()) {
            removeFragment(announcementFragment);
            announcementFragment = null;
        }

        if (questionToolFragment != null && questionToolFragment.isAdded()) {
            removeFragment(questionToolFragment);
            questionToolFragment = null;
        }
        if (questionShowFragment != null && questionShowFragment.isAdded()) {
            removeFragment(questionShowFragment);
            questionShowFragment = null;
        }

        removeAllFragment();

        flBackground.removeAllViews();
        getSupportFragmentManager().executePendingTransactions();
        flPPTLeft.setVisibility(View.GONE);
        liveRoom.quitRoom();

        flLoading.setVisibility(View.VISIBLE);
        loadingFragment = LoadingFragment.newInstance(checkUnique, shouldShowTechSupport);
        LoadingPresenter loadingPresenter;
        if (roomId == -1) {
            loadingPresenter = new LoadingPresenter(loadingFragment, code, name, avatar);
        } else {
            loadingPresenter = new LoadingPresenter(loadingFragment, roomId, sign, enterUser);
        }
        bindVP(loadingFragment, loadingPresenter);
        addFragment(R.id.activity_live_room_loading, loadingFragment);
    }

    private Disposable disposableOfMediaPublish;

    @Override
    public void navigateToMain() {
        AudioManager mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        int current;
        if (mAudioManager != null) {
            int currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
            mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, -1, 0);
            minVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
            mAudioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, currentVolume, 0);
            current = mAudioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL);
            if (current <= minVolume) {
                liveRoom.getPlayer().mute();
            }
        }
        subscriptionOfTeacherAbsent = liveRoom.getSpeakQueueVM().getObservableOfActiveUsers().subscribe(new Consumer<List<IMediaModel>>() {
            @Override
            public void accept(List<IMediaModel> iMediaModels) {
                if (!isTeacherOrAssistant() && liveRoom.getTeacherUser() == null) {
                    showMessage(getString(R.string.live_room_teacher_absent));
                }
                RxUtils.dispose(subscriptionOfTeacherAbsent);
            }
        });

        globalPresenter = new GlobalPresenter();
        globalPresenter.setRouter(this);
        globalPresenter.subscribe();

        //PPT操作监听
        mOnPPTStateListener = new OnPPTStateListener() {
            @Override
            public void onSuccess(int code, String successMessage) {
                if (code == OnPPTStateListener.CODE_PPT_WHITEBOARD_ADD) {
                    if (lppptView != null)
                        lppptView.switchPPTPage("0", Integer.valueOf(successMessage));
                }
            }

            @Override
            public void onError(int code, String errorMessage) {
                showMessage(errorMessage);
            }
        };

        lppptView = new MyPPTView(this);

        boolean sdkValid = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
        lppptView.attachLiveRoom(liveRoom);
//        lppptView.setAnimPPTEnable(false);
//        lppptView.setFlingEnable(false);
        lppptView.setOnPPTStateListener(mOnPPTStateListener);

        bindVP(lppptView, new PPTPresenter(lppptView));
        lppptView.switchToFullScreen();
        lppptView.onStart();

        topBarFragment = new TopBarFragment();
        bindVP(topBarFragment, new TopBarPresenter(topBarFragment));
        addFragment(R.id.activity_live_room_top, topBarFragment);

        cloudRecordFragment = new CloudRecordFragment();
        bindVP(cloudRecordFragment, new CloudRecordPresenter());
        addFragment(R.id.activity_live_room_cloud_record, cloudRecordFragment);

        speakersFragment = new SpeakersFragment();
        speakersFragment.setDisableSpeakQueuePlaceholder(disableSpeakQueuePlaceholder);
        speakersPresenter = new SpeakersPresenter(speakersFragment);
        bindVP(speakersFragment, speakersPresenter);
        addFragment(R.id.activity_live_room_speakers_container, speakersFragment);

        leftMenuFragment = new LeftMenuFragment();
        bindVP(leftMenuFragment, new LeftMenuPresenter(leftMenuFragment));
        addFragment(R.id.activity_live_room_bottom_left, leftMenuFragment);

        pptLeftFragment = new PPTLeftFragment();
        bindVP(pptLeftFragment, new PPTLeftPresenter(pptLeftFragment));
        addFragment(R.id.activity_live_room_ppt_left, pptLeftFragment);

        rightMenuFragment = new RightMenuFragment();
        rightMenuPresenter = new RightMenuPresenter(rightMenuFragment);
        bindVP(rightMenuFragment, rightMenuPresenter);
        addFragment(R.id.activity_live_room_right, rightMenuFragment);

        rightBottomMenuFragment = new RightBottomMenuFragment();
        bindVP(rightBottomMenuFragment, new RightBottomMenuPresenter(rightBottomMenuFragment));
        addFragment(R.id.activity_live_room_bottom_right, rightBottomMenuFragment);

        chatFragment = new ChatFragment();
        chatPresenter = new ChatPresenter(chatFragment);
        bindVP(chatFragment, chatPresenter);
        addFragment(R.id.activity_live_room_chat, chatFragment);

        RxUtils.dispose(subscriptionOfLoginConflict);
        subscriptionOfLoginConflict = getLiveRoom().getObservableOfLoginConflict().observeOn(AndroidSchedulers.mainThread())
                .subscribe(iLoginConflictModel -> {
                    if (enterRoomConflictListener != null) {
                        enterRoomConflictListener.onConflict(LiveRoomActivity.this, iLoginConflictModel.getConflictEndType()
                                , new LiveSDKWithUI.LPRoomExitCallback() {
                                    @Override
                                    public void exit() {
                                        LiveRoomActivity.super.finish();
                                    }

                                    @Override
                                    public void cancel() {
                                        LiveRoomActivity.super.finish();
                                    }
                                });
                    } else {
                        LiveRoomActivity.super.finish();
                    }
                });
        if (getLiveRoom().getCurrentUser().getType() == LPConstants.LPUserType.Teacher) {
            liveRoom.requestClassStart();
        }

        if (shareListener != null) {
            shareListener.getShareData(this, liveRoom.getRoomId());
        }

        // might delay 500ms to process
        Disposable timer = Observable.timer(500, TimeUnit.MILLISECONDS).observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                    removeFragment(loadingFragment);
                    flLoading.setVisibility(View.GONE);
                    flError.setVisibility(View.GONE);

                    if (liveRoom.getCurrentUser().getType() == LPConstants.LPUserType.Teacher) {
                        liveRoom.getRecorder().publish();
                        if (checkCameraAndMicPermission()) {
                            liveRoom.getRecorder().attachAudio();
                            speakersPresenter.attachVideoForce();
                        }
                        if (liveRoom.getAutoStartCloudRecordStatus() == 1) {
                            subscriptionOfIsCloudRecordAllowed = liveRoom.requestIsCloudRecordAllowed()
                                    .subscribe(lpCheckRecordStatusModel -> {
                                        if (lpCheckRecordStatusModel.recordStatus == 1) {
                                            liveRoom.requestCloudRecord(true);
                                        } else {
                                            showMessage(lpCheckRecordStatusModel.reason);
                                        }
                                    });
                        }
                    } else if (liveRoom.getCurrentUser().getType() == LPConstants.LPUserType.Student) {
                        enableStudentSpeakMode();
                    }
                });
        timerList.add(timer);

//        liveRoom.getObservableOfBroadcast().observeOn(AndroidSchedulers.mainThread())
//                .subscribe(new Consumer<LPKVModel>() {
//                    @Override
//                    public void accept(LPKVModel lpkvModel) {
//                        LPLogger.e(lpkvModel.key + " " + lpkvModel.value);
//                    }
//                });


        //成功进入房间后统一不再显示
        shouldShowTechSupport = false;
        if (!isTeacherOrAssistant())
            startMarqueeTape();
        LiveSDK.checkTeacherUnique = false;
    }

    /**
     * 跑马灯
     */
    private void startMarqueeTape() {
        String lampFormServer = liveRoom.getPartnerConfig().liveHorseLamp == null ? null : liveRoom.getPartnerConfig().liveHorseLamp.value;
        final String lamp = TextUtils.isEmpty(liveHorseLamp) ? lampFormServer : liveHorseLamp;
        if (TextUtils.isEmpty(lamp)) {
            return;
        }
        if (subscriptionOfMarquee != null && !subscriptionOfMarquee.isDisposed()) {
            return;
        }
        subscriptionOfMarquee = Observable.interval(0, liveHorseLampInterval, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Long>() {
                    @Override
                    public void accept(Long aLong) {
                        showMarqueeTape(lamp);
                    }
                });
    }

    private void showMarqueeTape(String lamp) {
        final TextView textView = new TextView(this);
        textView.setText(lamp);
        textView.setTextColor(ContextCompat.getColor(this, R.color.live_half_transparent_white));
        textView.setTextSize(10);
        textView.setLines(1);
        textView.setPadding(20, 10, 20, 10);
        textView.setGravity(Gravity.CENTER);
        textView.setBackgroundColor(ContextCompat.getColor(this, R.color.live_half_transparent_mask));
        RelativeLayout.LayoutParams lp = new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = new Random().nextInt(DisplayUtils.getScreenHeightPixels(this) - 120);
        lp.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
        rlContainer.addView(textView, lp);
        int width = DisplayUtils.getScreenWidthPixels(this);
        ObjectAnimator objectAnimator = ObjectAnimator.ofFloat(textView, "translationX", -width);
        objectAnimator.setDuration(width * 20);
        objectAnimator.setInterpolator(new LinearInterpolator());
        objectAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                rlContainer.removeView(textView);
            }
        });
        objectAnimator.start();
    }

    @Override
    public void clearScreen() {
        dlChat.setVisibility(View.INVISIBLE);
        isClearScreen = true;
        rightBottomMenuFragment.clearScreen();
        hideFragment(topBarFragment);
        hideFragment(rightMenuFragment);
        flLeft.setVisibility(View.INVISIBLE);
        flRight.setVisibility(View.INVISIBLE);
        flRightBottom.setVisibility(View.INVISIBLE);
        onRecordFullScreenConfigurationChanged(true);
    }

    @Override
    public void switchClearScreen() {
        if (isClearScreen) unClearScreen();
        else clearScreen();
    }

    @Override
    public void unClearScreen() {
        dlChat.setVisibility(View.VISIBLE);
        isClearScreen = false;
        rightBottomMenuFragment.unClearScreen();
        showFragment(topBarFragment);
        showFragment(rightMenuFragment);
        flLeft.setVisibility(View.VISIBLE);
        flRight.setVisibility(View.VISIBLE);
        flRightBottom.setVisibility(View.VISIBLE);
        onRecordFullScreenConfigurationChanged(false);
        dlChat.openDrawer(GravityCompat.START);
    }

    private MessageSentFragment messageSentFragment;

    @Override
    public void navigateToMessageInput() {
        if (messageSentFragment == null)
            messageSentFragment = MessageSentFragment.newInstance();
        if (messageSendPresenter == null)
            messageSendPresenter = new MessageSendPresenter(messageSentFragment);
        if (messageSentFragment.isAdded()) return;
        messageSendPresenter.setView(messageSentFragment);
        bindVP(messageSentFragment, messageSendPresenter);
        showDialogFragment(messageSentFragment);
    }

    @Override
    public void navigateToQuickSwitchPPT(int currentIndex, int maxIndex) {
        if (lppptView != null && lppptView.isEditable() && rightMenuPresenter != null)
            rightMenuPresenter.changeDrawing();
        QuickSwitchPPTFragment quickSwitchPPTFragment = QuickSwitchPPTFragment.newInstance();
        Bundle args = new Bundle();
        args.putInt("currentIndex", currentIndex);
        args.putInt("maxIndex", maxIndex);
        quickSwitchPPTFragment.setArguments(args);
        switchPPTFragmentPresenter = new SwitchPPTFragmentPresenter(quickSwitchPPTFragment, lppptView.isMultiWhiteboardEnable());
        bindVP(quickSwitchPPTFragment, switchPPTFragmentPresenter);
        showDialogFragment(quickSwitchPPTFragment);
    }

    @Override
    public void updateQuickSwitchPPTMaxIndex(int index) {
        if (switchPPTFragmentPresenter != null) {
            switchPPTFragmentPresenter.notifyMaxIndexChange(index);
        }
    }

    @Override
    public void notifyPageCurrent(int position) {
        lppptView.updatePage(position, true, false);
    }

    @Override
    public void navigateToPPTDrawing(boolean isAllowDrawing) {
        checkNotNull(lppptView);
        lppptView.setPPTCanvasMode(isAllowDrawing);

        int currentOrientation = getResources().getConfiguration().orientation;
        if (lppptView.isEditable()) {
//            flTop.setVisibility(View.GONE);
            if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
                checkNotNull(leftMenuFragment);
                flLeft.setVisibility(View.GONE);
                dlChat.setVisibility(View.GONE);
                flRightBottom.setVisibility(View.INVISIBLE);
            }
            if (!lppptView.isInFullScreen()) {
                getFullScreenItem().switchBackToList();
                lppptView.switchToFullScreen();
            }
            flPPTLeft.setVisibility(View.VISIBLE);
        } else {
//            flTop.setVisibility(View.VISIBLE);
            flLeft.setVisibility(View.VISIBLE);
            dlChat.setVisibility(View.VISIBLE);
            flRightBottom.setVisibility(View.VISIBLE);
            dlChat.openDrawer(GravityCompat.START);
            flPPTLeft.setVisibility(View.GONE);
        }
    }

    @Override
    public LPConstants.LPPPTShowWay getPPTShowType() {
        return lppptView.getPPTShowWay();
    }

    @Override
    public void setPPTShowType(LPConstants.LPPPTShowWay type) {
        lppptView.setPPTShowWay(type);
    }

    @Override
    public void navigateToUserList() {
        OnlineUserDialogFragment userListFragment = OnlineUserDialogFragment.newInstance();
        OnlineUserPresenter userPresenter = new OnlineUserPresenter(userListFragment);
        bindVP(userListFragment, userPresenter);
        showDialogFragment(userListFragment);
    }


    @Override
    public void navigateToPPTWareHouse() {
        PPTManageFragment pptManageFragment = PPTManageFragment.newInstance();
        if (pptManagePresenter == null) {
            pptManagePresenter = new PPTManagePresenter();
            pptManagePresenter.setRouter(this);
            pptManagePresenter.subscribe();
        }
        pptManageFragment.setPresenter(pptManagePresenter);
        showDialogFragment(pptManageFragment);
    }

    @Override
    public void addPPTWhiteboardPage() {
        if (lppptView == null)
            return;
        lppptView.addPPTWhiteboardPage();
    }

    @Override
    public void deletePPTWhiteboardPage(int pageId) {
        if (lppptView == null)
            return;
        lppptView.deletePPTWhiteboardPage(pageId);
    }

    @Override
    public void changePage(String docId, int pageNum) {
        if (lppptView == null)
            return;
        lppptView.switchPPTPage(docId, pageNum);
    }

    @Override
    public void disableSpeakerMode() {
        if (!isTeacherOrAssistant())
            rightBottomMenuFragment.disableSpeakerMode();
        if (getLiveRoom().getRecorder().isPublishing())
            getLiveRoom().getRecorder().stopPublishing();
        detachLocalVideo();
    }

    @Override
    public void enableSpeakerMode() {
        rightBottomMenuFragment.enableSpeakerMode();
    }

    @Override
    public void showMorePanel(int anchorX, int anchorY) {
        MoreMenuDialogFragment fragment = MoreMenuDialogFragment.newInstance(anchorX, anchorY);
        MoreMenuPresenter presenter = new MoreMenuPresenter(fragment);
        bindVP(fragment, presenter);
        showDialogFragment(fragment);
    }

    @Override
    public void navigateToShare() {
        if (shareListener == null || shareListener.setShareList() == null) {
            return;
        }
        LPShareDialog shareDialog = LPShareDialog.newInstance(shareListener.setShareList());
        shareDialog.setListener(new LPShareDialog.LPShareClickListener() {
            @Override
            public void onShareClick(int type) {
                shareListener.onShareClicked(LiveRoomActivity.this, type);
            }
        });
        showDialogFragment(shareDialog);
    }

    @Override
    public void navigateToAnnouncement() {
        globalPresenter.unObserveAnnouncementChange();
        if (announcementFragment != null && announcementFragment.isAdded()) return;
        announcementFragment = AnnouncementFragment.newInstance();
        AnnouncementPresenter presenter = new AnnouncementPresenter(announcementFragment, globalPresenter);
        bindVP(announcementFragment, presenter);
        showDialogFragment(announcementFragment);
    }

    /**
     * @param recordStatus 当前的状态 1:正在录制 0:未录制
     */
    @Override
    public void navigateToCloudRecord(boolean recordStatus) {
        if (recordStatus) {
            flCloudRecord.setVisibility(View.VISIBLE);
            showFragment(cloudRecordFragment);
        } else {
            flCloudRecord.setVisibility(View.GONE);
            hideFragment(cloudRecordFragment);
        }
    }

    @Override
    public void navigateToHelp() {

    }

    @Override
    public void navigateToSetting() {
        SettingDialogFragment settingFragment = SettingDialogFragment.newInstance();
        SettingPresenter settingPresenter = new SettingPresenter(settingFragment);
        bindVP(settingFragment, settingPresenter);
        showDialogFragment(settingFragment);
    }

    @Override
    public boolean isTeacherOrAssistant() {
        return liveRoom.isTeacherOrAssistant();
    }

    @Override
    public boolean isGroupTeacherOrAssistant() {
        return liveRoom.isGroupTeacherOrAssistant();
    }

    @Override
    public void attachLocalVideo() {
        speakersPresenter.attachVideo();
    }

    @Override
    public void detachLocalVideo() {
        speakersPresenter.detachVideo();
    }

    public boolean isPPTMax() {
        return flBackground.getChildAt(0) == lppptView;
    }

    @Override
    public void clearPPTAllShapes() {
        checkNotNull(lppptView);
        lppptView.eraseAllShapes();
    }

    @Override
    public void changeScreenOrientation() {
        int currentOrientation = getResources().getConfiguration().orientation;
        if (currentOrientation == Configuration.ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    @Override
    public int getCurrentScreenOrientation() {
        return getResources().getConfiguration().orientation;
    }

    @Override
    public int getSysRotationSetting() {
        int status = 0;
        try {
            status = Settings.System.getInt(getContentResolver(),
                    Settings.System.ACCELEROMETER_ROTATION);
        } catch (Settings.SettingNotFoundException e) {
            e.printStackTrace();
        }
        return status;
    }

    @Override
    public void letScreenRotateItself() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR);
    }

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public void forbidScreenRotateItself() {
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
    }

    @Override
    public void showBigChatPic(String url) {
        ChatPictureViewFragment fragment = ChatPictureViewFragment.newInstance(url);
        ChatPictureViewPresenter presenter = new ChatPictureViewPresenter();
        bindVP(fragment, presenter);
        showDialogFragment(fragment);
    }

    @Override
    public void sendImageMessage(String path) {
        chatPresenter.sendImageMessage(path);
    }

    public void showSavePicDialog(byte[] bmpArray) {
        ChatSavePicDialogFragment fragment = new ChatSavePicDialogFragment();
        ChatSavePicDialogPresenter presenter = new ChatSavePicDialogPresenter(bmpArray);
        bindVP(fragment, presenter);
        showDialogFragment(fragment);
    }

    @Override
    public void realSaveBmpToFile(byte[] bmpArray) {
        saveImageToGallery(bmpArray);
    }

    @Override
    public void doHandleErrorNothing() {
        removeFragment(errorFragment);
    }

//    private void setZOrderMediaOverlayTrue(View view) {
//        if (view instanceof SurfaceView) {
//            ((SurfaceView) view).setZOrderMediaOverlay(true);
//        } else if (view instanceof ViewGroup) {
//            for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++) {
//                setZOrderMediaOverlayTrue(((ViewGroup) view).getChildAt(i));
//            }
//        }
//    }

    public View getBackGroundView() {
        return flBackground.getChildAt(0);
    }


    private <V extends BaseView, P extends BasePresenter> void bindVP(V view, P presenter) {
        presenter.setRouter(this);
        view.setPresenter(presenter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (lppptView != null)
            lppptView.onDestroy();
        lppptView = null;

        timerList.clear();

        Log.i("tag", "ondestroy");

        shouldShowTechSupport = true;
        if (pptManagePresenter != null) {
            pptManagePresenter.destroy();
            pptManagePresenter = null;
        }
        if (globalPresenter != null) {
            globalPresenter.destroy();
            globalPresenter = null;
        }
        RxUtils.dispose(subscriptionOfMarquee);
        RxUtils.dispose(subscriptionOfLoginConflict);
        RxUtils.dispose(subscriptionOfStreamInfo);
        RxUtils.dispose(subscriptionOfIsCloudRecordAllowed);
        RxUtils.dispose(subscriptionOfTeacherAbsent);
        RxUtils.dispose(disposableOfMediaPublish);

        orientationEventListener = null;

        liveHorseLamp = null;

        if (liveRoom != null) liveRoom.quitRoom();
    }

    @Override
    public void onBackPressed() {
        if (exitListener != null) {
            super.onBackPressed();
        } else {
            if (isFinishing()) return;
            new MaterialDialog.Builder(this)
                    .title(getString(R.string.live_exit_hint_title))
                    .content(getString(R.string.live_exit_hint_content))
                    .contentColor(ContextCompat.getColor(LiveRoomActivity.this, R.color.live_text_color_light))
                    .positiveColor(ContextCompat.getColor(LiveRoomActivity.this, R.color.live_blue))
                    .positiveText(getString(R.string.live_exit_hint_confirm))
                    .negativeColor(ContextCompat.getColor(LiveRoomActivity.this, R.color.live_text_color))
                    .negativeText(getString(R.string.live_cancel))
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                            if (lpError != null && lpError.getCode() == LPError.CODE_ERROR_LOGIN_UNIQUE_CONFLICT) {
                                getLiveRoom().quitRoom();
                            }
                            finish();
                        }
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                            materialDialog.dismiss();
                        }
                    })
                    .build()
                    .show();
        }
    }

    //初始化时检测屏幕方向，以此设置聊天页面是否可隐藏
    private void checkScreenOrientationInit() {
        Configuration configuration = this.getResources().getConfiguration();
        int orientation = configuration.orientation;
        if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            dlChat.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        } else {
            dlChat.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_OPEN);
        }
    }

    /**
     * 保存图片
     */
    private void saveImageToGallery(final byte[] bmpArray) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 首先保存图片
                File appDir = new File(Environment.getExternalStorageDirectory(), "bjhl_lp_image");
                if (!appDir.exists()) {
                    appDir.mkdir();
                }
                String fileName = System.currentTimeMillis() + ".jpg";
                File file = new File(appDir, fileName);
                final String picPath = file.getAbsolutePath();
                try {
                    Bitmap bmp = BitmapFactory.decodeByteArray(bmpArray, 0, bmpArray.length);
                    FileOutputStream fos = new FileOutputStream(file);
                    bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos);
                    fos.flush();
                    fos.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // 其次把文件插入到系统图库
                try {
                    MediaStore.Images.Media.insertImage(getContentResolver(),
                            file.getAbsolutePath(), fileName, null);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
                // 最后通知图库更新
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.parse("file://" + path)));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(LiveRoomActivity.this, "图片保存在" + picPath, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }

    @Override
    public void finish() {
        if (exitListener != null) {
            exitListener.onRoomExit(this, new LiveSDKWithUI.LPRoomExitCallback() {
                @Override
                public void exit() {
                    tryToCloseCloudRecord();
                    clearStaticCallback();
                    LiveRoomActivity.super.finish();
                }

                @Override
                public void cancel() {
                }
            });
        } else {
            try {
                tryToCloseCloudRecord();
                clearStaticCallback();
                super.finish();
            } catch (Exception e) {
                super.finish();
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean isPrivateChat() {
        return privateChatUser != null;
    }

    @Override
    public void changeNewChatMessageReminder(boolean isNeedShow, int newMessageNumber) {
        if (messageReminderContainer == null)
            messageReminderContainer = (LinearLayout) findViewById(R.id.activity_live_room_new_message_reminder_container);
        if (!isNeedShow || newMessageNumber == 0) {
            messageReminderContainer.setVisibility(View.GONE);
            return;
        }
        messageReminderContainer.setGravity(Gravity.BOTTOM);
        messageReminderContainer.bringToFront();
        ((TextView) findViewById(R.id.activity_live_room_new_message_reminder)).setText(getString(R.string.live_room_new_chat_message, newMessageNumber));
        messageReminderContainer.setVisibility(View.VISIBLE);
        messageReminderContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (chatFragment != null && chatFragment.isAdded())
                    chatFragment.scrollToBottom();
            }
        });
    }

    @Override
    public void showNoSpeakers() {
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) dlChat.getLayoutParams();
        if (lp.getRules()[RelativeLayout.BELOW] == R.id.activity_live_room_background_container) {
            return;
        }
        flSpeakers.setVisibility(View.GONE);
        lp.addRule(RelativeLayout.BELOW, 0);
        lp.addRule(RelativeLayout.BELOW, R.id.activity_live_room_background_container);
        dlChat.setLayoutParams(lp);
    }

    @Override
    public void showHavingSpeakers() {
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) dlChat.getLayoutParams();
        if (lp.getRules()[RelativeLayout.BELOW] == R.id.activity_live_room_speakers_container) {
            return;
        }
        flSpeakers.setVisibility(View.VISIBLE);
        lp.addRule(RelativeLayout.BELOW, 0);
        lp.addRule(RelativeLayout.BELOW, R.id.activity_live_room_speakers_container);
        dlChat.setLayoutParams(lp);
    }

    @Override
    public void showPPTLoadErrorDialog(final int errorCode, String description) {
        try {
            if (isFinishing()) return;
            new MaterialDialog.Builder(this)
                    .title(getString(R.string.live_room_ppt_load_error, errorCode))
                    .content(getString(R.string.live_room_ppt_switch))
                    .contentColor(ContextCompat.getColor(LiveRoomActivity.this, R.color.live_text_color))
                    .positiveColor(ContextCompat.getColor(LiveRoomActivity.this, R.color.live_blue))
                    .positiveText(getString(R.string.live_room_ppt_switch_confirm))
                    .negativeColor(ContextCompat.getColor(LiveRoomActivity.this, R.color.live_text_color))
                    .negativeText(getString(R.string.live_cancel))
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                            lppptView.setAnimPPTEnable(false);
                        }
                    })
                    .onNegative(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                            materialDialog.dismiss();
                        }
                    })
                    .build()
                    .show();
        } catch (Exception ignore) {
        }
    }

    @Override
    public boolean enableAnimPPTView(boolean b) {
        if (lppptView != null)
            return lppptView.setAnimPPTEnable(b);
        return false;
    }

    @Override
    public void answerStart(LPAnswerModel model) {
        this.lpAnswerModel = model;
        removeAnswer();
        QuestionToolPresenter questionToolPresenter = new QuestionToolPresenter();
        questionToolPresenter.setRouter(this);
        questionToolPresenter.setLpQuestionToolModel(model);
        questionToolFragment = new QuestionToolFragment();
        questionToolPresenter.setView(questionToolFragment);
        bindVP(questionToolFragment, questionToolPresenter);
        flQuestionTool.setVisibility(View.VISIBLE);
//        flQuestionTool.setVisibility(View.GONE);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        flQuestionTool.setLayoutParams(layoutParams);
        addFragment(R.id.activity_dialog_question_tool, questionToolFragment);

        showFragment(questionToolFragment);
    }

    /**
     * 显示答题器Answer
     */
    private void showAnswer() {
        QuestionShowPresenter questionShowPresenter = new QuestionShowPresenter();
        questionShowPresenter.setRouter(this);
        questionShowPresenter.setLpQuestionToolModel(lpAnswerModel);
        questionShowFragment = new QuestionShowFragment();
        questionShowPresenter.setView(questionShowFragment);
        bindVP(questionShowFragment, questionShowPresenter);
        flQuestionTool.setVisibility(View.VISIBLE);
//        flQuestionTool.setVisibility(View.GONE);
        RelativeLayout.LayoutParams layoutParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT);
        flQuestionTool.setLayoutParams(layoutParams);
        addFragment(R.id.activity_dialog_question_tool, questionShowFragment);

        showFragment(questionShowFragment);
    }

    @Override
    public void answerEnd(boolean ended) {
        if (questionToolFragment != null && questionToolFragment.isAdded()) {
            removeFragment(questionToolFragment);
            if (ended)
                Toast.makeText(this, "答题时间已到", Toast.LENGTH_SHORT).show();
            flQuestionTool.setVisibility(View.GONE);
            questionToolFragment = null;
        }
        if (lpAnswerModel != null && lpAnswerModel.isShowAnswer && ended) {
            showAnswer();
        }
    }

    @Override
    public void removeAnswer() {
        if (questionShowFragment != null && questionShowFragment.isAdded()) {
            removeFragment(questionShowFragment);
            flQuestionTool.setVisibility(View.GONE);
            questionShowFragment = null;
        }
    }

    @Override
    public void showAwardAnimation(String userName) {
        awardView.startAnim();
        awardView.setVisibility(View.VISIBLE);
        awardView.setUserName(userName);
    }

    private boolean isQuestionAnswerShowing = false;

    @Override
    public void showQuestionAnswer(boolean showFragment) {
        isQuestionAnswerShowing = showFragment;
        if (showFragment) {
            if (questionAnswerFragment == null) {
                questionAnswerFragment = new QuestionAnswerFragment();
            }
            if (questionAnswerFragment.isAdded())
                return;
            QuestionAnswerPresenter questionAnswerPresenter = new QuestionAnswerPresenter(questionAnswerFragment);
            bindVP(questionAnswerFragment, questionAnswerPresenter);
            findViewById(R.id.activity_live_room_question_answer).setVisibility(View.VISIBLE);
            addFragment(R.id.activity_live_room_question_answer, questionAnswerFragment);
            showFragment(questionAnswerFragment);
        } else {
            removeFragment(questionAnswerFragment);
            findViewById(R.id.activity_live_room_question_answer).setVisibility(View.GONE);
        }
    }

    @Override
    public void setQuestionAnswerCahce(LPAnswerModel lpAnswerModel) {
        this.lpAnswerModel = lpAnswerModel;
    }

    @Override
    public boolean isQuestionAnswerShow() {
        return isQuestionAnswerShowing;
    }

    private int getStreamType() {
        // TODO: 2019-08-14 mix stream goes out in MUSIC_STREAM
        return LiveSDK.getAudioOutput() == LPConstants.VoiceType.VOICE_CALL ?
                AudioManager.STREAM_VOICE_CALL : AudioManager.STREAM_MUSIC;
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            AudioManager mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (mAudioManager == null) return true;
            mAudioManager.adjustStreamVolume(getStreamType(), AudioManager.ADJUST_RAISE,
                    AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
            int current = mAudioManager.getStreamVolume(getStreamType());
            if (current > minVolume) {
                try {
                    liveRoom.getPlayer().unMute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            AudioManager mAudioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
            if (mAudioManager == null) return true;
            mAudioManager.adjustStreamVolume(getStreamType(), AudioManager.ADJUST_LOWER,
                    AudioManager.FLAG_PLAY_SOUND | AudioManager.FLAG_SHOW_UI);
            int current = mAudioManager.getStreamVolume(getStreamType());
            if (current <= minVolume) {
                try {
                    liveRoom.getPlayer().mute();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            return true;
        } else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private void tryToCloseCloudRecord() {
        // 如果是被踢下线 core里面会调quitRoom
        if (getLiveRoom().isQuit()) return;
        if (getLiveRoom() == null || getLiveRoom().getCurrentUser() == null) return;
        if (getLiveRoom().getCurrentUser().getType() == LPConstants.LPUserType.Teacher) {
            if (getLiveRoom().getCloudRecordStatus()) {
                liveRoom.requestCloudRecord(false);
            }
            liveRoom.requestClassEnd();
        }
    }

    /* 房间外部回调 */
    private static LiveSDKWithUI.LPShareListener shareListener;
    private static LiveSDKWithUI.LPRoomExitListener exitListener;
    private static LiveSDKWithUI.RoomEnterConflictListener enterRoomConflictListener;
    private static LiveSDKWithUI.LPRoomResumeListener roomLifeCycleListener;

    private static boolean shouldShowTechSupport = true;

    private static boolean disableSpeakQueuePlaceholder = false;

    private void clearStaticCallback() {
        shareListener = null;
        exitListener = null;
        enterRoomConflictListener = null;
        roomLifeCycleListener = null;
    }

    public static LiveSDKWithUI.LPRoomExitListener getExitListener() {
        return exitListener;
    }

    public static void setRoomLifeCycleListener(LiveSDKWithUI.LPRoomResumeListener roomLifeCycleListener) {
        LiveRoomActivity.roomLifeCycleListener = roomLifeCycleListener;
    }

    public static void setShareListener(LiveSDKWithUI.LPShareListener listener) {
        LiveRoomActivity.shareListener = listener;
    }

    public static void setRoomExitListener(LiveSDKWithUI.LPRoomExitListener roomExitListener) {
        LiveRoomActivity.exitListener = roomExitListener;
    }

    public static void setEnterRoomConflictListener(LiveSDKWithUI.RoomEnterConflictListener enterRoomConflictListener) {
        LiveRoomActivity.enterRoomConflictListener = enterRoomConflictListener;
    }

    public static void setShouldShowTechSupport(boolean shouldShow) {
        shouldShowTechSupport = shouldShow;
    }

    public static void setLiveRoomMarqueeTape(String liveRoomMarqueeTape) {
        LiveRoomActivity.liveHorseLamp = liveRoomMarqueeTape;
    }

    public static void setLiveRoomMarqueeTape(String liveRoomMarqueeTape, int liveRoomMarqueeTapeInterval) {
        LiveRoomActivity.liveHorseLamp = liveRoomMarqueeTape;
        LiveRoomActivity.liveHorseLampInterval = liveRoomMarqueeTapeInterval;
    }

    public static void disableSpeakQueuePlaceholder() {
        disableSpeakQueuePlaceholder = true;
    }

    @Override
    public void setRemarksEnable(boolean isEnable) {

        if (lppptView == null) {
            return;
        }

        lppptView.setRemarksEnable(isEnable);
    }

    @Override
    public void switchRedPacketUI(boolean isShow, LPRedPacketModel lpRedPacketModel) {

        if (isShow) {
            //显示
            if (mRedPacketFragment != null) {
                if (mRedPacketPresenter.getRedPacketing()) {
                    return;
                }
                removeFragment(mRedPacketFragment);
                mRedPacketPresenter.unSubscribe();
                mRedPacketPresenter = null;
            }

            mRedPacketFragment = new RedPacketFragment();
            mRedPacketPresenter = new RedPacketPresenter(mRedPacketFragment, lpRedPacketModel);
            bindVP(mRedPacketFragment, mRedPacketPresenter);
            addFragment(R.id.activity_live_room_red_packet, mRedPacketFragment);
        } else {
            //隐藏
            removeFragment(mRedPacketFragment);
            mRedPacketFragment = null;
            mRedPacketPresenter.unSubscribe();
            mRedPacketPresenter = null;
        }
    }

    @Override
    public void updateRedPacket() {
        if (mRedPacketFragment != null) {
            removeFragment(mRedPacketFragment);
            mRedPacketFragment = null;
            mRedPacketPresenter.destroy();
        }
        mRedPacketPresenter = null;
    }

}
