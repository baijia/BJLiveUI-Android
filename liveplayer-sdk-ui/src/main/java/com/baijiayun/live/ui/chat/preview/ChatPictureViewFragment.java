package com.baijiayun.live.ui.chat.preview;

import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.baijiayun.glide.Glide;
import com.baijiayun.glide.request.target.SimpleTarget;
import com.baijiayun.glide.request.transition.Transition;
import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseDialogFragment;
import com.baijiayun.live.ui.utils.DisplayUtils;
import com.baijiayun.livecore.ppt.util.AliCloudImageUtil;

import java.io.ByteArrayOutputStream;

/**
 * Created by Shubo on 2017/3/23.
 */

public class ChatPictureViewFragment extends BaseDialogFragment implements ChatPictureViewContract.View {

    private ImageView imageView;
    private TextView tvLoading;
    //    private Button btnSave;
    private ChatPictureViewContract.Presenter presenter;

    public static ChatPictureViewFragment newInstance(String url) {

        Bundle args = new Bundle();
        args.putString("url", url);
        ChatPictureViewFragment fragment = new ChatPictureViewFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected int getLayoutId() {
        return R.layout.dialog_big_picture;
    }

    @Override
    protected void init(Bundle savedInstanceState, Bundle arguments) {
        super.hideBackground().contentBackgroundColor(ContextCompat.getColor(getContext(), R.color.live_transparent));
        String url = arguments.getString("url");
        imageView = contentView.findViewById(R.id.lp_dialog_big_picture_img);
        tvLoading = contentView.findViewById(R.id.lp_dialog_big_picture_loading_label);

        Glide.with(getContext())
                .load(AliCloudImageUtil.getScaledUrl(url, AliCloudImageUtil.SCALED_MFIT, DisplayUtils.getScreenWidthPixels(getContext()), DisplayUtils.getScreenHeightPixels(getContext())))
                .into(new SimpleTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        tvLoading.setVisibility(View.GONE);
                        imageView.setImageDrawable(resource);
                    }

                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        super.onLoadFailed(errorDrawable);
                        try {
                            if (getActivity() != null)
                                tvLoading.setText(getString(R.string.live_image_loading_fail));
                        } catch (IllegalStateException ignore) {
                        }
                    }
                });

        imageView.setOnClickListener(v -> {
            dismissAllowingStateLoss();
        });
        imageView.setOnLongClickListener(v -> {
            byte[] src = convertBmpToByteArray();
            if (src != null)
                presenter.showSaveDialog(src);
            return true;
        });
        contentView.setOnClickListener(v -> dismissAllowingStateLoss());
    }

    @Override
    protected void setWindowParams(WindowManager.LayoutParams windowParams) {
        windowParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        windowParams.dimAmount = 0.85f;
//        windowParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        windowParams.windowAnimations = R.style.ViewBigPicAnim;
    }

    /**
     * 将bitmap转为字节数组,避免presenter使用Android api
     */
    private byte[] convertBmpToByteArray() {
        if (imageView.getDrawable() == null) return null;
        Bitmap bmp = ((BitmapDrawable) imageView.getDrawable()).getBitmap();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
        return outputStream.toByteArray();
    }


    @Override
    public void setPresenter(ChatPictureViewContract.Presenter presenter) {
        super.setBasePresenter(presenter);
        this.presenter = presenter;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        presenter = null;
    }
}
