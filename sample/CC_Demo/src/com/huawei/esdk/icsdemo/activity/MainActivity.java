package com.huawei.esdk.icsdemo.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.huawei.esdk.cc.MobileCC;
import com.huawei.esdk.cc.common.BroadMsg;
import com.huawei.esdk.cc.common.NotifyMessage;
import com.huawei.esdk.cc.service.CCAPP;
import com.huawei.esdk.cc.utils.LogUtil;
import com.huawei.esdk.icsdemo.R;

import java.io.IOException;
import java.io.InputStream;

public class MainActivity extends Activity implements View.OnClickListener
{

    private static final String TAG = "MainActivity";
    private static final int GET_VERIFYCODE = 1;
    private EditText etIP;
    private EditText etPort;
    private EditText etName;
    private Button btnLogin;
    private RelativeLayout relativeLayout;
    private RadioGroup radioGroupPlatform = null;
    private RadioButton radioButtonTP = null;
    private RadioButton radioButtonMS = null;
    private EditText etAccessCode;
    private Button btnStartConversations;
    private Button btnExit;
    private IntentFilter filter;
    private int callType = MobileCC.SERVER_TP;
    private EditText etData;
    private RadioGroup radioGroupType = null;
    private RadioButton radioButtonHTTP = null;
    private RadioButton radioButtonHTTPS = null;

    private RadioGroup radioGroupCallType = null;
    private RadioButton radioButtonTLS = null;
    private RadioButton radioButtonUDP = null;
    private boolean isHttps = false;
    private EditText etSIPAddress;
    private EditText etSIPPort;
    private EditText etAudioAccesscode;
    private LinearLayout layoutSIP;
    private Button btnVerifycode;
    private ImageView imageVerifycode;
    private EditText etVerifycode;
    private LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);



    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initView();
    }

    /**
     * 初始化view、监听和filter
     */
    private void initView()
    {
        etIP = (EditText) findViewById(R.id.et_ip);
        etPort = (EditText) findViewById(R.id.et_port);
        etName = (EditText) findViewById(R.id.et_name);
        btnLogin = (Button) findViewById(R.id.btn_login);
        relativeLayout = (RelativeLayout) findViewById(R.id.relativeLayout);
        etAccessCode = (EditText) findViewById(R.id.et_accessCode);
        etData = (EditText) findViewById(R.id.etData);
        btnStartConversations = (Button) findViewById(R.id.btn_Call);
        btnExit = (Button) findViewById(R.id.btn_exit);
        radioGroupPlatform = (RadioGroup) findViewById(R.id.radio_group_platform);
        radioButtonTP = (RadioButton) findViewById(R.id.radio_button_tp);
        radioButtonMS = (RadioButton) findViewById(R.id.radio_button_ms);
        radioGroupType = (RadioGroup) findViewById(R.id.radio_group_type);
        radioButtonHTTP = (RadioButton) findViewById(R.id.radio_button_http);
        radioButtonHTTPS = (RadioButton) findViewById(R.id.radio_button_https);
        etSIPAddress = (EditText) findViewById(R.id.etSIPAddress);
        etSIPPort = (EditText) findViewById(R.id.etSIPPort);
        layoutSIP = (LinearLayout) findViewById(R.id.layout_sip);
        etAudioAccesscode = (EditText) findViewById(R.id.etAudioAccessCode);

        btnVerifycode = (Button)findViewById(R.id.button_get_verifycode);
        imageVerifycode = (ImageView)findViewById(R.id.iamgeview_verifycode);
        etVerifycode = (EditText)findViewById(R.id.edittext_verifycode);

        radioGroupCallType = (RadioGroup) findViewById(R.id.radio_group_call_type);
        radioButtonTLS = (RadioButton) findViewById(R.id.radio_button_tls);
        radioButtonUDP = (RadioButton) findViewById(R.id.radio_button_udp);

        radioGroupPlatform.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                if (radioButtonTP.getId() == checkedId)
                {
                    callType = MobileCC.SERVER_TP;
                    etIP.setText("172.22.8.99");
                    etAccessCode.setText("1007");
                    radioGroupCallType.setVisibility(View.VISIBLE);

                    MobileCC.getInstance().setTransportSecurity(true, true);
                }
                else if (radioButtonMS.getId() == checkedId)
                {
                    callType = MobileCC.SERVER_MS;
                    etIP.setText("10.170.197.219");
                    etAccessCode.setText("2001");
                    radioGroupCallType.setVisibility(View.INVISIBLE);
                    MobileCC.getInstance().setTransportSecurity(false, false);
                }

            }
        });

        radioButtonMS.setChecked(true);

        radioGroupType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup radioGroup, int checkedId)
            {
                if (radioButtonHTTP.getId() == checkedId)
                {
                    etIP.setText("172.22.8.99");
                    isHttps = false;
                    etPort.setText("8280");
                }
                else if (radioButtonHTTPS.getId() == checkedId)
                {
                    isHttps = true;
                    etPort.setText("8243");
                }
            }
        });

        radioGroupCallType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener()
        {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId)
            {
                if (radioButtonTLS.getId() == checkedId)
                {
                    MobileCC.getInstance().setTransportSecurity(true, true);
                }
                else if (radioButtonUDP.getId() == checkedId)
                {
                    MobileCC.getInstance().setTransportSecurity(false, false);
                }

            }
        });
        radioButtonHTTPS.setChecked(true);
        btnLogin.setOnClickListener(this);
        btnStartConversations.setOnClickListener(this);
        btnExit.setOnClickListener(this);
        btnVerifycode.setOnClickListener(this);

        filter = new IntentFilter();
        filter.addAction(NotifyMessage.AUTH_MSG_ON_LOGIN);
        filter.addAction(NotifyMessage.AUTH_MSG_ON_LOGOUT);
        filter.addAction(NotifyMessage.CALL_MSG_ON_VERIFYCODE);

        LogUtil.d(TAG, "Android Version is: " + android.os.Build.VERSION.SDK_INT);
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        localBroadcastManager.registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        localBroadcastManager.unregisterReceiver(receiver);
    }

    @Override
    public void onClick(View view)
    {
        switch (view.getId())
        {
            case R.id.btn_login:
                login();
                break;

            case R.id.btn_Call:
                call();
                break;

            case R.id.btn_exit:
                MobileCC.getInstance().logout();
                break;


            case R.id.button_get_verifycode:
                MobileCC.getInstance().getVerifyCode();
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

            if (NotifyMessage.AUTH_MSG_ON_LOGIN.equals(action))
            {
                //当retcode为空时，通过errorcode提示用户
                if (null == broadMsg.getRequestCode().getRetCode())
                {
                    Toast.makeText(MainActivity.this, getString(R.string.login_fail) + broadMsg.getRequestCode().getErrorCode(), Toast.LENGTH_SHORT).show();
                }
                //当retcode不为空时，通过服务器返回的错误码来提示用户
                else
                {
                    if (("0").equals(broadMsg.getRequestCode().getRetCode()))
                    {
                        //登录成功
                        radioGroupPlatform.setVisibility(View.INVISIBLE);
                        radioGroupType.setVisibility(View.INVISIBLE);
                        radioGroupCallType.setVisibility(View.INVISIBLE);
                        btnLogin.setVisibility(View.INVISIBLE);
                        relativeLayout.setVisibility(View.VISIBLE);
                        Toast.makeText(MainActivity.this, getString(R.string.login_success), Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        Toast.makeText(MainActivity.this, getString(R.string.login_fail) + broadMsg.getRequestCode().getRetCode(), Toast.LENGTH_SHORT).show();
                    }
                }
            }
            else if (NotifyMessage.AUTH_MSG_ON_LOGOUT.equals(action))
            {
                if (null == broadMsg.getRequestCode().getRetCode())
                {
                    Toast.makeText(MainActivity.this, getString(R.string.logout_fail) + broadMsg.getRequestCode().getErrorCode(), Toast.LENGTH_SHORT).show();
                }
                else
                {
                    //注销成功
                    if (("0").equals(broadMsg.getRequestCode().getRetCode()))
                    {
                        Toast.makeText(MainActivity.this, getString(R.string.logout_success), Toast.LENGTH_SHORT).show();
                    }
                    else
                    {
                        Toast.makeText(MainActivity.this, getString(R.string.logout_fail) + broadMsg.getRequestCode().getRetCode(), Toast.LENGTH_SHORT).show();
                    }
                }
                radioGroupPlatform.setVisibility(View.VISIBLE);
                btnLogin.setVisibility(View.VISIBLE);
                relativeLayout.setVisibility(View.GONE);
                radioGroupType.setVisibility(View.VISIBLE);
                if (callType == MobileCC.SERVER_TP)
                {
                    radioGroupCallType.setVisibility(View.VISIBLE);
                }
                imageVerifycode.setImageBitmap(null);
            }

            else if (NotifyMessage.CALL_MSG_ON_VERIFYCODE.equals(action))
            {
                if (null == broadMsg.getRequestCode().getRetCode())
                {
                    Toast.makeText(MainActivity.this, getString(R.string.get_varifycode_fail) + broadMsg.getRequestCode().getErrorCode(), Toast.LENGTH_SHORT).show();
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
                        Toast.makeText(MainActivity.this, getString(R.string.get_varifycode_fail) + retcode, Toast.LENGTH_SHORT).show();
                    }
                }
            }
        }
    };
    private int setHostAddress()
    {
        String ipStr = etIP.getText().toString();
        String portStr = etPort.getText().toString();
        if (MobileCC.SERVER_MS == callType)
        {
            etSIPAddress.setText("10.170.197.220");
            etSIPPort.setText("5063");
            etAudioAccesscode.setText("1001");

            layoutSIP.setVisibility(View.VISIBLE);
            return MobileCC.getInstance().setHostAddress(ipStr, portStr, isHttps, MobileCC.SERVER_MS);
        }
        else if (MobileCC.SERVER_TP == callType)
        {
            //MS中不涉及https的问题
            layoutSIP.setVisibility(View.GONE);
            return MobileCC.getInstance().setHostAddress(ipStr, portStr, isHttps, MobileCC.SERVER_TP);
        }

        return -1;
    }

    private void login()
    {
        hideKeyboard();
        saveCA();
        if (0 == (setHostAddress()))
        {
            if (0 != MobileCC.getInstance().login("1", etName.getText().toString().trim()))
            {
                Toast.makeText(MainActivity.this, getString(R.string.retype_name_error), Toast.LENGTH_SHORT).show();
            }
        }
        else
        {
            Toast.makeText(MainActivity.this, getString(R.string.net_settings_error), Toast.LENGTH_SHORT).show();
        }
    }

    private void call()
    {
        hideKeyboard();


        if (MobileCC.SERVER_TP == callType)
        {
            if (etAccessCode.getText().toString().isEmpty() ||etVerifycode.getText().toString().isEmpty()
                    || null == etVerifycode.getText().toString() || null == etAccessCode.getText().toString() )
            {
                Toast.makeText(MainActivity.this, "请正确设置接入码/验证码", Toast.LENGTH_SHORT).show();
            }
            else
            {
                Intent intent2 = new Intent(MainActivity.this, ChatActivity.class);
                intent2.putExtra("AccessCode", etAccessCode.getText().toString());
                intent2.putExtra("CallData", etData.getText().toString());
                intent2.putExtra("VerifyCode", etVerifycode.getText().toString());
                imageVerifycode.setImageBitmap(null);
                startActivity(intent2);
            }
        }
        else if (MobileCC.SERVER_MS == callType)
        {


            if (0 == MobileCC.getInstance().setSIPServerAddress(etSIPAddress.getText().toString(), etSIPPort.getText().toString()))
            {
                if ("".equals(etAudioAccesscode.getText().toString().trim()))
                {
                    Toast.makeText(MainActivity.this, getString(R.string.set_audio_accesscode), Toast.LENGTH_SHORT).show();
                }
                else if (etAccessCode.getText().toString().isEmpty() ||etVerifycode.getText().toString().isEmpty()
                        || null == etVerifycode.getText().toString() || null == etAccessCode.getText().toString() )
                {
                    Toast.makeText(MainActivity.this, "请正确设置接入码/验证码", Toast.LENGTH_SHORT).show();
                }
                else
                {
                    Intent intent3 = new Intent(MainActivity.this, MSChatActivity.class);
                    intent3.putExtra("AccessCode", etAccessCode.getText().toString());
                    intent3.putExtra("AudioAccessCode", etAudioAccesscode.getText().toString());
                    intent3.putExtra("CallData", etData.getText().toString());
                    intent3.putExtra("VerifyCode", etVerifycode.getText().toString());
                    imageVerifycode.setImageBitmap(null);
                    startActivity(intent3);
                }
            }
            else
            {
                Toast.makeText(MainActivity.this, getString(R.string.set_sip_info), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void hideKeyboard()
    {
        //收起小键盘
        View view = MainActivity.this.getCurrentFocus();
        IBinder binder = null;
        if (view == null)
        {
            return;
        }
        else
        {
            binder = view.getWindowToken();
        }

        if (binder == null)
        {
            return;
        }
        ((InputMethodManager) getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(binder, InputMethodManager.HIDE_NOT_ALWAYS);
    }

    @Override
    protected void onDestroy()
    {
        android.os.Process.killProcess(android.os.Process.myPid());
        super.onDestroy();
    }

    private void saveCA()
    {
        InputStream inputStream = null;
        try
        {
            inputStream = CCAPP.getInstances().getApplication().getAssets().open("certs/" + "server.cer");
            MobileCC.getInstance().setServerCertificateValidation(true,false, inputStream);

        } catch (IOException ioe)
        {
            LogUtil.d(TAG, "io Exception ");
        }
        finally
        {
            if (inputStream != null)
            {
                try
                {
                    inputStream.close();
                }
                catch (IOException e)
                {
                    LogUtil.d(TAG, "io Exception ");
                }
            }
        }
    }
    public static Bitmap base64ToBitmap(String base64Data)
    {
        byte[] bytes = Base64.decode(base64Data, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
    private Handler handler = new Handler()
    {
        @Override
        public void handleMessage(Message msg)
        {
            switch (msg.what)
            {
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
}
