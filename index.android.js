'use strict';

var React = require('react-native');
var { requireNativeComponent, NativeModules, PropTypes, View, DeviceEventEmitter } = React;

var ReactNativeBLE = {
  connect: NativeModules.ReactNativeBLEModule.connect,
  disconnect: NativeModules.ReactNativeBLEModule.disconnect,
  startScan: NativeModules.ReactNativeBLEModule.startScan,
  stopScan: NativeModules.ReactNativeBLEModule.stopScan,
  setCharacteristicNotification: NativeModules.ReactNativeBLEModule.setCharacteristicNotification,
  readCharacteristic: NativeModules.ReactNativeBLEModule.readCharacteristic,
  _onLeScan: function(callback) {DeviceEventEmitter.addListener('onLeScan', callback) }, 
  _onServicesDiscovered: function(callback) {DeviceEventEmitter.addListener('onServicesDiscovered', callback) }, 
  _onCharacteristicChanged: function(callback) {DeviceEventEmitter.addListener('onCharacteristicChanged', callback) }, 
  _onCharacteristicRead: function(callback) {DeviceEventEmitter.addListener('onCharacteristicRead', callback) }, 
  _onConnectionStateChange: function(callback) {DeviceEventEmitter.addListener('onConnectionStateChange', callback) }, 
}

module.exports = ReactNativeBLE;