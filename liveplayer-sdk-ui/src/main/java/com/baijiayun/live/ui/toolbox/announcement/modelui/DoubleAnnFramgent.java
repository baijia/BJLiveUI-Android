package com.baijiayun.live.ui.toolbox.announcement.modelui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.TextView;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.toolbox.announcement.AnnouncementContract;
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
    public void setType(int type, int groupId) {

        if (type == AnnouncementContract.TYPE_UI_TEACHERORASSISTANT) {
            //大班老师
            $.id(R.id.tv_announcement_double_group_title).visibility(View.INVISIBLE);
            $.id(R.id.tv_announcement_notice_group_info).visibility(View.INVISIBLE);
            $.id(R.id.tv_double_ann_info).visibility(View.INVISIBLE);
            $.id(R.id.iv_double_ann_down).visibility(View.INVISIBLE);
        } else if (type == AnnouncementContract.TYPE_UI_GROUPTEACHERORASSISTANT) {
            //分组老师
            isTeacher = true;
            showDownUI(false);
        } else {
            //学生
            isTeacher = false;
            showDownUI(false);
        }

        if (groupId == 0) {
            //未分组隐藏通知显示
            $.id(R.id.ll_announcement_double_group).visibility(View.GONE);
        }
    }

    @Override
    public void setNoticeInfo(IAnnouncementModel iAnnouncementModel) {

        if ($ == null) {
            mIAnnouncementModel = iAnnouncementModel;
            return;
        }

        View view;
        View linkTipsView;
        String link;
        if ("0".equals(iAnnouncementModel.getGroup()) || TextUtils.isEmpty(iAnnouncementModel.getGroup())) {
            //大班老师公告
            view = $.id(R.id.tv_announcement_notice_info).view();
            linkTipsView = $.id(R.id.tv_announcement_notice_info_tips).view();
            if (!TextUtils.isEmpty(iAnnouncementModel.getContent())) {
                $.id(R.id.tv_announcement_notice_info).text(iAnnouncementModel.getContent());
                link = iAnnouncementModel.getLink();
            } else {
                $.id(R.id.tv_announcement_notice_info).text(getResources().getString(R.string.live_announcement_none));
                link = null;
            }
        } else {
            //分组老师通知
            view = $.id(R.id.tv_announcement_notice_group_info).view();
            linkTipsView = $.id(R.id.tv_announcement_double_info_tips).view();
            if ("notice_change".equals(iAnnouncementModel.getMessageType())) {
                if (!TextUtils.isEmpty(iAnnouncementModel.getContent())) {
                    $.id(R.id.tv_announcement_notice_group_info).text(iAnnouncementModel.getContent());
                    link = iAnnouncementModel.getLink();
                } else {
                    $.id(R.id.tv_announcement_notice_group_info).text(getResources().getString(R.string.string_notice_group_none));
                    link = null;
                }
                //修改
            } else {
                //主动获取
                if (iAnnouncementModel.getSGroup() != null && !TextUtils.isEmpty(iAnnouncementModel.getSGroup().content)) {
                    $.id(R.id.tv_announcement_notice_group_info).text(iAnnouncementModel.getSGroup().content);
                    link = iAnnouncementModel.getSGroup().link;
                } else {
                    $.id(R.id.tv_announcement_notice_group_info).text(getResources().getString(R.string.string_notice_group_none));
                    link = null;
                }
            }
        }
        setTipsLinkViewInfo(linkTipsView, link);
        setUrl(view, link);
    }

    /**
     * 设置链接提示
     */
    private void setTipsLinkViewInfo(View view, String link) {
        if (view == null)
            return;
        if (TextUtils.isEmpty(link)) {
            ((TextView)view).setText("");
        } else {
            ((TextView)view).setText(getResources().getString(R.string.string_notice_link_tips));
        }
    }

    /**
     * 是否显示 底部分组公告
     */
    private void showDownUI(boolean isShow) {

        if(isShow) {
            $.id(R.id.tv_double_ann_info).visibility(View.INVISIBLE);
            $.id(R.id.iv_double_ann_down).visibility(View.INVISIBLE);
        } else {

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
