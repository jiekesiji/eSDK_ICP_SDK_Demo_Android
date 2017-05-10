package com.huawei.esdk.icsdemo.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.esdk.cc.MobileCC;
import com.huawei.esdk.cc.common.BroadMsg;
import com.huawei.esdk.cc.common.NotifyMessage;
import com.huawei.esdk.cc.utils.LogUtil;
import com.huawei.esdk.cc.video.StreamInfo;
import com.huawei.esdk.icsdemo.R;

public class ChatActivity extends Activity implements View.OnClickListener
{

    private static final String TAG = "ChatActivity";
    private final int QUEUEING = 1;
    private final int QUEUEOVER = 2;
    private IntentFilter filter;
    private FrameLayout remoteView;
    private FrameLayout localView;
    private Button btnStreamInfo;
    private Button btnMicMute;
    private Button btnCloseVideo;
    private Button btnSwitchCamera;
    private Button btnBack;
    private Button btnCancelQueue;
    private Button btnQueueInfo;
    private Button btnRotate;
    private int degree = 1;
    private Button btnSpeakerMute;
    private RelativeLayout queueLayout;
    private RelativeLayout btnLayout;
    private TextView tvQueue;
    private TextView tvNetStatus;

    /**
     * 视频发送分辨率
     */
    private String videoSendFramsize = "";

    /**
     * 视频发送帧率
     */
    private String videoSendFrameRate = "";

    /**
     * 视频发送码率
     */
    private StringBuffer videoSendDatarate = new StringBuffer(0);

    /**
     * 视频丢包率
     */
    private String videoSendPacketLossProbability;
    private String videoRecvPacketLossProbability;
    /**
     * 视频延时
     */
    private String videoSendDelay;
    private String videoRecvDelay;
    /**
     * 视频抖动
     */
    private String videoSendJitter;
    private String videoRecvJitter;
    /**
     * 视频接收帧率
     */
    private String videoRecvFrameRate = "";

    /**
     * 视频接收分辨率
     */
    private String videoRecvFramsize = "";

    /**
     * 视频接收码率
     */
    private StringBuffer videoRecvDatarate = new StringBuffer(0);

    private int micMute = 0;//1静音0恢复

    private int speakerMute = 0;//1静音0恢复

    private int speaker = 0;//0是扬声器 1 是听筒

    private Button btnSwitchAudio;
    private LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);

    private Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case QUEUEING:
                    tvQueue.setText(getString(R.string.queuing));
                    queueLayout.setVisibility(View.VISIBLE);
                    break;

                case QUEUEOVER:
                    queueLayout.setVisibility(View.GONE);
                    btnLayout.setVisibility(View.VISIBLE);
                    break;

                default:
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        // 保持屏幕常亮
        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        + WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        + WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        setContentView(R.layout.activity_chat);
        initView();
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        localBroadcastManager.registerReceiver(receiver, filter);
        MobileCC.getInstance().videoOperate(MobileCC.START);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        localBroadcastManager.unregisterReceiver(receiver);
        MobileCC.getInstance().videoOperate(MobileCC.STOP);
    }

    private void initView()
    {
        //本地、远端画面
        localView = (FrameLayout) findViewById(R.id.view_local);
        remoteView = (FrameLayout) findViewById(R.id.view_remote);

        btnStreamInfo = (Button) findViewById(R.id.btn_video_info);
        btnMicMute = (Button) findViewById(R.id.btn_mic_mute);
        tvQueue = (TextView) findViewById(R.id.tv_queue);
        queueLayout = (RelativeLayout) findViewById(R.id.view_queue_layout);
        btnCancelQueue = (Button) findViewById(R.id.btn_cancle_queue);
        btnCloseVideo = (Button) findViewById(R.id.btn_close_video);
        btnSwitchCamera = (Button) findViewById(R.id.btn_switch_camera);
        btnSwitchAudio = (Button) findViewById(R.id.btn_switch_audio);
        btnLayout = (RelativeLayout) findViewById(R.id.view_btn_layout);
        btnBack = (Button) findViewById(R.id.btn_back);
        btnBack.setVisibility(View.GONE);
        tvNetStatus = (TextView) findViewById(R.id.tv_net_status);
        btnSpeakerMute = (Button) findViewById(R.id.btn_speaker_mute);
        btnRotate = (Button) findViewById(R.id.btn_rotate);
        btnQueueInfo = (Button)findViewById(R.id.btn_queue_info);

        btnRotate.setOnClickListener(this);
        btnSpeakerMute.setOnClickListener(this);
        btnBack.setOnClickListener(this);
        btnSwitchAudio.setOnClickListener(this);
        btnSwitchCamera.setOnClickListener(this);
        btnCloseVideo.setOnClickListener(this);
        btnCancelQueue.setOnClickListener(this);
        btnStreamInfo.setOnClickListener(this);
        btnMicMute.setOnClickListener(this);
        btnQueueInfo.setOnClickListener(this);


        filter = new IntentFilter();
        filter.addAction(NotifyMessage.CALL_MSG_ON_QUEUE_INFO);
        filter.addAction(NotifyMessage.CALL_MSG_REFRESH_LOCALVIEW);
        filter.addAction(NotifyMessage.CALL_MSG_REFRESH_REMOTEVIEW);
        filter.addAction(NotifyMessage.CALL_MSG_ON_CONNECTED);
        filter.addAction(NotifyMessage.CALL_MSG_ON_DISCONNECTED);
        filter.addAction(NotifyMessage.CALL_MSG_ON_DROPCALL);
        filter.addAction(NotifyMessage.CALL_MSG_ON_FAIL);
        filter.addAction(NotifyMessage.CALL_MSG_ON_NET_QUALITY_LEVEL);
        filter.addAction(NotifyMessage.CALL_MSG_ON_QUEUING);
        filter.addAction(NotifyMessage.CALL_MSG_ON_QUEUE_TIMEOUT);
        filter.addAction(NotifyMessage.CALL_MSG_ON_CANCEL_QUEUE);
        filter.addAction(NotifyMessage.CALL_MSG_ON_CONNECT);
        filter.addAction(NotifyMessage.CALL_MSG_ON_POLL);
        filter.addAction(NotifyMessage.CALL_MSG_GET_VIDEO_INFO);

        Intent intent = getIntent();
        if (intent != null)
        {
            String accessCode = intent.getStringExtra("AccessCode");
            String callData = intent.getStringExtra("CallData");
            String verifyCode = intent.getStringExtra("VerifyCode");

            if (0 != MobileCC.getInstance().makeCall(accessCode, MobileCC.SERVER_TP + "", callData, verifyCode))
            {
                btnBack.setVisibility(View.VISIBLE);
                Toast.makeText(ChatActivity.this, getString(R.string.make_call_error), Toast.LENGTH_SHORT).show();
            }
        }
        else
        {
            Toast.makeText(ChatActivity.this, getString(R.string.intent_error), Toast.LENGTH_SHORT).show();
        }

    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.btn_video_info:
                MobileCC.getInstance().getChannelInfo();
                break;

            case R.id.btn_back:
                ChatActivity.this.finish();
                break;

            case R.id.btn_switch_audio:
                switchAudio();
                break;

            case R.id.btn_cancle_queue:
                MobileCC.getInstance().cancelQueue();
                break;

            case R.id.btn_close_video:
                MobileCC.getInstance().releaseCall();
                break;

            case R.id.btn_switch_camera:
                MobileCC.getInstance().switchCamera();
                break;

            case R.id.btn_mic_mute:
                micMute();
                break;
            case R.id.btn_speaker_mute:
                speakerMute();
                break;
            case R.id.btn_rotate:
                MobileCC.getInstance().setVideoRotate((degree % 4) * 90);
                degree++;
                break;

            case R.id.btn_queue_info:
                MobileCC.getInstance().getCallQueueInfo();
                break;

            default:
                break;
        }
    }

    private BroadcastReceiver receiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            BroadMsg broadMsg = (BroadMsg) intent
                    .getSerializableExtra(NotifyMessage.CC_MSG_CONTENT);

            if (broadMsg != null)
            {

                if (NotifyMessage.CALL_MSG_REFRESH_LOCALVIEW.equals(action))
                {
                    MobileCC.getInstance().setVideoContainer(ChatActivity.this, localView, null);
                    btnCloseVideo.setVisibility(View.VISIBLE);
                }
                else if (NotifyMessage.CALL_MSG_REFRESH_REMOTEVIEW.equals(action))
                {

                    MobileCC.getInstance().setVideoContainer(ChatActivity.this, null, remoteView);
                    btnStreamInfo.setVisibility(View.VISIBLE);
                }
                else if (NotifyMessage.CALL_MSG_ON_QUEUE_INFO.equals(action))
                {
                    if (null == broadMsg.getRequestCode().getRetCode())
                    {
                        Toast.makeText(ChatActivity.this, getString(R.string.get_queue_info_error) + broadMsg.getRequestCode().getErrorCode(), Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        String retcode = broadMsg.getRequestCode().getRetCode();

                        if ("0".equals(retcode))
                        {
                            //排队状态
                            long position = broadMsg.getQueueInfo().getPosition();
                            int onlineAgentNum = broadMsg.getQueueInfo().getOnlineAgentNum();
                            long longestWaitTime =broadMsg.getQueueInfo().getLongestWaitTime();

                            //提示
                            Toast.makeText(ChatActivity.this, getString(R.string.position) + position + getString(R.string.online_agent_num) + onlineAgentNum + getString(R.string.longest_wait_time) + longestWaitTime, Toast.LENGTH_SHORT).show();
                        }
                        else
                        {
                            //非排队状态
                            Toast.makeText(ChatActivity.this, getString(R.string.not_queue_status_error) + retcode, Toast.LENGTH_SHORT).show();
                            queueLayout.setVisibility(View.GONE);
                        }
                    }
                }
                else if (NotifyMessage.CALL_MSG_ON_FAIL.equals(action))
                {
                    btnBack.setVisibility(View.VISIBLE);
                    Toast.makeText(ChatActivity.this, getString(R.string.call_fail_return), Toast.LENGTH_SHORT).show();
                }

                else if (NotifyMessage.CALL_MSG_ON_CONNECTED.equals(action))
                {
                    Message message = new Message();
                    message.what = QUEUEOVER;
                    handler.sendMessage(message);
                }
                else if (NotifyMessage.CALL_MSG_ON_QUEUING.equals(action))
                {
                    Message message = new Message();
                    message.what = QUEUEING;
                    handler.sendMessage(message);

                }
                else if (NotifyMessage.CALL_MSG_ON_CANCEL_QUEUE.equals(action))
                {
                    Toast.makeText(ChatActivity.this, getString(R.string.cancel_queue_success), Toast.LENGTH_SHORT).show();
                    ChatActivity.this.finish();
                }
                else if (NotifyMessage.CALL_MSG_ON_QUEUE_TIMEOUT.equals(action))
                {
                    Toast.makeText(ChatActivity.this, getString(R.string.queue_timeout), Toast.LENGTH_SHORT).show();
                    ChatActivity.this.finish();
                }
                else if (NotifyMessage.CALL_MSG_ON_NET_QUALITY_LEVEL.equals(action))
                {

                }
                else if (NotifyMessage.CALL_MSG_ON_POLL.equals(action))
                {

                    int status = broadMsg.getRequestCode().getErrorCode();
                    tvNetStatus.setText(getString(R.string.net_loop_fail) + status);

                }
                else if (NotifyMessage.CALL_MSG_ON_DISCONNECTED.equals(action))
                {
                    Toast.makeText(ChatActivity.this, getString(R.string.disconnected), Toast.LENGTH_LONG).show();
                    ChatActivity.this.finish();
                }
                else if (NotifyMessage.CALL_MSG_ON_CONNECT.equals(action))
                {
                    if (null == broadMsg.getRequestCode().getRetCode())
                    {
                        Toast.makeText(ChatActivity.this, getString(R.string.connect_fail) + broadMsg.getRequestCode().getErrorCode(), Toast.LENGTH_LONG).show();
                        btnBack.setVisibility(View.VISIBLE);
                    }
                    else
                    {
                        if ("0".equals(broadMsg.getRequestCode().getRetCode()))
                        {
                            LogUtil.d(TAG, "get ability success");
                        }
                        else
                        {
                            Toast.makeText(ChatActivity.this, getString(R.string.connect_fail) + broadMsg.getRequestCode().getRetCode(), Toast.LENGTH_LONG).show();
                            btnBack.setVisibility(View.VISIBLE);
                        }
                    }
                }

                else if (NotifyMessage.CALL_MSG_GET_VIDEO_INFO.equals(action))
                {
                    StreamInfo streamInfo = broadMsg.getRequestInfo().getStreamInfo();
                    if (streamInfo == null)
                    {
                        Toast.makeText(ChatActivity.this, getString(R.string.no_stream_info), Toast.LENGTH_SHORT).show();
                        return;
                    }
                    else
                    {
                        //分辨率
                        videoSendFramsize = streamInfo.getEncoderSize();
                        //帧率
                        videoSendFrameRate = streamInfo.getSendFrameRate() + "";
                        //码率
                        videoSendDatarate.setLength(0);
                        videoSendDatarate.append(streamInfo.getVideoSendBitRate() / 1000);
                        videoSendDatarate.append('k');
                        //丢包率
                        videoSendPacketLossProbability = Float.valueOf(streamInfo.getVideoSendLossFraction()).intValue() + "";
                        videoRecvPacketLossProbability = Float.valueOf(streamInfo.getVideoRecvLossFraction()).intValue() + "";
                        //延迟
                        videoSendDelay = Float.valueOf(streamInfo.getVideoSendDelay()).intValue() + "";
                        videoRecvDelay = Float.valueOf(streamInfo.getVideoRecvDelay()).intValue() + "";
                        //抖动
                        videoSendJitter = Float.valueOf(streamInfo.getVideoSendJitter()).intValue() + "";
                        videoRecvJitter = Float.valueOf(streamInfo.getVideoRecvJitter()).intValue() + "";

                        /**接收部分**/
                        // 帧率
                        videoRecvFrameRate = streamInfo.getRecvFrameRate() + "";
                        // 分辨率
                        videoRecvFramsize = streamInfo.getDecoderSize();
                        // 码率
                        videoRecvDatarate.setLength(0);
                        videoRecvDatarate.append(streamInfo.getVideoRecvBitRate() / 1000);
                        videoRecvDatarate.append('k');
                        Toast.makeText(ChatActivity.this, getString(R.string.send_framsize) + videoSendFramsize + getString(R.string.frame_rate) + videoSendFrameRate + getString(R.string.bit_rate) + videoSendDatarate
                                + getString(R.string.loss_rate) + videoSendPacketLossProbability + getString(R.string.delay) + videoSendDelay + getString(R.string.jitter) + videoSendJitter + "\n"
                                + getString(R.string.receive_framsize) + videoRecvFramsize + getString(R.string.frame_rate) + videoRecvFrameRate + getString(R.string.bit_rate) + videoRecvDatarate
                                + getString(R.string.loss_rate) + videoRecvPacketLossProbability + getString(R.string.delay) + videoRecvDelay + getString(R.string.jitter) + videoRecvJitter, Toast.LENGTH_LONG).show();
                    }
                }

                else if (NotifyMessage.CALL_MSG_ON_DROPCALL.equals(action))
                {
                    String retcode = broadMsg.getRequestCode().getRetCode();
                    if (null == retcode)
                    {
                        Toast.makeText(ChatActivity.this, getString(R.string.dropcall_fail) + broadMsg.getRequestCode().getErrorCode(), Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        if ("0".equals(retcode))
                        {
                            LogUtil.d(TAG, "dropcall success");
                        }
                        else
                        {
                            Toast.makeText(ChatActivity.this, getString(R.string.dropcall_fail) + broadMsg.getRequestCode().getRetCode(), Toast.LENGTH_SHORT).show();
                        }
                    }
                }
            }
        }
    };

    private void micMute()
    {
        ++micMute;
        if (1 == micMute % 2)
        {
            MobileCC.getInstance().setMicMute(true);
            btnMicMute.setText(getString(R.string.resume));
        }
        else
        {
            MobileCC.getInstance().setMicMute(false);
            btnMicMute.setText(getString(R.string.mute_mic));
        }
    }

    private void speakerMute()
    {
        ++speakerMute;
        if (1 == speakerMute % 2)
        {
            MobileCC.getInstance().setSpeakerMute(true);
            btnSpeakerMute.setText(getString(R.string.resume));
        }
        else
        {
            MobileCC.getInstance().setSpeakerMute(false);
            btnSpeakerMute.setText(getString(R.string.mute_speaker));
        }


    }

    public void switchAudio()
    {
        ++speaker;
        if (0 == speaker % 2)
        {
            MobileCC.getInstance().changeAudioRoute(MobileCC.getInstance().AUDIO_ROUTE_SPEAKER);
        }
        else
        {
            MobileCC.getInstance().changeAudioRoute(MobileCC.getInstance().AUDIO_ROUTE_RECEIVER);
        }
        Toast.makeText(ChatActivity.this, (0 == speaker % 2 ? getString(R.string.speaker_mode) : getString(R.string.receiver_mode)), Toast.LENGTH_SHORT)
                .show();
    }

    @Override
    protected void onDestroy()
    {
        LogUtil.d(TAG, "onDestroy()");
        super.onDestroy();
    }

    @Override
    public void onBackPressed()
    {
        MobileCC.getInstance().releaseCall();
        super.onBackPressed();
    }
}
