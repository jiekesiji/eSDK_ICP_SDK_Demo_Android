package com.huawei.esdk.cc.service.ics;

import com.huawei.esdk.cc.video.VideoParams;

/**
 * 设置类
 */
public final class SystemSetting
{

    private static final Object LOCKOBJECT = new Object();
    private static SystemSetting ins;

    /**
     * 服务器地址
     **/
    private String serverIp = "127.0.0.1";

    /**
     * 端口号
     **/
    private int serverPort = 0;

    /**
     * SC/会议服务器地址
     **/
    private String sipIp = "";

    /**
     * SC/会议端口号
     **/
    private String sipPort = "";
    private String transSecurity = "";
    private String vndID = "";
    private String userName = "";

    private boolean isTLSEncoded = true;
    private boolean isSRTPEncoded = true;

    private boolean isAudioConnected = false;
    private boolean isAudio = false;
    private boolean isMeeting = false;

    private boolean isHTTPS = false;

    private String logPath = "";

    private int logLevel = 2;

    private VideoParams videoParams = null;

    private int nUserID;
    private String deviceID;
    private boolean isTransSecurity = false;
    private boolean validateServerCertificate;
    private boolean validateDomain;

    private String verifyCode = "";
    private String anonymousCard = "";


    private SystemSetting()
    {
    }

    public boolean isTransSecurity()
    {
        return isTransSecurity;
    }

    public void setIsTransSecurity(boolean isTransSecurity)
    {
        this.isTransSecurity = isTransSecurity;
    }

    public boolean isMeeting()
    {
        return isMeeting;
    }

    public void setIsMeeting(boolean isMeeting)
    {
        this.isMeeting = isMeeting;
    }

    public boolean isAudio()
    {
        return isAudio;
    }

    public void setIsAudio(boolean isAudio)
    {
        this.isAudio = isAudio;
    }

    /**
     * 构造
     *
     * @return SystemSetting
     */
    public static SystemSetting getInstance()
    {
        synchronized (LOCKOBJECT)
        {
            if (ins == null)
            {
                ins = new SystemSetting();
            }
            return ins;
        }
    }

    public int getLogLevel()
    {
        return logLevel;
    }

    public void setLogLevel(int logLevel)
    {
        this.logLevel = logLevel;
    }

    public String getLogPath()
    {
        return logPath;
    }

    public void setLogPath(String logPath)
    {
        this.logPath = logPath;
    }

    public boolean isAudioConnected()
    {
        return isAudioConnected;
    }

    public void setIsAudioConnected(boolean isAudioConnected)
    {
        this.isAudioConnected = isAudioConnected;
    }

    public boolean isSRTPEncoded()
    {
        return isSRTPEncoded;
    }

    public void setSRTPEncoded(boolean isSRTPEncoded)
    {
        this.isSRTPEncoded = isSRTPEncoded;
    }

    public boolean isTLSEncoded()
    {
        return isTLSEncoded;
    }

    public void setTLSEncoded(boolean isEncoded)
    {
        this.isTLSEncoded = isEncoded;
    }

    /**
     * 初始化地址端口
     *
     * @param ip            ip
     * @param port          port
     * @param transSecurity transSecurity
     */
    public void initServer(String ip, int port, String transSecurity)
    {
        setServerIp(ip);
        setServerPort(port);
        setTransSecurity(transSecurity);
    }

    /**
     * 初始化
     *
     * @param sipIp   sipIp
     * @param sipPort sipPort
     */
    public void initSIPServerAddr(String sipIp, String sipPort)
    {
        setSIPIp(sipIp);
        setSIPPort(sipPort);
    }

    public String getServerIp()
    {
        return serverIp;
    }

    public void setServerIp(String serverIp)
    {
        this.serverIp = serverIp;
    }

    public int getServerPort()
    {
        return serverPort;
    }

    public void setServerPort(int serverPort)
    {
        this.serverPort = serverPort;
    }

    public String getSIPIp()
    {
        return sipIp;
    }

    private void setSIPIp(String sipIp)
    {
        this.sipIp = sipIp;
    }

    public String getSIPPort()
    {
        return sipPort;
    }

    private void setSIPPort(String sipPort)
    {
        this.sipPort = sipPort;
    }


    public String getTransSecurity()
    {
        return transSecurity;
    }

    public void setTransSecurity(String transSecurity)
    {
        this.transSecurity = transSecurity;
    }

    public String getVndID()
    {
        return vndID;
    }

    public void setVndID(String vndID)
    {
        this.vndID = vndID;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public boolean isHTTPS()
    {
        return isHTTPS;
    }

    public void setIsHTTPS(boolean isHTTPS)
    {
        this.isHTTPS = isHTTPS;
    }

    public VideoParams getVideoParams()
    {
        return videoParams;
    }

    public void setVideoParams(VideoParams videoParams)
    {
        this.videoParams = videoParams;
    }

    public String getDeviceID()
    {
        return deviceID;
    }

    public void setDeviceID(String deviceID)
    {
        this.deviceID = deviceID;
    }

    /**
     * 获取用户id
     *
     * @return int
     */
    public int getnUserID()
    {
        return nUserID;
    }

    /**
     * 设置用户id
     *
     * @param nUserID nUserID
     */
    public void setnUserID(int nUserID)
    {
        this.nUserID = nUserID;
    }

    public boolean isValidateServerCertificate()
    {
        return validateServerCertificate;
    }

    public void setValidateServerCertificate(boolean validateServerCertificate)
    {
        this.validateServerCertificate = validateServerCertificate;
    }

    public boolean isValidateDomain()
    {
        return validateDomain;
    }

    public void setValidateDomain(boolean validateDomain)
    {
        this.validateDomain = validateDomain;
    }

    public String getVerifyCode()
    {
        return verifyCode;
    }

    public void setVerifyCode(String verifyCode)
    {
        this.verifyCode = verifyCode;
    }

    public String getAnonymousCard()
    {
        return anonymousCard;
    }

    public void setAnonymousCard(String anonymousCard)
    {
        this.anonymousCard = anonymousCard;
    }
}
