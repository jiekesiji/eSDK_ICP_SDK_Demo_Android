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
package com.huawei.esdk.cc.service.conference;

import android.content.Context;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.huawei.esdk.cc.MobileCC;
import com.huawei.esdk.cc.common.BroadMsg;
import com.huawei.esdk.cc.common.NotifyMessage;
import com.huawei.esdk.cc.common.RequestCode;
import com.huawei.esdk.cc.common.RequestInfo;
import com.huawei.esdk.cc.service.ServiceConfig;
import com.huawei.esdk.cc.service.CCAPP;
import com.huawei.esdk.cc.service.ics.SystemSetting;
import com.huawei.esdk.cc.utils.AddressTools;
import com.huawei.esdk.cc.utils.LogUtil;
import com.huawei.esdk.cc.video.CameraInfo;
import com.huawei.esdk.cc.video.StreamInfo;
import com.huawei.esdk.cc.video.VideoParams;
import com.huawei.meeting.ConfDefines;
import com.huawei.meeting.ConfExtendMsg;
import com.huawei.meeting.ConfExtendUserDataMsg;
import com.huawei.meeting.ConfExtendUserInfoMsg;
import com.huawei.meeting.ConfExtendVideoDeviceInfoMsg;
import com.huawei.meeting.ConfExtendVideoParamMsg;
import com.huawei.meeting.ConfGLView;
import com.huawei.meeting.ConfInstance;
import com.huawei.meeting.ConfMsg;
import com.huawei.meeting.ConfOper;
import com.huawei.meeting.ConfResult;
import com.huawei.meeting.Conference;
import com.huawei.meeting.IConferenceUI;
import com.huawei.meeting.serverip;
import com.huawei.videoengine.ViERenderer;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.File;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Semaphore;

/**
 * Created on 2016/3/5.
 */
public final class ConferenceMgr implements IConferenceUI
{
    /**
     * 共享状态-停止
     **/
    private static final int CONF_SHARED_STOP = 0;
    /**
     * 共享状态-开始
     **/
    private static final int CONF_SHARED_START = 1;
    private static final Object LOCKOBJECT = new Object();
    private static final String TAG = "ConferenceMgr";
    private static final int CONF_NEW_VAL = 99999;
    private static final int CONF_RELEASE_VAL = 99998;
    private static final int CONF_HEARTBEAT = 99997;
    private static final int CONF_MUTE = 99996;
    private static final int VTA_OPEN_VIDEO = 99994;
    private static final int VTM_OPEN_VIDEO = 99993;
    private static final int VTA_CLOSE_VIDEO = 99992;
    private static final int VTM_CLOSE_VIDEO = 99991;
    private static final int VTM_RESUME_VIDEO = 99990;
    private static final int LEAVE_CONF = 99989;
    private static final int UPDATE_DESKVIEW = 99988;
    private static final int UPDATE_DOCUVIEW = 99987;
    private static final int RELEASE_DESKVIEW = 99986;
    private static final int RELEASE_DOCUVIEW = 99985;
    private static final int DETACH_THRID_VIDEO_VIEW = 99984;
    private static final int MONITOR_SWITCH_MSG_VIEW = 99983;
    private static final int CONF_FLUSH = 99982;
    private static final int VIDEO_GETPARAM = 10000;
    private static final int VIDEO_SETPARAM = 10001;
    private static final int VIDEO_ON_RECONNECT = 10002;
    private static ConferenceMgr instance;
    private ServiceConfig serviceConfig = ServiceConfig.getInstance();
    private SystemSetting systemSetting = SystemSetting.getInstance();
    private ConferenceInfo conferenceInfo = null;
    /**
     * 会议组件开关
     **/
    private int componentVal = ConfDefines.IID_COMPONENT_BASE
            | ConfDefines.IID_COMPONENT_DS | ConfDefines.IID_COMPONENT_AS
            | ConfDefines.IID_COMPONENT_AUDIO
            | ConfDefines.IID_COMPONENT_VIDEO
            | ConfDefines.IID_COMPONENT_CHAT
            | ConfDefines.IID_COMPONENT_POLLING
            | ConfDefines.IID_COMPONENT_FT | ConfDefines.IID_COMPONENT_WB;
    /**
     * videoHandler
     */
    private final Handler videoHandler = new Handler(Looper.getMainLooper())
    {
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case VTA_OPEN_VIDEO:
                    mRemoteContainer.removeAllViews();
                    mRemoteContainer.addView(remoteSurfaceView);
                    break;
                case VTM_OPEN_VIDEO:
                    mLocalContainer.removeAllViews();

                    mLocalContainer.addView(svLocalSurfaceView);
                    if (turnOpenLocalFlag)
                    {
                        mLocalContainer.removeAllViews();
                        mLocalContainer.addView(svLocalSurfaceView);
                        turnOpenLocalFlag = false;
                    }

                    break;
                case VTM_RESUME_VIDEO:
                    mLocalContainer.removeAllViews();
                    mLocalContainer.addView(svLocalSurfaceView);

                    break;
                case VTM_CLOSE_VIDEO:
                    ViERenderer.setSurfaceNull(svLocalSurfaceView);
                    mLocalContainer.removeAllViews();
                    break;
                case VTA_CLOSE_VIDEO:
                    ViERenderer.setSurfaceNull(remoteSurfaceView);
                    mRemoteContainer.removeAllViews();
                    break;
                case LEAVE_CONF:
                    leaveConf();

                    exitConf();
                    releaseConf();
                    cameraIndex = 1;
                    BroadMsg broadMsg = new BroadMsg(
                            NotifyMessage.CALL_MSG_USER_END);
                    CCAPP.getInstances().sendBroadcast(broadMsg);
                    break;
                case UPDATE_DOCUVIEW:
                    updateDocSharedView();
                    break;
                case UPDATE_DESKVIEW:
                    updateDesktopSharedView();
                    break;
                case RELEASE_DOCUVIEW:
                    releaseDocShareView();
                    break;
                case RELEASE_DESKVIEW:
                    releaseDesktopShareView();
                    break;
            /*解除第三方视频页面绑定*/
                case DETACH_THRID_VIDEO_VIEW:
                    break;
                case MONITOR_SWITCH_MSG_VIEW:
                    break;

                case VIDEO_SETPARAM:
                    logInfo(TAG, "", " videoHadnler");
                    if (viewGroupSetVideoparam != null && surfaceViewSetParam != null)
                    {
                        logInfo(TAG, "", " videoHadnler -> setParam -- addView");
                        viewGroupSetVideoparam.addView(surfaceViewSetParam);
                    }

                    break;

                case VIDEO_ON_RECONNECT:
                    if (viewGroupSetVideoparam != null && surfaceViewSetParam != null)
                    {
                        logInfo(TAG, "", " videoHadnler -> reconnect -- addView");
                        updateVideoView(viewGroupSetVideoparam, surfaceViewSetParam);
//                        viewGroupSetVideoparam.addView(surfaceViewSetParam);
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private int userId;
    private String deviceId;
    private int flag = 0;

    private int cameraIndex = 1; //1 front ;2  back

    private int ori = 0; //默认无定义
    private boolean rotatoFlag = false; //竖屏拉起视频后的旋转标志（要270度处理）
    private Message updateDocumsg;
    private Message updateDeskmsg;
    private ConfInstance conf;
    /**
     * 会议预览摄像头开启标识
     **/
    private boolean captureFlag = false;
    /**
     * 转移开启本地摄像头控制
     **/
    private boolean turnCaptureFlag = false;
    /**
     * 转移匿名呼叫时显示视频控制
     **/
    private boolean turnOpenLocalFlag = false;
    /**
     * 控制心跳的Timer
     **/
    private Timer mytimer;
    /**
     * 控制心跳的Handler
     **/
    private Handler mheartBeatHandler;
    private WorkThread confThread;
    private Semaphore confThreadStartSemaphore;
    private Handler mConfHandler;
    /**
     * 主线程 ID
     **/
    private long mMainThreadID;
    /**
     * 会议句柄
     **/
    private int confHandle = 0;
    /**
     * 用于存储个用户的视频能力
     **/
    private Map<String, List<VideoParams>> videoParamsMap = new HashMap<String, List<VideoParams>>();
    /**
     * 显示本地视频的 SurfaceView
     **/
    private SurfaceView svLocalSurfaceView;
    private SurfaceView localSurfaceView;
    /**
     * 显示对端视频的 SurfaceView
     **/
    private SurfaceView remoteSurfaceView;
    /**
     * 用于装载本地视频的 ViewGroup
     **/
    private ViewGroup mLocalContainer;
    /**
     * 用于装载远端视频的 ViewGroup
     **/
    private ViewGroup mRemoteContainer;
    /**
     * 显示远端共享屏幕的 SurfaceView
     **/
    private ConfGLView desktopSurfaceView;
    /**
     * 显示远端共享文档的 SurfaceView
     **/
    private ConfGLView docSurfaceView;
    /**
     * 用于装载远端共享屏幕的 ViewGroup
     **/
    private ViewGroup mDesktopViewContainer;
    /**
     * 用于装载远端共享文档的 ViewGroup
     **/
    private ViewGroup mDocViewContainer;
    /**
     * 当前共享文档的数量
     **/
    private int dscurrentDocCount = 0;
    /**
     * 当前共享文档的ID
     **/
    private int dscurrentDocID = 0;
    /**
     * 当前共享文档的页码
     **/
    private int dscurrentPageID = 0;

    private ViewGroup viewGroupSetVideoparam = null;
    private SurfaceView surfaceViewSetParam = null;

    private ConferenceMgr()
    {
    }

    /**
     * @return ConferenceMgr
     */
    public static ConferenceMgr getInstance()
    {
        synchronized (LOCKOBJECT)
        {
            if (instance == null)
            {
                instance = new ConferenceMgr();
            }
            return instance;
        }
    }

    /**
     * 开启心跳
     */
    public void initConf()
    {
        mytimer = new Timer();
        mytimer.schedule(new TimerTask()
        {
            @Override
            public void run()
            {
                Message m = new Message();
                m.what = 0;
                mheartBeatHandler.sendMessage(m);
            }
        }, 200, 100);

        mheartBeatHandler = new Handler()
        {
            public void handleMessage(Message msg)
            {
                heartBeat();
            }
        };

        mMainThreadID = Looper.getMainLooper().getThread().getId();

        confThreadStartSemaphore = new Semaphore(0);
        confThread = new WorkThread();
        confThread.start();
        confThreadStartSemaphore.acquireUninterruptibly();
        mConfHandler = confThread.getHandler();
    }

    /**
     * 设置视频视图SurfaceView的装载容器ViewGroup
     *
     * @param context    上下文
     * @param localView  显示本地视频的ViewGroup
     * @param remoteView 显示远端视频的ViewGroup
     */
    public void setVideoContainer(Context context, ViewGroup localView,
                                  ViewGroup remoteView)
    {
        if (ori != 1)
        {
            Configuration cf = context.getResources().getConfiguration();
            ori = cf.orientation;
        }
        if (null == mLocalContainer)
        {
            mLocalContainer = localView;
        }
        if (null == svLocalSurfaceView)
        {
            svLocalSurfaceView = ViERenderer.createLocalRenderer(context);
        }
        if (null == localSurfaceView)
        {
            localSurfaceView = ViERenderer.createRenderer(context, true);
        }

        if (null == mRemoteContainer)
        {
            mRemoteContainer = remoteView;
        }

        if (null == remoteSurfaceView)
        {
            remoteSurfaceView = ViERenderer.createRenderer(context, true);
        }

        if (remoteSurfaceView != null)
        {
            mRemoteContainer.removeView(remoteSurfaceView);
            mRemoteContainer.addView(remoteSurfaceView);
        }

    }


    /**
     * 设置显示共享的容器
     *
     * @param context    context
     * @param sharedView sharedView
     * @param sharedType sharedType
     */
    public void setSharedViewContainer(Context context, ViewGroup sharedView,
                                       int sharedType)
    {
        if (ConfDefines.IID_COMPONENT_AS == sharedType)
        {
            mDesktopViewContainer = sharedView;
            mDesktopViewContainer.removeAllViews();
            desktopSurfaceView = new ConfGLView(context);
            desktopSurfaceView.setConf(conf);
            desktopSurfaceView.setViewType(sharedType);
            mDesktopViewContainer.addView(desktopSurfaceView);
            desktopSurfaceView.onResume();
            desktopSurfaceView.setVisibility(View.VISIBLE);
        }
        else if (ConfDefines.IID_COMPONENT_DS == sharedType)
        {
            mDocViewContainer = sharedView;
            mDocViewContainer.removeAllViews();
            docSurfaceView = new ConfGLView(context);
            docSurfaceView.setConf(conf);
            docSurfaceView.setViewType(sharedType);
            mDocViewContainer.addView(docSurfaceView);
            docSurfaceView.onResume();
            docSurfaceView.setVisibility(View.VISIBLE);
        }
        else
        {
            logInfo(TAG, "",  "setDesktopShareContainer | sharedType = "
                    + sharedType + " not support type");
        }
    }

    /**
     * 释放共享容器
     */
    public void releaseShareView()
    {
        releaseDesktopShareView();
        releaseDocShareView();
    }

    /**
     * 释放桌面共享容器
     */
    public void releaseDesktopShareView()
    {
        if (desktopSurfaceView != null && mDesktopViewContainer != null)
        {
            desktopSurfaceView.onPause();
            mDesktopViewContainer.removeView(desktopSurfaceView);
            mDesktopViewContainer.removeAllViews();
            mDesktopViewContainer.invalidate();
            desktopSurfaceView = null;
        }
    }

    /**
     * 释放文档共享容器
     */
    public void releaseDocShareView()
    {
        if (docSurfaceView != null && mDocViewContainer != null)
        {
            docSurfaceView.onPause();
            mDocViewContainer.removeView(docSurfaceView);
            mDocViewContainer.removeAllViews();
            mDocViewContainer.invalidate();
            docSurfaceView = null;
        }

        dscurrentDocCount = 0;
        dscurrentDocID = 0;
        dscurrentPageID = 0;
    }

    /**
     * 更新视频显示
     * @param viewGroup viewGroup
     * @param surfaceView surfaceView
     */
    public void updateVideoView(ViewGroup viewGroup, SurfaceView surfaceView)
    {
        if (surfaceView == null)
        {
            return;
        }
        if (viewGroup == null)
        {
            return;
        }
        Configuration configuration = CCAPP.getInstances().getApplication()
                .getResources().getConfiguration();
        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT && cameraIndex == 2)
        {
            setRotate(270);
        }
        else if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT && cameraIndex == 1)
        {
            setRotate(90);
        }
        viewGroup.removeAllViews();


        try
        {
            Thread.sleep(500);
        } catch (InterruptedException e)
        {
            logError(TAG, "", "interrupted exception");
        }


        logInfo(TAG, "",  "updateVideoView | videoAttach succeed");
        viewGroup.addView(surfaceView);

    }

    /**
     * 释放会议
     */
    public void releaseConf()
    {
        if (mytimer != null)
        {
            mytimer.cancel();
            mytimer = null;
        }
        if (confThreadStartSemaphore != null)
        {
            confThreadStartSemaphore.release();
            confThreadStartSemaphore = null;
        }

        if (svLocalSurfaceView != null && mLocalContainer != null)
        {
            ViERenderer.setSurfaceNull(svLocalSurfaceView);
            mLocalContainer.removeAllViews();
            svLocalSurfaceView = null;
            mLocalContainer = null;
        }

        if (remoteSurfaceView != null && mRemoteContainer != null)
        {
            ViERenderer.setSurfaceNull(remoteSurfaceView);
            mRemoteContainer.removeAllViews();
            remoteSurfaceView = null;
            mRemoteContainer = null;
        }
        releaseShareView();
    }

    /**
     * 创建并加入会议
     *
     * @return boolean
     */
    public boolean joinConference()
    {
        newConf();

        try
        {
            Thread.sleep(50, 0);
        } catch (InterruptedException e)
        {
            logInfo(TAG, "", "Interrupted Exception");
        }

        if (serviceConfig.isNat())
        {
            openIpMap();
        }

        return joinConf();
    }

    /*
    * 开启ipmap(MS重定向所需)
    */
    private void openIpMap()
    {
        String servIp = serviceConfig.getServerIp();
        String a[] = servIp.split(":");
        serverip[] sip = new serverip[1];
        for (int i = 0; i < 1; i++)
        {
            sip[i] = new serverip();
            sip[i].SetInterIp(a[0]);
            sip[i].SetOuterIp(serviceConfig.getNatIp());
        }
        if (conf != null)
        {
            int flag = conf.setipmap(sip, 1);
        }
    }

    /**
     * 离开会议，释放资源
     */
    public void toleaveConf()
    {
        clearVideoParamsMap();
        if (conf != null)
        {
            Message leaveConfmsg = new Message();
            leaveConfmsg.what = LEAVE_CONF;
            videoHandler.sendMessage(leaveConfmsg);
        }
        flag = 0;
    }

    /**
     * 初始化
     */
    public void initConfSDK()
    {
        String logFile = Environment.getExternalStorageDirectory().toString()
                + File.separator + NotifyMessage.CC_LOG + "/conf";
        File dirFile = new File(logFile);
        if (!(dirFile.exists()) && !(dirFile.isDirectory()))
        {
            if (dirFile.mkdir())
            {
                logInfo(TAG, "",  "mkdir " + dirFile.getPath());
            }
        }
        Conference.getInstance().setLogLevel(3, 3);
        Conference.getInstance().setPath(logFile, logFile);
        Conference.getInstance().initSDK(false, 4);
    }

    /**
     * 创建会议
     */
    public void newConf()
    {
        initConfSDK();
        conf = new ConfInstance();
        conf.setConfUI(this);
        com.huawei.meeting.ConfInfo cinfo = new com.huawei.meeting.ConfInfo();
        cinfo.setConfId(conferenceInfo.getConfId());
        cinfo.setConfKey(conferenceInfo.getConfKey());
        cinfo.setConfOption(1);
        cinfo.setHostKey(conferenceInfo.getHostKey());
        cinfo.setUserId(conferenceInfo.getUserId());
        cinfo.setUserName(conferenceInfo.getUserName());
        cinfo.setUserType(8);

        cinfo.setSiteId(conferenceInfo.getSiteId());
        cinfo.setSvrIp(conferenceInfo.getServerIp());
        cinfo.setSiteUrl(conferenceInfo.getSiteUrl());
        cinfo.setUserUri("");
        confNew(cinfo);

    }

    /**
     *
     * @return boolean
     */
    public boolean  flush()
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = CONF_FLUSH;
            mConfHandler.sendMessage(msg);
            return false;
        }
        if (conf != null)
        {
            conf.asFlush();
            return true;
        }
        return false;
    }

    /**
     * 创会
     *
     * @param cinfo cinfo
     * @return boolean
     */
    public boolean confNew(com.huawei.meeting.ConfInfo cinfo)
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = CONF_NEW_VAL;
            msg.obj = cinfo;
            mConfHandler.sendMessage(msg);
            return true;
        }

        boolean flag = conf.confNew(cinfo);
        if (flag == true)
        {
            confHandle = conf.getConfHandle();
        }
        return flag;
    }

    /**
     * 加入会议
     *
     * @return boolean
     */
    public boolean joinConf()
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.CONF_OPER_JOIN;
            mConfHandler.sendMessage(msg);
            return true;
        }
        int nRet = conf.confJoin();
        logInfo(TAG, "",  "JoinConf |  nRet = " + nRet);

        return (nRet == 0);
    }

    /**
     * 结束会议
     */
    public void exitConf()
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = CONF_RELEASE_VAL;
            mConfHandler.sendMessage(msg);
            return;
        }

        conf.confRelease();
        confHandle = 0;
        logInfo(TAG, "",  "ExitConf");

        mMainThreadID = 0;
        if (confThread != null)
        {
            confThread.getHandler().getLooper().quit();
            confThread.interrupt();
            confThread = null;
        }
        serviceConfig.setConfId("");
        conferenceInfo = null;
        ori = 0;
        rotatoFlag = false;
    }

    /**
     * 离开会议
     *
     * @return boolean
     */
    public boolean leaveConf()
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.CONF_OPER_LEAVE;
            mConfHandler.sendMessage(msg);
            return true;
        }
        int nRet = conf.confLeave();
        logInfo(TAG, "",  "leaveConference | nRet = " + nRet);
        return (nRet == 0);
    }

    /**
     * 锁定会议
     *
     * @return boolean
     */
    public boolean lockConf()
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.CONF_OPER_LOCK;
            mConfHandler.sendMessage(msg);

            return true;
        }
        int nRet = conf.confLock();
        return (nRet == 0);
    }

    /**
     * @param bMute bMute
     * @return boolean
     */
    public boolean muteConf(boolean bMute)
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = CONF_MUTE;
            msg.obj = bMute;
            mConfHandler.sendMessage(msg);
            return true;
        }
        int nRet = conf.confMute(bMute);
        return (nRet == 0);
    }

    /**
     * 会场静音设置
     *
     * @return boolean
     */
    public boolean unLockConf()
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.CONF_OPER_UNLOCK;
            mConfHandler.sendMessage(msg);
            return true;
        }

        int nRet = conf.confUnLock();
        return (nRet == 0);
    }

    /**
     * 加载组件
     *
     * @return boolean
     */
    public boolean loadComponent()
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.CONF_OPER_LOAD_COMPONENT;
            mConfHandler.sendMessage(msg);
            return true;
        }

        int nRet = conf.confLoadComponent(componentVal);
        logInfo(TAG, "",  "load componentVal:" + componentVal + "LoadComponent |  nRet = "
                + nRet + "check confHandle::::::" + confHandle + ",,,," + conf.getConfHandle());

        return (nRet == 0);
    }

    /**
     * 心跳
     */
    public void heartBeat()
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = CONF_HEARTBEAT;
            mConfHandler.sendMessage(msg);
            return;
        }
        conf.confHeartBeat();
    }

    /**
     * 终止会议
     *
     * @return boolean
     */
    public boolean terminateConf()
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.CONF_OPER_TERMINATE;
            mConfHandler.sendMessage(msg);
            return true;
        }

        int nRet = conf.confTerminate();
        logInfo(TAG, "",  "TerminateConf | nRet = " + nRet);

        return (nRet == 0);
    }

    /**
     * 踢人
     *
     * @param nUserID nUserID
     * @return boolean
     */
    public boolean kickout(int nUserID)
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.CONF_OPER_KICKOUT;
            msg.arg1 = nUserID;
            mConfHandler.sendMessage(msg);
            return true;
        }
        int nRet = conf.confUserKickout(nUserID);
        return (nRet == 0);
    }

    /**
     * 设置角色
     *
     * @param nUserID nUserID
     * @param nRole   nRole
     * @return boolean
     */
    public boolean setRole(int nUserID, int nRole)
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.CONF_OPER_SET_ROLE;
            msg.arg1 = nUserID;
            msg.arg2 = nRole;
            mConfHandler.sendMessage(msg);
            return true;
        }
        int nRet = conf.confUserSetRole(nUserID, nRole);
        return (nRet == 0);
    }

    /**
     * 设置音频参数
     *
     * @return boolean
     */
    public boolean setAudioParam()
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.AUDIO_OPER_SET_AUDIOPARAM;
            mConfHandler.sendMessage(msg);
            return true;
        }

        return false;
    }

    /**
     * 打开麦克风
     *
     * @param micID micID
     * @return boolean
     */
    public boolean openMic(int micID)
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.AUDIO_OPER_OPEN_MIC;
            mConfHandler.sendMessage(msg);
            return true;
        }

        int nRet = conf.audioOpenMic(micID);

        return (nRet == 0);
    }

    /**
     * 音频输入设备静音设置
     *
     * @param isMmute 0:静音; 1:取消静音
     * @return boolean
     */
    public boolean muteMic(int isMmute)
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.AUDIO_OPER_MUTE_MIC;
            msg.arg1 = isMmute;
            mConfHandler.sendMessage(msg);
            return true;
        }

        int nRet = conf.audioMuteMic();

        return (nRet == 0);
    }

    /**
     * 关闭音频输入设备
     *
     * @return boolean
     */
    public boolean closeMic()
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.AUDIO_OPER_CLOSE_MIC;
            mConfHandler.sendMessage(msg);
            return true;
        }
        int nRet = conf.audioCloseMic();
        return (nRet == 0);
    }

    /**
     * 打开音频输出设备
     *
     * @param speakerID speakerID
     * @return boolean
     */
    public boolean openSpeaker(int speakerID)
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.AUDIO_OPER_OPEN_SPEAKER;
            msg.arg1 = speakerID;
            mConfHandler.sendMessage(msg);

            return true;
        }

        int nRet = conf.audioOpenSpeaker(speakerID);

        return (nRet == 0);
    }

    /**
     * 音频输出设备静音设置
     *
     * @param isMmute 0:静音; 1:取消静音
     * @return boolean
     */
    public boolean muteSpeaker(int isMmute)
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.AUDIO_OPER_MUTE_SPEAKER;
            msg.arg1 = isMmute;
            mConfHandler.sendMessage(msg);
            return true;
        }
        int nRet = conf.audioMuteSpeaker();

        return (nRet == 0);
    }

    /**
     * 关闭音频输出设备
     *
     * @return boolean
     */
    public boolean closeSpeaker()
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.AUDIO_OPER_CLOSE_SPEAKER;
            mConfHandler.sendMessage(msg);
            return true;
        }

        int nRet = conf.audioCloseSpeaker();
        return (nRet == 0);
    }

    /**
     * 获取本地视频设备数量
     *
     * @return boolean
     */
    public boolean getVideoDeviceNum()
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.VIDEO_OPER_GETDEVICE_NUM;
            mConfHandler.sendMessage(msg);
            return true;
        }

        int nRet = conf.videoGetDeviceCount();
        return (nRet == 0);
    }

    /**
     * 设置视频编码端的最大宽和高。 Android对于不同的型号有不同的编码要求，
     * 如果需要请设置，不设置的话，采用默认值640*480
     *
     * @param xResolution xResolution
     * @param yResolution yResolution
     * @return boolean
     */
    public boolean setEncodeMaxResolution(int xResolution, int yResolution)
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.VIDEO_OPER_SETENCODE_MAXRESOLUTION;
            msg.arg1 = xResolution;
            msg.arg2 = yResolution;
            mConfHandler.sendMessage(msg);
            return true;
        }
        // 设置设备能编码的最大分辨率（针对发送方）
        // ，加载成功之后立即进行设置
        int nRet = conf.videoSetEncodeMaxResolution(xResolution, yResolution);
        return (nRet == 0);
    }


    /**
     * @param xRes   xRes
     * @param yRes   yRes
     * @param nFrame nFrame
     */
    public void setVideoParam(int xRes, int yRes, int nFrame)
    {
        VideoParams videoParams = new VideoParams();
        videoParams.setxRes(xRes);
        videoParams.setyRes(yRes);
        videoParams.setnFrame(nFrame);
        systemSetting.setVideoParams(videoParams);

        

        if (String.valueOf(userId).equals(
                serviceConfig.getSelfUserInfo().getUserId()))
        {
            viewGroupSetVideoparam = mLocalContainer;
            if (svLocalSurfaceView != null)
            {
                surfaceViewSetParam = svLocalSurfaceView;
            }
        }
        else
        {
            viewGroupSetVideoparam = mRemoteContainer;
            if (remoteSurfaceView != null)
            {
                surfaceViewSetParam = remoteSurfaceView;
            }
        }
        if (surfaceViewSetParam == null)
        {
            return;
        }
        if (viewGroupSetVideoparam == null)
        {
            return;
        }
        Configuration configuration = CCAPP.getInstances().getApplication()
                .getResources().getConfiguration();
        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT && cameraIndex == 2)
        {
            setRotate(270);
        }
        else if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT && cameraIndex == 1)
        {
            setRotate(90);
        }
        viewGroupSetVideoparam.removeAllViews();
        videoSetParam(userId, deviceId);
        logInfo(TAG, "", "videoParams -> addView");
    }

    /**
     * 设置视频参数
     *
     * @param nUserID  nUserID
     * @param deviceID deviceID
     * @return boolean
     */
    public boolean videoSetParam(int nUserID, String deviceID)
    {
        systemSetting.setnUserID(nUserID);
        systemSetting.setDeviceID(deviceID);
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.VIDEO_OPER_SETPARAM;
            msg.arg1 = nUserID;
            msg.obj = deviceID;
            mConfHandler.sendMessage(msg);

            return true;
        }

        VideoParams videoParams = systemSetting.getVideoParams();
        int xRes = videoParams.getxRes();
        int yRes = videoParams.getyRes();
        int nFrame = videoParams.getnFrame();
        int nRet = conf.videoSetParam(Long.parseLong(deviceID), xRes, yRes,
                nFrame, 0);

        logInfo(TAG, "", "SystemSetting videoParams :xRes=" + xRes + ", yRes=" + yRes
                + ", nFrame" + nFrame + "videoSetParam result:" + nRet);

        return (nRet == 0);
    }

    /**
     * 获取本地视频设备信息
     *
     * @return boolean
     */
    public boolean getVideoDeviceInfo()
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.VIDEO_OPER_GETDEVICE_INFO;
            mConfHandler.sendMessage(msg);
            return true;
        }

        int nRet = conf.videoGetDeviceInfo();
        return (nRet == 0);
    }

    /**
     * 打开视频
     *
     * @param nUserID  nUserID
     * @param deviceID deviceID
     * @return boolean
     */
    public boolean videoOpen(int nUserID, String deviceID)
    {
        logInfo(TAG, "", "videoOpen | userId = " + nUserID + ", deviceId = "
                + deviceID);
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.VIDEO_OPER_OPEN;
            msg.arg1 = nUserID;
            msg.obj = deviceID;
            mConfHandler.sendMessage(msg);

            return true;
        }
        int nRet = conf.videoOpen(Long.parseLong(deviceID));
        logInfo(TAG, "",  "videoOpen RESULT:" + nRet);
        return (nRet == 0);
    }

    /**
     * 关闭视频
     *
     * @param nUserID  nUserID
     * @param deviceID deviceID
     * @return boolean
     */
    public boolean videoClose(int nUserID, String deviceID)
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.VIDEO_OPER_CLOSE;
            msg.arg1 = nUserID;
            msg.obj = deviceID;
            mConfHandler.sendMessage(msg);

            return true;
        }

        int nRet = conf.videoClose(Long.parseLong(deviceID), false);
        return (nRet == 0);
    }

    /**
     * 暂停视频
     *
     * @param nUserID  nUserID
     * @param deviceID deviceID
     * @return boolean
     */
    public boolean videoPause(int nUserID, String deviceID)
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.VIDEO_OPER_PAUSE;
            msg.arg1 = nUserID;
            msg.obj = deviceID;
            mConfHandler.sendMessage(msg);

            return true;
        }
        int nRet = conf.videoPause(nUserID, Long.parseLong(deviceID));

        return (nRet == 0);
    }

    /**
     * 继续播放视频
     *
     * @param nUserID  nUserID
     * @param deviceID deviceID
     * @return boolean
     */
    public boolean videoResume(int nUserID, String deviceID)
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.VIDEO_OPER_RESUME;
            msg.arg1 = nUserID;
            msg.obj = deviceID;
            mConfHandler.sendMessage(msg);

            return true;
        }

        int nRet = conf.videoResume(nUserID, Long.parseLong(deviceID));
        return (nRet == 0);
    }

    /**
     * 得到视频设备能力个数
     *
     * @param nUserID  nUserID
     * @param deviceID deviceID
     * @return boolean
     */
    public boolean videGetDevicecApbilityNum(int nUserID, String deviceID)
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.VIDEO_OPER_GETDEVICECAPBILITY_NUM;
            msg.arg1 = nUserID;
            msg.obj = deviceID;
            mConfHandler.sendMessage(msg);
            return true;
        }

        String videoGetCapbiltityNum = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<MSG type = \"VideoGetCapbilityNum\">"
                + "<version>1</version>"
                + "<userid>%d</userid> "
                + "<deviceid>%s</deviceid>" + "</MSG>";

        videoGetCapbiltityNum = String.format(videoGetCapbiltityNum, nUserID,
                deviceID);

        int nRet = Conference.getInstance().confHandleMsg(conf.getConfHandle(),
                ConfOper.VIDEO_OPER_GETDEVICECAPBILITY_NUM,
                videoGetCapbiltityNum, null);
        logInfo(TAG, "",  "videGetDevicecApbilityNum result: " + nRet);
        return (nRet == 0);
    }

    /**
     * 得到视频设备能力信息
     *
     * @param nUserID  nUserID
     * @param deviceID deviceID
     * @return boolean
     */
    public boolean videoGetDevicecApbilityInfo(int nUserID, String deviceID)
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.VIDEO_OPER_GETDEVICECAPBILITY_INFO;
            msg.arg1 = nUserID;
            msg.obj = deviceID;
            mConfHandler.sendMessage(msg);
            return true;
        }

        String videoGetCapbiltityInfo = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<MSG type = \"VideoGetCapbilityNum\">"
                + "<version>1</version>"
                + "<userid>%d</userid> "
                + "<deviceid>%s</deviceid>" + "</MSG>";
        videoGetCapbiltityInfo = String.format(videoGetCapbiltityInfo, nUserID,
                deviceID);

        int nRet = Conference.getInstance().confHandleMsg(conf.getConfHandle(),
                ConfOper.VIDEO_OPER_GETDEVICECAPBILITY_INFO,
                videoGetCapbiltityInfo, null);
        logInfo(TAG, "",  "videoGetDevicecApbilityInfo result: " + nRet + " ,deviceID:"
                + deviceID);
        return (nRet == 0);
    }

    /**
     * 获取视频流信息
     *
     * @return boolean
     */
    public boolean getVideoStream()
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = VIDEO_GETPARAM;
            mConfHandler.sendMessage(msg);
            return true;
        }
        int nRet = conf.videoGetParam(userId, Long.parseLong(deviceId));
        return (nRet == 0);
    }

    /**
     * 获取视频参数
     *
     * @param nUserID  nUserID
     * @param deviceID deviceID
     * @return boolean
     */
    public boolean videoGetParam(int nUserID, String deviceID)
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.VIDEO_OPER_GETPARAM;
            msg.arg1 = nUserID;
            msg.obj = deviceID;
            mConfHandler.sendMessage(msg);

            return true;
        }
        int nRet = conf.videoGetParam(nUserID, Long.parseLong(deviceID));

        return (nRet == 0);
    }


    /**
     * 设置角度
     *
     * @param rotate rotate
     * @return boolean
     */
    public boolean setRotate(int rotate)
    {
        return videoSetCaptureRotate(userId, deviceId, rotate);
    }

    /**
     * 视频旋转
     *
     * @param nUserID  nUserID
     * @param deviceID deviceID
     * @param rotate   rotate
     * @return boolean
     */
    public boolean videoSetCaptureRotate(int nUserID, String deviceID, int rotate)
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.VIDEO_OPER_SET_CAPTURE_ROTATE;
            msg.arg1 = nUserID;
            msg.arg2 = rotate;
            msg.obj = deviceID;
            mConfHandler.sendMessage(msg);

            return true;
        }
        int mRotate = rotate;
        if (ori == 1 && rotatoFlag)
        {
            mRotate = 270 - rotate;
        }
        int nRet = conf.videoSetCaptureRotate(nUserID,
                Long.parseLong(deviceID), mRotate);
        return (nRet == 0);
    }

    /**
     * 设置视频依附于页面
     * 远端视频数据与窗口进行绑定(针对接收方)
     *
     * @param indexWnd indexWnd
     * @param userid   userid
     * @param deviceid deviceid
     * @return boolean
     */
    public boolean videoAttach(int indexWnd, int userid, String deviceid)
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.VIDEO_OPER_ATTACH;
            msg.arg1 = indexWnd;
            msg.arg2 = userid;
            msg.obj = deviceid;
            mConfHandler.sendMessage(msg);
            return true;
        }

        int videoShowMode = 0; //全屏
        int nRet = conf.videoAttach(userid, Long.parseLong(deviceid), indexWnd,
                1, videoShowMode);
        return (nRet == 0);
    }

    /**
     * 设置视频分离页面
     * 远端视频数据与窗口进行解除绑定(针对接收方)
     *
     * @param indexWnd indexWnd
     * @param userid   userid
     * @param deviceid deviceid
     * @return boolean
     */
    public boolean videoDetach(int indexWnd, int userid, String deviceid)
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.VIDEO_OPER_DETACH;
            msg.arg1 = indexWnd;
            msg.arg2 = userid;
            msg.obj = deviceid;
            mConfHandler.sendMessage(msg);

            return true;
        }

        int nRet = conf.videoDetach(userid, Long.parseLong(deviceid), indexWnd,
                false);
        return (nRet == 0);
    }

    /**
     * 本地视频截图-暂时保留这个接口
     *
     * @param userid   userid
     * @param deviceid deviceid
     * @param filename filename
     * @return boolean
     */

    /**
     * 发送信令消息给VTA
     *
     * @param nUserID    nUserID
     * @param msgID      msgID
     * @param optContext optContext
     * @return boolean
     */
    public boolean sendData(int nUserID, int msgID, byte[] optContext)
    {
        byte[] mOptContext = optContext;
        if (optContext == null)
        {
            mOptContext = "".getBytes(Charset.forName(NotifyMessage.CC_CHARSET_GBK));
        }
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.CONF_OPER_SEND_DATA;
            msg.arg1 = nUserID;
            msg.obj = mOptContext;
            Bundle data = new Bundle();
            data.putInt("msgID", msgID);
            data.putByteArray("optContext", mOptContext);
            msg.setData(data);
            mConfHandler.sendMessage(msg);

            return true;
        }

        String loadcomponent = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<MSG type = \"SendData\">"
                + "<version>1</version>"
                + "<userid>%d</userid> " + "<intparam>%d</intparam>" + "</MSG>";

        loadcomponent = String.format(loadcomponent, nUserID, msgID);

        int nRet = Conference.getInstance().confHandleMsg(conf.getConfHandle(),
                ConfOper.CONF_OPER_SEND_DATA, loadcomponent, optContext);
        logInfo(TAG, "",  "sendData result :" + nRet);
        if (turnCaptureFlag)
        {
            openLocalVideo();
            turnCaptureFlag = false;
        }
        return (nRet == 0);
    }

    /**
     * 通知某他人打开或是关闭视频
     *
     * @param userid   userid
     * @param deviceid deviceid
     * @param isOpen   1:open; 2:close
     * @return boolean
     */
    public boolean videoNotifyOpen(int userid, String deviceid, int isOpen)
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.VIDEO_OPER_NOTIFY;
            msg.arg1 = isOpen; // 1:open; 2:close
            msg.arg2 = userid;
            msg.obj = deviceid;
            mConfHandler.sendMessage(msg);

            return true;
        }

        int nRet = conf.videoNotifyOpen(userid, Long.parseLong(deviceid), 176,
                144, 10);
        return (nRet == 0);
    }

    /**
     * 发送消息
     * chat
     *
     * @param nDstID nDstID
     * @param strMsg strMsg
     * @return boolean
     */
    public boolean chatSendMsg(int nDstID, String strMsg)
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.CHAT_OPER_SEND;
            msg.arg1 = nDstID;
            msg.obj = strMsg;
            mConfHandler.sendMessage(msg);

            return true;
        }

        int nRet = conf.chatSendMsg(1, 0, strMsg);
        return (nRet == 0);
    }

    /**
     * 文档共享 —— 设置当前页码
     *
     * @param nDocID  nDocID
     * @param nPageID nPageID
     * @return boolean
     */
    public boolean dsSetcurrentpage(int nDocID, int nPageID)
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.DS_OPER_SET_CURRENTPAGE;
            msg.arg1 = nDocID;
            msg.arg2 = nPageID;
            mConfHandler.sendMessage(msg);

            return true;
        }
        int nRet = conf.dsSetCurrentPage(nDocID, nPageID);

        return (nRet == 0);
    }

    /**
     * 设置屏幕共享参数
     *
     * @param value 1:开启；0：关闭
     * @return boolean
     */
    public boolean asSetParam(int value)
    {
        if (isMainThread())
        {
            Message msg = new Message();
            msg.what = ConfOper.AS_OPER_SET_PARAM;
            msg.arg1 = value;
            mConfHandler.sendMessage(msg);

            return true;
        }

        int nRet = conf.asSetParam(ConfDefines.AS_PROP_SAMPLING, value);
        logInfo(TAG, "",  "as_set_param end | value = " + value + ", nRet = "
                + nRet);

        return (nRet == 0);
    }

    /**
     * 加载DS组件的两个接口 应该都为会议线程
     *
     * @param compid compid
     * @return int
     */
    public int annotRegCustomerType(int compid)
    {
        return conf.annotRegCustomerType(compid);

    }

    /**
     * 会议回调消息通知
     *
     * @param msg       msg
     * @param extendMsg extendMsg
     */
    @Override
    public void confMsgNotify(ConfMsg msg, ConfExtendMsg extendMsg)
    {
        long nValue1 = msg.getnValue1();
        long nValue2 = msg.getnValue2();
        int msgType = msg.getMsgType();

        logInfo(TAG, "",  "msgType = " + msgType + " , nValue1 = " + nValue1
                + " , nValue2 = " + nValue2);

        BroadMsg broadMsg = new BroadMsg();
        switch (msgType)
        {
            case ConfMsg.CONF_MSG_ON_CONFERENCE_TERMINATE:
                //这里是收到了结束会议的通知
                LogUtil.d(TAG, "=========结束会议=======");
                MobileCC.getInstance().releaseCall();

                break;
            case ConfMsg.CONF_MSG_ON_CONFERENCE_LEAVE:
                break;
            case ConfMsg.CONF_MSG_ON_CONFERENCE_JOIN:
                clearVideoParamsMap();

                broadMsg.setAction(NotifyMessage.CALL_MSG_USER_JOIN);
                RequestCode requestCode = new RequestCode();
                requestCode.setRetCode(String.valueOf(nValue1));
                broadMsg.setRequestCode(requestCode);

                serviceConfig.addUser(
                        new ConferenceUserInfo(serviceConfig.getUserId()));
                if (nValue1 == ConfResult.TC_OK && (serviceConfig.getAnonyCallMode() == 1))
                {
                    loadComponent();
                }

                CCAPP.getInstances().sendBroadcast(broadMsg);

                break;
            case ConfMsg.CONF_MSG_USER_ON_LEAVE_IND:
            {
                ConfExtendUserInfoMsg infoMsg = (ConfExtendUserInfoMsg) extendMsg;
                if (infoMsg != null)
                {
                    String userId = infoMsg.getUserid() + "";

                    serviceConfig.removeUser(userId);
                    broadMsg.setAction(NotifyMessage.CONF_USER_LEAVE_EVENT);
                    RequestInfo requestInfo = new RequestInfo();
                    requestInfo.setMsg(userId);
                    broadMsg.setRequestInfo(requestInfo);
                    CCAPP.getInstances().sendBroadcast(broadMsg);
                }

            }
            break;
            case ConfMsg.CONF_MSG_USER_ON_ENTER_IND:
            {
                ConfExtendUserInfoMsg infoMsg = (ConfExtendUserInfoMsg) extendMsg;
                if (infoMsg != null)
                {
                    String userId = infoMsg.getUserid() + "";
                    serviceConfig.addUser(new ConferenceUserInfo(userId));
                    broadMsg.setAction(NotifyMessage.CONF_USER_ENTER_EVENT);

                    RequestInfo requestInfo = new RequestInfo();
                    requestInfo.setMsg(userId);
                    broadMsg.setRequestInfo(requestInfo);
                    CCAPP.getInstances().sendBroadcast(broadMsg);
                }

            }
            break;
            case ConfMsg.CONF_MSG_USER_ON_MESSAGE_IND: // 用户收到消息
                ConfExtendUserDataMsg rawData = (ConfExtendUserDataMsg) extendMsg;
                if (rawData != null)
                {
                    String fromId = rawData.getFromuserid() + "";
                    byte[] data = rawData.getUserData();
                    msgType = (int) rawData.getMsgtype();
                    handleUserMsg(fromId, msgType, data);

                }

                break;
            case ConfMsg.CONF_MSG_ON_DISCONNECT: // 网络故障（导致会议终止）
                broadMsg.setAction(NotifyMessage.CALL_MSG_USER_NETWORK_ERROR);
                CCAPP.getInstances().sendBroadcast(broadMsg);
                break;
            case ConfMsg.CONF_MSG_ON_RECONNECT:
                broadMsg.setAction(NotifyMessage.CONF_RECONNECTED);
                CCAPP.getInstances().sendBroadcast(broadMsg);
                break;
            case ConfMsg.CONF_MSG_ON_COMPONENT_LOAD:
                switch ((int) nValue2)
                {
                    case ConfDefines.IID_COMPONENT_VIDEO:
                        // Android对于不同的型号有不同的编码要求，
                        // 如果需要请设置，不设置的话，采用默认值640*480
                        setEncodeMaxResolution(640, 480);
                        getVideoDeviceNum();
                        getVideoDeviceInfo();
                        break;
                    case ConfDefines.IID_COMPONENT_DS:
                        annotRegCustomerType(ConfDefines.IID_COMPONENT_DS);
                        break;
                    case ConfDefines.IID_COMPONENT_AS:
                        break;
                    default:
                        break;
                }
                break;
            case ConfMsg.COMPT_MSG_VIDEO_ON_GETDEVICE_NUM:
            case ConfMsg.COMPT_MSG_VIDEO_ON_GETDEVICE_INFO:
            case ConfMsg.COMPT_MSG_VIDEO_ON_DEVICE_INFO:
            case ConfMsg.COMPT_MSG_VIDEO_ON_DEVICECAPBILITY_NUM:
            case ConfMsg.COMPT_MSG_VIDEO_ON_DEVICECAPBILITY_INFO:
            case ConfMsg.COMPT_MSG_VIDEO_ON_SWITCH:
            case ConfMsg.COMPT_MSG_VIDEO_ON_FIRST_KEYFRAME:
            case ConfMsg.COMPT_MSG_VIDEO_ON_SNAPSHOTDATA:
                confMsgNotifyVideo(msg, extendMsg);
                break;
            case ConfMsg.COMPT_MSG_AS_ON_SCREEN_DATA:
            case ConfMsg.COMPT_MSG_AS_ON_SHARING_SESSION:
            case ConfMsg.COMPT_MSG_AS_ON_SCREEN_SIZE:
            case ConfMsg.COMPT_MSG_AS_ON_SHARING_STATE:
                confMsgNotifyAs(msg, extendMsg);
                break;
            case ConfMsg.COMPT_MSG_DS_ANDROID_DOC_COUNT:
            case ConfMsg.COMPT_MSG_DS_ON_DOC_NEW:
            case ConfMsg.COMPT_MSG_DS_ON_DOC_DEL:
            case ConfMsg.COMPT_MSG_DS_ON_PAGE_NEW:
            case ConfMsg.COMPT_MSG_DS_ON_PAGE_DEL:
            case ConfMsg.COMPT_MSG_DS_ON_CURRENT_PAGE:
            case ConfMsg.COMPT_MSG_DS_ON_DRAW_DATA_NOTIFY:
            case ConfMsg.COMPT_MSG_DS_PAGE_DATA_DOWNLOAD:
            case ConfMsg.COMPT_MSG_DS_ON_CURRENT_PAGE_IND:
                confMsgNotifyDs(msg, extendMsg);
                break;
            case ConfMsg.COMPT_MSG_VIDEO_GETPARAM:
                if (extendMsg != null)
                {
                    confMsgNotifyVideo(msg, extendMsg);
                }
                break;

            case ConfMsg.COMPT_MSG_VIDEO_SETPARAM:

                logInfo(TAG, "", "COMPT_MSG_VIDEO_SETPARAM:");
                Message setParam = new Message();
                setParam.what = VIDEO_SETPARAM;
                videoHandler.sendMessage(setParam);

                break;
            case ConfMsg.COMPT_MSG_VIDEO_ON_RECONNECT:
                logInfo(TAG, "", "VIDEO_ON_RECONNECT:");
                Message reconnect = new Message();
                reconnect.what = VIDEO_ON_RECONNECT;
                videoHandler.sendMessage(reconnect);
                break;
            default:
                break;
        }
    }

    public void setConferenceInfo(ConferenceInfo conferenceInfo)
    {
        this.conferenceInfo = conferenceInfo;
    }


    /**
     * 处理会议中其他用户通过会议通道发送过来的普通消息
     *
     * @param fromUserId fromUserId
     * @param msgType    msgType
     * @param data       data
     */
    private void handleUserMsg(String fromUserId, int msgType, byte[] data)
    {
        switch (msgType)
        {
            case NotifyMessage.CONF_NEGOTIATE_MSG:// 信令协商消息
            {
                serviceConfig.addUser(new ConferenceUserInfo(fromUserId));
                if (data != null)
                {
                    String dataStr = new String(data,
                            Charset.forName(NotifyMessage.CC_CHARSET_GBK));
                    try
                    {
                        JSONObject jObject = new JSONObject(dataStr);
                        JSONObject jObjectDataSend = createSendMsg(jObject);
                        if (jObjectDataSend == null)
                        {
                            return;
                        }
                        sendData(
                                AddressTools.parseInt(fromUserId),
                                NotifyMessage.CONF_NEGOTIATE_RESPOND_MSG,
                                jObjectDataSend.toString().getBytes(
                                        Charset.forName(NotifyMessage.CC_CHARSET_GBK)));
                    } catch (JSONException e)
                    {
                        logInfo(TAG, "", "json exception");
                    }
                }
            }
            break;

            case NotifyMessage.CONF_MONITOR_JOIN_MSG: // 质检员加入
            break;

            default:
                break;
        }
    }



    /**
     * 视频相关回调通知
     *
     * @param msg       msg
     * @param extendMsg extendMsg
     */
    private void confMsgNotifyVideo(ConfMsg msg, ConfExtendMsg extendMsg)
    {
        int msgType = msg.getMsgType();
        int nValue1 = msg.getnValue1();
        long nValue2 = msg.getnValue2();
        switch (msgType)
        {
            case ConfMsg.COMPT_MSG_VIDEO_ON_GETDEVICE_NUM:
            {
                logInfo(TAG, "", "COMPT_MSG_VIDEO_ON_GETDEVICE_NUM num:" + nValue1);


                ConferenceUserInfo conferenceUserInfo = serviceConfig.getSelfUserInfo();
                conferenceUserInfo.setDeviceNum(nValue1);
                serviceConfig.addUser(conferenceUserInfo);
            }
            break;
            case ConfMsg.COMPT_MSG_VIDEO_ON_GETDEVICE_INFO: // 取得自己的设备信息
            {
                if (extendMsg == null)
                {
                    break;
                }
                logInfo(TAG, "",  "COMPT_MSG_VIDEO_ON_GETDEVICE_INFO extendMsg not null");
                ConfExtendVideoDeviceInfoMsg rawData = (ConfExtendVideoDeviceInfoMsg) extendMsg;

                if (rawData != null)
                {
                    String deviceId = rawData.getDeviceId() + "";
                    String deviceName = rawData.getDeviceName();

                    CameraInfo selfCameraInfo = new CameraInfo(nValue1, deviceId,
                            deviceName);

                    ConferenceUserInfo conferenceUserInfo = serviceConfig.getSelfUserInfo();

                    conferenceUserInfo.addCameraInfo(selfCameraInfo);

                    serviceConfig.addUser(conferenceUserInfo);

                    int nUserID = AddressTools.parseInt(conferenceUserInfo.getUserId());
                    logInfo(TAG, "",  "COMPT_MSG_VIDEO_ON_GETDEVICE_INFO nUserID:" + nUserID
                            + " ,deviceId:" + deviceId + " ,deviceName:" + deviceName);
                    videGetDevicecApbilityNum(nUserID, deviceId);
                    videoGetDevicecApbilityInfo(nUserID, deviceId);

                    if (serviceConfig.getAnonyCallMode() == 1)
                    {
                        openLocalVideo();
                    }
                }
            }
            break;
            case ConfMsg.COMPT_MSG_VIDEO_ON_DEVICECAPBILITY_NUM: // 获取视频设备能力个数
            {
            }
            break;

            case ConfMsg.COMPT_MSG_VIDEO_GETPARAM:
            {
                ConfExtendVideoParamMsg rawData = (ConfExtendVideoParamMsg) extendMsg;
                if (rawData != null)
                {
                    //获取到视屏流
                    BroadMsg broadMsg = new BroadMsg(NotifyMessage.CALL_MSG_GET_VIDEO_INFO);

                    StreamInfo streamInfo = new StreamInfo();

                    streamInfo.setEncoderSize(rawData.getXresolution() + "*" + rawData.getYresolution());
                    streamInfo.setSendFrameRate(rawData.getFramerate());
                    streamInfo.setVideoSendBitRate(rawData.getBitrate());

                    RequestInfo requestInfo = new RequestInfo();
                    requestInfo.setStreamInfo(streamInfo);
                    broadMsg.setRequestInfo(requestInfo);
                    CCAPP.getInstances().sendBroadcast(broadMsg);

                    logInfo(TAG, "",  "stream info " + rawData.getXresolution() + "*"
                            + rawData.getYresolution() + "nFrame is: " + rawData.getFramerate()
                            + "BitRat is" + rawData.getBitrate());
                }
                else
                {
                    logError(TAG, "", "stream info is null");
                }
                break;
            }
            case ConfMsg.COMPT_MSG_VIDEO_ON_DEVICECAPBILITY_INFO: // 获取视频设备能力信息
            {
                ConfExtendVideoParamMsg rawData = (ConfExtendVideoParamMsg) extendMsg;
                if (rawData != null)
                {
                    String deviceID = rawData.getDeviceId() + "";
                    int nFrame = rawData.getFramerate();
                    VideoParams videoParams = new VideoParams();
                    videoParams.setxRes(rawData.getXresolution());
                    videoParams.setyRes(rawData.getYresolution());
                    videoParams.setnFrame(nFrame);
                    videoParams.setnBitRate(rawData.getBitrate());
                    addVideoParamsMap(deviceID, videoParams);
                }

            }
            break;
            case ConfMsg.COMPT_MSG_VIDEO_ON_DEVICE_INFO:
                // 设备添加或是删除:(包括自己和别人)
            {
                if (extendMsg != null)
                {
                    ConfExtendVideoDeviceInfoMsg fromMsg = (ConfExtendVideoDeviceInfoMsg) extendMsg;
                    if (fromMsg != null)
                    {
                        logInfo(TAG, "",  "deviceID:" + fromMsg.getDeviceId() + ", orientation = "
                                + fromMsg.getDeviceStatus() + "userId:" + fromMsg.getUserId());
                    }

                    if (nValue1 == 1) // 0:删除 1:添加
                    {
                        String userid = fromMsg.getUserId() + "";
                        ConferenceUserInfo selfConferenceUserInfo = serviceConfig
                                .getSelfUserInfo();
                        if (!selfConferenceUserInfo.getUserId().equals(userid))
                        {
                            String deviceid = fromMsg.getDeviceId() + "";
                            String deviceName = fromMsg.getDeviceName();
                            CameraInfo fromCameraInfo = new CameraInfo(deviceid,
                                    deviceName);
                            ConferenceUserInfo conferenceUserInfo = serviceConfig.getUserInfo(
                                    userid);
                            conferenceUserInfo.addCameraInfo(fromCameraInfo);
                        }
                    }
                }


            }
            break;
            case ConfMsg.COMPT_MSG_VIDEO_ON_FIRST_KEYFRAME: // 返回解码后的第一个关键帧
            break;

            case ConfMsg.COMPT_MSG_VIDEO_ON_SWITCH: // 视频状态相关:1:打开 0:关闭 2:Resume
                ConfExtendVideoParamMsg paramMsg = (ConfExtendVideoParamMsg) extendMsg;
                String deviceid = "";

                if (paramMsg != null)
                {
                    deviceid = "" + paramMsg.getDeviceId();
                }


                if (nValue1 == 1 || nValue1 == 2) // 1:打开 2:Resume
                {
                    if (nValue2 == AddressTools.parseInt(serviceConfig
                            .getUserId())) // 本地
                    {
                        logInfo(TAG, "",  "COMPT_MSG_VIDEO_ON_SWITCH CC nValue1:"
                                + nValue1);
                        if (mLocalContainer != null && svLocalSurfaceView != null)
                        {
                            if (nValue1 == 1)
                            {

                                Message vtmopenmsg = new Message();
                                vtmopenmsg.what = VTM_OPEN_VIDEO;
                                videoHandler.sendMessage(vtmopenmsg);

                                int indexOflocalSurfaceView = ViERenderer
                                        .getIndexOfSurface(localSurfaceView);
                                int userid = Integer.parseInt(serviceConfig
                                        .getSelfUserInfo().getUserId());
                                Configuration configuration = CCAPP.getInstances().getApplication()
                                        .getResources().getConfiguration();
                                LogUtil.d(TAG, "configuration.orientation == " + configuration.orientation);
                                if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT && cameraIndex == 1)
                                {
                                    LogUtil.d(TAG, "front camera");
                                    setRotate(270);
                                    cameraIndex = 2;
                                }
                                else if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT && cameraIndex == 2)
                                {
                                    LogUtil.d(TAG, "back camera");
                                    setRotate(90);
                                    cameraIndex = 1;
                                }

                                boolean attachRet = videoAttach(indexOflocalSurfaceView, userid, deviceid);
                                logInfo(TAG, "",  "COMPT_MSG_VIDEO_ON_SWITCH attachRet:" + attachRet
                                        + " ,userid:" + userid + " ,deviceid:" + deviceid
                                        + " ,indexOflocalSurfaceView:" + indexOflocalSurfaceView);
                            }
                            else if (nValue1 == 2)
                            {
                                Message vtmresumemsg = new Message();
                                vtmresumemsg.what = VTM_RESUME_VIDEO;
                                videoHandler.sendMessage(vtmresumemsg);

                            }
                        }
                    }
                    else
                    // 远端VTA
                    {
                        logInfo(TAG, "",  "COMPT_MSG_VIDEO_ON_SWITCH VTA nValue1:"
                                + nValue1);
                        vtaOpenVideo((int) nValue2, deviceid);
                        serviceConfig.setVtaDeviceId(deviceid);
                    }
                }
                else if (nValue1 == 0) // 关闭
                {
                    if (nValue2 == AddressTools.parseInt(serviceConfig
                            .getUserId())) // 本地
                    {
                        if (mLocalContainer != null && svLocalSurfaceView != null)
                        {
                            Message vtmclosemsg = new Message();
                            vtmclosemsg.what = VTM_CLOSE_VIDEO;
                            videoHandler.sendMessage(vtmclosemsg);

                        }
                    }
                    else if (nValue2 == AddressTools.parseInt(serviceConfig
                            .getVTAUserInfos().get(0).getUserId()))
                    // 远端VTA
                    {
                        // 把窗口置空,必须的
                        if (mRemoteContainer != null && remoteSurfaceView != null)
                        {
                            int indexOfSurface = ViERenderer
                                    .getIndexOfSurface(remoteSurfaceView);
                            if (videoDetach(indexOfSurface, (int) nValue2, deviceid))
                            {
                                Message vtaclosemsg = new Message();
                                vtaclosemsg.what = VTA_CLOSE_VIDEO;
                                videoHandler.sendMessage(vtaclosemsg);
                            }
                        }
                    }
                    else
                    {
                        logInfo(TAG, "",  "[video], tripartite video closed ");

                    }
                }
                BroadMsg broadMsg = new BroadMsg(
                        NotifyMessage.COMPT_VIDEO_SWITCH_EVENT);
                RequestCode requestCode = new RequestCode();
                requestCode.setRetCode(String.valueOf(nValue1));
                broadMsg.setRequestCode(requestCode);

                if (nValue2 == AddressTools.parseInt(serviceConfig.getUserId()))
                {
                    RequestInfo requestInfo = new RequestInfo();
                    // msg: 0 本地，1 远端
                    requestInfo.setMsg(String.valueOf(0));
                    broadMsg.setRequestInfo(requestInfo);
                }
                else
                {
                    RequestInfo requestInfo = new RequestInfo();
                    // msg: 0 本地，1 远端
                    requestInfo.setMsg(String.valueOf(1));
                    broadMsg.setRequestInfo(requestInfo);
                }
                CCAPP.getInstances().sendBroadcast(broadMsg);
                break;
            case ConfMsg.COMPT_MSG_VIDEO_ON_SNAPSHOTDATA:
            break;

            default:
                break;
        }
    }

    /**
     * 远端 VTA视频打开
     *
     * @param userID   userID
     * @param deviceid deviceid
     */
    private void vtaOpenVideo(int userID, String deviceid)
    {
        try
        {
            Thread.sleep(50);
        } catch (InterruptedException e)
        {
        }

        ConferenceUserInfo conferenceUserInfo = serviceConfig.getUserInfo(
                String.valueOf(userID));

        List<CameraInfo> cameraInfos = conferenceUserInfo.getCameraInfos();
        CameraInfo cameraInfo1;
        // 获取指定视频设备编号的视频设备信息
        for (int i = 0; i < cameraInfos.size(); i++)
        {
            cameraInfo1 = cameraInfos.get(i);
            if (deviceid.equals(cameraInfo1.getDeviceID()))
            {
                cameraInfos.remove(cameraInfo1);
                cameraInfos.add(0, cameraInfo1);
                break;
            }
        }
        // 判断用户ID为是否为VTA端被叫用户（即非第三方用户）
        if (userID == AddressTools.parseInt(serviceConfig
                .getVTAUserInfos().get(0).getUserId()))
        {
            if (mRemoteContainer != null && remoteSurfaceView != null)
            {
                int indexOfSurface = ViERenderer
                        .getIndexOfSurface(remoteSurfaceView);
                if (videoAttach(indexOfSurface, (int) userID, deviceid))
                {

                    Message msg = new Message();
                    msg.what = VTA_OPEN_VIDEO;
                    videoHandler.sendMessage(msg);

                    ConferenceUserInfo attachUser = serviceConfig
                            .getUserInfo(String.valueOf(userID));

                    serviceConfig.setAttachUser(attachUser);

                }
            }
            else
            {
                logInfo(TAG, "",  "COMPT_MSG_VIDEO_ON_SWITCH,mLlRemoteSurface is null");
            }
        }
        else
        {
            logInfo(TAG, "",  "[video], tripartite video opened ");
        }
    }

    /**
     * 切换摄像头
     */
    public void switchCamera()
    {

        videoClose(userId, deviceId);
        if (0 == flag % 2)
        {
            openBackVideo();
        }
        else if (1 == flag % 2)
        {
            openLocalVideo();
        }
        flag++;
    }

    private void openLocalVideo()
    {
        // 打开前置摄像头
        ConferenceUserInfo selfInfo = serviceConfig.getSelfUserInfo();
        List<CameraInfo> selfCameraInfos = selfInfo.getCameraInfos();
        if (selfInfo.getDeviceNum() != selfCameraInfos.size())
        {
            logInfo(TAG, "",  "openLocalVideo error1");
            return;
        }
        int nSelfUserID = AddressTools.parseInt(selfInfo.getUserId());
        String selfDeviceID;
        CameraInfo cameraInfo = null;
        for (int i = 0; i < selfCameraInfos.size(); i++)
        {
            cameraInfo = selfCameraInfos.get(i);
            logInfo(TAG, "",  "cameraInfo  -> "
                    + cameraInfo.getDeviceName() + "\t" + cameraInfo.getDeviceID());
            if (cameraInfo.getDeviceName().contains("front"))
            {
                break;
            }

        }
        if (cameraInfo != null)
        {
            selfCameraInfos.remove(cameraInfo);
            selfCameraInfos.add(0, cameraInfo);

            selfDeviceID = cameraInfo.getDeviceID();
            videoOpen(nSelfUserID, selfDeviceID);
            userId = nSelfUserID;
            deviceId = selfDeviceID;
        }
    }

    private void openBackVideo()
    {
        // 打开后置摄像头
        ConferenceUserInfo selfInfo = serviceConfig.getSelfUserInfo();
        List<CameraInfo> selfCameraInfos = selfInfo.getCameraInfos();
        if (selfInfo.getDeviceNum() != selfCameraInfos.size())
        {
            logInfo(TAG, "",  "openLocalVideo error1");

            return;
        }
        int nSelfUserID = AddressTools.parseInt(selfInfo.getUserId());
        String selfDeviceID;
        CameraInfo cameraInfo = null;
        for (int i = 0; i < selfCameraInfos.size(); i++)
        {
            cameraInfo = selfCameraInfos.get(i);
            if (cameraInfo.getDeviceName().contains("back"))
            {
                break;
            }

        }
        if (cameraInfo != null)
        {
            selfCameraInfos.remove(cameraInfo);
            selfCameraInfos.add(0, cameraInfo);

            selfDeviceID = cameraInfo.getDeviceID();
            videoOpen(nSelfUserID, selfDeviceID);
            userId = nSelfUserID;
            deviceId = selfDeviceID;
        }
    }


    private void confMsgNotifyAs(ConfMsg msg, ConfExtendMsg extendMsg)
    {
        int msgType = msg.getMsgType();
        int nValue1 = msg.getnValue1();
        int nValue2 = (int) msg.getnValue2();

        switch (msgType)
        {
            case ConfMsg.COMPT_MSG_AS_ON_SHARING_SESSION:
                if (nValue1 == ConfDefines.AS_SESSION_OWNER)
                {
                    if (nValue2 == ConfDefines.AS_ACTION_ADD
                            || nValue2 == ConfDefines.AS_ACTION_MODIFY)
                    {
                        BroadMsg broadMsg = new BroadMsg(
                                NotifyMessage.CALL_MSG_USER_RECEIVE_SHARED_DATA);

                        RequestCode requestCode = new RequestCode();
                        requestCode.setRetCode(String
                                .valueOf(ConfDefines.IID_COMPONENT_AS));
                        broadMsg.setRequestCode(requestCode);


                        RequestInfo requestInfo = new RequestInfo();
                        requestInfo.setMsg(String.valueOf(CONF_SHARED_START));
                        broadMsg.setRequestInfo(requestInfo);
                        CCAPP.getInstances().sendBroadcast(broadMsg);
                    }
                }
                updateDeskmsg = new Message();
                updateDeskmsg.what = UPDATE_DESKVIEW;
                videoHandler.sendMessage(updateDeskmsg);
                break;
            case ConfMsg.COMPT_MSG_AS_ON_SHARING_STATE:
                if (nValue2 == ConfDefines.AS_STATE_NULL)
                {
                    Message releaseDeskmsg = new Message();
                    releaseDeskmsg.what = RELEASE_DESKVIEW;
                    videoHandler.sendMessage(releaseDeskmsg);
                    BroadMsg broadMsg = new BroadMsg(
                            NotifyMessage.CALL_MSG_USER_RECEIVE_SHARED_DATA);

                    RequestCode requestCode = new RequestCode();
                    requestCode.setRetCode(String
                            .valueOf(ConfDefines.IID_COMPONENT_AS));
                    broadMsg.setRequestCode(requestCode);


                    RequestInfo requestInfo = new RequestInfo();
                    requestInfo.setMsg(String.valueOf(CONF_SHARED_STOP));
                    broadMsg.setRequestInfo(requestInfo);

                    CCAPP.getInstances().sendBroadcast(broadMsg);
                }
                updateDeskmsg = new Message();
                updateDeskmsg.what = UPDATE_DESKVIEW;
                videoHandler.sendMessage(updateDeskmsg);
                break;
            default:
                if (msgType == ConfMsg.COMPT_MSG_AS_ON_SCREEN_SIZE
                        || msgType == ConfMsg.COMPT_MSG_AS_ON_SCREEN_DATA)
                {
                    updateDeskmsg = new Message();
                    updateDeskmsg.what = UPDATE_DESKVIEW;
                    videoHandler.sendMessage(updateDeskmsg);
                }
                break;
        }

    }

    /**
     * 文档共享通知
     */
    private void confMsgNotifyDs(ConfMsg msg, ConfExtendMsg extendMsg)
    {
        int msgType = msg.getMsgType();
        int nValue1 = msg.getnValue1();
        int nValue2 = (int) msg.getnValue2();
        switch (msgType)
        {
            case ConfMsg.COMPT_MSG_DS_ON_DOC_NEW:
                if (dscurrentDocCount == 0)
                // 新建一个文档共享时，当前共享文档数量为零，即文档共享开始
                {
                    BroadMsg broadMsg = new BroadMsg(
                            NotifyMessage.CALL_MSG_USER_RECEIVE_SHARED_DATA);

                    RequestCode requestCode = new RequestCode();
                    requestCode.setRetCode(String
                            .valueOf(ConfDefines.IID_COMPONENT_DS));
                    broadMsg.setRequestCode(requestCode);


                    RequestInfo requestInfo = new RequestInfo();
                    requestInfo.setMsg(String.valueOf(CONF_SHARED_START));
                    broadMsg.setRequestInfo(requestInfo);
                    CCAPP.getInstances().sendBroadcast(broadMsg);
                }
                updateDocumsg = new Message();
                updateDocumsg.what = UPDATE_DOCUVIEW;
                videoHandler.sendMessage(updateDocumsg);
                break;
            case ConfMsg.COMPT_MSG_DS_PAGE_DATA_DOWNLOAD: // 文档页面数据已经下载通知
                if ((nValue1 == dscurrentDocID)
                        && ((int) nValue2 == dscurrentPageID))
                {
                    updateDocumsg = new Message();
                    updateDocumsg.what = UPDATE_DOCUVIEW;
                    videoHandler.sendMessage(updateDocumsg);
                }
                break;
            case ConfMsg.COMPT_MSG_DS_ON_CURRENT_PAGE_IND: // 同步翻页预先通知
                if (nValue1 != 0) // nValue1：文档ID
                {
                    dscurrentDocID = nValue1;
                    dscurrentPageID = (int) nValue2;
                    dsSetcurrentpage(nValue1, (int) nValue2);
                }
                updateDocumsg = new Message();
                updateDocumsg.what = UPDATE_DOCUVIEW;
                videoHandler.sendMessage(updateDocumsg);
                break;
            case ConfMsg.COMPT_MSG_DS_ANDROID_DOC_COUNT:
                dscurrentDocCount = nValue1;
                if (nValue1 == 0) // 共享文档数量为零，即文档共享停止
                {
                    Message releaseDocumsg = new Message();
                    releaseDocumsg.what = RELEASE_DOCUVIEW;
                    videoHandler.sendMessage(releaseDocumsg);

                    BroadMsg broadMsg = new BroadMsg(
                            NotifyMessage.CALL_MSG_USER_RECEIVE_SHARED_DATA);

                    RequestCode requestCode = new RequestCode();
                    requestCode.setRetCode(String
                            .valueOf(ConfDefines.IID_COMPONENT_DS));
                    broadMsg.setRequestCode(requestCode);


                    RequestInfo requestInfo = new RequestInfo();
                    requestInfo.setMsg(String.valueOf(CONF_SHARED_STOP));
                    broadMsg.setRequestInfo(requestInfo);
                    CCAPP.getInstances().sendBroadcast(broadMsg);
                }
                break;
            default:
                if (msgType == ConfMsg.COMPT_MSG_DS_ON_DOC_DEL || msgType == ConfMsg.COMPT_MSG_DS_ON_PAGE_NEW
                        || msgType == ConfMsg.COMPT_MSG_DS_ON_CURRENT_PAGE
                        || msgType == ConfMsg.COMPT_MSG_DS_ON_DRAW_DATA_NOTIFY)
                {
                    updateDocumsg = new Message();
                    updateDocumsg.what = UPDATE_DOCUVIEW;
                    videoHandler.sendMessage(updateDocumsg);
                    if (msgType == ConfMsg.COMPT_MSG_DS_ON_DRAW_DATA_NOTIFY)
                    {
                        updateDeskmsg = new Message();
                        updateDeskmsg.what = UPDATE_DESKVIEW;
                        videoHandler.sendMessage(updateDeskmsg);
                    }
                }
                break;
        }
    }

    /**
     * 刷新共享屏幕页面
     */
    private void updateDesktopSharedView()
    {
        if (desktopSurfaceView != null)
        {
            desktopSurfaceView.update();
        }
    }

    /**
     * 刷新共享文档页面
     */
    private void updateDocSharedView()
    {
        if (docSurfaceView != null)
        {
            docSurfaceView.update();
        }
    }

    /**
     * 判断是否是主线程
     *
     * @return boolean
     */
    private boolean isMainThread()
    {
        return Thread.currentThread().getId() == mMainThreadID;
    }

    /**
     * 协商回调消息解析
     *
     * @param sourceMsgObj
     * @return JSONObject
     * @throws JSONException
     */

    private JSONObject createSendMsg(JSONObject sourceMsgObj)
            throws JSONException
    {

        JSONObject msgToSend = new JSONObject();
        String baudRate = sourceMsgObj.getString("baudRate");
        String callId = sourceMsgObj.getString("callId");
        int callmode = sourceMsgObj.getInt("callmode");
        String calltype = sourceMsgObj.getString("calltype");
        String videoMode = sourceMsgObj.getString("video_mode");
        String addrCallMode = sourceMsgObj.getString("addr_call_mode");
        JSONArray addressObjToSend = new JSONArray();
        JSONObject addressItemPhone = new JSONObject();
        addressItemPhone.put("ResType", "phone");
        addressItemPhone.put("resId", serviceConfig.getUserId());

        JSONObject addressItemAgent = new JSONObject();
        addressItemAgent.put("ResType", "agent");
        addressItemAgent.put("resId", "");

        JSONObject addressItemCollaboration = new JSONObject();
        addressItemCollaboration.put("ResType", "collaboration");
        addressItemCollaboration.put("resId", serviceConfig
                .getUserId());

        JSONObject addressItemMessage = new JSONObject();
        addressItemMessage.put("ResType", "message");
        addressItemMessage.put("resId", serviceConfig.getUserId());

        JSONObject addressItemFile = new JSONObject();
        addressItemFile.put("ResType", "file");
        addressItemFile.put("resId", serviceConfig.getUserId());

        JSONObject addressItemVideo = new JSONObject();
        addressItemVideo.put("ResType", "video");
        addressItemVideo.put("resId", serviceConfig.getUserId());

        JSONObject addressItemWorkNo = new JSONObject();
        addressItemWorkNo.put("ResType", "workNo");
        addressItemWorkNo.put("resId", "anonymousCard");
        addressObjToSend.put(0, addressItemPhone);
        addressObjToSend.put(1, addressItemAgent);
        addressObjToSend.put(2, addressItemCollaboration);
        addressObjToSend.put(3, addressItemMessage);
        addressObjToSend.put(4, addressItemFile);
        addressObjToSend.put(5, addressItemVideo);
        addressObjToSend.put(6, addressItemWorkNo);

        msgToSend.put("Address", addressObjToSend);
        msgToSend.put("baudRate", baudRate);
        msgToSend.put("callId", callId);
        msgToSend.put("callmode", serviceConfig.getAnonyCallMode());
        msgToSend.put("calltype", calltype);
        msgToSend.put("video_mode", videoMode);
        msgToSend.put("addr_call_mode", addrCallMode);
        msgToSend
                .put("video_session_id", serviceConfig.getConfId());
        return msgToSend;
    }

    /**
     * Handle 消息处理
     *
     * @param msg msg
     */
    private void handleMsg(Message msg)
    {
        switch (msg.what)
        {
            case CONF_NEW_VAL:
                confNew((com.huawei.meeting.ConfInfo) msg.obj);
                break;
            case CONF_FLUSH:
                flush();
                break;
            case CONF_RELEASE_VAL:
                exitConf();
                break;
            case CONF_HEARTBEAT:
                heartBeat();
                break;
            case CONF_MUTE:
                muteConf((Boolean) msg.obj);
                break;
            case ConfOper.CONF_OPER_JOIN:
                joinConf();
                break;
            case ConfOper.CONF_OPER_LEAVE:
                leaveConf();
                break;
            case ConfOper.CONF_OPER_TERMINATE:
                terminateConf();
                break;
            case ConfOper.CONF_OPER_LOCK:
                lockConf();
                break;
            case ConfOper.CONF_OPER_UNLOCK:
                unLockConf();
                break;
            case ConfOper.CONF_OPER_KICKOUT:
            {
                int nUserID = msg.arg1;
                kickout(nUserID);
            }
            break;
            case ConfOper.VIDEO_OPER_SETENCODE_MAXRESOLUTION:
            {
                int xResolution = msg.arg1;
                int yResolution = msg.arg2;
                setEncodeMaxResolution(xResolution, yResolution);
            }
            break;
            case ConfOper.CONF_OPER_SET_ROLE:
            {
                int nUserID = msg.arg1;
                int nRole = msg.arg2;
                setRole(nUserID, nRole);
            }
            break;
            case ConfOper.CONF_OPER_REQUEST_ROLE:
                break;
            case ConfOper.CONF_OPER_SEND_DATA:
                sendData(msg.arg1, msg.getData().getInt("msgID"),
                        msg.getData().getByteArray("optContext"));
                break;
            case ConfOper.CONF_OPER_LOAD_COMPONENT:
                loadComponent();
                break;
            case ConfOper.VIDEO_OPER_OPEN:
                videoOpen(msg.arg1, (String) msg.obj);
                break;
            case ConfOper.VIDEO_OPER_CLOSE:
                videoClose(msg.arg1, (String) msg.obj);
                break;
            case ConfOper.VIDEO_OPER_PAUSE:
                videoPause(msg.arg1, (String) msg.obj);
                break;
            case ConfOper.VIDEO_OPER_RESUME:
                videoResume(msg.arg1, (String) msg.obj);
                break;
            case ConfOper.VIDEO_OPER_ATTACH:
                videoAttach(msg.arg1, msg.arg2, (String) msg.obj);
                break;
            case ConfOper.VIDEO_OPER_DETACH:
                videoDetach(msg.arg1, msg.arg2, (String) msg.obj);
                break;
            case ConfOper.VIDEO_OPER_SET_CAPTURE_ROTATE:
                videoSetCaptureRotate(msg.arg1, (String) msg.obj, msg.arg2);
                break;
            case ConfOper.VIDEO_OPER_SETPARAM:
                videoSetParam(userId, deviceId);
                logInfo(TAG, "",  " not correct thread --> VIDEO_OPER_SETPARAM");
                break;
            case ConfOper.VIDEO_OPER_GETDEVICECAPBILITY_INFO:
                videGetDevicecApbilityNum(msg.arg1, (String) msg.obj);
                break;
            case ConfOper.VIDEO_OPER_GETDEVICECAPBILITY_NUM:
                videoGetDevicecApbilityInfo(msg.arg1, (String) msg.obj);
                break;
            case ConfOper.VIDEO_OPER_GETPARAM:
                videoGetParam(msg.arg1, (String) msg.obj);
                break;
            case ConfOper.VIDEO_OPER_GETDEVICE_NUM:
                getVideoDeviceNum();
                break;
            case ConfOper.VIDEO_OPER_GETDEVICE_INFO:
                getVideoDeviceInfo();
                break;
            case ConfOper.VIDEO_OPER_NOTIFY :
            {
                videoNotifyOpen(msg.arg2, (String) msg.obj, msg.arg1);
            }
            break;
            case ConfOper.AUDIO_OPER_SET_AUDIOPARAM:
                setAudioParam();
                break;
            case ConfOper.AUDIO_OPER_OPEN_MIC:
                openMic(0);
                break;
            case ConfOper.AUDIO_OPER_CLOSE_MIC:
                closeMic();
                break;
            case ConfOper.AUDIO_OPER_MUTE_MIC:
            {
                muteMic(msg.arg1);
            }
            break;
            case ConfOper.AUDIO_OPER_OPEN_SPEAKER:
            {
                int speakerID = msg.arg1;
                openSpeaker(speakerID);
            }
            break;
            case ConfOper.AUDIO_OPER_CLOSE_SPEAKER:
                closeSpeaker();
                break;
            case ConfOper.AUDIO_OPER_MUTE_SPEAKER:
            {
                muteSpeaker(msg.arg1);
            }
            break;
            case ConfOper.DS_OPER_SET_CURRENTPAGE:
                dsSetcurrentpage(msg.arg1, msg.arg2);
                break;
            case ConfOper.CHAT_OPER_SEND:
            {
                String str = msg.obj.toString();
                chatSendMsg(msg.arg1, str);
            }
            break;
            case ConfOper.AS_OPER_SET_PARAM:
            {
                int value = msg.arg1;
                asSetParam(value);
            }
            break;
            case VIDEO_GETPARAM:
                getVideoStream();
                break;
            default:
                break;
        }
    }

    /**
     * 添加视频设备参数
     *
     * @param key   视频设备ID
     * @param param 视频参数
     */
    private void addVideoParamsMap(String key, VideoParams param)
    {
        List<VideoParams> params = videoParamsMap.get(key);
        if (null == params || params.size() == 0)
        {
            params = new ArrayList<VideoParams>();
        }
        params.add(param);
        videoParamsMap.put(key, params);
    }

    /**
     * 清除视频设备能力参数缓存
     */
    private void clearVideoParamsMap()
    {
        if (null != videoParamsMap)
        {
            videoParamsMap.clear();
        }
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
     * 该类主要是确保操作是在主线程中
     */
    private class WorkThread extends Thread
    {
        private Handler handler;

        public void setHandler(Handler handler)
        {
            this.handler = handler;
        }

        public void run()
        {
            Looper.prepare();
            handler = new Handler()
            {
                public void handleMessage(Message msg)
                {
                    handleMsg(msg);
                }
            };
            confThreadStartSemaphore.release();

            Looper.loop();
        }
        public Handler getHandler()
        {
            return handler;
        }
    }


}
