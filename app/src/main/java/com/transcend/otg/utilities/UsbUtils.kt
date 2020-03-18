package com.transcend.otg.utilities

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import com.github.mjdev.libaums.UsbMassStorageDevice
import com.github.mjdev.libaums.fs.FileSystem

object UsbUtils {
    val ACTION_USB_PERMISSION = "com.transcend.otg.USB_PERMISSION"
    var usbMassStorageDevice: UsbMassStorageDevice? = null
    var usbDevice: UsbDevice? = null
    var usbFileSystem: FileSystem? = null
    var usbManager: UsbManager? = null

    fun clearDevice(){
        usbMassStorageDevice = null
        usbDevice = null
        usbFileSystem = null
        Constant.OTG_ROOT = null
    }

    fun isOtgDeviceExist(context: Context): Boolean{
        val devices = UsbMassStorageDevice.getMassStorageDevices(context)
        if (devices.size == 0) {
            clearDevice()
            return false
        }
        Constant.OTG_ROOT = "/"
        return true
    }

    fun discoverDevices(context: Context): Boolean{
        initManager(context)
        val devices = UsbMassStorageDevice.getMassStorageDevices(context)
        if (devices.size == 0) {
            clearDevice()
            return false
        }
        usbMassStorageDevice = devices[0]
        setDevice(devices[0])
        return true
    }

    fun setDevice(device: UsbMassStorageDevice){
        usbDevice = device.usbDevice
        Constant.OTG_ROOT = "/"
        if (hasUSBPermission()){
            device.init()
            usbFileSystem = device.partitions[0].fileSystem
        }
    }

    fun initManager(context: Context) {
        if (usbManager == null)
            usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    }

    fun hasUSBPermission(): Boolean {
        if (usbManager == null || usbDevice == null)
            return false
        return usbManager!!.hasPermission(usbDevice)
    }

    fun doUSBRequestPermission(context: Context) {
        if (usbManager == null || usbDevice == null)
            discoverDevices(context)
        val permissionIntent = PendingIntent.getBroadcast(context, 0, Intent(ACTION_USB_PERMISSION), 0)
        usbManager!!.requestPermission(usbDevice, permissionIntent)
    }
}