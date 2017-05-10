/**
 * Copyright 2015 Huawei Technologies Co., Ltd. All rights reserved.
 * eSDK is licensed under the Apache License, Version 2.0 ^(the "License"^);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.huawei.esdk.cc;

import android.app.Application;
import android.content.Context;
import android.os.Environment;
import android.view.SurfaceView;
import android.view.ViewGroup;

import com.huawei.esdk.cc.service.ServiceConfig;
import com.huawei.esdk.cc.common.NotifyMessage;
import com.huawei.esdk.cc.service.CCAPP;
import com.huawei.esdk.cc.service.ServiceManager;
import com.huawei.esdk.cc.service.call.CallManager;
import com.huawei.esdk.cc.service.conference.ConferenceMgr;
import com.huawei.esdk.cc.service.ics.SystemSetting;
import com.huawei.esdk.cc.utils.LogUtil;
import com.huawei.esdk.cc.video.VideoControl;
import com.huawei.meeting.ConfDefines;

import java.io.File;
import java.io.InputStream;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * CC+TP接口调用类
 */
public final class MobileCC
{
    /**
     * 视屏模式 流畅优先
     */
    public static final int VIDEOMODE_FLUENT = 1;

    /**
     * 视屏模式 画质优先
     */
    public static final int VIDEOMODE_QUALITY = 0;

    /**
     * 平台类型 TP
     */
    public static final int SERVER_TP = 1;

    /**
     * 平台类型 MS
     */
    public static final int SERVER_MS = 2;


    /**
     * 视屏控制-开启
     */
    public static final int START = 0x04;

    /**
     * 视屏控制-停止
     */
    public static final int STOP = 0x08;

    /**
     * 呼叫类型-视频呼叫
     */
    public static final String VIDEO_CALL = "0";

    /**
     * 呼叫类型-语音呼叫
     */
    public static final String AUDIO_CALL = "3";

    /**
     * 消息类型-文字
     */
    public static final String MESSAGE_TYPE_TEXT = "1";

    /**
     * 消息类型-语音
     */
    public static final String MESSAGE_TYPE_AUDIO = "2";

    /**
     * 返回码为"0",表示成功
     */
    public static final String MESSAGE_OK = "0";

    /**
     * 音频路由-扬声器
     */
    public static final int AUDIO_ROUTE_SPEAKER = 1;

    /**
     * 音频路由-听筒
     */
    public static final int AUDIO_ROUTE_RECEIVER = 0;

    /**
     * 呼叫出错
     */
    public static final int CALL_ERROR = -1;
    private static final Object LOCKOBJECT = new Object();
    private static final String TAG = "MobileCC.java";
    private static MobileCC instance;
    private ServiceConfig serviceConfig = ServiceConfig.getInstance();
    private SystemSetting systemSetting = SystemSetting.getInstance();

    private MobileCC()
    {
    }

    /**
     * 实例获取
     *
     * @return MobileCC
     */
    public static MobileCC getInstance()
    {
        synchronized (LOCKOBJECT)
        {
            if (null == instance)
            {
                instance = new MobileCC();
            }
            return instance;
        }

    }

    /**
     * 初始化SDK
     *
     * @param app app
     */
    public void initSDK(Application app)
    {
        CCAPP.getInstances().initApp(app);
        logInfo(TAG, "initSDK", "Application init");
    }

    /**
     * 停止SDK
     */
    public void unInitSDK()
    {
        CallManager callManager = CallManager.getInstance();
        if (callManager != null)
        {
            callManager.tupUninit();
            logInfo(TAG, "unInitSDK", "SDK unInit");
        }

    }

    /**
     * 接入网关地址设置
     *
     * @param ipStr         ipStr
     * @param portStr       portStr
     * @param transSecurity transSecurity
     * @param sipServerType sipServerType
     * @return int
     */
    public int setHostAddress(final String ipStr, String portStr, boolean transSecurity, int sipServerType)
    {
        if (null == ipStr || null == portStr || "".equals(ipStr) || "".equals(portStr))
        {
            logError(TAG, "setHostAddress", "IPStr=" + "***" + ", portStr="
                    + portStr + ", transSecurity=" + "transSecurity" + ", sipServerType=" + sipServerType);
            return NotifyMessage.RET_ERROR_PARAM;
        }

        if (!isIp(ipStr))
        {
            new Thread(new Runnable() {

                @Override
                public void run() {
                    getInetAddress(ipStr);
                }
            }).start();
        }
        else
        {
            systemSetting.setServerIp(ipStr);
        }
        if (!isNumber(portStr))
        {
            logError(TAG, "setHostAddress", "IPStr=" + "***" + ", portStr="
                    + portStr + ", transSecurity=" + transSecurity + ", sipServerType=" + sipServerType);
            return NotifyMessage.RET_ERROR_PARAM;
        }
        int port = 0;
        try
        {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e)
        {
            logError(TAG, "setHostAddress", "NumberException!");
        }
        if (port <= 0 || port >= 65535)
        {
            logError(TAG, "setHostAddress", "ipStr=" + "***" + ", portStr="
                    + portStr + ", transSecurity=" + transSecurity + ", sipServerType=" + sipServerType);
            return NotifyMessage.RET_ERROR_PARAM;
        }

        if (sipServerType == SERVER_TP)
        {
            serviceConfig.setSipServerType(SERVER_TP + "");
        }
        if (sipServerType == SERVER_MS)
        {
            serviceConfig.setSipServerType(SERVER_MS + "");
        }

        if (transSecurity)
        {
//            systemSetting.initServer(ipStr, port, "https://");
            systemSetting.setServerPort(port);
            systemSetting.setTransSecurity("https://");
            systemSetting.setIsHTTPS(transSecurity);
            ServiceManager.getInstance().initService(sipServerType);
            logInfo(TAG, "setHostAddress", "ipStr=" + "***" + ", portStr=" + portStr
                    + ", transSecurity=" + transSecurity + ", sipServerType=" + sipServerType);
            return NotifyMessage.RET_OK;
        }
        else
        {
//            systemSetting.initServer(ipStr, port, "http://");
            systemSetting.setServerPort(port);
            systemSetting.setTransSecurity("http://");
            systemSetting.setIsHTTPS(transSecurity);
            ServiceManager.getInstance().initService(sipServerType);
            logInfo(TAG, "setHostAddress", "ipStr=" + "***" + ", portStr=" + portStr
                    + ", transSecurity=" + transSecurity + ", sipServerType=" + sipServerType);
            return NotifyMessage.RET_OK;
        }
    }


    /**
     * 登录
     *
     * @param vdnId    vdnId
     * @param userName userName
     * @return int
     */
    public int login(String vdnId, String userName)
    {
        if (null == vdnId || null == userName || "".equals(vdnId) || "".equals(userName))
        {
            logError(TAG, "login", "vdnId=" + vdnId + ", userName = " + userName);
            return NotifyMessage.RET_ERROR_PARAM;
        }
        if (!isName(userName) || userName.length() > 20 || !isNumber(vdnId) || vdnId.length() > 4)
        {
            logError(TAG, "login", "vdnId=" + vdnId + ", userName = " + userName);
            return NotifyMessage.RET_ERROR_PARAM;
        }
        systemSetting.setUserName(userName);
        systemSetting.setVndID(vdnId);
        ServiceManager.getInstance().login(systemSetting.getUserName());
        logInfo(TAG, "login", "vdnId=" + vdnId + ", userName = " + userName);
        return NotifyMessage.RET_OK;
    }

    /**
     * 注销
     */
    public void logout()
    {

        if (serviceConfig.isQueuing())
        {
            ServiceManager.getInstance().dropCall();
        }
        ServiceManager.getInstance().logout();
        logInfo(TAG, "logout", "");
    }

    /**
     * 建立文字连接
     *
     * @param accessCode accessCode
     * @param callData   callData
     * @param varifyCode varifyCode
     * @return int
     */
    public int webChatCall(String accessCode, String callData, String varifyCode)
    {
        if (null == accessCode || null == callData || "".equals(accessCode))
        {
            logError(TAG, "webChatCall", "accessCode = " + accessCode + ",callData= ***");
            return NotifyMessage.RET_ERROR_PARAM;
        }
        if (!isNumber(accessCode) || accessCode.length() > 24 || callData.length() > 1024)
        {
            logError(TAG, "webChatCall", "accessCode = " + accessCode + ",callData= ***");
            return NotifyMessage.RET_ERROR_PARAM;
        }
        systemSetting.setVerifyCode(varifyCode);
        ServiceManager.getInstance().getTextConnect(accessCode, systemSetting.getUserName(),
                serviceConfig.getSipServerType(), callData, systemSetting.getVerifyCode());
        logInfo(TAG, "webChatCall", "accessCode = " + accessCode + ",callData= ***");
        return NotifyMessage.RET_OK;
    }


    /**
     * 发起呼叫
     *
     * @param accessCode accessCode
     * @param callType   callType
     * @param callData   callData
     * @param varifyCode varifyCode
     * @return int
     */
    public int makeCall(String accessCode, String callType, String callData, String varifyCode)
    {
        if (null == accessCode || null == callType || null == callData || null == varifyCode
                || "".equals(accessCode) || "".equals(callType))
        {
            logError(TAG, "makeCall", "accessCode = " + accessCode + ",callType="
                    + callType + ",callData= ***" + "varifyCode" + varifyCode);
            return NotifyMessage.RET_ERROR_PARAM;
        }
        systemSetting.setVerifyCode(varifyCode);
        if (!isNumber(accessCode) || accessCode.length() > 24 || callData.length() > 1024 || varifyCode.length() > 10)
        {
            logError(TAG, "makeCall", "accessCode = " + accessCode + ",callType="
                    + callType + ",callData= ***" + "varifyCode" + varifyCode);
            return NotifyMessage.RET_ERROR_PARAM;
        }
        if ((SERVER_TP + "").equals(serviceConfig.getSipServerType()))
        {
            if ((SERVER_TP + "").equals(callType)) //TP情况下，直接发起呼叫
            {
                ServiceManager.getInstance().getTextConnect(accessCode, systemSetting.getUserName(),
                        callType, callData, varifyCode);
                logInfo(TAG, "makeCall", "accessCode = " + accessCode + ",callType="
                        + callType + ",callData= ***" + "varifyCode" + varifyCode);
                return NotifyMessage.RET_OK;
            }
            return NotifyMessage.RET_ERROR_PARAM;
        }
        else if ((SERVER_MS + "").equals(serviceConfig.getSipServerType()))
        {
            if (MobileCC.VIDEO_CALL.equals(callType)) //MS情况下
            {
                //当视屏呼叫时
                serviceConfig.setCallType(callType);
                if (!systemSetting.isAudioConnected())
                {
                    ServiceManager.getInstance().audioConnect(accessCode, callData);
                }
                logInfo(TAG, "makeCall", "accessCode = " + accessCode + ",callType="
                        + callType + ",callData= ***" + "varifyCode" + varifyCode);
                return NotifyMessage.RET_OK;
            }
            else if (AUDIO_CALL.equals(callType))
            {
                //音频呼叫时
                serviceConfig.setCallType(callType);
                //语音呼叫首先判断有没有语音能力，如果没有，首先获取语音能力再呼叫
                if (systemSetting.isAudioConnected())
                {
                    CallManager.getInstance().getVoipConfig().resetData("", "", systemSetting.getSIPIp(),
                            systemSetting.getSIPPort());
                    String toNum = serviceConfig.getCalledNum();
                    int status = CallManager.getInstance().makeAnonymousCall(toNum);
                    if (MobileCC.CALL_ERROR != status)
                    {
                        logInfo(TAG, "makeCall", "accessCode = " + accessCode + ",callType="
                                + callType + ",callData= ***");
                        return NotifyMessage.RET_OK;
                    }
                    else
                    {
                        logError(TAG, "makeCall", "accessCode = " + accessCode
                                + ",callType=" + callType + ",callData= ***" + "varifyCode" + varifyCode);
                        return NotifyMessage.RET_ERROR_AUDIO_NOT_CONNECTED;
                    }
                }
                else
                {
                    ServiceManager.getInstance().audioConnect(accessCode, callData);
                    logInfo(TAG, "makeCall",    "accessCode = " + accessCode + ",callType="
                            + callType + ",callData= ***" + "varifyCode" + varifyCode);
                    return NotifyMessage.RET_OK;
                }
            }
        }
        logError(TAG, "makeCall", "accessCode = " + accessCode + ",callType="
                + callType + ",callData= ***" + "varifyCode" + varifyCode);
        return NotifyMessage.RET_ERROR_PARAM;
    }

    /**
     * 释放呼叫
     *
     * @return
     */
    public void releaseCall()
    {
        logInfo(TAG, "releaseCall", "");
        //如果是MS中的会议，首先释放会议，其余情况一律用dropCall()
        if ((SERVER_MS + "").equals(serviceConfig.getSipServerType()))
        {
            if (systemSetting.isMeeting())
            {
                ServiceManager.getInstance().stopConf(serviceConfig.getConfId());
                ConferenceMgr.getInstance().toleaveConf();
                systemSetting.setIsMeeting(false);
            }
            if (systemSetting.isAudioConnected() || serviceConfig.isQueuing())
            {
                dropCall();
            }
        }
        else if ((SERVER_TP + "").equals(serviceConfig.getSipServerType()))
        {
            if (systemSetting.isAudio())
            {
                dropCall();
                CallManager.getInstance().releaseCall();

            }
            else if (serviceConfig.isQueuing())
            {
                dropCall();
            }
            else
            {
                dropCall();
            }

        }


    }

    /**
     * 发送消息
     *
     * @param content content
     * @return int
     */
    public int sendMsg(String content)
    {
        if (null == content || "".equals(content) || content.length() > 300)
        {
            logError(TAG, "sendMsg", "content=" + content);
            return NotifyMessage.RET_ERROR_PARAM;
        }
        ServiceManager.getInstance().sendMsg(content);
        logInfo(TAG, "sendMsg", "content=" + content);
        return NotifyMessage.RET_OK;
    }

    /**
     * 静音
     *
     * @param isMute isMute
     * @return boolean
     */
    public boolean setMicMute(boolean isMute)
    {
        logInfo(TAG, "setMicMute", "isMute=" + isMute);
        return CallManager.getInstance().mute(isMute, true);
    }


    /**
     * 获取排队信息
     */
    public void getCallQueueInfo()
    {
        ServiceManager.getInstance().getCallQueueInfo();
        logInfo(TAG, "getCallQueueInfo", "");
    }


    /**
     * 获取扬声器音量
     *
     * @return int
     */
    public int getSpeakerVolume()
    {
        logInfo(TAG, "getSpeakerVolume", "");
        return CallManager.getInstance().getVoiceValue();
    }

    /**
     * 设置扬声器音量
     *
     * @param nVolume nVolume
     */
    private void setSpeakVolume(int nVolume)
    {
        CallManager.getInstance().setVoiceValue(nVolume);
        logInfo(TAG, "setSpeakerVolume", "nVolume=" + nVolume);

    }

    private void addSufaceView(ViewGroup container, SurfaceView child)
    {
        if (child == null)
        {
            return;
        }
        if (child.getParent() != null)
        {
            ViewGroup vGroup = (ViewGroup) child.getParent();
            if (vGroup != null)
            {
                vGroup.removeAllViews();
            }
        }
        container.addView(child);
    }

    /**
     * 设置视频容器
     *
     * @param context    context
     * @param localView  localView
     * @param remoteView remoteView
     */
    public void setVideoContainer(Context context, ViewGroup localView,
                                  ViewGroup remoteView)
    {
        if ((SERVER_TP + "").equals(serviceConfig.getSipServerType()))
        {
            if (null != localView)
            {
                addSufaceView(localView, VideoControl.getIns().getLocalVideoView());

            }
            if (null != remoteView)
            {
                addSufaceView(remoteView, VideoControl.getIns().getRemoteVideoView());
            }

        }
        else if ((SERVER_MS + "").equals(serviceConfig.getSipServerType()))
        {
            ConferenceMgr.getInstance().setVideoContainer(context, localView,
                    remoteView);
        }

        logInfo(TAG, "setVideoContainer", "context=" + context + ",localView="
                + localView + ",remoteView=" + remoteView);


    }

    /**
     * 设置共享容器
     *
     * @param context    context
     * @param sharedView sharedView
     */
    public void setDesktopShareContainer(Context context, ViewGroup sharedView)
    {
        ConferenceMgr.getInstance().setSharedViewContainer(context, sharedView,
                ConfDefines.IID_COMPONENT_AS);
        logInfo(TAG, "setDesktopShareContainer", "context=" + context + ",sharedView=" + sharedView);
        ConferenceMgr.getInstance().flush();
    }

    /**
     * 升级为视屏
     *
     * @return int
     */
    public int updateToVideo()
    {
        if ("".equals(serviceConfig.getAudioCallId()))
        {
            logError(TAG, "updateToVideo", "");
            return NotifyMessage.RET_ERROR_AUDIO_NOT_CONNECTED;
        }
        ServiceManager.getInstance().applyMeeting(serviceConfig.getAudioCallId());

        logInfo(TAG, "updateToVideo", "");
        return NotifyMessage.RET_OK;
    }

    /**
     * 呼叫带宽设置
     *
     * @param bandwidth bandwidth
     * @return boolean
     */
    public boolean setDataRate(int bandwidth)
    {
        if (bandwidth > 768 || bandwidth < 1)
        {
            logError(TAG, "setDataRate", "bandwidth=" + bandwidth);
            return false;
        }
        if ((MobileCC.SERVER_TP + "").equals(serviceConfig.getSipServerType()))
        {
            if (bandwidth <= 128)
            {
                CallManager.getInstance().setDataRate(128);
            }
            else if (bandwidth <= 256)
            {
                CallManager.getInstance().setDataRate(256);
            }
            else if (bandwidth <= 384)
            {
                CallManager.getInstance().setDataRate(384);
            }
            else if (bandwidth <= 512)
            {
                CallManager.getInstance().setDataRate(512);
            }
            else if (bandwidth > 512)
            {
                CallManager.getInstance().setDataRate(768);
            }
        }
        else if ((MobileCC.SERVER_MS + "").equals(serviceConfig.getSipServerType()))
        {
            if (bandwidth <= 512)
            {
                ConferenceMgr.getInstance().setVideoParam(176, 144, 15);
            }
            else if (bandwidth > 512)
            {
                ConferenceMgr.getInstance().setVideoParam(640, 480, 30);
            }
        }
        logInfo(TAG, "setDataRate", "bandwidth=" + bandwidth);
        return true;
    }

    /**
     * 设置视频模式 0画质优先（默认） 1流畅优先
     *
     * @param videoMode videoMode
     * @return int
     */
    public int setVideoMode(int videoMode)
    {
        if ((MobileCC.SERVER_TP + "").equals(serviceConfig.getSipServerType()))
        {
            if (VIDEOMODE_QUALITY == videoMode || VIDEOMODE_FLUENT == videoMode)
            {
                CallManager.getInstance().setVideoMode(videoMode);
                logInfo(TAG, "setVideoMode", "videoMode=" + videoMode);
                return NotifyMessage.RET_OK;
            }
        }
        else if ((MobileCC.SERVER_MS + "").equals(serviceConfig.getSipServerType()))
        {
            if (VIDEOMODE_QUALITY == videoMode)
            {
                ConferenceMgr.getInstance().setVideoParam(640, 480, 30);
                logInfo(TAG, "setVideoMode", "videoMode=" + videoMode);
                return NotifyMessage.RET_OK;
            }
            else if (VIDEOMODE_FLUENT == videoMode)
            {
                ConferenceMgr.getInstance().setVideoParam(176, 144, 15);
                logInfo(TAG, "setVideoMode", "videoMode=" + videoMode);
                return NotifyMessage.RET_OK;
            }
        }
        logError(TAG, "setVideoMode", "videoMode=" + videoMode);
        return NotifyMessage.RET_ERROR_PARAM;
    }

    /**
     * 获取通话的分辨率带宽延迟等信息
     *
     * @return boolean
     */
    public boolean getChannelInfo()
    {
        if ((MobileCC.SERVER_TP + "").equals(serviceConfig.getSipServerType()))
        {
            CallManager.getInstance().getChannelInfo();
            logInfo(TAG, "getChannelInfo", "");
            return true;
        }
        else if ((MobileCC.SERVER_MS + "").equals(serviceConfig.getSipServerType()))
        {
            ConferenceMgr.getInstance().getVideoStream();
            logInfo(TAG, "getChannelInfo", "");
            return true;

        }
        logError(TAG, "getChannelInfo", "");
        return false;

    }

    private void dropCall()
    {
        LogUtil.d(TAG, "isAudioConnect" + systemSetting.isAudio());
        ServiceManager.getInstance().dropCall();
    }

    /**
     * 取消排队
     */
    public void cancelQueue()
    {
        ServiceManager.getInstance().cancelQueue();
        logInfo(TAG, "cancelQueue", "");
    }

    /**
     * 获取版本信息
     *
     * @return string
     */
    public String getVersion()
    {
        logInfo(TAG, "getVersion", "");
        return "2.1.10";
    }

    private boolean isName(String name)
    {
        String regEx = "^[A-Za-z0-9]*$";
        return Pattern.compile(regEx).matcher(name).find();
    }

    private boolean isNumber(String port)
    {
        String regEx = "^[0-9]*$";
        return Pattern.compile(regEx).matcher(port).find();
    }

    private boolean isIp(String ipAddress)
    {
        String ip = "([1-9]|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])(\\.(\\d|[1-9]\\d|1\\d{2}|2[0-4]\\d|25[0-5])){3}";
        Pattern pattern = Pattern.compile(ip);
        Matcher matcher = pattern.matcher(ipAddress);
        return matcher.matches();
    }


    /**
     * SIP服务器地址设置
     *
     * @param ipStr   ipStr
     * @param portStr portStr
     * @return int
     */
    public int setSIPServerAddress(String ipStr, String portStr)
    {

        if (null == ipStr || null == portStr || "".equals(ipStr) || "".equals(portStr))
        {
            logError(TAG, "setSIPServerAddress", "IPStr=" + ipStr + ",portStr=" + portStr);
            return NotifyMessage.RET_ERROR_PARAM;
        }
        if (!isIp(ipStr) || !isNumber(portStr))
        {
            logError(TAG, "setSIPServerAddress", "IPStr=" + ipStr + ",portStr=" + portStr);
            return NotifyMessage.RET_ERROR_PARAM;
        }
        systemSetting.initSIPServerAddr(ipStr, portStr);
        logInfo(TAG, "setSIPServerAddress", "ipStr=" + ipStr + ",portStr=" + portStr);
        return NotifyMessage.RET_OK;
    }


    /**
     * 改变音频路由
     *
     * @param route route
     * @return boolean
     */
    public boolean changeAudioRoute(int route)
    {
        boolean isSuccess;

        //1:扬声器  2：听筒
        if (AUDIO_ROUTE_SPEAKER == route || AUDIO_ROUTE_RECEIVER == route)
        {
            CallManager.getInstance().setSpeakerOn(route);

            logInfo(TAG, "changeAudioRoute", "device=" + route);
            isSuccess = true;
        }
        else
        {
            logError(TAG, "changeAudioRoute", "device=" + route);
            isSuccess = false;
        }

        return isSuccess;

    }

    /**
     * 切换摄像头
     */
    public void switchCamera()
    {
        if ((MobileCC.SERVER_TP + "").equals(serviceConfig.getSipServerType()))
        {
            VideoControl.getIns().switchCamera();

        }
        else if ((MobileCC.SERVER_MS + "").equals(serviceConfig.getSipServerType()))
        {
            ConferenceMgr.getInstance().switchCamera();
        }
        logInfo(TAG, "switchCamera", "");
    }

    /**
     * 设置传输加密
     *
     * @param enableTLS  enableTLS
     * @param enableSRTP enableSRTP
     */
    public void setTransportSecurity(boolean enableTLS, boolean enableSRTP)
    {
        systemSetting.setTLSEncoded(enableTLS);
        systemSetting.setSRTPEncoded(enableSRTP);
        systemSetting.setIsTransSecurity(true);
        logInfo(TAG, "setTransportSecurity", "enableTLS=" + enableTLS + ",enableSRTP=" + enableSRTP);
    }


    /**
     * 设置Log路径和等级
     *
     * @param path  path
     * @param level level
     * @return int
     */
    public int setLog(String path, int level)
    {
        if ("".equals(path))
        {
            logError(TAG, "setLog", "path=" + path + ",level=" + level);
            return NotifyMessage.RET_ERROR_PARAM;
        }
        if (path.length() > 60 || level < 1 || level > 3)
        {
            logError(TAG, "setLog", "path=" + path + ",level=" + level);
            return NotifyMessage.RET_ERROR_PARAM;
        }

        String logFile = Environment.getExternalStorageDirectory().toString()
                + File.separator + path;

        File dirFile = new File(logFile);

        //如果该文件夹不存在且不是目录的情况下，创建文件夹
        if (!(dirFile.exists()) && !(dirFile.isDirectory()))
        {
            try
            {
                //按照指定的路径创建文件夹，创建成功说明路径合法，返回0
                if (dirFile.mkdirs())
                {
                    systemSetting.setLogPath(path);
                    systemSetting.setLogLevel(level);
                    logInfo(TAG, "setLog", "path=" + path + ",level=" + level);
                    return NotifyMessage.RET_OK;
                }
            } catch (SecurityException e)
            {
                //创建出异常返回-1
                logError(TAG, "setLog", "path=" + path + ",level=" + level);
                return NotifyMessage.RET_ERROR_PARAM;
            }
        }
        //当该文件夹存在或者是一个目录的情况下，路径本身合法，直接设置，返回0即可。
        systemSetting.setLogPath(path);
        systemSetting.setLogLevel(level);
        logInfo(TAG, "setLog", "path=" + path + ",level=" + level);
        return NotifyMessage.RET_OK;
    }

    private void logInfo(String tagName, String methodName, String content)
    {
        LogUtil.d(tagName, methodName + " " + content);

    }

    private void logError(String tagName, String methodName, String content)
    {
        LogUtil.e(tagName, methodName + " " + content);
    }

    /**
     * 视频后台控制
     *
     * @param operation operation
     */
    public void videoOperate(int operation)
    {
        CallManager.getInstance().videoControl(operation);
        logInfo(TAG, "videoOperate", "operation=" + operation);
    }

    /**
     * 释放文字连接
     */
    public void releaseWebChatCall()
    {
        ServiceManager.getInstance().dropTextCall();
        logInfo(TAG, "releaseWebChatCall", "");
    }

    /**
     * 设置扬声器静音
     *
     * @param isMute isMute
     * @return boolean
     */
    public boolean setSpeakerMute(boolean isMute)
    {
        logInfo(TAG, "setSpeakerMute", "isMute=" + isMute);
        return CallManager.getInstance().mute(isMute, false);

    }

    /**
     * 设置视频角度
     *
     * @param rotate rotate
     * @return boolean
     */
    public boolean setVideoRotate(int rotate)
    {
        if (0 != rotate && 90 != rotate && 180 != rotate && 270 != rotate)
        {
            logError(TAG, "setVideoRotate", "rotate=" + rotate);
            return false;
        }
        int rt[] = {0, 90, 180, 270};
        Arrays.sort(rt);
        int result = Arrays.binarySearch(rt, rotate);

        if (result < 0 || result >= rt.length)
        {
            logError(TAG, "setVideoRotate", "rotate=" + rotate);
            return false;
        }

        if ((SERVER_TP + "").equals(serviceConfig.getSipServerType()))
        {
            CallManager.getInstance().setRotation(result);
            logInfo(TAG, "setVideoRotate", "rotate=" + rotate);
            return true;
        }
        else if ((SERVER_MS + "").equals(serviceConfig.getSipServerType()))
        {
            logInfo(TAG, "setVideoRotate", "rotate=" + rotate);
            return ConferenceMgr.getInstance().setRotate(rotate);

        }
        logError(TAG, "setVideoRotate", "rotate=" + rotate);
        return false;
    }

    /**
     * 设置证书
     *
     * @param needValidate needValidate
     * @param needValidateDomain needValidateDomain
     * @param certInputStream certInputStream
     */
    public void setServerCertificateValidation(boolean needValidate, boolean needValidateDomain,
                                               InputStream certInputStream)
    {
        logInfo(TAG, "setServerCertificateValidation", "needValidate=" + needValidate
                + " needValidateDomain" + needValidateDomain);
        systemSetting.setValidateServerCertificate(needValidate);
        systemSetting.setValidateDomain(needValidateDomain);
        if (needValidate)
        {
            ServiceConfig.addCertificate(certInputStream);
        }
    }

    /**
     * 获取验证码
     */
    public void getVerifyCode()
    {

        ServiceManager.getInstance().getVerifyCode();
    }

    /**
     * 设置匿名卡号
     * @param anonymousCard anonymousCard
     */
    public void setAnonymousCard(String anonymousCard)
    {
        systemSetting.setAnonymousCard(anonymousCard);
    }

    private  void getInetAddress(String  host)
    {
        String ipAddress = "";
        InetAddress result = null;
        try
        {
            result = java.net.InetAddress.getByName(host);
            logError("getInetAddress", "", "ReturnStr1" + result);
            if (result != null)
            {
                ipAddress = result.getHostAddress();
            }

        }
        catch (Exception e)
        {
            logError("getInetAddress", "", "exception");
        }
        if (!"".equals(ipAddress))
        {
            logInfo("getInetAddress", "", ipAddress);
            systemSetting.setServerIp(ipAddress);
        }
        else
        {
            logError("getInetAddress", "", "IPAddress parse error");
        }

    }
}
