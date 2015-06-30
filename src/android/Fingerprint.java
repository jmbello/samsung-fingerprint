package com.any.cordova;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.SparseArray;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.util.Log;
import com.samsung.android.sdk.SsdkUnsupportedException;
import com.samsung.android.sdk.pass.Spass;
import com.samsung.android.sdk.pass.SpassFingerprint;
import com.samsung.android.sdk.pass.SpassInvalidStateException;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

// import android.provider.Settings;

public class Fingerprint extends CordovaPlugin {

    // Public var
    public static final String TAG = "Fingerprint";

    public static final String ACTION_IS_AVAILABLE = "isAvailable";
    public static final String ACTION_VERIFY_FINGERPRINT = "verifyFingerprint";
    
    private SpassFingerprint mSpassFingerprint;
    private Spass mSpass;
    private Context mContext;
	private boolean isFeatureEnabled = false;
	private boolean onReadyIdentify = false;
	private JSONObject response = new JSONObject();
	private boolean couldIdentify = false;
	private CallbackContext callbackId = null ;

    /**
     * Executes the request and returns PluginResult.
     *
     * @param action            The action to execute.
     * @param args              JSONArry of arguments for the plugin.
     * @param callbackContext   The callback id used when calling back into JavaScript.
     * @return                  True if the action was valid, false if not.
     */
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
		callbackId = callbackContext;
        Log.v(TAG, action);
		boolean returnValue = false;
        if (action.equals(ACTION_IS_AVAILABLE)) {
			returnValue = isAvailable();
			if(returnValue){
				PluginResult result = new PluginResult(PluginResult.Status.OK, "Service is available");
				result.setKeepCallback(true);
				//callbackContext.success(response);
				callbackId.sendPluginResult(result);
			} else {
				//callbackContext.error(response);
				PluginResult result = new PluginResult(PluginResult.Status.ERROR, "Service is not available");
				result.setKeepCallback(true);
				callbackId.sendPluginResult(result);
			}
            return returnValue;
        } else if (action.equals(ACTION_VERIFY_FINGERPRINT)) {
            returnValue = verifyFingerprint();
			if(returnValue){
				Log.v(TAG, "ACTION_VERIFY_FINGERPRINT finish successfully on native code");
				callbackContext.success(response);
			} else {
				Log.v(TAG, "ACTION_VERIFY_FINGERPRINT finish unsuccessfully on native code");
				callbackContext.error(response);
			}
            return returnValue;
        }
        return returnValue;
    }

    // Constructor
    public Fingerprint() {
		mSpass = new Spass();
    }

    public boolean isAvailable() throws JSONException {
		try {
            mSpass.initialize(this.cordova.getActivity().getApplicationContext());
        } catch (SsdkUnsupportedException e) {
            Log.v(TAG, "Exception: " + e.getMessage());
			return false;
        } catch (UnsupportedOperationException e){
            Log.v(TAG, "Fingerprint Service is not supported in the device " + e.getMessage());
			return false;
        }
		isFeatureEnabled = mSpass.isFeatureEnabled(Spass.DEVICE_FINGERPRINT);
        if(isFeatureEnabled){
            mSpassFingerprint = new SpassFingerprint(this.cordova.getActivity().getApplicationContext());
            Log.v(TAG, "Fingerprint Service is supported in the device.");
			Log.v(TAG, "SDK version : " + mSpass.getVersionName());
        } else { 
            Log.v(TAG, "Fingerprint Service is not supported in the device.");
        }
		return isFeatureEnabled;
    }

    public boolean verifyFingerprint() throws JSONException {
		Log.v(TAG, "entering function");
        try {
			if (!mSpassFingerprint.hasRegisteredFinger()) {
				Log.v(TAG, "Native verifyFingerprint: Please register finger first");
				return false;
			} else {
				if (onReadyIdentify == false) {
					try {
						onReadyIdentify = true;
						mSpassFingerprint.startIdentifyWithDialog(this.cordova.getActivity().getApplicationContext(), listener, true);
						Log.v(TAG, "Please identify finger to verify you");
						if(couldIdentify){
							Log.v(TAG, "You were identified");
						} else {
							Log.v(TAG, "You were NOT identified");
						}
					} catch (SpassInvalidStateException ise) {
						onReadyIdentify = false;
						if (ise.getType() == SpassInvalidStateException.STATUS_OPERATION_DENIED) {
							Log.v(TAG, "Native verifyFingerprint: Exception: SpassInvalidStateException: " + ise.getMessage());
							return false;
						}
						Log.v(TAG, "Native verifyFingerprint: Exception: SpassInvalidStateException: " + ise.getMessage());
					} catch (IllegalStateException e) {
						onReadyIdentify = false;
						Log.v(TAG, "Native Exception: " + e.getMessage());
						return false;
					}
				} else {
					Log.v(TAG, "Native verifyFingerprint: Please cancel Identify first");
					return false;
				}
				return true;
			}
		} catch (UnsupportedOperationException e) {
			Log.v(TAG, "Again, This Service is not supported in current device");
			return false;
		}
    }

	private static String getEventStatusName(int eventStatus) {
        switch (eventStatus) {
        case SpassFingerprint.STATUS_AUTHENTIFICATION_SUCCESS:
            return "STATUS_AUTHENTIFICATION_SUCCESS";
        case SpassFingerprint.STATUS_AUTHENTIFICATION_PASSWORD_SUCCESS:
            return "STATUS_AUTHENTIFICATION_PASSWORD_SUCCESS";
        case SpassFingerprint.STATUS_TIMEOUT_FAILED:
            return "STATUS_TIMEOUT";
        case SpassFingerprint.STATUS_SENSOR_FAILED:
            return "STATUS_SENSOR_ERROR";
        case SpassFingerprint.STATUS_USER_CANCELLED:
            return "STATUS_USER_CANCELLED";
        case SpassFingerprint.STATUS_QUALITY_FAILED:
            return "STATUS_QUALITY_FAILED";
        case SpassFingerprint.STATUS_USER_CANCELLED_BY_TOUCH_OUTSIDE:
            return "STATUS_USER_CANCELLED_BY_TOUCH_OUTSIDE";
        case SpassFingerprint.STATUS_AUTHENTIFICATION_FAILED:
			return "STATUS_AUTHENTIFICATION_FAILED";
        default:
            return "STATUS_AUTHENTIFICATION_FAILED";
        }
    }

	private SpassFingerprint.IdentifyListener listener = new SpassFingerprint.IdentifyListener() {
        @Override
        public void onFinished(int eventStatus) {
            Log.v(TAG, "identify finished : reason = " + getEventStatusName(eventStatus));
            onReadyIdentify = false;
            int FingerprintIndex = 0;
            try {
                FingerprintIndex = mSpassFingerprint.getIdentifiedFingerprintIndex();
            } catch (IllegalStateException ise) {
                Log.v(TAG, ise.getMessage());
            }
            if (eventStatus == SpassFingerprint.STATUS_AUTHENTIFICATION_SUCCESS) {
                Log.v(TAG, "onFinished() : Identify authentification Success with FingerprintIndex : " + FingerprintIndex);
				couldIdentify = true;
            } else if (eventStatus == SpassFingerprint.STATUS_AUTHENTIFICATION_PASSWORD_SUCCESS) {
                Log.v(TAG, "onFinished() : Password authentification Success");
				couldIdentify = true;
            } else {
                Log.v(TAG, "onFinished() : Authentification Fail for identify");
				couldIdentify = false;
            }
        }
		@Override
		public void onReady() {
			Log.v(TAG, "identify state is ready");
		}
		@Override
		public void onStarted() {
		Log.v(TAG, "sensor is waiting for your finger!");
		}
    };
}