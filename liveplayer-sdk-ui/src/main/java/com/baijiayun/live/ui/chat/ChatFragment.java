package com.baijiayun.live.ui.chat;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GestureDetectorCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.util.Linkify;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseFragment;
import com.baijiayun.live.ui.chat.utils.CenterImageSpan;
import com.baijiayun.live.ui.chat.utils.URLImageParser;
import com.baijiayun.live.ui.chat.widget.ChatMessageView;
import com.baijiayun.live.ui.utils.AliCloudImageUtil;
import com.baijiayun.live.ui.utils.ChatImageUtil;
import com.baijiayun.live.ui.utils.DisplayUtils;
import com.baijiayun.live.ui.utils.LinearLayoutWrapManager;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.models.imodels.IMessageModel;
import com.baijiayun.livecore.models.imodels.IUserModel;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        return R.layout.bjy_fragment_chat;
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
        recyclerView.getRecycledViewPool().setMaxRecycledViews(0, 0);
        recyclerView.setLayoutManager(mLayoutManager);
        recyclerView.setAdapter(adapter);

        $.id(R.id.fragment_chat_private_end_btn).clicked(v -> {
            if (presenter != null)
                presenter.endPrivateChat();
            Toast.makeText(getContext(), "私聊已取消", Toast.LENGTH_SHORT).show();
        });
        $.id(R.id.fragment_chat_filter_close).clicked(v -> {
            if (presenter != null)
                presenter.setFilter(false);
        });
    }

    @Override
    public void notifyDataChanged() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                boolean isNeedScroll = false;
                if (needRemindNewMessageArrived()) {
                    presenter.changeNewMessageReminder(true);
                } else
                    isNeedScroll = true;
                adapter.notifyDataSetChanged();
                if (isNeedScroll)
                    scrollToBottom();
            }
        });
    }


    private boolean needRemindNewMessageArrived() {
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
    public void notifyItemTranslateMessage() {
        adapter.notifyDataSetChanged();
    }

    @Override
    public void showHavingPrivateChat(IUserModel privateChatUser) {
        if (!presenter.isLiveCanWhisper()) return;
        presenter.setFilter(false);
        $.id(R.id.fragment_chat_private_status_container).visible();
        $.id(R.id.fragment_chat_private_user).text(getString(R.string.live_room_private_chat_with_name, privateChatUser.getName()));
    }

    @Override
    public void showFilterChat(boolean filter) {
        $.id(R.id.fragment_chat_filter).visibility(filter ? View.VISIBLE : View.GONE);
    }

    public void scrollToBottom() {
        if (recyclerView != null)
            recyclerView.smoothScrollToPosition(adapter.getItemCount());
    }

    private void hideNewMessageReminder() {
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
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bjy_item_chat_emoji, parent, false);
                return new EmojiViewHolder(view);
            } else if (viewType == MESSAGE_TYPE_IMAGE) {
                View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.bjy_item_chat_image, parent, false);
                return new ImageViewHolder(view);
            }
            return null;
        }

        @SuppressLint("ClickableViewAccessibility")
        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
            if (position < 0 || position >= getItemCount()) {
                return;
            }
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
                textViewHolder.chatMessageView.getTextViewChat().setMovementMethod(LinkMovementClickMethod.getInstance());
                textViewHolder.chatMessageView.getTextViewChat().setTextColor(textColor);
                SpannableStringBuilder ssb = new SpannableStringBuilder();
                ssb.append(spanText).append(getMixText(message.getContent(), textViewHolder.chatMessageView.getTextViewChat()));
                ssb.setSpan(textViewHolder.chatMessageView.getLineHeightSpan(), 0, ssb.length() - 1, Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
                textViewHolder.chatMessageView.getTextViewChat().setText(ssb);
                textViewHolder.chatMessageView.getTextViewChat().setFocusable(false);
                textViewHolder.chatMessageView.getTextViewChat().setClickable(false);
                textViewHolder.chatMessageView.getTextViewChat().setLongClickable(false);
                textViewHolder.chatMessageView.enableTranslation(presenter.isEnableTranslate());
                textViewHolder.chatMessageView.setRecallStatus(presenter.getRecallStatus(message));
                textViewHolder.chatMessageView.enableFilter((message.getFrom().getType() == LPConstants.LPUserType.Teacher ||
                        message.getFrom().getType() == LPConstants.LPUserType.Assistant) && !presenter.isPrivateChatMode());
                textViewHolder.chatMessageView.setFiltered(presenter.getFilter());
                textViewHolder.chatMessageView.setMessage(message.getContent());
                textViewHolder.chatMessageView.addTranslateMessage(presenter.getTranslateResult(position));
//                Log.d("ChatMessageView", "onBindViewHolder: message id=" + message.getId() + "........message content=" + message.getContent() + ".....result=" + presenter.getTranslateResult(position));
                textViewHolder.chatMessageView.setOnProgressListener(() -> {
                    //判断是否有中文，有就翻译成英文，没有就翻译成中文
                    String fromLanguage, toLanguage;
                    Pattern pattern = Pattern.compile("[\\u4E00-\\u9FBF]+");
                    if (pattern.matcher(message.getContent()).find()) {
                        fromLanguage = "zh";
                        toLanguage = "en";
                    } else {
                        fromLanguage = "en";
                        toLanguage = "zh";
                    }
                    presenter.translateMessage(getTranslateText(message.getContent()), message.getFrom().getUserId() + message.getTime().getTime(), fromLanguage, toLanguage);
                });
                textViewHolder.chatMessageView.setOnFilterListener(() -> presenter.setFilter(true));
                textViewHolder.chatMessageView.setOnReCallListener(() -> presenter.reCallMessage(message));
                if (message.getFrom().getType() == LPConstants.LPUserType.Teacher ||
                        message.getFrom().getType() == LPConstants.LPUserType.Assistant) {
                    Linkify.addLinks(textViewHolder.chatMessageView.getTextViewChat(), Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);
                } else {
                    textViewHolder.chatMessageView.getTextViewChat().setAutoLinkMask(0);
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
                if (presenter.getRecallStatus(message) != ChatMessageView.NONE) {
                    GestureDetectorCompat gestureDetectorCompat = new GestureDetectorCompat(getContext(), new PressListener(message, holder));
                    emojiViewHolder.itemView.setOnTouchListener((v, event) -> {
                        gestureDetectorCompat.onTouchEvent(event);
                        return false;
                    });
                }
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
                        imageViewHolder.ivImg.setOnClickListener(v -> presenter.reUploadImage(holder.getAdapterPosition()));
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
                    GestureDetectorCompat gestureDetectorCompat = new GestureDetectorCompat(getContext(), new PressListener(message, holder, true));
                    imageViewHolder.ivImg.setOnTouchListener((v, event) -> {
                        gestureDetectorCompat.onTouchEvent(event);
                        return false;
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

    private SpannableStringBuilder getMixText(String content, TextView textView) {
        Pattern p = Pattern.compile("\\[[a-zA-Z0-9\u4e00-\u9fa5]+]");
        Matcher m = p.matcher(content);
        SpannableStringBuilder ssb = new SpannableStringBuilder(content);
        while (m.find()) {
            String group = m.group();
            if (presenter.getExpressions().containsKey(group)) {
                Drawable drawable = new URLImageParser(textView, textView.getTextSize()).getDrawable(presenter.getExpressions().get(group));
                CenterImageSpan centerImageSpan = new CenterImageSpan(drawable, ImageSpan.ALIGN_BASELINE);
                ssb.setSpan(centerImageSpan, m.start(), m.end(), Spanned.SPAN_EXCLUSIVE_INCLUSIVE);
                ssb.removeSpan(group);
            }
        }
        return ssb;
    }

    /**
     * 翻译去除表情中的[大笑]
     *
     * @param content
     * @return
     */
    private String getTranslateText(String content) {
        Pattern p = Pattern.compile("\\[[a-zA-Z0-9\u4e00-\u9fa5]+]");
        Matcher m = p.matcher(content);
        while (m.find()) {
            String group = m.group();
            if (presenter.getExpressions().containsKey(group)) {
                content = content.replace(group, "");
            }
        }
        return content;
    }

    private static class TextViewHolder extends RecyclerView.ViewHolder {
        ChatMessageView chatMessageView;

        TextViewHolder(View itemView) {
            super(itemView);
            chatMessageView = itemView.findViewById(R.id.item_chat_view);
        }
    }

    private static class ImageViewHolder extends RecyclerView.ViewHolder {

        TextView tvName, tvExclamation, tvMask;
        ImageView ivImg;

        ImageViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.item_chat_image_name);
            ivImg = itemView.findViewById(R.id.item_chat_image);
            tvExclamation = itemView.findViewById(R.id.item_chat_image_exclamation);
            tvMask = itemView.findViewById(R.id.item_chat_image_mask);
        }
    }

    private static class EmojiViewHolder extends RecyclerView.ViewHolder {

        TextView tvName;
        ImageView ivEmoji;

        EmojiViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.item_chat_emoji_name);
            ivEmoji = itemView.findViewById(R.id.item_chat_emoji);
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
        public void onBitmapFailed(Drawable errorDrawable) {
            imageView.setImageDrawable(errorDrawable);
        }

        @Override
        public void onPrepareLoad(Drawable placeHolderDrawable) {
            imageView.setImageDrawable(placeHolderDrawable);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (recyclerView != null) {
            recyclerView.setAdapter(null);
            recyclerView = null;
        }
        presenter = null;
    }

    private class NameClickSpan extends ClickableSpan {
        private IUserModel userModel;
        private WeakReference<ChatContract.Presenter> wrPresenter;

        NameClickSpan(ChatContract.Presenter presenter, IUserModel userModel) {
            this.userModel = userModel;
            this.wrPresenter = new WeakReference<ChatContract.Presenter>(presenter);
        }

        @Override
        public void onClick(View widget) {
            ChatContract.Presenter presenter = wrPresenter.get();
            if (presenter != null && presenter.isForbiddenByTeacher()) {
                ChatFragment.this.showToast(getString(R.string.live_forbid_send_message));
                return;
            }
            if (presenter != null) {
                presenter.showPrivateChat(userModel);
            }
        }

        @Override
        public void updateDrawState(TextPaint ds) {
        }
    }

    private void showMenu(int x, int y, View parentView, IMessageModel iMessageModel) {
        PopupWindow popupWindow = new PopupWindow(getContext());
        popupWindow.setFocusable(true);
        popupWindow.setWidth(DisplayUtils.dip2px(getContext(), 60));
        popupWindow.setBackgroundDrawable(new ColorDrawable(0));

        List<String> items = new ArrayList<>();
        final int recallStatus = presenter.getRecallStatus(iMessageModel);
        if (recallStatus == ChatMessageView.RECALL) {
            items.add(getContext().getString(R.string.live_chat_recall));
        }
        if (recallStatus == ChatMessageView.DELETE) {
            items.add(getContext().getString(R.string.live_chat_delete));
        }
        popupWindow.setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        String[] strs = new String[items.size()];
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), R.layout.menu_chat_message, items.toArray(strs));
        ListView listView = new ListView(getContext());

        GradientDrawable bgDrawable = new GradientDrawable();
        bgDrawable.setColor(Color.WHITE);
        bgDrawable.setCornerRadius(DisplayUtils.dip2px(getContext(), 4));
        listView.setBackground(bgDrawable);
        listView.setAdapter(adapter);
        listView.setDividerHeight(0);
        listView.setPadding(0, DisplayUtils.dip2px(getContext(), 2), 0, DisplayUtils.dip2px(getContext(), 2));
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position == 0) {
                presenter.reCallMessage(iMessageModel);
            }
            popupWindow.dismiss();
        });
        popupWindow.setContentView(listView);
        popupWindow.showAtLocation(parentView, Gravity.NO_GRAVITY, x - popupWindow.getWidth() / 2, y - popupWindow.getHeight());
    }

    class PressListener extends GestureDetector.SimpleOnGestureListener {
        private IMessageModel iMessageModel;
        private View parent;
        private boolean showBigPic;
        private int position;

        public PressListener(IMessageModel iMessageModel, RecyclerView.ViewHolder holder) {
            this.iMessageModel = iMessageModel;
            this.parent = holder.itemView;
        }

        public PressListener(IMessageModel iMessageModel, RecyclerView.ViewHolder holder, boolean showBigPic) {
            this.iMessageModel = iMessageModel;
            this.parent = holder.itemView;
            this.showBigPic = showBigPic;
            this.position = holder.getAdapterPosition();
        }

        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
            if (presenter.getRecallStatus(iMessageModel) == ChatMessageView.NONE) {
                return;
            }
            showMenu((int) e.getRawX(), (int) e.getRawY(), parent, iMessageModel);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            if (!showBigPic) {
                return false;
            }
            presenter.showBigPic(position);
            return true;
        }
    }
}
