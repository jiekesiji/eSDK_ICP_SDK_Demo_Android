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
package com.huawei.esdk.icsdemo.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.RelativeLayout;
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


public class ConfShareActivity extends Activity
{
    private Button leaveConfBtn;
    private ImageButton backConfBtn;
    private RelativeLayout desktopSharedLayout;
    private final String SHARE_START = "1";
    private final String SHARE_COMPONENT = "2";
    private LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);

    private String TAG = ConfShareActivity.class.getSimpleName();
    private BroadcastReceiver receiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            String action = intent.getAction();
            LogUtil.d(TAG, "action = " + action);
            if (NotifyMessage.CALL_MSG_USER_END.equals(action))
            {
                finish();
            }
            else if (NotifyMessage.CALL_MSG_USER_RECEIVE_SHARED_DATA.equals(action))
            {
                BroadMsg notifyMsg = (BroadMsg) intent
                        .getSerializableExtra(NotifyMessage.CC_MSG_CONTENT);
                String recode = notifyMsg.getRequestCode().getRetCode();
                String msg = notifyMsg.getRequestInfo().getMsg();

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
                if (SHARE_START.equals(msg))
                {
                    sharedState = "begin !";
                }
                else
                {
                    sharedState = "end !";
                }
                LogUtil.d(TAG, sharedType + " - " + sharedState);
            }
            else if (NotifyMessage.CALL_MSG_ON_STOP_MEETING.equals(action))
            {
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_conf_share);

        IntentFilter filter = new IntentFilter();
        filter.addAction(NotifyMessage.CALL_MSG_USER_END);
        filter.addAction(NotifyMessage.CALL_MSG_USER_RECEIVE_SHARED_DATA);
        filter.addAction(NotifyMessage.CALL_MSG_ON_STOP_MEETING);
        localBroadcastManager.registerReceiver(receiver, filter);

        initComp();


    }

    @Override
    protected void onResume()
    {
        super.onResume();
        MobileCC.getInstance().setDesktopShareContainer(getBaseContext(),
                desktopSharedLayout); // 接收共享数据，设置显示容器
    }

    private void initComp()
    {
        desktopSharedLayout = (RelativeLayout) findViewById(R.id.desktopSharedLayout); // sharedView
        leaveConfBtn = (Button) findViewById(R.id.leaveConf);
        backConfBtn = (ImageButton) findViewById(R.id.backConf);
        leaveConfBtn.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                MobileCC.getInstance().releaseCall(); // 结束会议
            }
        });
        backConfBtn.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                ConfShareActivity.this.finish();
            }
        });
    }


    @Override
    protected void onDestroy()
    {
        localBroadcastManager.unregisterReceiver(receiver);
        super.onDestroy();
    }

    @Override
    public void onBackPressed()
    {

    }
}
