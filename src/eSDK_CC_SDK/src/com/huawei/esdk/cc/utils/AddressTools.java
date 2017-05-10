package com.huawei.esdk.cc.utils;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Pattern;


/**
 * 工具类
 */
public final class AddressTools
{
    private AddressTools()
    {

    }

    /**
     * Function: 获取当前的IP地址.
     *
     * @return String
     */
    public static String getLocalIpAddress()
    {
        String ip = "";
        try
        {
            Enumeration<NetworkInterface> networkInfo = NetworkInterface
                    .getNetworkInterfaces();
            NetworkInterface intf = null;
            Enumeration<InetAddress> intfAddress = null;
            InetAddress inetAddress = null;
            if (networkInfo == null)
            {
                return "";
            }
            for (Enumeration<NetworkInterface> en = networkInfo; en
                    .hasMoreElements();)
            {
                intf = en.nextElement();
                intfAddress = intf.getInetAddresses();
                for (Enumeration<InetAddress> enumIpAddr = intfAddress; enumIpAddr
                        .hasMoreElements();)
                {
                    inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress())
                    {
                        ip = inetAddress.getHostAddress();
                        if (isIPV4Addr(ip))
                        {
                            return ip;
                        }
                    }
                }
            }
        }
        catch (SocketException e)
        {
            LogUtil.e("AddressTools", "socket excption");
        }
        return ip;
    }

    /**
     * 判断是否是ipv4地址
     *
     * @param ipAddr ipAddr
     * @return boolean
     */
    public static boolean isIPV4Addr(String ipAddr)
    {
        Pattern p = Pattern
                .compile("^((25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])\\.){3}"
                        + "(25[0-5]|2[0-4][0-9]|1[0-9][0-9]|[1-9]?[0-9])$");
        return p.matcher(ipAddr).matches();

    }

    /**
     * 字符串转换为数字
     *
     * @param source source
     * @return int
     */
    public static int parseInt(String source)
    {
        try
        {
            int target = Integer.parseInt(source);
            return target;
        } catch (NumberFormatException e)
        {
            return -1;
        }
    }


}
