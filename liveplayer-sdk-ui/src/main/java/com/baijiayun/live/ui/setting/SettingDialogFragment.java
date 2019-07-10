package com.baijiayun.live.ui.setting;

import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.widget.Button;

import com.afollestad.materialdialogs.MaterialDialog;
import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseDialogFragment;
import com.baijiayun.live.ui.utils.QueryPlus;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.context.LPError;
import com.baijiayun.livecore.utils.LPRxUtils;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;

/**
 * Created by Shubo on 2017/3/2.
 */

public class SettingDialogFragment extends BaseDialogFragment implements SettingContract.View {

    private QueryPlus $;
    private SettingContract.Presenter presenter;
    private Disposable disposable;

    public static SettingDialogFragment newInstance() {
        return new SettingDialogFragment();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_setting;
    }

    @Override
    protected void init(Bundle savedInstanceState, Bundle arguments) {
        super.title(getString(R.string.live_setting)).editable(false);
        $ = QueryPlus.with(contentView);
        $.id(R.id.dialog_setting_mic).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.changeMic();
            }
        });
        $.id(R.id.dialog_setting_camera).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkClickable(getString(R.string.live_frequent_error_switch_camera)))
                    presenter.changeCamera();
            }
        });
        $.id(R.id.dialog_setting_beauty_filter).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
                    showToast(getString(R.string.live_not_support_beauty_filter));
                } else if (presenter.isUseWebRTC()) {
                    showToast(getString(R.string.live_room_not_support_beautify));
                } else {
                    presenter.changeBeautyFilter();
                }
            }
        });
        $.id(R.id.dialog_setting_radio_ppt_fs).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.setPPTFullScreen();
            }
        });
        $.id(R.id.dialog_setting_radio_ppt_os).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.setPPTOverspread();
            }
        });
        $.id(R.id.dialog_setting_radio_definition_low).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkClickable(getString(R.string.live_frequent_error_resolution)))
                    presenter.setDefinitionLow();
            }
        });
        $.id(R.id.dialog_setting_ppt_view_type_anim).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkClickable(getString(R.string.live_frequent_error_switch_ppt)))
                    presenter.setPPTViewAnim();
            }
        });
        $.id(R.id.dialog_setting_ppt_view_type_static).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkClickable(getString(R.string.live_frequent_error_switch_ppt)))
                    presenter.setPPTViewStatic();
            }
        });
        $.id(R.id.dialog_setting_radio_definition_high).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkClickable(getString(R.string.live_frequent_error_resolution)))
                    presenter.setDefinitionHigh();
            }
        });
        $.id(R.id.dialog_setting_radio_definition_720).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkClickable(getString(R.string.live_frequent_error_resolution)))
                    presenter.setDefinition_720();
            }
        });
        $.id(R.id.dialog_setting_radio_definition_1080).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkClickable(getString(R.string.live_frequent_error_resolution)))
                    presenter.setDefinition_1080();
            }
        });
        $.id(R.id.dialog_setting_radio_link_up_1).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkClickable(getString(R.string.live_frequent_error_line))) {
                    if (presenter.getCDNCount() > 1) {
                        ArrayList<String> options = new ArrayList<>();
                        for (int i = 1; i <= presenter.getCDNCount(); i++) {
                            options.add("线路" + i);
                        }
                        new MaterialDialog.Builder(getActivity())
                                .items(options)
                                .itemsCallback((dialog, itemView, position, text) -> {
                                    presenter.setUpCDNLink(position);
                                    dialog.dismiss();
                                })
                                .show();
                    } else {
                        presenter.setUpLinkTCP();
                    }
                }

            }
        });
        $.id(R.id.dialog_setting_radio_link_up_2).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkClickable(getString(R.string.live_frequent_error_line)))
                    presenter.setUpLinkUDP();
            }
        });
        $.id(R.id.dialog_setting_radio_link_down_1).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkClickable(getString(R.string.live_frequent_error_line))) {
                    if (presenter.getCDNCount() > 1) {
                        ArrayList<String> options = new ArrayList<>();
                        for (int i = 1; i <= presenter.getCDNCount(); i++) {
                            options.add("线路" + i);
                        }
                        new MaterialDialog.Builder(getActivity())
                                .items(options)
                                .itemsCallback((dialog, itemView, position, text) -> {
                                    presenter.setDownCDNLink(position);
                                    dialog.dismiss();
                                })
                                .show();
                    } else {
                        presenter.setDownLinkTCP();
                    }
                }
            }
        });
        $.id(R.id.dialog_setting_radio_link_down_2).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkClickable(getString(R.string.live_frequent_error_line)))
                    presenter.setDownLinkUDP();
            }
        });

        $.id(R.id.dialog_setting_radio_camera_front).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkClickable(getString(R.string.live_frequent_error_camera)))
                    presenter.switchCamera();
            }
        });

        $.id(R.id.dialog_setting_radio_camera_back).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (checkClickable(getString(R.string.live_frequent_error_camera)))
                    presenter.switchCamera();
            }
        });

        $.id(R.id.dialog_setting_forbid_all_speak).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.switchForbidStatus();
            }
        });

        $.id(R.id.dialog_setting_forbid_raise_hand).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.switchForbidRaiseHand();
            }
        });

        $.id(R.id.dialog_setting_forbid_all_audio).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                presenter.switchForbidAllAudio();
            }
        });


        if (presenter.isTeacherOrAssistant()) {
            //只要是老师或助教，就显示全体禁言
            $.id(R.id.dialog_setting_forbid_all_speak_container).visible();
            //只有小班才有静音功能.
            if (presenter.getRoomType() == LPConstants.LPRoomType.Multi) {
                //大班课显示禁止举手，不显示全体静音
                $.id(R.id.dialog_setting_forbid_raise_hand_container).visible();
                $.id(R.id.dialog_setting_forbid_all_audio_container).gone();
            } else if (presenter.getRoomType() == LPConstants.LPRoomType.SmallGroup
                    || presenter.getRoomType() == LPConstants.LPRoomType.NewSmallGroup || presenter.getRoomType() == LPConstants.LPRoomType.DoubleTeachers) {
                //小班课、新版小班课、双师，显示全体静音，不显示禁止举手
                $.id(R.id.dialog_setting_forbid_all_audio_container).visible();
                $.id(R.id.dialog_setting_forbid_raise_hand_container).gone();
            } else if (presenter.getRoomType() == LPConstants.LPRoomType.Single) {
                //一对一，禁止举手、全体静音都不显示
                $.id(R.id.dialog_setting_forbid_raise_hand_container).gone();
                $.id(R.id.dialog_setting_forbid_all_audio_container).gone();
            }
        } else {
            $.id(R.id.dialog_setting_forbid_all_speak_container).gone();
            $.id(R.id.dialog_setting_forbid_raise_hand_container).gone();
            $.id(R.id.dialog_setting_forbid_all_audio_container).gone();
        }

    }

    @Override
    public void showMicOpen() {
        $.id(R.id.dialog_setting_mic).image(R.drawable.ic_on_switch);
    }

    @Override
    public void showMicClosed() {
        $.id(R.id.dialog_setting_mic).image(R.drawable.ic_off_switch);
    }

    @Override
    public void showCameraOpen() {
        $.id(R.id.dialog_setting_camera).image(R.drawable.ic_on_switch);
    }

    @Override
    public void showCameraClosed() {
        $.id(R.id.dialog_setting_camera).image(R.drawable.ic_off_switch);
    }

    @Override
    public void showBeautyFilterEnable() {
        $.id(R.id.dialog_setting_beauty_filter).image(R.drawable.ic_on_switch);
    }

    @Override
    public void showBeautyFilterDisable() {
        $.id(R.id.dialog_setting_beauty_filter).image(R.drawable.ic_off_switch);
    }

    @Override
    public void showPPTFullScreen() {
        $.id(R.id.dialog_setting_radio_ppt_fs).enable(false);
        $.id(R.id.dialog_setting_radio_ppt_os).enable(true);
        ((Button) $.id(R.id.dialog_setting_radio_ppt_fs).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_white));
        ((Button) $.id(R.id.dialog_setting_radio_ppt_os).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_text_color));
    }

    @Override
    public void showPPTOverspread() {
        $.id(R.id.dialog_setting_radio_ppt_fs).enable(true);
        $.id(R.id.dialog_setting_radio_ppt_os).enable(false);
        ((Button) $.id(R.id.dialog_setting_radio_ppt_fs).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_text_color));
        ((Button) $.id(R.id.dialog_setting_radio_ppt_os).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_white));
    }

    @Override
    public void showDefinitionLow(LPError lpError) {
        if (lpError != null) {
            showToast(lpError.getMessage());
        }
        $.id(R.id.dialog_setting_radio_definition_low).enable(false);
        $.id(R.id.dialog_setting_radio_definition_high).enable(true);
        $.id(R.id.dialog_setting_radio_definition_720).enable(true);
        $.id(R.id.dialog_setting_radio_definition_1080).enable(true);
        ((Button) $.id(R.id.dialog_setting_radio_definition_low).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_white));
        ((Button) $.id(R.id.dialog_setting_radio_definition_high).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_text_color));
        ((Button) $.id(R.id.dialog_setting_radio_definition_720).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_text_color));
        ((Button) $.id(R.id.dialog_setting_radio_definition_1080).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_text_color));
    }

    @Override
    public void showDefinitionHigh(LPError lpError) {
        if (lpError != null) {
            showToast(lpError.getMessage());
        }
        $.id(R.id.dialog_setting_radio_definition_low).enable(true);
        $.id(R.id.dialog_setting_radio_definition_high).enable(false);
        $.id(R.id.dialog_setting_radio_definition_720).enable(true);
        $.id(R.id.dialog_setting_radio_definition_1080).enable(true);
        ((Button) $.id(R.id.dialog_setting_radio_definition_low).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_text_color));
        ((Button) $.id(R.id.dialog_setting_radio_definition_high).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_white));
        ((Button) $.id(R.id.dialog_setting_radio_definition_720).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_text_color));
        ((Button) $.id(R.id.dialog_setting_radio_definition_1080).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_text_color));
    }

    @Override
    public void showDefinition_720(LPError lpError) {
        if (lpError != null) {
            showToast(lpError.getMessage());
        }
        $.id(R.id.dialog_setting_radio_definition_low).enable(true);
        $.id(R.id.dialog_setting_radio_definition_high).enable(true);
        $.id(R.id.dialog_setting_radio_definition_720).enable(false);
        $.id(R.id.dialog_setting_radio_definition_1080).enable(true);
        ((Button) $.id(R.id.dialog_setting_radio_definition_low).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_text_color));
        ((Button) $.id(R.id.dialog_setting_radio_definition_high).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_text_color));
        ((Button) $.id(R.id.dialog_setting_radio_definition_720).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_white));
        ((Button) $.id(R.id.dialog_setting_radio_definition_1080).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_text_color));
    }

    @Override
    public void showDefinition_1080(LPError lpError) {
        if (lpError != null) {
            showToast(lpError.getMessage());
        }
        $.id(R.id.dialog_setting_radio_definition_low).enable(true);
        $.id(R.id.dialog_setting_radio_definition_high).enable(true);
        $.id(R.id.dialog_setting_radio_definition_720).enable(true);
        $.id(R.id.dialog_setting_radio_definition_1080).enable(false);
        ((Button) $.id(R.id.dialog_setting_radio_definition_low).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_text_color));
        ((Button) $.id(R.id.dialog_setting_radio_definition_high).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_text_color));
        ((Button) $.id(R.id.dialog_setting_radio_definition_720).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_text_color));
        ((Button) $.id(R.id.dialog_setting_radio_definition_1080).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_white));
    }

    @Override
    public void showDefinition(LPConstants.LPResolutionType resolutionType) {
        switch (resolutionType) {
            case LOW:
                $.id(R.id.dialog_setting_radio_definition_high).gone();
                $.id(R.id.dialog_setting_radio_definition_720).gone();
                $.id(R.id.dialog_setting_radio_definition_1080).gone();
                $.id(R.id.rl_setting_definition_webrtc).gone();
                break;
            case HIGH:
                $.id(R.id.dialog_setting_radio_definition_720).gone();
                $.id(R.id.dialog_setting_radio_definition_1080).gone();
                $.id(R.id.rl_setting_definition_webrtc).gone();
                break;
            case _720:
                $.id(R.id.dialog_setting_radio_definition_1080).gone();
                $.id(R.id.rl_setting_definition_webrtc).visible();
                break;
            case _1080:
                $.id(R.id.rl_setting_definition_webrtc).visible();
                break;
            default:
                $.id(R.id.dialog_setting_radio_definition_high).gone();
                $.id(R.id.dialog_setting_radio_definition_720).gone();
                $.id(R.id.dialog_setting_radio_definition_1080).gone();
                $.id(R.id.rl_setting_definition_webrtc).gone();
                break;
        }
    }

    @Override
    public void showUpLinkTCP() {
        $.id(R.id.dialog_setting_radio_link_up_1).enable(false);
        $.id(R.id.dialog_setting_radio_link_up_2).enable(true);
        ((Button) $.id(R.id.dialog_setting_radio_link_up_1).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_white));
        ((Button) $.id(R.id.dialog_setting_radio_link_up_2).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_text_color));
    }

    @Override
    public void showUpLinkUDP() {
        $.id(R.id.dialog_setting_radio_link_up_1).enable(true);
        $.id(R.id.dialog_setting_radio_link_up_2).enable(false);
        ((Button) $.id(R.id.dialog_setting_radio_link_up_1).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_text_color));
        ((Button) $.id(R.id.dialog_setting_radio_link_up_2).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_white));
    }

    @Override
    public void showDownLinkTCP() {
        $.id(R.id.dialog_setting_radio_link_down_1).enable(false);
        $.id(R.id.dialog_setting_radio_link_down_2).enable(true);
        ((Button) $.id(R.id.dialog_setting_radio_link_down_1).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_white));
        ((Button) $.id(R.id.dialog_setting_radio_link_down_2).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_text_color));
    }

    @Override
    public void showDownLinkUDP() {
        $.id(R.id.dialog_setting_radio_link_down_1).enable(true);
        $.id(R.id.dialog_setting_radio_link_down_2).enable(false);
        ((Button) $.id(R.id.dialog_setting_radio_link_down_1).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_text_color));
        ((Button) $.id(R.id.dialog_setting_radio_link_down_2).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_white));
    }

    @Override
    public void showVisitorFail() {
        showToast(getString(R.string.live_media_visitor_fail));
    }

    @Override
    public void showStudentFail() {
        showToast(getString(R.string.live_media_student_fail));
    }

    @Override
    public void showSmallGroupFail() {
        showToast(getString(R.string.live_media_group_fail));
    }

    @Override
    public void showCameraFront() {
        $.id(R.id.dialog_setting_radio_camera_front).enable(false);
        $.id(R.id.dialog_setting_radio_camera_back).enable(true);
        ((Button) $.id(R.id.dialog_setting_radio_camera_front).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_white));
        ((Button) $.id(R.id.dialog_setting_radio_camera_back).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_text_color));
    }

    @Override
    public void showCameraBack() {
        $.id(R.id.dialog_setting_radio_camera_front).enable(true);
        $.id(R.id.dialog_setting_radio_camera_back).enable(false);
        ((Button) $.id(R.id.dialog_setting_radio_camera_front).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_text_color));
        ((Button) $.id(R.id.dialog_setting_radio_camera_back).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_white));
    }

    @Override
    public void showPPTViewTypeAnim() {
        $.id(R.id.dialog_setting_ppt_view_type_anim).enable(false);
        $.id(R.id.dialog_setting_ppt_view_type_static).enable(true);
        ((Button) $.id(R.id.dialog_setting_ppt_view_type_anim).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_white));
        ((Button) $.id(R.id.dialog_setting_ppt_view_type_static).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_text_color));
    }

    @Override
    public void showPPTViewTypeStatic() {
        $.id(R.id.dialog_setting_ppt_view_type_anim).enable(true);
        $.id(R.id.dialog_setting_ppt_view_type_static).enable(false);
        ((Button) $.id(R.id.dialog_setting_ppt_view_type_anim).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_text_color));
        ((Button) $.id(R.id.dialog_setting_ppt_view_type_static).view()).setTextColor(ContextCompat.getColor(getContext(), R.color.live_white));
    }

    @Override
    public void showCameraSwitchStatus(boolean whetherShow) {
        if (whetherShow) {
            $.id(R.id.dialog_setting_camera_switch_wrapper).visible();
        } else {
            $.id(R.id.dialog_setting_camera_switch_wrapper).gone();
        }
    }

    @Override
    public void showForbidden() {
        $.id(R.id.dialog_setting_forbid_all_speak).image(R.drawable.ic_on_switch);
    }

    @Override
    public void showNotForbidden() {
        $.id(R.id.dialog_setting_forbid_all_speak).image(R.drawable.ic_off_switch);
    }

    @Override
    public void showAudioRoomError() {
        showToast(getString(R.string.live_audio_room_error));
    }

    @Override
    public void showForbidRaiseHandOn() {
        $.id(R.id.dialog_setting_forbid_raise_hand).image(R.drawable.ic_on_switch);
    }

    @Override
    public void showForbidRaiseHandOff() {
        $.id(R.id.dialog_setting_forbid_raise_hand).image(R.drawable.ic_off_switch);
    }

    @Override
    public void showForbidAllAudioOn() {
        $.id(R.id.dialog_setting_forbid_all_audio).image(R.drawable.ic_on_switch);
    }

    @Override
    public void showForbidAllAudioOff() {
        $.id(R.id.dialog_setting_forbid_all_audio).image(R.drawable.ic_off_switch);
    }

    @Override
    public void showSwitchLinkTypeError() {
        showToast(getString(R.string.live_switch_link_type_error));
    }

    @Override
    public void hidePPTShownType() {
        $.id(R.id.dialog_setting_radio_ppt_container).gone();
    }

    @Override
    public void showSwitchPPTFail() {
        showToast("老师上传的PPT都是静态的哦");
    }

    @Override
    public void setPresenter(SettingContract.Presenter presenter) {
        super.setBasePresenter(presenter);
        this.presenter = presenter;
    }

    private Map<String, AtomicBoolean> clickableWithKey = new ConcurrentHashMap<>();

    private boolean checkClickable(String error) {
        if (!clickableWithKey.containsKey(error)) {
            clickableWithKey.put(error, new AtomicBoolean(true));
        }
        final AtomicBoolean clickable = clickableWithKey.get(error);
        if (clickable.get()) {
            clickable.set(false);
            disposable = Observable.timer(2, TimeUnit.SECONDS).subscribe(aLong -> clickable.set(true));
            return true;
        } else {
            showToast(error);
            return false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter = null;
        $ = null;
        LPRxUtils.dispose(disposable);
    }
}
