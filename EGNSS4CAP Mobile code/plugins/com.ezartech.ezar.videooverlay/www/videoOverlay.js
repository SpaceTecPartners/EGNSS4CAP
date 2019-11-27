/**
 * videoOverlay.js
 * Copyright 2015-2016, ezAR Technologies
 * Licensed under a modified MIT license, see LICENSE or http://ezartech.com/ezarstartupkit-license
 *
 * @file Implements the ezar video overlay api for controlling device cameras, zoom level.
 * @author @wayne_parrott, @vridosh, @kwparrott
 * @version 0.1.2
 */

var Camera = require('./camera'),
    exec = require('cordova/exec'),
    argscheck = require('cordova/argscheck'),
    utils = require('cordova/utils');

module.exports = (function() {

	 //--------------------------------------
    var _ezAR = {};
    var errorHandler;
    var _isInitialized = false;
    var _frontCamera;
    var _backCamera;
    var _activeCamera;

    /**
     * Has ezAR been successfully initialized.
     * @return {boolean} true when initialize() completes successfully
     */
    _ezAR.isVideoOverlayInitialized = function() {
        return _isInitialized;
    }

    /**
     * Initialize ezAR internal state, cameras and zoom features.
     * @param {function} [successCB] function called on success
	 * @param {function} [errorCB] function with error data parameter called on error
     * @param {options} {
     *        backgroundColor: '#RRGGBB' = '#FFFFFF',
     *        fitWebViewToCameraView: boolean
     *     }
     */
    _ezAR.initializeVideoOverlay = function(successCallback,errorCallback,options) {

        options = options || {};
        var backgroundColorRGB =
                  (options.backgroundColor === undefined ||
                   typeof options.backgroundColor != 'string' ||
                   !(/^#?[0-9A-F]{6}$/i.test(options.backgroundColor))) ?
                  '#FFFFFF' :
                  options.backgroundColor;
        backgroundColorRGB =
            backgroundColorRGB.charAt(0) == '#' ?
                backgroundColorRGB :
                '#' + backgroundColorRGB;

        var fitWebViewToCameraView =
                options.fitWebViewToCameraView === undefined ? true :
                !!options.fitWebViewToCameraView;

        //execute successCallback immediately if already initialized
    	if (_ezAR.isVideoOverlayInitialized()) {
           if (isFunction(successCallback)) successCallback();
           return;
        }

        var onInit = function(deviceData) {
            //console.log(deviceData);
            _ezAR.displaySize =
                { width: deviceData.displayWidth,
                  height: deviceData.displayHeight
                };
            initCameras(deviceData);
            _isInitialized = true;
            if (successCallback) {
                successCallback();
            }
        }

        _ezAR.onError = errorCallback;

        exec(onInit,
             _onError,
            "videoOverlay",
            "init",
            [backgroundColorRGB, fitWebViewToCameraView]);
    }

    /**
     * Return Camera[]. Must call initialize() before calling this function.
     * @return {Camera[]} array of cameras detected
     */
    _ezAR.getCameras = function() {
        var cameras = [];
        if (_frontCamera) cameras.push(_frontCamera);
        if (_backCamera) cameras.push(_backCamera);
         return cameras;
    }

    /**
     * The camera facing away from the user. The camera has position BACK.
     * @return {Camera} null the device does not have a BACK position camera or if ezAR has no been initialized
     */
    _ezAR.getBackCamera = function() {
         return _backCamera;
    }


    /**
     * Test for a camera facing away from the user. Call initialize() before using this function.
     * @return {boolean} true when the device has a camera with position BACK; otherwise return false.
     */
    _ezAR.hasBackCamera = function() {
         return !!_ezAR.getBackCamera();
    }


    /**
     * The camera facing towards the user. The camera has position FRONT.
     * @return {Camera} null the device does not have a FRONT position camera or if ezAR has no been initialized
     */
    _ezAR.getFrontCamera = function() {
         return _frontCamera;
    }

    /**
     * Test for a camera facing towards the user. Call initialize() before using this function.
     * @return {boolean} true when the device has a camera with position FRONT; otherwise return false.
     */
    _ezAR.hasFrontCamera = function() {
         return !!_ezAR.getFrontCamera();
    }

    /**
     * The camera currently running or null.
     * Call initialize() before using this function.
     * @return {Camera}
     */
    _ezAR.getActiveCamera = function() {
        return _activeCamera;
    }

    /**
     * Test for a running camera.
     * Call initialize() before using this function.
     * @return {boolean}
     */
    _ezAR.hasActiveCamera = function() {
        return _ezAR.getActiveCamera() != null;
    }


    //PROTECTED ------------

    //protected, update ezar active camera
    _ezAR._activateCamera = function(camera) {
         _activeCamera = camera;
    }


    //protected - update ezar activate camera to undefined
    _ezAR._deactivateCamera = function() {
        _activeCamera = null;
    }



    //PRIVATE---------------

    function isFunction(f) {
        return typeof f == "function";
    }

    function _onError(data) {
        if (isFunction(_ezAR.onError)) {
           _ezAR.onError(data);
        }
    }

    function initCameras(deviceData) {
        //console.log(deviceData);

        if ('FRONT' in deviceData) {
            _frontCamera = initCamera(deviceData.FRONT);
        }
        if ('BACK' in deviceData) {
            _backCamera = initCamera(deviceData.BACK);
        }
    }

    function initCamera(cameraData) {
        return new Camera(_ezAR,cameraData); 
    }


    return _ezAR;

}());
