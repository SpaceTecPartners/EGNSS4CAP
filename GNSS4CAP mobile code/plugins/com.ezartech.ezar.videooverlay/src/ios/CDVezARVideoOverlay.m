/*
 * CDVezARVideoOverlay.m
 *
 * Copyright 2015, ezAR Technologies
 * http://ezartech.com
 *
 * By @wayne_parrott, @vridosh, @kwparrott
 *
 * Licensed under a modified MIT license. 
 * Please see LICENSE or http://ezartech.com/ezarstartupkit-license for more information
 *
 */

//#import <WebKit/WebKit.h>

#import "CDVezARVideoOverlay.h"
#import "CDVezARCameraViewController.h"
#import "MainViewController.h"


NSString *const EZAR_ERROR_DOMAIN = @"EZAR_ERROR_DOMAIN";
NSInteger const EZAR_CAMERA_VIEW_TAG = 999;

@implementation CDVezARVideoOverlay
{
    CDVezARCameraViewController* camController;
    AVCaptureSession *captureSession;
    AVCaptureDevice  *backVideoDevice, *frontVideoDevice, *videoDevice;
    AVCaptureDeviceInput *backVideoDeviceInput, *frontVideoDeviceInput, *videoDeviceInput;
    AVCaptureStillImageOutput *stillImageOutput;
    UIColor *bgColor;
}


// INIT PLUGIN - does nothing atm
- (void) pluginInitialize
{
    [super pluginInitialize];
}

// SETUP EZAR 
// Create camera view and preview, make webview transparent.
// return camera, zoom features and display details
// 
- (void)init:(CDVInvokedUrlCommand*)command
{
    //set webview background color for restoring later, default is WHITE
    NSString *bgColorRGB = [command argumentAtIndex:0 withDefault:@"#FFFFFF"];
    bgColor = [self colorFromHexString: bgColorRGB];
    
    
    //set main view background to black; otherwise white area appears during rotation
    self.viewController.view.backgroundColor = [UIColor blackColor];
 
    //impl for Brandon B problem
    //   videoOverlay accessed from inappbrowser which reloads all plugins. 
    //   Thus prevent new captureSession and cameraView creation
    if (!captureSession) { 
        // SETUP CAPTURE SESSION -----
        NSLog(@"Setting up capture session");
        captureSession = [[AVCaptureSession alloc] init];
        
        NSError *error;
        NSArray *devices = [AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo];
        for (AVCaptureDevice *device in devices) {
            if (error) break;
            if ([device position] == AVCaptureDevicePositionBack) {
                backVideoDevice = device;
                backVideoDeviceInput =
                    [AVCaptureDeviceInput deviceInputWithDevice:backVideoDevice error: &error];
            } else if ([device position] == AVCaptureDevicePositionFront) {
                frontVideoDevice = device;
                frontVideoDeviceInput=
                    [AVCaptureDeviceInput deviceInputWithDevice:frontVideoDevice error:&error];
            }
        }
        
        if (error) {
            NSDictionary* errorResult = [self makeErrorResult: 1 withError: error];
            
            CDVPluginResult* pluginResult =
            [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                            messageAsDictionary: errorResult];
            
            return  [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
        }
        
        //SETUP CameraViewController
        camController =[[CDVezARCameraViewController alloc]
                        initWithController: (CDVViewController*)self.viewController
                        session: captureSession];
        camController.view;
        camController.view.tag = EZAR_CAMERA_VIEW_TAG;
    }

     //set main view background to black; otherwise white area appears during rotation
    self.viewController.view.backgroundColor = [UIColor blackColor];
    
    //MAKE WEBVIEW TRANSPARENT
    self.webView.opaque = NO;
    self.webView.backgroundColor = [self getBackgroundColor];
   
    [self forceWebViewRedraw];
    
    //ACCESS DEVICE INFO: CAMERAS, ...
    NSDictionary* deviceInfoResult = [self basicGetDeviceInfo];
    
    CDVPluginResult* pluginResult =
        [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary: deviceInfoResult];
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

//
// Return device Camera details
//
- (UIColor *)getBackgroundColor
{
    if (!bgColor) bgColor = [UIColor whiteColor];
    return bgColor;
}

//
// Return device Camera details
//
- (void)getCameras:(CDVInvokedUrlCommand*)command
{
    NSDictionary* cameras = [self basicGetCameras];
    
    CDVPluginResult* pluginResult =
        [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary: cameras];
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


//
// Set camera as the default 
//
- (void)activateCamera:(CDVInvokedUrlCommand*)command
{
    NSString* cameraPos = [command.arguments objectAtIndex:0];

    NSNumber* zoomArg = [command.arguments objectAtIndex:1];
    double zoomLevel = [zoomArg doubleValue];
    
    //todo add error handling
    NSError *error;
    [self basicActivateCamera: cameraPos zoom: zoomLevel error: error];

    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


//
//
//
- (void)deactivateCamera:(CDVInvokedUrlCommand*)command
{
    [self basicDeactivateCamera];
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


//
//
//
- (BOOL) isCameraRunning
{
    return self.getAVCaptureSession && [self.getAVCaptureSession isRunning];
}

- (BOOL) isFrontCameraRunning
{
    return [self isCameraRunning] && videoDevice && videoDevice == frontVideoDevice;
}

- (BOOL) isBackCameraRunning
{
    return [self isCameraRunning] && videoDevice && videoDevice == backVideoDevice;
}


//
//
//
- (void)startCamera:(CDVInvokedUrlCommand*)command
{
    NSString* cameraPos = [command.arguments objectAtIndex:0];
    
    NSNumber* zoomArg = [command.arguments objectAtIndex:1];
    double zoomLevel = [zoomArg doubleValue];
    
    [self basicDeactivateCamera]; //stops camera if running before deactivation
    
    NSError *error;
    [self basicActivateCamera: cameraPos zoom: zoomLevel error: error];
    
    if (error) {
        
    }

    //SET WEBVIEW BACKGROUND transparent
    self.webView.backgroundColor = [UIColor clearColor];
    
    //START THE CAPTURE SESSION
    [captureSession startRunning];
    
     CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


//
//
//
- (void)stopCamera:(CDVInvokedUrlCommand*)command
{
    [self basicStopCamera];
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


//
//
//
/*
- (void) maxZoom:(CDVInvokedUrlCommand*)command
{
    CGFloat result = videoDeviceInput.device.activeFormat.videoZoomFactorUpscaleThreshold;
 
    CDVPluginResult* pluginResult =
        [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDouble: result ];
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}
*/

//
//
//
- (void) getZoom:(CDVInvokedUrlCommand*)command
{
    double zoomLevel = videoDevice.videoZoomFactor;
    
    CDVPluginResult* pluginResult =
        [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDouble: zoomLevel ];

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}


//
//
//
- (void) setZoom:(CDVInvokedUrlCommand*)command
{
    NSNumber* zoomArg = [command.arguments objectAtIndex:0];
    double zoomLevel = [zoomArg doubleValue];
    
    [self basicSetZoom: zoomLevel];
    
    CDVPluginResult* pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) setFocus:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult;
    NSError *error = NULL;
    
    double x = [[command argumentAtIndex:0] doubleValue];
    double y = [[command argumentAtIndex:1] doubleValue];
    
    CGRect screenRect = [[UIScreen mainScreen] bounds];
    int screenWidth = screenRect.size.width;
    int screenHeight = screenRect.size.height;
    double focus_x = x/(double)screenWidth;
    double focus_y = y/(double)screenHeight;
    
    CGPoint pt = CGPointMake(focus_x,focus_y);
    
    if ([videoDevice lockForConfiguration:&error]) {
        
        if ([videoDevice isFocusPointOfInterestSupported] && [videoDevice isFocusModeSupported:AVCaptureFocusModeAutoFocus]) {
            videoDevice.focusPointOfInterest = pt;
            videoDevice.focusMode = AVCaptureFocusModeAutoFocus;
            
            if ([videoDevice isExposurePointOfInterestSupported] && [videoDevice isExposureModeSupported:AVCaptureExposureModeAutoExpose]) {
                videoDevice.exposurePointOfInterest = pt;
                videoDevice.exposureMode =AVCaptureExposureModeAutoExpose;
            }
            
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        } else {
            NSDictionary* errorResult = [self makeErrorResult:EZAR_ERROR_CODE_ERROR withData:@"Camera does not support focus mode."];
            pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                          messageAsDictionary: errorResult];
        }

        [videoDevice unlockForConfiguration];
        
    } else {
        //error, no device lock obtained
        NSDictionary* errorResult = [self makeErrorResult:EZAR_ERROR_CODE_INVALID_STATE withData:@"Invalid camera state"];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR
                                     messageAsDictionary: errorResult];
    }
    
    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (void) resetFocus:(CDVInvokedUrlCommand*)command
{
    CDVPluginResult* pluginResult;
    NSError *error = NULL;

    if ([videoDevice lockForConfiguration:&error]) {
        
        if ([videoDevice isFocusModeSupported:AVCaptureFocusModeContinuousAutoFocus]) {
            videoDevice.focusMode = AVCaptureFocusModeContinuousAutoFocus;
        } else if ([videoDevice isFocusModeSupported:AVCaptureFocusModeAutoFocus]) {
            videoDevice.focusMode = AVCaptureFocusModeAutoFocus;
        }
        
        if ([videoDevice isExposureModeSupported:AVCaptureExposureModeContinuousAutoExposure]) {
            videoDevice.exposureMode = AVCaptureExposureModeContinuousAutoExposure;
        } else if ([videoDevice isExposureModeSupported:AVCaptureExposureModeAutoExpose]) {
            videoDevice.exposureMode = AVCaptureExposureModeAutoExpose;
        }
        
        [videoDevice unlockForConfiguration];
    
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK];
        
    } else {
        //error, no device lock obtained
        NSDictionary* errorResult = [self makeErrorResult:EZAR_ERROR_CODE_INVALID_STATE withData:@"Invalid camera state"];
        pluginResult = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsDictionary: errorResult];
    }

    [self.commandDelegate sendPluginResult:pluginResult callbackId:command.callbackId];
}

- (UIImageView *) getCameraView
{
    //UIImageView* cameraView = camController.view;
    UIImageView* cameraView = (UIImageView *)[self.viewController.view viewWithTag: EZAR_CAMERA_VIEW_TAG];
    return cameraView;
}

- (AVCaptureSession *) getAVCaptureSession
{
    return captureSession;
}

- (AVCaptureStillImageOutput *) getAVCaptureStillImageOutput
{
    return stillImageOutput;
}


//---------------------------------------------------------------
//
- (NSDictionary*)basicGetDeviceInfo
{
    NSMutableDictionary* deviceInfo =
    	[NSMutableDictionary dictionaryWithDictionary: [self basicGetCameras]];

    CGRect screenRect = [[UIScreen mainScreen] bounds];
    CGFloat screenWidth = screenRect.size.width;
    CGFloat screenHeight = screenRect.size.height;

    [deviceInfo setObject: @(screenWidth) forKey:@"displayWidth"];
    [deviceInfo setObject: @(screenHeight) forKey:@"displayHeight"];

    return deviceInfo;
}


//
//
//
- (NSDictionary*)basicGetCameras
{
    NSMutableDictionary* cameraInfo = [NSMutableDictionary dictionaryWithCapacity:4];

    NSArray *cameras = [AVCaptureDevice devicesWithMediaType:AVMediaTypeVideo];
    for (AVCaptureDevice *camera in cameras) {
        if ([camera position] == AVCaptureDevicePositionFront) {
            [cameraInfo setObject: [self basicGetCameraProps: camera]  forKey:@"FRONT"];
        } else if ([camera position] == AVCaptureDevicePositionBack) {
            [cameraInfo setObject: [self basicGetCameraProps: camera]  forKey:@"BACK"];
        }
    }

    return cameraInfo;
}


//
//
//
- (NSDictionary*)basicGetCameraProps: (AVCaptureDevice *)camera
{
    NSMutableDictionary* cameraProps = [NSMutableDictionary dictionaryWithCapacity:4];
    [cameraProps setObject: camera.uniqueID forKey:@"id"];
    
    [cameraProps setObject: @(camera.activeFormat.videoMaxZoomFactor) forKey:@"maxZoom"];
    [cameraProps setObject: @(camera.videoZoomFactor) forKey:@"zoom"];
    
    if ([camera position] == AVCaptureDevicePositionFront) {
        [cameraProps setObject: @"FRONT" forKey:@"position"];
    } else if ([camera position] == AVCaptureDevicePositionBack) {
        [cameraProps setObject: @"BACK" forKey:@"position"];
    }

    float hpov = camera.activeFormat.videoFieldOfView;
    [cameraProps setObject: @(hpov) forKey:@"horizontalViewAngle"];

    CGRect screenBounds = [[UIScreen mainScreen] bounds];
    float aspectRatio = screenBounds.size.width / screenBounds.size.height;
    float invertAspectRatio = aspectRatio < 1.0 ? aspectRatio : 1.0 / aspectRatio;
    //float vpov = hpov * invertAspectRatio;
    float vpov = 2.0 * atanf( tanf(hpov/2.0 * M_PI/180.0) * invertAspectRatio) * 180.0/M_PI;
    [cameraProps setObject: @(vpov) forKey:@"verticalViewAngle"];

    return cameraProps;
}


//
//
//
- (void)basicActivateCamera: (NSString*)cameraPos zoom: (double)zoomLevel error: (NSError*) error
{
    videoDevice = nil;
    videoDeviceInput = nil;
    
    if ([cameraPos caseInsensitiveCompare: @"FRONT"] == NSOrderedSame) {
        videoDevice = frontVideoDevice;
        videoDeviceInput = frontVideoDeviceInput;
    } else  if ([cameraPos caseInsensitiveCompare: @"BACK"] == NSOrderedSame) {
        videoDevice = backVideoDevice;
        videoDeviceInput = backVideoDeviceInput;
    }
    
    if (!videoDevice) {
        error = [NSError errorWithDomain: EZAR_ERROR_DOMAIN
                                code: EZAR_ERROR_CODE_INVALID_ARGUMENT
                                userInfo: @{@"description": @"No camera found"}];
        return;
    }
   
    if ([captureSession canAddInput:videoDeviceInput]) {
        
        [captureSession addInput:videoDeviceInput];
            
        if ([videoDevice lockForConfiguration: &error]) {
                
            //configure focus
            if ([videoDevice isFocusModeSupported: AVCaptureFocusModeContinuousAutoFocus]) {
                videoDevice.focusMode = AVCaptureFocusModeContinuousAutoFocus;
            }
                
            if ([videoDevice isExposureModeSupported: AVCaptureExposureModeContinuousAutoExposure]) {
                videoDevice.exposureMode = AVCaptureExposureModeContinuousAutoExposure;
            } else if ([videoDevice isExposureModeSupported: AVCaptureExposureModeAutoExpose]) {
                videoDevice.exposureMode = AVCaptureExposureModeAutoExpose;
            }
                
            [self basicSetZoom: zoomLevel];
                
            [videoDevice unlockForConfiguration];
        }
        
        //configure to capture a video frame
        stillImageOutput = [[AVCaptureStillImageOutput alloc] init];
        NSDictionary *outputSettings =
            [[NSDictionary alloc] initWithObjectsAndKeys: AVVideoCodecJPEG, AVVideoCodecKey, nil];
        [stillImageOutput setOutputSettings:outputSettings];
        [captureSession addOutput: stillImageOutput];
        
        
    } else
    {
        error = [NSError errorWithDomain: EZAR_ERROR_DOMAIN
                                code: EZAR_ERROR_CODE_ACTIVATION
                                userInfo: @{@"description": @"Unable to activate camera"}];
    }
                 
}


//
//
//
- (void)basicDeactivateCamera
{
    //stop the session
    //remove the current video device from the session
    [self basicStopCamera];
    
    [captureSession removeInput: videoDeviceInput];
    [captureSession removeOutput:stillImageOutput];
    videoDevice = nil;
    videoDeviceInput = nil;
    stillImageOutput = nil;
    
}


//
//
//
- (void)basicStopCamera
{
    if ([self isCameraRunning]) {
        self.webView.backgroundColor = [self getBackgroundColor];
        
        //----- STOP THE CAPTURE SESSION RUNNING -----
        [captureSession stopRunning];
    }
}


//
//
//
- (void) basicSetZoom:(double) zoomLevel
{
    if ([videoDevice lockForConfiguration:nil]) {
        [videoDevice setVideoZoomFactor: MAX(1.0,zoomLevel)];
        [videoDevice unlockForConfiguration];
    }
}

//
//
//
- (NSDictionary*)makeErrorResult: (EZAR_ERROR_CODE) errorCode withData: (NSString*) description
{
    NSMutableDictionary* errorData = [NSMutableDictionary dictionaryWithCapacity:4];
    
    [errorData setObject: @(errorCode)  forKey:@"code"];
    [errorData setObject: @{ @"description": description}  forKey:@"data"];
    
    return errorData;
}

//
//
//
- (NSDictionary*)makeErrorResult: (EZAR_ERROR_CODE) errorCode withError: (NSError*) error
{
    NSMutableDictionary* errorData = [NSMutableDictionary dictionaryWithCapacity:2];
    [errorData setObject: @(errorCode)  forKey:@"code"];
    
     NSMutableDictionary* data = [NSMutableDictionary dictionaryWithCapacity:2];
    [data setObject: [error.userInfo objectForKey: NSLocalizedFailureReasonErrorKey] forKey:@"description"];
    [data setObject: @(error.code) forKey:@"iosErrorCode"];
    
    [errorData setObject: data  forKey:@"data"];
    
    return errorData;
}

//warning! total hack - setting transparency in web doc does not immediately take effect.
//The hack is to toggle <body> display to none and then to block causes full repaint.
//  
- (void)forceWebViewRedraw 
{
    NSString *jsstring =
        @"document.body.style.display='none';"
         "setTimeout(function(){document.body.style.display='block'},10);";
    
    if ([self.webView isKindOfClass:[UIWebView class]]) {
        [(UIWebView*)self.webView stringByEvaluatingJavaScriptFromString: jsstring];
    }
    
    //todo add logic to account for wkwebview
    /* else if ([self.webView isKindOfClass:[WKWebView class]]) {
        [(WKWebView*)self.webView stringByEvaluatingJavaScriptFromString: jsstring];
    }
    */
}


// Assumes input like #RRGGBB
- (UIColor *)colorFromHexString:(NSString *)hexString {
    unsigned rgbValue = 0;
    NSScanner *scanner = [NSScanner scannerWithString:hexString];
    [scanner setScanLocation:1]; // bypass '#' character
    [scanner scanHexInt:&rgbValue];
    return [UIColor colorWithRed:((rgbValue & 0xFF0000) >> 16)/255.0 green:((rgbValue & 0xFF00) >> 8)/255.0 blue:(rgbValue & 0xFF)/255.0 alpha:1.0];
}


@end
