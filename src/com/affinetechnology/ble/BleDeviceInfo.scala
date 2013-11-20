package com.affinetechnology.ble

import android.bluetooth.BluetoothDevice;

class BleDeviceInfo(val device: BluetoothDevice, var rssi: Int) {
  def getBluetoothDevice = device

  def getRssi = rssi
  
  def updateRssi(rssiValue: Int) = {
    rssi = rssiValue;
  }

}