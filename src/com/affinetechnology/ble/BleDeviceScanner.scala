package com.affinetechnology.ble


///
/// Author: Corey Auger
/// coreyauger@gmail.com
///

import android.os.Bundle
import android.app.Activity
import android.content.pm.PackageManager
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.bluetooth.BluetoothDevice
import android.util.Log


trait BleDeviceScanner  {
	private val TAG = "BleDeviceScanner"
	  
	protected var mBleSupported = true
	protected var mBluetoothManager: BluetoothManager = null
	protected var mBtAdapter: BluetoothAdapter = null
	
	protected var mBluetoothDevice: BluetoothDevice = null;
	//private var mBluetoothLeService: BluetoothLeService = null;
	protected var mNumDevs = 0
	protected var mScanning = false
	protected var mDeviceFilter = Array[String]();
	
	protected var mDeviceInfoList: List[BleDeviceInfo] = Nil
	
	
	protected var mActivity: Activity = null
	 
	protected def initBleScanner(activity: Activity) = {
		mActivity = activity
		// Use this check to determine whether BLE is supported on the device. Then
	    // you can selectively disable BLE-related features.
	    if (!mActivity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
	      Log.e(TAG,"ble not supported");
	      mBleSupported = false;
	    }
	
	    // Initializes a Bluetooth adapter. For API level 18 and above, get a
	    // reference to BluetoothAdapter through BluetoothManager.
	    mBluetoothManager = mActivity.getSystemService(Context.BLUETOOTH_SERVICE).asInstanceOf[BluetoothManager]
	    mBtAdapter = mBluetoothManager.getAdapter();
	
	    // Checks if Bluetooth is supported on the device.
	    if (mBtAdapter == null) {
	      Log.e(TAG,"ble not supported");
	      mBleSupported = false;
	    }	
	}	
	
	def startScan() = {
	    if (mBleSupported) {
	      mNumDevs = 0;   
	      scanLeDevice(true);
	    }	
	  }	
	
	private def scanLeDevice(enable: Boolean): Boolean = {
	    if (enable) {
	      mScanning = mBtAdapter.startLeScan(mLeScanCallback);
	    } else {
	      mScanning = false;
	      mBtAdapter.stopLeScan(mLeScanCallback);
	    }
	    return mScanning;
	  }
	
	
	
	private def checkDeviceFilter(device: BluetoothDevice): Boolean = {
	  	var  n = mDeviceFilter.length;
	  	if (n > 0) {
	  		var found = false;
	  		var i =0
	  		while ( i<n && !found) {
	  			if( device.getName() != null){ // added a device null check... 
	  				found = device.getName().equals(mDeviceFilter(i));
	  			}
	  			i = i +1
	  		}
	  		found;
	  	} else
	  	true
	  }
	
	
	private def deviceInfoExists(address: String): Boolean = {
	  mDeviceInfoList.filter( _.getBluetoothDevice.getAddress.equals(address) ).size > 0	  
	}
	
	
	private def createDeviceInfo(device: BluetoothDevice, rssi: Int): BleDeviceInfo = {
		new BleDeviceInfo(device, rssi);
	}
	
	
	private def addDevice(device: BleDeviceInfo) = {
	    mNumDevs = mNumDevs + 1;
	    mDeviceInfoList = device :: mDeviceInfoList	 
	}
	
	
	private def findDeviceInfo(device: BluetoothDevice): BleDeviceInfo = {
	    val item = mDeviceInfoList.filter(_.getBluetoothDevice.getAddress().equals(device.getAddress()))
	    if( item.size > 0 )item(0)
	    else null
		
	  }
	
	
	// Device scan callback.
  // NB! Nexus 4 and Nexus 7 (2012) only provide one scan result per scan
  private val mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

    def onLeScan(device: BluetoothDevice, rssi: Int, scanRecord: Array[Byte]) {
      mActivity.runOnUiThread(new Runnable() {
        def run() = {
        	// Filter devices
        	if (checkDeviceFilter(device)) {
        		val deviceInfo = createDeviceInfo(device, rssi);
        		Log.d(TAG, "rsii: " + deviceInfo.getRssi)
        		Log.d(TAG, "address: " + deviceInfo.getBluetoothDevice.getAddress())        	          	  
        		if (!deviceInfoExists(device.getAddress())) {
        			// New device        			
        			addDevice(deviceInfo);         			 
        		} else {
        			// Already in list, update RSSI info
        			val deviceInfo = findDeviceInfo(device);
        			deviceInfo.updateRssi(rssi);
        		}
        	}
        }

      });
    }
  };
	

}
