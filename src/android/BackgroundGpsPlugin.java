package com.tenforwardconsulting.cordova.bgloc;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

import de.greenrobot.event.EventBus;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

public class BackgroundGpsPlugin extends CordovaPlugin {
    private static final String TAG = "BackgroundGpsPlugin";

    public static final String ACTION_START = "start";
    public static final String ACTION_STOP = "stop";
    public static final String ACTION_CONFIGURE = "configure";
    public static final String ACTION_SET_CONFIG = "setConfig";

    private Intent updateServiceIntent;

    private Boolean isEnabled = false;

    private String stationaryRadius = "30";
    private String desiredAccuracy = "100";
    private String distanceFilter = "30";
    private String locationTimeout = "60";
    private String isDebugging = "false";
    private String notificationTitle = "Background tracking";
    private String notificationText = "ENABLED";
    private String stopOnTerminate = "false";
    
    private CallbackContext callback;
    
    @Override
    protected void pluginInitialize() {
        Log.d("BUS","registering");
        EventBus.getDefault().register(this);
    }

    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) {
        Log.d(TAG, "execute / action : " + action);
        Activity activity = this.cordova.getActivity();
        Boolean result = false;
        updateServiceIntent = new Intent(activity, LocationUpdateService.class);

        if (ACTION_START.equalsIgnoreCase(action) && !isEnabled) {
            result = true;
            
            updateServiceIntent.putExtra("stationaryRadius", stationaryRadius);
            updateServiceIntent.putExtra("desiredAccuracy", desiredAccuracy);
            updateServiceIntent.putExtra("distanceFilter", distanceFilter);
            updateServiceIntent.putExtra("locationTimeout", locationTimeout);
            updateServiceIntent.putExtra("desiredAccuracy", desiredAccuracy);
            updateServiceIntent.putExtra("isDebugging", isDebugging);
            updateServiceIntent.putExtra("notificationTitle", notificationTitle);
            updateServiceIntent.putExtra("notificationText", notificationText);
            updateServiceIntent.putExtra("stopOnTerminate", stopOnTerminate);

            activity.startService(updateServiceIntent);
            isEnabled = true;
        } else if (ACTION_STOP.equalsIgnoreCase(action)) {
            isEnabled = false;
            result = true;
            activity.stopService(updateServiceIntent);
            callbackContext.success();
            
        } else if (ACTION_CONFIGURE.equalsIgnoreCase(action)) {
            result = true;
            try {
                JSONObject config = data.getJSONObject(0);
                
                Log.i(TAG, "CONFIGURE: " + config.toString());

                this.stationaryRadius   = config.getString("stationaryRadius");
                this.distanceFilter     = config.getString("distanceFilter");
                this.locationTimeout    = config.getString("locationTimeout");
                this.desiredAccuracy    = config.getString("desiredAccuracy");
                this.isDebugging        = config.getString("debug");
                this.notificationTitle  = config.getString("notificationTitle");
                this.notificationText   = config.getString("notificationText");
                this.stopOnTerminate    = config.getString("stopOnTerminate");
                
                this.callback = callbackContext;
            } catch (JSONException e) {
                callbackContext.error("Configuration error " + e.getMessage());
            }
        } else if (ACTION_SET_CONFIG.equalsIgnoreCase(action)) {
            result = true;
            // TODO reconfigure Service
            callbackContext.success();
        }

        return result;
    }

    /**
     * Override method in CordovaPlugin.
     * Checks to see if it should turn off
     */
    public void onDestroy() {
        Activity activity = this.cordova.getActivity();

        if(isEnabled && stopOnTerminate.equalsIgnoreCase("true")) {
            activity.stopService(updateServiceIntent);
        }
    }

    public void onEventMainThread(JSONObject loc){
        Log.i(TAG, "BUS received : " + loc.toString());
        PluginResult result = new PluginResult(PluginResult.Status.OK, loc);
        result.setKeepCallback(true);
        if(callback != null){
            callback.sendPluginResult(result);    
        }
    }
}
