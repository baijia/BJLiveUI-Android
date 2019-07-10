package com.baijiayun.live.ui.chat.privatechat;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.activity.LiveRoomRouterListener;
import com.baijiayun.live.ui.base.BaseFragment;
import com.baijiayun.live.ui.utils.AliCloudImageUtil;
import com.baijiayun.live.ui.utils.LinearLayoutWrapManager;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.models.imodels.IUserModel;
import com.squareup.picasso.Picasso;

/**
 * Created by yangjingming on 2018/1/16.
 */

public class ChatUsersDialogFragment extends BaseFragment implements ChatUsersContract.View{
    private RelativeLayout privateChatLabel;
    private RecyclerView recyclerView;
    private ChatUserAdapter adapter;
    private ChatUsersContract.Presenter presenter;
    private int visibleThreshold = 5;
    private int lastVisibleItem, totalItemCount;
    private IUserModel privateChatToUser;

    @Override
    public int getLayoutId() {
        return R.layout.dialog_chat_users;
    }

    @Override
    public void setPresenter(ChatUsersContract.Presenter presenter) {
        super.setBasePresenter(presenter);
    }

    @Override
    public void initPrivateChatLabel(IUserModel iUserModel){
        privateChatToUser = iUserModel;
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
    protected void init(Bundle savedInstanceState) {
        super.init(savedInstanceState);
        presenter = new ChatUsersPresenter(this);
        presenter.setRouter((LiveRoomRouterListener) getActivity());
        presenter.loadMore();
        setPresenter(presenter);
        recyclerView = (RecyclerView) $.id(R.id.dialog_chat_user_recycler_view).view();
        privateChatLabel = (RelativeLayout) $.id(R.id.dialog_private_chat_container).view();

        recyclerView.setLayoutManager(new LinearLayoutWrapManager(getActivity()));
        adapter = new ChatUserAdapter();
        recyclerView.setAdapter(adapter);
        if(privateChatToUser != null) showPrivateChatLabel(privateChatToUser.getName());
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState){
                final LinearLayoutManager linearLayoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                totalItemCount = linearLayoutManager.getItemCount();
                lastVisibleItem = linearLayoutManager.findLastVisibleItemPosition();
                if (!presenter.isLoading() && totalItemCount <= (lastVisibleItem + visibleThreshold)) {
                    presenter.loadMore();
                }
            }
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
            }
        });
//        initFragment();
    }

    @Override
    public void privateChatUserChanged(boolean isEmpty) {
        if (recyclerView == null) return;
        if (isEmpty) {
            recyclerView.setVisibility(View.GONE);
            LinearLayout noPrivateChatUser = (LinearLayout) $.id(R.id.no_online_chat).view();
            noPrivateChatUser.setVisibility(View.VISIBLE);
        } else {
            if (recyclerView.getVisibility() ==  View.VISIBLE) return;
            LinearLayout noPrivateChatUser = (LinearLayout) $.id(R.id.no_online_chat).view();
            noPrivateChatUser.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void showPrivateChatLabel(String chatName) {
        if (chatName != null) {
            privateChatLabel.setVisibility(View.VISIBLE);
            ((TextView) $.id(R.id.dialog_private_chat_status_content).view()).setText(getString(R.string.live_room_private_chat_with_name, chatName));
            ($.id(R.id.end_private_chat_btn).view()).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    privateChatLabel.setVisibility(View.GONE);
                    presenter.setPrivateChatUser(null);
                    adapter.notifyDataSetChanged();
                    Toast.makeText(getContext(),"私聊已取消",Toast.LENGTH_SHORT).show();
                }
            });
        }else{
            privateChatLabel.setVisibility(View.GONE);
        }
    }

    private static class LoadingViewHolder extends RecyclerView.ViewHolder {
        ProgressBar progressBar;

        private LoadingViewHolder(View itemView) {
            super(itemView);
            progressBar = (ProgressBar) itemView.findViewById(R.id.item_chat_user_progress);
        }
    }

    private static class ChatUserViewHolder extends RecyclerView.ViewHolder {
        TextView name, teacherTag, assistantTag;
        ImageView avatar;
        LinearLayout privateChatUser;

        ChatUserViewHolder(View itemView) {
            super(itemView);
            name = (TextView) itemView.findViewById(R.id.item_chat_user_name);
            avatar = (ImageView) itemView.findViewById(R.id.item_chat_user_avatar);
            teacherTag = (TextView) itemView.findViewById(R.id.item_chat_user_teacher_tag);
            assistantTag = (TextView) itemView.findViewById(R.id.item_chat_user_assist_tag);
            privateChatUser = (LinearLayout) itemView.findViewById(R.id.item_private_chat_user);
        }
    }

    private class ChatUserAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int VIEW_TYPE_USER = 0;
        private static final int VIEW_TYPE_LOADING = 1;

        @Override
        public int getItemViewType(int position) {
            return presenter.getUser(position) == null ? VIEW_TYPE_LOADING : VIEW_TYPE_USER;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == VIEW_TYPE_USER) {
                View view = LayoutInflater.from(getActivity()).inflate(R.layout.item_chat_user, parent, false);
                return new ChatUserViewHolder(view);
            } else if (viewType == VIEW_TYPE_LOADING) {
                View view = LayoutInflater.from(getActivity()).inflate(R.layout.item_chat_user_loadmore, parent, false);
                return new LoadingViewHolder(view);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof ChatUserViewHolder) {
                String teacherLabel = presenter.getTeacherLabel();
                String assistantLabel = presenter.getAssistantLabel();
                final IUserModel userModel = presenter.getUser(position);
                final ChatUserViewHolder userViewHolder = (ChatUserViewHolder) holder;

                if (presenter.getPrivateChatUser() != null && userModel.getUserId().equals(presenter.getPrivateChatUser().getUserId())) {
                    userViewHolder.privateChatUser.setBackgroundColor(getResources().getColor(R.color.live_private_chat_bg));
                } else {
                    userViewHolder.privateChatUser.setBackgroundColor(getResources().getColor(R.color.live_white));
                }

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
                String avatar = userModel.getAvatar().startsWith("//") ? "https:" + userModel.getAvatar() : userModel.getAvatar();
                Picasso.with(getContext()).load(AliCloudImageUtil.getRoundedAvatarUrl(avatar, 64)).into(userViewHolder.avatar);
                userViewHolder.privateChatUser.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        userViewHolder.privateChatUser.setBackgroundColor(getResources().getColor(R.color.live_private_chat_bg));
                        presenter.setPrivateChatUser(userModel);
                        presenter.chooseOneToChat(userModel.getName(), true);
                    }
                });
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
        presenter = null;
        if (recyclerView != null)
            recyclerView.setAdapter(null);
        recyclerView = null;
    }
}
