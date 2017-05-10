package com.huawei.esdk.cc.service.call;

import android.os.Handler;
import android.os.Looper;

import com.huawei.esdk.cc.video.VideoControl;

/**
 * Created on 2016/2/3.
 */
public final class VoipFunction
{
    /**
     * 初始状态
     */
    public static final int STATUS_INIT = 0;

    private static VoipFunction instance = null;
    /**
     * Voip状态
     */
    private int voipStatus = STATUS_INIT;
    private CallSession callSession;
    /**
     * 是否是视频通话
     **/
    private boolean isVideo = false;

    private VoipFunction()
    {
        initBroadcasts();
    }

    /**
     * @return VoipFunction
     */
    public static VoipFunction getInstance()
    {

        if (null == instance)
        {
            instance = new VoipFunction();
        }
        return instance;
    }

    private void initBroadcasts()
    {
    }

    public void setCallSession(CallSession callSession)
    {
        this.callSession = callSession;
    }

    /**
     * prepareVideoCall
     */
    public void prepareVideoCall()
    {
        // 必须放到ui线程来执行.
        new Handler(Looper.getMainLooper()).post(new Runnable()
        {
            @Override
            public void run()
            {
                if (callSession != null)
                {
                    VideoControl.getIns().deploySessionVideoCaps();
                }
            }
        });
    }

    private void clearAfterVideoCallEnd()
    {
        VideoControl.getIns().clearSurfaceView();
    }

    /**
     * @param isVideo isVideo
     */
    public void setVideo(boolean isVideo)
    {
        if (isVideo && callSession != null)
        {
            VideoControl.getIns().setCallId(callSession.getSessionId());
        }

        if (isVideo == this.isVideo)
        {
            return;
        }

        if (isVideo)
        {
            prepareVideoCall();
        }
        else
        {
            clearAfterVideoCallEnd();
        }
        this.isVideo = isVideo;
    }
}
