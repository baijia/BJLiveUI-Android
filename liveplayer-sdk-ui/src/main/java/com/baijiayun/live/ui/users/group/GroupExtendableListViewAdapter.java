package com.baijiayun.live.ui.users.group;

import android.graphics.Color;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.afollestad.materialdialogs.color.CircleView;
import com.baijiayun.glide.Glide;
import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.viewsupport.CircleTextView;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.models.LPGroupItem;
import com.baijiayun.livecore.models.LPUserModel;
import com.baijiayun.livecore.models.roomresponse.LPResRoomGroupInfoModel;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * 分组显示Adapter
 * 20190712
 * panzq
 */
public class GroupExtendableListViewAdapter extends BaseExpandableListAdapter {

    private List<LPGroupItem> lpGroupItems;
    private OnUpdateListener mOnUpdateListener;
    private String mAssistantLabel;
    private int mGroupId = -1;

    public GroupExtendableListViewAdapter(String assistantLabel, int groupId){
        this.mAssistantLabel = assistantLabel;
        this.mGroupId = groupId;
    }

    private String []color = {
            "#FF607D",
            "#BE21E9",
            "#35CD3B",
            "#EE87FF",
            "#775FCF",
            "#AB8678",
            "#3D5AFE",
            "#03A9F4",
            "#C3FFA6",
            "#F44336",
            "#7ED321",
            "#FB9D3E",
            "#795548",
            "#FFEB3B",
            "#FFAE8E",
            "#E91E63"
    };

    @Override
    public int getGroupCount() {
        if (lpGroupItems == null)
            return 0;
        return lpGroupItems.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        LPGroupItem group = (LPGroupItem) getGroup(groupPosition);
        if (group == null) {
            return 0;
        }
        return group.userModelList.size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        if (lpGroupItems == null || groupPosition >= lpGroupItems.size())
            return null;
        return lpGroupItems.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {

        LPGroupItem group = (LPGroupItem) getGroup(groupPosition);
        if (group == null) {
            return null;
        }
        if (group.userModelList.size() > 0 && group.userModelList.size() <= childPosition) {
            return null;
        }
        return group.userModelList.get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return 0;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return 0;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, ViewGroup parent) {
        GroupHolder groupHolder;
        if (convertView == null) {
            groupHolder = new GroupHolder();
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_online_group_partent, parent, false);
            groupHolder.mIvItemOnlinePartentState = convertView.findViewById(R.id.iv_item_online_partent_state);
            groupHolder.mTvItemOnlinePartentTitle = convertView.findViewById(R.id.tv_item_online_partent_title);
            groupHolder.mTvItemOnlinePartentNumber = convertView.findViewById(R.id.tv_item_online_partent_number);
            groupHolder.mCtvItemOnlinePartent = convertView.findViewById(R.id.ctv_item_online_partent);

            convertView.setTag(groupHolder);
        } else {
            groupHolder = (GroupHolder) convertView.getTag();
        }

        LPGroupItem item = (LPGroupItem) getGroup(groupPosition);
        LPResRoomGroupInfoModel.GroupItem groupItem = item.groupItemModel;
        groupHolder.mTvItemOnlinePartentTitle.setText("" + (groupItem != null && !TextUtils.isEmpty(groupItem.name) ? groupItem.name : "分组" + ++groupPosition));
        groupHolder.mCtvItemOnlinePartent.setCircleBackgroundColor(Color.parseColor(color[(groupItem != null ? groupItem.id : 1 - 1) % 16]));
        groupHolder.mTvItemOnlinePartentNumber.setText("" + item.count);

        if (isExpanded) {
            groupHolder.mIvItemOnlinePartentState.setImageDrawable(parent.getContext().getResources().getDrawable(R.drawable.iv_lp_ui_down));
        } else {
            groupHolder.mIvItemOnlinePartentState.setImageDrawable(parent.getContext().getResources().getDrawable(R.drawable.iv_lp_ui_group_close));
        }

        if (groupItem != null && groupItem.id == mGroupId) {
            groupHolder.mCtvItemOnlinePartent.setText("√");
        } else {
            groupHolder.mCtvItemOnlinePartent.setText("");
        }

        return convertView;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        GroupChildHolder groupChildHolder;
        if (convertView == null) {
            groupChildHolder = new GroupChildHolder();
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_online_user, parent, false);
            groupChildHolder.mItemOnlineUserAvatar = convertView.findViewById(R.id.item_online_user_avatar);
            groupChildHolder.mItemOnlineUserName = convertView.findViewById(R.id.item_online_user_name);
            groupChildHolder.mItemOnlineUserTeacherTag = convertView.findViewById(R.id.item_online_user_teacher_tag);
            groupChildHolder.mItemOnlineUserAssistTag = convertView.findViewById(R.id.item_online_user_assist_tag);
            groupChildHolder.mItemOlineUserPresenterTag = convertView.findViewById(R.id.item_online_user_presenter_tag);
            convertView.setTag(groupChildHolder);
        } else {
            groupChildHolder = (GroupChildHolder) convertView.getTag();
        }

        LPUserModel userModel = (LPUserModel) getChild(groupPosition, childPosition);

        String avatar = userModel.getAvatar().startsWith("//") ? "https:" + userModel.getAvatar() : userModel.getAvatar();
        if(!TextUtils.isEmpty(avatar))
            Glide.with(parent.getContext())
                    .load(avatar)
                    .into(groupChildHolder.mItemOnlineUserAvatar);
//            Picasso.with(parent.getContext())
//                    .load(AliCloudImageUtil.getRoundedAvatarUrl(avatar, 64))
//                    .centerCrop()
//                    .resize(DisplayUtils.dip2px(parent.getContext(), 32), DisplayUtils.dip2px(parent.getContext(), 32))
//                    .into(groupChildHolder.mItemOnlineUserAvatar);

        groupChildHolder.mItemOnlineUserName.setText("" + userModel.name);

        if (userModel.getType() == LPConstants.LPUserType.Assistant) {
            groupChildHolder.mItemOnlineUserAssistTag.setVisibility(View.VISIBLE);
            groupChildHolder.mItemOnlineUserAssistTag.setText(TextUtils.isEmpty(mAssistantLabel)
                    ? parent.getContext().getResources().getString(R.string.live_assistent) : mAssistantLabel);
        } else {
            groupChildHolder.mItemOnlineUserAssistTag.setVisibility(View.GONE);
        }


        groupChildHolder.mItemOlineUserPresenterTag.setVisibility(View.INVISIBLE);
        groupChildHolder.mItemOnlineUserTeacherTag.setVisibility(View.INVISIBLE);

        if (childPosition == getChildrenCount(groupPosition) -1) {
            //请求更新
            if (mOnUpdateListener != null)
                mOnUpdateListener.onUpdate(userModel.groupId);
        }

        return convertView;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

    public void setDate(List<LPGroupItem> lpGroupItems) {
        this.lpGroupItems = lpGroupItems;
    }

    class GroupHolder {
        ImageView mIvItemOnlinePartentState;
        TextView mTvItemOnlinePartentTitle;
        TextView mTvItemOnlinePartentNumber;
        CircleTextView mCtvItemOnlinePartent;
    }

    class GroupChildHolder {
        CircleImageView mItemOnlineUserAvatar;
        TextView mItemOnlineUserName;
        TextView mItemOnlineUserTeacherTag;
        TextView mItemOnlineUserAssistTag;
        TextView mItemOlineUserPresenterTag;
    }

    public void setOnUpdateListener(OnUpdateListener listener) {
        this.mOnUpdateListener = listener;
    }

    public interface OnUpdateListener {

        void onUpdate(int groupId);
    }
}
