package com.huawei.esdk.cc.service.call;

import android.os.Environment;
import android.text.TextUtils;

import com.huawei.esdk.cc.MobileCC;
import com.huawei.esdk.cc.common.BroadMsg;
import com.huawei.esdk.cc.common.NotifyMessage;
import com.huawei.esdk.cc.common.RequestCode;
import com.huawei.esdk.cc.common.RequestInfo;
import com.huawei.esdk.cc.service.ServiceConfig;
import com.huawei.esdk.cc.service.CCAPP;
import com.huawei.esdk.cc.utils.LogUtil;
import com.huawei.esdk.cc.utils.StringUtils;
import com.huawei.esdk.cc.service.ics.SystemSetting;
import com.huawei.esdk.cc.utils.AddressTools;
import com.huawei.esdk.cc.video.StreamInfo;
import com.huawei.esdk.cc.video.VideoConfig;

import java.io.File;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import common.HdacceleType;
import common.TupBool;
import common.TupCallParam;
import object.DecodeSuccessInfo;
import object.HdacceleRate;
import object.TupAudioQuality;
import object.TupCallCfgAudioVideo;
import object.TupCallCfgMedia;
import object.TupCallCfgSIP;
import tupsdk.TupCall;
import tupsdk.TupCallManager;

/**
 * 呼叫控制类
 */
public final class CallManager extends CallManagerImpl
{
    private static final Object LOCKOBJECT = new Object();
    private static final String TAG = "CallManager";
    private static final int AUDIO_MIN = 10500;
    private static final int AUDIO_MAX = 10519;
    private static final int VIDEO_MIN = 10580;
    private static final int VIDEO_MAX = 10599;
    private static CallManager instance;
    private boolean initFlag = false;
    private TupCallManager tupManager;
    private TupCallCfgAudioVideo tupCallCfgAudioVideo = null;
    private Map<Integer, CallSession> calls = null;
    private VOIPParams voipConfig = new VOIPParams();
    private CallSession currentCallSession = null;
    private int callId;
    private ServiceConfig serviceConfig = ServiceConfig.getInstance();
    private SystemSetting systemSetting = SystemSetting.getInstance();
    /**
     * 默认的带宽值
     */
    private int datarate = 512;
    /**
     * 视频模式
     */
    private int vMode = 0;

    private CallManager()
    {
        calls = new ConcurrentHashMap();
        tupManager = new TupCallManager(this, CCAPP.getInstances().getApplication());
        //加载需要的库
        tupManager.loadLibForUC();
        loadMeeting();
        tupManager.setAndroidObjects();

        String logFile = "";
        if ("".equals(systemSetting.getLogPath()))
        {
            logFile = Environment.getExternalStorageDirectory().toString()
                    + File.separator + "ICS";
        }
        else
        {
            logFile = Environment.getExternalStorageDirectory().toString()
                    + File.separator + systemSetting.getLogPath();
        }

        File dirFile = new File(logFile);
        if (!(dirFile.exists()) && !(dirFile.isDirectory()))
        {
            if (dirFile.mkdirs())
            {
                logInfo(TAG, "CallManager", "mkdir " + dirFile.getPath());
            }
        }
        int logLevel = systemSetting.getLogLevel();
        tupManager.logStart(logLevel, 5 * 1000, 1, logFile);

        tupCallCfgAudioVideo = new TupCallCfgAudioVideo();
    }

    /**
     * 获取实例
     *
     * @return CallManager
     */
    public static CallManager getInstance()
    {
        synchronized (LOCKOBJECT)
        {
            if (instance == null)
            {
                instance = new CallManager();
            }
            return instance;
        }
    }

    public int getDatarate()
    {
        return datarate;
    }

    public void setDatarate(int datarate)
    {
        this.datarate = datarate;
    }

    /**
     * 获取视频模式
     *
     * @return int
     */
    public int getvMode()
    {
        return vMode;
    }

    /**
     * 设置视屏模式
     *
     * @param vMode vMode
     */
    public void setvMode(int vMode)
    {
        this.vMode = vMode;
    }

    /**
     * 加载会议组件
     *
     * @return boolean
     */
    public boolean loadMeeting()
    {
        System.loadLibrary("TupConf");
        return true;
    }

    /**
     * 获取voip配置
     *
     * @return VOIPParams
     */
    public VOIPParams getVoipConfig()
    {
        return voipConfig;
    }

    /**
     * 配置
     */
    public void tupConfig()
    {
        configCall();
        configMedia();
        configSip();
    }

    /**
     * 配置sip参数
     */
    private void configSip()
    {
        TupCallCfgSIP tupCallCfgSIP = new TupCallCfgSIP();

        tupCallCfgSIP.setSubAuthType(common.TupBool.TUP_TRUE);

        tupCallCfgSIP.setServerRegPrimary(this.getVoipConfig().getServerIp(),
                StringUtils.stringToInt(this.getVoipConfig().getServerPort()));
        tupCallCfgSIP.setServerProxyPrimary(this.getVoipConfig().getServerIp(),
                StringUtils.stringToInt(this.getVoipConfig().getServerPort()));

        String svnIp = StringUtils.getIpAddress();
        tupCallCfgSIP.setNetAddress(svnIp);

        if (systemSetting.isTLSEncoded())
        {
//         设置TLS
            tupCallCfgSIP.setSipTransMode(1); //TLS
        }
        else
        {
            tupCallCfgSIP.setSipTransMode(0);
        }


        // 刷新注册的时间
        tupCallCfgSIP.setSipRegistTimeout(getVoipConfig().getRegExpires());
        // 刷新订阅的时间
        tupCallCfgSIP.setSipSubscribeTimeout(getVoipConfig()
                .getSessionExpires());
        // 会话
        tupCallCfgSIP.setSipSessionTimerEnable(TupBool.TUP_TRUE);
        // 会话超时
        tupCallCfgSIP.setSipSessionTime(90);

        // 设置 DSCP
        tupCallCfgSIP.setDscpEnable(TupBool.TUP_TRUE);
        // 设置 tup 再注册的时间间隔， 注册失败后 间隔再注册
        tupCallCfgSIP.setSipReregisterTimeout(10);

        tupCallCfgSIP.setCheckCSeq(TupBool.TUP_FALSE);

        String localIpAddress = AddressTools.getLocalIpAddress();
        String anonymousNum = systemSetting.getAnonymousCard() + "@" + localIpAddress;

        logInfo(TAG, "configSip", "anonymousNum = " + anonymousNum);

        tupCallCfgSIP.setAnonymousNum(anonymousNum);


        tupManager.setCfgSIP(tupCallCfgSIP);
    }

    private void configCall()
    {
        TupCallCfgMedia c = new TupCallCfgMedia();


        if (systemSetting.isSRTPEncoded())
        {
            c.setMediaSrtpMode(2); //设置SRTP  0不加密 1自动模式  2强制加密
        }
        else
        {
            c.setMediaSrtpMode(0); //设置SRTP  0不加密 1自动模式  2强制加密
        }

        //设置SDP带宽
        c.setCt(getDatarate());
        c.setMediaIframeMethod(TupBool.TUP_TRUE);
        c.setMediaFluidControl(TupBool.TUP_TRUE);
        tupManager.setCfgMedia(c);
    }

    /**
     * TUP初始化
     *
     * @return boolean
     */
    public boolean tupInit()
    {
        if (!initFlag)
        {
            logInfo(TAG, "tupInit", "TupCallManager_call_Init enter");
            tupManager.callInit();
            tupManager.registerReceiver();
            initFlag = true;
            logInfo(TAG, "tupInit", "TupCallManager_Init end");
            return true;
        }

        return false;

    }

    /**
     * 挂断
     *
     * @return boolean
     */
    public boolean releaseCall()
    {
        if (currentCallSession != null)
        {
            currentCallSession.hangUp(false);
            currentCallSession = null;
            return true;
        }
        return false;
    }

    /**
     * 发起匿名呼叫
     *
     * @param number number
     * @return int
     */
    public int makeAnonymousCall(String number)
    {
        tupConfig();
        int callId = this.tupManager.startAnonmousCall(number);
        if (callId != -1)
        {
            TupCall tupCall = new TupCall(callId, 0);
            tupCall.setCaller(true);
            tupCall.setNormalCall(true);
            tupCall.setToNumber(number);
            CallSession iCSCallSession = new CallSession(this);


            iCSCallSession.setTupCall(tupCall);
            calls.put(callId, iCSCallSession);

            currentCallSession = iCSCallSession;
        }

        return callId;
    }

    /**
     * 视频控制
     *
     * @param operation operation
     */
    public void videoControl(int operation)
    {
        int result = tupManager.vedioControl(callId, operation, 0x03);
    }


    /**
     * 发起匿名视屏
     *
     * @param number number
     * @return int
     */
    public int startAnonymousVideoCall(String number)
    {
        tupConfig();
        TupCall call = null;
        call = tupManager.StartAnonymousVideoCall(number);
        if (call == null)
        {
            return -1;
        }
        CallSession iCSCallSession = new CallSession(this);
        iCSCallSession.setTupCall(call);
        VoipFunction.getInstance().setCallSession(iCSCallSession);

        currentCallSession = iCSCallSession;

        VoipFunction.getInstance().setCallSession(currentCallSession);
        VoipFunction.getInstance().setVideo(true);

        this.callId = call.getCallId();
        return call.getCallId();

    }

    /**
     * 视频画面旋转
     *
     * @param rotation rotation
     * @return int
     */
    public int setRotation(int rotation)
    {
        return currentCallSession.getTupCall()
                .setCaptureRotation(serviceConfig.getCameraIndex(), rotation);
    }

    private void configMedia()
    {

        tupCallCfgAudioVideo.setAudioPortRange(AUDIO_MIN, AUDIO_MAX);
        tupCallCfgAudioVideo.setVideoPortRange(VIDEO_MIN, VIDEO_MAX);
        // audioCode ， 区分 3G 和WIFI
        tupCallCfgAudioVideo.setAudioCodec("0,8,18");


        //设置降噪处理
        tupCallCfgAudioVideo.setAudioAnr(1);

        //抗丢包设置
        tupCallCfgAudioVideo.setVideoErrorcorrecting(TupBool.TUP_TRUE);

        tupCallCfgAudioVideo.setAudioAec(1);
        // Dscp
        tupCallCfgAudioVideo.setDscpAudio(getVoipConfig().getAudioDSCP());
        tupCallCfgAudioVideo.setDscpVideo(getVoipConfig().getVideoDSCP());
        // net level
        tupCallCfgAudioVideo.setAudioNetatelevel(3);
        // opus 采样率
        tupCallCfgAudioVideo.setAudioClockrate(getVoipConfig().getOpusSamplingFreq());
        tupCallCfgAudioVideo.setForceIdrInfo(1);

        // 摄像头旋转
        // 可选，设置视频捕获（逆时针旋转）的角度。
        // 仅Android/iOS平台有效。
        // 0：0度；1：90度；2：180度；3：270度；
        // {0,1,2,3}
        tupCallCfgAudioVideo.setVideoCaptureRotation(0);

        tupCallCfgAudioVideo.setVideoCoderQuality(15);
        tupCallCfgAudioVideo.setVideoKeyframeinterval(10);
        tupCallCfgAudioVideo
                .setAudioDtmfMode(TupCallParam.CALL_E_DTMF_MODE.CALL_E_DTMF_MODE_CONST2833);

        tupCallCfgAudioVideo.setVideoFramesize(8, 6, 11);
        HdacceleRate videoHdacceleRate = new HdacceleRate();
        videoHdacceleRate.setEncode(HdacceleType.Other);
        videoHdacceleRate.setDecode(HdacceleType.Other);
        tupCallCfgAudioVideo.setVideoHdaccelerate(videoHdacceleRate);

        int dataval = getDatarate();
        int maxDw = 2000;
        if (dataval <= 128)
        {
            maxDw = 600;
        }
        else
        {
            maxDw = 2000;
        }
        tupCallCfgAudioVideo.setVideoDatarate(dataval, dataval, maxDw, dataval);
        //视频模式
        tupCallCfgAudioVideo.setVideoTactic(getvMode());

        tupCallCfgAudioVideo.setVideoClarityFluencyEnable(TupBool.TUP_TRUE);

        tupManager.setCfgAudioAndVideo(tupCallCfgAudioVideo);
        // 先只配置默认值
        tupManager.setMboileVideoOrient(0, 1, 1, 0, 0, 0);

    }


    /**
     * 设置视屏参数
     *
     * @param caps caps
     * @return int
     */
    public int setOrientParams(VideoConfig caps)
    {
        if (caps == null)
        {
            return -1;
        }
        VideoConfig.OrientParams params = caps.getOrientParams();
        int callId = StringUtils.stringToInt(caps.getSessionId(), -1);
        return tupManager.setMboileVideoOrient(callId, params.getCameraIndex(), params.getOrient(), params
                .getOrientPortrait(), params.getOrientLandscape(), params.getOrientSeascape());
    }

    /**
     * SDK封装的视频窗口操作
     *
     * @param type   0为远端窗口，1为本地窗口
     * @param index  视频index
     * @param callId 会话号
     */
    public void videoWindowAction(int type, int index, String callId)
    {
        // 修改为如果有callid调 update方法，没有callid调create方法
        if (TextUtils.isEmpty(callId))
        {
            createVideoWindow(type, index);
        }
        else
        {
            updateVideoWindow(type, index, callId);
        }
    }

    /**
     * @param type  0为远端窗口，1为本地窗口
     * @param index index
     */
    private void createVideoWindow(int type, int index)
    {
        tupManager.createVideoWindow(type, index);
    }

    /**
     * 有callId时需要调update接口
     *
     * @param type      0为远端窗口，1为本地窗口
     * @param index     index
     * @param callIdStr callIdStr
     */
    private void updateVideoWindow(int type, int index, String callIdStr)
    {
        int callId = StringUtils.stringToInt(callIdStr);
        tupManager.updateVideoWindow(type, index, callId);
    }


    /**
     * 设置远端index
     *
     * @param index index
     */
    public void setVideoIndex(int index)
    {
        tupManager.mediaSetVideoIndex(index);
    }


    /**
     * 注销TUP
     */
    public void tupUninit()
    {
        if (initFlag)
        {
            tupManager.callUninit();
            tupManager.unregisterReceiver();
            logInfo(TAG, "tupUninit", "call_unInit end");
        }


    }

    @Override
    public void onCallComing(TupCall call)
    {

        if (tupManager.getRegState() != TupCallParam.CALL_E_REG_STATE.CALL_E_REG_STATE_REGISTERED)
        {
            call.endCall();
            return;
        }
        CallSession callSession = new CallSession(this);
        callSession.setTupCall(call);
        calls.put(callSession.getTupCall().getCallId(), callSession);
    }



    @Override
    public void onCallConnected(TupCall call)
    {

        logInfo(TAG, "onCallConnected", "onCallConnected");

        BroadMsg broadMsg = new BroadMsg(NotifyMessage.CALL_MSG_ON_SUCCESS);
        systemSetting.setIsAudio(true);
        CCAPP.getInstances().sendBroadcast(broadMsg);
    }

    @Override
    public void onCallEnded(TupCall call)
    {
        logInfo(TAG, "onCallEnded", "onCallEnded");
        systemSetting.setIsAudio(false);
    }

    @Override
    public void onCallDestroy(TupCall call)
    {
        logInfo(TAG, "onCallDestroy", "onCallDestroy");
        BroadMsg broadMsg = new BroadMsg(NotifyMessage.CALL_MSG_ON_CALL_END);
        CCAPP.getInstances().sendBroadcast(broadMsg);
    }

    @Override
    public void onCallRTPCreated(TupCall call)
    {
        MobileCC.getInstance().changeAudioRoute(MobileCC.AUDIO_ROUTE_SPEAKER);
    }

    @Override
    public void onCallAddVideo(TupCall call)
    {
        CallSession session = calls.get(call.getCallId());
    }


    @Override
    public void onCallRefreshView(TupCall call)
    {
        BroadMsg broadMsg = new BroadMsg(NotifyMessage.CALL_MSG_REFRESH_LOCALVIEW);
        CCAPP.getInstances().sendBroadcast(broadMsg);
    }


    @Override
    public void onNetQualityChange(TupAudioQuality audioQuality)
    {

        logInfo(TAG, "onNetQualityChange", "NetQualityChange -> " + audioQuality.getAudioNetLevel());

        BroadMsg broadMsg = new BroadMsg(NotifyMessage.CALL_MSG_ON_NET_QUALITY_LEVEL);
        RequestCode requestCode = new RequestCode();
        requestCode.setNetLevel(audioQuality.getAudioNetLevel());
        broadMsg.setRequestCode(requestCode);

        CCAPP.getInstances().sendBroadcast(broadMsg);
    }


    @Override
    public void onDecodeSuccess(DecodeSuccessInfo decodeSuccessInfo)
    {
        BroadMsg broadMsg = new BroadMsg(NotifyMessage.CALL_MSG_REFRESH_REMOTEVIEW);
        CCAPP.getInstances().sendBroadcast(broadMsg);
    }


    /**
     * 设置带宽
     *
     * @param value value
     */
    public void setDataRate(int value)
    {
        setDatarate(value);
    }

    /**
     * 设置视频模式
     * 1流畅优先  0画质优先（默认）
     *
     * @param value value
     */
    public void setVideoMode(int value)
    {
        setvMode(value);
    }

    /**
     * 获取通话的分辨率带宽延迟等信息
     *
     * @return boolean
     */
    public boolean getChannelInfo()
    {
        if (currentCallSession != null)
        {
            currentCallSession.getTupCall().getChannelInfo();
            if (currentCallSession.getTupCall().getChannelInfo() != null)
            {
                BroadMsg broadMsg = new BroadMsg(NotifyMessage.CALL_MSG_GET_VIDEO_INFO);
                //这里建一个StreamInfo对象进行赋值
                StreamInfo streamInfo = new StreamInfo();
                streamInfo.setEncoderSize(currentCallSession.getTupCall()
                        .getChannelInfo().getVideoStreamInfo().getEncoderSize());
                streamInfo.setSendFrameRate(currentCallSession.getTupCall()
                        .getChannelInfo().getVideoStreamInfo().getSendFrameRate());
                streamInfo.setVideoSendBitRate(currentCallSession.getTupCall()
                        .getChannelInfo().getVideoStreamInfo().getVideoSendBitRate());
                streamInfo.setVideoSendDelay(currentCallSession.getTupCall()
                        .getChannelInfo().getVideoStreamInfo().getVideoSendDelay());
                streamInfo.setVideoSendJitter(currentCallSession.getTupCall()
                        .getChannelInfo().getVideoStreamInfo().getVideoSendJitter());
                streamInfo.setVideoSendLossFraction(currentCallSession.getTupCall()
                        .getChannelInfo().getVideoStreamInfo().getVideoSendLossFraction());

                streamInfo.setDecoderSize(currentCallSession.getTupCall().getChannelInfo()
                        .getVideoStreamInfo().getDecoderSize());
                streamInfo.setRecvFrameRate(currentCallSession.getTupCall().getChannelInfo()
                        .getVideoStreamInfo().getRecvFrameRate());
                streamInfo.setVideoRecvBitRate(currentCallSession.getTupCall().getChannelInfo()
                        .getVideoStreamInfo().getVideoRecvBitRate());
                streamInfo.setVideoRecvDelay(currentCallSession.getTupCall().getChannelInfo()
                        .getVideoStreamInfo().getVideoRecvDelay());
                streamInfo.setVideoRecvJitter(currentCallSession.getTupCall().getChannelInfo()
                        .getVideoStreamInfo().getVideoRecvJitter());
                streamInfo.setVideoRecvLossFraction(currentCallSession.getTupCall().getChannelInfo()
                        .getVideoStreamInfo().getVideoRecvLossFraction());

                RequestInfo requestInfo = new RequestInfo();
                requestInfo.setStreamInfo(streamInfo);
                broadMsg.setRequestInfo(requestInfo);
                CCAPP.getInstances().sendBroadcast(broadMsg);
                return true;
            }
        }
        return false;
    }

    /**
     * 设置静音
     *
     * @param isMute isMute
     * @param isMic  isMic
     * @return boolean
     */
    public boolean mute(boolean isMute, boolean isMic)
    {
        if (currentCallSession != null)
        {
            if (isMic)
            {
                currentCallSession.mute(0, isMute);
            }
            else
            {
                currentCallSession.mute(1, isMute);
            }
            return true;
        }
        else
        {
            return false;
        }
    }

    /**
     * 获取音量值
     *
     * @return int
     */
    public int getVoiceValue()
    {
        return tupManager.mediaGetSpeakVolume();
    }

    /**
     * 设置音量
     *
     * @param volume volume
     */
    public void setVoiceValue(int volume)
    {
        int value = tupManager.mediaSetSpeakVolume(0, volume);
        logInfo(TAG, "setVoiceValue", "volume = " + volume);
    }


    /**
     * 设置音频路由
     *
     * @param device 1:扬声器  0：听筒
     */
    public void setSpeakerOn(int device)
    {
        tupManager.setMobileAudioRoute(device);
    }

    private void logInfo(String tagName, String methodName, String content)
    {
        LogUtil.d(tagName, methodName + " " + content);
    }
}
