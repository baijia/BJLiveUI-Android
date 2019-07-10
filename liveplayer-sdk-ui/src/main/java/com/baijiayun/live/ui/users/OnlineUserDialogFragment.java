package com.baijiayun.live.ui.users;

import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseDialogFragment;
import com.baijiayun.live.ui.utils.AliCloudImageUtil;
import com.baijiayun.live.ui.utils.LinearLayoutWrapManager;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.models.imodels.IUserModel;
import com.squareup.picasso.Picasso;

/**
 * Created by Shubo on 2017/4/5.
 */

public class OnlineUserDialogFragment extends BaseDialogFragment implements OnlineUserContract.View {

    private OnlineUserContract.Presenter presenter;
    private RecyclerView recyclerView;
    private OnlineUserAdapter adapter;


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
        recyclerView = (RecyclerView) contentView.findViewById(R.id.dialog_online_user_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutWrapManager(getActivity()));
        adapter = new OnlineUserAdapter();
        recyclerView.setAdapter(adapter);
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
                        presenter.loadMore();
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
                    Picasso.with(getContext()).load(AliCloudImageUtil.getRoundedAvatarUrl(avatar, 64)).into(userViewHolder.avatar);
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
    public void onDestroy(){
        super.onDestroy();
        if (recyclerView != null){
            recyclerView.setAdapter(null);
            recyclerView = null;
        }
        presenter = null;
    }

}
