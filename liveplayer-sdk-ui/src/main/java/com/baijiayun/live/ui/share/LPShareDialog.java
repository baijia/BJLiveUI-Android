package com.baijiayun.live.ui.share;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.baijiahulian.common.utils.DisplayUtils;
import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseDialogFragment;
import com.baijiayun.live.ui.utils.LPShareModel;
import com.baijiayun.livecore.utils.LPRecyclerItemClickSupport;

import java.util.ArrayList;

/**
 * Created by Shubo on 16/6/15.
 */
public class LPShareDialog extends BaseDialogFragment {

    private static final String SHARE_CHANNELS = "SHARE_CHANNELS";
    private LPShareClickListener listener;
    private ArrayList<LPShareModel> shareChannels;
    private RecyclerView recyclerView;

    public static LPShareDialog newInstance(ArrayList<? extends LPShareModel> shareChannels) {
        LPShareDialog f = new LPShareDialog();
        Bundle args = new Bundle();
        args.putSerializable(SHARE_CHANNELS, shareChannels);
        f.setArguments(args);
        return f;
    }

    @Override
    protected void setWindowParams(WindowManager.LayoutParams windowParams) {
        windowParams.gravity = Gravity.RIGHT | Gravity.TOP;
        windowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
        windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        windowParams.y = DisplayUtils.dip2px(getContext(), 32) + DisplayUtils.getStatusBarHeight(getActivity());
        windowParams.windowAnimations = R.style.ShareDialogAnim;
    }


    @Override
    protected int getLayoutId() {
        return R.layout.dialog_share_room;
    }

    @Override
    protected void init(Bundle savedInstanceState, Bundle arguments) {
        super.hideBackground().contentBackgroundColor(ContextCompat.getColor(getContext(), R.color.live_transparent));
        shareChannels = (ArrayList) arguments.getSerializable(SHARE_CHANNELS);
        recyclerView = (RecyclerView) contentView.findViewById(R.id.lp_dialog_share_recycler);
        int GRID_SPAN_COUNT = 3;

//        if (getLPSdkContext().getCurrentUser().type == LPConstants.LPUserType.Student ||
//                getLPSdkContext().getCurrentUser().type == LPConstants.LPUserType.Visitor) {
//            GRID_SPAN_COUNT = 4;
//        } else {
//            GRID_SPAN_COUNT = 3;
//        }
        GridLayoutManager gridLayoutManager = new GridLayoutManager(getActivity(), GRID_SPAN_COUNT);
        recyclerView.setLayoutManager(gridLayoutManager);
        recyclerView.setAdapter(new LPShareAdapter());
        LPRecyclerItemClickSupport.addTo(recyclerView).setOnItemClickListener(new LPRecyclerItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                if (listener != null)
                    listener.onShareClick(shareChannels.get(position).getShareType());
                dismissAllowingStateLoss();
            }
        });
    }

    private class LPShareAdapter extends RecyclerView.Adapter<LPShareViewHolder> {

        @Override
        public LPShareViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_share, parent, false);
            return new LPShareViewHolder(view);
        }

        @Override
        public void onBindViewHolder(LPShareViewHolder holder, int position) {
            holder.ivShareIcon.setImageResource(shareChannels.get(position).getShareIconRes());
            holder.tvShareTitle.setText(shareChannels.get(position).getShareIconText());
            if (shareChannels.get(position).hasCorner()) {
                holder.tvShareCorner.setVisibility(View.VISIBLE);
                holder.tvShareCorner.setText(shareChannels.get(position).getCornerText());
            } else {
                holder.tvShareCorner.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return shareChannels.size();
        }
    }


    private class LPShareViewHolder extends RecyclerView.ViewHolder {

        private ImageView ivShareIcon;
        private TextView tvShareTitle;
        private TextView tvShareCorner;

        public LPShareViewHolder(View itemView) {
            super(itemView);
            ivShareIcon = (ImageView) itemView.findViewById(R.id.lp_item_share_icon);
            tvShareTitle = (TextView) itemView.findViewById(R.id.lp_item_share_title);
            tvShareCorner = (TextView) itemView.findViewById(R.id.lp_item_share_corner);
        }
    }

    public void setListener(LPShareClickListener listener) {
        this.listener = listener;
    }

    public interface LPShareClickListener {
        void onShareClick(int type);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (recyclerView != null)
            LPRecyclerItemClickSupport.removeFrom(recyclerView);
    }
}
