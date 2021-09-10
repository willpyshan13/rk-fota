package android.rockchip.update.util;

import android.app.Activity;
import android.content.Context;
import android.net.ConnectivityManager;

public class NetWork {

	public static boolean checkNetworkConnect(Context context)
	{
		ConnectivityManager con=(ConnectivityManager)context.getSystemService(Activity.CONNECTIVITY_SERVICE);  
		boolean wifi=con.getNetworkInfo(ConnectivityManager.TYPE_WIFI).isConnectedOrConnecting();  
		//	boolean internet=con.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).isConnectedOrConnecting(); 
		boolean eth = con.getNetworkInfo(ConnectivityManager.TYPE_ETHERNET).isConnectedOrConnecting(); 
		if(wifi|eth){  
			//执行相关操作 
			return true;
		}

		return false;

	}
}
