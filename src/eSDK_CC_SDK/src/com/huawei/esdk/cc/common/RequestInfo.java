package com.huawei.esdk.cc.common;

import com.huawei.esdk.cc.video.StreamInfo;

import java.io.Serializable;

/**
 * Created on 2017/1/11.
 */
public class RequestInfo implements Serializable
{
    //消息
    private String msg = "";
    //视频流信息
    private StreamInfo streamInfo = null;

    /**
     * 构造
     */
    public RequestInfo()
    {

    }

    public StreamInfo getStreamInfo()
    {
        return streamInfo;
    }

    public void setStreamInfo(StreamInfo streamInfo)
    {
        this.streamInfo = streamInfo;
    }

    public String getMsg()
    {
        return msg;
    }

    public void setMsg(String msg)
    {
        this.msg = msg;
    }
}
