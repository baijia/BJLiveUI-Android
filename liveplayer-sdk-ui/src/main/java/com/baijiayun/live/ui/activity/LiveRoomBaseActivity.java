package com.baijiayun.live.ui.activity;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.inputmethod.InputMethodManager;

import com.baijiayun.live.ui.base.BaseDialogFragment;


public abstract class LiveRoomBaseActivity extends AppCompatActivity {

    protected boolean isForeground = true;// 判断Activity是否处于前台
    protected BaseDialogFragment tempDialogFragment;
    @Override
    protected void onCreate(Bundle bundle) {
        if (bundle != null) {
            //如果系统回收的Activity， 但是系统却保留了Fragment， 当Activity被重新初始化， 此时， 系统保存的Fragment 的getActivity为空，
            //所以要移除旧的Fragment ， 重新初始化新的Fragment
            String FRAGMENTS_TAG = "android:support:fragments";
            bundle.remove(FRAGMENTS_TAG);
        }
        super.onCreate(bundle);
    }

    protected void addFragment(int layoutId, Fragment fragment, boolean addToBackStack, String fragmentTag) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//        int inAnim = LPFragmentAnimUtil.getInAnim(fragment);
//        int outAnim = LPFragmentAnimUtil.getOutAnim(fragment);
//        if (inAnim != 0 && outAnim != 0) {
//            transaction.setCustomAnimations(inAnim, outAnim);
//        }
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
//        int inAnim = LPFragmentAnimUtil.getInAnim(fragment);
//        int outAnim = LPFragmentAnimUtil.getOutAnim(fragment);
//        if (inAnim != 0 && outAnim != 0) {
//            transaction.setCustomAnimations(inAnim, outAnim);
//        }
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
//        int inAnim = LPFragmentAnimUtil.getInAnim(fragment);
//        int outAnim = LPFragmentAnimUtil.getOutAnim(fragment);
//        if (inAnim != 0 && outAnim != 0) {
//            transaction.setCustomAnimations(inAnim, outAnim, inAnim, outAnim);
//        }
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
//        int inAnim = LPFragmentAnimUtil.getInAnim(fragment);
//        int outAnim = LPFragmentAnimUtil.getOutAnim(fragment);
//        if (inAnim != 0 && outAnim != 0) {
//            transaction.setCustomAnimations(inAnim, outAnim);
//        }
        transaction.show(fragment);
        transaction.commitAllowingStateLoss();
    }

    protected void switchFragment(Fragment from, Fragment to, int layoutId) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
//        int inAnim = LPFragmentAnimUtil.getInAnim(from);
//        int outAnim = LPFragmentAnimUtil.getOutAnim(from);
//        if (inAnim != 0 && outAnim != 0) {
//            transaction.setCustomAnimations(inAnim, outAnim, inAnim, outAnim);
//        }
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
}
