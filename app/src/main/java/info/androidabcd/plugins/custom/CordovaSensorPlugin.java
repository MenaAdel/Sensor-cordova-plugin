package info.androidabcd.plugins.custom;

import android.os.Build;

import androidx.annotation.NonNull;

import com.t2.sensorreader.domain.SensorListener;
import com.t2.sensorreader.domain.SensorReport;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;

/**
 * This class echoes a string called from JavaScript.
 */
public class CordovaSensorPlugin extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("coolMethod")) {
            String message = args.getString(0);
            this.coolMethod(message, callbackContext);
            return true;
        }
        if (action.equals("startSensors")) {
            this.startSensors(callbackContext ,"");
            return true;
        }
        return false;
    }

    private void coolMethod(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    private void startSensors(final CallbackContext callbackContext ,String deviceId) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            SensorReport sensorReport = new SensorReport(this.cordova.getContext(), this.cordova.getActivity() ,deviceId);
            sensorReport.setSensorListener(new SensorListener() {
                                               @Override
                                               public void onApiValueChanged(@NonNull String response) {
                                                   PluginResult result = new PluginResult(PluginResult.Status.OK, response);
                                                   result.setKeepCallback(true);
                                                   callbackContext.sendPluginResult(result);
                                               }
                                           }
            );
        }
    }
}
