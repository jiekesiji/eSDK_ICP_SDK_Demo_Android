package com.huawei.esdk.cc.service;


import com.huawei.esdk.cc.service.conference.ConferenceMgr;
import com.huawei.esdk.cc.service.ics.ICSService;

/**
 * Created on 2016/3/21.
 */
public final class ServiceManager
{
    private static final Object LOCKOBJECT = new Object();
    private static ServiceManager instance;


    private ServiceManager()
    {
    }

    /**
     * MobileCC的单例
     *
     * @return ServiceManager
     */
    public static ServiceManager getInstance()
    {
        synchronized (LOCKOBJECT)
        {
            if (null == instance)
            {
                instance = new ServiceManager();
            }
            return instance;
        }

    }

    /**
     * 初始化SDK
     *
     * @param sipServerType sipServerType
     */
    public void initService(int sipServerType)
    {
        ICSService.getInstance().initService(sipServerType);
    }


    /**
     * 登录
     *
     * @param name name
     */
    public void login(String name)
    {
        ICSService.getInstance().login(name);
    }

    /**
     * 注销
     */
    public void logout()
    {
        ICSService.getInstance().logout();
    }

    /**
     * 获取文字连接
     *
     * @param accessCode accessCode
     * @param caller     caller
     * @param callType   callType
     * @param callData   callData
     * @param varifyCode  varifyCode
     */
    public void getTextConnect(String accessCode, String caller, String callType, String callData, String varifyCode)
    {
        ICSService.getInstance().textConnect(accessCode, caller, callType, callData, varifyCode);

    }

    /**
     * 发消息
     *
     * @param content content
     */
    public void sendMsg(String content)
    {
        ICSService.getInstance().sendMsg(content);
    }

    /**
     * 获取排队信息
     */
    public void getCallQueueInfo()
    {
        ICSService.getInstance().getCallQueueInfo();
    }

    /**
     * 获取语音连接
     *
     * @param accessCode accessCode
     * @param callData callData
     */
    public void audioConnect(String accessCode, String callData)
    {
        ICSService.getInstance().msAudioConnect(accessCode, callData);
    }

    /**
     * 申请会议
     *
     * @param callId callId
     */
    public void applyMeeting(String callId)
    {
        ICSService.getInstance().updateVideo(callId);

    }

    /**
     * 新建以及加入会议
     */
    public void joinConference()
    {
        ConferenceMgr.getInstance().initConf();
        ConferenceMgr.getInstance().joinConference();
    }


    /**
     * 结束会议
     *
     * @param confId confId
     * @return boolean
     */
    public boolean stopConf(String confId)
    {
        ICSService.getInstance().stopConf(confId);
        return true;
    }

    /**
     * 释放呼叫
     */
    public void dropCall()
    {
        ICSService.getInstance().dropCall();
    }

    /**
     * 取消排队
     */
    public void cancelQueue()
    {
        ICSService.getInstance().dropCall();
    }

    /**
     * 释放文字连接
     */
    public void dropTextCall()
    {
        ICSService.getInstance().dropTextCall();
    }

    /**
     * 获取验证码
     */
    public void getVerifyCode()
    {
        ICSService.getInstance().getVerifyCode();
    }
}
