package com.huawei.esdk.cc.video;

import android.content.Context;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.os.Handler;
import android.view.SurfaceView;

import com.huawei.esdk.cc.MobileCC;
import com.huawei.esdk.cc.service.ServiceConfig;
import com.huawei.esdk.cc.service.CCAPP;
import com.huawei.esdk.cc.service.call.CallManager;
import com.huawei.esdk.cc.utils.LogUtil;
import com.huawei.videoengine.ViERenderer;

/**
 * VideoControl
 */
public final class VideoControl
{
    /**
     * 后置摄像头
     */
    public static final int BACK_CAMERA = 0;
    /**
     * 前置摄像头
     */
    public static final int FRONT_CAMERA = 1;
    /**
     * 1：竖屏
     **/
    public static final int PORTRAIT = 1;
    private static VideoControl ins;
    /**
     * 摄像头数量
     */
    private static int numberOfCameras = Camera.getNumberOfCameras();
    /**
     * 本地隐藏视频视图，用一个像素布局
     */
    private SurfaceView localVideo;

    /**
     * 远端视频视图
     */
    private SurfaceView remoteVideo;
    /**
     * 协商后的视频方向
     */
    private VideoConfig videoConfig;
    private ServiceConfig serviceConfig = ServiceConfig.getInstance();

    private VideoControl()
    {
        videoConfig = new VideoConfig();
        configVideoCaps();
    }

    /**
     * getIns
     *
     * @return VideoControl
     */
    public static VideoControl getIns()
    {
        if (ins == null)
        {
            ins = new VideoControl();
        }
        return ins;
    }

    /**
     * 切换前后摄像头时,角度也会发生变化.
     */
    public void switchCamera()
    {
        int cameraIndex = videoConfig.getCameraIndex();
        int newIndex = (cameraIndex + 1) % 2;
        LogUtil.d("VideoControl", "cameraIndex" + cameraIndex + " newIndex" + newIndex);
        serviceConfig.setCameraIndex(newIndex);
        videoConfig.setCameraIndex(newIndex);
        // 切换摄像头需要重写下方，fast目前没有处理
        videoConfig.setOrientPortrait(getVideoChangeOrientation(0, isFrontCamera()) / 90);
        videoConfig.setOrientLandscape(getVideoChangeOrientation(90, isFrontCamera()) / 90);
        videoConfig.setOrientSeascape(getVideoChangeOrientation(270, isFrontCamera()) / 90);

        deployGlobalVideoCaps();

    }

    /**
     * setCallId
     *
     * @param callId callId
     */
    public void setCallId(String callId)
    {
        videoConfig.setSessionId(callId);
    }

    private void configVideoCaps()
    {

        // 设置默认采用的摄像头
        setDefaultCamera();

        // 设置为竖屏
        videoConfig.setOrient(2);

        videoConfig.setOrientPortrait(getVideoChangeOrientation(0, isFrontCamera()) / 90);
        videoConfig.setOrientLandscape(getVideoChangeOrientation(90, isFrontCamera()) / 90);
        videoConfig.setOrientSeascape(getVideoChangeOrientation(270, isFrontCamera()) / 90);
    }

    /**
     * 根据Activity取回的界面旋转度数来计算需要转动的度数.
     *
     * @param degree
     * @return
     */
    private int getVideoChangeOrientation(int degree, boolean isfront)
    {
        int resultDegree = 0;
        if (isfront)
        {
            // 注意: 魅族手机与寻常手机不一致.需要分别做判断.正常手机采用下面方法即可
            if (degree == 0)
            {
                resultDegree = 270;
            }
            else if (degree == 90)
            {
                resultDegree = 0;
            }
            else if (degree == 270)
            {
                resultDegree = 180;
            }

        }
        else
        {
            resultDegree = (90 - degree + 360) % 360;
        }
        return resultDegree;
    }

    /**
     * 如果有前置摄像头,就设置前置摄像头;否则设置后置摄像头.
     */
    private void setDefaultCamera()
    {
        if (canSwitchCamera())
        {
            videoConfig.setCameraIndex(FRONT_CAMERA);
        }
        else
        {
            videoConfig.setCameraIndex(BACK_CAMERA);
        }
    }

    private boolean isFrontCamera()
    {
        return videoConfig.getCameraIndex() == FRONT_CAMERA;
    }

    /**
     * 是否能转化摄像头
     *
     * @return boolean
     */
    public boolean canSwitchCamera()
    {
        return numberOfCameras > 1;
    }

    /**
     * deploySessionVideoCaps
     */
    public void deploySessionVideoCaps()
    {
        Context context = CCAPP.getInstances().getApplication();
        setDefaultCamera();
        if (localVideo == null)
        {
            localVideo = ViERenderer.createLocalRenderer(context);
            localVideo.setZOrderMediaOverlay(true);
        }
        videoConfig.setPlaybackLocal(ViERenderer.getIndexOfSurface(localVideo));
        if (remoteVideo == null)
        {
            remoteVideo = ViERenderer.createRenderer(context, false);
            remoteVideo.setZOrderOnTop(false);
        }
        videoConfig.setPlaybackRemote(ViERenderer.getIndexOfSurface(remoteVideo));

        CallManager tempCallMan = CallManager.getInstance();
        String sessionId = videoConfig.getSessionId();
        if (tempCallMan != null)
        {
            tempCallMan.setVideoIndex(serviceConfig.getCameraIndex());

            // 新的sdk里面createVideoWindow及updateVideoWindow方法都变成私有的，
            // 由videoWindowAction来替代了。
            tempCallMan.videoWindowAction(0, videoConfig.getPlaybackRemote(), sessionId);
        }

        configVideoCaps();
        deployGlobalVideoCaps();


    }

    /**
     * deployGlobalVideoCaps
     */
    public void deployGlobalVideoCaps()
    {
        CallManager manager = CallManager.getInstance();
        if (manager != null)
        {
            manager.setOrientParams(videoConfig);
            Configuration configuration = CCAPP.getInstances().getApplication().getResources().getConfiguration();
            if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                    && serviceConfig.getCameraIndex() == 0)
            {
                new Handler().postDelayed(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        LogUtil.d("VideoControl", " 90");
                        CallManager.getInstance().setRotation(90);
                    }
                }, 600);
            }
            else
            {

            }
        }
    }

    /**
     * 初始化SurfaceView (localView)
     *
     * @return SurfaceView
     */
    public SurfaceView getLocalVideoView()
    {
        if (localVideo != null)
        {
            Configuration configuration = CCAPP.getInstances().getApplication().getResources().getConfiguration();
            if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT
                    && serviceConfig.getCameraIndex() == 1)
            {
                MobileCC.getInstance().setVideoRotate(270);
            }
            return localVideo;
        }
        return null;
    }

    /**
     * 初始化SurfaceView (remoteView)
     *
     * @return SurfaceView
     */
    public SurfaceView getRemoteVideoView()
    {
        if (remoteVideo != null)
        {
            return remoteVideo;
        }
        return null;
    }

    /**
     * 设置摄像头默认状态，释放占用资源
     */
    public void clearSurfaceView()
    {
        setDefaultCamera();
        videoConfig.setPlaybackLocal(-1);
        videoConfig.setPlaybackRemote(-1);
        videoConfig.setSessionId(null);

        // 摄像头前后切换后，fast保留上次的值需要进行复位操作(3,0,2),默认为竖屏，
        // 协商后会调整为横屏
        // fast目前没有做切换摄像头下发处理，因此需要UI 处理规避切换摄像头的部分问题
        videoConfig.setOrientPortrait(getVideoChangeOrientation(0, isFrontCamera()) / 90);
        videoConfig.setOrientLandscape(getVideoChangeOrientation(90, isFrontCamera()) / 90);
        videoConfig.setOrientSeascape(getVideoChangeOrientation(270, isFrontCamera()) / 90);

        // 释放HME资源
        if (localVideo != null)
        {
            ViERenderer.setSurfaceNull(localVideo);
            localVideo = null;
        }
        if (remoteVideo != null)
        {
            ViERenderer.setSurfaceNull(remoteVideo);
            remoteVideo = null;
        }
    }
}
