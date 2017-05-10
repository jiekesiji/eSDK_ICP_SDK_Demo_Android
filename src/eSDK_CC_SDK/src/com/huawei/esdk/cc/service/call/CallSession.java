package com.huawei.esdk.cc.service.call;


import tupsdk.TupCall;

/**
 * 会话管理
 */
public class CallSession
{

    private CallManager callManager = null;

    private TupCall tupCall;

    private boolean userHangup = false;

    /**
     * 构造
     *
     * @param callManager callManager
     */
    public CallSession(CallManager callManager)
    {
        this.callManager = callManager;
    }

    public TupCall getTupCall()
    {
        return tupCall;
    }

    public void setTupCall(TupCall tupCall)
    {
        this.tupCall = tupCall;
    }

    /**
     * 挂断
     *
     * @param isBusy isBusy
     */
    public void hangUp(boolean isBusy)
    {
        setUserHangup(true);
        this.getTupCall().endCall();
    }

    public void setUserHangup(boolean userHangup)
    {
        this.userHangup = userHangup;
    }

    public String getSessionId()
    {
        return tupCall.getCallId() + "";
    }

    /**
     * 静音
     *
     * @param type 类型 -1：扬声器和麦克风， 0：麦克风， 1：扬声器
     * @param mute mute
     * @return int
     */
    public int mute(int type, boolean mute)
    {
        int result;
        if (type == 0)
        {
            result = tupCall.mediaMuteMic(mute ? 1 : 0);
        }
        else
        {
            result = tupCall.mediaMuteSpeak(mute ? 1 : 0);
        }
        return result;
    }


}
