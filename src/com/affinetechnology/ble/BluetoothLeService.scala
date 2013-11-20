package com.affinetechnology.ble
/*
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;



class BluetoothLeService extends Service{
// BLE
  private var mBluetoothManager: BluetoothManager = null;
  private var mBtAdapter: BluetoothAdapter = null;
  private var mBluetoothGatt: BluetoothGatt = null;
  BluetoothLeService.mThis = null;
  private var mBusy = false; // Write/read pending response
  private var mBluetoothDeviceAddress = "";

  /**
   * GATT client callbacks
   */
  private val mGattCallbacks = new BluetoothGattCallback() {

    def withAddress = (i:Intent, address: String) => {
      i.putExtra(BluetoothLeService.EXTRA_ADDRESS, address)
    }
    def withCharacteristic = (i:Intent, characteristic: BluetoothGattCharacteristic) => {
      i.putExtra(BluetoothLeService.EXTRA_UUID, characteristic.getUuid().toString());
      i.putExtra(BluetoothLeService.EXTRA_DATA, characteristic.getValue());
    }
    
    @Override
    override def onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) = {
      if (mBluetoothGatt == null) {
        Log.e(BluetoothLeService.TAG, "mBluetoothGatt not created!");       
      }else{
	
	      val device = gatt.getDevice();
	      val address = device.getAddress();
	      Log.d(BluetoothLeService.TAG, "onConnectionStateChange (" + address + ") " + newState + " status: " + status);
	
	      try {
	        newState match {
	        	case BluetoothProfile.STATE_CONNECTED => broadcastUpdate(BluetoothLeService.ACTION_GATT_CONNECTED, status, withAddress(_,address) );
	        	case BluetoothProfile.STATE_DISCONNECTED => broadcastUpdate(BluetoothLeService.ACTION_GATT_DISCONNECTED, status, withAddress(_, address));
	        	case _ => Log.e(BluetoothLeService.TAG, "New state not processed: " + newState);
	        }
	      } catch  {
	      	case e: NullPointerException => e.printStackTrace();
	      }
      }
    }

    @Override
    override def onServicesDiscovered(gatt: BluetoothGatt, status: Int) = {
    	val device = gatt.getDevice();
    	broadcastUpdate(BluetoothLeService.ACTION_GATT_SERVICES_DISCOVERED,status, withAddress(_,device.getAddress));
    }

    @Override
    override def onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) = {
    	broadcastUpdate(BluetoothLeService.ACTION_DATA_NOTIFY,BluetoothGatt.GATT_SUCCESS, withCharacteristic(_, characteristic));
    }

    @Override
    override def onCharacteristicRead(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) = {
    	broadcastUpdate(BluetoothLeService.ACTION_DATA_READ,status,withCharacteristic(_, characteristic));
    }

    @Override
    override def onCharacteristicWrite(gatt: BluetoothGatt, characteristic:BluetoothGattCharacteristic, status: Int) = {
    	broadcastUpdate(BluetoothLeService.ACTION_DATA_WRITE,status,withCharacteristic(_, characteristic));
    }

    @Override
    override def onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) = {
      mBusy = false;
      Log.i(BluetoothLeService.TAG, "onDescriptorRead");
    }

    @Override
    override def onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) = {
      mBusy = false;
      Log.i(BluetoothLeService.TAG, "onDescriptorWrite");
    }
  };  
  
  private def broadcastUpdate(action: String, status: Int, f: Intent => Unit) = {
	  val intent = new Intent(action);
	  f(intent)
	  intent.putExtra(BluetoothLeService.EXTRA_STATUS, status)
	  sendBroadcast(intent);
	  mBusy = false;
  }
  
    
  private def checkGatt(): Boolean = {
    if (mBtAdapter == null) {
      Log.w(BluetoothLeService.TAG, "BluetoothAdapter not initialized");
      false;
    }
    if (mBluetoothGatt == null) {
      Log.w(BluetoothLeService.TAG, "BluetoothGatt not initialized");
      false;
    }
    	
    if (mBusy) {
      Log.w(BluetoothLeService.TAG, "LeService busy");
      false;
    }
    true;	
  }
  
  /**
   * Manage the BLE service
   */
  class LocalBinder extends Binder {
    def getService(): BluetoothLeService = {
      BluetoothLeService.this;
    }
  }

  @Override
  def onBind(intent: Intent): IBinder = {
    binder;
  }

  @Override
  override def onUnbind(intent: Intent): Boolean = {
    // After using a given device, you should make sure that
    // BluetoothGatt.close() is called
    // such that resources are cleaned up properly. In this particular example,
    // close() is
    // invoked when the UI is disconnected from the Service.
    close();
    super.onUnbind(intent);
  }

  private val binder = new LocalBinder();

  /**
   * Initializes a reference to the local Bluetooth adapter.
   * 
   * @return Return true if the initialization is successful.
   */
   def initialize(): Boolean = {

    Log.d(BluetoothLeService.TAG, "initialize");
    // For API level 18 and above, get a reference to BluetoothAdapter through
    // BluetoothManager.
    BluetoothLeService.mThis = this;
    if (mBluetoothManager == null) {
      mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE).asInstanceOf[BluetoothManager]
      if (mBluetoothManager == null) {
        Log.e(BluetoothLeService.TAG, "Unable to initialize BluetoothManager.");
        false
      }
    }

    mBtAdapter = mBluetoothManager.getAdapter();
    if (mBtAdapter == null) {
      Log.e(BluetoothLeService.TAG, "Unable to obtain a BluetoothAdapter.");
      return false;
    }
    return true;
  }

  @Override
  override def onStartCommand(intent: Intent, flags: Int, startId: Int): Int = {
    Log.i(BluetoothLeService.TAG, "Received start id " + startId + ": " + intent);
    // We want this service to continue running until it is explicitly
    // stopped, so return sticky.
    Service.START_STICKY
  }

  @Override
  override def onDestroy() = {
    super.onDestroy();
    Log.d(BluetoothLeService.TAG, "onDestroy() called");
    if (mBluetoothGatt != null) {
      mBluetoothGatt.close();
      mBluetoothGatt = null;
    }
  }

  
  
  
  
  //
  // GATT API
  //
  /**
   * Request a read on a given {@code BluetoothGattCharacteristic}. The read result is reported asynchronously through the
   * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)} callback.
   * 
   * @param characteristic
   *          The characteristic to read from.
   */
  def readCharacteristic(characteristic: BluetoothGattCharacteristic) = {
  	if (!checkGatt()){}
  	else{
  		mBusy = true;
    	mBluetoothGatt.readCharacteristic(characteristic);
  	}
  }

  def writeCharacteristic(characteristic: BluetoothGattCharacteristic, b: Byte): Boolean = {
  	if (!checkGatt())
  		false
  	
    val bval = Array(b);    
    characteristic.setValue(bval);

    mBusy = true;
    mBluetoothGatt.writeCharacteristic(characteristic);
  }

  def writeCharacteristic(characteristic: BluetoothGattCharacteristic, b: Boolean): Boolean = {
  	if (!checkGatt())
  		false;

    val bval = Array( (if(b)1.asInstanceOf[Byte] else 0.asInstanceOf[Byte]) );

    characteristic.setValue(bval);
    mBusy = true;
    mBluetoothGatt.writeCharacteristic(characteristic);
  }

  def writeCharacteristic(characteristic: BluetoothGattCharacteristic): Boolean = {
  	if (!checkGatt())
  		false;

  	mBusy = true;
    mBluetoothGatt.writeCharacteristic(characteristic);
  }

  /**
   * Retrieves the number of GATT services on the connected device. This should be invoked only after {@code BluetoothGatt#discoverServices()} completes
   * successfully.
   * 
   * @return A {@code integer} number of supported services.
   */
  def getNumServices(): Int = {
    if (mBluetoothGatt == null)0
    else mBluetoothGatt.getServices().size();
  }

  /**
   * Retrieves a list of supported GATT services on the connected device. This should be invoked only after {@code BluetoothGatt#discoverServices()} completes
   * successfully.
   * 
   * @return A {@code List} of supported services.
   */
  def getSupportedGattServices(): java.util.List[BluetoothGattService] = {
    if (mBluetoothGatt == null)
      return null;
    mBluetoothGatt.getServices();
  }

  /**
   * Enables or disables notification on a give characteristic.
   * 
   * @param characteristic
   *          Characteristic to act on.
   * @param enabled
   *          If true, enable notification. False otherwise.
   */
  def setCharacteristicNotification(characteristic: BluetoothGattCharacteristic, enable: Boolean): Boolean = {
  	if (!checkGatt())
  		false;

    if (!mBluetoothGatt.setCharacteristicNotification(characteristic, enable)) {
      Log.w(BluetoothLeService.TAG, "setCharacteristicNotification failed");
      false;
    }

    val clientConfig = characteristic.getDescriptor(GattInfo.CLIENT_CHARACTERISTIC_CONFIG);
    if (clientConfig == null)
      return false;

    if (enable) {
      Log.i(BluetoothLeService.TAG, "enable notification");
      clientConfig.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
    } else {
      Log.i(BluetoothLeService.TAG, "disable notification");
      clientConfig.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
    }

    mBusy = true;
    mBluetoothGatt.writeDescriptor(clientConfig);
  }

  def isNotificationEnabled(characteristic: BluetoothGattCharacteristic): Boolean = {
  	if (!checkGatt())
  		return false;

  	val clientConfig = characteristic.getDescriptor(GattInfo.CLIENT_CHARACTERISTIC_CONFIG);
    if (clientConfig == null)
      false;

    clientConfig.getValue() == BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE;
  }

  /**
   * Connects to the GATT server hosted on the Bluetooth LE device.
   * 
   * @param address
   *          The device address of the destination device.
   * 
   * @return Return true if the connection is initiated successfully. The connection result is reported asynchronously through the
   *         {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)} callback.
   */
  def connect(address: String): Boolean = {
    if (mBtAdapter == null || address == null) {
      Log.w(BluetoothLeService.TAG, "BluetoothAdapter not initialized or unspecified address.");
      return false;
    }
    val device = mBtAdapter.getRemoteDevice(address);
    val connectionState = mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);

    if (connectionState == BluetoothProfile.STATE_DISCONNECTED) {

      // Previously connected device. Try to reconnect.
      if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress) && mBluetoothGatt != null) {
        Log.d(BluetoothLeService.TAG, "Re-use GATT connection");
        if (mBluetoothGatt.connect()) {
          return true;
        } else {
          Log.w(BluetoothLeService.TAG, "GATT re-connect failed.");
          return false;
        }
      }

      if (device == null) {
        Log.w(BluetoothLeService.TAG, "Device not found.  Unable to connect.");
        return false;
      }
      // We want to directly connect to the device, so we are setting the
      // autoConnect parameter to false.
      Log.d(BluetoothLeService.TAG, "Create a new GATT connection.");
      mBluetoothGatt = device.connectGatt(this, false, mGattCallbacks);
      mBluetoothDeviceAddress = address;
    } else {
      Log.w(BluetoothLeService.TAG, "Attempt to connect in state: " + connectionState);
      return false;
    }
    return true;
  }

  /**
   * Disconnects an existing connection or cancel a pending connection. The disconnection result is reported asynchronously through the
   * {@code BluetoothGattCallback#onConnectionStateChange(android.bluetooth.BluetoothGatt, int, int)} callback.
   */
  def disconnect(address: String) {
    if (mBtAdapter == null) {
      Log.w(BluetoothLeService.TAG, "disconnect: BluetoothAdapter not initialized");
      return;
    }
    val device = mBtAdapter.getRemoteDevice(address);
    val connectionState = mBluetoothManager.getConnectionState(device, BluetoothProfile.GATT);

    if (mBluetoothGatt != null) {
      Log.i(BluetoothLeService.TAG, "disconnect");
      if (connectionState != BluetoothProfile.STATE_DISCONNECTED) {
        mBluetoothGatt.disconnect();
      } else {
        Log.w(BluetoothLeService.TAG, "Attempt to disconnect in state: " + connectionState);
      }
    }
  }

  /**
   * After using a given BLE device, the app must call this method to ensure resources are released properly.
   */
  def close() = {
    if (mBluetoothGatt != null) {
      Log.i(BluetoothLeService.TAG, "close");
      mBluetoothGatt.close();
      mBluetoothGatt = null;
    }
  }

  def numConnectedDevices(): Int = {
	if (mBluetoothGatt != null) {
      val devList = mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
      devList.size();
    }
    0
  }
  

  def waitIdle(ii: Int): Boolean = {
    val fac = ii / 10;
    def idle(i: Int): Boolean = {
      if (mBusy && i > 0 )
        try {
          Thread.sleep(10);
          idle(i-1)
        } catch {
        	case e: InterruptedException => e.printStackTrace();
        }
      i > 0
    }
    idle(fac)    
  }
  
  
}


object BluetoothLeService{
  val TAG = "BluetoothLeService";

  val ACTION_GATT_CONNECTED ="ti.android.ble.common.ACTION_GATT_CONNECTED";
  val ACTION_GATT_DISCONNECTED = "ti.android.ble.common.ACTION_GATT_DISCONNECTED";
  val ACTION_GATT_SERVICES_DISCOVERED = "ti.android.ble.common.ACTION_GATT_SERVICES_DISCOVERED";
  val ACTION_DATA_READ = "ti.android.ble.common.ACTION_DATA_READ";
  val ACTION_DATA_NOTIFY = "ti.android.ble.common.ACTION_DATA_NOTIFY";
  val ACTION_DATA_WRITE = "ti.android.ble.common.ACTION_DATA_WRITE";
  val EXTRA_DATA = "ti.android.ble.common.EXTRA_DATA";
  val EXTRA_UUID = "ti.android.ble.common.EXTRA_UUID";
  val EXTRA_STATUS = "ti.android.ble.common.EXTRA_STATUS";
  val EXTRA_ADDRESS = "ti.android.ble.common.EXTRA_ADDRESS";
  
  var mThis: BluetoothLeService = null
  
  
  //
  // Utility functions
  //
  def getBtGatt(): BluetoothGatt = {
    mThis.mBluetoothGatt;
  }

  def getBtManager() = {
    mThis.mBluetoothManager;
  }

  def getInstance() = {
    mThis;
  }
}
*/