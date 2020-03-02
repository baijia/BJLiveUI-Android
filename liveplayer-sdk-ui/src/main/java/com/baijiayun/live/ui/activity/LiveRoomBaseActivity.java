package com.baijiayun.live.ui.activity;

import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.StringRes;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Toast;

import com.baijiayun.live.ui.LiveSDKWithUI;
import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseDialogFragment;
import com.baijiayun.livecore.LiveSDK;
import com.baijiayun.livecore.models.imodels.IUserModel;
import com.baijiayun.livecore.utils.ToastCompat;


public abstract class LiveRoomBaseActivity extends AppCompatActivity {

    protected boolean isForeground = true;// 判断Activity是否处于前台
    protected BaseDialogFragment tempDialogFragment;
    protected String code, name, avatar, sign;
    protected long roomId;
    protected IUserModel enterUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) { // 5.0+ 打开硬件加速
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (savedInstanceState != null) {
            //如果系统回收的Activity， 但是系统却保留了Fragment， 当Activity被重新初始化， 此时， 系统保存的Fragment 的getActivity为空，
            //所以要移除旧的Fragment ， 重新初始化新的Fragment
            String FRAGMENTS_TAG = "android:support:fragments";
            savedInstanceState.remove(FRAGMENTS_TAG);
        }
        super.onCreate(savedInstanceState);

        // 大班课自动播放屏幕分享和媒体文件
        LiveSDK.AUTO_PLAY_SHARING_SCREEN_AND_MEDIA = true;

        // x86平台的机器不让进教室
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (Build.SUPPORTED_ABIS[0].contains("x86")) {
                showToastMessage(getString(R.string.live_room_x86_not_supported));
                super.finish();
            }
        } else {
            if (Build.CPU_ABI.contains("x86")) {
                showToastMessage(getString(R.string.live_room_x86_not_supported));
                super.finish();
            }
        }

        if (savedInstanceState == null) {
            code = getIntent().getStringExtra("code");
            name = getIntent().getStringExtra("name");
            avatar = getIntent().getStringExtra("avatar");
            roomId = getIntent().getLongExtra("roomId", -1L);
            sign = getIntent().getStringExtra("sign");
            enterUser = (IUserModel) getIntent().getSerializableExtra("user");
        } else {
            code = savedInstanceState.getString("code");
            name = savedInstanceState.getString("name");
            avatar = savedInstanceState.getString("avatar");
            roomId = savedInstanceState.getLong("roomId", -1L);
            sign = savedInstanceState.getString("sign");
            enterUser = (IUserModel) savedInstanceState.getSerializable("user");
        }
    }

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

    public static boolean getShowTechSupport() {
        return shouldShowTechSupport;
    }

    public abstract LiveRoomRouterListener getRouterListener();

    protected void addFragment(int layoutId, Fragment fragment, boolean addToBackStack, String fragmentTag) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (fragmentTag == null) {
            transaction.add(layoutId, fragment);
        } else {
            transaction.add(layoutId, fragment, fragmentTag);
        }
        transaction.commitAllowingStateLoss();
    }

    protected void addFragment(int layoutId, Fragment fragment, boolean addToBackStack) {
        addFragment(layoutId, fragment, addToBackStack, null);
    }

    protected void addFragment(int layoutId, Fragment fragment, String tag) {
        addFragment(layoutId, fragment, false, tag);
    }

    protected void addFragment(int layoutId, Fragment fragment) {
        addFragment(layoutId, fragment, false);
    }

    protected Fragment findFragment(int layoutId) {
        FragmentManager fm = getSupportFragmentManager();
        return fm.findFragmentById(layoutId);
    }

    protected void removeFragment(Fragment fragment) {
        if (fragment == null) return;
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.remove(fragment);
        transaction.commitAllowingStateLoss();
    }

    protected void removeAllFragment() {
        for (Fragment fragment : getSupportFragmentManager().getFragments()) {
            if (fragment != null)
                getSupportFragmentManager().beginTransaction().remove(fragment).commitAllowingStateLoss();
        }
    }

    protected void hideFragment(Fragment fragment) {
        if (!fragment.isAdded())
            return;
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.hide(fragment);
        transaction.commitAllowingStateLoss();
    }

    protected void replaceFragment(int layoutId, Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(layoutId, fragment);
        transaction.commitAllowingStateLoss();
    }

    protected void showFragment(Fragment fragment) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.show(fragment);
        transaction.commitAllowingStateLoss();
    }

    protected void switchFragment(Fragment from, Fragment to, int layoutId) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        if (!to.isAdded()) {    // 先判断是否被add过
            transaction.hide(from).add(layoutId, to).commit(); // 隐藏当前的fragment，add下一个到Activity中
        } else {
            transaction.hide(from).show(to).commit(); // 隐藏当前的fragment，显示下一个
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (null != this.getCurrentFocus()) {
            /**
             * 点击空白位置 隐藏软键盘
             */
            InputMethodManager mInputMethodManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            return mInputMethodManager.hideSoftInputFromWindow(this.getCurrentFocus().getWindowToken(), 0);
        }
        return super.onTouchEvent(event);
    }

    protected void showDialogFragment(final BaseDialogFragment dialogFragment) {
        if(isFinishing()) return;
        if(!isForeground){
            tempDialogFragment = dialogFragment;
            return;
        }
        if (getSupportFragmentManager().isStateSaved()) {
            return;
        }
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        dialogFragment.show(ft, dialogFragment.getClass().getSimpleName() + dialogFragment.hashCode());
        getSupportFragmentManager().executePendingTransactions();
        dialogFragment.getDialog().setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                if (isFinishing()) return;
                FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                Fragment prev = getSupportFragmentManager().findFragmentByTag(dialogFragment.getClass().getSimpleName() + dialogFragment.hashCode());
                if (prev != null)
                    ft.remove(prev);
                ft.commitAllowingStateLoss();
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        isForeground = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isForeground = true;
        if (tempDialogFragment != null){
            showDialogFragment(tempDialogFragment);
            tempDialogFragment = null;
        }
    }

    protected void showToastMessage(final String message) {
        if (TextUtils.isEmpty(message)) return;

        this.runOnUiThread(() -> {
            if (this.isFinishing() || this.isDestroyed()) return;
            ToastCompat.showToastCenter(LiveRoomBaseActivity.this,message,Toast.LENGTH_SHORT);
        });
    }

    protected void showToastMessage(@StringRes int strRes) {
        showToastMessage(getResources().getString(strRes));
    }


    /* 房间外部回调 */
    protected static LiveSDKWithUI.LPShareListener shareListener;
    protected static LiveSDKWithUI.LPRoomExitListener exitListener;
    protected static LiveSDKWithUI.RoomEnterConflictListener enterRoomConflictListener;
    protected static LiveSDKWithUI.LPRoomResumeListener roomLifeCycleListener;
    protected static String liveHorseLamp; // 跑马灯
    protected static int liveHorseLampInterval = 60; // 跑马灯时间间隔
    protected static boolean shouldShowTechSupport = false; //是否显示百家云提供技术支持话术

    protected static boolean disableSpeakQueuePlaceholder = false;

    protected void clearStaticCallback() {
        shareListener = null;
        exitListener = null;
        enterRoomConflictListener = null;
        roomLifeCycleListener = null;
    }

    public static LiveSDKWithUI.LPRoomExitListener getExitListener() {
        return exitListener;
    }

    public static void setRoomLifeCycleListener(LiveSDKWithUI.LPRoomResumeListener roomLifeCycleListener) {
        LiveRoomBaseActivity.roomLifeCycleListener = roomLifeCycleListener;
    }

    public static void setShareListener(LiveSDKWithUI.LPShareListener listener) {
        LiveRoomBaseActivity.shareListener = listener;
    }

    public static void setRoomExitListener(LiveSDKWithUI.LPRoomExitListener roomExitListener) {
        LiveRoomBaseActivity.exitListener = roomExitListener;
    }

    public static void setEnterRoomConflictListener(LiveSDKWithUI.RoomEnterConflictListener enterRoomConflictListener) {
        LiveRoomBaseActivity.enterRoomConflictListener = enterRoomConflictListener;
    }

    public static void setLiveRoomMarqueeTape(String liveRoomMarqueeTape) {
        LiveRoomBaseActivity.liveHorseLamp = liveRoomMarqueeTape;
    }

    public static void setLiveRoomMarqueeTape(String liveRoomMarqueeTape, int liveRoomMarqueeTapeInterval) {
        LiveRoomBaseActivity.liveHorseLamp = liveRoomMarqueeTape;
        LiveRoomBaseActivity.liveHorseLampInterval = liveRoomMarqueeTapeInterval;
    }

    public static void disableSpeakQueuePlaceholder() {
        disableSpeakQueuePlaceholder = true;
    }

}
