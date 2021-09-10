package android.rockchip.update.service;

import android.os.Build;
import android.os.SystemProperties;
import android.rockchip.update.util.LogUtil;
import android.text.TextUtils;
import android.util.Log;

public class OTAConfig {
	
	private static final int OTA_STYLE_SERVER = 0;//国内版，使用服务器方式
	private static final int OTA_STYLE_CONFIG = 1;//国内版，使用配置文件方式
	
	private static final int OTA_STYLE_FOREIGN_SERVER = 2;//外单版，使用服务器方式
	private static final int OTA_STYLE_FOREIGN_CONFIG = 3;//外单版，使用配置文件方式

	private static final String TAG = "OTAConfig";
	private static final boolean DEBUG = true;
	
	public static final String default_ota_host = "http://fota.t-chip.com.cn";//默认的ota服务器
	public static final String KEY_OTA_HOST  ="ro.product.ota.host";
	public static final String KEY_FULL_OTA_HOST  ="ro.product.fullota.host";
	public static final String KEY_OTA_STYLE  ="ro.product.ota.style";
	public static final String CONFIG_FILE  ="OtaManifest.xml";
	
	
	/***
	 * 判断OTA采用的方式，未设置默认为服务器方式
	 * @return true is 服务器方式，false is 配置文件方式
	 */
	public static boolean isStyleServer()
	{
		int style = SystemProperties.getInt(KEY_OTA_STYLE,OTA_STYLE_SERVER);
		return style == OTA_STYLE_SERVER || style == OTA_STYLE_FOREIGN_SERVER;
	}
	
	//判断是否外单版，并显示不同的ui
	/***
	 * 判断是否外单版，并显示不同的ui,未设置默认为国内版
	 * @return
	 */
	public static boolean isForeign()
	{
		int style = SystemProperties.getInt(KEY_OTA_STYLE,OTA_STYLE_SERVER);
		return style == OTA_STYLE_FOREIGN_SERVER || style == OTA_STYLE_FOREIGN_CONFIG;
	}

	public static boolean isMiYue()
	{
		return OTAConfig.getSystemModel().equals("MIYUE");
	}
	
	
	public static String getCheckServerUpdate()
	{
		String checkurl = getRemoteHost()+"/download/version/" +
				getSystemBrand() + "/" +
				getSystemModel() + "/" + 
				getSystemVersion()+"/"+
				getFwType();
		LogUtil.v(TAG, "getCheckServerUpdate()="+ checkurl,DEBUG);
		return checkurl;
	}
	
	public static String getCheckConfigUpdate()
	{
		String checkurl = getRemoteHost() 
				  + "?brand="+getSystemBrand()
				  + "&model="+getSystemModel()
				  + "&version="+getSystemVersion()
				  + "&fwtype="+getFwType();
		LogUtil.v(TAG, "getCheckConfigUpdate()="+ checkurl,DEBUG);
		return checkurl;
	}
	
	public static String getCheckFullUpdate()
	{
		String url =SystemProperties.get(KEY_FULL_OTA_HOST);
		if(TextUtils.isEmpty(url))return null;
		String checkurl = url 
				  + "?brand="+getSystemBrand()
				  + "&model="+getSystemModel()
				  + "&version="+getSystemVersion()
				  + "&fwtype="+getFwType();
		LogUtil.v(TAG, "getCheckFullUpdate()="+ checkurl,DEBUG);
		return checkurl;
	}
	public static boolean hasCheckFullUpdate()
	{
		String url =SystemProperties.get(KEY_FULL_OTA_HOST);
		return !TextUtils.isEmpty(url);
	}

	public static String getOtaPackage(String upload)
	{
		String packagePath = getRemoteHost()+ "/" + upload;
		LogUtil.v(TAG, " getCheckUpdate()="+ packagePath,DEBUG);
		return packagePath;
	}
	
	public static String getFullOtaPackage(String upload)
	{
		String packagePath =SystemProperties.get(KEY_FULL_OTA_HOST)+ "/" + upload;
		LogUtil.v(TAG, " getCheckUpdate()="+ packagePath,DEBUG);
		return packagePath;
	}

	public static String getOtaSuccess(String version_id)
	{
		String success = getRemoteHost()+"/download/success.html?version_id="+version_id+"&serial="+getSystemSerial();
		LogUtil.v(TAG, " getCheckUpdate()="+ success,DEBUG);
		return success;
	}



	public static String getRemoteHost() {
		String remoteHost = SystemProperties.get(KEY_OTA_HOST);
		if(remoteHost == null || remoteHost.length() == 0) {
			remoteHost = default_ota_host;
		}
		return remoteHost;
	}


	public static String getSystemModel() {
		String productName = SystemProperties.get("ro.product.model");
		if(productName.contains(" ")) {
			productName = productName.replaceAll(" ", "");
		}

		return productName;
	}


	public static String getSystemBrand(){
		String brand = SystemProperties.get("ro.product.brand");
		if(brand == null || brand.length() == 0)
		{
			brand = "rk30sdk";
		}

		return brand;

	}

	public static String getSystemVersion() {
		String version = SystemProperties.get("ro.product.version");
		if(version == null || version.length() == 0) {
			version = "1.0.0";
		}

		return version;
	}

	public static String getFwType(){
		String fwtype = SystemProperties.get("ro.product.fwtype", "0");

		return fwtype;
	}
	
	public static String getSystemSerial()
	{
		String serial = null;

		if(serial == null || serial.length() ==0)
		{
			return Build.SERIAL;
		}
		return serial;
	}



}
