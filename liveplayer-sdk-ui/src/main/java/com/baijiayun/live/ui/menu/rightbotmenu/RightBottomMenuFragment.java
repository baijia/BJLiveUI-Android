package com.baijiayun.live.ui.menu.rightbotmenu;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.View;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseFragment;
import com.baijiayun.live.ui.utils.RotationObserver;
import com.baijiayun.live.ui.utils.RxUtils;
import com.baijiayun.livecore.context.LPConstants;

import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;


/**
 * Created by Shubo on 2017/2/16.
 */

public class RightBottomMenuFragment extends BaseFragment implements RightBottomMenuContract.View,
        RotationObserver.OnRotationSettingChangedListener {

    private RightBottomMenuContract.Presenter presenter;
    private Disposable subscriptionOfVideoClick, subscriptionOfAudioClick, subscriptionOfZoomClick, subscriptionOfMoreClick;
    private RotationObserver rotationObserver;

    @Override
    public int getLayoutId() {
        return R.layout.fragment_right_bottom_menu;
    }

    @Override
    public void init(Bundle savedInstanceState) {
        super.init(savedInstanceState);

        subscriptionOfVideoClick = $.id(R.id.fragment_right_bottom_video).clicked()
                .throttleFirst(2, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer aVoid) {
                        if (!clickableCheck()) {
                            showToast(getString(R.string.live_frequent_error));
                            return;
                        }
                        presenter.changeVideo();
                    }
                });

        subscriptionOfAudioClick = $.id(R.id.fragment_right_bottom_audio).clicked()
                .throttleFirst(2, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer aVoid) {
                        if (!clickableCheck()) {
                            showToast(getString(R.string.live_frequent_error));
                            return;
                        }
                        presenter.changeAudio();
                    }
                });

        subscriptionOfMoreClick = $.id(R.id.fragment_right_bottom_more).clicked()
                .throttleFirst(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer aVoid) {
                        int location[] = new int[2];
                        $.id(R.id.fragment_right_bottom_more).view().getLocationInWindow(location);
                        presenter.more(location[0], location[1]);
                    }
                });

        subscriptionOfZoomClick = $.id(R.id.fragment_right_bottom_zoom)
                .clicked()
                .throttleFirst(1, TimeUnit.SECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Consumer<Integer>() {
                    @Override
                    public void accept(Integer aVoid) {
                        presenter.changeZoom();
                    }
                });

        rotationObserver = new RotationObserver(new Handler(), getActivity().getContentResolver());
        rotationObserver.startObserver();
        rotationObserver.setOnRotationSettingChangedListener(this);
    }

    @Override
    public void showVideoStatus(boolean isOn) {
        if (isOn) {
            $.id(R.id.fragment_right_bottom_video).image(R.drawable.live_ic_stopvideo_on);
            showToast(getString(R.string.live_camera_on));
        } else {
            $.id(R.id.fragment_right_bottom_video).image(R.drawable.live_ic_stopvideo);
            showToast(getString(R.string.live_camera_off));
        }
    }

    @Override
    public void showAudioStatus(boolean isOn) {
        if (isOn) {
            $.id(R.id.fragment_right_bottom_audio).image(R.drawable.live_ic_stopaudio_1);
            showToast(getString(R.string.live_mic_on));
        } else {
            $.id(R.id.fragment_right_bottom_audio).image(R.drawable.live_ic_stopaudio);
            showToast(getString(R.string.live_mic_off));
        }
    }

    @Override
    public void enableSpeakerMode() {
        $.id(R.id.fragment_right_bottom_video).visible();
        $.id(R.id.fragment_right_bottom_audio).visible();
        presenter.notifySpeakerStatus(true);
    }

    @Override
    public void disableSpeakerMode() {
        $.id(R.id.fragment_right_bottom_video).gone();
        $.id(R.id.fragment_right_bottom_audio).gone();
        presenter.notifySpeakerStatus(false);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void clearScreen() {
        //fix npe bugly #81021
        if($ == null){
            return;
        }
        if ($.id(R.id.fragment_right_bottom_video).view().getVisibility() == View.VISIBLE)
            $.id(R.id.fragment_right_bottom_video).invisible();
        if ($.id(R.id.fragment_right_bottom_audio).view().getVisibility() == View.VISIBLE)
            $.id(R.id.fragment_right_bottom_audio).invisible();
        if ($.id(R.id.fragment_right_bottom_zoom).view().getVisibility() == View.VISIBLE)
            $.id(R.id.fragment_right_bottom_zoom).invisible();
    }

    @Override
    public void unClearScreen() {
        if ($ == null) return;
        if ($.id(R.id.fragment_right_bottom_video).view().getVisibility() == View.INVISIBLE)
            $.id(R.id.fragment_right_bottom_video).visible();
        if ($.id(R.id.fragment_right_bottom_audio).view().getVisibility() == View.INVISIBLE)
            $.id(R.id.fragment_right_bottom_audio).visible();
        if ($.id(R.id.fragment_right_bottom_zoom).view().getVisibility() == View.INVISIBLE)
            $.id(R.id.fragment_right_bottom_zoom).visible();
        presenter.getSysRotationSetting();
    }

    @Override
    public void showVolume(LPConstants.VolumeLevel level) {
        // level between [0,9]
        switch (level) {
//            case MUTE:
//                $.id(R.id.fragment_right_bottom_audio).image(R.drawable.live_ic_stopaudio_1);
            case NONE:
                $.id(R.id.fragment_right_bottom_audio).image(R.drawable.live_ic_stopaudio_2);
                break;
            case LOW:
                $.id(R.id.fragment_right_bottom_audio).image(R.drawable.live_ic_stopaudio_3);
                break;
            case MIDDLE:
                $.id(R.id.fragment_right_bottom_audio).image(R.drawable.live_ic_stopaudio_4);
                break;
            case HIGH:
                $.id(R.id.fragment_right_bottom_audio).image(R.drawable.live_ic_stopaudio_5);
//            case 9:
//                $.id(R.id.fragment_right_bottom_audio).image(R.drawable.live_ic_stopaudio_6);
                break;
            default:
                break;
        }
    }

    @Override
    public void showZoomIn() {
        $.id(R.id.fragment_right_bottom_zoom).image(R.drawable.live_ic_zoomin);
    }

    @Override
    public void showZoomOut() {
        $.id(R.id.fragment_right_bottom_zoom).image(R.drawable.live_ic_zoomout);
    }

    @Override
    public void showZoom() {
        $.id(R.id.fragment_right_bottom_zoom).visible();
    }

    @Override
    public void hideZoom() {
        $.id(R.id.fragment_right_bottom_zoom).gone();
    }

    @Override
    public void showAudioRoomError() {
        showToast(getString(R.string.live_audio_room_error));
    }

    @Override
    public void setPresenter(RightBottomMenuContract.Presenter presenter) {
        super.setBasePresenter(presenter);
        this.presenter = presenter;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        RxUtils.dispose(subscriptionOfAudioClick);
        RxUtils.dispose(subscriptionOfVideoClick);
        RxUtils.dispose(subscriptionOfMoreClick);
        RxUtils.dispose(subscriptionOfZoomClick);
        rotationObserver.stopObserver();
        presenter = null;
    }

    @Override
    public void onRotationSettingChanged() {
        presenter.setSysRotationSetting();
    }

    private Disposable subscriptionOfClickable;

    private boolean clickableCheck() {
        if (subscriptionOfClickable != null && !subscriptionOfClickable.isDisposed()) {
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

    @Override
    public void setAudition() {
        $.id(R.id.fragment_right_bottom_audio).visibility(View.GONE);
        $.id(R.id.fragment_right_bottom_video).visibility(View.GONE);
    }
}
