/**
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
package com.ezartech.ezar.videooverlay;

import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;

public enum CameraDirection {
	BACK {
		public int getDirection() {
			return Camera.CameraInfo.CAMERA_FACING_BACK;
		}
		
		public boolean isMirror() {
			return false;
		}
	},
	FRONT {
		public int getDirection() {
			return Camera.CameraInfo.CAMERA_FACING_FRONT;
		}		
		
		public boolean isMirror() {
			return true;
		}
	}
	;

	public abstract int getDirection();
	public abstract boolean isMirror();

	public static CameraDirection fromCameraInfo(CameraInfo info) {
		return values()[info.facing];
	}
}
