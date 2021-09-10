package android.rockchip.update.service;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.List;

import android.rockchip.update.ConStants;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpHead;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import android.rockchip.update.util.XMLUtil;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Binder;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.UserHandle;
import android.text.TextUtils;
import android.util.Log;
import android.os.SystemProperties;
import android.os.storage.StorageManager;
import android.os.storage.StorageVolume;
import android.os.storage.VolumeInfo;
import android.os.storage.DiskInfo;

public class RKUpdateService extends Service {
	public static final String VERSION = "1.8.0";
	private static final String TAG = "RKUpdateService";
    private static final boolean DEBUG = true;
	private static final boolean mIsNotifyDialog = true;
	private static final boolean mIsSupportUsbUpdate = true;
	
    private Context mContext;
    private volatile boolean mIsFirstStartUp = true;
    private static void LOG(String msg) {
        if ( DEBUG ) {
            Log.d(TAG, msg);  
        }
    }
 
    static {
        /*
         * Load the library.  If it's already loaded, this does nothing.
         */
        System.loadLibrary("rockchip_update_jni");
    }

    public static String OTA_PACKAGE_FILE = "update.zip";
	public static String RKIMAGE_FILE = "update.img";	
	public static final int RKUPDATE_MODE = 1;
	public static final int OTAUPDATE_MODE = 2;      
	private static volatile boolean mWorkHandleLocked = false; 
	private static volatile boolean mIsNeedDeletePackage = false;
	
	public static final String EXTRA_IMAGE_PATH = "android.rockchip.update.extra.IMAGE_PATH";
    public static final String EXTRA_IMAGE_VERSION = "android.rockchip.update.extra.IMAGE_VERSION";
    public static final String EXTRA_CURRENT_VERSION = "android.rockchip.update.extra.CURRENT_VERSION";
    public static String DATA_ROOT = "/data/media/0";
    public static String FLASH_ROOT = Environment.getExternalStorageDirectory().getAbsolutePath();
    //public static String SDCARD_ROOT = "/mnt/external_sd";
    //public static String USB_ROOT = "/mnt/usb_storage";
    public static String SDCARD_ROOT = "/mnt/media_rw";
    public static String USB_ROOT = "/mnt/media_rw";
    public static String CACHE_ROOT = Environment.getDownloadCacheDirectory().getAbsolutePath();
    
    public static final int COMMAND_NULL = 0;
    public static final int COMMAND_CHECK_LOCAL_UPDATING = 1;
    public static final int COMMAND_CHECK_REMOTE_UPDATING = 2;
    public static final int COMMAND_CHECK_REMOTE_UPDATING_BY_HAND = 3;
    public static final int COMMAND_DELETE_UPDATEPACKAGE = 4;
    
    private static final String COMMAND_FLAG_SUCCESS = "success";
    private static final String COMMAND_FLAG_UPDATING = "updating";
    
    public static final int UPDATE_SUCCESS = 1;
    public static final int UPDATE_FAILED = 2;
    
    private static final String[] IMAGE_FILE_DIRS = {
    	DATA_ROOT + "/",
        FLASH_ROOT + "/",  
//        SDCARD_ROOT + "/",
//        USB_ROOT + "/",
    };
    
    private String mLastUpdatePath;
    private WorkHandler mWorkHandler;
    private Handler mMainHandler;
    private SharedPreferences mAutoCheckSet;
   
    /*----------------------------------------------------------------------------------------------------*/
    public static URI mRemoteURI = null;
    public static URI mRemoteURIBackup = null;
    private String mTargetURI = null;
    private boolean mUseBackupHost = false;
    private String mOtaPackageVersion = null;
    private String mSystemVersion = null;
    private String mOtaPackageName = null;
    private String mOtaPackageLength = null;
	private String mOtaPackageDescription = null;
	private String mOtaVersionId = null;
    private volatile boolean mIsOtaCheckByHand = false;
    private volatile boolean mIsCheckFullUpgrade = false;
    
	@Override
	public IBinder onBind(Intent arg0) {
		// TODO Auto-generated method stub
		return mBinder;
	}

	private final LocalBinder mBinder = new LocalBinder();

	public class LocalBinder extends Binder {
		public void updateFirmware(String imagePath, int mode) {
			LOG("updateFirmware(): imagePath = " + imagePath);
			try {       
				mWorkHandleLocked = true;
				if(mode == OTAUPDATE_MODE){
					RKRecoverySystem.installPackage(mContext, new File(imagePath));
				}else if(mode == RKUPDATE_MODE){
					RKRecoverySystem.installPackage(mContext, new File(imagePath));
				}
			} catch (IOException e) {
				Log.e(TAG, "updateFirmware() : Reboot for updateFirmware() failed", e);
			}
		}

		public boolean doesOtaPackageMatchProduct(String imagePath) {
			LOG("doesImageMatchProduct(): start verify package , imagePath = " + imagePath);

			try{
				RKRecoverySystem.verifyPackage(new File(imagePath), null, null);
			}catch(GeneralSecurityException e){
				LOG("doesImageMatchProduct(): verifaPackage faild!");	
				return false;	
			}catch(IOException exc) {
				LOG("doesImageMatchProduct(): verifaPackage faild!");
				return false;
			}
			return true;
		}
		public void deletePackage(String path) {
			LOG("try to deletePackage...");
			if(path.startsWith("@")){
				String fileName = "/data/media/0/update.zip";
                			LOG("ota was maped, so try to delete path = " + path);
                			File f_ota = new File(fileName);
                			if(f_ota.exists()){
                				f_ota.delete();
                    				LOG("delete complete! path=" + fileName);
                    			}else{
                    				fileName = "/data/media/0/update.img";
                    				f_ota = new File(fileName);
                    				if(f_ota.exists()){
                    					f_ota.delete();
                        					LOG("delete complete! path=" + fileName);
                        				}else{
                        					LOG("path = " + fileName + ", file not exists!");
                    				}
                    			}
                    		}

			File f = new File(path);
			if(f.exists()) {
				f.delete();
				LOG("delete complete! path=" + path);
			}else {
				LOG("path=" + path + " ,file not exists!");
			}
		}
		

		public void unLockWorkHandler() {
			LOG("unLockWorkHandler...");
			mWorkHandleLocked = false;
		}

		public void LockWorkHandler() {
			mWorkHandleLocked = true;
			LOG("LockWorkHandler...!");
		}
		public int getCheckStatus()
		{
			return checkStatus;
		}
	}



	private boolean doesOtaPackageMatchProduct(String imagePath) {
		LOG("doesImageMatchProduct(): start verify package , imagePath = " + imagePath);

		try{
			RKRecoverySystem.verifyPackage(new File(imagePath), null, null);
		}catch(GeneralSecurityException e){
			LOG("doesImageMatchProduct(): verifaPackage faild!");	
			return false;	
		}catch(IOException exc) {
			LOG("doesImageMatchProduct(): verifaPackage faild!");
			return false;
		}
		return true;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		
		mContext = this;
        /*-----------------------------------*/
		LOG("starting RKUpdateService, version is " + VERSION);
		
		//whether is UMS or m-user 
		if(getMultiUserState()) {
			FLASH_ROOT = DATA_ROOT;
		}
		
        String ota_packagename = getOtaPackageFileName();
        if(ota_packagename != null) {
        	OTA_PACKAGE_FILE = ota_packagename;
        	LOG("get ota package name private is " + OTA_PACKAGE_FILE);
        }
        
        String rk_imagename = getRKimageFileName();
        if(rk_imagename != null) {
        	RKIMAGE_FILE = rk_imagename;
        	LOG("get rkimage name private is " + RKIMAGE_FILE);
        }
        
	/*
        try {
        	mRemoteURI = new URI(getRemoteUri());
        	mRemoteURIBackup = new URI(getRemoteUriBackup());
        	LOG("remote uri is " + mRemoteURI.toString());
        	LOG("remote uri backup is " + mRemoteURIBackup.toString());
        }catch(URISyntaxException e) {
        	e.printStackTrace();
        }*/
        
        mAutoCheckSet = getSharedPreferences("auto_check", MODE_PRIVATE);
        
        mMainHandler = new Handler(Looper.getMainLooper()); 
        HandlerThread workThread = new HandlerThread("UpdateService : work thread");
        workThread.start();
        mWorkHandler = new WorkHandler(workThread.getLooper());
        
        if(mIsFirstStartUp) {
        	LOG("first startup!!!");
			mIsFirstStartUp = false;
			String command = RKRecoverySystem.readFlagCommand();
			String path;
			if(command != null) {
				LOG("command = " + command);				
				if(command.contains("$path")) {
					path = command.substring(command.indexOf('=') + 1);
					LOG("last_flag: path = " + path);

					if(command.startsWith(COMMAND_FLAG_SUCCESS)) {
						if(!mIsNotifyDialog) {
							mIsNeedDeletePackage = true;
							mLastUpdatePath = path;
							return;
						}

						LOG("now try to start notifydialog activity!");
						Intent intent = new Intent(mContext, NotifyDeleteActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						intent.putExtra("flag", UPDATE_SUCCESS);
						intent.putExtra("path", path);
						startActivity(intent);
						mWorkHandleLocked = true;
						return;
					} 
					if(command.startsWith(COMMAND_FLAG_UPDATING)) {
						Intent intent = new Intent(mContext, NotifyDeleteActivity.class);
						intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						intent.putExtra("flag", UPDATE_FAILED);
						intent.putExtra("path", path);
						startActivity(intent);
						mWorkHandleLocked = true;
						return;
					}
				}
			}
		}
	}

	@Override
	public void onDestroy() {
		LOG("onDestroy.......");
		super.onDestroy();
	}



	boolean otaChecking = true;//ota升级，是否在检查中

	@TargetApi(Build.VERSION_CODES.ECLAIR)
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		LOG("onStartCommand.......");

		if(intent == null) {
			return Service.START_NOT_STICKY;
		}

		int command = intent.getIntExtra("command", COMMAND_NULL);
		int delayTime = intent.getIntExtra("delay", 1000);
		otaChecking = intent.getBooleanExtra("otaChecking", true);
		mIsCheckFullUpgrade = intent.getBooleanExtra("check_full_upgrade", false);

		LOG("command = " + command + " delaytime = " + delayTime);
		if(command == COMMAND_NULL) {
			return Service.START_NOT_STICKY;
		}

		if(command == COMMAND_CHECK_REMOTE_UPDATING) {
			mIsOtaCheckByHand = false;
			if(!mAutoCheckSet.getBoolean("auto_check", true)) {
				LOG("user set not auto check!");
				return Service.START_NOT_STICKY;
			}
		}

		if(command == COMMAND_CHECK_REMOTE_UPDATING_BY_HAND) {
			mIsOtaCheckByHand = true;
			command = COMMAND_CHECK_REMOTE_UPDATING;
		}

		if(mIsNeedDeletePackage) {
			command = COMMAND_DELETE_UPDATEPACKAGE;
			delayTime = 20000;
			mWorkHandleLocked = true;
		}

		Message msg = new Message();
		msg.what = command;
		msg.arg1 = WorkHandler.NOT_NOTIFY_IF_NO_IMG;
		checkStatus = ConStants.STATUS_CHECKING;

		mWorkHandler.sendMessageDelayed(msg, delayTime);
		return Service.START_REDELIVER_INTENT;
	}


	private int checkStatus = ConStants.STATUS_CHECKEND;////0 is  checking,1 is check end and have new_vewsion,2 1 is check end and have no new_vewsion




	/** @see mWorkHandler. */
	private class WorkHandler extends Handler {
		private static final int NOTIFY_IF_NO_IMG = 1;
		private static final int NOT_NOTIFY_IF_NO_IMG = 0;

		/*-----------------------------------*/

		public WorkHandler(Looper looper) {
			super(looper);
		}

		public void handleMessage(Message msg) {       

			String[] searchResult = null;       

			switch (msg.what) {

			case COMMAND_CHECK_LOCAL_UPDATING:
				LOG("WorkHandler::handleMessage() : To perform 'COMMAND_CHECK_LOCAL_UPDATING'.");
				if(mWorkHandleLocked){
					LOG("WorkHandler::handleMessage() : locked !!!");
					return;
				}

				if ( null != (searchResult = getValidFirmwareImageFile(IMAGE_FILE_DIRS) ) ) {
					if ( 1 == searchResult.length ) { 
						String path = searchResult[0];      
						String imageFileVersion = null;
						String currentVersion = null;

						//if it is rkimage, check the image
						if(path.endsWith("img")){
							if(!checkRKimage(path)){
								LOG("WorkHandler::handleMessage() : not a valid rkimage !!");
								return;	
							}

							imageFileVersion = getImageVersion(path);

							LOG("WorkHandler::handleMessage() : Find a VALID image file : '" + path 
									+ "'. imageFileVersion is '" + imageFileVersion);

							currentVersion = getCurrentFirmwareVersion();
							LOG("WorkHandler::handleMessage() : Current system firmware version : '" + currentVersion + "'.");
						}else if(path.endsWith("zip"))
						{
							if(!doesOtaPackageMatchProduct(path))
							{
								LOG("WorkHandler::handleMessage() : not a valid ota zip !!");
								return ;
							}
							LOG("WorkHandler::handleMessage() :  ota zip is ok!!");
						}
						startProposingActivity(path, imageFileVersion, currentVersion);
						return;
					}else {
						LOG("find more than two package files, so it is invalid!");
						return;
					}
				}

				break; 
			case COMMAND_CHECK_REMOTE_UPDATING:
				if(mWorkHandleLocked ){
					LOG("WorkHandler::handleMessage() : locked !!!");
					checkStatus = ConStants.STATUS_CHECKEND_LOCKED;
					return;
				}
				LOG("WorkHandler::handleMessage() : To perform 'COMMAND_CHECK_LOCAL_UPDATING'.");
				for(int i = 0; i < 1; i++) {
					try {	
						if(requestRemoteServerForUpdate()) {
							checkStatus = ConStants.STATUS_CHECKEND;
							if(!otaChecking)
							{
								LOG("WorkHandler::handleMessage() : OtaChecking is false!!!");
								return;
							}
							LOG("find a remote update package, now start PackageDownloadActivity...");
							Intent intent;
							if(OTAConfig.getSystemModel().equals("MIYUE"))
							{
								intent = new Intent(mContext,MiYuePackageDownloadActivity.class);
							}else if(OTAConfig.isForeign())
							{
								intent = new Intent(mContext, ForeignPackageDownloadActivity.class);
							}else{
								intent = new Intent(mContext, PackageDownloadActivity.class);
							}
							
							intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							intent.putExtra("uri", mTargetURI);
							intent.putExtra("OtaPackageLength", mOtaPackageLength);
							intent.putExtra("OtaPackageName", mOtaPackageName);
							intent.putExtra("OtaVersionId", mOtaVersionId);
							intent.putExtra("OtaPackageVersion", mOtaPackageVersion);
							intent.putExtra("SystemVersion", mSystemVersion);
							intent.putExtra("OtaPackageDescription",mOtaPackageDescription );
							mContext.startActivity(intent);
						}else {
							checkStatus =ConStants.STATUS_CHECKEND_NO_NEW;
							LOG("no find remote update package...");
						}
						break;
					}catch(Exception e) {
						//e.printStackTrace();
						checkStatus =ConStants.STATUS_CHECKEND_ERROR;
						LOG("request remote server error...");
					}

					try{
						Thread.sleep(5000);
					}catch(InterruptedException e) {
						e.printStackTrace();
					}
				}
				break;
			case COMMAND_DELETE_UPDATEPACKAGE:
				//if mIsNeedDeletePackage == true delete the package
				if(mIsNeedDeletePackage) {
					LOG("try to deletePackage...");
					File f = new File(mLastUpdatePath);
					if(f.exists()) {
						f.delete();
						LOG("delete complete! path=" + mLastUpdatePath);
					}else {
						LOG("path=" + mLastUpdatePath + " ,file not exists!");
					}

					mIsNeedDeletePackage = false;
					mWorkHandleLocked = false;
				}

				break;
			default:
				break; 
			}
		}

	}  

    private String[] findFromSdOrUsb() {
        final int userId = UserHandle.myUserId();
        StorageManager mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        final List<VolumeInfo> volumes = mStorageManager.getVolumes();
        for (VolumeInfo volume : volumes) {
            if (!volume.isMountedReadable()) continue;
            if(volume == null) continue;
            final DiskInfo disk = volume.getDisk();
            if(disk == null) continue;
            Log.e(TAG, "jkand.huang --- volume.internalPath = " + volume.internalPath);
            String OtaPath = volume.internalPath + "/" + OTA_PACKAGE_FILE;
            String ImgPath = volume.internalPath + "/" + RKIMAGE_FILE;
            int flag = -1;
            if(new File(OtaPath).exists()){
                flag = 0;
            }else if(new File(ImgPath).exists()){
                flag = 1;
            }else{
                continue ;
            }
            if(disk.isSd()){
                if(volume.fsType.equals("ntfs") || volume.fsType.equals("exfat")){
                    Log.e(TAG, "sd fstype is " + volume.fsType);
                    continue ;
                }
            }else if(disk.isUsb()){
                if(volume.fsType.equals("ntfs") || volume.fsType.equals("exfat")){
                    Log.e(TAG, "usb fstype is " + volume.fsType);
                    continue ;
                }
            }
            if(flag == 0){
                Log.e(TAG, "find package from " + OtaPath);
                return (new String[] {OtaPath});
            }else if(flag == 1){
                Log.e(TAG, "find package from " + ImgPath);
                return (new String[] {ImgPath});
            }
        }
        return null;
    }
    
    private String[] getValidFirmwareImageFile(String searchPaths[]) {
        for ( String dir_path : searchPaths) {
            String filePath = dir_path + OTA_PACKAGE_FILE;    
            LOG("getValidFirmwareImageFile() : Target image file path : " + filePath);
           
            if ((new File(filePath)).exists()) {
                return (new String[] {filePath} );
            }
        }

		//find rkimage
        for ( String dir_path : searchPaths) {
            String filePath = dir_path + RKIMAGE_FILE;
            //LOG("getValidFirmwareImageFile() : Target image file path : " + filePath);
           
            if ( (new File(filePath) ).exists() ) {
                return (new String[] {filePath} );
            }
        }
        if(mIsSupportUsbUpdate){
            //find usb device update package
            return findFromSdOrUsb();
        }
        
        return null;
    }

	native private static String getImageVersion(String path);

	native private static String getImageProductName(String path);

	private void startProposingActivity(String path, String imageVersion, String currentVersion) {
		Intent intent = new Intent();

		intent.setComponent(new ComponentName("android.rockchip.update.service", "android.rockchip.update.service.FirmwareUpdatingActivity") );
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(EXTRA_IMAGE_PATH, path);
		intent.putExtra(EXTRA_IMAGE_VERSION, imageVersion);
		intent.putExtra(EXTRA_CURRENT_VERSION, currentVersion);

		mContext.startActivity(intent);
	}

	private boolean checkRKimage(String path){
		/*		Edit by zhansb@131012
		String imageProductName = getImageProductName(path);
		LOG("checkRKimage() : imageProductName = " + imageProductName);
		if(imageProductName == null) {
			return false;
		}
		
		if(imageProductName.trim().equals(getProductName())){
			return true;
		}else {
			return false;
		}	
		 */
		return true;
	} 

	private String getOtaPackageFileName() {
		String str = SystemProperties.get("ro.ota.packagename");	
		if(str == null || str.length() == 0) {
			return null;
		}
		if(!str.endsWith(".zip")) {
			return str + ".zip";
		}

		return str;
	}

	private String getRKimageFileName() {
		String str = SystemProperties.get("ro.rkimage.name");	
		if(str == null || str.length() == 0) {
			return null;
		}
		if(!str.endsWith(".img")) {
			return str + ".img";
		}

		return str;
	}

	private String getCurrentFirmwareVersion() {    
		return SystemProperties.get("ro.firmware.version");
	}

	private static String getProductName() { 
		return SystemProperties.get("ro.product.model");        
	}

    public static boolean getMultiUserState() {    	
    	String multiUser = SystemProperties.get("ro.factory.hasUMS");
    	if(multiUser != null && multiUser.length() > 0) {
    		return !multiUser.equals("true");
    	}
    	
    	multiUser = SystemProperties.get("ro.factory.storage_policy");
    	if(multiUser != null && multiUser.length() > 0) {
    		return multiUser.equals("1");
    	}
    	
    	return false;
    }


	private void notifyInvalidImage(String path) {
		Intent intent = new Intent();

		intent.setComponent(new ComponentName("android.rockchip.update.service", "android.rockchip.update.service.InvalidFirmwareImageActivity") );
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		intent.putExtra(EXTRA_IMAGE_PATH, path); 

		mContext.startActivity(intent);
	}

	/**********************************************************************************************************************
    							  ota update
	 ***********************************************************************************************************************/




	/***
	 * 
	 * 查询是否存在ota升级包，并对相关参数赋值
	 * 注：服务器存在管理端
	 * 
	 * <info>
       		<path>packages/Q-BOX02/fwtype0/1.1.1/update.zip</path>
       		<version_id>8</version_id>
       		<aim_version>1.1.4</aim_version>
       		<content>
             	1.隐藏了不需要的apk
             	2.优化了以太网连接策略。             ...
       		</content>
	   </info>
	 * @return true is has update，false is has not
	 */
	private boolean checkServerUpdate()
	{
		LOG( "checkServerUpdate:"+OTAConfig.getCheckServerUpdate());
		String xmlStr = XMLUtil.getXmlFromUrl(OTAConfig.getCheckServerUpdate());
		
		LOG("checkServerUpdate:"+xmlStr);
		
		if(xmlStr == null || xmlStr.length() <= 10) return false;
		Document d = XMLUtil.getDomElement(xmlStr);	
		if(d == null)return false;
		//NodeList nlist = d.getElementsByTagName("info");
		Element element = d.getDocumentElement();
		if(element == null)return false;
		String path = XMLUtil.getValue(element, "path");
		String version_id =  XMLUtil.getValue(element, "version_id");
		String version =  XMLUtil.getValue(element, "aim_version");
		String content =  XMLUtil.getValue(element, "content");

		Log.v("sjf","checkConfigUpdate path = "+path+"  ,version_id="+version_id+" ,content="+content);

		
		if(path == null)return false;
		String name = path.substring(path.lastIndexOf("/") + 1,path.length());
		mOtaPackageName = name;//substring and set name
		mOtaPackageVersion  = version;
		mTargetURI = OTAConfig.getOtaPackage(path);
		mOtaPackageDescription = content;
		mOtaVersionId = version_id;
		LOG("checkServerUpdate name="+name+" ,path = "+path+"  ,version_id="+version_id+" ,content="+content);
		return true;
	}
	
	
	/**
	 * 查询是否存在ota升级包，并对相关参数赋值
	 * 注：服务器仅通过OtaManifest.xml配置相关升级属性
	 * 
	 * <?xml version="1.0" encoding="UTF-8"?>
	   <OtaManifast>
			<product name="Q-BOX02">
					<fwtype name="0">
						<version name="1.1.1">
       						<path>packages/Q-BOX02/fwtype0/1.1.1/update.zip</path>
       						<version_id>8</version_id>
       						<aim_version>1.1.4</aim_version>
       						<content>
             					1.隐藏了不需要的apk
             					2.优化了以太网连接策略。
             ...
       						</content>
						</version>
					</fwtype>
			</product>
		</OtaManifast>
	 * @return true is has update，false is has not
	 */
	private boolean checkConfigUpdate()
	{
		String xmlStr = XMLUtil.getXmlFromUrl(OTAConfig.getCheckConfigUpdate());
		if(xmlStr == null || xmlStr.length() <= 10) return false;
		Document d = XMLUtil.getDomElement(xmlStr);	
		if(d == null)return false;
		
		Element root = d.getDocumentElement();				
		Element productElement = XMLUtil.getElementByName(root,"product", OTAConfig.getSystemModel());		
		Element fwtypeElement = XMLUtil.getElementByName(productElement,"fwtype", OTAConfig.getFwType());
		Element versionElement = XMLUtil.getElementByName(fwtypeElement,"version", OTAConfig.getSystemVersion());

		if(versionElement == null) return false;
		String path = XMLUtil.getValue(versionElement, "path");
		String version_id =  XMLUtil.getValue(versionElement, "version_id");
		String aim_version =  XMLUtil.getValue(versionElement, "aim_version");
		String content =  XMLUtil.getValue(versionElement, "content");
		
		if(path == null)return false;
		String name = path.substring(path.lastIndexOf("/") + 1,path.length());
		mOtaPackageName = name;//substring and set name
		mOtaPackageVersion  = aim_version;
		mTargetURI = OTAConfig.getOtaPackage(path);
		mOtaPackageDescription = content;
		mOtaVersionId = version_id;
		LOG("checkConfigUpdate name="+name+" ,path = "+path+"  ,version_id="+version_id+" ,content="+content);
		return true;
	}

	/**
	 * 完整包升级,实现流程同checkConfigUpdate
	 * */
	
	private boolean checkFullUpdate()
	{
		String checkurl = OTAConfig.getCheckFullUpdate();
		LOG("checkFullUpdate checkurl="+checkurl);
		if(TextUtils.isEmpty(checkurl)) return false;
		
		String xmlStr = XMLUtil.getXmlFromUrl(checkurl);
		LOG("checkFullUpdate xmlStr="+xmlStr);
		if(xmlStr == null || xmlStr.length() <= 10) return false;
		
		
		
		Document d = XMLUtil.getDomElement(xmlStr);	
		if(d == null)return false;
		Element root = d.getDocumentElement();				
		Element productElement = XMLUtil.getElementByName(root,"product", OTAConfig.getSystemModel());		
		Element fwtypeElement = XMLUtil.getElementByName(productElement,"fwtype", OTAConfig.getFwType());
		Element versionElement = XMLUtil.getElementByName(fwtypeElement,"version", OTAConfig.getSystemVersion());
		if(versionElement == null) return false;
		String path = XMLUtil.getValue(versionElement, "path");
		String version_id =  XMLUtil.getValue(versionElement, "version_id");
		String aim_version =  XMLUtil.getValue(versionElement, "aim_version");
		String content =  XMLUtil.getValue(versionElement, "content");
		if(path == null)return false;
		String name = path.substring(path.lastIndexOf("/") + 1,path.length());
		mOtaPackageName = name;//substring and set name
		mOtaPackageVersion  = aim_version;
		mTargetURI = OTAConfig.getFullOtaPackage(path);
		mOtaPackageDescription = content;
		mOtaVersionId = version_id;
		LOG("checkFullUpdate name="+name+" ,path = "+path+"  ,version_id="+version_id+" ,content="+content);
		return true;
	}


	/*    private boolean  parseUpdateJson()
    {


		try{
			JSONObject rootJsonObject = JSONUtil.getJSON(mContext,getRemoteUri());
			if( rootJsonObject == null ) return false;
			//JSONObject nJsonObject = mJsonObject.getJSONObject("update");
			JSONObject updateJsonObject = rootJsonObject.getJSONObject("update");

			mOtaPackageName = updateJsonObject.getString("OtaPackageName").trim();

			mOtaPackageVersion = updateJsonObject.getString("OtaPackageVersion").trim();

			mOtaPackageDescription = updateJsonObject.getString("OtaPackageDescription").trim();


			mTargetURI = getBaseUri() + mOtaPackageName;
			Log.v("parsejson", "1>>>>>>>>>>>>>>>>>>>>>>>>>2" );
			//JSONObject mUpdateObject=nJsonObject;
			Log.v("parsejson", "1>>>>>>>>>>>>>>>>>>>>>>>>>3" );
		} catch (Exception e) {
			e.printStackTrace(System.err);
			Log.v("parsejson", "1>>>>>>>>>>>>>>>>>>>>>>>>>4" );
			return false;
		}
		return true;

    }*/

	private boolean requestRemoteServerForUpdate() throws IOException, ClientProtocolException{

		LOG("requestRemoteServerForUpdate mIsCheckFullUpgrade:"+mIsCheckFullUpgrade);
		if(mIsCheckFullUpgrade)
		{
			LOG("checkFullUpdate");
			if(!checkFullUpdate()) return false;
		}
		else if(OTAConfig.isStyleServer())
		{
			LOG("checkServerUpdate");
			if(!checkServerUpdate()){
				LOG("checkServerUpdate is false");
				return false;
			}
		}else{
			LOG("checkConfigUpdate");
			if(!checkConfigUpdate()){
				LOG("checkConfigUpdate is false");
				return false;
			}
		}

		LOG("mTargetURI="+mTargetURI);
		HttpClient httpClient = CustomerHttpClient.getHttpClient();
		HttpHead httpHead = new HttpHead(mTargetURI); 

		HttpResponse response = httpClient.execute(httpHead);       
		int statusCode = response.getStatusLine().getStatusCode();    

		LOG("statusCode="+statusCode);
		if(statusCode != 200) {
			return false;    
		}
		if(DEBUG){    
			for(Header header : response.getAllHeaders()){    
				LOG(header.getName()+":"+header.getValue());    
			}    
		}

		Header[] headLength = response.getHeaders("Content-Length");//response.getHeaders("OtaPackageLength");
		if(headLength == null) {
			return false;
		}
		if(headLength.length > 0) {
			mOtaPackageLength = headLength[0].getValue();
		}


		/*Header[] headName = response.getHeaders("OtaPackageName");
	    if(headName == null) {
	    	return false;
	    }
	    if(headName.length > 0) {
	    	mOtaPackageName = headName[0].getValue();
	    }*/

		/* Header[] headVersion = response.getHeaders("OtaPackageVersion");
	    if(headVersion == null) {
	    	return false;
	    }
	    if(headVersion.length > 0) {
	    	mOtaPackageVersion = headVersion[0].getValue();
	    }

	    Header[] headTargetURI = response.getHeaders("OtaPackageUri");
	    if(headTargetURI == null) {
	    	return false;
	    }
	    if(headTargetURI.length > 0) {
	    	mTargetURI = headTargetURI[0].getValue();
	    }*/
		mSystemVersion = OTAConfig.getSystemVersion();

		LOG("OtaPackageName = " + mOtaPackageName + " OtaPackageVersion = " + mOtaPackageVersion 
				+ " OtaPackageLength = " + mOtaPackageLength + " SystemVersion = " + mSystemVersion+"OtaVersionId="+mOtaVersionId
				+ " OtaPackageUri = " + mTargetURI);

		if(mOtaPackageLength == null || mOtaPackageName == null 
				|| mOtaPackageVersion == null || mTargetURI == null || mOtaVersionId ==null) {
			LOG("server response format error!");
			return false;
		}
		return true;
	}


}
