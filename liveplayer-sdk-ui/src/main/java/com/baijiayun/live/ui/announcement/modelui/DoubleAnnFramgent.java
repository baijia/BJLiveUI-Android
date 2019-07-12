package com.baijiayun.live.ui.announcement.modelui;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.announcement.AnnouncementContract;
import com.baijiayun.live.ui.base.BaseFragment;
import com.baijiayun.livecore.models.imodels.IAnnouncementModel;

/**
 * 分组老师与学生信息展示
 * panzq
 * 20190708
 */
public class DoubleAnnFramgent extends BaseFragment implements DoubleAnnContract.View {

    private DoubleAnnContract.Presenter mPresenter;

    private IAnnouncementModel mIAnnouncementModel;
    private boolean isTeacher = true;

    @Override
    public int getLayoutId() {
        return R.layout.fragment_announcement_double_ann;
    }

    @Override
    public void setPresenter(DoubleAnnContract.Presenter presenter) {
        setBasePresenter(presenter);
        this.mPresenter = presenter;
    }

    @Override
    protected void init(Bundle savedInstanceState) {
        super.init(savedInstanceState);

        TextView text1 = (TextView) $.id(R.id.tv_announcement_notice_info).view();
        TextView text2 = (TextView) $.id(R.id.tv_announcement_notice_group_info).view();

        text1.setMovementMethod(ScrollingMovementMethod.getInstance());
        text2.setMovementMethod(ScrollingMovementMethod.getInstance());

        if (mIAnnouncementModel != null)
            setNoticeInfo(mIAnnouncementModel);
    }

    @Override
    public void setType(int type) {

        if (type == AnnouncementContract.TYPE_UI_TEACHERORASSISTANT) {
            //大班老师
            $.id(R.id.tv_announcement_double_group_title).visibility(View.INVISIBLE);
            $.id(R.id.tv_announcement_notice_group_info).visibility(View.INVISIBLE);
            $.id(R.id.tv_double_ann_info).visibility(View.INVISIBLE);
            $.id(R.id.iv_double_ann_down).visibility(View.INVISIBLE);
        } else if (type == AnnouncementContract.TYPE_UI_GROUPTEACHERORASSISTANT) {
            //分组老师
            isTeacher = true;
        } else {
            //学生
            $.id(R.id.tv_double_ann_info).visibility(View.INVISIBLE);
            isTeacher = false;
        }
    }

    @Override
    public void setNoticeInfo(IAnnouncementModel iAnnouncementModel) {

        if ($ == null) {
            mIAnnouncementModel = iAnnouncementModel;
            return;
        }

        View view;
        String link;
        if ("0".equals(iAnnouncementModel.getGroup())) {
            //大班老师公告
            if (!TextUtils.isEmpty(iAnnouncementModel.getContent())) {
                $.id(R.id.tv_announcement_notice_info).text(iAnnouncementModel.getContent());
                view = $.id(R.id.tv_announcement_notice_info).view();
                link = iAnnouncementModel.getLink();
            } else {
                $.id(R.id.tv_announcement_notice_info).text(getResources().getString(R.string.live_announcement_none));
                view = $.id(R.id.tv_announcement_notice_info).view();
                link = null;
            }
        } else {

            //分组老师通知
            if ("notice_change".equals(iAnnouncementModel.getMessageType())) {
                if (!TextUtils.isEmpty(iAnnouncementModel.getContent())) {
                    showDownUI(true);
                    $.id(R.id.tv_announcement_notice_group_info).text(iAnnouncementModel.getContent());
                    view = $.id(R.id.tv_announcement_notice_group_info).view();
                    link = iAnnouncementModel.getLink();

                } else {
                    showDownUI(false);
                    $.id(R.id.tv_announcement_notice_group_info).text(getResources().getString(R.string.string_notice_group_none));
                    view = $.id(R.id.tv_announcement_notice_group_info).view();
                    link = null;
                }
                //修改
            } else {
                //主动获取
                if (iAnnouncementModel.getSGroup() != null && !TextUtils.isEmpty(iAnnouncementModel.getSGroup().content)) {
                    showDownUI(true);
                    $.id(R.id.tv_announcement_notice_group_info).text(iAnnouncementModel.getSGroup().content);
                    view = $.id(R.id.tv_announcement_notice_group_info).view();
                    link = iAnnouncementModel.getSGroup().link;
                } else {
                    showDownUI(false);
                    $.id(R.id.tv_announcement_notice_group_info).text(getResources().getString(R.string.string_notice_group_none));
                    view = $.id(R.id.tv_announcement_notice_group_info).view();
                    link = null;
                }
            }
        }
        setUrl(view, link);
    }

    /**
     * 是否显示 底部分组公告
     */
    private void showDownUI(boolean isShow) {

        if(isShow) {
            $.id(R.id.tv_announcement_double_group_title).visibility(View.VISIBLE);
            $.id(R.id.tv_announcement_notice_group_info).visibility(View.VISIBLE);
            $.id(R.id.tv_double_ann_info).visibility(View.INVISIBLE);
            $.id(R.id.iv_double_ann_down).visibility(View.INVISIBLE);
        } else {
            $.id(R.id.tv_announcement_double_group_title).visibility(View.INVISIBLE);
            $.id(R.id.tv_announcement_notice_group_info).visibility(View.INVISIBLE);

            if (isTeacher) {
                $.id(R.id.tv_double_ann_info).visibility(View.VISIBLE);
                $.id(R.id.iv_double_ann_down).visibility(View.VISIBLE);
            } else {
                $.id(R.id.tv_double_ann_info).visibility(View.INVISIBLE);
                $.id(R.id.iv_double_ann_down).visibility(View.INVISIBLE);
            }
        }
    }

    private void setUrl(View view, String url) {
        if (view == null)
            return;

        if (TextUtils.isEmpty(url)) {
            view.setOnClickListener(null);
        } else {
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent();
                    intent.setAction("android.intent.action.VIEW");
                    Uri uri = Uri.parse(url);
                    intent.setData(uri);
                    startActivity(intent);
                }
            });
        }
    }
}
