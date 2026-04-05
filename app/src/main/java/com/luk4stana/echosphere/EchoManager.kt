package com.luk4stana.echosphere

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import java.util.*

class EchoManager(context: Context) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager.adapter
    private val advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
    private val scanner = bluetoothAdapter?.bluetoothLeScanner
    // UUID corto per risparmiare byte preziosi (16-bit UUID)
    private val SERVICE_UUID = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb")
    private var advertiseCallback: AdvertiseCallback? = null

    fun startEcho(message: String) {
        stopAdvertisingOnly()
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(false)
            .build()

        val data = AdvertiseData.Builder()
            .addServiceUuid(ParcelUuid(SERVICE_UUID))
            // Inviamo solo il messaggio per avere il massimo spazio (max ~26 byte)
            .addServiceData(ParcelUuid(SERVICE_UUID), message.toByteArray(Charsets.UTF_8))
            .build()

        advertiseCallback = object : AdvertiseCallback() {}
        try {
            advertiser?.startAdvertising(settings, data, advertiseCallback)
        } catch (e: Exception) {}
    }

    private fun stopAdvertisingOnly() {
        try {
            advertiseCallback?.let { advertiser?.stopAdvertising(it) }
            advertiseCallback = null
        } catch (e: Exception) {}
    }

    fun startScanning(onMessageReceived: (String) -> Unit) {
        val filter = ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_UUID)).build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        try {
            scanner?.startScan(listOf(filter), settings, object : ScanCallback() {
                override fun onScanResult(callbackType: Int, result: ScanResult) {
                    val data = result.scanRecord?.getServiceData(ParcelUuid(SERVICE_UUID))
                    data?.let { onMessageReceived(String(it, Charsets.UTF_8)) }
                }
            })
        } catch (e: Exception) {}
    }

    fun stopAll() {
        stopAdvertisingOnly()
        try { scanner?.stopScan(object : ScanCallback() {}) } catch (e: Exception) {}
    }
}