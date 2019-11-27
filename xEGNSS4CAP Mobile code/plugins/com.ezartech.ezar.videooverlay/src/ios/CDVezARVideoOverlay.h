/**
 * CDVezARVideoOverlay.h
 *
 * Copyright 2015, ezAR Technologies
 * http://ezartech.com
 *
 * By @wayne_parrott, @vridosh, @kparrott
 *
 * Licensed under a modified MIT license. 
 * Please see LICENSE or http://ezartech.com/ezarstartupkit-license for more information
 */

#import <AVFoundation/AVFoundation.h>

#import "Cordova/CDV.h"

/**
 * Implements the ezAR Cordova api. 
 */
@interface CDVezARVideoOverlay : CDVPlugin

- (void) init:(CDVInvokedUrlCommand*)command;

- (UIColor *) getBackgroundColor;

- (void) getCameras:(CDVInvokedUrlCommand*)command;

- (void) activateCamera:(CDVInvokedUrlCommand*)command;

- (void) deactivateCamera:(CDVInvokedUrlCommand*)command;

- (BOOL) isCameraRunning;

- (BOOL) isBackCameraRunning;

- (BOOL) isFrontCameraRunning;

- (void) startCamera:(CDVInvokedUrlCommand*)command;

- (void) stopCamera:(CDVInvokedUrlCommand*)command;

- (void) getZoom:(CDVInvokedUrlCommand*)command;

- (void) setZoom:(CDVInvokedUrlCommand*)command;

- (void) setFocus:(CDVInvokedUrlCommand*)command;

- (void) resetFocus:(CDVInvokedUrlCommand*)command;

- (UIImageView *) getCameraView;

- (AVCaptureSession *) getAVCaptureSession;

- (AVCaptureStillImageOutput *) getAVCaptureStillImageOutput;

@end

typedef NS_ENUM(NSUInteger, EZAR_ERROR_CODE) {
    EZAR_ERROR_CODE_ERROR=1,
    EZAR_ERROR_CODE_INVALID_ARGUMENT,
    EZAR_ERROR_CODE_INVALID_STATE,
    EZAR_ERROR_CODE_ACTIVATION
};

typedef NS_ENUM(NSUInteger, EZAR_IMAGE_ENCODING) {
    EZAR_IMAGE_ENCODING_JPG=0,
    EZAR_IMAGE_ENCODING_PNG
};



