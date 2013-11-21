android-scala-ble
=================

Blue Tooth Low Energy Scanner for Android written in scala


## Overview

This project serves to wrap up some common functionality regarding scanning for a BLE sensor device.  
Right now the code is simple and to the point.  Install a filter (device name, mac address) and then start 
scanning for devices. 

## Features

I will try to keep adding to this list as I go.  For now here is a short list of features:

* Scan for a BLE sensor devices
  * Filter device list
* Callback function to report device and signal strength


## Example Usage

```scala
class MainActivity extends Activity with BleDeviceScanner{
  // Notice the "with BleDeviceScenner"
  // ...
  
  @Override
	protected override def onCreate(savedInstanceState: Bundle) = {
	  // ... normal android init
	  initBleScanner(this)
	  
	  val bscan = findViewById( R.id.bscan ).asInstanceOf[Button]
		bscan.setOnClickListener(new View.OnClickListener {
		  override def onClick(v: View) = {		    
		     val filter = {
		        d: BluetoothDevice =>
					d.getName() != null		// You could filter by device name or address here..  									
		      }
		      startScanWithFilter(filter){
		        di: BleDeviceInfo =>  // This ia a callback with the located device 
		          Log.d(TAG,"Found device[%s] with signal stregth: %s".format(di.getBluetoothDevice.getAddress, di.getRssi) )
		      } 		    
		  }
		})
		
	}
	  
}
```

## Example Projects
Was used in a hackathon to try to do accurate indoor positioning.  We ended up having to make a ton of modifications since the TI Sensor Tag can not be used to accuratly interpolate position.

Here is a screen shot
TODO://



