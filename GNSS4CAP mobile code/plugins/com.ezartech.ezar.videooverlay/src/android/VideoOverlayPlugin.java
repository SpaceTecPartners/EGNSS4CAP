/**
 * Copyright 2016, ezAR Technologies
 * http://ezartech.com
 *
 * By @wayne_parrott, @vridosh, @kwparrott
 *
 * Licensed under a modified MIT license.
 * Please see LICENSE or http://ezartech.com/ezarstartupkit-license for more information
 *
 */
package com.ezartech.ezar.videooverlay;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.hardware.Camera.Parameters;
import android.service.media.CameraPrewarmService;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.FrameLayout;


/**
 * Implements the ezAR VideoOverlay api for Android.
 */
public class VideoOverlayPlugin extends CordovaPlugin implements Camera.PreviewCallback {
	private static final String TAG = "ezAR";

	static final boolean EXPERIMENTAL = false;

	//event type code
	static final int UNDEFINED = -1;
	static final int STARTED = 0;
	static final int STOPPED = 1;

	static int PREVIEW_FORMAT = ImageFormat.NV21;
	static int FRAME_BUFFER_CNT = 2;

	private CallbackContext callbackContext;

	private Activity activity;
	private FrameLayout cameraViewContainer;
	private View webViewView;
	private TextureView cameraView;

	static final String DEFAULT_RGB = "#FFFFFF";
	private int bgColor = Color.WHITE;;

	private Camera camera = null;
	private int cameraId = -1;
	private CameraDirection cameraDirection;
	private int displayOrientation;
	private String defaultFocusMode;
	private SizePair previewSizePair = null;
	private double currentZoom;
	private boolean isPreviewing = false;
	private boolean isPaused = false;

	private VideoOverlayFrameListener frameListener;

	private boolean supportSnapshot;
	private Matrix matrix;
	private boolean continousFocusSupported;
	private boolean focusAreaSupported;
	private boolean meteringAreaSupported;
	private boolean aeLockSupported;
	private boolean awbLockSupported;

	protected final static String[] permissions = {Manifest.permission.CAMERA};
	public final static int PERMISSION_DENIED_ERROR = 20;
	public final static int CAMERA_SEC = 0;

	boolean surfaceDestroyed = true;

	private View.OnLayoutChangeListener layoutChangeListener =
			new View.OnLayoutChangeListener() {
				public void onLayoutChange (View v, int left, int top, int right, int bottom,
									         int oldLeft, int oldTop, int oldRight, int oldBottom) {
					Log.d(TAG,"layout change: " + left + ":" + top + ":" + right + ":" + bottom);
					if (left == oldLeft && top == oldTop && right == oldRight && bottom == oldBottom) {
						//do nothing
						return;
					}

					if (isPreviewing()) {
						updateCordovaViewContainerSize();
						updateMatrix();
						resetFocus(null);
					} else {
           				resetCameraViewContainerSize();
          			}
				}
			};

	/**
	 * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a
	 * {@link TextureView}.
	 */
	private TextureView.SurfaceTextureListener mSurfaceTextureListener =
			new TextureView.SurfaceTextureListener() {

				@Override
				public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture,
													  int width, int height) {
					Log.d(TAG,"onSurfaceTextureAvail, isPreviewing: " + isPreviewing());
					surfaceDestroyed = false;

					if (isPreviewing()) { //only called from onResume
						forcePreviewRestart();
					}
				}

				@Override
				public void onSurfaceTextureSizeChanged(SurfaceTexture surfaceTexture,
														int width, int height) {
					Log.v(TAG,"SURFACE TEXTURE size changed");

				}

				@Override
				public boolean onSurfaceTextureDestroyed(SurfaceTexture surfaceTexture) {
					//Log.v(TAG,"SURFACE TEXTURE DESTROYED");
					surfaceDestroyed = true;
					return true;
				}

				@Override
				public void onSurfaceTextureUpdated(SurfaceTexture surfaceTexture) {
					//Log.v(TAG,"SURFACE TEXTURE ");
				}

			};

	@Override
	public void initialize(final CordovaInterface cordova, final CordovaWebView cvWebView) {
		super.initialize(cordova, cvWebView);

		webViewView = cvWebView.getView();

		activity = cordova.getActivity();
		activity.runOnUiThread(new Runnable() {
			@Override
			public void run() {

				//configure webview
				webViewView.setKeepScreenOn(true);

				Log.d(TAG,"WebView HW accelerated: " + webViewView.isHardwareAccelerated());
				// if (webViewView.isHardwareAccelerated()) {
				// 	webViewView.setLayerType(WebView.LAYER_TYPE_HARDWARE, null);
				// } else {
				// 	webViewView.setLayerType(WebView.LAYER_TYPE_SOFTWARE, null);
				// }

				//temporarily remove webview from view stack
				((ViewGroup) webViewView.getParent()).removeView(webViewView);

				if (EXPERIMENTAL) {
					cameraViewContainer =
							new AspectRatioFrameLayout(cordova.getActivity().getApplicationContext());
					cameraViewContainer.setForegroundGravity(Gravity.CENTER);
					FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
							LayoutParams.MATCH_PARENT,
							LayoutParams.MATCH_PARENT,
							Gravity.CENTER);
					activity.setContentView(cameraViewContainer, params);
				} else {
					cameraViewContainer = new FrameLayout(activity);
					cameraViewContainer.setBackgroundColor(Color.BLACK);
					activity.setContentView(cameraViewContainer,
							new ViewGroup.LayoutParams(
									LayoutParams.MATCH_PARENT,
									LayoutParams.MATCH_PARENT));
				}

				//create & add videoOverlay to view stack

				cameraView = new TextureView(activity);
				cameraView.setSurfaceTextureListener(mSurfaceTextureListener);
				cameraViewContainer.addView(cameraView,
						new ViewGroup.LayoutParams(
								LayoutParams.MATCH_PARENT,
								LayoutParams.MATCH_PARENT));


				//add webview on top of videoOverlay
				cameraViewContainer.addView(webViewView,
						new ViewGroup.LayoutParams(
								LayoutParams.MATCH_PARENT,
								LayoutParams.MATCH_PARENT));
				if("org.xwalk.core.XWalkView".equals(webViewView.getClass().getName())
						|| "org.crosswalk.engine.XWalkCordovaView".equals(webViewView.getClass().getName())) {
					try {
					/* view.setZOrderOnTop(true)
					 * Called just in time as with root.setBackground(...) the color
					 * come in front and take the whoel screen */
						webViewView.getClass().getMethod("setZOrderOnTop", boolean.class)
								.invoke(webViewView, true);
					}
					catch(Exception e) {}
				}
				((FrameLayout) cameraViewContainer.getParent()).addOnLayoutChangeListener(layoutChangeListener);
				((FrameLayout) cameraViewContainer.getParent()).setBackgroundColor(Color.BLACK);
			}
		});
	}


	@Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		Log.d(TAG, action + " " + args.length());

		if (action.equals("init")) {
			this.init(args, callbackContext);
			return true;
		} else if (action.equals("startCamera")) {
			this.startPreview(
					args.getString(0),
					getDoubleOrNull(args, 1),
					callbackContext);

			return true;
		} else if (action.equals("stopCamera")) {
			this.stopPreview(callbackContext);

			return true;
		} else if (action.equals("setZoom")) {
			this.setZoom(getDoubleOrNull(args, 0), callbackContext);

			return true;
		} else if (action.equals("setFocus")) {
			this.setFocus(getIntOrNull(args, 0), getIntOrNull(args, 1), callbackContext);

			return true;
		} else if (action.equals("resetFocus")) {
			this.resetFocus(callbackContext);

			return true;
		}

		return false;
	}

	private void init(JSONArray args, final CallbackContext callbackContext) {
		this.callbackContext = callbackContext;

		supportSnapshot = getSnapshotPlugin() != null;

		if (args != null) {
			String rgb = DEFAULT_RGB;
      		boolean fitWebViewToCameraViewArg = true;

			try {
				rgb = args.getString(0);
        		fitWebViewToCameraViewArg = args.getBoolean(1);
			} catch (JSONException e) {
				//do nothing; resort to DEFAULT_RGB
			}

			final boolean fitWebViewToCameraView = fitWebViewToCameraViewArg;
			setBackgroundColor(Color.parseColor(rgb));
			cordova.getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					webViewView.setBackgroundColor(getBackgroundColor());

				  if (!fitWebViewToCameraView) {
						cameraViewContainer.removeView(webViewView);
						((FrameLayout) cameraViewContainer.getParent()).addView(webViewView,
							new ViewGroup.LayoutParams(
								LayoutParams.MATCH_PARENT,
								LayoutParams.MATCH_PARENT));
				   }
				}
			});
		}

		if (!PermissionHelper.hasPermission(this, permissions[0])) {
			PermissionHelper.requestPermission(this, CAMERA_SEC, Manifest.permission.CAMERA);
			return;
		}

		JSONObject jsonObject = new JSONObject();
		try {
			Display display = activity.getWindowManager().getDefaultDisplay();
			DisplayMetrics m = new DisplayMetrics();
			display.getMetrics(m);

			jsonObject.put("displayWidth", m.widthPixels);
			jsonObject.put("displayHeight", m.heightPixels);

			int mNumberOfCameras = Camera.getNumberOfCameras();
			Log.d(TAG, "Cameras:" + mNumberOfCameras);

			// Find the ID of the back-facing ("default") camera
			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
			for (int i = 0; i < mNumberOfCameras; i++) {
				Camera.getCameraInfo(i, cameraInfo);

				Parameters parameters;
				Camera open = null;
				try {
					open = Camera.open(i);
					parameters = open.getParameters();
				} finally {
					if (open != null) {
						open.release();
					}
				}

				Log.d(TAG, "Camera facing:" + cameraInfo.facing);

				CameraDirection type = null;
				for (CameraDirection f : CameraDirection.values()) {
					if (f.getDirection() == cameraInfo.facing) {
						type = f;
					}
				}

				if (type != null) {
					double zoom = 0;
					double maxZoom = 0;
					if (parameters.isZoomSupported()) {
						maxZoom = (parameters.getMaxZoom() + 1) / 10.0;
						zoom = Math.min(parameters.getZoom() / 10.0 + 1, maxZoom);
					}

					float hpov = parameters.getHorizontalViewAngle();
					float vpov = parameters.getVerticalViewAngle();

					JSONObject jsonCamera = new JSONObject();
					jsonCamera.put("id", i);
					jsonCamera.put("position", type.toString());
					jsonCamera.put("zoom", zoom);
					jsonCamera.put("maxZoom", maxZoom);
					jsonCamera.put("horizontalViewAngle", hpov);
					jsonCamera.put("verticalViewAngle", vpov);
					jsonObject.put(type.toString(), jsonCamera);
				}
			}
		} catch (JSONException e) {
			Log.e(TAG, "Can't set exception", e);
		}

		callbackContext.success(jsonObject);
	}

	//removed @Override, causes compile issue for cordova 5 & earlier that does not have this method.
	public void onRequestPermissionResult(int requestCode, String[] permissions,
										  int[] grantResults) throws JSONException {
		for (int r : grantResults) {
			if (r == PackageManager.PERMISSION_DENIED) {
				this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, PERMISSION_DENIED_ERROR));
				return;
			}
		}
		switch (requestCode) {
			case CAMERA_SEC:
				init(null,this.callbackContext);
				break;
		}
	}

	public synchronized void forcePreviewRestart() {
		setIsPreviewing(false); //must set isPreviewing false before calling startPreview else NOP occurs
		startPreview(cameraDirection, currentZoom, null);
	}

	private void startPreview(final String cameraDirName,
							  final double zoom,
							  final CallbackContext callbackContext) {

		CameraDirection cameraDir = CameraDirection.valueOf(cameraDirName);

		Log.d(TAG, "startPreview called " + cameraDir +
				" " + zoom +
				" " + "isShown " + cameraView.isShown() +
				" " + cameraView.getWidth() + ", " + cameraView.getHeight());

		startPreview(cameraDir, zoom, callbackContext);
	}


	private void startPreview(final CameraDirection cameraDir,
							  final double zoom,
							  final CallbackContext callbackContext) {

		Log.d(TAG,"start Preview");

		if (activity == null || activity.isFinishing()) {
			return;
		}

		if (isPreviewing()) {
			if (cameraId != getCameraId(cameraDir)) {
				stopPreview(null,false);
			}
		}

		matrix = new Matrix();
		cameraId = getCameraId(cameraDir);
		cameraDirection = cameraDir;

		if (cameraId != UNDEFINED) {
			camera = Camera.open(cameraId);
		}

		if (camera == null) {
			if (callbackContext != null) callbackContext.error("No camera available");
			return;
		}

		initCamera(camera);

		cordova.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				try {
					setIsPreviewing(true);
					updateCameraDisplayOrientation();

					//configure scaled CVG size & preview matrix
					updateCordovaViewContainerSize();
					camera.startPreview();
					webViewView.setBackgroundColor(Color.TRANSPARENT);

					camera.setPreviewCallback(VideoOverlayPlugin.this);

					setZoom(zoom, null);

					sendFlashlightEvent(STARTED, cameraDirection, cameraId, camera);
					sendFaceDetectorEvent(STARTED, cameraDirection, cameraId, camera);
					sendOpenCVEvent(STARTED, cameraDirection, cameraId, camera);

					if (callbackContext != null) {
						callbackContext.success();
					}

				} catch (Exception e) {
					Log.e(TAG, "Error during preview create", e);
					if (callbackContext != null) callbackContext.error(TAG + ": " + e.getMessage());
				}

			}

		});
	}

	private void stopPreview(final CallbackContext callbackContext) {
		stopPreview(callbackContext,true);
	}

	private void stopPreview(final CallbackContext callbackContext, final boolean updateWebView) {
		Log.d(TAG, "stopPreview called");

		if (!isPreviewing()) { //do nothing if not currently previewing
			if (callbackContext != null) {
				callbackContext.success();
			}
			return;
		}

		try {
			cordova.getActivity().runOnUiThread(new Runnable() {
				@Override
				public void run() {
					if (updateWebView) {
						webViewView.setBackgroundColor(getBackgroundColor());
						resetCameraViewContainerSize();
					}
				}
			});

			camera.cancelAutoFocus();
			camera.stopPreview();
			camera.setPreviewCallback(null);
			camera.setPreviewDisplay(null);
			sendFlashlightEvent(STOPPED, cameraDirection, cameraId, camera);
			sendFaceDetectorEvent(STOPPED, cameraDirection, cameraId, camera);
			sendOpenCVEvent(STOPPED, cameraDirection, cameraId, camera);
			camera.release();

		} catch (IOException e) {
			e.printStackTrace();
		}

		cameraId = UNDEFINED;
		camera = null;
		setIsPreviewing(false);

		if (callbackContext != null) {
			callbackContext.success();
		}
	}

	// when listener not null, camera must be running
	public void setFrameListener(VideoOverlayFrameListener listener) {
		frameListener = listener;

		if (frameListener == null) {
			camera.setPreviewCallbackWithBuffer(null);
		} else {
			//initialize frame buffers
			int sz = previewSizePair.previewSize.width * previewSizePair.previewSize.height *
						ImageFormat.getBitsPerPixel(PREVIEW_FORMAT) / 8;

			for (int i=0; i < FRAME_BUFFER_CNT; i++) {
				camera.addCallbackBuffer(new byte[sz]);
			}

			camera.setPreviewCallbackWithBuffer(this);
		}
	}

	public void onPreviewFrame(byte[] data, Camera camera) {
		if (frameListener == null) return;

		frameListener.frameReady( new FrameBufferHolder(data,camera) );
	}


	public int getBackgroundColor() {
		return bgColor;
	}

	private void setBackgroundColor(int color) {
		this.bgColor = color;
	}

	private void setZoom(final double newZoom, final CallbackContext callbackContext) {
		try {
			Parameters parameters = camera.getParameters();
			if (!parameters.isZoomSupported()) {
				//do nothing
				if (callbackContext != null) {
					callbackContext.success();
				}
				return;
			}

			int maxZoom = parameters.getMaxZoom();
			double normalizedNewZoom = Math.max(1.0, newZoom);
			float scale = (float)(10-1) / (float)maxZoom;
			int androidZoom = (int) Math.round((normalizedNewZoom - 1) / scale);
			androidZoom = Math.min(androidZoom, maxZoom);

			parameters.setZoom(androidZoom);
			camera.setParameters(parameters);
			currentZoom = normalizedNewZoom;

			if (callbackContext != null) {
				callbackContext.success();
			}
		} catch (Throwable e) {
			if (callbackContext != null) {
				callbackContext.error(e.getMessage());
			}
		}
	}

	public void setFocus(int x, int y, final CallbackContext callbackContext) {
		int FOCUS_AREA_WIDTH = 100, FOCUS_AREA_HT = 100;

		if (!isPreviewing()) {
			//error - not in preview mode

		} else if (!focusAreaSupported) {
			//todo inform user manual focus is not supported

			return;
		}

		Size sz = getWebViewSize();
		ArrayList<Area> focusArea = new ArrayList<Area>();
		focusArea.add(new Area(new Rect(), 1));
		ArrayList<Area> meteringArea = new ArrayList<Area>();
		meteringArea.add(new Area(new Rect(), 1));

		calculateFocusArea(FOCUS_AREA_WIDTH, FOCUS_AREA_HT, 1f, x, y, sz.width, sz.height,
				focusArea.get(0).rect);
		calculateFocusArea(FOCUS_AREA_WIDTH, FOCUS_AREA_HT, 1.5f, x, y, sz.width, sz.height,
				meteringArea.get(0).rect);

		Parameters parameters = camera.getParameters();
		parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
		parameters.setFocusAreas(focusArea);

		if (meteringAreaSupported) {
			// Use the same area for focus and metering.
			parameters.setMeteringAreas(meteringArea);
		}

		camera.setParameters(parameters);
		camera.autoFocus(null);
	}


	public void resetFocus(final CallbackContext callbackContext) {
		if (!isPreviewing() || !focusAreaSupported) {
			//do nothing
			return;
		}

		Parameters parameters = camera.getParameters();
		parameters.setFocusAreas(null);
		if (continousFocusSupported) {
			parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
		} else {
			parameters.setFocusMode(defaultFocusMode);
		}

		if (meteringAreaSupported) {
			parameters.setMeteringAreas(null);
		}

		camera.setParameters(parameters);
	}


	//reused from https://android.googlesource.com/platform/packages/apps/Camera/+/jb-release/src/com/android/camera/FocusManager.java
	private void calculateFocusArea(int focusWidth, int focusHeight, float areaMultiple,
									int x, int y, int previewWidth, int previewHeight,
									Rect rect) {
		int areaWidth = (int) (focusWidth * areaMultiple);
		int areaHeight = (int) (focusHeight * areaMultiple);
		int left = Util.clamp(x - areaWidth / 2, 0, previewWidth - areaWidth);
		int top = Util.clamp(y - areaHeight / 2, 0, previewHeight - areaHeight);
		RectF rectF = new RectF(left, top, left + areaWidth, top + areaHeight);
		matrix.mapRect(rectF);
		Util.rectFToRect(rectF, rect);
	}


	private void initCamera(Camera camera) {
		Camera.Parameters cameraParameters = camera.getParameters();

		defaultFocusMode = cameraParameters.getFocusMode();

		continousFocusSupported = cameraParameters.getSupportedFocusModes().contains(
				Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
		focusAreaSupported = (cameraParameters.getMaxNumFocusAreas() > 0
					&& Util.isSupported(Parameters.FOCUS_MODE_AUTO, cameraParameters.getSupportedFocusModes()));
		meteringAreaSupported = (cameraParameters.getMaxNumMeteringAreas() > 0);
		aeLockSupported = cameraParameters.isAutoExposureLockSupported();
		awbLockSupported = cameraParameters.isAutoWhiteBalanceLockSupported();


		if (continousFocusSupported) {
			cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
		} else if (Util.isSupported(Parameters.FOCUS_MODE_AUTO, cameraParameters.getSupportedFocusModes())) {
			cameraParameters.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
		}

//		camera.enableShutterSound(true);  //requires api 17
		int camWidth = isPortraitOrientation() ? cameraView.getHeight() : cameraView.getWidth();
		int camHt = isPortraitOrientation() ? cameraView.getWidth() : cameraView.getHeight();
		previewSizePair = selectSizePair(
				cameraParameters.getPreferredPreviewSizeForVideo(),
				cameraParameters.getSupportedPreviewSizes(),
				cameraParameters.getSupportedPictureSizes(),
				camWidth,
				camHt);

		Log.d(TAG, "preview size: " + previewSizePair.previewSize.width + ":" + previewSizePair.previewSize.height);

		cameraParameters.setPreviewSize(previewSizePair.previewSize.width, previewSizePair.previewSize.height);
		cameraParameters.setPreviewFormat(ImageFormat.NV21);

		//commenting out; not used now
		//Camera.Size picSize = previewSizePair.pictureSize != null ? previewSizePair.pictureSize : previewSizePair.previewSize;
		//cameraParameters.setPictureSize(picSize.width,picSize.height);
		//Log.d(TAG, "picture size: " + picSize.width + ":" + picSize.height);

		camera.setParameters(cameraParameters);

		try {
			if (cameraView.getSurfaceTexture() != null) {
				//camera.setPreviewCallbackWithBuffer(this);
				camera.setPreviewTexture(cameraView.getSurfaceTexture());
			}
		} catch (IOException e) {
			Log.e(TAG, "Unable to attach preview to camera!", e);
		}
	}


	@Override
	public void onPause(boolean multitasking) {
		Log.d(TAG,"pause");

		super.onPause((multitasking));
		if (isPreviewing()) {
			int camId = cameraId;
			CameraDirection camDir = cameraDirection;
			stopPreview(null,false);

			//reset state so it can be restored onResume
			setIsPreviewing(true);
			this.cameraId = camId;
			this.cameraDirection = camDir;
		}

		isPaused = true;
	}


	@Override
	public void onResume(boolean multitasking) {
		Log.d(TAG,"resume");

		super.onResume(multitasking);

		if (isPreviewing() && !surfaceDestroyed) {
			forcePreviewRestart();
		} else {
			//must wait until surfaceTexture is available for use by camera before starting preview
			//see onSurfaceTextureAvailable
		}

		isPaused = false;
	}

	//only call from UI thread
	private void updateCordovaViewContainerSize() {

		cordova.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {

				if (EXPERIMENTAL) {
					float aspectRatio = isPortraitOrientation() ? 1.0f / previewSizePair.previewAspectRatio : previewSizePair.previewAspectRatio;
					((AspectRatioFrameLayout)cameraViewContainer).setAspectRatio(aspectRatio);
					View v = cordova.getActivity().findViewById(android.R.id.content);
					v.requestLayout();

					updateCameraDisplayOrientation();
				} else {
				FrameLayout.LayoutParams paramsX = (FrameLayout.LayoutParams) cameraViewContainer.getLayoutParams();
				Log.d(TAG,"updateCordovaViewContainer PRE invalidate: " + paramsX.width + ":" + paramsX.height);

				Size sz = getDefaultWebViewSize();
				int previewWidth = previewSizePair.previewSize.width;
				int previewHeight = previewSizePair.previewSize.height;

				if (isPortraitOrientation()) {
					previewWidth = previewSizePair.previewSize.height;
					previewHeight = previewSizePair.previewSize.width;
				}

				float scale = Math.min((float) sz.width / (float) previewWidth, (float) sz.height / (float) previewHeight);
				//float dx = Math.abs(sz.width - previewWidth * scale) / 2f;
				//float dy = Math.abs(sz.height - previewHeight * scale) / 2f;

				Log.d(TAG, "computeTransform, scale: " + scale);

				int cvcWidth = (int)(previewWidth * scale);
				int cvcHt = (int)(previewHeight * scale);

				Log.d(TAG, "updateCordovaViewContainer cvs size: " + cvcWidth + ":" + cvcHt);

				FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) cameraViewContainer.getLayoutParams();
				params.width = cvcWidth;
				params.height = cvcHt;
				params.gravity = Gravity.CENTER;
				cameraViewContainer.setLayoutParams(params);

				View v = cordova.getActivity().findViewById(android.R.id.content);
				v.requestLayout();

				updateCameraDisplayOrientation();

				paramsX = (FrameLayout.LayoutParams) cameraViewContainer.getLayoutParams();
				Log.d(TAG, "updateCordovaViewContainer POST invalidate: " + paramsX.width + ":" + paramsX.height);

				updateMatrix();
				}
			}
		});
	}

	private void resetCameraViewContainerSize() {
		cordova.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Size sz = getDefaultWebViewSize();
				int cvcWidth = sz.width;
				int cvcHt =  sz.height;

				FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) cameraViewContainer.getLayoutParams();
				params.width = cvcWidth;
				params.height = cvcHt;
				params.gravity = Gravity.FILL;
				cameraViewContainer.setLayoutParams(params);

				View v = cordova.getActivity().findViewById(android.R.id.content);
				v.requestLayout();

				updateMatrix();
			}
		});
	}

	private int getCameraId(CameraDirection cameraDir) {

		// Find number of cameras available
		int numberOfCameras = Camera.getNumberOfCameras();
		Log.d(TAG, "Cameras:" + numberOfCameras);

		// Find ID of the back-facing ("default") camera
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		int cameraIdToOpen = UNDEFINED;
		for (int i = 0; i < numberOfCameras; i++) {
			Camera.getCameraInfo(i, cameraInfo);

			Log.d(TAG, "Camera facing:" + cameraInfo.facing);

			if (cameraInfo.facing == cameraDir.getDirection()) {
				cameraIdToOpen = i;
				break;
			}
		}

		if (cameraIdToOpen == UNDEFINED) {
			cameraIdToOpen = numberOfCameras - 1;
		}

		return cameraIdToOpen;
	}


	public void updateCameraDisplayOrientation() {
		displayOrientation = getRoatationAngle(cameraId);
		camera.setDisplayOrientation(displayOrientation);
		updateMatrix();
		Log.i(TAG, "updateCameraDeviceOrientation: " + displayOrientation);
	}

	/**
	 * Get Rotation Angle
	 *
	 * @param cameraId probably front cam
	 * @return angel to rotate
	 */
	public int getRoatationAngle(int cameraId) {
		int rotation = Util.getDisplayRotation(activity);
		int result = Util.getDisplayOrientation(rotation,cameraId);
		return result;
	}

	private boolean isPortraitOrientation() {
		int rotation = Util.getDisplayRotation(activity);
		return rotation == Surface.ROTATION_0 || rotation == Surface.ROTATION_180;
	}

	private boolean isMirror() {
		return cameraDirection != null ? cameraDirection.isMirror() : false;
	}

	private void updateMatrix() {
		Log.i(TAG, "update Matrix");
		Matrix workMatrix = new Matrix();
		Size sz = getWebViewSize();

		Log.d(TAG, "getDisplayOrientation "+getDisplayOrientation());
		Util.prepareMatrix(workMatrix, isMirror(), getDisplayOrientation(), sz.width, sz.height);

		// In face detection, the matrix converts the driver coordinates to UI
		// coordinates. In tap focus, the inverted matrix converts the UI
		// coordinates to driver coordinates.
		workMatrix.invert(matrix);
	}

	/**
	 * Selects the most suitable preview and picture size, given the desired width and height.
	 * <p/>
	 * Even though we may only need the preview size, it's necessary to find both the preview
	 * size and the picture size of the camera together, because these need to have the same aspect
	 * ratio.  On some hardware, if you would only set the preview size, you will get a distorted
	 * image.
	 *
	 * @param preferredVideoSize  the best preview size
	 * @param desiredWidth  the desired width of the camera preview frames
	 * @param desiredHeight the desired height of the camera preview frames
	 * @return the selected preview and picture size pair
	 */
	//code influenced by https://github.com/googlesamples/android-vision/blob/master/visionSamples/barcode-reader/app/src/main/java/com/google/android/gms/samples/vision/barcodereader/ui/camera/CameraSource.java
	private static SizePair selectSizePair(Camera.Size preferredVideoSize,
										   List<android.hardware.Camera.Size> supportedPreviewSizes,
										   List<android.hardware.Camera.Size> supportedPictureSizes,
										   int desiredWidth, int desiredHeight) {
		List<SizePair> validPreviewSizes =
				generateValidPreviewSizeList(supportedPreviewSizes,supportedPictureSizes);

		Log.d(TAG, "sto chiedendo W:"+desiredWidth+" H:"+desiredHeight);

		SizePair selectedPair = null;

		//strategy #1 - match aspect ratio exactly with scale of 1x or 2x //non va, la seconda volta seleziona una risoluzione bassissima
		/*float targetAspectRatio = (float)desiredWidth / (float)desiredHeight;
		int targetArea = desiredHeight * desiredWidth;
		for (SizePair sizePair : validPreviewSizes) {

			if (Math.abs(targetAspectRatio - sizePair.previewAspectRatio) < 0.05) {
				//exact aspect ratio match
				//ensure that sizePair

				if (sizePair.previewSizeArea <= targetArea) {
					Log.d(TAG, "strategy 1 wins, torno W:"+sizePair.previewSize.width+" H:"+sizePair.previewSize.height);
					return sizePair;
				}
			}
		}*/

		//strategy 2
		// The method for selecting the best size is to minimize the sum of the differences between
		// the desired values and the actual values for width and height.  This is certainly not the
		// only way to select the best size, but it provides a decent tradeoff between using the
		// closest aspect ratio vs. using the closest pixel area.
		int minDiff = Integer.MAX_VALUE;
		for (SizePair sizePair : validPreviewSizes) {

			//use camera's preferred video size if  possible
			if (preferredVideoSize != null && preferredVideoSize.equals(sizePair.previewSize)) {
				Log.d(TAG, "strategy 2 wins, torno W:"+sizePair.previewSize.width+" H:"+sizePair.previewSize.height);
				return sizePair;
			}

			//find largest previewSize w/ perimeter < desired perimeter
			Camera.Size size = sizePair.previewSize;
			int diff = (desiredWidth + desiredHeight) - (size.width + size.height);
			if (0 <= diff && diff < minDiff) {
				selectedPair = sizePair;
				minDiff = diff;
			}
		}
		Log.d(TAG, "No strategy win , torno W:"+selectedPair.previewSize.width+" H:"+selectedPair.previewSize.height);
		return selectedPair;
	}


	/**
	 * If the absolute difference between a preview size aspect ratio and a picture size aspect
	 * ratio is less than this tolerance, they are considered to be the same aspect ratio.
	 */
	private static final float ASPECT_RATIO_TOLERANCE = 0.01f;

	/**
	 * Stores a preview size and a corresponding same-aspect-ratio picture size.  To avoid distorted
	 * preview images on some devices, the picture size must be set to a size that is the same
	 * aspect ratio as the preview size or the preview may end up being distorted.  If the picture
	 * size is null, then there is no picture size with the same aspect ratio as the preview size.
	 * https://github.com/googlesamples/android-vision/blob/master/visionSamples/barcode-reader/app/src/main/java/com/google/android/gms/samples/vision/barcodereader/ui/camera/CameraSource.java
	 */
	private static class SizePair {

		public Camera.Size previewSize;
		public Camera.Size pictureSize;
		public float previewAspectRatio;
		public int previewSizeArea;

		public SizePair(Camera.Size previewSize,
						Camera.Size pictureSize) {
			this.previewSize = previewSize;
			this.pictureSize = pictureSize;
			this.previewAspectRatio = (float)this.previewSize.width / (float)this.previewSize.height;
			previewSizeArea= previewSize.width * previewSize.height;
		}

		public String toString() {
			return "w: " + previewSize.width + " h: " + previewSize.height + " ar: " + previewAspectRatio;
		}
	}

	/**
	 * Generates a list of acceptable preview sizes.  Preview sizes are not acceptable if there is
	 * not a corresponding picture size of the same aspect ratio.  If there is a corresponding
	 * picture size of the same aspect ratio, the picture size is paired up with the preview size.
	 * <p/>
	 * This is necessary because even if we don't use still pictures, the still picture size must be
	 * set to a size that is the same aspect ratio as the preview size we choose.  Otherwise, the
	 * preview images may be distorted on some devices.
	 */
	private static List<SizePair> generateValidPreviewSizeList(
			List<android.hardware.Camera.Size> supportedPreviewSizes,
			List<android.hardware.Camera.Size> supportedPictureSizes) {

		List<SizePair> validPreviewSizes = new ArrayList<SizePair>();
//		for (android.hardware.Camera.Size previewSize : supportedPreviewSizes) {
////			Log.v(TAG, "PV:  " + previewSize.width + ":" + previewSize.height);
//
//			//if no supported picture sizes then leave this loop
//			if (supportedPictureSizes == null) break;
//
//			float previewAspectRatio = (float) previewSize.width / (float) previewSize.height;
//			Camera.Size bestPictureSize = null;
//			float  bestScale = Float.MAX_VALUE;
//
//			// By looping through the picture sizes in order, we favor the higher resolutions.
//			// We choose the highest resolution in order to support taking the full resolution
//			// picture later.
//			for (Camera.Size pictureSize : supportedPictureSizes) {
////				Log.v(TAG, "PIC:  " + pictureSize.width + ":" + pictureSize.height);
//
//				float pictureAspectRatio = (float) pictureSize.width / (float) pictureSize.height;
//				if (Math.abs(previewAspectRatio - pictureAspectRatio) < ASPECT_RATIO_TOLERANCE) {
//
//					float scale = (float) pictureSize.width / (float) previewSize.width;
//					if (1.0f <= scale && scale <= 1.5f && (bestPictureSize == null || scale < bestScale)) {
//						bestScale = scale;
//						bestPictureSize = pictureSize;
//						//break;
//						if (bestScale == 1.0f) break;
//					} else if (scale < 1.0f && bestScale > 1.5f) {
//						bestScale = scale;
//						bestPictureSize = pictureSize;
//						break;
//					}
//				}
//			}
//			if (bestPictureSize != null) {
//				validPreviewSizes.add(new SizePair(previewSize, bestPictureSize));
//			}
//		}

		// If there are no picture sizes with the same aspect ratio as any preview sizes, allow all
		// of the preview sizes and hope that the camera can handle it.  Probably unlikely, but we
		// still account for it.
		if (validPreviewSizes.size() == 0) {
			Log.d(TAG, "No preview sizes have a corresponding same-aspect-ratio picture size");
			for (Camera.Size previewSize : supportedPreviewSizes) {
				Log.d(TAG, "PV:  " + previewSize.width + ":" + previewSize.height);
				// The null picture size will let us know that we shouldn't set a picture size.
				validPreviewSizes.add(new SizePair(previewSize, null));
			}

			//sort largest to smallest
			Collections.sort(validPreviewSizes, new SizeComparator());

		}

		return validPreviewSizes;
	}

	static class SizeComparator implements Comparator<SizePair> {

		@Override
		public int compare(SizePair lhs, SizePair rhs) {
			if (lhs.previewSizeArea < rhs.previewSizeArea) return -1;
			if (lhs.previewSizeArea == rhs.previewSizeArea) return 0;
			return 1;
		}
	}

	private Size getDefaultWebViewSize() {
		FrameLayout cvcParent = (FrameLayout) cameraViewContainer.getParent();
		Size sz = new Size(cvcParent.getWidth(),cvcParent.getHeight());
		return sz;
	}

	private Size getWebViewSize() {
		FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) cameraViewContainer.getLayoutParams();
		Size sz = new Size(params.width,params.height);
		Log.i(TAG, "get Webview size w"+params.width+" h"+params.height);
		return sz;
	}


	private static class Size {
		public int width;
		public int height;

		public Size(int width, int height) {
			this.width = width;
			this.height = height;
		}
	}



	private static int getIntOrNull(JSONArray args, int i) {
		if (args.isNull(i)) {
			return Integer.MIN_VALUE;
		}

		try {
			return args.getInt(i);
		} catch (JSONException e) {
			Log.e(TAG, "Can't get double", e);
			throw new RuntimeException(e);
		}
	}

	private static double getDoubleOrNull(JSONArray args, int i) {
		if (args.isNull(i)) {
			return Double.NaN;
		}

		try {
			return args.getDouble(i);
		} catch (JSONException e) {
			Log.e(TAG, "Can't get double", e);
			throw new RuntimeException(e);
		}
	}


  //------------- used by Flashlight plugin --------------------
  //HACK using IPC between videoOverlayPlugin and flashlight plugin
  //TODO: refactor to use events and listener pattern

	public Camera getActiveCamera() {
		return camera;
	}

	public Integer getActiveCameraId() {
		return Integer.valueOf(cameraId);
	}

	public Camera getBackCamera() {
		Camera camera = null;
		if (cameraDirection == CameraDirection.BACK) {
			camera = this.camera;
		}
		return camera;
	}

	public Camera getFrontCamera() {
		Camera camera = null;
		if (cameraDirection == CameraDirection.FRONT) {
			camera = this.camera;
		}
		return camera;
	}

	public int getActiveCameraDirection() {
		return cameraDirection.getDirection();
	}

	public Integer getDisplayOrientation() {
		return displayOrientation;
	}

	public TextureView getCameraView() {
		return cameraView;
	}

	private void setIsPreviewing(boolean val) {
		isPreviewing = val;
	}

	public Boolean isPreviewing() {
		return isPreviewing;
	}

	//reflectively access VideoOverlay plugin to get camera in same direction as lightLoc
	private void sendFlashlightEvent(int state, CameraDirection cameraDirection, int cameraId, Camera camera) {

		CordovaPlugin flashlightPlugin = getFlashlightPlugin();
		if (flashlightPlugin == null) {
			return;
		}

		Method method = null;

		try {
			if (state == STARTED) {
				method = flashlightPlugin.getClass().getMethod("videoOverlayStarted", int.class, int.class, Camera.class );
			} else {
				method = flashlightPlugin.getClass().getMethod("videoOverlayStopped", int.class, int.class, Camera.class );
			}
		} catch (SecurityException e) {
			//e.printStackTrace();
		} catch (NoSuchMethodException e) {
			//e.printStackTrace();
		}

		try {
			if (method == null) return;

			method.invoke(flashlightPlugin, cameraDirection.ordinal(), cameraId, camera);

		} catch (IllegalArgumentException e) { // exception handling omitted for brevity
			//e.printStackTrace();
		} catch (IllegalAccessException e) { // exception handling omitted for brevity
			//e.printStackTrace();
		} catch (InvocationTargetException e) { // exception handling omitted for brevity
			//e.printStackTrace();
		}
	}

	//reflectively access VideoOverlay plugin to get camera in same direction as lightLoc
	private void sendFaceDetectorEvent(int state, CameraDirection cameraDirection, int cameraId, Camera camera) {

		CordovaPlugin faceDetectorPlugin = getFaceDetectorPlugin();
		if (faceDetectorPlugin == null) {
			return;
		}

		Method method = null;

		try {
			if (state == STARTED) {
				method = faceDetectorPlugin.getClass().getMethod("videoOverlayStarted", int.class, int.class, Camera.class );
			} else {
				method = faceDetectorPlugin.getClass().getMethod("videoOverlayStopped", int.class, int.class, Camera.class );
			}
		} catch (SecurityException e) {
			//e.printStackTrace();
		} catch (NoSuchMethodException e) {
			//e.printStackTrace();
		}

		try {
			if (method == null) return;

			method.invoke(faceDetectorPlugin, cameraDirection.ordinal(), cameraId, camera);

		} catch (IllegalArgumentException e) { // exception handling omitted for brevity
			//e.printStackTrace();
		} catch (IllegalAccessException e) { // exception handling omitted for brevity
			//e.printStackTrace();
		} catch (InvocationTargetException e) { // exception handling omitted for brevity
			//e.printStackTrace();
		}
	}

	//reflectively access VideoOverlay plugin to get camera in same direction as lightLoc
	private void sendOpenCVEvent(int state, CameraDirection cameraDirection, int cameraId, Camera camera) {

		CordovaPlugin openCVPlugin = getOpenCVPlugin();
		if (openCVPlugin == null) {
			return;
		}

		Method method = null;

		try {
			if (state == STARTED) {
				method = openCVPlugin.getClass().getMethod("videoOverlayStarted", int.class, int.class);
			} else {
				method = openCVPlugin.getClass().getMethod("videoOverlayStopped", int.class, int.class );
			}
		} catch (SecurityException e) {
			//e.printStackTrace();
		} catch (NoSuchMethodException e) {
			//e.printStackTrace();
		}

		try {
			if (method == null) return;

			method.invoke(openCVPlugin, cameraDirection.ordinal(), cameraId, camera);

		} catch (IllegalArgumentException e) { // exception handling omitted for brevity
			//e.printStackTrace();
		} catch (IllegalAccessException e) { // exception handling omitted for brevity
			//e.printStackTrace();
		} catch (InvocationTargetException e) { // exception handling omitted for brevity
			//e.printStackTrace();
		}
	}

	private CordovaPlugin getPlugin(String pluginName) {
		CordovaPlugin plugin = webView.getPluginManager().getPlugin(pluginName);
		return plugin;
	}

	private CordovaPlugin getFlashlightPlugin() {
		String pluginName = "flashlight";
		return getPlugin(pluginName);
	}

	private CordovaPlugin getSnapshotPlugin() {
		String pluginName = "snapshot";
		return getPlugin(pluginName);
	}

	private CordovaPlugin getFaceDetectorPlugin() {
		String pluginName = "facedetector";
		return getPlugin(pluginName);
	}

	private CordovaPlugin getOpenCVPlugin() {
		String pluginName = "opencv";
		return getPlugin(pluginName);
	}
}
