/**
 * Copyright 2015 Huawei Technologies Co., Ltd. All rights reserved.
 * eSDK is licensed under the Apache License, Version 2.0 ^(the "License"^);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.huawei.esdk.cc.service.conference;

import com.huawei.esdk.cc.video.CameraInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 会议成员对象
 */
public class ConferenceUserInfo
{
    /**
     * 用户Id（软终端号码）
     **/
    private String userId;

    private int deviceNum = 1;

    private List<CameraInfo> cameraInfos = new ArrayList<CameraInfo>();

    /**
     * 构造
     */
    public ConferenceUserInfo()
    {

    }

    /**
     * 构造
     *
     * @param userId userId
     */
    public ConferenceUserInfo(String userId)
    {
        this.userId = userId;
    }

    public String getUserId()
    {
        return userId;
    }

    public void setUserId(String userId)
    {
        this.userId = userId;
    }

    public int getDeviceNum()
    {
        return deviceNum;
    }

    public void setDeviceNum(int deviceNum)
    {
        this.deviceNum = deviceNum;
    }

    /**
     * getCurrDeviceId
     * @return String
     */
    public String getCurrDeviceId()
    {
        if (cameraInfos != null && cameraInfos.size() > 0)
        {
            return cameraInfos.get(0).getDeviceID();
        }
        return null;
    }

    public List<CameraInfo> getCameraInfos()
    {
        return cameraInfos;
    }

    /**
     * addCameraInfo
     *
     * @param cameraInfo cameraInfo
     */
    public void addCameraInfo(CameraInfo cameraInfo)
    {
        cameraInfos.add(cameraInfo);
    }

    /**
     * removeCameraInfo
     *
     * @param cameraInfo cameraInfo
     */
    public void removeCameraInfo(CameraInfo cameraInfo)
    {
        cameraInfos.remove(cameraInfo);
    }

}
