package com.huawei.esdk.icsdemo.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.huawei.esdk.cc.MobileCC;
import com.huawei.esdk.cc.common.BroadMsg;
import com.huawei.esdk.cc.common.NotifyMessage;
import com.huawei.esdk.cc.utils.LogUtil;
import com.huawei.esdk.icsdemo.R;
import com.huawei.esdk.icsdemo.im.Msg;
import com.huawei.esdk.icsdemo.im.MsgAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class MSChatActivity extends Activity implements View.OnClickListener
{

    private static final String TAG = "MSChatActivity";
    private static final int GET_VERIFYCODE = 8;

    private static final int RECEIVEDATA = 1;
    private static final int SENDDATA = 4;
    private static final int CANCELQUEUE = 3;
    private static final int QUEUING = 2;
    private static final int AUDIOCONNECTED = 5;
    private static final int CLOSEDIALOG = 6;
    private int mute = 0;//1静音0恢复
    private int speaker = 0;//0是扬声器 1 是听筒

    private Button btnSend;
    private Button btnCall;
    private Button btnVideo;
    private Button btnLeave;
    private EditText etContent;
    private IntentFilter filter;
    private MsgAdapter adapter;
    private ListView msgListView;
    private List<Msg> msgList = new ArrayList<Msg>();
    private String receiveData = null;
    private Button btnCancelQueue;
    private String data = " ";
    private boolean isAudioConnected = false;
    private TextView tvStatus;
    private Button btnSwitchAudio;
    private Button btnMute;
    private LinearLayout audioView;
    private ProgressDialog progressDialog;

    private boolean hasAudioAbility = false;
    private boolean hasTextAbility = false;
    private boolean isQueuing = false;
    private String audioAccessCode;
    private Timer timer = null;
    private boolean isPause = false;
    private int num = 0;
    private String verifyCode;
    private Button btnVerifycode;
    private ImageView imageVerifycode;
    private EditText etVerifycode;
    private LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);


    private Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
                case RECEIVEDATA:
                    btnCancelQueue.setVisibility(View.INVISIBLE);

                    Msg receiveContent = new Msg((String) msg.obj, Msg.TYPE_RECEIVED);
                    msgList.add(receiveContent);
                    adapter.notifyDataSetChanged();
                    msgListView.setSelection(msgList.size());
                    break;

                case AUDIOCONNECTED:
                    Msg audioConnect = new Msg((String) msg.obj, Msg.TYPE_RECEIVED);
                    msgList.add(audioConnect);
                    adapter.notifyDataSetChanged();
                    msgListView.setSelection(msgList.size());
                    isPause = true;

                    break;
                case QUEUING:
                    btnCancelQueue.setVisibility(View.VISIBLE);
                    Msg msgStr2 = new Msg((String) msg.obj, Msg.TYPE_RECEIVED);
                    msgList.add(msgStr2);
                    adapter.notifyDataSetChanged();
                    msgListView.setSelection(msgList.size());
                    break;

                case CANCELQUEUE:
                    Toast.makeText(MSChatActivity.this, getString(R.string.cancel_queue_success), Toast.LENGTH_SHORT).show();
                    MSChatActivity.this.finish();
                    break;
                case SENDDATA:

                    Msg sendContent = new Msg((String) msg.obj, Msg.TYPE_SENT);
                    msgList.add(sendContent);
                    adapter.notifyDataSetChanged();
                    msgListView.setSelection(msgList.size());
                    break;
                case CLOSEDIALOG:
                    closeDialog();

                    break;

                case GET_VERIFYCODE:
                    String verifyValue = (String)msg.obj;
                    Bitmap bitmap = base64ToBitmap(verifyValue);
                    imageVerifycode.setImageBitmap(bitmap);
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
        setContentView(R.layout.activity_ms_chat);
        initView();
        filter = new IntentFilter();
        filter.addAction(NotifyMessage.CALL_MSG_ON_CONNECTED);
        filter.addAction(NotifyMessage.CHAT_MSG_ON_RECEIVE);
        filter.addAction(NotifyMessage.CHAT_MSG_ON_SEND);
        filter.addAction(NotifyMessage.CALL_MSG_ON_DISCONNECTED);
        filter.addAction(NotifyMessage.CALL_MSG_USER_STATUS);
        filter.addAction(NotifyMessage.CALL_MSG_ON_QUEUE_INFO);
        filter.addAction(NotifyMessage.CALL_MSG_ON_DROPCALL);
        filter.addAction(NotifyMessage.CALL_MSG_ON_SUCCESS);
        filter.addAction(NotifyMessage.CALL_MSG_ON_QUEUE_TIMEOUT);
        filter.addAction(NotifyMessage.CALL_MSG_ON_FAIL);
        filter.addAction(NotifyMessage.CALL_MSG_ON_CANCEL_QUEUE);
        filter.addAction(NotifyMessage.CALL_MSG_ON_NET_QUALITY_LEVEL);
        filter.addAction(NotifyMessage.CALL_MSG_ON_QUEUING);
        filter.addAction(NotifyMessage.CALL_MSG_ON_POLL);
        filter.addAction(NotifyMessage.CALL_MSG_ON_CONNECT);
        filter.addAction(NotifyMessage.CALL_MSG_ON_APPLY_MEETING);
        filter.addAction(NotifyMessage.CALL_MSG_ON_VERIFYCODE);
        filter.addAction(NotifyMessage.CALL_MSG_ON_CALL_END);

        data = getIntent().getStringExtra("CallData");
        String accessCode = getIntent().getStringExtra("AccessCode");
        audioAccessCode = getIntent().getStringExtra("AudioAccessCode");
        verifyCode = getIntent().getStringExtra("VerifyCode");

        if (0 != MobileCC.getInstance().webChatCall(accessCode, data, verifyCode))
        {
            btnLeave.setVisibility(View.VISIBLE);
            LogUtil.d(TAG, "makecall()  fail");
            Toast.makeText(MSChatActivity.this, getString(R.string.call_fail_return), Toast.LENGTH_SHORT).show();
        }

        localBroadcastManager.registerReceiver(receiver, filter);
    }

    private void initView()
    {
        etContent = (EditText) findViewById(R.id.et_chat);
        //先写一个文本做测试用
        etContent.setText("Help! Help!");
        btnSend = (Button) findViewById(R.id.btn_send);
        btnCall = (Button) findViewById(R.id.btn_call);
        btnVideo = (Button) findViewById(R.id.btn_video);
        btnLeave = (Button) findViewById(R.id.leaveChat);
        btnLeave.setVisibility(View.INVISIBLE);
        btnCancelQueue = (Button) findViewById(R.id.cancelQueue);
        tvStatus = (TextView) findViewById(R.id.tvStatus);
        //ListView和adapter的初始化
        adapter = new MsgAdapter(MSChatActivity.this, R.layout.msg_item, msgList);
        msgListView = (ListView) findViewById(R.id.msg_list_view);
        msgListView.setAdapter(adapter);

        btnSwitchAudio = (Button) findViewById(R.id.switch_audio);
        btnMute = (Button) findViewById(R.id.mute_audio);
        audioView = (LinearLayout) findViewById(R.id.view_audio);

        btnVerifycode = (Button)findViewById(R.id.button_get_verifycode);
        imageVerifycode = (ImageView)findViewById(R.id.iamgeview_verifycode);
        etVerifycode = (EditText)findViewById(R.id.edittext_verifycode);

        btnSwitchAudio.setOnClickListener(this);
        btnMute.setOnClickListener(this);

        btnLeave.setOnClickListener(this);
        btnCall.setOnClickListener(this);
        btnSend.setOnClickListener(this);
        btnVideo.setOnClickListener(this);
        btnCancelQueue.setOnClickListener(this);
        btnVerifycode.setOnClickListener(this);
    }

    private int send(String content)
    {
        return MobileCC.getInstance().sendMsg(content);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

    }

    @Override
    protected void onDestroy()
    {
        if (receiver != null)
        {
            localBroadcastManager.unregisterReceiver(receiver);
        }
        stopTimer();
        super.onDestroy();
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.btn_call:
                getCallConnect();
                break;

            case R.id.btn_send:
                String content = etContent.getText().toString();
                send(content);
                etContent.setText("");
                break;

            case R.id.btn_video:
                getVideoConnect();
                break;
            case R.id.switch_audio:
                switchAudio();
                break;

            case R.id.mute_audio:
                mute();
                break;

            case R.id.leaveChat:
                if (hasAudioAbility)
                {
                    MobileCC.getInstance().releaseCall();
                    LogUtil.d(TAG, "button --> drop audioconnect");
                }
                if (hasTextAbility)
                {
                    MobileCC.getInstance().releaseWebChatCall();
                    LogUtil.d(TAG, "button--> drop textconnect");
                }
                MSChatActivity.this.finish();
                break;

            case R.id.cancelQueue:
                MobileCC.getInstance().cancelQueue();
                break;

            case R.id.button_get_verifycode:
                MobileCC.getInstance().getVerifyCode();
                break;

            default:
                break;

        }
    }

    private void getCallConnect()
    {
        if (isAudioConnected)
        {
            MobileCC.getInstance().releaseCall();
        }
        else
        {
            if (etVerifycode.getText().toString().isEmpty() || null == etVerifycode.getText().toString())
            {
                Toast.makeText(MSChatActivity.this, "请正确设置验证码", Toast.LENGTH_SHORT).show();
            }
            else
            {
                isPause = false;
                LogUtil.d(TAG, "-----------audio call-------------");
                MobileCC.getInstance().makeCall(audioAccessCode, MobileCC.AUDIO_CALL, data, etVerifycode.getText().toString());
                showDialog();
            }
        }
    }

    private void getVideoConnect()
    {
        if (hasAudioAbility)
        {
            MobileCC.getInstance().updateToVideo();
        }
        else
        {
            if (etVerifycode.getText().toString().isEmpty() || null == etVerifycode.getText().toString())
            {
                Toast.makeText(MSChatActivity.this, "请正确设置验证码", Toast.LENGTH_SHORT).show();
            }
            else
            {
                MobileCC.getInstance().makeCall(audioAccessCode, MobileCC.VIDEO_CALL, data,etVerifycode.getText().toString());
            }

        }
    }

    private void switchAudio()
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
        Toast.makeText(MSChatActivity.this, (0 == speaker % 2 ? getString(R.string.speaker_mode) : getString(R.string.receiver_mode)), Toast.LENGTH_SHORT)
                .show();
    }

    private void mute()
    {
        ++mute;
        if (1 == mute % 2)
        {
            MobileCC.getInstance().setMicMute(true);
            btnMute.setText(getString(R.string.resume));
        }
        else
        {
            MobileCC.getInstance().setMicMute(false);
            btnMute.setText(getString(R.string.mute_mic));
        }
    }

    @Override
    public void onBackPressed()
    {
        if (hasAudioAbility || isQueuing)
        {
            MobileCC.getInstance().releaseCall();
            LogUtil.d(TAG, "back --> drop audio connect or cancel queue");
        }
        if (hasTextAbility)
        {
            MobileCC.getInstance().releaseWebChatCall();
            LogUtil.d(TAG, "back--> drop text connect");
        }

        super.onBackPressed();
    }

    private BroadcastReceiver receiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            BroadMsg broadMsg = (BroadMsg) intent
                    .getSerializableExtra(NotifyMessage.CC_MSG_CONTENT);

            LogUtil.d(TAG, "action === " + action);

            if (NotifyMessage.CHAT_MSG_ON_SEND.equals(action))
            {
                if (null == broadMsg.getRequestCode().getRetCode())
                {
                    //retcode为空时
                    Toast.makeText(MSChatActivity.this, getString(R.string.text_send_fail) + broadMsg.getRequestCode().getErrorCode(), Toast.LENGTH_LONG).show();
                }
                else
                {
                    //retcode不为为空时
                    String retcode = broadMsg.getRequestCode().getRetCode();
                    if (MobileCC.MESSAGE_OK.equals(retcode))
                    {
                        //消息发送成功
                        String content = broadMsg.getRequestInfo().getMsg();
                        if (!"".equals(content))
                        {
                            Message message = new Message();
                            message.what = SENDDATA;
                            message.obj = content;
                            handler.sendMessage(message);
                        }
                    }
                    else
                    {
                        //消息发送未成功
                        Toast.makeText(MSChatActivity.this, getString(R.string.text_send_fail) + retcode, Toast.LENGTH_LONG).show();
                    }
                }
            }
            else if (NotifyMessage.CHAT_MSG_ON_RECEIVE.equals(action))
            {
                //收到IM消息
                receiveData = broadMsg.getRequestInfo().getMsg();
                LogUtil.d(TAG, "receive msg： " + receiveData);

                Message message = new Message();
                message.what = RECEIVEDATA;
                message.obj = receiveData;
                handler.sendMessage(message);
            }
            else if (NotifyMessage.CALL_MSG_ON_CONNECTED.equals(action))
            {
                //与坐席连接成功
                btnCancelQueue.setVisibility(View.INVISIBLE);

                isQueuing = false;
                Message message = new Message();

                if (MobileCC.MESSAGE_TYPE_TEXT.equals(broadMsg.getRequestInfo().getMsg()))
                {
                    message.what = RECEIVEDATA;
                    message.obj = getString(R.string.text_connect);
                    handler.sendMessage(message);
                    showVerifyCodeLayout();
                    hasTextAbility = true;
                }
                else if (MobileCC.MESSAGE_TYPE_AUDIO.equals(broadMsg.getRequestInfo().getMsg()))
                {
                    hasAudioAbility = true;
                    hideVerifyCodeLayout();
                }
            }
            else if (NotifyMessage.CALL_MSG_ON_QUEUING.equals(action))
            {
                isQueuing = true;
                Message message = new Message();
                message.what = QUEUING;
                message.obj = getString(R.string.queuing);
                handler.sendMessage(message);
                btnCancelQueue.setVisibility(View.VISIBLE);
                hideVerifyCodeLayout();
            }
            else if (NotifyMessage.CALL_MSG_ON_QUEUE_INFO.equals(action))
            {
                if (null == broadMsg.getRequestCode().getRetCode())
                {
                    Toast.makeText(MSChatActivity.this, getString(R.string.get_queue_info_error) + broadMsg.getRequestCode().getErrorCode(), Toast.LENGTH_SHORT).show();
                }
                else
                {
                    String retcode = broadMsg.getRequestCode().getRetCode();
                    if (MobileCC.MESSAGE_OK.equals(retcode))
                    {
                        //正处于排队状态
                        long position = broadMsg.getQueueInfo().getPosition();
                        LogUtil.d(TAG, "queuing , position =" + position);

                        Message message = new Message();
                        message.what = QUEUING;
                        message.obj = position;
                        handler.sendMessage(message);
                    }
                    else
                    {
                        //不是排队状态
                        Toast.makeText(MSChatActivity.this, getString(R.string.not_queue_status_error) + retcode, Toast.LENGTH_SHORT).show();
                    }
                }
            }
            else if (NotifyMessage.CALL_MSG_ON_CANCEL_QUEUE.equals(action))
            {
                LogUtil.d(TAG, "cancel queue success");
                Message message = new Message();
                message.what = CANCELQUEUE;
                handler.sendMessage(message);
            }
            else if (NotifyMessage.CALL_MSG_ON_QUEUE_TIMEOUT.equals(action))
            {
                Toast.makeText(MSChatActivity.this, getString(R.string.queue_timeout), Toast.LENGTH_SHORT).show();
                MSChatActivity.this.finish();
            }
            else if (NotifyMessage.CALL_MSG_ON_NET_QUALITY_LEVEL.equals(action))
            {
                int netLevel = broadMsg.getRequestCode().getNetLevel();
                LogUtil.d(TAG, "net level is:" + netLevel);
                if (1 == netLevel)
                {
                    isPause = true;
                }
            }
            else if (NotifyMessage.CALL_MSG_ON_CONNECT.equals(action))
            {
                if (null == broadMsg.getRequestCode().getRetCode())
                {
                    //没有收到服务器的retcode，判断是文字还是语音，打印出错误码
                    if (MobileCC.MESSAGE_TYPE_TEXT.equals(broadMsg.getType()))
                    {
                        Toast.makeText(MSChatActivity.this, getString(R.string.text_connect_fail) + broadMsg.getRequestCode().getErrorCode(), Toast.LENGTH_LONG).show();
                        btnLeave.setVisibility(View.VISIBLE);
                    }
                    else
                    {
                        Toast.makeText(MSChatActivity.this, getString(R.string.audio_connect_fail) + broadMsg.getRequestCode().getErrorCode(), Toast.LENGTH_LONG).show();
                        isPause = true;
                    }
                }
                else
                {
                    //收到服务器的retcode，判断是文字还是语音，打印出错误码
                    if (MobileCC.MESSAGE_OK.equals(broadMsg.getRequestCode().getRetCode()))
                    {
                        if (MobileCC.MESSAGE_TYPE_TEXT.equals(broadMsg.getType()))
                        {
                            LogUtil.d(TAG, "webChatCall --->get callId success");
                        }
                        else
                        {
                            LogUtil.d(TAG, "get audio ablity success");
                        }
                    }
                    else
                    {
                        if (MobileCC.MESSAGE_TYPE_TEXT.equals(broadMsg.getType()))
                        {
                            Toast.makeText(MSChatActivity.this, getString(R.string.text_connect_fail) + broadMsg.getRequestCode().getRetCode(), Toast.LENGTH_LONG).show();
                            btnLeave.setVisibility(View.VISIBLE);
                        }
                        else
                        {
                            Toast.makeText(MSChatActivity.this, getString(R.string.audio_connect_fail) + broadMsg.getRequestCode().getRetCode(), Toast.LENGTH_LONG).show();
                            isPause = true;
                        }
                    }
                }
            }
            else if (NotifyMessage.CALL_MSG_ON_DISCONNECTED.equals(action))
            {
                //断开链接
                Message message = new Message();
                message.what = RECEIVEDATA;
                if (MobileCC.MESSAGE_TYPE_TEXT.equals(broadMsg.getRequestInfo().getMsg()))
                {
                    message.obj = getString(R.string.text_disconnect);
                    hasTextAbility = false;
                    handler.sendMessage(message);

                }
                else if (MobileCC.MESSAGE_TYPE_AUDIO.equals(broadMsg.getRequestInfo().getMsg()))
                {

                    Toast.makeText(MSChatActivity.this, R.string.audio_call_disconnect, Toast.LENGTH_SHORT).show();
                    audioView.setVisibility(View.INVISIBLE);
                    isAudioConnected = false;
                    hasAudioAbility = false;
                    showVerifyCodeLayout();
                    btnCall.setText(getString(R.string.audio_call));
                }
                btnLeave.setVisibility(View.VISIBLE);


            }
            else if (NotifyMessage.CALL_MSG_USER_STATUS.equals(action))
            {
                LogUtil.d(TAG, "MS: get conf info, into ConferenceActivity");
                Intent confIntent = new Intent(MSChatActivity.this, ConferenceActivity.class);
                startActivity(confIntent);
            }
            else if (NotifyMessage.CALL_MSG_ON_SUCCESS.equals(action))
            {
                //语音连接成功
                isAudioConnected = true;
                isPause = true;
                audioView.setVisibility(View.VISIBLE);
                btnCall.setText(getString(R.string.hang_up));
                Toast.makeText(MSChatActivity.this, getString(R.string.call_success), Toast.LENGTH_SHORT).show();
            }
            else if (NotifyMessage.CALL_MSG_ON_FAIL.equals(action))
            {
                isPause = true;
                btnLeave.setVisibility(View.VISIBLE);
                LogUtil.d(TAG, "call fail ");
                Toast.makeText(MSChatActivity.this, getString(R.string.call_fail_return), Toast.LENGTH_SHORT).show();
                showVerifyCodeLayout();
            }

            else if (NotifyMessage.CALL_MSG_ON_CALL_END.equals(action))
            {
                isPause = true;
                btnLeave.setVisibility(View.VISIBLE);
                LogUtil.d(TAG, "call end ");
                Toast.makeText(MSChatActivity.this, getString(R.string.call_end), Toast.LENGTH_SHORT).show();
                showVerifyCodeLayout();
            }

            else if (NotifyMessage.CALL_MSG_ON_POLL.equals(action))
            {
                int status = broadMsg.getRequestCode().getErrorCode();
                tvStatus.setText(getString(R.string.net_loop_fail) + status);
                isPause = true;
            }

            else if (NotifyMessage.CALL_MSG_ON_APPLY_MEETING.equals(action))
            {

                if (null == broadMsg.getRequestCode().getRetCode())
                {
                    Toast.makeText(MSChatActivity.this, getString(R.string.apply_meeting_fail) + broadMsg.getRequestCode().getErrorCode(), Toast.LENGTH_SHORT).show();
                }
                else
                {
                    String retcode = broadMsg.getRequestCode().getRetCode();

                    if (MobileCC.MESSAGE_OK.equals(retcode))
                    {

                    }
                    else
                    {
                        Toast.makeText(MSChatActivity.this, getString(R.string.apply_meeting_fail) + retcode, Toast.LENGTH_SHORT).show();
                    }
                }
            }
            else if (NotifyMessage.CALL_MSG_ON_DROPCALL.equals(action))
            {
                String recode = broadMsg.getRequestCode().getRetCode();
                if (null == recode)
                {
                    Toast.makeText(MSChatActivity.this, getString(R.string.dropcall_fail) + broadMsg.getRequestCode().getErrorCode(), Toast.LENGTH_SHORT).show();
                }
                else
                {
                    if (MobileCC.MESSAGE_OK.equals(recode))
                    {
                        LogUtil.d(TAG, "drop call success");
                    }
                    else
                    {
                        Toast.makeText(MSChatActivity.this, getString(R.string.dropcall_fail) + broadMsg.getRequestCode().getRetCode(), Toast.LENGTH_SHORT).show();
                    }
                }
            }

            else if (NotifyMessage.CALL_MSG_ON_VERIFYCODE.equals(action))
            {
                if (null == broadMsg.getRequestCode().getRetCode())
                {
                    Toast.makeText(MSChatActivity.this, getString(R.string.get_varifycode_fail) + broadMsg.getRequestCode().getErrorCode(), Toast.LENGTH_SHORT).show();
                }
                else
                {
                    String retcode = broadMsg.getRequestCode().getRetCode();

                    if ("0".equals(retcode))
                    {
                        //收到验证码
                        String verifyCode = broadMsg.getRequestInfo().getMsg();

                        Message message = new Message();
                        message.what = GET_VERIFYCODE;
                        message.obj = verifyCode;
                        handler.sendMessage(message);
                    }
                    else
                    {
                        //没有收到验证码
                        Toast.makeText(MSChatActivity.this, getString(R.string.get_varifycode_fail) + retcode, Toast.LENGTH_SHORT).show();
                    }
                }

            }
        }
    };

    /**
     * 等待对话框
     */
    private void showDialog()
    {
        if (null == progressDialog)
        {
            progressDialog = new ProgressDialog(this);
            progressDialog.setMessage(getString(R.string.calling));
            progressDialog.setCanceledOnTouchOutside(false);
            progressDialog.setCancelable(false);
        }
        progressDialog.show();
        beginTask();
    }

    private void closeDialog()
    {
        if (null != progressDialog && progressDialog.isShowing())
        {
            progressDialog.dismiss();
        }
    }

    public void beginTask()
    {
        if (timer == null)
        {
            timer = new Timer();
            timer.schedule(task, 1000, 1000);
        }
    }

    private TimerTask task = new TimerTask()
    {
        @Override
        public void run()
        {
            LogUtil.d(TAG, "num = " + num + " Pause is: " + isPause);
            if (num == 40)
            {
                isPause = true;
                Message message = new Message();
                message.what = CLOSEDIALOG;
                handler.sendMessage(message);
            }
            if (isPause)
            {
                num = 0;
                Message message = new Message();
                message.what = CLOSEDIALOG;
                handler.sendMessage(message);
            }
            else
            {
                num++;
            }
        }
    };

    private void stopTimer()
    {

        if (timer != null)
        {
            timer.cancel();
            timer = null;
        }
        if (task != null)
        {
            task.cancel();
            task = null;
        }
    }
    public static Bitmap base64ToBitmap(String base64Data)
    {
        byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    private void showVerifyCodeLayout()
    {
        btnVerifycode.setVisibility(View.VISIBLE);
        imageVerifycode.setVisibility(View.VISIBLE);
        etVerifycode.setVisibility(View.VISIBLE);
    }

    private void hideVerifyCodeLayout()
    {
        btnVerifycode.setVisibility(View.INVISIBLE);
        imageVerifycode.setVisibility(View.INVISIBLE);
        etVerifycode.setVisibility(View.INVISIBLE);
    }

}
