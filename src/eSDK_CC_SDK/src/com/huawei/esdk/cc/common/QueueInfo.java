package com.huawei.esdk.cc.common;

import java.io.Serializable;

/**
 * Created on 2017/1/11.
 */
public class QueueInfo implements Serializable
{
    //排队相关消息
    private long position;
    private int onlineAgentNum;
    private long longestWaitTime;

    /**
     * 构造
     */
    public QueueInfo()
    {
    }

    public long getPosition()
    {
        return position;
    }

    public void setPosition(long position)
    {
        this.position = position;
    }

    public int getOnlineAgentNum()
    {
        return onlineAgentNum;
    }

    public void setOnlineAgentNum(int onlineAgentNum)
    {
        this.onlineAgentNum = onlineAgentNum;
    }

    public long getLongestWaitTime()
    {
        return longestWaitTime;
    }

    public void setLongestWaitTime(long longestWaitTime)
    {
        this.longestWaitTime = longestWaitTime;
    }
}
