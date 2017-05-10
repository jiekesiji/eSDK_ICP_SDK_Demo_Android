package com.huawei.esdk.icsdemo.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.huawei.esdk.cc.MobileCC;
import com.huawei.esdk.cc.common.BroadMsg;
import com.huawei.esdk.cc.common.NotifyMessage;
import com.huawei.esdk.cc.utils.LogUtil;
import com.huawei.esdk.icsdemo.R;

import java.util.Calendar;

/**
 * Created on 2016/3/4.
 */
public class ConferenceActivity extends Activity implements View.OnClickListener
{

    private Button leaveConfBtn;
    private ImageButton dataBtn;
    private boolean isAppShare = false;
    private boolean isClicked = false;

    private FrameLayout mLlRemoteSurface;
    private FrameLayout mLlLocalSurface;

    private Button btnSwitchCamera;
    private Button getVideoStream;
    private Button btnSetMode;
    private Button btnDegree;
    private int degree = 1;
    private int videoMode = MobileCC.VIDEOMODE_QUALITY;
    private final String SHARE_START = "1";
    private final String SHARE_COMPONENT = "2";
    public static final int MIN_CLICK_DELAY_TIME = 5000;
    private long lastClickTime = 0;
    private LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);


    private String TAG = ConferenceActivity.class.getSimpleName();
    private BroadcastReceiver receiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            LogUtil.d(TAG, "action=" + action);
            if (NotifyMessage.CALL_MSG_USER_JOIN.equals(action))
            {
                MobileCC.getInstance().setVideoContainer(ConferenceActivity.this, mLlLocalSurface, mLlRemoteSurface);
                LogUtil.d(TAG, "join conf.");
            }
            else if (NotifyMessage.CALL_MSG_USER_NETWORK_ERROR.equals(action))
            {
                LogUtil.d(TAG, "network error.");
            }
            else if (NotifyMessage.CALL_MSG_USER_END.equals(action))
            {
                LogUtil.d(TAG, "==============meeting end=============");
                ConferenceActivity.this.finish();
            }

            else if (NotifyMessage.CALL_MSG_GET_VIDEO_INFO.equals(action))
            {
                BroadMsg broadMsg = (BroadMsg) intent
                        .getSerializableExtra(NotifyMessage.CC_MSG_CONTENT);
                String encodeSize = broadMsg.getRequestInfo().getStreamInfo().getEncoderSize();
                int framRate = broadMsg.getRequestInfo().getStreamInfo().getSendFrameRate();
                int bitRate = broadMsg.getRequestInfo().getStreamInfo().getVideoSendBitRate();
                Toast.makeText(ConferenceActivity.this, getString(R.string.send_framsize) + encodeSize + getString(R.string.frame_rate) + framRate + getString(R.string.bit_rate) + bitRate, Toast.LENGTH_SHORT).show();
            }

            else if (NotifyMessage.CALL_MSG_USER_RECEIVE_SHARED_DATA.equals(action))
            {
                BroadMsg broadMsg = (BroadMsg) intent
                        .getSerializableExtra(NotifyMessage.CC_MSG_CONTENT);
                String recode = broadMsg.getRequestCode().getRetCode();
                String msg = broadMsg.getRequestInfo().getMsg();

                String sharedType;
                String sharedState;
                if (SHARE_COMPONENT.equals(recode))
                {
                    sharedType = "Application sharing";
                }
                else
                {
                    sharedType = "";
                }
                isClicked = false;

                if (SHARE_START.equals(msg))
                {
                    sharedState = "begin !";
                    if (SHARE_COMPONENT.equals(recode))
                    {
                        isAppShare = true; // 接收共享数据，设置显示容器
                    }
                }
                else
                {
                    sharedState = "end !";
                    if (SHARE_COMPONENT.equals(recode))
                    {
                        isAppShare = false; // 接收共享数据，设置显示容器
                    }
                }
                if (isAppShare)
                {
                    dataBtn.setImageResource(R.drawable.icon_share_select);
                }
                else
                {
                    dataBtn.setImageResource(R.drawable.icon_data_select);
                }
                LogUtil.d(TAG, sharedType + " - " + sharedState);

            }

            else if (NotifyMessage.CALL_MSG_ON_STOP_MEETING.equals(action))
            {
                Toast.makeText(ConferenceActivity.this, getString(R.string.exit_meeting), Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conference);
        IntentFilter filter = new IntentFilter();
        filter.addAction(NotifyMessage.CALL_MSG_USER_JOIN);
        filter.addAction(NotifyMessage.CALL_MSG_USER_NETWORK_ERROR);
        filter.addAction(NotifyMessage.CALL_MSG_USER_END);
        filter.addAction(NotifyMessage.CALL_MSG_USER_RECEIVE_SHARED_DATA);
        filter.addAction(NotifyMessage.CALL_MSG_ON_STOP_MEETING);
        filter.addAction(NotifyMessage.CALL_MSG_GET_VIDEO_INFO);
        localBroadcastManager.registerReceiver(receiver, filter);
        initComp();
    }

    private void initComp()
    {
        mLlRemoteSurface = (FrameLayout) findViewById(R.id.view_remote); // RemoteView
        mLlLocalSurface = (FrameLayout) findViewById(R.id.view_local); // LocalView

        leaveConfBtn = (Button) findViewById(R.id.leaveConf);
        dataBtn = (ImageButton) findViewById(R.id.data_share);

        getVideoStream = (Button) findViewById(R.id.get_video_stream);
        btnSwitchCamera = (Button) findViewById(R.id.swich_camera);
        btnSetMode = (Button) findViewById(R.id.btn_set_mode);
        btnDegree = (Button) findViewById(R.id.btn_set_degree);

        btnDegree.setOnClickListener(this);
        btnSetMode.setOnClickListener(this);
        getVideoStream.setOnClickListener(this);
        btnSwitchCamera.setOnClickListener(this);
        dataBtn.setOnClickListener(this);
        leaveConfBtn.setOnClickListener(this);



    }


    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.data_share:
            {
                LogUtil.d(TAG, "isAppShare:" + isAppShare + ",isClicked:" + isClicked);
                if (isAppShare && !isClicked)
                {
                    LogUtil.d(TAG, "isAppShare:" + isAppShare + ",isClicked:" + isClicked);

                    Intent intent = new Intent(ConferenceActivity.this,
                            ConfShareActivity.class);
                    startActivity(intent);
                }
            }
            break;
            case R.id.leaveConf:
                MobileCC.getInstance().releaseCall(); // 结束会议
                break;

            case R.id.btn_set_degree:
                //角度是0，90，180, 270
                MobileCC.getInstance().setVideoRotate((degree % 4) * 90);
                degree++;
                break;

            case R.id.swich_camera:
                long currentTime = Calendar.getInstance().getTimeInMillis();
                if (currentTime - lastClickTime > MIN_CLICK_DELAY_TIME)
                {
                    MobileCC.getInstance().switchCamera();
                    lastClickTime = currentTime;
                }
                break;

            case R.id.get_video_stream:
                MobileCC.getInstance().getChannelInfo();
                break;

            case R.id.btn_set_mode:
                setVideoMode();
                break;

            default:
                break;

        }

    }

    @Override
    protected void onDestroy()
    {
        localBroadcastManager.unregisterReceiver(receiver);
        super.onDestroy();
    }

    private void setVideoMode()
    {

        //设置videoMode
        if (0 == MobileCC.getInstance().setVideoMode(videoMode))
        {
            Toast.makeText(ConferenceActivity.this, (MobileCC.VIDEOMODE_QUALITY == videoMode ? getString(R.string.image_quality_priority) : getString(R.string.fluency_priority)), Toast.LENGTH_SHORT)
                    .show();
        }

        // videoMode取值0或者1
        if (videoMode == MobileCC.VIDEOMODE_FLUENT)
        {
            videoMode = MobileCC.VIDEOMODE_QUALITY;
        }
        else
        {
            videoMode = MobileCC.VIDEOMODE_FLUENT;
        }
    }

    @Override
    public void onBackPressed()
    {
    }

    @Override
    protected void onResume()
    {
        super.onResume();

    }

}
