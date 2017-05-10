package com.huawei.esdk.cc.common;

import java.io.Serializable;

/**
 * Created on 2017/1/11.
 */
public class RequestCode implements Serializable
{
    //网络质量
    private int netLevel = 0;

    //返回码
    private String retCode;
    private int errorCode;

    /**
     * 构造
     */
    public RequestCode()
    {

    }

    public int getNetLevel()
    {
        return netLevel;
    }

    public void setNetLevel(int netLevel)
    {
        this.netLevel = netLevel;
    }

    public String getRetCode()
    {
        return retCode;
    }

    public void setRetCode(String retCode)
    {
        this.retCode = retCode;
    }

    public int getErrorCode()
    {
        return errorCode;
    }

    public void setErrorCode(int errorCode)
    {
        this.errorCode = errorCode;
    }
}
