package com.huawei.esdk.cc.service.ics.model.request;

/**
 * Created on 2016/2/22.
 */
public class LoginRequest
{

    private String userIp;
    private String appId;
    private String entryIp;

    /**
     * 构造
     *
     * @param userAddress userAddress
     * @param appId       appId
     * @param entryIp     entryIp
     */
    public LoginRequest(String userAddress, String appId, String entryIp)
    {
        this.userIp = userAddress;
        this.appId = appId;
        this.entryIp = entryIp;
    }
}
