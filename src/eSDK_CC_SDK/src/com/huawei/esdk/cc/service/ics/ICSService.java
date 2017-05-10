package com.huawei.esdk.cc.service.ics;

import com.huawei.esdk.cc.MobileCC;
import com.huawei.esdk.cc.common.BroadMsg;
import com.huawei.esdk.cc.common.NotifyMessage;
import com.huawei.esdk.cc.common.QueueInfo;
import com.huawei.esdk.cc.common.RequestCode;
import com.huawei.esdk.cc.common.RequestInfo;
import com.huawei.esdk.cc.service.ServiceConfig;
import com.huawei.esdk.cc.service.CCAPP;
import com.huawei.esdk.cc.service.ServiceManager;
import com.huawei.esdk.cc.service.call.CallManager;
import com.huawei.esdk.cc.service.conference.ConferenceInfo;
import com.huawei.esdk.cc.service.conference.ConferenceMgr;
import com.huawei.esdk.cc.service.ics.model.request.GetConnectRequest;
import com.huawei.esdk.cc.service.ics.model.request.LoginRequest;
import com.huawei.esdk.cc.service.ics.model.request.SendMsgRequest;
import com.huawei.esdk.cc.service.ics.model.response.ResultResponse;
import com.huawei.esdk.cc.service.ics.model.response.DropCallResponse;
import com.huawei.esdk.cc.service.ics.model.response.GetEventResponse;
import com.huawei.esdk.cc.service.ics.model.response.QueueInfoResponse;
import com.huawei.esdk.cc.service.ics.model.response.SendMsgResponse;
import com.huawei.esdk.cc.service.ics.model.response.StopConfResponse;
import com.huawei.esdk.cc.utils.LogUtil;
import com.huawei.esdk.cc.utils.StringUtils;
import com.huawei.esdk.cc.video.VideoControl;


import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.OkHttpClient;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;


/**
 * Created on 2016/5/10.
 */
public final class ICSService
{

    private static final Object LOCKOBJECT = new Object();
    private static final int MEDIA_TYPE_TP = 22;
    private static final int MEDIA_TYPE_TEXT = 1;
    private static final int MEDIA_TYPE_AUDIO = 2;
    private static final String TAG = "ICSService";
    private static ICSService instance;
    private boolean isLogout = false;
    private boolean isNat = false;
    private Retrofit localRetrofit = null;
    private OkHttpClient client =  new OkHttpClient();
    private SSLSocketFactory sslSocketFactory = null;
    private TrustManager[] trustManager = new TrustManager[]{
            new MyX509TrustManager()
    };
    private ServiceConfig serviceConfig = ServiceConfig.getInstance();
    private SystemSetting systemSetting = SystemSetting.getInstance();

    private ICSService()
    {
    }

    /**
     * MobileCC的单例
     *
     * @return ICSService
     */
    public static ICSService getInstance()
    {
        synchronized (LOCKOBJECT)
        {
            if (null == instance)
            {
                instance = new ICSService();
            }
            return instance;
        }

    }

    /**
     * 开启任务
     */
    private void beginTask()
    {
        beginLoop();
    }

    /**
     * 初始化SDK
     *
     * @param sipServerType sipServerType
     */
    public void initService(int sipServerType)
    {

        CallManager callManager = CallManager.getInstance();


        if (sipServerType == MobileCC.SERVER_MS)
        {
            CallManager.getInstance().loadMeeting();

        }
        callManager.tupInit();
    }

    /**
     * 登录
     *
     * @param name name
     */
    public void login(String name)
    {

        Retrofit retrofit = getRetrofit();
        if (retrofit == null)
        {
            return;
        }
        RequestManager requestManager = retrofit.create(RequestManager.class);
        Call call = requestManager.login(name, systemSetting.getVndID(),
                new LoginRequest(StringUtils.getIpAddress(), "", systemSetting.getServerIp()));
        if (call == null)
        {
            return;
        }
        call.enqueue(new Callback<ResultResponse>()
        {
            private BroadMsg broadMsg = new BroadMsg(NotifyMessage.AUTH_MSG_ON_LOGIN);

            @Override
            public void onResponse(Call<ResultResponse> call, Response<ResultResponse> response)
            {
                ResultResponse model = null;
                if (response != null)
                {
                    model = response.body();
                    if (model == null)
                    {
                        //返回信息出错
                        RequestCode requestCode = new RequestCode();
                        requestCode.setErrorCode(NotifyMessage.RET_ERROR_RESPONSE);
                        broadMsg.setRequestCode(requestCode);
                        CCAPP.getInstances().sendBroadcast(broadMsg);
                    }
                    else
                    {

                        logInfo(TAG, "login" + " " + model.getMessage());

                        String retcode = model.getRetcode();
                        if (MobileCC.MESSAGE_OK.equals(retcode))
                        {
                            logInfo(TAG, "login" + " " + "Retcode:" + model.getRetcode() + "\n Result:" + model.getResult());

                            //取出和保存guid和cookie的值，之后的所有请求都会用到
                            String guid = "";
                            if (null != response.headers())
                            {
                                String setGUID = response.headers().get("Set-GUID");
                                if (setGUID != null && setGUID.contains("="))
                                {
                                    String[] array = setGUID.split("=");
                                    if (array.length > 1)
                                    {
                                        guid = array[1];
                                        serviceConfig.setGuid(guid);
                                    }
                                }
                            }
                            String cookie = "";

                            logInfo(TAG, "login" + " " + "heads: " + response.headers().toString());
                            if (null != response.headers())
                            {
                                if (response.headers().toString().contains("Set-Cookie"))
                                {
                                String setCookie = response.headers().get("Set-Cookie");
                                if (setCookie != null && setCookie.contains("="))
                                {
                                    String[] array = setCookie.split("=");
                                    if (array.length > 1)
                                    {
                                        String cookieTemp = array[1];
                                        String[] cookieValues = cookieTemp.split(";");

                                        if (cookieValues.length > 1)
                                        {
                                            cookie = cookieValues[0];
                                        }
                                    }
                                }
                              }
                            }
                            serviceConfig.setCookie(cookie);
                            logInfo(TAG, "login" + " " + "cookie: " + cookie);

                            logInfo(TAG, "login" + " " + "login success");
                            //发送登录成功的广播
                            RequestCode requestCode = new RequestCode();
                            requestCode.setRetCode(retcode);
                            broadMsg.setRequestCode(requestCode);
                            CCAPP.getInstances().sendBroadcast(broadMsg);
                            isLogout = false;
                            beginTask();
                        }
                        else
                        {
                            RequestCode requestCode = new RequestCode();
                            requestCode.setRetCode(retcode);
                            broadMsg.setRequestCode(requestCode);
                            CCAPP.getInstances().sendBroadcast(broadMsg);
                        }
                    }
                }
                else
                {
                    //返回信息出错
                    RequestCode requestCode = new RequestCode();
                    requestCode.setErrorCode(NotifyMessage.RET_ERROR_RESPONSE);
                    broadMsg.setRequestCode(requestCode);
                    CCAPP.getInstances().sendBroadcast(broadMsg);
                }
            }

            @Override
            public void onFailure(Call<ResultResponse> call, Throwable t)
            {

                //网络访问异常
                logError(TAG, "login" + " " + "login -> t = " + t.getMessage());
                RequestCode requestCode = new RequestCode();
                requestCode.setErrorCode(NotifyMessage.RET_ERROR_NETWORK);
                broadMsg.setRequestCode(requestCode);
                CCAPP.getInstances().sendBroadcast(broadMsg);
            }
        });
    }

    /**
     * 注销
     */
    public void logout()
    {

        Retrofit retrofit = getRetrofit();
        if (retrofit == null)
        {
            return;
        }
        RequestManager requestManager = retrofit.create(RequestManager.class);
        Call call = requestManager.logOut(serviceConfig.getGuid(), serviceConfig.getCookie(), systemSetting.
                        getUserName(),
                systemSetting.getVndID());

        if (call == null)
        {
            return;
        }
        call.enqueue(new Callback<ResultResponse>()
        {
            private BroadMsg broadMsg = new BroadMsg(NotifyMessage.AUTH_MSG_ON_LOGOUT);

            @Override
            public void onResponse(Call<ResultResponse> call, Response<ResultResponse> response)
            {
                ResultResponse model = response.body();
                if (model == null)
                {
                    //通知上层数据异常处理404或者转换失败
                    RequestCode requestCode = new RequestCode();
                    requestCode.setErrorCode(NotifyMessage.RET_ERROR_RESPONSE);
                    broadMsg.setRequestCode(requestCode);
                    CCAPP.getInstances().sendBroadcast(broadMsg);
                }
                else
                {

                    //200 model不为空 请求成功
                    if (MobileCC.MESSAGE_OK.equals(model.getRetcode()))
                    {
                        serviceConfig.setUvid(0);
                        RequestCode requestCode = new RequestCode();
                        requestCode.setRetCode(model.getRetcode());
                        broadMsg.setRequestCode(requestCode);
                        systemSetting.setIsAudioConnected(false);
                        serviceConfig.setCalledNum("");
                        serviceConfig.clear();
                        CCAPP.getInstances().sendBroadcast(broadMsg);
                    }
                    else
                    {
                        logInfo(TAG, "logout" + " " + "Logout fail ,retcode is："
                                + model.getRetcode() + ",Message:" + model.getMessage());
                        RequestCode requestCode = new RequestCode();
                        requestCode.setRetCode(model.getRetcode());
                        broadMsg.setRequestCode(requestCode);
                        CCAPP.getInstances().sendBroadcast(broadMsg);
                    }
                }
                isLogout = true;

            }

            @Override
            public void onFailure(Call<ResultResponse> call, Throwable t)
            {

                logError(TAG, "logout" + " " + "logout -> t = " + t.getMessage());
                RequestCode requestCode = new RequestCode();
                requestCode.setErrorCode(NotifyMessage.RET_ERROR_NETWORK);
                broadMsg.setRequestCode(requestCode);
                CCAPP.getInstances().sendBroadcast(broadMsg);
            }
        });
    }

    /**
     * 轮询
     */
    public void beginLoop()
    {
        synchronized (LOCKOBJECT)
        {
            if (localRetrofit == null)
            {
                return;
            }
            RequestManager requestManager = localRetrofit.create(RequestManager.class);
            Call call = requestManager.loop(serviceConfig.getGuid(), serviceConfig.getCookie(), systemSetting.getUserName(),
                    systemSetting.getVndID());

            if (call == null)
            {
                return;
            }
            call.enqueue(new Callback<GetEventResponse>()
            {
                private BroadMsg broadMsg = new BroadMsg(NotifyMessage.CALL_MSG_ON_POLL);
                @Override
                public void onResponse(Call<GetEventResponse> call, Response<GetEventResponse> response)
                {
                    GetEventResponse model = response.body();
                    if (model == null)
                    {
                        //通知上层数据异常处理404或者转换失败
                        RequestCode requestCode = new RequestCode();
                        requestCode.setErrorCode(NotifyMessage.RET_ERROR_RESPONSE);
                        broadMsg.setRequestCode(requestCode);
                        CCAPP.getInstances().sendBroadcast(broadMsg);
                    }
                    else
                    {
                        if (MobileCC.MESSAGE_OK.equals(model.getRetcode()) && model.getEvent() != null)
                        {
                            String eventType = model.getEvent().getEventType();
                            logInfo(TAG, "Event： " + eventType);
                            if (NotifyMessage.WECC_WEBM_CALL_CONNECTED.equals(eventType))
                            {
                                parseWebmCallConnected(model);
                            }
                            else if (NotifyMessage.WECC_CHAT_RECEIVEDATA.equals(eventType))
                            {
                                parseChatReceiveData(model);
                            }
                            else if (NotifyMessage.WECC_CHAT_POSTDATA_SUCC.equals(eventType))
                            {
                                parseChatPostDataSucc(model);
                            }
                            else if (NotifyMessage.WECC_WEBM_CALL_DISCONNECTED.equals(eventType))
                            {
                               parseWebmCallDisconnected(model);
                            }
                            else if (NotifyMessage.WECC_WEBM_CALL_QUEUING.equals(eventType))
                            {
                                parseWebmCallQueuing();
                            }
                            else if (NotifyMessage.WECC_WEBM_QUEUE_TIMEOUT.equals(eventType))
                            {
                                parseWebmQueueTimeout();
                            }
                            else if (NotifyMessage.WECC_WEBM_CALL_FAIL.equals(eventType))
                            {
                                parseWebmCallFail(model);
                            }
                            else if (NotifyMessage.WECC_MEETING_PREPARE_JOIN.equals(eventType))
                            {
                                parseMeetingPrepareJoin(model);
                            }
                        }
                    }
                    if (!isLogout)
                    {
                        beginTask();
                    }
                }
                @Override
                public void onFailure(Call<GetEventResponse> call, Throwable t)
                {
                    logError(TAG, "Event -> t = " + t.getMessage());
                    RequestCode requestCode = new RequestCode();
                    requestCode.setErrorCode(NotifyMessage.RET_ERROR_NETWORK);
                    broadMsg.setRequestCode(requestCode);
                    CCAPP.getInstances().sendBroadcast(broadMsg);
                    if (!isLogout)
                    {
                        beginTask();
                    }
                }
            });
        }
    }

    private void parseWebmCallConnected(GetEventResponse model)
    {
        serviceConfig.setIsQueuing(false);
        BroadMsg broadMsg = new BroadMsg(NotifyMessage.CALL_MSG_ON_CONNECTED);
        logInfo(TAG, "==============WECC_WEBM_CALL_CONNECTED==============");
        int mediaType = model.getEvent().getContent().getMediaType();
        long uvid = model.getEvent().getContent().getUvid();
        logInfo(TAG, "mediaType  === " + mediaType);
        RequestCode requestCode = new RequestCode();
        requestCode.setRetCode(model.getRetcode());
        broadMsg.setRequestCode(requestCode);
        if (MEDIA_TYPE_TP == mediaType)
        {
            String protocolType = model.getEvent().getContent().getVcConfInfo().getProtocolType();
            String confAccessNumber = model.getEvent().getContent().getVcConfInfo()
                    .getAccessNumber();
            String confIP = "";
            String confPort = "";
            if ("".equals(systemSetting.getSIPIp())
                    || "".equals(systemSetting.getSIPPort()))
            {
                confIP = model.getEvent().getContent().getVcConfInfo().getServerIp();
                confPort = model.getEvent().getContent().getVcConfInfo().getPort();
            }
            else
            {
                confIP = systemSetting.getSIPIp();
                confPort = systemSetting.getSIPPort();
            }
            if (protocolType != null && !systemSetting.isTransSecurity())
            {
                if (NotifyMessage.TLS.equals(protocolType))
                {
                    MobileCC.getInstance().setTransportSecurity(true, true);
                }
                else
                {
                    MobileCC.getInstance().setTransportSecurity(false, false);
                }
            }
            logInfo(TAG, "get conf info");
            if (makeCall(confAccessNumber, confIP, confPort))
            {
                VideoControl.getIns().deploySessionVideoCaps();
                serviceConfig.setMeidaType(mediaType);
                CCAPP.getInstances().sendBroadcast(broadMsg);
            }
            else
            {
                broadMsg = new BroadMsg(NotifyMessage.CALL_MSG_ON_FAIL);
                CCAPP.getInstances().sendBroadcast(broadMsg);

            }
        }
        else if (MEDIA_TYPE_TEXT == mediaType)
        {
            //MS接入码60011 mediaType：1 文字交谈能力
            logInfo(TAG, "mediaType ==1");
            serviceConfig.setMeidaType(mediaType);

            serviceConfig.setUvid(uvid);

            RequestInfo requestInfo = new RequestInfo();
            requestInfo.setMsg("" + mediaType);
            broadMsg.setRequestInfo(requestInfo);
            CCAPP.getInstances().sendBroadcast(broadMsg);
        }
        else if (MEDIA_TYPE_AUDIO == mediaType)
        {
            //MS接入码60021 mediaType：2 语音能力
            String calledNum = model.getEvent().getContent().getClickToDial();
            serviceConfig.setCalledNum(calledNum);
            serviceConfig.setMeidaType(mediaType);
            logInfo(TAG, "get teller's num：" + calledNum);
            serviceConfig.setUvid(uvid);
            RequestInfo requestInfo = new RequestInfo();
            requestInfo.setMsg("" + mediaType);
            broadMsg.setRequestInfo(requestInfo);
            CCAPP.getInstances().sendBroadcast(broadMsg);
            systemSetting.setIsAudioConnected(true);

            if (MobileCC.AUDIO_CALL.equals(serviceConfig.getCallType()))
            {
                CallManager.getInstance().getVoipConfig().resetData("", "", SystemSetting
                        .getInstance().getSIPIp(), systemSetting.getSIPPort());
                String toNum = serviceConfig.getCalledNum();
                logInfo(TAG, "toNum-> " + toNum);

                if (MobileCC.CALL_ERROR != CallManager.getInstance().makeAnonymousCall(toNum))
                {
                    logInfo(TAG, "Call Success");
                }
                else
                {
                    broadMsg = new BroadMsg(NotifyMessage.CALL_MSG_ON_FAIL);
                    CCAPP.getInstances().sendBroadcast(broadMsg);
                }
            }
            else if (MobileCC.VIDEO_CALL.equals(serviceConfig.getCallType()))
            {
                MobileCC.getInstance().updateToVideo();
            }
        }
    }
    private void parseChatReceiveData(GetEventResponse model)
    {
        BroadMsg broadMsg = new BroadMsg(NotifyMessage.CHAT_MSG_ON_RECEIVE);
        String content = model.getEvent().getContent().getChatContent();
        logInfo(TAG, "Receive content: ***");
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setMsg(content);
        broadMsg.setRequestInfo(requestInfo);
        CCAPP.getInstances().sendBroadcast(broadMsg);
    }
    private void parseChatPostDataSucc(GetEventResponse model)
    {
        BroadMsg broadMsg = new BroadMsg(NotifyMessage.CHAT_MSG_ON_SEND);
        String content = model.getEvent().getContent().getChatContent();
        logInfo(TAG, "Content post success: ***");
        RequestInfo requestInfo = new RequestInfo();
        requestInfo.setMsg(content);
        broadMsg.setRequestInfo(requestInfo);
        RequestCode requestCode = new RequestCode();
        requestCode.setRetCode(model.getRetcode());
        broadMsg.setRequestCode(requestCode);
        CCAPP.getInstances().sendBroadcast(broadMsg);

    }
    private void parseWebmCallDisconnected(GetEventResponse model)
    {
        BroadMsg broadMsg = new BroadMsg(NotifyMessage.CALL_MSG_ON_DISCONNECTED);
        int mediaType = model.getEvent().getContent().getMediaType();
        logInfo(TAG, "disconnected  mediaType ==" + mediaType);
        if (MEDIA_TYPE_TEXT == mediaType)
        {
            serviceConfig.setTextCallId("");
            serviceConfig.setUvid(0);
            RequestInfo requestInfo = new RequestInfo();
            requestInfo.setMsg(MEDIA_TYPE_TEXT + "");
            broadMsg.setRequestInfo(requestInfo);
            logInfo(TAG, "ms text disconnected");
        }
        else if (MEDIA_TYPE_AUDIO == mediaType)
        {
            RequestInfo requestInfo = new RequestInfo();
            requestInfo.setMsg(MEDIA_TYPE_AUDIO + "");
            broadMsg.setRequestInfo(requestInfo);
            serviceConfig.setAudioCallId("");
            serviceConfig.setCalledNum("");
            serviceConfig.setCallType("");
            systemSetting.setIsAudioConnected(false);
            logInfo(TAG, "ms audio disconnected");
        }
        else if (MEDIA_TYPE_TP == mediaType)
        {
            serviceConfig.setTextCallId("");
            serviceConfig.setCameraIndex(1);
            logInfo(TAG, "tp disconnected");
        }
        CCAPP.getInstances().sendBroadcast(broadMsg);
    }

    private void parseWebmCallQueuing()
    {
        serviceConfig.setIsQueuing(true);
        BroadMsg broadMsg = new BroadMsg(NotifyMessage.CALL_MSG_ON_QUEUING);
        CCAPP.getInstances().sendBroadcast(broadMsg);
    }

    private void parseWebmQueueTimeout()
    {
        BroadMsg broadMsg = new BroadMsg(NotifyMessage.CALL_MSG_ON_QUEUE_TIMEOUT);
        CCAPP.getInstances().sendBroadcast(broadMsg);
    }

    private void parseWebmCallFail(GetEventResponse model)
    {
        if (serviceConfig.isQueuing())
        {
            BroadMsg broadMsg = new BroadMsg(NotifyMessage.CALL_MSG_ON_CANCEL_QUEUE);
            CCAPP.getInstances().sendBroadcast(broadMsg);
            serviceConfig.setIsQueuing(false);

        }
        else
        {
            BroadMsg broadMsg = new BroadMsg(NotifyMessage.CALL_MSG_ON_FAIL);

            RequestCode requestCode = new RequestCode();
            requestCode.setRetCode(model.getRetcode());
            broadMsg.setRequestCode(requestCode);
            logInfo(TAG, "fail  retcode is ->" + model.getRetcode());
            CCAPP.getInstances().sendBroadcast(broadMsg);
        }
    }

    private void parseMeetingPrepareJoin(GetEventResponse model)
    {
        logInfo(TAG, "get conf param");
        String confInfo = model.getEvent().getContent().getConfInfo();
        String a[] = confInfo.split("\\|");
        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i < a.length; i++)
        {
            String b[] = a[i].split("=");
            if (a[i].contains("MsNatMap"))
            {
                if (b.length == 1)
                {
                    isNat = false;
                    serviceConfig.setIsNat(false);
                    continue;
                }
                else
                {
                    isNat = true;
                    serviceConfig.setIsNat(true);
                }
            }
            map.put(b[0], b[1]);
        }
        String siteId = map.get("SiteID");
        String serverIp = map.get("MSServerIP");
        String natIp = map.get("MsNatMap");
        String siteUrl = map.get("SiteUrl");
        serviceConfig.setNatIp(natIp);
        serviceConfig.setServerIp(serverIp);
        int confId = Integer.parseInt(map.get("ConfID"));
        String confKey = map.get("ConfPrivilege");
        int userId = Integer.parseInt(map.get("UserID"));
        serviceConfig.setUserId(userId + "");
        String userName = model.getEvent().getUserName();
        String hostKey = map.get("HostKey");
        logInfo(TAG, "siteID:" + "*" + ",confId:"
                + "*" + ",userId:" + userId
                + ",userName:" + "*" + ",hostKey:" + "*"
                + " ,serverIp:" + "*" + ", natIp" + "*");
        serviceConfig.setConfId(confId + "");
        ConferenceInfo conferenceInfo = new ConferenceInfo();
        conferenceInfo.setConfId(confId);
        conferenceInfo.setConfKey(confKey);
        conferenceInfo.setHostKey(hostKey);
        if (isNat)
        {
            conferenceInfo.setServerIp(natIp);
        }
        else
        {
            conferenceInfo.setServerIp(serverIp);
        }
        conferenceInfo.setSiteUrl(siteUrl);
        conferenceInfo.setUserId(userId);
        conferenceInfo.setUserName(userName);
        conferenceInfo.setSiteId(siteId);
        ConferenceMgr.getInstance().setConferenceInfo(conferenceInfo);
        ServiceManager.getInstance().joinConference();
        BroadMsg broadMsg = new BroadMsg(NotifyMessage.CALL_MSG_USER_STATUS);
        systemSetting.setIsMeeting(true);
        CCAPP.getInstances().sendBroadcast(broadMsg);
    }

    /**
     * 获取文字能力
     *
     * @param accessCode accessCode
     * @param caller     caller
     * @param callType   callType
     * @param callData   callData
     * @param varifyCode varifyCode
     */
    public void textConnect(String accessCode, String caller, String callType, String callData, String varifyCode)
    {

        Retrofit retrofit = getRetrofit();
        if (retrofit == null)
        {
            return;
        }
        RequestManager requestManager = retrofit.create(RequestManager.class);
        Call call = null;

        //mediaType是1，TP的新环境由于没有文字交谈功能，这里换成22 ,
        if ((MobileCC.SERVER_TP + "").equals(callType))
        {
            call = requestManager.getConnect(serviceConfig.getGuid(), serviceConfig.getCookie(), caller,
                    systemSetting.getVndID(), new GetConnectRequest(MEDIA_TYPE_TP, caller,
                            accessCode, callData, -1, varifyCode));
        }
        else if ((MobileCC.SERVER_MS + "").equals(callType))
        {
            call = requestManager.getConnect(serviceConfig.getGuid(), serviceConfig.getCookie(), caller, systemSetting
                    .getVndID(), new GetConnectRequest(1, caller, accessCode, callData,
                    serviceConfig.getUvid(), varifyCode));
            logInfo(TAG, "Uvid ===== " + serviceConfig.getUvid());
        }
        else
        {
            return;
        }

        if (call == null)
        {
            return;
        }
        call.enqueue(new Callback<ResultResponse>()
        {
            private BroadMsg broadMsg = new BroadMsg(NotifyMessage.CALL_MSG_ON_CONNECT);

            @Override
            public void onResponse(Call<ResultResponse> call, Response<ResultResponse> response)
            {
                ResultResponse model = response.body();
                if (model == null)
                {
                    //通知上层数据异常处理404或者转换失败
                    RequestCode requestCode = new RequestCode();
                    requestCode.setErrorCode(NotifyMessage.RET_ERROR_RESPONSE);
                    broadMsg.setRequestCode(requestCode);
                    broadMsg.setType(MEDIA_TYPE_TEXT + "");
                    CCAPP.getInstances().sendBroadcast(broadMsg);
                }
                else
                {
                    //200 model不为空 请求成功
                    if (MobileCC.MESSAGE_OK.equals(model.getRetcode()))
                    {
                        logInfo(TAG, "Retcode:" + model.getRetcode() + "\n Message:" + model.getMessage()
                                + "\n webChat Result:" + model.getResult());
                        String resultMsg = model.getResult();
                        serviceConfig.setTextCallId(resultMsg);
                    }
                    else
                    {
                        logInfo(TAG, "Text ability error ->Retcode:" + model.getRetcode() + "\n Message:"
                                + model.getMessage() + "\n webChat Result:" + model.getResult());
                    }
                    RequestCode requestCode = new RequestCode();
                    requestCode.setRetCode(model.getRetcode());
                    broadMsg.setRequestCode(requestCode);
                    broadMsg.setType(MEDIA_TYPE_TEXT + "");
                    CCAPP.getInstances().sendBroadcast(broadMsg);
                }
            }

            @Override
            public void onFailure(Call<ResultResponse> call, Throwable t)
            {
                logError(TAG, "Text ablitity ->  t = " + t.getMessage());
                RequestCode requestCode = new RequestCode();
                requestCode.setErrorCode(NotifyMessage.RET_ERROR_NETWORK);
                broadMsg.setRequestCode(requestCode);
                broadMsg.setType(MEDIA_TYPE_TEXT + "");
                CCAPP.getInstances().sendBroadcast(broadMsg);

            }
        });

    }

    /**
     * 发送消息
     *
     * @param content content
     */
    public void sendMsg(String content)
    {
        Retrofit retrofit = getRetrofit();
        if (retrofit == null)
        {
            return;
        }
        RequestManager requestManager = retrofit.create(RequestManager.class);
        Call call = requestManager.sendMessage(serviceConfig.getGuid(),  serviceConfig.getCookie(),
                systemSetting.getUserName(),
                systemSetting.getVndID(), new SendMsgRequest(serviceConfig.getTextCallId(),
                        content));
        if (call == null)
        {
            return;
        }
        call.enqueue(new Callback<SendMsgResponse>()
        {
            private BroadMsg broadMsg = new BroadMsg(NotifyMessage.CHAT_MSG_ON_SEND);

            @Override
            public void onResponse(Call<SendMsgResponse> call, Response<SendMsgResponse> response)
            {
                SendMsgResponse model = response.body();
                if (model == null)
                {
                    //通知上层数据异常处理404或者转换失败
                    RequestCode requestCode = new RequestCode();
                    requestCode.setErrorCode(NotifyMessage.RET_ERROR_RESPONSE);
                    broadMsg.setRequestCode(requestCode);
                    CCAPP.getInstances().sendBroadcast(broadMsg);
                }
                else
                {
                    if (MobileCC.MESSAGE_OK.equals(model.getRetcode()))
                    {
                        logInfo(TAG, "content send success");
                    }
                    else
                    {
                        logInfo(TAG, "content send fail");

                        RequestCode requestCode = new RequestCode();
                        requestCode.setRetCode(model.getRetcode());
                        broadMsg.setRequestCode(requestCode);
                        CCAPP.getInstances().sendBroadcast(broadMsg);
                    }
                }
            }

            @Override
            public void onFailure(Call<SendMsgResponse> call, Throwable t)
            {
                logError(TAG, "send msg-> t = " + t.getMessage());
                RequestCode requestCode = new RequestCode();
                requestCode.setErrorCode(NotifyMessage.RET_ERROR_NETWORK);
                broadMsg.setRequestCode(requestCode);
                CCAPP.getInstances().sendBroadcast(broadMsg);
            }
        });
    }


    /**
     * 获取排队信息
     */
    public void getCallQueueInfo()
    {
        Retrofit retrofit = getRetrofit();
        if (retrofit == null)
        {
            return;
        }
        RequestManager requestManager = retrofit.create(RequestManager.class);
        Call call = null;
        if (MobileCC.AUDIO_CALL.equals(serviceConfig.getCallType())
                || MobileCC.VIDEO_CALL.equals(serviceConfig.getCallType()))
        {
            call = requestManager.getQueueInfo(serviceConfig.getGuid(), serviceConfig.getCookie(), systemSetting
                    .getUserName(), systemSetting.getVndID(), serviceConfig.getAudioCallId());
        }
        else
        {
            call = requestManager.getQueueInfo(serviceConfig.getGuid(), serviceConfig.getCookie(), systemSetting
                    .getUserName(), systemSetting.getVndID(), serviceConfig.getTextCallId());
        }

        if (call == null)
        {
            return;
        }
        call.enqueue(new Callback<QueueInfoResponse>()
        {

            private BroadMsg broadMsg = new BroadMsg(NotifyMessage.CALL_MSG_ON_QUEUE_INFO);

            @Override
            public void onResponse(Call<QueueInfoResponse> call, Response<QueueInfoResponse> response)
            {
                logInfo(TAG, response.headers() + "");
                QueueInfoResponse model = response.body();

                if (model == null)
                {
                    //通知上层数据异常处理404或者转换失败
                    RequestCode requestCode = new RequestCode();
                    requestCode.setErrorCode(NotifyMessage.RET_ERROR_RESPONSE);
                    broadMsg.setRequestCode(requestCode);
                    CCAPP.getInstances().sendBroadcast(broadMsg);
                }
                else
                {
                    //200 model不为空 请求成功
                    if (MobileCC.MESSAGE_OK.equals(model.getRetcode()))
                    {
                        //正在排队
                        long position = model.getResult().getPosition();
                        int onlineAgentNum = model.getResult().getOnlineAgentNum();
                        long longestWaitTime = model.getResult().getLongestWaitTime();

                        QueueInfo queueInfo = new QueueInfo();
                        queueInfo.setPosition(position);
                        queueInfo.setOnlineAgentNum(onlineAgentNum);
                        queueInfo.setLongestWaitTime(longestWaitTime);
                        broadMsg.setQueueInfo(queueInfo);

                        RequestCode requestCode = new RequestCode();
                        requestCode.setRetCode(model.getRetcode());
                        broadMsg.setRequestCode(requestCode);
                        CCAPP.getInstances().sendBroadcast(broadMsg);
                    }
                    else
                    {
                        //非排队状态
                        logInfo(TAG, "Not queuing");
                        RequestCode requestCode = new RequestCode();
                        requestCode.setRetCode(model.getRetcode());
                        broadMsg.setRequestCode(requestCode);
                        CCAPP.getInstances().sendBroadcast(broadMsg);
                    }
                }
            }

            @Override
            public void onFailure(Call<QueueInfoResponse> call, Throwable t)
            {

                logError(TAG, "get queue info -> t = " + t.getMessage());
                RequestCode requestCode = new RequestCode();
                requestCode.setErrorCode(NotifyMessage.RET_ERROR_NETWORK);
                broadMsg.setRequestCode(requestCode);
                CCAPP.getInstances().sendBroadcast(broadMsg);
            }
        });
    }

    /**
     * 呼叫
     *
     * @param confAccessNumber confAccessNumber
     * @param scAddress        scAddress
     * @param port             port
     * @return boolean
     */
    public boolean makeCall(String confAccessNumber, String scAddress, String port)
    {
        if ((confAccessNumber != null) && (scAddress != null))
        {
            CallManager.getInstance().getVoipConfig().resetData("", "", scAddress, port);
            int status = CallManager.getInstance().startAnonymousVideoCall(confAccessNumber);
            logInfo(TAG, "videoCall()");
            if (MobileCC.CALL_ERROR != status)
            {
                return true;
            }
            else
            {
                return false;
            }
        }
        return false;
    }

    /**
     * 语音连接
     *
     * @param accessCode accessCode
     * @param callData callData
     */
    public void msAudioConnect(String accessCode, String callData)
    {

        Retrofit retrofit = getRetrofit();

        if (retrofit == null)
        {
            return;
        }
        RequestManager requestManager = retrofit.create(RequestManager.class);
        Call call = requestManager.getMSConnect(serviceConfig.getGuid(), serviceConfig.getCookie(), systemSetting
                .getUserName(), systemSetting.getVndID(), new GetConnectRequest(2,
                systemSetting.getUserName(), accessCode, callData,
                serviceConfig.getUvid(), systemSetting.getVerifyCode()));
        logInfo(TAG, "Uvid ===== " + serviceConfig.getUvid());

        if (call == null)
        {
            return;
        }
        call.enqueue(new Callback<ResultResponse>()
        {

            private BroadMsg broadMsg = new BroadMsg(NotifyMessage.CALL_MSG_ON_CONNECT);

            @Override
            public void onResponse(Call<ResultResponse> call, Response<ResultResponse> response)
            {
                ResultResponse model = response.body();
                if (model == null)
                {
                    //通知上层数据异常处理404或者转换失败
                    RequestCode requestCode = new RequestCode();
                    requestCode.setErrorCode(NotifyMessage.RET_ERROR_RESPONSE);
                    broadMsg.setRequestCode(requestCode);
                    broadMsg.setType(MEDIA_TYPE_AUDIO + "");
                    CCAPP.getInstances().sendBroadcast(broadMsg);
                }
                else
                {
                    //200 model不为空 请求成功
                    if (MobileCC.MESSAGE_OK.equals(model.getRetcode()))
                    {
                        logInfo(TAG, "Retcode:" + model.getRetcode() + "\n Message:"
                                + model.getMessage() + "\n Event:" + model.getResult());
                        String resultMsg = model.getResult();
                        serviceConfig.setAudioCallId(resultMsg);
                    }
                    else
                    {
                        logInfo(TAG, "Audio ability error ->Retcode:" + model.getRetcode()
                                + "\n Message:" + model.getMessage() + "\n  Result:" + model.getResult());
                    }

                    RequestCode requestCode = new RequestCode();
                    requestCode.setRetCode(model.getRetcode());
                    broadMsg.setRequestCode(requestCode);
                    broadMsg.setType(MEDIA_TYPE_AUDIO + "");
                    CCAPP.getInstances().sendBroadcast(broadMsg);
                }
            }

            @Override
            public void onFailure(Call<ResultResponse> call, Throwable t)
            {
                logError(TAG, "t = " + t.getMessage());
                RequestCode requestCode = new RequestCode();
                requestCode.setErrorCode(NotifyMessage.RET_ERROR_NETWORK);
                broadMsg.setRequestCode(requestCode);
                broadMsg.setType(MEDIA_TYPE_AUDIO + "");
                CCAPP.getInstances().sendBroadcast(broadMsg);

            }
        });
    }

    /**
     * 升级为视频
     *
     * @param callId callId
     */
    public void updateVideo(String callId)
    {


        logInfo(TAG, "============updateVideo============");
        Retrofit retrofit = getRetrofit();

        if (retrofit == null)
        {
            return;
        }
        RequestManager requestManager = retrofit.create(RequestManager.class);
        Call call = requestManager.updateVideo(serviceConfig.getGuid(), serviceConfig.getCookie(), systemSetting
                .getUserName(), systemSetting.getVndID(), callId);

        if (call == null)
        {
            return;
        }
        call.enqueue(new Callback<ResultResponse>()
        {

            private BroadMsg broadMsg = new BroadMsg(NotifyMessage.CALL_MSG_ON_APPLY_MEETING);

            @Override
            public void onResponse(Call<ResultResponse> call, Response<ResultResponse> response)
            {
                ResultResponse model = response.body();
                if (model == null)
                {
                    //通知上层数据异常处理404或者转换失败
                    RequestCode requestCode = new RequestCode();
                    requestCode.setErrorCode(NotifyMessage.RET_ERROR_RESPONSE);
                    broadMsg.setRequestCode(requestCode);
                    CCAPP.getInstances().sendBroadcast(broadMsg);
                }
                else
                {
                    //200
                    if (MobileCC.MESSAGE_OK.equals(model.getRetcode()))
                    {
                        logInfo(TAG, "apply Meeting sent");
                    }
                    else
                    {
                        logInfo(TAG, "apply meeting fail");
                    }
                    RequestCode requestCode = new RequestCode();
                    requestCode.setRetCode(model.getRetcode());
                    broadMsg.setRequestCode(requestCode);
                    CCAPP.getInstances().sendBroadcast(broadMsg);
                }
            }

            @Override
            public void onFailure(Call<ResultResponse> call, Throwable t)
            {
                logError(TAG, "t = " + t.getMessage());
                RequestCode requestCode = new RequestCode();
                requestCode.setErrorCode(NotifyMessage.RET_ERROR_NETWORK);
                broadMsg.setRequestCode(requestCode);
                CCAPP.getInstances().sendBroadcast(broadMsg);

            }
        });
    }


    /**
     * 结束会议
     *
     * @param confId confId
     */
    public void stopConf(String confId)
    {
        Retrofit retrofit = getRetrofit();
        if (retrofit == null)
        {
            return;
        }
        RequestManager requestManager = retrofit.create(RequestManager.class);
        Call call = requestManager.stopConf(serviceConfig.getGuid(), serviceConfig.getCookie(), systemSetting
                .getUserName(), systemSetting.getVndID(), confId);
        logInfo(TAG, "stopConf");
        if (call == null)
        {
            return;
        }
        call.enqueue(new Callback<StopConfResponse>()
        {
            private BroadMsg broadMsg = new BroadMsg(NotifyMessage.CALL_MSG_ON_STOP_MEETING);

            @Override
            public void onResponse(Call<StopConfResponse> call, Response<StopConfResponse> response)
            {
                StopConfResponse model = response.body();
                if (model == null)
                {
                    //通知上层数据异常处理404或者转换失败
                    RequestCode requestCode = new RequestCode();
                    requestCode.setErrorCode(NotifyMessage.RET_ERROR_RESPONSE);
                    broadMsg.setRequestCode(requestCode);
                    CCAPP.getInstances().sendBroadcast(broadMsg);
                }
                else
                {
                    //200 model不为空 请求成功
                    if (MobileCC.MESSAGE_OK.equals(model.getRetcode()))
                    {
                        //退出会议成功
                        logInfo(TAG, "stop Conf success");
                    }
                    else
                    {
                        logInfo(TAG, "stop Conf fail" + model.getRetcode());
                    }
                    RequestCode requestCode = new RequestCode();
                    requestCode.setRetCode(model.getRetcode());
                    broadMsg.setRequestCode(requestCode);
                    CCAPP.getInstances().sendBroadcast(broadMsg);
                }
            }

            @Override
            public void onFailure(Call<StopConfResponse> call, Throwable t)
            {
                logError(TAG, "Stop Conf t = " + t.getMessage());
                RequestCode requestCode = new RequestCode();
                requestCode.setErrorCode(NotifyMessage.RET_ERROR_NETWORK);
                broadMsg.setRequestCode(requestCode);
                CCAPP.getInstances().sendBroadcast(broadMsg);

            }
        });
    }

    /**
     * 释放呼叫
     */
    public void dropCall()
    {
        logInfo(TAG, "dropcall()");
        Retrofit retrofit = getRetrofit();
        if (retrofit == null)
        {
            logInfo(TAG, "dropcall  retrofit == null");
            return;
        }

        RequestManager requestManager = retrofit.create(RequestManager.class);
        Call call;
        if ((MobileCC.SERVER_MS + "").equals(serviceConfig.getSipServerType()))
        {
            if (serviceConfig.isQueuing())
            {
                if (MobileCC.AUDIO_CALL.equals(serviceConfig.getCallType())
                        || MobileCC.VIDEO_CALL.equals(serviceConfig.getCallType()))
                {

                    //如果是正当排队，取消排队的是用text的callid
                    call = requestManager.dropCall(serviceConfig.getGuid(), serviceConfig.getCookie(), systemSetting
                                    .getUserName(), systemSetting.getVndID(),
                            serviceConfig.getAudioCallId());
                }
                else
                {
                    call = requestManager.dropCall(serviceConfig.getGuid(), serviceConfig.getCookie(), systemSetting
                                    .getUserName(), systemSetting.getVndID(),
                            serviceConfig.getTextCallId());
                }
            }
            else
            {
                call = requestManager.dropCall(serviceConfig.getGuid(), serviceConfig.getCookie(), systemSetting
                                .getUserName(), systemSetting.getVndID(),
                        serviceConfig.getAudioCallId());
            }
        }
        else
        {
            call = requestManager.dropCall(serviceConfig.getGuid(), serviceConfig.getCookie(), systemSetting
                            .getUserName(), systemSetting.getVndID(),
                    serviceConfig.getTextCallId());
        }
        if (call == null)
        {
            logInfo(TAG, "dropcall  call == null");
            return;
        }

        call.enqueue(new Callback<DropCallResponse>()
        {

            private BroadMsg broadMsg = new BroadMsg(NotifyMessage.CALL_MSG_ON_DROPCALL);

            @Override
            public void onResponse(Call<DropCallResponse> call, Response<DropCallResponse> response)
            {
                DropCallResponse model = response.body();
                if (model == null)
                {
                    //通知上层数据异常处理404或者转换失败
                    RequestCode requestCode = new RequestCode();
                    requestCode.setErrorCode(NotifyMessage.RET_ERROR_RESPONSE);
                    broadMsg.setRequestCode(requestCode);
                    CCAPP.getInstances().sendBroadcast(broadMsg);
                }
                else
                {
                    //200 model不为空 请求成功
                    logInfo(TAG, "dropcall retcode is :" + model.getRetcode());
                    RequestCode requestCode = new RequestCode();
                    requestCode.setRetCode(model.getRetcode());
                    broadMsg.setRequestCode(requestCode);
                    CCAPP.getInstances().sendBroadcast(broadMsg);
                }
            }

            @Override
            public void onFailure(Call<DropCallResponse> call, Throwable t)
            {
                logError(TAG, "drop call -> t = " + t.getMessage());
                RequestCode requestCode = new RequestCode();
                requestCode.setErrorCode(NotifyMessage.RET_ERROR_NETWORK);
                broadMsg.setRequestCode(requestCode);
                CCAPP.getInstances().sendBroadcast(broadMsg);

            }
        });
    }

    /**
     * 释放文字连接
     */
    public void dropTextCall()
    {
        Retrofit retrofit = getRetrofit();
        if (retrofit == null)
        {
            return;
        }
        RequestManager requestManager = retrofit.create(RequestManager.class);
        Call call = requestManager.dropCall(serviceConfig.getGuid(), serviceConfig.getCookie(), systemSetting
                .getUserName(), systemSetting.getVndID(), serviceConfig.getTextCallId());
        if (call == null)
        {
            return;
        }
        call.enqueue(new Callback<DropCallResponse>()
        {
            private BroadMsg broadMsg = new BroadMsg(NotifyMessage.CALL_MSG_ON_DROPCALL);

            @Override
            public void onResponse(Call<DropCallResponse> call, Response<DropCallResponse> response)
            {
                DropCallResponse model = response.body();
                if (model == null)
                {
                    //通知上层数据异常处理404或者转换失败
                    RequestCode requestCode = new RequestCode();
                    requestCode.setErrorCode(NotifyMessage.RET_ERROR_RESPONSE);
                    broadMsg.setRequestCode(requestCode);
                    CCAPP.getInstances().sendBroadcast(broadMsg);
                }
                else
                {
                    //200 model不为空 请求成功
                    logInfo(TAG, "dropTextCall() retcode is :" + model.getRetcode());
                    RequestCode requestCode = new RequestCode();
                    requestCode.setRetCode(model.getRetcode());
                    broadMsg.setRequestCode(requestCode);
                    CCAPP.getInstances().sendBroadcast(broadMsg);
                }

            }

            @Override
            public void onFailure(Call<DropCallResponse> call, Throwable t)
            {

                logError(TAG, "dropTextCall()  -> t = " + t.getMessage());
                RequestCode requestCode = new RequestCode();
                requestCode.setErrorCode(NotifyMessage.RET_ERROR_NETWORK);
                broadMsg.setRequestCode(requestCode);
                CCAPP.getInstances().sendBroadcast(broadMsg);
            }
        });
    }

    private Retrofit getRetrofit()
    {

            if (systemSetting.isHTTPS())
            {
                client = createOkhttp();
                localRetrofit = new Retrofit.Builder()
                        .baseUrl(systemSetting.getTransSecurity() + systemSetting.getServerIp()
                                + ":" + systemSetting.getServerPort())
                        .addConverterFactory(GsonConverterFactory.create()).client(client)
                        .build();
                logInfo(TAG, "path " + systemSetting.getTransSecurity() + systemSetting.getServerIp()
                        + ":" + systemSetting.getServerPort());
            }
            else
            {
                localRetrofit = new Retrofit.Builder()
                        .baseUrl(systemSetting.getTransSecurity() + systemSetting.getServerIp()
                                + ":" + systemSetting.getServerPort())
                        .addConverterFactory(GsonConverterFactory.create())
                        .build();
            }
            return localRetrofit;
    }

    /**
     * 获取SSLSocketFactory
     *
     * @param certificates certificates
     * @return SSLSocketFactory
     */
    private  SSLSocketFactory getSocketFactory(List<InputStream> certificates)
    {
        try
        {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null);
            try
            {
                for (int i = 0, size = certificates.size(); i < size; i++)
                {
                    InputStream certificate = certificates.get(i);
                    String certificateAlias = Integer.toString(i);
                    keyStore.setCertificateEntry(certificateAlias, certificateFactory.generateCertificate(certificate));
                    if (certificate != null)
                    {
                        certificate.close();
                    }
                }
            }
            catch (IOException e)
            {
                logInfo(TAG, "e: " + e);
            }

            TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            return new TLSSocketFactory(trustManagerFactory.getTrustManagers());
        }
        catch (NoSuchAlgorithmException e)
        {
            logInfo(TAG, "" + e.getMessage());
        }
        catch (KeyStoreException e)
        {
            logInfo(TAG, "" + e.getMessage());
        }
        catch (KeyManagementException e)
        {
            logInfo(TAG, "" + e.getMessage());
        }
        catch (CertificateException e)
        {
            logInfo(TAG, "" + e.getMessage());
        }
        catch (IOException e)
        {
            logInfo(TAG, "" + e.getMessage());
        }

        return null;
    }

    /**
     * 获取OkHttpClient
     *
     * @return OkHttpClient
     */
    private OkHttpClient createOkhttp()
    {
        if (systemSetting.isValidateServerCertificate())
        {
            // 添加证书
            List<InputStream> certificates = new ArrayList<InputStream>();

            List<byte[]> certsData = ServiceConfig.getCertificatesData();

            // 将字节数组转为数组输入流
            if (certsData != null && !certsData.isEmpty())
            {
                for (byte[] bytes : certsData)
                {
                    certificates.add(new ByteArrayInputStream(bytes));
                }
            }

            sslSocketFactory = getSocketFactory(certificates);

            if (sslSocketFactory != null)
            {
                if (systemSetting.isValidateDomain())
                {
                    client = client.newBuilder().sslSocketFactory(sslSocketFactory).
                            connectTimeout(20, TimeUnit.SECONDS).
                            hostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.STRICT_HOSTNAME_VERIFIER)
                            .build();
                }
                else
                {
                    client = client.newBuilder().sslSocketFactory(sslSocketFactory).
                            connectTimeout(20, TimeUnit.SECONDS).
                            hostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
                            .build();
                }
            }
        }
        else
        {
            try
            {
                if (client != null)
                {
                    client = client.newBuilder().sslSocketFactory(new TLSSocketFactory(trustManager)).
                            connectTimeout(20, TimeUnit.SECONDS)
                            .hostnameVerifier(org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
                            .build();
                }
            }
        catch (NoSuchAlgorithmException e)
        {
            logInfo(TAG, "noSuchAlgorithm exception");
        }
        catch (KeyManagementException e)
        {
            logInfo(TAG, "keyManagement exception");
        }
        }

        return client;
    }

    /**
     * 获取验证码
     */
    public void getVerifyCode()
    {

        Retrofit retrofit = getRetrofit();

        if (retrofit == null)
        {
            return;
        }
        RequestManager requestManager = retrofit.create(RequestManager.class);
        Call call = requestManager.getVerifycode(serviceConfig.getGuid(), serviceConfig.getCookie(),
                systemSetting.getVndID(), systemSetting
                .getUserName());
        if (call == null)
        {
            return;
        }

        call.enqueue(new Callback<ResultResponse>()
        {

            private BroadMsg broadMsg = new BroadMsg(NotifyMessage.CALL_MSG_ON_VERIFYCODE);

            @Override
            public void onResponse(Call<ResultResponse> call, Response<ResultResponse> response)
            {

                ResultResponse model = response.body();

                if (model == null)
                {
                    //通知上层数据异常处理404或者转换失败
                    RequestCode requestCode = new RequestCode();
                    requestCode.setErrorCode(NotifyMessage.RET_ERROR_RESPONSE);
                    broadMsg.setRequestCode(requestCode);
                    CCAPP.getInstances().sendBroadcast(broadMsg);
                }
                else
                {
                    //200 model不为空 请求成功
                    if (MobileCC.MESSAGE_OK.equals(model.getRetcode()))
                    {
                        //获取验证码数据成功
                        logInfo(TAG, "get varifycode success");

                        RequestCode requestCode = new RequestCode();
                        requestCode.setRetCode(model.getRetcode());
                        broadMsg.setRequestCode(requestCode);

                        RequestInfo requestInfo = new RequestInfo();
                        requestInfo.setMsg(model.getResult());
                        broadMsg.setRequestInfo(requestInfo);
                        CCAPP.getInstances().sendBroadcast(broadMsg);
                    }
                    else
                    {
                        //获取验证码失败
                        logInfo(TAG, "getVerifyCode fail" + model.getRetcode());
                        RequestCode requestCode = new RequestCode();
                        requestCode.setRetCode(model.getRetcode());
                        broadMsg.setRequestCode(requestCode);
                        CCAPP.getInstances().sendBroadcast(broadMsg);
                    }
                }
            }

            @Override
            public void onFailure(Call<ResultResponse> call, Throwable t)
            {

                logError(TAG, "get verifycode fail -> t = " + t.getMessage());
                RequestCode requestCode = new RequestCode();
                requestCode.setErrorCode(NotifyMessage.RET_ERROR_NETWORK);
                broadMsg.setRequestCode(requestCode);
                CCAPP.getInstances().sendBroadcast(broadMsg);
            }
        });

    }

    private void logInfo(String tagName, String content)
    {
        LogUtil.d(tagName, " " + content);
    }

    private void logError(String tagName, String content)
    {
        LogUtil.e(tagName, " " + content);
    }

    private static class MyX509TrustManager implements X509TrustManager
    {
        private static final X509Certificate[] X509_CERTIFICATES = new X509Certificate[0];
        private X509TrustManager defaultTrustManager = null;

        public MyX509TrustManager()
        {
        }

        public MyX509TrustManager(final X509TrustManager trustManager)
        {
            if (trustManager == null)
            {
                throw new IllegalArgumentException("Trust manager may not be null");
            }
            this.defaultTrustManager = trustManager;
        }

        public X509Certificate[] getAcceptedIssuers()
        {
            X509Certificate[] x509Certificates = new X509Certificate[0];
            return x509Certificates;
        }

        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException
        {
            if (defaultTrustManager != null)
            {
                defaultTrustManager.checkServerTrusted(chain, authType);
            }
        }

        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException
        {
            if (defaultTrustManager != null)
            {
                defaultTrustManager.checkClientTrusted(chain, authType);
            }
        }
    }
}
