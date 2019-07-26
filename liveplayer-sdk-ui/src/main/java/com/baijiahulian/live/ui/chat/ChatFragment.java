package com.baijiahulian.live.ui.chat;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.util.Linkify;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.baijiahulian.live.ui.R;
import com.baijiahulian.live.ui.base.BaseFragment;
import com.baijiahulian.live.ui.utils.AliCloudImageUtil;
import com.baijiahulian.live.ui.utils.ChatImageUtil;
import com.baijiahulian.live.ui.utils.DisplayUtils;
import com.baijiahulian.live.ui.utils.LinearLayoutWrapManager;
import com.baijiahulian.livecore.context.LPConstants;
import com.baijiahulian.livecore.models.imodels.IMessageModel;
import com.baijiahulian.livecore.models.imodels.IUserModel;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * Created by Shubo on 2017/2/23.
 */

public class ChatFragment extends BaseFragment implements ChatContract.View {

    private RecyclerView recyclerView;
    private MessageAdapter adapter;
    private ChatContract.Presenter presenter;
    LinearLayoutManager mLayoutManager;
    private ColorDrawable failedColorDrawable;
    private int emojiSize;
    private int backgroundRes;
    private int currentPosition;
    @ColorInt
    private int textColor;
//    private ImageSpan privateChatImageSpan;

    @Override
    public int getLayoutId() {
        return R.layout.fragment_chat;
    }

    @Override
    public void init(Bundle savedInstanceState) {
        super.init(savedInstanceState);
        failedColorDrawable = new ColorDrawable(ContextCompat.getColor(getContext(), R.color.live_half_transparent));
        emojiSize = (int) (DisplayUtils.getScreenDensity(getContext()) * 32);
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            backgroundRes = R.drawable.live_item_chat_bg_land;
            textColor = ContextCompat.getColor(getContext(), R.color.live_white);
        } else {
            backgroundRes = R.drawable.live_item_chat_bg;
            textColor = ContextCompat.getColor(getContext(), R.color.primary_text);
        }
//        privateChatImageSpan = new CenterImageSpan(getContext(), R.drawable.ic_live_private_chat, ImageSpan.ALIGN_BASELINE);

        adapter = new MessageAdapter();
        mLayoutManager = new LinearLayoutWrapManager(getContext());
        mLayoutManager.setOrientation(LinearLayoutManager.VERTICAL);

        recyclerView = (RecyclerView) $.id(R.id.fragment_chat_recycler).view();
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(adapter);

        $.id(R.id.fragment_chat_private_end_btn).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (presenter != null)
                    presenter.endPrivateChat();
            }
        });
    }

    @Override
    public void notifyDataChanged() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean isNeedScroll = false;
                if (needRemindNewMessageArrived()){
                    presenter.changeNewMessageReminder(true);
                }else
                    isNeedScroll = true;
                adapter.notifyDataSetChanged();
                if (isNeedScroll)
                    scrollToBottom();
            }
        });
    }


    private boolean needRemindNewMessageArrived(){
        return currentPosition < presenter.getCount() - 2 && presenter.needScrollToBottom();
    }

    @Override
    public void notifyItemChange(int position) {
        adapter.notifyItemChanged(position);
    }

    @Override
    public void notifyItemInserted(int position) {
        adapter.notifyItemInserted(position);
//        recyclerView.smoothScrollToPosition(adapter.getItemCount());
    }

    @Override
    public void showHavingPrivateChat(IUserModel privateChatUser) {
        if (!presenter.isLiveCanWhisper()) return;
        $.id(R.id.fragment_chat_private_status_container).visible();
        $.id(R.id.fragment_chat_private_user).text(getString(R.string.live_room_private_chat_with_name, privateChatUser.getName()));
    }

    public void scrollToBottom(){
        if (recyclerView != null)
        recyclerView.smoothScrollToPosition(adapter.getItemCount());
    }

    private void hideNewMessageReminder(){
        presenter.changeNewMessageReminder(false);
    }

    @Override
    public void showNoPrivateChat() {
        $.id(R.id.fragment_chat_private_status_container).gone();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            backgroundRes = R.drawable.live_item_chat_bg_land;
            textColor = ContextCompat.getColor(getContext(), R.color.live_white);
        } else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
            backgroundRes = R.drawable.live_item_chat_bg;
            textColor = ContextCompat.getColor(getContext(), R.color.primary_text);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void clearScreen() {
        recyclerView.setVisibility(View.INVISIBLE);
    }

    @Override
    public void unClearScreen() {
        recyclerView.setVisibility(View.VISIBLE);
    }

    @Override
    public void setPresenter(ChatContract.Presenter presenter) {
        this.presenter = presenter;
        super.setBasePresenter(presenter);
    }

    private class MessageAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int MESSAGE_TYPE_TEXT = 0;
        private static final int MESSAGE_TYPE_EMOJI = 1;
        private static final int MESSAGE_TYPE_IMAGE = 2;

        @Override
        public int getItemViewType(int position) {
            switch (presenter.getMessage(position).getMessageType()) {
                case Text:
                    return MESSAGE_TYPE_TEXT;
                case Emoji:
                case EmojiWithName:
                    return MESSAGE_TYPE_EMOJI;
                case Image:
                    return MESSAGE_TYPE_IMAGE;
                default:
                    return MESSAGE_TYPE_TEXT;
            }
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == MESSAGE_TYPE_TEXT) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_text, parent, false);
                return new TextViewHolder(view);
            } else if (viewType == MESSAGE_TYPE_EMOJI) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bj_live_item_chat_emoji, parent, false);
                return new EmojiViewHolder(view);
            } else if (viewType == MESSAGE_TYPE_IMAGE) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_chat_image, parent, false);
                return new ImageViewHolder(view);
            }
            return null;
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            currentPosition = position;
            if (position == presenter.getCount() - 1)
                hideNewMessageReminder();
            IMessageModel message = presenter.getMessage(position);
            SpannableString spanText;
            if (presenter.isPrivateChatMode()) {
                //私聊模式
                int color;
                if (message.getFrom().getType() == LPConstants.LPUserType.Teacher) {
                    color = ContextCompat.getColor(getContext(), R.color.live_blue);
                } else {
                    color = ContextCompat.getColor(getContext(), R.color.live_text_color_light);
                }
                String name = "";
                if (message.getFrom().getUserId().equals(presenter.getCurrentUser().getUserId())) {
                    color = ContextCompat.getColor(getContext(), R.color.live_yellow);
                    name = "我：";
                } else {
                    name = message.getFrom().getName() + "：";
                }
                spanText = new SpannableString(name);
                spanText.setSpan(new ForegroundColorSpan(color), 0, name.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

            } else {
                if (message.isPrivateChat()) {
                    // 群聊模式 私聊item
                    if (!presenter.isLiveCanWhisper()) return;
                    boolean isFromMe = message.getFrom().getUserId().equals(presenter.getCurrentUser().getUserId());
                    boolean isToMe = message.getTo().equals(presenter.getCurrentUser().getUserId());
                    String toName = message.getToUser() == null ? message.getTo() : message.getToUser().getName();
                    String name = (isFromMe ? "我" : message.getFrom().getName()) + " 私聊 " + (isToMe ? "我" : toName) + ": ";
                    spanText = new SpannableString(name);

                    if (isFromMe) {
                        spanText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getContext(), R.color.live_yellow)), 0, 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    } else if (message.getFrom().getType() == LPConstants.LPUserType.Teacher || message.getFrom().getType() == LPConstants.LPUserType.Assistant) {
                        spanText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getContext(), R.color.live_blue)), 0, message.getFrom().getName().length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                        spanText.setSpan(new NameClickSpan(presenter, message.getFrom()), 0, message.getFrom().getName().length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    } else {
                        spanText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getContext(), R.color.live_text_color_light)), 0, message.getFrom().getName().length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                        if (presenter.getCurrentUser().getType() == LPConstants.LPUserType.Teacher || presenter.getCurrentUser().getType() == LPConstants.LPUserType.Assistant)
                            spanText.setSpan(new NameClickSpan(presenter, message.getFrom()), 0, message.getFrom().getName().length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    }
                    if (isToMe) {
                        int start = name.lastIndexOf("我");
                        spanText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getContext(), R.color.live_yellow)), start, start + 1, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    } else if (message.getToUser() != null && (message.getToUser().getType() == LPConstants.LPUserType.Teacher || message.getToUser().getType() == LPConstants.LPUserType.Assistant)) {
                        int start = name.lastIndexOf(toName);
                        spanText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getContext(), R.color.live_blue)), start, start + toName.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                        spanText.setSpan(new NameClickSpan(presenter, message.getToUser()), start, start + toName.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    } else {
                        int start = name.lastIndexOf(toName);
                        spanText.setSpan(new ForegroundColorSpan(ContextCompat.getColor(getContext(), R.color.live_text_color_light)), start, start + toName.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                        if (message.getToUser() != null && (presenter.getCurrentUser().getType() == LPConstants.LPUserType.Teacher || presenter.getCurrentUser().getType() == LPConstants.LPUserType.Assistant))
                            spanText.setSpan(new NameClickSpan(presenter, message.getToUser()), start, start + toName.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    }
                } else {
                    // 群聊模式 群聊item
                    int color;
                    if (message.getFrom().getType() == LPConstants.LPUserType.Teacher || message.getFrom().getType() == LPConstants.LPUserType.Assistant) {
                        color = ContextCompat.getColor(getContext(), R.color.live_blue);
                    } else {
                        color = ContextCompat.getColor(getContext(), R.color.live_text_color_light);
                    }
                    String name = "";
                    if (message.getFrom().getNumber().equals(presenter.getCurrentUser().getNumber())) {
                        color = ContextCompat.getColor(getContext(), R.color.live_yellow);
                        name = "我：";
                    } else {
                        name = message.getFrom().getName() + "：";
                    }
                    spanText = new SpannableString(name);
                    spanText.setSpan(new ForegroundColorSpan(color), 0, name.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                    if (!message.getFrom().getUserId().equals(presenter.getCurrentUser().getUserId()) && presenter.isLiveCanWhisper()) {
                        if (presenter.getCurrentUser().getType() == LPConstants.LPUserType.Student) {
                            if (message.getFrom().getType() == LPConstants.LPUserType.Teacher || message.getFrom().getType() == LPConstants.LPUserType.Assistant)
                                spanText.setSpan(new NameClickSpan(presenter, message.getFrom()), 0, name.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                        } else if (presenter.getCurrentUser().getType() == LPConstants.LPUserType.Teacher || presenter.getCurrentUser().getType() == LPConstants.LPUserType.Assistant) {
                            spanText.setSpan(new NameClickSpan(presenter, message.getFrom()), 0, name.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                        }
                    }
                }
            }

            if (holder instanceof TextViewHolder) {
                TextViewHolder textViewHolder = (TextViewHolder) holder;
                textViewHolder.textView.setText(spanText);
                textViewHolder.textView.setMovementMethod(LinkMovementMethod.getInstance());
                textViewHolder.textView.setTextColor(textColor);
                textViewHolder.textView.append(message.getContent());
                if (message.getFrom().getType() == LPConstants.LPUserType.Teacher ||
                        message.getFrom().getType() == LPConstants.LPUserType.Assistant) {
                    Linkify.addLinks(textViewHolder.textView, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
                } else {
                    textViewHolder.textView.setAutoLinkMask(0);
                }
            } else if (holder instanceof EmojiViewHolder) {
                EmojiViewHolder emojiViewHolder = (EmojiViewHolder) holder;
                emojiViewHolder.tvName.setText(spanText);
                emojiViewHolder.tvName.setMovementMethod(LinkMovementMethod.getInstance());
                emojiViewHolder.tvName.setTextColor(textColor);
                Picasso.with(getContext()).load(message.getUrl())
                        .placeholder(R.drawable.live_ic_emoji_holder)
                        .error(R.drawable.live_ic_emoji_holder)
                        .resize(emojiSize, emojiSize)
                        .into(emojiViewHolder.ivEmoji);
            } else if (holder instanceof ImageViewHolder) {
                ImageViewHolder imageViewHolder = (ImageViewHolder) holder;
                imageViewHolder.ivImg.setOnClickListener(null);
                imageViewHolder.tvName.setText(spanText);
                imageViewHolder.tvName.setMovementMethod(LinkMovementMethod.getInstance());
                imageViewHolder.tvName.setTextColor(textColor);
                if (message instanceof UploadingImageModel) {
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inJustDecodeBounds = true;
                    BitmapFactory.decodeFile(message.getUrl(), options);
                    int[] size = {options.outWidth, options.outHeight};
                    ChatImageUtil.calculateImageSize(size, DisplayUtils.dip2px(getContext(), 100), DisplayUtils.dip2px(getContext(), 50));
                    Picasso.with(getContext()).load(new File(AliCloudImageUtil.getScaledUrl(message.getUrl(), AliCloudImageUtil.SCALED_MFIT, size[0], size[1])))
                            .resize(size[0], size[1])
                            .placeholder(failedColorDrawable)
                            .error(failedColorDrawable)
                            .into(imageViewHolder.ivImg);
                    if (((UploadingImageModel) message).getStatus() == UploadingImageModel.STATUS_UPLOADING) {
                        imageViewHolder.tvMask.setVisibility(View.VISIBLE);
                        imageViewHolder.tvExclamation.setVisibility(View.GONE);
                    } else if (((UploadingImageModel) message).getStatus() == UploadingImageModel.STATUS_UPLOAD_FAILED) {
                        imageViewHolder.tvMask.setVisibility(View.GONE);
                        imageViewHolder.tvExclamation.setVisibility(View.VISIBLE);
                        imageViewHolder.ivImg.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                presenter.reUploadImage(holder.getAdapterPosition());
                            }
                        });
                    }
                } else {
                    imageViewHolder.tvMask.setVisibility(View.GONE);
                    imageViewHolder.tvExclamation.setVisibility(View.GONE);
                    ImageTarget target = new ImageTarget(getContext(), imageViewHolder.ivImg);
                    Picasso.with(getContext()).load(AliCloudImageUtil.getScaledUrl(message.getUrl(), AliCloudImageUtil.SCALED_MFIT, 300, 300))
                            .placeholder(failedColorDrawable)
                            .error(failedColorDrawable)
                            .into(target);
                    // set tag to avoid target being garbage collected!
                    imageViewHolder.ivImg.setTag(target);
                    imageViewHolder.ivImg.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            presenter.showBigPic(holder.getAdapterPosition());
                        }
                    });
                }
            }
            holder.itemView.setBackgroundResource(backgroundRes);
        }

        @Override
        public int getItemCount() {
            return presenter.getCount();
        }
    }

    private static class TextViewHolder extends RecyclerView.ViewHolder {

        TextView textView;

        TextViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(R.id.item_chat_text);
        }
    }

    private static class ImageViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvExclamation, tvMask;
        ImageView ivImg;

        ImageViewHolder(View itemView) {
            super(itemView);
            tvName = (TextView) itemView.findViewById(R.id.item_chat_image_name);
            ivImg = (ImageView) itemView.findViewById(R.id.item_chat_image);
            tvExclamation = (TextView) itemView.findViewById(R.id.item_chat_image_exclamation);
            tvMask = (TextView) itemView.findViewById(R.id.item_chat_image_mask);
        }
    }

    private static class EmojiViewHolder extends RecyclerView.ViewHolder {

        TextView tvName;
        ImageView ivEmoji;

        EmojiViewHolder(View itemView) {
            super(itemView);
            tvName = (TextView) itemView.findViewById(R.id.item_chat_emoji_name);
            ivEmoji = (ImageView) itemView.findViewById(R.id.item_chat_emoji);
        }
    }

    private static class ImageTarget implements Target {

        private ImageView imageView;
        private WeakReference<Context> mContext;

        ImageTarget(Context context, ImageView imageView) {
            this.imageView = imageView;
            this.mContext = new WeakReference<>(context);
        }

        @Override
        public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom from) {
            Context context = mContext.get();
            if (context == null) return;
            RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) imageView.getLayoutParams();
            int[] size = {bitmap.getWidth(), bitmap.getHeight()};
            ChatImageUtil.calculateImageSize(size, DisplayUtils.dip2px(context, 100), DisplayUtils.dip2px(context, 50));
            lp.width = size[0];
            lp.height = size[1];
            imageView.setLayoutParams(lp);
            imageView.setImageBitmap(bitmap);
        }

        @Override
        public void onBitmapFailed( Drawable errorDrawable) {
            imageView.setImageDrawable(errorDrawable);
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
            imageView.setImageDrawable(placeHolderDrawable);
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

    private static class NameClickSpan extends ClickableSpan {
        private IUserModel userModel;
        private WeakReference<ChatContract.Presenter> wrPresenter;

        NameClickSpan(ChatContract.Presenter presenter, IUserModel userModel) {
            this.userModel = userModel;
            this.wrPresenter = new WeakReference<ChatContract.Presenter>(presenter);
        }

        @Override
        public void onClick(View widget) {
            ChatContract.Presenter presenter = wrPresenter.get();
            if (presenter != null) {
                presenter.showPrivateChat(userModel);
            }
        }

        @Override
        public void updateDrawState(TextPaint ds) {
        }
    }
}
