package com.baijiayun.live.ui.users;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.baijiayun.glide.Glide;
import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseDialogFragment;
import com.baijiayun.live.ui.users.group.GroupExtendableListViewAdapter;
import com.baijiayun.live.ui.utils.AliCloudImageUtil;
import com.baijiayun.live.ui.utils.LinearLayoutWrapManager;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.models.LPGroupItem;
import com.baijiayun.livecore.models.imodels.IUserModel;
import com.baijiayun.livecore.models.roomresponse.LPResRoomGroupInfoModel;
import com.baijiayun.livecore.utils.DisplayUtils;
import com.baijiayun.livecore.utils.LPLogger;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.Disposable;

/**
 * Created by Shubo on 2017/4/5.
 */

public class OnlineUserDialogFragment extends BaseDialogFragment implements OnlineUserContract.View {

    private OnlineUserContract.Presenter presenter;
    private RecyclerView recyclerView;
    private OnlineUserAdapter adapter;

    private TextView mTvOnlineGroupTitle;
    private ExpandableListView mElvOnlineGroup;
    private GroupExtendableListViewAdapter mGroupAdapter;

    public static OnlineUserDialogFragment newInstance() {
        OnlineUserDialogFragment instance = new OnlineUserDialogFragment();
        return instance;
    }

    @Override
    public void setPresenter(OnlineUserContract.Presenter presenter) {
        super.setBasePresenter(presenter);
        this.presenter = presenter;
    }

    @Override
    public void notifyDataChanged() {
        adapter.notifyDataSetChanged();
    }

    @Override
    public void notifyNoMoreData() {
        adapter.notifyDataSetChanged();
    }

    @Override
    public void notifyUserCountChange(int count) {
        super.title(getString(R.string.live_on_line_user_count_dialog, count));
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_online_users;
    }

    @Override
    protected void init(Bundle savedInstanceState, Bundle arguments) {
        super.editable(false);

//        AbsListView.LayoutParams params = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);

        recyclerView = new RecyclerView(getContext());
        LinearLayout.LayoutParams rcvParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);

//        mTvOnlineGroupTitle = contentView.findViewById(R.id.tv_online_group_title);
        mTvOnlineGroupTitle = new TextView(getContext());
        mTvOnlineGroupTitle.setTextSize(16);
        mTvOnlineGroupTitle.setHeight(DisplayUtils.dip2px(getContext(), 30));

        mElvOnlineGroup = contentView.findViewById(R.id.elv_online_group);

        recyclerView.setHasFixedSize(true);

        LinearLayout layoutView = new LinearLayout(getContext());
        layoutView.setOrientation(LinearLayout.VERTICAL);
        layoutView.addView(recyclerView, rcvParams);
        layoutView.addView(mTvOnlineGroupTitle);

        mElvOnlineGroup.addHeaderView(layoutView);
        mTvOnlineGroupTitle.setVisibility(View.GONE);

        recyclerView.setLayoutManager(new LinearLayoutWrapManager(getActivity()));
        adapter = new OnlineUserAdapter();
        recyclerView.setAdapter(adapter);

        mGroupAdapter = new GroupExtendableListViewAdapter(presenter.getAssistantLabel(), presenter.getGroupId());
        mElvOnlineGroup.setAdapter(mGroupAdapter);
        mGroupAdapter.setOnUpdateListener(new GroupExtendableListViewAdapter.OnUpdateListener() {
            @Override
            public void onUpdate(int groupId) {
                presenter.loadMore(groupId);
            }
        });

        mElvOnlineGroup.setOnGroupExpandListener(new ExpandableListView.OnGroupExpandListener() {

            @Override
            public void onGroupExpand(int groupPosition) {

                if(mGroupAdapter == null)
                    return;
                for (int i = 0, count = mGroupAdapter.getGroupCount(); i < count; i++) {
                    if (i != groupPosition) {
                        mElvOnlineGroup.collapseGroup(i);
                    }
                }

                LPGroupItem item = (LPGroupItem) mGroupAdapter.getGroup(groupPosition);
                presenter.updateGroupInfo(item);
            }
        });
    }

    private static class LoadingViewHolder extends RecyclerView.ViewHolder {
        ProgressBar progressBar;

        LoadingViewHolder(View itemView) {
            super(itemView);
            progressBar = (ProgressBar) itemView.findViewById(R.id.item_online_user_progress);
        }
    }

    private static class OnlineUserViewHolder extends RecyclerView.ViewHolder {
        TextView name, teacherTag, assistantTag, presenterTag;
        ImageView avatar;

        OnlineUserViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.item_online_user_name);
            avatar = (ImageView) itemView.findViewById(R.id.item_online_user_avatar);
            teacherTag = (TextView) itemView.findViewById(R.id.item_online_user_teacher_tag);
            assistantTag = (TextView) itemView.findViewById(R.id.item_online_user_assist_tag);
            presenterTag = (TextView) itemView.findViewById(R.id.item_online_user_presenter_tag);
        }
    }

    private class OnlineUserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int VIEW_TYPE_USER = 0;
        private static final int VIEW_TYPE_LOADING = 1;

        private int visibleThreshold = 5;
        private int lastVisibleItem, totalItemCount;

        OnlineUserAdapter() {
            recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView1, int dx, int dy) {
                    super.onScrolled(recyclerView1, dx, dy);
                    if (recyclerView == null) return;
                    final LinearLayoutWrapManager linearLayoutManager = (LinearLayoutWrapManager) recyclerView1.getLayoutManager();
                    totalItemCount = linearLayoutManager.getItemCount();
                    lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();

                    if (!presenter.isLoading() && totalItemCount <= (lastVisibleItem + visibleThreshold)) {

                        if (presenter.isGroup()) {
                            presenter.loadMore(0);
                        } else {
                            presenter.loadMore(-1);
                        }
                    }
                }
            });
        }

        @Override
        public int getItemViewType(int position) {
            return presenter.getUser(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_USER;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_USER) {
                View view = LayoutInflater.from(getActivity()).inflate(R.layout.item_online_user, parent, false);
                return new OnlineUserViewHolder(view);
            } else if (viewType == VIEW_TYPE_LOADING) {
                View view = LayoutInflater.from(getActivity()).inflate(R.layout.item_online_user_loadmore, parent, false);
                return new LoadingViewHolder(view);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, final int position) {
            if (holder instanceof OnlineUserViewHolder) {
                String teacherLabel = presenter.getTeacherLabel();
                String assistantLabel = presenter.getAssistantLabel();
                IUserModel userModel = presenter.getUser(position);

                final OnlineUserViewHolder userViewHolder = (OnlineUserViewHolder) holder;
                userViewHolder.name.setText(userModel.getName());
                if (userModel.getType() == LPConstants.LPUserType.Teacher) {
                    userViewHolder.teacherTag.setVisibility(View.VISIBLE);
                    userViewHolder.teacherTag.setText(TextUtils.isEmpty(teacherLabel) ? getString(R.string.live_teacher) : teacherLabel);
                } else {
                    userViewHolder.teacherTag.setVisibility(View.GONE);
                }
                if (userModel.getType() == LPConstants.LPUserType.Assistant) {
                    userViewHolder.assistantTag.setVisibility(View.VISIBLE);
                    userViewHolder.assistantTag.setText(TextUtils.isEmpty(assistantLabel) ? getString(R.string.live_assistent) : assistantLabel);
                } else {
                    userViewHolder.assistantTag.setVisibility(View.GONE);
                }
                if (userModel.getType() == LPConstants.LPUserType.Assistant &&
                        userModel.getUserId() != null && userModel.getUserId().equals(presenter.getPresenter()))
                    userViewHolder.presenterTag.setVisibility(View.VISIBLE);
                else
                    userViewHolder.presenterTag.setVisibility(View.GONE);
                String avatar = userModel.getAvatar().startsWith("//") ? "https:" + userModel.getAvatar() : userModel.getAvatar();
                if(!TextUtils.isEmpty(avatar))
                    Glide.with(getContext())
                        .load(avatar)
                        .into(userViewHolder.avatar);

//                    Picasso.with(getContext())
//                            .load(AliCloudImageUtil.getRoundedAvatarUrl(avatar, 64))
//                            .centerInside()
//                            .resize(32, 32)
//                            .into(userViewHolder.avatar);
            } else if (holder instanceof LoadingViewHolder) {
                LoadingViewHolder loadingViewHolder = (LoadingViewHolder) holder;
                loadingViewHolder.progressBar.setIndeterminate(true);
            }
        }

        @Override
        public int getItemCount() {
            return presenter.getCount();
        }
    }

    @Override
    public void notifyGroupData(List<LPGroupItem> lpGroupItems) {
        showGroupView(lpGroupItems.size() > 0);
        mTvOnlineGroupTitle.setText(getResources().getString(R.string.string_group) + "(" + lpGroupItems.size() + ")");

        mGroupAdapter.setDate(lpGroupItems);
        mGroupAdapter.notifyDataSetChanged();
    }

    @Override
    public void showGroupView(boolean isShow) {
        if (isShow) {
//            mElvOnlineGroup.setVisibility(View.VISIBLE);
            mTvOnlineGroupTitle.setVisibility(View.VISIBLE);
        } else {
//            mElvOnlineGroup.setVisibility(View.GONE);
            mTvOnlineGroupTitle.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        if (recyclerView != null){
            recyclerView.setAdapter(null);
            recyclerView = null;
        }
        presenter = null;
    }

}
