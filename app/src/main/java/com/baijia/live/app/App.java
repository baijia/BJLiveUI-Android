package com.baijia.live.app;

import android.app.Application;

import com.tencent.bugly.crashreport.CrashReport;

/**
 * Created by Shubo on 2017/4/21.
 */

public class App extends Application{

    @Override
    public void onCreate() {
        super.onCreate();
        // catch捕获的异常
        CrashReport.initCrashReport(getApplicationContext(), "ce7b872000", false);

//        LiveSDK.deployType = LPConstants.LPDeployType.Test;
    }
}
