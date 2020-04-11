package com.baijiayun.live.ui.ppt.pptmanage;

import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.baijiahulian.common.cropperv2.BJCommonImageCropHelper;
import com.baijiahulian.common.cropperv2.ThemeConfig;
import com.baijiahulian.common.cropperv2.model.PhotoInfo;
import com.baijiayun.glide.Glide;
import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseDialogFragment;
import com.baijiayun.live.ui.utils.LinearLayoutWrapManager;
import com.baijiayun.live.ui.utils.QueryPlus;
import com.baijiayun.livecore.ppt.util.AliCloudImageUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Shubo on 2017/4/26.
 */

public class PPTManageFragment extends BaseDialogFragment implements PPTManageContract.View {

    private PPTManageContract.Presenter presenter;
    private QueryPlus $;
    private DocumentAdapter adapter;

    public static PPTManageFragment newInstance() {

        Bundle args = new Bundle();

        PPTManageFragment fragment = new PPTManageFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.bjy_dialog_ppt_manage;
    }

    @Override
    protected void init(Bundle savedInstanceState, Bundle arguments) {
        super.title(getString(R.string.live_ppt)).editable(true);
        $ = QueryPlus.with(contentView);
        RecyclerView recyclerView = (RecyclerView) $.id(R.id.dialog_ppt_manage_rv).view();
        recyclerView.setLayoutManager(new LinearLayoutWrapManager(getContext()));
        adapter = new DocumentAdapter();
        recyclerView.setAdapter(adapter);

        $.id(R.id.dialog_ppt_manage_btn).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!presenter.canOperateDocument()) {
                    showToast(getString(R.string.live_room_document_upload_permission_forbid));
                    return;
                }
                if (isEditing()) {
                    presenter.removeSelectedItems();
                } else {
                    ThemeConfig.Builder builder = new ThemeConfig.Builder();
                    builder.setMainElementsColor(ContextCompat.getColor(getContext(), R.color.live_blue));
                    BJCommonImageCropHelper.openImageMulti(getActivity(), 20, builder.build(), new BJCommonImageCropHelper.OnHandlerResultCallback() {
                        @Override
                        public void onHandlerSuccess(List<PhotoInfo> list) {
                            if (presenter == null) {
                                return;
                            }
                            List<String> pics = new ArrayList<>();
                            for (PhotoInfo photoInfo : list) {
                                pics.add(photoInfo.getPhotoPath());
                            }
                            presenter.uploadNewPics(pics);
                        }

                        @Override
                        public void onHandlerFailure(String s) {
                            showToast(s);
                        }
                    });
                }
            }
        });
        presenter.attachView(this);
    }

    @Override
    protected void enableEdit() {
        if (!presenter.canOperateDocument()) {
            showToast(getString(R.string.live_room_document_upload_permission_forbid));
            return;
        }
        super.enableEdit();
        $.id(R.id.dialog_ppt_manage_btn)
                .background(ContextCompat.getColor(getContext(), R.color.live_fail_dark))
                .text(getString(R.string.live_remove))
                .enable(false);
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void disableEdit() {
        super.disableEdit();
        $.id(R.id.dialog_ppt_manage_btn)
                .background(ContextCompat.getColor(getContext(), R.color.live_blue))
                .text(getString(R.string.live_upload_new_file))
                .enable(true);
        adapter.notifyDataSetChanged();
    }

    @Override
    public void setPresenter(PPTManageContract.Presenter presenter) {
        super.setBasePresenter(null);
        this.presenter = presenter;
    }

    @Override
    public void showPPTEmpty() {
        $.id(R.id.dialog_ppt_manage_empty_container).visible();
        $.id(R.id.dialog_ppt_manage_rv).gone();
        disableEdit();
    }

    @Override
    public void showPPTNotEmpty() {
        $.id(R.id.dialog_ppt_manage_empty_container).gone();
        $.id(R.id.dialog_ppt_manage_rv).visible();
    }

    @Override
    public void notifyDataChange() {
        adapter.notifyDataSetChanged();
    }

    @Override
    public void notifyItemRemoved(int position) {
        adapter.notifyItemRemoved(position);
    }

    @Override
    public void notifyItemChanged(final int position) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                adapter.notifyItemChanged(position);
            }
        });
    }

    @Override
    public void notifyItemInserted(int position) {
        adapter.notifyItemInserted(position);
    }

    @Override
    public void showRemoveBtnEnable() {
        $.id(R.id.dialog_ppt_manage_btn)
                .background(ContextCompat.getColor(getContext(), R.color.live_red))
                .text(getString(R.string.live_remove))
                .enable(true);
    }

    @Override
    public void showRemoveBtnDisable() {
        $.id(R.id.dialog_ppt_manage_btn)
                .background(ContextCompat.getColor(getContext(), R.color.live_fail_dark))
                .text(getString(R.string.live_remove))
                .enable(false);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter.detachView();
        presenter = null;
        $ = null;
    }

    private static class DocViewHolder extends RecyclerView.ViewHolder {

        CheckBox checkBox;
        ImageView ivIcon;
        TextView tvTitle;

        DocViewHolder(View itemView) {
            super(itemView);
            checkBox = (CheckBox) itemView.findViewById(R.id.item_ppt_manage_normal_check_box);
            ivIcon = (ImageView) itemView.findViewById(R.id.item_ppt_manage_normal_icon);
            tvTitle = (TextView) itemView.findViewById(R.id.item_ppt_manage_normal_title);
        }
    }

    private static class UploadingViewHolder extends RecyclerView.ViewHolder {

        ImageView ivIcon;
        TextView tvTitle;
        View progress;
        TextView tvStatus;

        UploadingViewHolder(View itemView) {
            super(itemView);
            ivIcon = (ImageView) itemView.findViewById(R.id.item_ppt_manage_uploading_icon);
            tvTitle = (TextView) itemView.findViewById(R.id.item_ppt_manage_uploading_title);
            tvStatus = (TextView) itemView.findViewById(R.id.item_ppt_manage_uploading_status);
            progress = itemView.findViewById(R.id.item_ppt_manage_uploading_progress);
        }
    }

    private class DocumentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int ITEM_TYPE_NORMAL = 0;
        private static final int ITEM_TYPE_UPLOADING = 1;

        @Override
        public int getItemViewType(int position) {
            return presenter.isDocumentAdded(position) ? ITEM_TYPE_NORMAL : ITEM_TYPE_UPLOADING;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == ITEM_TYPE_NORMAL) {
                View view = LayoutInflater.from(getActivity()).inflate(R.layout.bjy_item_ppt_manage_normal, parent, false);
                return new DocViewHolder(view);
            } else if (viewType == ITEM_TYPE_UPLOADING) {
                View view = LayoutInflater.from(getActivity()).inflate(R.layout.bjy_item_ppt_manage_uploading, parent, false);
                return new UploadingViewHolder(view);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            if (holder instanceof DocViewHolder) {
                DocViewHolder docViewHolder = (DocViewHolder) holder;
                docViewHolder.tvTitle.setText(presenter.getItem(position).getFileName());
                if (isEditing()) {
                    docViewHolder.checkBox.setVisibility(View.VISIBLE);
                } else {
                    docViewHolder.checkBox.setVisibility(View.GONE);
                }
                docViewHolder.checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        if (isChecked)
                            presenter.selectItem(holder.getLayoutPosition());
                        else
                            presenter.deselectItem(holder.getLayoutPosition());
                    }
                });
                docViewHolder.checkBox.setChecked(presenter.isItemSelected(position));
                int res = getDrawableResByFileExt(presenter.getItem(position).getFileExt());
                if (res == 0) { // pic
                    String url = AliCloudImageUtil.getScaledUrl(((DocumentModel) presenter.getItem(position)).pageInfoModel.url, AliCloudImageUtil.SCALED_FILL, 50, 50);
                    Glide.with(getContext()).load(url).into(docViewHolder.ivIcon);
                } else {
                    Glide.with(getContext()).load(res).into(docViewHolder.ivIcon);
                }
            } else if (holder instanceof UploadingViewHolder) {
                UploadingViewHolder viewHolder = (UploadingViewHolder) holder;
                viewHolder.tvTitle.setText(presenter.getItem(position).getFileName());
                int res = getDrawableResByFileExt(presenter.getItem(position).getFileExt());
                if (res == 0) {
                    Glide.with(getContext()).load(R.drawable.live_ic_file_jpg).into(viewHolder.ivIcon);
                } else {
                    Glide.with(getContext()).load(res).into(viewHolder.ivIcon);
                }
                if (presenter.getItem(position).getStatus() == DocumentUploadingModel.UPLOADING) {
                    viewHolder.tvStatus.setText(getString(R.string.live_uploading));
                } else if (presenter.getItem(position).getStatus() == DocumentUploadingModel.UPLOADED) {
                    viewHolder.tvStatus.setText(getString(R.string.live_queueing));
                } else if (presenter.getItem(position).getStatus() == DocumentUploadingModel.UPLOAD_FAIL) {
                    viewHolder.tvStatus.setText(getString(R.string.live_upload_fail));
                } else {
                    viewHolder.tvStatus.setText("");
                }
//                TextViewHolder.tvStatus.setText(String.valueOf(presenter.getItem(position).getUploadPercent()));
            }
        }

        @Override
        public int getItemCount() {
            return presenter.getCount();
        }

        private int getDrawableResByFileExt(String ext) {
            if (ext == null) return 0;
            switch (ext) {
                case ".doc":
                case ".docx":
                    return R.drawable.live_ic_file_doc;
                case ".ppt":
                case ".pptx":
                    return R.drawable.live_ic_file_ppt;
                case ".pdf":
                    return R.drawable.live_ic_file_pdf;
                default:
                    return 0;
            }
        }
    }
}
