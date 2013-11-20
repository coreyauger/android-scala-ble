package com.affinetechnology.ble

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import android.content.res.XmlResourceParser;

object GattInfo {
  // Bluetooth SIG identifiers
  val CLIENT_CHARACTERISTIC_CONFIG = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
  val uuidBtSigBase = "0000****-0000-1000-8000-00805f9b34fb";
  val uuidTiBase = "f000****-0451-4000-b000-000000000000";

  val OAD_SERVICE_UUID = UUID.fromString("f000ffc0-0451-4000-b000-000000000000");
  val CC_SERVICE_UUID = UUID.fromString("f000ccc0-0451-4000-b000-000000000000");

  val mNameMap = new java.util.HashMap[String, String]();
  val mDescrMap = new java.util.HashMap[String, String]();
  
  
  def uuidToName(uuid: UUID): String = {
    val str = toShortUuidStr(uuid);
    uuidToName(str.toUpperCase());
  }

  def getDescription(uuid: UUID) = {
    val str = toShortUuidStr(uuid);
    mDescrMap.get(str.toUpperCase());
  }

  def isTiUuid(u: UUID) = {
    val us = u.toString();
    val r = toShortUuidStr(u);
    val us2 = us.replace(r, "****");
    us2.equals(uuidTiBase);
  }

  def isBtSigUuid(u: UUID) = {
    val us = u.toString();
    val r = toShortUuidStr(u);
    val us2 = us.replace(r, "****");
    us2.equals(uuidBtSigBase);
  }

  def uuidToString(u: UUID) = {
    val uuidStr = if (isBtSigUuid(u)) GattInfo.toShortUuidStr(u) else u.toString()
    uuidStr.toUpperCase();
  }

  def toShortUuidStr(u: UUID) = {
    u.toString().substring(4, 8);
  }

  def uuidToName(uuidStr16: String): String = {
    mNameMap.get(uuidStr16);
  }
  
}

class GattInfo(xpp: XmlResourceParser) {
  

    // XML data base
    try {
      readUuidData(xpp);
    } catch {
    	case e: IOException => e.printStackTrace();
    	case e: XmlPullParserException => e.printStackTrace();
    }
  

  

  //
  // XML loader
  //
  private def readUuidData(xpp: XmlResourceParser) = {
    xpp.next();
    var tagName: String = null;
    var uuid: String = null;
    var descr: String = null;
    var eventType = xpp.getEventType();

    while (eventType != XmlPullParser.END_DOCUMENT) {
      if (eventType == XmlPullParser.START_DOCUMENT) {
        // do nothing
      } else if (eventType == XmlPullParser.START_TAG) {
        tagName = xpp.getName();
        uuid = xpp.getAttributeValue(null, "uuid");
        descr = xpp.getAttributeValue(null, "descr");
      } else if (eventType == XmlPullParser.END_TAG) {
        // do nothing
      } else if (eventType == XmlPullParser.TEXT) {
        if (tagName.equalsIgnoreCase("item")) {
          if (!uuid.isEmpty()) {
            uuid = uuid.replace("0x", "");
            GattInfo.mNameMap.put(uuid, xpp.getText());
            GattInfo.mDescrMap.put(uuid, descr);
          }
        }
      }
      eventType = xpp.next();
    }
  }
}