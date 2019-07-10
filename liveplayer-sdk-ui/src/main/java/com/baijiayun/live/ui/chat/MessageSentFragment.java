package com.baijiayun.live.ui.chat;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;

import com.baijiahulian.common.cropperv2.BJCommonImageCropHelper;
import com.baijiahulian.common.cropperv2.ThemeConfig;
import com.baijiahulian.common.cropperv2.model.PhotoInfo;
import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseDialogFragment;
import com.baijiayun.live.ui.chat.emoji.EmojiFragment;
import com.baijiayun.live.ui.chat.emoji.EmojiSelectCallBack;
import com.baijiayun.live.ui.chat.privatechat.ChatUsersDialogFragment;
import com.baijiayun.live.ui.utils.QueryPlus;
import com.baijiayun.livecore.models.imodels.IExpressionModel;

import java.util.List;

import static android.content.Context.INPUT_METHOD_SERVICE;

/**
 * Created by Shubo on 2017/3/4.
 */

public class MessageSentFragment extends BaseDialogFragment implements MessageSendContract.View {

    private MessageSendContract.Presenter presenter;
    private QueryPlus $;
    private MessageTextWatcher textWatcher;
    private EmojiFragment emojiFragment;
    private ChatUsersDialogFragment chatUsersDialogFragment;

    public static MessageSentFragment newInstance() {
        return new MessageSentFragment();
    }

    @Override
    public int getLayoutId() {
        return R.layout.dialog_message_send;
    }

    @Override
    protected void setWindowParams(WindowManager.LayoutParams windowParams) {
        windowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
        windowParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        windowParams.gravity = Gravity.BOTTOM | GravityCompat.END;
        windowParams.windowAnimations = R.style.LiveBaseSendMsgDialogAnim;
    }

    @Override
    protected void init(Bundle savedInstanceState, Bundle arguments) {
        super.hideTitleBar();
        $ = QueryPlus.with(contentView);
        textWatcher = new MessageTextWatcher();

        ((EditText) $.id(R.id.dialog_message_send_et).view()).addTextChangedListener(textWatcher);

        ((EditText) $.id(R.id.dialog_message_send_et).view()).setFilters(new InputFilter[]{new InputFilter.LengthFilter(140)});

        ((EditText) $.id(R.id.dialog_message_send_et).view()).setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND &&  $.id(R.id.dialog_message_send_btn).view().isEnabled()) {
                sendMessage();
            }
            return true;
        });
        if (presenter.isLiveCanWhisper()) {
            showPrivateChatChange();
        } else {
            $.id(R.id.dialog_private_chat_btn_container).gone();
            $.id(R.id.dialog_interval_line).gone();
        }

        $.id(R.id.dialog_message_send_et).view().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (getActivity() == null) return;
                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(INPUT_METHOD_SERVICE);
                if (imm == null) return;
                imm.showSoftInput($.id(R.id.dialog_message_send_et).view(), InputMethodManager.SHOW_IMPLICIT);
            }
        }, 100);

        $.id(R.id.dialog_message_send_et).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (chatUsersDialogFragment != null) {
                    $.id(R.id.dialog_private_chat_users).gone();
                    FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
                    transaction.remove(chatUsersDialogFragment);
                    if (Build.VERSION.SDK_INT >= 24) {
                        transaction.commitNowAllowingStateLoss();
                    } else {
                        transaction.commitAllowingStateLoss();
                    }
                    chatUsersDialogFragment = null;
//                        imm.showSoftInput($.id(R.id.dialog_message_send_et).view(), InputMethodManager.SHOW_IMPLICIT);
                } else if (emojiFragment != null) {
                    $.id(R.id.dialog_message_send_emoji).gone();
                    FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
                    transaction.remove(emojiFragment);
                    if (Build.VERSION.SDK_INT >= 24) {
                        transaction.commitNowAllowingStateLoss();
                    } else {
                        transaction.commitAllowingStateLoss();
                    }
                    emojiFragment = null;
                }

                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(INPUT_METHOD_SERVICE);
                if (!imm.isActive()) {
                    imm.showSoftInput($.id(R.id.dialog_message_send_et).view(), InputMethodManager.SHOW_IMPLICIT);
                }
            }
        });

        $.id(R.id.dialog_message_send_btn).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMessage();
            }
        });

        $.id(R.id.dialog_message_send_pic).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ThemeConfig.Builder builder = new ThemeConfig.Builder();
                builder.setMainElementsColor(ContextCompat.getColor(getContext(), R.color.live_blue));
                BJCommonImageCropHelper.openImageSingleAblum(getActivity(), BJCommonImageCropHelper.PhotoCropType.Free, builder.build(), new BJCommonImageCropHelper.OnHandlerResultCallback() {
                    @Override
                    public void onHandlerSuccess(List<PhotoInfo> list) {
                        if (list.size() == 1) {
                            presenter.sendPicture(list.get(0).getPhotoPath());
                        }
                    }

                    @Override
                    public void onHandlerFailure(String s) {
                        showToast(s);
                    }
                });
            }
        });

        if (presenter.canSendPicture()) {
            $.id(R.id.dialog_message_send_pic).visible();
        } else {
            $.id(R.id.dialog_message_send_pic).gone();
        }

        $.id(R.id.dialog_message_emoji).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.chooseEmoji();
            }
        });

        $.id(R.id.dialog_private_chat_btn_container).clicked(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                presenter.choosePrivateChatUser();
            }
        });

        $.id(R.id.dialog_message_send_btn).enable(false);
    }

    private void sendMessage() {
        if (presenter.isPrivateChat()) {
            presenter.sendMessageToUser(((EditText) $.id(R.id.dialog_message_send_et).view())
                    .getEditableText().toString());
        } else {
            presenter.sendMessage(((EditText) $.id(R.id.dialog_message_send_et).view())
                    .getEditableText().toString());
        }
        dismissAllowingStateLoss();
    }

    @Override
    public void onStart() {
        super.onStart();
    }


    @Override
    public void setPresenter(MessageSendContract.Presenter presenter) {
        super.setBasePresenter(presenter);
        this.presenter = presenter;
    }

    @Override
    public void showPrivateChatChange() {
        if(!presenter.isLiveCanWhisper()) return;
        if (presenter.isPrivateChat()) {
            ((EditText) $.id(R.id.dialog_message_send_et).view()).setHint("私聊" + presenter.getPrivateChatUser().getName());
            ($.id(R.id.dialog_private_chat_btn).view()).setSelected(true);
            ((TextView) $.id(R.id.dialog_private_chat_btn).view()).setTextColor(getResources().getColor(R.color.live_blue));
        } else {
            ((EditText) $.id(R.id.dialog_message_send_et).view()).setHint("输入聊天内容");
            ($.id(R.id.dialog_private_chat_btn).view()).setSelected(false);
            ((TextView) $.id(R.id.dialog_private_chat_btn).view()).setTextColor(getResources().getColor(R.color.live_text_color_mid_light));
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ((EditText) $.id(R.id.dialog_message_send_et).view()).removeTextChangedListener(textWatcher);
        $ = null;
        presenter = null;
    }

    @Override
    public void showMessageSuccess() {
        $.id(R.id.dialog_message_send_et).text("");
        dismissAllowingStateLoss();
    }

    @Override
    public void showPrivateChatUserPanel() {
        if (!presenter.isLiveCanWhisper()) return;
        if (chatUsersDialogFragment == null && emojiFragment == null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(INPUT_METHOD_SERVICE);
            if (imm.isActive()) {//isOpen若返回true，则表示输入法打开
                imm.hideSoftInputFromWindow($.id(R.id.dialog_message_send_et).view().getWindowToken(), 0);
            }

            contentView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if ($ == null) return;
                    $.id(R.id.dialog_private_chat_users).visible();
                    chatUsersDialogFragment = new ChatUsersDialogFragment();
                    if (presenter.isPrivateChat()) {
                        chatUsersDialogFragment.initPrivateChatLabel(presenter.getPrivateChatUser());
                    }
                    FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
                    transaction.add(R.id.dialog_private_chat_users, chatUsersDialogFragment);
                    if (Build.VERSION.SDK_INT >= 24) {
                        transaction.commitNowAllowingStateLoss();
                    } else {
                        transaction.commitAllowingStateLoss();
                    }
                }
            }, 100);


        } else if (emojiFragment != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(INPUT_METHOD_SERVICE);
            if (imm.isActive()) {//isOpen若返回true，则表示输入法打开
                imm.hideSoftInputFromWindow($.id(R.id.dialog_message_send_et).view().getWindowToken(), 0);
            }

            $.id(R.id.dialog_message_send_emoji).gone();
            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
            transaction.remove(emojiFragment);
            if (Build.VERSION.SDK_INT >= 24) {
                transaction.commitNowAllowingStateLoss();
            } else {
                transaction.commitAllowingStateLoss();
            }
            emojiFragment = null;

            $.id(R.id.dialog_private_chat_users).visible();
            chatUsersDialogFragment = new ChatUsersDialogFragment();
            if (presenter.isPrivateChat()) {
                chatUsersDialogFragment.initPrivateChatLabel(presenter.getPrivateChatUser());
            }
            FragmentTransaction _transaction = getChildFragmentManager().beginTransaction();
            _transaction.add(R.id.dialog_private_chat_users, chatUsersDialogFragment);
            if (Build.VERSION.SDK_INT >= 24) {
                _transaction.commitNowAllowingStateLoss();
            } else {
                _transaction.commitAllowingStateLoss();
            }

        } else {

            $.id(R.id.dialog_private_chat_users).gone();
            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
            transaction.remove(chatUsersDialogFragment);
            if (Build.VERSION.SDK_INT >= 24) {
                transaction.commitNowAllowingStateLoss();
            } else {
                transaction.commitAllowingStateLoss();
            }
            chatUsersDialogFragment = null;

            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (!imm.isActive())
                imm.showSoftInput($.id(R.id.dialog_message_send_et).view(), InputMethodManager.SHOW_IMPLICIT);
        }
    }

    @Override
    public void showEmojiPanel() {
        if (emojiFragment == null && chatUsersDialogFragment == null) {
            // hide keyborad
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm.isActive()) {//isOpen若返回true，则表示输入法打开
                imm.hideSoftInputFromWindow($.id(R.id.dialog_message_send_et).view().getWindowToken(), 0);
            }

            contentView.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if ($ == null) return;
                    $.id(R.id.dialog_message_send_emoji).visible();
                    emojiFragment = EmojiFragment.newInstance();
                    emojiFragment.setCallBack(new EmojiSelectCallBack() {
                        @Override
                        public void onEmojiSelected(IExpressionModel emoji) {
                            if (presenter.isPrivateChat()) {
                                presenter.sendMessageToUser("[" + emoji.getKey() + "]");
                            } else {
                                presenter.sendEmoji("[" + emoji.getKey() + "]");
                            }
                        }
                    });
                    FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
                    transaction.add(R.id.dialog_message_send_emoji, emojiFragment);
                    transaction.commitAllowingStateLoss();
                }
            }, 100);

        } else if (chatUsersDialogFragment != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(INPUT_METHOD_SERVICE);
            if (imm.isActive()) {//isOpen若返回true，则表示输入法打开
                imm.hideSoftInputFromWindow($.id(R.id.dialog_message_send_et).view().getWindowToken(), 0);
            }

            $.id(R.id.dialog_private_chat_users).gone();
            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
            transaction.remove(chatUsersDialogFragment);
            if (Build.VERSION.SDK_INT >= 24) {
                transaction.commitNowAllowingStateLoss();
            } else {
                transaction.commitAllowingStateLoss();
            }
            chatUsersDialogFragment = null;

            $.id(R.id.dialog_message_send_emoji).visible();
            emojiFragment = EmojiFragment.newInstance();
            emojiFragment.setCallBack(new EmojiSelectCallBack() {
                @Override
                public void onEmojiSelected(IExpressionModel emoji) {
                    if (presenter.isPrivateChat()) {
                        presenter.sendMessageToUser("[" + emoji.getKey() + "]");
                    } else {
                        presenter.sendEmoji("[" + emoji.getKey() + "]");
                    }
                }
            });
            FragmentTransaction _transaction = getChildFragmentManager().beginTransaction();
            _transaction.add(R.id.dialog_message_send_emoji, emojiFragment);
            _transaction.commitAllowingStateLoss();

        } else {

            $.id(R.id.dialog_message_send_emoji).gone();
            FragmentTransaction transaction = getChildFragmentManager().beginTransaction();
            transaction.remove(emojiFragment);
            if (Build.VERSION.SDK_INT >= 24) {
                transaction.commitNowAllowingStateLoss();
            } else {
                transaction.commitAllowingStateLoss();
            }
            emojiFragment = null;

            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput($.id(R.id.dialog_message_send_et).view(), InputMethodManager.SHOW_IMPLICIT);
        }
    }

    @Override
    public void onPictureSend() {
        dismissAllowingStateLoss();
    }

    private class MessageTextWatcher implements android.text.TextWatcher {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {

        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (TextUtils.isEmpty(s)) {
                $.id(R.id.dialog_message_send_btn).enable(false);
            } else {
                $.id(R.id.dialog_message_send_btn).enable(true);
            }
        }

        @Override
        public void afterTextChanged(Editable s) {

        }
    }
}
