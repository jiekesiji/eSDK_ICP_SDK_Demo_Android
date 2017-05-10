package com.huawei.esdk.cc.utils;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;

import com.huawei.esdk.cc.service.CCAPP;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 * Created on 2015/12/29.
 */
public final class StringUtils
{
    private StringUtils()
    {

    }

    /**
     * @param source source
     * @return boolean
     */
    public static boolean isStringEmpty(String source)
    {
        return source == null || "".equals(source);
    }

    /**
     * @param str str
     * @return int
     */
    public static int stringToInt(String str)
    {
        return stringToInt(str, -1);
    }

    /**
     * @param str          str
     * @param defaultValue defaultValue
     * @return int
     */
    public static int stringToInt(String str, int defaultValue)
    {
        if (isStringEmpty(str))
        {
            return defaultValue;
        }
        try
        {
            return Integer.parseInt(str);
        } catch (NumberFormatException e)
        {
            return defaultValue;
        }
    }


    /**
     * Function: 获取当前的IP地址.
     *
     * @return String
     */
    public static String getIpAddress()
    {
        WifiManager wifiManager = (WifiManager) CCAPP.getInstances().getApplication()
                .getSystemService(Context.WIFI_SERVICE);
        if (wifiManager == null)
        {
            return "";
        }
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        if (wifiInfo == null)
        {
            return "";
        }
        int ipAddress = wifiInfo.getIpAddress();
        String ip = intToIp(ipAddress);
        if (0 != ipAddress)
        {
            return ip;
        }
        try
        {
            Enumeration<NetworkInterface> networkInfo = NetworkInterface.getNetworkInterfaces();
            NetworkInterface intf = null;
            Enumeration<InetAddress> intfAddress = null;
            InetAddress inetAddress = null;
            if (networkInfo == null)
            {
                return "";
            }
            for (Enumeration<NetworkInterface> en = networkInfo; en.hasMoreElements();)
            {
                intf = en.nextElement();
                intfAddress = intf.getInetAddresses();
                for (Enumeration<InetAddress> enumIpAddr = intfAddress; enumIpAddr.hasMoreElements();)
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
        } catch (SocketException e)
        {
            LogUtil.e("StringUtils", "socket excption");
        }
        return ip;
    }


    /**
     * 获取本机IP
     *
     * @param i i
     * @return String
     */
    public static String intToIp(int i)
    {
        return (i & 0xFF) + "." + ((i >> 8) & 0xFF) + "." + ((i >> 16) & 0xFF)
                + "." + ((i >> 24) & 0xFF);
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


}
