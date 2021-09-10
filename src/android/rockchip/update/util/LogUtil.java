package android.rockchip.update.util;

import android.util.Log;

public class LogUtil {
	private static final boolean DEBUG = true;


	public static void v(String tag, String msg , boolean needDebug) { 
		if (DEBUG && needDebug) {  
			Log.v(tag, msg);  
		}  
	}  




}
