package com.huawei.esdk.cc.service;


import com.huawei.esdk.cc.service.conference.ConferenceUserInfo;
import com.huawei.esdk.cc.utils.LogUtil;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created on 2016/2/22.
 */
public final class ServiceConfig
{
    private static final String TAG = "ServiceConfig";
    private static final Object LOCKOBJECT = new Object();
    // 证书数据
    private static List<byte[]> certificatesData = new ArrayList<byte[]>();
    private static ServiceConfig ins;
    private String guid = "";
    private String cookie = "";
    private String callId = "";
    private String audioCallId = "";
    private String textCallId = "";
    private int meidaType = 0;
    private int anonyCallMode = 1;
    private ConferenceUserInfo attachUser;
    private Map<String, ConferenceUserInfo> userMap = new HashMap<String, ConferenceUserInfo>();
    private List<String> userids = new ArrayList<String>();
    private boolean isNat = false;
    private String serverIp;
    private String natIp;

    /**
     * userId
     **/
    private String userId = "";
    private String vtaDeviceId;
    /**
     * 会议ID
     **/
    private String confId = "";
    private String calledNum = "";
    private long uvid = -1;
    private String callType = "";
    /**
     * 记录是否是排队状态
     **/
    private boolean isQueuing = false;
    private String sipServerType = "";
    private int cameraIndex = 1;


    private ServiceConfig()
    {
    }

    /**
     * 获取实例
     *
     * @return ServiceConfig
     */
    public static ServiceConfig getInstance()
    {
        synchronized (LOCKOBJECT)
        {
            if (ins == null)
            {
                ins = new ServiceConfig();
            }
            return ins;
        }

    }

    public String getSipServerType()
    {
        return sipServerType;
    }

    public void setSipServerType(String sipServerType)
    {
        this.sipServerType = sipServerType;
    }

    private static void releaseIns()
    {
        synchronized (LOCKOBJECT)
        {
            ins = null;
        }

    }

    /**
     * 清除信息
     */
    public void clear()
    {
        releaseIns();
    }

    public String getGuid()
    {
        return guid;
    }

    public void setGuid(String guid)
    {
        this.guid = guid;
    }

    public String getCallId()
    {
        return callId;
    }

    public void setCallId(String callId)
    {
        this.callId = callId;
    }

    public int getAnonyCallMode()
    {
        return anonyCallMode;
    }

    public void setAnonyCallMode(int anonyCallMode)
    {
        this.anonyCallMode = anonyCallMode;
    }

    public String getCurrDeviceId()
    {
        return getSelfUserInfo().getCurrDeviceId();
    }

    public ConferenceUserInfo getAttachUser()
    {
        return attachUser;
    }

    public void setAttachUser(ConferenceUserInfo attachUser)
    {
        this.attachUser = attachUser;
    }

    public ConferenceUserInfo getSelfUserInfo()
    {
        return userMap.get(getUserId());
    }

    public String getUserId()
    {
        return userId;
    }

    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    /**
     * 添加与会人员
     *
     * @param conferenceUserInfo conferenceUserInfo
     * @return Map
     */
    public Map<String, ConferenceUserInfo> addUser(ConferenceUserInfo conferenceUserInfo)
    {
        String userId = conferenceUserInfo.getUserId();
        if (!userMap.containsKey(userId))
        {
            userids.add(userId);
        }

        userMap.put(conferenceUserInfo.getUserId(), conferenceUserInfo);
        return userMap;
    }

    /**
     * 移除与会人员
     *
     * @param userId userId
     */
    public void removeUser(String userId)
    {
        userMap.remove(userId);
        userids.remove(userId);
    }

    /**
     * 获取与会人员信息
     *
     * @return List
     */
    public List<ConferenceUserInfo> getUserInfos()
    {
        List<ConferenceUserInfo> conferenceUserInfos = new ArrayList<ConferenceUserInfo>();
        ConferenceUserInfo conferenceUserInfo;
        for (String userId2 : userids)
        {
            conferenceUserInfo = userMap.get(userId2);
            conferenceUserInfos.add(conferenceUserInfo);
        }
        return conferenceUserInfos;
    }

    /**
     * 获取VTA用户信息
     *
     * @return List
     */
    public List<ConferenceUserInfo> getVTAUserInfos()
    {
        List<ConferenceUserInfo> conferenceUserInfos = new ArrayList<ConferenceUserInfo>();

        for (ConferenceUserInfo conferenceUserInfo : getUserInfos())
        {
            if (!conferenceUserInfo.getUserId().equals(getUserId()))
            {
                conferenceUserInfos.add(conferenceUserInfo);
            }
        }

        return conferenceUserInfos;
    }

    /**
     * 获取用户信息
     *
     * @param userId userId
     * @return ConferenceUserInfo
     */
    public ConferenceUserInfo getUserInfo(String userId)
    {
        return userMap.get(userId);
    }

    public String getVtaDeviceId()
    {
        return vtaDeviceId;
    }

    public void setVtaDeviceId(String vtaDeviceId)
    {
        this.vtaDeviceId = vtaDeviceId;
    }

    public String getConfId()
    {
        return confId;
    }

    public void setConfId(String confId)
    {
        this.confId = confId;
    }

    public String getCalledNum()
    {
        return calledNum;
    }

    public void setCalledNum(String calledNum)
    {
        this.calledNum = calledNum;
    }

    public long getUvid()
    {
        return uvid;
    }

    public void setUvid(long uvid)
    {
        this.uvid = uvid;
    }

    public boolean isQueuing()
    {
        return isQueuing;
    }

    public void setIsQueuing(boolean isQueuing)
    {
        this.isQueuing = isQueuing;
    }

    public String getCallType()
    {
        return callType;
    }

    public void setCallType(String callType)
    {
        this.callType = callType;
    }


    public String getAudioCallId()
    {
        return audioCallId;
    }

    public void setAudioCallId(String audioCallId)
    {
        this.audioCallId = audioCallId;
    }

    public String getTextCallId()
    {
        return textCallId;
    }

    public void setTextCallId(String textCallId)
    {
        this.textCallId = textCallId;
    }

    public int getMeidaType()
    {
        return meidaType;
    }

    public void setMeidaType(int meidaType)
    {
        this.meidaType = meidaType;
    }

    public int getCameraIndex()
    {
        return cameraIndex;
    }

    public void setCameraIndex(int cameraIndex)
    {
        this.cameraIndex = cameraIndex;
    }

    public boolean isNat()
    {
        return isNat;
    }

    public void setIsNat(boolean isNat)
    {
        this.isNat = isNat;
    }

    public String getNatIp()
    {
        return natIp;
    }

    public void setNatIp(String natIp)
    {
        this.natIp = natIp;
    }

    public String getServerIp()
    {
        return serverIp;
    }

    public void setServerIp(String serverIp)
    {
        this.serverIp = serverIp;
    }
    /**
     * 添加https证书
     * @param inputStream inputStream
     */
    public static synchronized void addCertificate(InputStream inputStream) {
        synchronized (LOCKOBJECT)
        {
            if (inputStream != null)
            {

                try
                {
                    int ava = 0; // 数据当次可读长度
                    int len = 0; // 数据总长度
                    ArrayList<byte[]> data = new ArrayList<byte[]>();
                    while ((ava = inputStream.available()) > 0)
                    {
                        byte[] buffer = new byte[ava];
                        inputStream.read(buffer);
                        data.add(buffer);
                        len += ava;
                    }

                    byte[] buff = new byte[len];
                    int dstPos = 0;
                    for (byte[] bytes : data)
                    {
                        int length = bytes.length;
                        System.arraycopy(bytes, 0, buff, dstPos, length);
                        dstPos += length;
                    }

                    certificatesData.add(buff);
                } catch (IOException e)
                {
                    LogUtil.e("CertificateConfig", "io excption");
                }

            }
        }
    }

    public String getCookie()
    {
        return cookie;
    }

    public void setCookie(String cookie)
    {
        this.cookie = cookie;
    }

    /**
     * https证书
     * @return  List<byte[]>
     */
    public static List<byte[]> getCertificatesData() {
        return certificatesData;
    }
}


