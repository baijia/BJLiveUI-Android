package com.baijiahulian.live.ui.leftmenu;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.View;

import com.baijiahulian.live.ui.R;
import com.baijiahulian.live.ui.base.BaseFragment;

/**
 * Created by Shubo on 2017/2/15.
 */

public class LeftMenuFragment extends BaseFragment implements LeftMenuContract.View {

    LeftMenuContract.Presenter presenter;

    @Override
    public int getLayoutId() {
        return R.layout.fragment_leftmenu;
    }

    @Override
    protected void init(Bundle savedInstanceState) {
        super.init(savedInstanceState);
        $.id(R.id.fragment_left_menu_clear_screen).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.clearScreen();
            }
        });
        $.id(R.id.fragment_left_menu_send_message).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (presenter.isAllForbidden() || presenter.isForbiddenByTeacher()) {
                    showToast(getString(R.string.live_forbid_send_message));
                    return;
                }
                presenter.showMessageInput();
            }
        });
//        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
//            $.id(R.id.fragment_left_menu_clear_screen).gone();
//        } else {
//            $.id(R.id.fragment_left_menu_clear_screen).visible();
//        }
    }

    @Override
    public void notifyClearScreenChanged(boolean isCleared) {
        if (isCleared)
            $.id(R.id.fragment_left_menu_clear_screen).image(R.drawable.live_ic_clear_on);
        else $.id(R.id.fragment_left_menu_clear_screen).image(R.drawable.live_ic_clear);
    }

    @Override
    public void showDebugBtn() {
        $.id(R.id.fragment_left_menu_stream).visible();
        $.id(R.id.fragment_left_menu_stream).view().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.showStreamDebugPanel();
            }
        });
        $.id(R.id.fragment_left_menu_huiyin).visible();
        $.id(R.id.fragment_left_menu_huiyin).view().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.showHuiyinDebugPanel();
            }
        });
        $.id(R.id.fragment_left_menu_copy_2_SD).visible();
        $.id(R.id.fragment_left_menu_copy_2_SD).view().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.showCopyLogDebugPanel();
            }
        });
    }

    @Override
    public void setPresenter(LeftMenuContract.Presenter presenter) {
        super.setBasePresenter(presenter);
        this.presenter = presenter;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
//        if (presenter.isScreenCleared()) {
//            presenter.clearScreen();
//        }
//        if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
//            $.id(R.id.fragment_left_menu_clear_screen).gone();
//        } else {
//            $.id(R.id.fragment_left_menu_clear_screen).visible();
//        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        presenter = null;
    }
}
