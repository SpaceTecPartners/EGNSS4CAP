/*
 * CDVezARCameraViewController.h
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

#import <UIKit/UIKit.h>
#import <AVFoundation/AVFoundation.h>
#import "Cordova/CDV.h"

@interface CDVezARCameraViewController : UIViewController

-(CDVezARCameraViewController*) initWithController: (CDVViewController*) mainViewController
                                           session:(AVCaptureSession*) captureSession;

@end
