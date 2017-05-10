package com.huawei.esdk.icsdemo.application;

import android.app.Application;

import com.huawei.esdk.cc.MobileCC;

/**
 * Created on 2015/12/28.
 */
public class CCApplication extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();
        MobileCC.getInstance().initSDK(this);
        MobileCC.getInstance().setLog("CCTUPLOG", 3);
        MobileCC.getInstance().setAnonymousCard("AnonymousCard");
    }

    @Override
    public void onTerminate()
    {
        super.onTerminate();
        MobileCC.getInstance().unInitSDK(); // 停止SDK服务
    }
}
