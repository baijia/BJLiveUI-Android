package com.baijiayun.live.ui.ppt.quickswitchppt;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.baijiayun.glide.Glide;
import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseDialogFragment;
import com.baijiayun.live.ui.utils.QueryPlus;
import com.baijiayun.livecore.ppt.util.AliCloudImageUtil;
import com.baijiayun.livecore.ppt.util.ShapeContent;
import com.baijiayun.livecore.utils.DisplayUtils;
import com.baijiayun.livecore.viewmodels.impl.LPDocListViewModel;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by szw on 17/7/4.
 */

public class QuickSwitchPPTFragment extends BaseDialogFragment implements SwitchPPTContract.View {
    private SwitchPPTContract.Presenter presenter;
    private QueryPlus $;

    private List<LPDocListViewModel.DocModel> mBoardList = new ArrayList<>();
    private List<LPDocListViewModel.DocModel> mPPTList = new ArrayList<>();

    private boolean isOpen = false;
    //白板
    private QuickSwitchPPTAdapter adapter;
    //PPT
    private QuickSwitchPPTAdapter mPPTAdapter;

    private boolean isStudent = false;
    private boolean enableMultiWhiteboard = false;
    private int maxIndex;//学生可以快速滑动ppt的最大页数
    private int currentIndex, lastIndex;

    private boolean isChangePage = false;

    public static QuickSwitchPPTFragment newInstance(Bundle args) {
        QuickSwitchPPTFragment quickSwitchPPTFragment = new QuickSwitchPPTFragment();
        quickSwitchPPTFragment.setArguments(args);
        return quickSwitchPPTFragment;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.bjy_dialog_ppt_switch;
    }

    @Override
    protected void init(Bundle savedInstanceState, Bundle arguments) {
        super.hideTitleBar();
        $ = QueryPlus.with(contentView);

        initView();

        //白板
        LinearLayoutManager manager = new LinearLayoutManager(getActivity());
        manager.setOrientation(LinearLayoutManager.HORIZONTAL);
        adapter = new QuickSwitchPPTAdapter(mBoardList);
        ((RecyclerView) $.id(R.id.dialog_switch_ppt_rv).view()).setLayoutManager(manager);
        ((RecyclerView) $.id(R.id.dialog_switch_ppt_rv).view()).setAdapter(adapter);

        //PPT
        LinearLayoutManager managerPPT = new LinearLayoutManager(getActivity());
        managerPPT.setOrientation(LinearLayoutManager.HORIZONTAL);
        mPPTAdapter = new QuickSwitchPPTAdapter(mPPTList);
        ((RecyclerView) $.id(R.id.dialog_switch_ppt).view()).setLayoutManager(managerPPT);
        ((RecyclerView) $.id(R.id.dialog_switch_ppt).view()).setAdapter(mPPTAdapter);

        this.maxIndex = getArguments().getInt("maxIndex");
        lastIndex = currentIndex = getArguments().getInt("currentIndex");
    }

    private void initView() {

        $.id(R.id.iv_ppt_switch).clicked(v -> switchPPTState());

        $.id(R.id.iv_board_add).clicked(v -> {
            if (!presenter.canOperateDocumentControl()) {
                showToast(getString(R.string.live_room_document_control_permission_forbid));
                return;
            }
            isChangePage = true;
            //添加白板
            presenter.addPage();
        });

        $.id(R.id.tv_ppt_ok).clicked(v -> {
            if (!presenter.canOperateDocumentControl()) {
                showToast(getString(R.string.live_room_document_control_permission_forbid));
                return;
            }
            switchEditStatus(false);
        });
    }

    private void switchPPTState() {
        RelativeLayout.LayoutParams params;
        Drawable drawable;
        if (isOpen) {
            params = new RelativeLayout.LayoutParams(DisplayUtils.dip2px(getContext(), 58), DisplayUtils.dip2px(getContext(), 76));
            params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, R.id.rl_ppt_switch);
            drawable = getResources().getDrawable(R.drawable.iv_back_left);
        } else {
            params =
                    new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, DisplayUtils.dip2px(getContext(), 76));
            params.leftMargin = DisplayUtils.dip2px(getContext(), 80);
            drawable = getResources().getDrawable(R.drawable.iv_back_right);
        }
        isOpen = !isOpen;
        $.id(R.id.ll_ppt).view().setLayoutParams(params);
        $.id(R.id.iv_ppt_switch).backgroundDrawable(drawable);
    }

    @Override
    protected void setWindowParams(WindowManager.LayoutParams windowParams) {
        windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        windowParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        windowParams.gravity = Gravity.BOTTOM | GravityCompat.END;
        windowParams.windowAnimations = R.style.LiveBaseSendMsgDialogAnim;
    }

    @Override
    public void setPresenter(SwitchPPTContract.Presenter presenter) {
        this.presenter = presenter;
        super.setBasePresenter(presenter);
    }

    @Override
    public void setIndex(int index) {
        lastIndex = currentIndex;
        if(index >= 0 && currentIndex != index) {
            currentIndex = index;
        }
        if (index >= 0 && maxIndex != index) {
            maxIndex = index;
            presenter.changeDocList();
        }
        if (currentIndex < mBoardList.size()) {
            //当前页面在白板页面
            ((RecyclerView) $.id(R.id.dialog_switch_ppt_rv).view()).scrollToPosition(currentIndex);
            notifyOnIndexChange(true);
        } else {
            ((RecyclerView) $.id(R.id.dialog_switch_ppt).view()).scrollToPosition(currentIndex - mBoardList.size());
            isOpen = false;
            switchPPTState();
            notifyOnIndexChange(false);
        }
    }

    @Override
    public void setMaxIndex(int updateMaxIndex) {
    }

    @Override
    public void setType(boolean isStudent, boolean enableMultiWhiteboard) {
        this.isStudent = isStudent;
        this.enableMultiWhiteboard = enableMultiWhiteboard;

        if (isStudent || enableMultiWhiteboard) {
            $.id(R.id.ll_add_page).visibility(View.GONE);
        } else {
            $.id(R.id.ll_add_page).visibility(View.VISIBLE);
        }

    }

    @Override
    public void docListChanged(List<LPDocListViewModel.DocModel> docModelList) {

        this.mBoardList.clear();
        this.mPPTList.clear();

        if (isStudent) {
            //限制最大
            for (int i = 0; i < docModelList.size(); i++) {
                if (maxIndex < i) {
                    break;
                }
                LPDocListViewModel.DocModel model = docModelList.get(i);
                if (ShapeContent.TYPE_WHITEBOARD_DOC_ID.equals(model.docId)) {
                    //白板
                    mBoardList.add(model);
                } else {
                    //PPT
                    mPPTList.add(model);
                }
            }
        } else {
            for (int i = 0; i < docModelList.size(); i++) {
                LPDocListViewModel.DocModel model = docModelList.get(i);
                if (ShapeContent.TYPE_WHITEBOARD_DOC_ID.equals(model.docId)) {
                    //白板
                    mBoardList.add(model);
                } else {
                    //PPT
                    mPPTList.add(model);
                }
            }

            if (isChangePage && mBoardList.size() > 0) {
                presenter.changePage(mBoardList.get(mBoardList.size() - 1).pageId);
            }
            isChangePage = false;
        }

        if (mPPTList.size() <= 0) {
            //隐藏PPT展示区域
            $.id(R.id.ll_ppt).visibility(View.INVISIBLE);
        } else {
            $.id(R.id.ll_ppt).visibility(View.VISIBLE);
        }

        mPPTAdapter.setDeviation(mBoardList.size());
        mPPTAdapter.setBoardTag(false);
        adapter.notifyDataSetChanged();
        mPPTAdapter.notifyDataSetChanged();
    }

    private void switchEditStatus(boolean isEdit) {
        if (isEdit) {
            //显示编辑状态
            $.id(R.id.ll_ppt_ok).visibility(View.VISIBLE);
            adapter.setEditState(true);
        } else {
            //状态还原
            $.id(R.id.ll_ppt_ok).visibility(View.INVISIBLE);
            adapter.setEditState(false);
        }
        adapter.notifyDataSetChanged();

        isOpen = true;
        switchPPTState();
    }

    private class QuickSwitchPPTAdapter extends RecyclerView.Adapter<SwitchHolder> {

        private List<LPDocListViewModel.DocModel> list;
        private int deviation = 0;
        private boolean isBoard = true;//默认白板
        private boolean isEdit = false;

        public QuickSwitchPPTAdapter(List<LPDocListViewModel.DocModel> list) {
            this.list = list;
        }

        public void setBoardTag(boolean isBoard) {
            this.isBoard = isBoard;
        }

        public void setEditState(boolean isEdit) {
            this.isEdit = isEdit;
        }

        /**
         * 设置偏移，用于单击跳转PPT
         * @param deviation
         */
        public void setDeviation(int deviation) {
            this.deviation = deviation;
        }

        @Override
        public SwitchHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new SwitchHolder(LayoutInflater.from(getActivity()).inflate(R.layout.bjy_item_switch_ppt, parent, false));
        }

        @Override
        public void onBindViewHolder(final SwitchHolder holder, int position) {
            if(list.get(position).isH5Doc){
                Glide.with(getContext()).load(getContext().getResources().getDrawable(R.drawable.lp_h5_ppt_cover)).into(holder.PPTView);
            } else{
                Glide.with(getContext()).load(AliCloudImageUtil.getScaledUrl(list.get(position).url, AliCloudImageUtil.SCALED_MFIT, 200, 200)).into(holder.PPTView);
            }
            if (isBoard) {
                //白板
                holder.PPTOrder.setText(getResources().getString(R.string.string_board_title) + (position + 1));
                holder.PPTRL.setSelected(currentIndex == position);
            } else {
                holder.PPTOrder.setText(String.valueOf(position + 1));
                holder.PPTRL.setSelected(position == (currentIndex - mBoardList.size()));
            }

            if (isEdit) {
                holder.mIvPptDelete.setVisibility(View.VISIBLE);
                holder.mIvPptDelete.setTag(list.get(position).pageId);
                holder.mIvPptDelete.setOnClickListener(v -> deleteBoard(list.get(position).pageId));
            } else {
                holder.mIvPptDelete.setVisibility(View.INVISIBLE);
            }

            holder.PPTRL.setOnClickListener(v -> {
                if (!presenter.canOperateDocumentControl()) {
                    showToast(getString(R.string.live_room_document_control_permission_forbid));
                    return;
                }
                lastIndex = currentIndex;
                notifyOnIndexChange(isBoard);
                currentIndex = position + (deviation < 0 ? 0 : deviation);
                presenter.setSwitchPosition(position + (deviation < 0 ? 0 : deviation));
            });

            holder.PPTRL.setOnLongClickListener(v -> {
                if (!presenter.canOperateDocumentControl()) {
                    showToast(getString(R.string.live_room_document_control_permission_forbid));
                    return false;
                }
                //长按进行编辑
                if (!isBoard || isStudent || enableMultiWhiteboard)
                    return false;

                switchEditStatus(true);
                return true;
            });
        }

        @Override
        public int getItemCount() {
            return list.size();
        }
    }

    private void notifyOnIndexChange(boolean isBoard){
        if(isBoard){
            adapter.notifyDataSetChanged();
            if(lastIndex >= mBoardList.size()){
                mPPTAdapter.notifyDataSetChanged();
            }
        } else{
            if(lastIndex < mBoardList.size()){
                adapter.notifyDataSetChanged();
            }
            mPPTAdapter.notifyDataSetChanged();
        }
    }

    private void deleteBoard(int pageId) {

        if (pageId < 0)
            return;

        new MaterialDialog.Builder(getActivity())
                .title(getString(R.string.live_exit_hint_title))
                .content(getString(R.string.string_board_del_info))
                .contentColor(ContextCompat.getColor(getActivity(), R.color.live_text_color_light))
                .positiveColor(ContextCompat.getColor(getActivity(), R.color.live_blue))
                .positiveText(getString(R.string.live_exit_hint_confirm))
                .negativeColor(ContextCompat.getColor(getActivity(), R.color.live_text_color))
                .negativeText(getString(R.string.live_cancel))
                .onPositive((materialDialog, dialogAction) -> presenter.delPage(pageId))
                .onNegative((materialDialog, dialogAction) -> materialDialog.dismiss())
                .build()
                .show();

    }

    class SwitchHolder extends RecyclerView.ViewHolder {
        ImageView PPTView;
        TextView PPTOrder;
        RelativeLayout PPTRL;
        ImageView mIvPptDelete;

        SwitchHolder(View itemView) {
            super(itemView);
            this.PPTView = (ImageView) itemView.findViewById(R.id.item_ppt_view);
            this.PPTOrder = (TextView) itemView.findViewById(R.id.item_ppt_order);
            this.PPTRL = (RelativeLayout) itemView.findViewById(R.id.item_ppt_rl);
            this.mIvPptDelete = itemView.findViewById(R.id.iv_ppt_delete);
        }
    }
}
