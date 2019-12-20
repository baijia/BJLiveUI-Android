package com.baijiayun.live.ui.more;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseDialogFragment;
import com.baijiayun.live.ui.utils.DisplayUtils;
import com.baijiayun.live.ui.utils.QueryPlus;

/**
 * Created by Shubo on 2017/4/17.
 */

public class MoreMenuDialogFragment extends BaseDialogFragment implements MoreMenuContract.View {

    private MoreMenuContract.Presenter presenter;
    private int anchorX, anchorY;
    private QueryPlus $;

    public static MoreMenuDialogFragment newInstance(int anchorX, int anchorY) {

        Bundle args = new Bundle();
        args.putInt("anchorX", anchorX);
        args.putInt("anchorY", anchorY);

        MoreMenuDialogFragment fragment = new MoreMenuDialogFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.bjy_dialog_more;
    }

    @Override
    protected void init(Bundle savedInstanceState, Bundle arguments) {
        super.hideBackground().contentBackgroundColor(ContextCompat.getColor(getContext(), R.color.live_transparent));
        $ = QueryPlus.with(contentView);
        anchorX = arguments.getInt("anchorX");
        anchorY = arguments.getInt("anchorY");
        $.id(R.id.dialog_more_announcement).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.navigateToAnnouncement();
                dismissAllowingStateLoss();
            }
        });
        $.id(R.id.dialog_more_record).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.switchCloudRecord();
            }
        });
        $.id(R.id.dialog_more_help).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.navigateToHelp();
                dismissAllowingStateLoss();
            }
        });
        $.id(R.id.dialog_more_setting).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.navigateToSetting();
                dismissAllowingStateLoss();
            }
        });

        if (presenter.isTeacher()) {
            $.id(R.id.dialog_more_record).visible();
        } else {
            $.id(R.id.dialog_more_record).gone();
        }
    }

    @Override
    protected void setWindowParams(WindowManager.LayoutParams windowParams) {
        windowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        windowParams.gravity = Gravity.BOTTOM | GravityCompat.END;
        windowParams.x = DisplayUtils.getScreenWidthPixels(getContext()) - anchorX;
        windowParams.y = DisplayUtils.getScreenHeightPixels(getContext()) - anchorY;

        windowParams.windowAnimations = R.style.LiveMoreDialogAnim;
    }

    @Override
    protected void resetWindowParams(WindowManager.LayoutParams windowParams) {
        // do nothing
    }

    @Override
    public void setPresenter(MoreMenuContract.Presenter presenter) {
        super.setBasePresenter(presenter);
        this.presenter = presenter;
    }

    @Override
    public void showCloudRecordOn() {
        TextView tv = (TextView) $.id(R.id.dialog_more_record).view();
        tv.setText(getString(R.string.live_cloud_recording));
        tv.setTextColor(ContextCompat.getColor(getContext(), R.color.live_red));
        Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.live_ic_luxiang_on);
        drawable.setBounds(0, 0, DisplayUtils.dip2px(getContext(), 36), DisplayUtils.dip2px(getContext(), 36));
        tv.setCompoundDrawables(null, drawable, null, null);
    }

    @Override
    public void showCloudRecordOff() {
        TextView tv = (TextView) $.id(R.id.dialog_more_record).view();
        tv.setText(getString(R.string.live_cloud_record));
        tv.setTextColor(ContextCompat.getColor(getContext(), R.color.live_text_color_mid_light));
        Drawable drawable = ContextCompat.getDrawable(getContext(), R.drawable.live_ic_luxiang);
        drawable.setBounds(0, 0, DisplayUtils.dip2px(getContext(), 36), DisplayUtils.dip2px(getContext(), 36));
        tv.setCompoundDrawables(null, drawable, null, null);
    }

    @Override
    public void setAudition() {
        $.id(R.id.dialog_more_setting).visibility(View.GONE);
    }

    @Override
    public void showCloudRecordNotAllowed(String reason) {
        showToast(reason);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter = null;
        $ = null;
    }
}
