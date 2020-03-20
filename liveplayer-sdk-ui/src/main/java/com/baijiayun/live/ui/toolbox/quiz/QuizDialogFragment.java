package com.baijiayun.live.ui.toolbox.quiz;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.os.Build;
import android.os.Bundle;
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
import com.baijiayun.live.ui.utils.JsonObjectUtil;
import com.baijiayun.live.ui.utils.QueryPlus;
import com.baijiayun.livecore.LiveSDK;
import com.baijiayun.livecore.context.LPConstants;
import com.baijiayun.livecore.models.LPJsonModel;
import com.baijiayun.livecore.models.imodels.IUserModel;
import com.baijiayun.livecore.utils.LPLogger;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by wangkangfei on 17/5/31.
 */

public class QuizDialogFragment extends BaseDialogFragment implements QuizDialogContract.View {
    public static final String KEY_FORCE_JOIN = "key_force_join";
    private static final String argsType = "1";

    private QueryPlus $;
    private static final String windowName = "bjlapp";
    private QuizDialogContract.Presenter presenter;
    private IUserModel currentUserInfo;
    private String quizId;
    private String roomId;
    private String roomToken;
    private List<LPJsonModel> signalList;
    private boolean forceJoin;
    private boolean isUrlLoaded;
    private boolean isLoadFailed = false; //url load失败了也会调到onPageFinished
    private boolean isDestroyed = false;
    private WebView mWebView;

//    private static final String str = "(function() {\n" +
//            "    var bjlapp = this.bjlapp = this.bjlapp || {};\n" +
//            "    bjlapp.sendMessage = function(json) {\n" +
//            "        bjlapp.sendMessageString(JSON.stringify(json));\n" +
//            "    };\n" +
//            "})();";

    public QuizDialogFragment() {
        if (signalList == null) {
            signalList = new ArrayList<>();
        } else {
            signalList.clear();
        }
    }

    @Override
    protected int getLayoutId() {
        return R.layout.bjy_fragment_quiz;
    }

    @Override
    protected void init(Bundle savedInstanceState, Bundle arguments) {
        $ = QueryPlus.with(contentView);
        title(getString(R.string.live_quiz_title));
        Bundle args = getArguments();
        forceJoin = args.getBoolean(KEY_FORCE_JOIN);
        initWebClient();
        loadUrl();
    }

    //是否显示关闭按钮
    public void setCloseBtnStatus(boolean forceJoin) {
        if(presenter == null) return;
        if (!forceJoin) {
            if (isQueryPlusNull()) return;
            editable(true);
            editText(getString(R.string.live_quiz_close));
            editClick(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showCloseDlg();
                }
            });
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

    @SuppressLint("SetJavaScriptEnabled")
    private void initWebClient() {
        mWebView = ((WebView) $.id(R.id.wv_quiz_main).view());
        mWebView.getSettings().setJavaScriptEnabled(true);
        mWebView.addJavascriptInterface(this, windowName);
        mWebView.setVerticalScrollBarEnabled(false);

//
//        mWebView.getSettings().setDatabaseEnabled(true);
//        mWebView.getSettings().setDomStorageEnabled(true);
//        mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);
//        mWebView.getSettings().setAppCacheEnabled(true);

        mWebView.getSettings().setDomStorageEnabled(true);
        mWebView.getSettings().setAppCacheMaxSize(1024 * 1024 * 8);
        String appCachePath = getActivity().getApplicationContext().getCacheDir().getAbsolutePath();
        mWebView.getSettings().setAppCachePath(appCachePath);
        mWebView.getSettings().setAllowFileAccess(true);
        mWebView.getSettings().setAppCacheEnabled(true);
        mWebView.getSettings().setCacheMode(WebSettings.LOAD_DEFAULT);

        //chrome client
        mWebView.setWebChromeClient(new WebChromeClient() {
            @Override
            public void onProgressChanged(WebView view, int newProgress) {
                ((ProgressBar) $.id(R.id.pb_web_view_quiz).view()).setProgress(newProgress);
                super.onProgressChanged(view, newProgress);
            }
        });
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        //webView client
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                $.id(R.id.pb_web_view_quiz).visible();
                setCloseBtnStatus(forceJoin);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if(isDestroyed) return;
                isUrlLoaded = true;
                $.id(R.id.pb_web_view_quiz).gone();
                if (!isLoadFailed) {
                    setCloseBtnStatus(forceJoin);
                }
                isLoadFailed = false;

                callJsInQueue();
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
//                super.onReceivedSslError(view, handler, error);
                handler.proceed();
            }

            @TargetApi(Build.VERSION_CODES.M)
            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                // code -11 net::ERR_BAD_SSL_CLIENT_AUTH_CERT 忽略https证书问题
                if(error.getErrorCode() != -11) {
                    $.id(R.id.pb_web_view_quiz).gone();
                    setCloseBtnStatus(forceJoin);
                    isLoadFailed = true;
                }
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                super.onReceivedError(view, errorCode, description, failingUrl);
                if(errorCode != -11){
                    $.id(R.id.pb_web_view_quiz).gone();
                    setCloseBtnStatus(forceJoin);
                    isLoadFailed = true;
                }
            }
        });
    }

    private void loadUrl() {
        try {
            roomToken = URLEncoder.encode(roomToken, "utf-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        String params = "?userNumber=" + currentUserInfo.getNumber() + "&userName=" + currentUserInfo.getName() + "&quizId=" + quizId
                + "&roomId=" + roomId + "&token=" + roomToken + "&argType=" + argsType + "&userGroup=" + currentUserInfo.getGroup();
        String host = LPConstants.HOSTS_WEB[LiveSDK.getDeployType().getType()];
        String url = host.concat("m/quiz/student").concat(params);
        LPLogger.i(getClass().getSimpleName() + " : " + url);
        ((WebView) $.id(R.id.wv_quiz_main).view()).loadUrl(url);
    }


    @Override
    protected void setWindowParams(WindowManager.LayoutParams windowParams) {
        windowParams.width = WindowManager.LayoutParams.MATCH_PARENT;
        windowParams.height = WindowManager.LayoutParams.MATCH_PARENT;
        windowParams.gravity = Gravity.CENTER;
        windowParams.x = 0;
        windowParams.y = 0;

        windowParams.windowAnimations = R.style.LiveBaseSendMsgDialogAnim;
    }

    @Override
    public void setPresenter(QuizDialogContract.Presenter presenter) {
        super.setBasePresenter(presenter);
        this.presenter = presenter;
        presenter.getCurrentUser();
        roomToken = TextUtils.isEmpty(presenter.getRoomToken()) ? "" : presenter.getRoomToken();
    }

    @Override
    public void onStartArrived(LPJsonModel jsonModel) {
        String key = jsonModel.data.get("message_type").getAsString();
        if (!"quiz_start".equals(key)) {
            return;
        }
        if (JsonObjectUtil.isJsonNull(jsonModel.data, "quiz_id")) {
            quizId = "";
        } else {
            quizId = jsonModel.data.get("quiz_id").getAsString();
        }
        if (JsonObjectUtil.isJsonNull(jsonModel.data, "class_id")) {
            roomId = "";
        } else {
            roomId = jsonModel.data.get("class_id").getAsString();
        }
        signalList.add(jsonModel);
    }

    @Override
    public void onEndArrived(LPJsonModel jsonModel) {
        String key = jsonModel.data.get("message_type").getAsString();
        if (!"quiz_end".equals(key)) {
            return;
        }
        signalList.add(jsonModel);
        /*只有load完之后才去转发h5，否则加入队列，load完成后依次转发*/
        if (isUrlLoaded) {
            callJs(jsonModel.data.toString());
        }
    }

    @Override
    public void onSolutionArrived(LPJsonModel jsonModel) {
        String key = jsonModel.data.get("message_type").getAsString();
        if (!"quiz_solution".equals(key)) {
            return;
        }
        if (JsonObjectUtil.isJsonNull(jsonModel.data, "quiz_id")) {
            quizId = "";
        } else {
            quizId = jsonModel.data.get("quiz_id").getAsString();
        }
        if (JsonObjectUtil.isJsonNull(jsonModel.data, "class_id")) {
            roomId = "";
        } else {
            roomId = jsonModel.data.get("class_id").getAsString();
        }
        signalList.add(jsonModel);
    }

    @Override
    public void onQuizResArrived(LPJsonModel jsonModel) {
        String key = jsonModel.data.get("message_type").getAsString();
        if (!"quiz_res".equals(key)) {
            return;
        }
        if (JsonObjectUtil.isJsonNull(jsonModel.data, "quiz_id")) {
            quizId = "";
        } else {
            quizId = jsonModel.data.get("quiz_id").getAsString();
        }
        if (JsonObjectUtil.isJsonNull(jsonModel.data, "class_id")) {
            roomId = "";
        } else {
            roomId = jsonModel.data.get("class_id").getAsString();
        }
        signalList.add(jsonModel);
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
        mWebView.destroy();
        super.onDestroy();
        isDestroyed = true;
        presenter = null;
        if (signalList != null) {
            signalList.clear();
        }
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
        ((WebView) $.id(R.id.wv_quiz_main).view()).loadUrl("javascript:bjlapp.receivedMessage(" + json + ")");
    }
}
