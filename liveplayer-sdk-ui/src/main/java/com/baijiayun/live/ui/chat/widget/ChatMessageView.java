package com.baijiayun.live.ui.chat.widget;

import android.annotation.SuppressLint;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v4.view.GestureDetectorCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.Gravity;
import android.view.MotionEvent;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.utils.DisplayUtils;
import com.baijiayun.live.ui.utils.RxUtils;

import java.util.Locale;
import java.util.concurrent.TimeUnit;

import io.reactivex.Observable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

/**
 * Created by Dujuncan on 2019/2/20
 */
public class ChatMessageView extends LinearLayout {

    private final String TAG = ChatMessageView.class.getCanonicalName();
    public static final String MARK = "-@translate@-";

    private TextView tvMsg;
    private TextView tvTranslateResult;

    private OnProgressListener onProgressListener;
    private OnFilterListener onFilterListener;
    private String message;
    private boolean isEnableTranslation;
    private boolean isTranslate;
    private boolean isFailed;
    private boolean enableFilter;//消息是老师/助教的可以显示过滤选项
    private boolean isFiltered;//已显示过滤选项
    private Disposable subscribeTimer;

    private GestureDetectorCompat gestureDetectorCompat;

    public ChatMessageView(Context context) {
        this(context, null);
    }

    public ChatMessageView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChatMessageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
        initListener();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public ChatMessageView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        init(context, attrs);
        initListener();
    }

    @SuppressLint("ClickableViewAccessibility")
    private void init(Context context, AttributeSet attrs) {
        setOrientation(LinearLayout.VERTICAL);
        GradientDrawable drawable = new GradientDrawable();
        drawable.setSize(100, DisplayUtils.dip2px(getContext(), 1));
        drawable.setColor(Color.parseColor("#D8D8D8"));
        setDividerDrawable(drawable);
        setShowDividers(LinearLayout.SHOW_DIVIDER_MIDDLE);

        tvMsg = new TextView(getContext());
        tvMsg.setTextColor(getResources().getColor(R.color.primary_text));
        tvMsg.setOnTouchListener((v, event) -> {
            gestureDetectorCompat.onTouchEvent(event);
            return false;
        });
        addView(tvMsg);

        tvTranslateResult = new TextView(getContext());
        tvTranslateResult.setTextColor(Color.parseColor("#804A4A4A"));

    }

    private void initListener() {
        gestureDetectorCompat = new GestureDetectorCompat(getContext(), new LongPressListener());
    }

    public void enableTranslation(boolean isEnable) {
        isEnableTranslation = isEnable;
    }

    public void enableFilter(boolean enableFilter) {
        this.enableFilter = enableFilter;
    }

    public void setFiltered(boolean filtered) {
        isFiltered = filtered;
    }

    private void showMenu(int x, int y) {
        PopupWindow popupWindow = new PopupWindow(getContext());
        popupWindow.setFocusable(true);
        popupWindow.setWidth(DisplayUtils.dip2px(getContext(), 64));
        popupWindow.setBackgroundDrawable(new ColorDrawable(0));

        String ITEMS[];
        if (isEnableTranslation && !isTranslate) {
            if (enableFilter && !isFiltered) {
                ITEMS = new String[]{"复制", "翻译","只看老师/助教"};
                popupWindow.setWidth(DisplayUtils.dip2px(getContext(), 128));
                popupWindow.setHeight(DisplayUtils.dip2px(getContext(), 128));
            } else {
                ITEMS = new String[]{"复制", "翻译"};
                popupWindow.setHeight(DisplayUtils.dip2px(getContext(), 88));
            }
        } else {
            if (enableFilter && !isFiltered) {
                ITEMS = new String[]{"复制","只看老师/助教"};
                popupWindow.setWidth(DisplayUtils.dip2px(getContext(), 128));
                popupWindow.setHeight(DisplayUtils.dip2px(getContext(), 88));
            } else {
                ITEMS = new String[]{"复制"};
                popupWindow.setHeight(DisplayUtils.dip2px(getContext(), 48));
            }
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), R.layout.menu_chat_message, ITEMS);
        ListView listView = new ListView(getContext());

        GradientDrawable bgDrawable = new GradientDrawable();
        bgDrawable.setColor(Color.WHITE);
        bgDrawable.setCornerRadius(DisplayUtils.dip2px(getContext(), 4));
        listView.setBackground(bgDrawable);
        listView.setAdapter(adapter);
        listView.setDividerHeight(0);
        listView.setPadding(0, DisplayUtils.dip2px(getContext(), 4), 0, DisplayUtils.dip2px(getContext(), 4));
        listView.setOnItemClickListener((parent, view, position, id) -> {
            switch (position) {
                case 0:
                    copyMessage(message);
                    break;
                case 1:
                    if (isEnableTranslation && !isTranslate) {
                        if (onProgressListener == null) return;
                        onProgressListener.onProgress();
                        isFailed = false;
                        countDown();
                        Log.d(TAG, "translate =" + message);
                    } else {
                        if (!isFiltered && enableFilter && onFilterListener != null) {
                            onFilterListener.onFilter();
                        }
                    }
                    break;
                case 2:
                    if (!isFiltered && enableFilter && onFilterListener != null) {
                        onFilterListener.onFilter();
                    }
                    break;
            }
            popupWindow.dismiss();
        });
        popupWindow.setContentView(listView);
        int pos[] = new int[2];
        getLocationOnScreen(pos);
        popupWindow.showAtLocation(this, Gravity.NO_GRAVITY, x - popupWindow.getWidth() / 2, y - popupWindow.getHeight());
    }

    private void copyMessage(String content) {
        ClipboardManager clipboardManager = (ClipboardManager) getContext().getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clipData = ClipData.newPlainText("copy", content);
        clipboardManager.setPrimaryClip(clipData);
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void addTranslateMessage(String translateMessage) {
        if (isFailed) return;
        if (getChildCount() > 1 || translateMessage.equals("")) return;
        if (subscribeTimer != null && !subscribeTimer.isDisposed()) {
            subscribeTimer.dispose();
        }
        if (translateMessage.endsWith("\n")) {
            translateMessage = translateMessage.substring(0, translateMessage.length() - 1);
        }
        tvTranslateResult.setText(translateMessage);
        addView(tvTranslateResult);

        isTranslate = true;
        Log.d(TAG, "addTranslateMessage: message=" + message + "..........show translate =" + translateMessage);
    }

    public TextView getTextViewChat() {
        return tvMsg;
    }

    @SuppressLint("CheckResult")
    private void countDown() {
        //翻译时就开始倒计时，如果倒计时自然结束，显示失败，并打上失败的标签。
        //再次翻译时，再清空标签，重新开始倒计时。
        subscribeTimer = Observable.timer(5000, TimeUnit.MILLISECONDS)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(aLong -> {
                    Log.d(TAG, "countDown: 倒计时完成");
                    tvTranslateResult.setText(Locale.getDefault().getCountry().equalsIgnoreCase("cn") ? "翻译失败" : "Translate Fail!");
                    if (getChildCount() <= 1) {
                        addView(tvTranslateResult);
                    }
                    isFailed = true;
                });
    }
    public interface OnFilterListener {
        void onFilter();
    }

    public void setOnFilterListener(OnFilterListener onFilterListener) {
        this.onFilterListener = onFilterListener;
    }

    public interface OnProgressListener {
        void onProgress();
    }

    public void setOnProgressListener(OnProgressListener onProgressListener) {
        this.onProgressListener = onProgressListener;
    }

    class LongPressListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public void onLongPress(MotionEvent e) {
            super.onLongPress(e);
            showMenu((int) e.getRawX(), (int) e.getRawY());
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        gestureDetectorCompat.onTouchEvent(event);
        return true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        RxUtils.dispose(subscribeTimer);
        subscribeTimer = null;
    }
}
