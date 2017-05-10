package com.huawei.esdk.cc.utils;

import android.os.Environment;
import android.util.Log;
import com.huawei.esdk.cc.common.NotifyMessage;
import com.huawei.esdk.cc.service.ics.SystemSetting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created on 2015/12/28.
 */
public final class LogUtil
{
    private static final String TAG = "CCMobile";
    private static final String FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";
    private static double logFileSize = 5 * 1024.00 * 1024;

    private LogUtil()
    {

    }

    /**
     * 日志打印
     *
     * @param tag tag
     * @param msg msg
     */
    public static void d(String tag, String msg)
    {
        if (SystemSetting.getInstance().getLogLevel() <= Log.DEBUG)
        {
            Log.d(TAG, tag + " " + msg);
            writeLog("info" + "-" + getTagName(tag) + " : " + msg);
        }
    }

    /**
     * 日志打印
     *
     * @param tag tag
     * @param msg msg
     */
    public static void e(String tag, String msg)
    {
        if (SystemSetting.getInstance().getLogLevel() <= Log.ERROR)
        {
            Log.e(TAG, tag + " " + msg);
            writeLog("error" + "-" + getTagName(tag) + " : " + msg);
        }
    }

    private static String getTagName(String tag)
    {
        return tag == null ? NotifyMessage.CC_LOG : tag;
    }

    private static void writeLog(String logText)
    {
        String mounted = Environment.MEDIA_MOUNTED;
        if (!mounted.equals(Environment.getExternalStorageState()))
        {
            return;
        }
        String nowTimeStr = String.format("[%s]", new SimpleDateFormat(FORMAT).format(new Date()));
        String toLogStr = nowTimeStr + " " + logText;
        toLogStr += "\r\n";

        FileOutputStream fileOutputStream = null;
        String logFile = Environment.getExternalStorageDirectory().toString() + "/" + NotifyMessage.CC_LOG;
        String filename = NotifyMessage.CC_LOG_FILE_NAME;
        try
        {

            File fileOld = new File(logFile + "/" + filename);
            if ((float) ((fileOld.length() + logText.length()) / 1024.00) > logFileSize)
            {
                File bakFile = new File(fileOld.getPath() + ".bak");
                if (bakFile.exists())
                {
                    if (bakFile.delete())
                    {
                        Log.d("Write Log", "delete " + bakFile.getName());
                    }
                }
                if (fileOld.renameTo(bakFile))
                {
                    Log.d("Write Log", fileOld.getName() + " rename to " + bakFile.getName());
                }
            }

            File file = new File(logFile);
            if (!file.exists())
            {
                if (file.mkdir())
                {
                    Log.d("Write Log", "create " + file.getName());
                }
            }

            File filepath = new File(logFile + "/" + filename);
            if (!filepath.exists())
            {
                if (filepath.createNewFile())
                {
                    Log.d("Write Log", "create " + filepath.getName());
                }
            }
            fileOutputStream = new FileOutputStream(filepath, true);

            byte[] buffer = toLogStr.getBytes(NotifyMessage.CHARSET_UTF_8);

            fileOutputStream.write(buffer);
        } catch (FileNotFoundException e)
        {
            Log.e(TAG, "not found exception!");
        } catch (IOException e)
        {
            Log.e(TAG, " write log exception！");
        } finally
        {
            if (fileOutputStream != null)
            {
                try
                {
                    fileOutputStream.close();
                } catch (IOException e)
                {
                    Log.e(TAG, "close io exception！");
                }
            }
        }
    }
}
