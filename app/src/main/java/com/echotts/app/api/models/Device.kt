package com.echotts.app.api.models

import com.google.gson.annotations.SerializedName

data class Device(
    @SerializedName("accountName") val accountName: String = "",
    @SerializedName("appDeviceList") val appDeviceList: List<AppDevice>? = null,
    @SerializedName("capabilities") val capabilities: List<Map<String, Any>>? = null,
    @SerializedName("deviceAccountId") val deviceAccountId: String = "",
    @SerializedName("deviceFamily") val deviceFamily: String = "",
    @SerializedName("deviceOwnerCustomerId") val deviceOwnerCustomerId: String = "",
    @SerializedName("deviceType") val deviceType: String = "",
    @SerializedName("online") val online: Boolean = false,
    @SerializedName("serialNumber") val serialNumber: String = "",
    @SerializedName("softwareVersion") val softwareVersion: String = ""
) {
    // Display name for the spinner
    val displayName: String
        get() = if (accountName.isNotEmpty()) accountName else serialNumber

    // Is this an Echo/Alexa speaker?
    val isEchoDevice: Boolean
        get() = deviceFamily.contains("ECHO", ignoreCase = true) ||
                deviceFamily.contains("ALEXA", ignoreCase = true) ||
                deviceType.contains("ECHO", ignoreCase = true)
}

data class AppDevice(
    @SerializedName("deviceAccountId") val deviceAccountId: String = "",
    @SerializedName("deviceSerialNumber") val deviceSerialNumber: String = "",
    @SerializedName("deviceType") val deviceType: String = ""
)

data class DeviceListResponse(
    @SerializedName("devices") val devices: List<Device> = emptyList()
)
