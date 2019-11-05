package com.baijiayun.live.ui.toolbox.evaluation;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.baijiayun.live.ui.R;
import com.baijiayun.live.ui.base.BaseDialogFragment;
import com.baijiayun.live.ui.utils.Precondition;
import com.baijiayun.live.ui.utils.QueryPlus;
import com.baijiayun.livecore.LiveSDK;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.models.LPJsonModel;
import com.baijiayun.livecore.models.imodels.IUserModel;
import com.baijiayun.livecore.utils.DisplayUtils;
import com.baijiayun.livecore.utils.LPLogger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangkangfei on 17/5/31.
 */

public class EvaDialogFragment extends BaseDialogFragment implements EvaDialogContract.View {
    private static final String argsType = "1";
    private QueryPlus $;
    private static final String windowName = "bjlapp";
    private EvaDialogContract.Presenter presenter;
    private IUserModel currentUserInfo;
    private String roomId;
    private String roomToken;
    private boolean isUrlLoaded;
    private boolean isLoadFailed = false; //url load失败了也会调到onPageFinished
    private boolean isDestroyed = false;
    private List<LPJsonModel> signalList;
    public static final int WIDTH_MARGIN = 24;
    public static final int HEIGHT_MARGIN = 32;
    public static final int HEIGHT_MARGIN_LANDSCAPE = 48;
    private float downY;
    private WebView webView;

    public EvaDialogFragment() {
        signalList = new ArrayList<>();
    }

    @Override
    protected int getLayoutId() {
        return R.layout.fragment_eva;
    }

    @Override
    protected void init(Bundle savedInstanceState, Bundle arguments) {
        $ = QueryPlus.with(contentView);
        hideTitleBar();
        initWebClient();
        loadUrl();
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Dialog dialog = new Dialog(getActivity(), R.style.DialogStyle);
        Precondition.checkNotNull(dialog.getWindow());
        dialog.setCanceledOnTouchOutside(false);
        return dialog;
    }

    //是否显示关闭按钮
    public void setCloseBtnStatus(boolean forceJoin) {
        if (presenter == null) return;
        if (!forceJoin) {
            if (isQueryPlusNull()) return;
            editable(true);
            editText(getString(R.string.live_quiz_close));
            editClick(v -> showCloseDlg());
        } else {
            editable(false);
        }
    }

    private void showCloseDlg() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        AlertDialog dialog = builder.setMessage(R.string.live_quiz_dialog_tip)
                .setPositiveButton(R.string.live_quiz_dialog_confirm, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                        if (presenter != null)
                            presenter.dismissDlg();
                    }
                }).setNegativeButton(R.string.live_quiz_dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(getResources().getColor(R.color.live_blue));
        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(getResources().getColor(R.color.live_blue));
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        keepAboveKeyboard(view);
    }

    private void keepAboveKeyboard(final View root) {
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            Rect rect = new Rect();
            root.getWindowVisibleDisplayFrame(rect);
            int rootInvisibleHeight = root.getRootView().getBottom() - rect.bottom;
            //认定大于200弹出的是软键盘
            if (rootInvisibleHeight > 150) {
                int srollHeight = (int) (downY + 150 - rect.bottom);
                if (srollHeight > 0) {
                    root.scrollTo(0, srollHeight);
                } else {
                    root.scrollTo(0, 0);
                }
            } else {
                root.scrollTo(0, 0);
            }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Precondition.checkNotNull(getDialog().getWindow());
        getDialog().getWindow().setBackgroundDrawableResource(R.drawable.shape_bg_radius_6dp);
    }

    @SuppressLint({"SetJavaScriptEnabled", "ClickableViewAccessibility"})
    private void initWebClient() {
        webView = ((WebView) $.id(R.id.wv_eva_main).view());
        webView.setOnTouchListener((v, event) -> {
            downY = event.getRawY();
            return false;
        });
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setLayerType(View.LAYER_TYPE_HARDWARE,null);
        webView.addJavascriptInterface(this, windowName);
        webView.setVerticalScrollBarEnabled(false);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setAppCacheMaxSize(1024 * 1024 * 8);
        String appCachePath = getActivity().getApplicationContext().getCacheDir().getAbsolutePath();
        webView.getSettings().setAppCachePath(appCachePath);
        webView.getSettings().setAllowFileAccess(true);
        webView.getSettings().setAppCacheEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
        webView.setBackgroundColor(0);
        //chrome client
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                ((ProgressBar) $.id(R.id.pb_web_view_eva).view()).setProgress(newProgress);
                super.onProgressChanged(view, newProgress);
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        //webView client
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                $.id(R.id.pb_web_view_eva).visible();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (isDestroyed) return;
                isUrlLoaded = true;
                $.id(R.id.pb_web_view_eva).gone();
                isLoadFailed = false;
//                injectJs();
                callJsInQueue();
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                // code -11 net::ERR_BAD_SSL_CLIENT_AUTH_CERT 忽略https证书问题
                if (error.getErrorCode() != -11) {
                    $.id(R.id.pb_web_view_eva).gone();
                    setCloseBtnStatus(false);
                    isLoadFailed = true;
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                if (errorCode != -11) {
                    $.id(R.id.pb_web_view_eva).gone();
                    setCloseBtnStatus(false);
                    isLoadFailed = true;
                }
            }
        });
    }

    private void injectJs() {
        String backgroundStr = "javascript: function changeBackground() { " +
                "        document.getElementById('main').style.background ='transparent';" +
                "        document.getElementById('main').parentNode.style.background = 'transparent';" +
                "}";
        webView.loadUrl(backgroundStr);
        webView.loadUrl("javascript: changeBackground();");
    }

    private void loadUrl() {
        try {
            roomToken = URLEncoder.encode(roomToken, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String params = "?userNumber=" + currentUserInfo.getNumber() + "&userName=" + currentUserInfo.getName() + "&roomId=" + roomId +
                "&token=" + roomToken + "&argType=" + argsType + "&userGroup=" + currentUserInfo.getGroup();
        String host = LPConstants.HOSTS_WEB[LiveSDK.getDeployType().getType()];
        String url = host.concat("m/evaluation/index").concat(params);
        LPLogger.i(getClass().getSimpleName() + " : " + url);
        webView.loadUrl(url);
    }


    @Override
    protected void setWindowParams(WindowManager.LayoutParams windowParams) {
        windowParams.windowAnimations = R.style.LiveBaseSendMsgDialogAnim;
        windowParams.gravity = Gravity.CENTER;
        int height = DisplayUtils.getScreenHeightPixels(getContext()) - DisplayUtils.getStatusBarHeight(getActivity());
        int width = DisplayUtils.getScreenWidthPixels(getContext());
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            //竖屏
            windowParams.width = width - DisplayUtils.dip2px(getContext(), WIDTH_MARGIN);
            windowParams.height = height - DisplayUtils.dip2px(getContext(), HEIGHT_MARGIN);
        } else {
            //横屏
            windowParams.width = WindowManager.LayoutParams.WRAP_CONTENT;
            windowParams.height = Math.min(height,width) - DisplayUtils.dip2px(getContext(), HEIGHT_MARGIN_LANDSCAPE);
        }
    }

    @Override
    public void setPresenter(EvaDialogContract.Presenter presenter) {
        super.setBasePresenter(presenter);
        this.presenter = presenter;
        presenter.getCurrentUser();
        roomToken = presenter.getRoomToken();
        roomId = String.valueOf(presenter.getRoomId());
    }

    @Override
    public void onClassEnd(LPJsonModel jsonModel) {
        if (signalList != null) {
            signalList.add(jsonModel);
        }
    }

    @Override
    public void onGetCurrentUser(IUserModel userModel) {
        this.currentUserInfo = userModel;
    }

    @Override
    public void dismissDlg() {
        presenter.dismissDlg();
    }

    /**
     * 逐个给js转发信令
     */
    private void callJsInQueue() {
        if (signalList != null && signalList.size() > 0) {
            for (LPJsonModel jsonModel : signalList) {
                callJs(jsonModel.data.toString());
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isDestroyed = true;
        if (signalList != null) {
            signalList.clear();
        }
        if (webView != null) {
            webView.destroy();
        }
        presenter = null;
    }

    /******************
     * js call android
     ******************/
    @JavascriptInterface
    public void close() {
        if (presenter.checkRouterNull()) return;
        presenter.dismissDlg();
    }

    @JavascriptInterface
    public void sendMessage(String json) {
        if (!TextUtils.isEmpty(json)) {
            presenter.submitAnswer(json);
        }
    }
    /*************
     * android call js
     ************/
    private void callJs(String json) {
        webView.loadUrl("javascript:bjlapp.receivedMessage(" + json + ")");
    }
}
