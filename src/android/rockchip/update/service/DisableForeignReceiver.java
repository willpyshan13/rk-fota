
package android.rockchip.update.service;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera.CameraInfo;
import android.rockchip.update.util.LogUtil;
import android.util.Log;


public class DisableForeignReceiver extends BroadcastReceiver {
    private static final String TAG = "DisableForeignReceiver";
    private static final boolean DEBUG = false;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Disable camera-related activities if there is no camera.
        boolean isForeign = OTAConfig.isForeign();

        if (!isForeign || OTAConfig.isMiYue()) {
        	LogUtil.v(TAG, "disable ForeignCheckUpdateActivity",DEBUG);
            disableComponent(context, "android.rockchip.update.service.ForeignCheckUpdateActivity");
        }

        // Disable this receiver so it won't run again.
        disableComponent(context, "android.rockchip.update.service.DisableForeignReceiver");
    }


    private void disableComponent(Context context, String klass) {
        ComponentName name = new ComponentName(context, klass);
        PackageManager pm = context.getPackageManager();

        // We need the DONT_KILL_APP flag, otherwise we will be killed
        // immediately because we are in the same app.
        pm.setComponentEnabledSetting(name,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP);
    }
}
