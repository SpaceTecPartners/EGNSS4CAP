/**
 * CDVezARCameraViewController.m
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

#include "CDVezARCameraViewController.h"

@implementation CDVezARCameraViewController
{
    CDVViewController* _mainController;
    AVCaptureSession* _captureSession;
    AVCaptureVideoPreviewLayer *_previewLayer;
}

-(CDVezARCameraViewController*) initWithController: (CDVViewController*) mainViewController
                                           session:(AVCaptureSession*) captureSession;
{
    //[super init];
    _mainController = mainViewController;
    _captureSession = captureSession;
    
    //register for CAPTURESESSIONSTARTED events
    //see captureSessionStarted for explanation
    NSNotificationCenter *nc = [NSNotificationCenter defaultCenter];
    [nc addObserver:self
          selector:@selector(captureSessionStarted:)
           name:AVCaptureSessionDidStartRunningNotification
           object:_captureSession];
    
    return self;
}

//********** CAPTURE SESSION STARTED **********
//HACK: set video preview orientation when the 1st capture session starts.
//      Earlier attempts to set video preview orienation during layout failed
//      so do it when the _captureSession is started. Only need the 1st event
//      thus remove the event registration immediately after receiveing the 1st callback.
- (void)captureSessionStarted:(NSNotification *)note
{
    [self updatePreviewOrientation: [self getUIInterfaceOrientation]];
    
    NSNotificationCenter *nc = [NSNotificationCenter defaultCenter];
    [nc removeObserver:self
        name: AVCaptureSessionDidStartRunningNotification
        object: _captureSession];
    
}

-(void)loadView
{
    //ADD VIDEO PREVIEW LAYER
    _previewLayer = [[AVCaptureVideoPreviewLayer alloc] initWithSession: _captureSession] ;
    [_previewLayer setVideoGravity:AVLayerVideoGravityResizeAspectFill];
    _previewLayer.frame = _mainController.view.frame;
    
    //create camera with no default image
    UIImageView *cameraView = [[UIImageView alloc] initWithFrame:_mainController.view.frame ];
    [[cameraView layer] addSublayer: _previewLayer];
    cameraView.autoresizingMask = (UIViewAutoresizingFlexibleWidth | UIViewAutoresizingFlexibleHeight);
    cameraView.backgroundColor = [UIColor blackColor];
    
    cameraView.frame = _mainController.view.frame;
    
    self.view = cameraView;
    
    [_mainController addChildViewController: self];
    [self didMoveToParentViewController: _mainController];
    
    //POSITION cameraview below the webview
    [_mainController.view insertSubview: cameraView belowSubview: _mainController.webView];
    
}

//do nothing atm
- (void)viewWillAppear:(BOOL)animated
{
    [super viewWillAppear: animated];
}

//do nothing atm
- (void)viewDidAppear:(BOOL)animated
{
    [super viewWillAppear: animated];
}

- (void)viewWillLayoutSubviews
{
    CGRect webViewFrame = _mainController.webView.frame;
   _previewLayer.frame = webViewFrame;
    
    //hack: FORCE webview to redraw by resizing
    CGRect webviewFramePrime =
        CGRectMake(webViewFrame.origin.x, webViewFrame.origin.y, webViewFrame.size.width, webViewFrame.size.height-1);
    _mainController.webView.frame = webviewFramePrime;
    _mainController.webView.frame = webViewFrame;
}

//do nothing atm
- (void)viewDidLayoutSubviews
{
    [super viewDidLayoutSubviews];
}

//called automatically during rotation
- (void)willAnimateRotationToInterfaceOrientation:(UIInterfaceOrientation)toInterfaceOrientation
                        duration:(NSTimeInterval)duration {

    [self updatePreviewOrientation: toInterfaceOrientation];
}

-(void)updatePreviewOrientation:(UIInterfaceOrientation)interfaceOrientation
{
    AVCaptureVideoOrientation videoOrient = [self videoOrientationFromUIInterfaceOrientation: interfaceOrientation];
    
    //shouldAutorotateToInterfaceOrientation is incorrectly returning NO 
    //As a result replacing with custom orientation support check
    //BOOL shouldRotate = [_mainController shouldAutorotateToInterfaceOrientation: interfaceOrientation];
    BOOL shouldRotate = orientationMaskSupportsOrientation(
                                [_mainController supportedInterfaceOrientations],
                                interfaceOrientation);
    
    if (shouldRotate && videoOrient != _previewLayer.connection.videoOrientation) {
        _previewLayer.connection.videoOrientation = videoOrient;
    }
}
                                                            
BOOL orientationMaskSupportsOrientation(UIInterfaceOrientationMask mask, UIInterfaceOrientation orientation)
{
    return (mask & (1 << orientation)) != 0;
}

- (UIInterfaceOrientation)getUIInterfaceOrientation
{
    //return self.interfaceOrientation;
    return [UIApplication sharedApplication].statusBarOrientation;
}

- (AVCaptureVideoOrientation)videoOrientationFromUIInterfaceOrientation: (UIInterfaceOrientation) ifOrientation
{
    AVCaptureVideoOrientation videoOrientation;
    switch (ifOrientation) {
        case UIInterfaceOrientationPortrait:
            videoOrientation = AVCaptureVideoOrientationPortrait;
            break;
        case UIInterfaceOrientationPortraitUpsideDown:
            videoOrientation = AVCaptureVideoOrientationPortraitUpsideDown;
            break;
        case UIInterfaceOrientationLandscapeRight:
            videoOrientation = AVCaptureVideoOrientationLandscapeRight;
            break;
        case UIInterfaceOrientationLandscapeLeft:
            videoOrientation = AVCaptureVideoOrientationLandscapeLeft;
            break;
        default:
            videoOrientation = AVCaptureVideoOrientationPortrait;
    }
    
    return videoOrientation;
}

@end