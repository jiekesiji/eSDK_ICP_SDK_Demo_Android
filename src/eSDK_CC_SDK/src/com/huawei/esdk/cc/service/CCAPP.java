package com.huawei.esdk.cc.service;

import android.app.Application;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.huawei.esdk.cc.common.BroadMsg;
import com.huawei.esdk.cc.common.NotifyMessage;

/**
 * Created on 2015/12/28.
 */
public final class CCAPP
{

    private static CCAPP ins = new CCAPP();
    private Application app;
    private LocalBroadcastManager localBroadcastManager;

    private CCAPP()
    {

    }

    public static CCAPP getInstances()
    {
        return ins;
    }

    public Application getApplication()
    {
        return app;
    }

    /**
     * 传入application
     *
     * @param application application
     */
    public void initApp(Application application)
    {
        if (application != null)
        {
            app = application;
            localBroadcastManager = LocalBroadcastManager.getInstance(app);
        }
    }

    /**
     * 发送广播
     *
     * @param broadMsg broadMsg
     */
    public void sendBroadcast(BroadMsg broadMsg)
    {
        Intent intent = new Intent(broadMsg.getAction());
        intent.putExtra(NotifyMessage.CC_MSG_CONTENT, broadMsg);
        localBroadcastManager.sendBroadcast(intent);
    }


}
