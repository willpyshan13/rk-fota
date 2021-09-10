package android.rockchip.update.service;

import java.net.URI;
import java.net.URISyntaxException;
import org.apache.http.client.HttpClient;
import android.app.Activity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.rockchip.update.util.FTPRequestInfo;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.Toast;

public class BasePackageDownloadActivity extends FullscreenActivity {
	private String TAG = "PackageDownloadActivity";
	private Context mContext;
	private static PowerManager.WakeLock mWakeLock;
	private final static int AUTO_RETRY_COUNT = 10;//下载过程错误时，自动重试的次数 
	public int retryCount = AUTO_RETRY_COUNT;//计数器

	private String WAKELOCK_KEY = "myDownload";
	private HttpClient mHttpClient;
	private HTTPdownloadHandler mHttpDownloadHandler;


	private int mState = STATE_IDLE;

	private ResolveInfo homeInfo;
	private NotificationManager mNotifyManager;
	private Notification mNotify;
	private int notification_id = 20110921;
	private HTTPFileDownloadTask mHttpTask;
	private FTPFileDownloadTask mFtpTask;
	private URI mHttpUri;
	private FTPRequestInfo mFTPRequest;
	private int mDownloadProtocol = 0;
	private String mFileName;
	private String mVersionId;
	private RKUpdateService.LocalBinder mBinder;
	private volatile boolean mIsCancelDownload = false;

	private static final int DOWNLOAD_THREAD_COUNT = 1;
	public static final int STATE_IDLE = 0;
	public static final int STATE_STARTING = 1; //not used
	public static final int STATE_STARTED = 2;
	public static final int STATE_STOPING = 3; //not used
	public static final int STATE_STOPED = 4;
	public static final int STATE_ERROR = 5; //not used

	public static final int DOWNLOAD_PROTOCOL_HTTP = 0;
	public static final int DOWNLOAD_PROTOCOL_FTP = 1;



	private ServiceConnection mConnection = new ServiceConnection() { 
		public void onServiceConnected(ComponentName className, IBinder service) { 
			mBinder = (RKUpdateService.LocalBinder)service;
			mBinder.LockWorkHandler();
		} 

		public void onServiceDisconnected(ComponentName className) { 
			mBinder = null;
		} 


	}; 


	String mOtaPackageVersion;
	String mSystemVersion;
	String mOtaPackageDescription;	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mContext = this;
		mHttpUri = null;
		Intent intent = getIntent();
		String uriStr = intent.getStringExtra("uri");
		if(uriStr == null) {
			finish();
		}
		mVersionId = intent.getStringExtra("OtaVersionId");

		mFileName = intent.getStringExtra("OtaPackageName");
		//int index = mFileName!=null ? mFileName.lastIndexOf("/"):-1;
		//mFileName = mFileName.substring(index != -1?index:0, mFileName.length());
		mOtaPackageVersion = intent.getStringExtra("OtaPackageVersion");
		mSystemVersion = intent.getStringExtra("SystemVersion");
		mOtaPackageDescription = intent.getStringExtra("OtaPackageDescription");
		
		Log.v("RKUpdateService", "BasePackageDownloadActivity mOtaPackageVersion:"+mOtaPackageVersion
				+",mSystemVersion:"+mSystemVersion+",mOtaPackageDescription:"+mOtaPackageDescription);

		initContentView();

		setFinishOnTouchOutside(false);

		if(uriStr.startsWith("ftp://")) {
			mDownloadProtocol = DOWNLOAD_PROTOCOL_FTP;
			mFTPRequest = parseFtpUri(uriStr);
		}else {
			mDownloadProtocol = DOWNLOAD_PROTOCOL_HTTP;
			try {
				mHttpUri = new URI(uriStr);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}
		}

		mContext.bindService(new Intent(mContext, RKUpdateService.class), mConnection, Context.BIND_AUTO_CREATE);




		//not finish activity
		PackageManager pm = getPackageManager();  
		homeInfo = pm.resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);

		mNotifyManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		mNotify = new Notification(R.drawable.ota_update, getString(R.string.app_name), System.currentTimeMillis());
		mNotify.contentView = new RemoteViews(getPackageName(), R.layout.download_notify); 
		mNotify.contentView.setProgressBar(R.id.pb_download, 100, 0, false);
		Intent notificationIntent = new Intent(this, PackageDownloadActivity.class); 
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0); 
		mNotify.contentIntent = pIntent;    

		PowerManager powerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
		mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_KEY);

		init();
	} 

	protected void initContentView()
	{

	}
	private void init()
	{
		mHttpDownloadHandler = new HTTPdownloadHandler();
		mHttpClient = CustomerHttpClient.getHttpClient();

		//try to start
		//startDownLoad();
	}
	public void startDownLoad()
	{
		startDownLoad(true);
	}
	public void startDownLoad(boolean reset) {
		if(mDownloadProtocol == DOWNLOAD_PROTOCOL_HTTP){
			mHttpTask = new HTTPFileDownloadTask(mHttpClient, mHttpUri, RKUpdateService.FLASH_ROOT, mFileName,mVersionId, DOWNLOAD_THREAD_COUNT);
			mHttpTask.setProgressHandler(mHttpDownloadHandler);
			mHttpTask.start();
		}else {
			mFtpTask = new FTPFileDownloadTask(mFTPRequest, RKUpdateService.FLASH_ROOT, mFileName);
			mFtpTask.setProgressHandler(mHttpDownloadHandler);
			mFtpTask.start();
		}
		if(reset)retryCount = AUTO_RETRY_COUNT;
	}
	public void stopDownLoad() {
		if(mDownloadProtocol == DOWNLOAD_PROTOCOL_HTTP){
			mHttpTask.stopDownload();
		}else {
			mFtpTask.stopDownload();
		}
		mIsCancelDownload = true;
	}



	public class HTTPdownloadHandler extends Handler {

		@Override
		public void handleMessage(Message msg) {
			int whatMassage = msg.what;
			switch(whatMassage) {
			case HTTPFileDownloadTask.PROGRESS_UPDATE : {
				Bundle b = msg.getData();
				long receivedCount = b.getLong("ReceivedCount", 0);
				long contentLength = b.getLong("ContentLength", 0);
				long receivedPerSecond = b.getLong("ReceivedPerSecond", 0);
				int percent = (int)(receivedCount * 100 / contentLength);
				Log.d(TAG, "percent = " + percent);

				setDownloadInfo(contentLength, receivedCount, receivedPerSecond);
				setNotificationProgress(percent);
				showNotification();
			}
			break;
			case HTTPFileDownloadTask.PROGRESS_DOWNLOAD_COMPLETE : {
				mState = STATE_IDLE;
				updateDownloadState();

				Intent intent = new Intent();
				intent.setClass(mContext, UpdateAndRebootActivity.class);
				intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				intent.putExtra(RKUpdateService.EXTRA_IMAGE_PATH, RKUpdateService.FLASH_ROOT + "/" + mFileName);
				startActivity(intent);
				finish();
			}
			break;
			case HTTPFileDownloadTask.PROGRESS_START_COMPLETE : {
				mState = STATE_STARTED;

				setNotificationStrat();
				showNotification();
				mWakeLock.acquire();
			}
			break;
			case HTTPFileDownloadTask.PROGRESS_STOP_COMPLETE : {
				Bundle b  = msg.getData();
				int errCode = b.getInt("err", HTTPFileDownloadTask.ERR_NOERR);
				if(errCode == HTTPFileDownloadTask.ERR_CONNECT_TIMEOUT) {
					//mTxtState.setText("State: ERR_CONNECT_TIMEOUT");
					Toast.makeText(getApplicationContext(), getString(R.string.error_display), Toast.LENGTH_LONG).show();
				}else if(errCode == HTTPFileDownloadTask.ERR_FILELENGTH_NOMATCH) {
					//mTxtState.setText("State: ERR_FILELENGTH_NOMATCH");
				}else if(errCode == HTTPFileDownloadTask.ERR_NOT_EXISTS) {
					//mTxtState.setText("State: ERR_NOT_EXISTS");
					Toast.makeText(getApplicationContext(), getString(R.string.error_display), Toast.LENGTH_LONG).show();
				}else if(errCode == HTTPFileDownloadTask.ERR_REQUEST_STOP) {
					//mTxtState.setText("State: ERR_REQUEST_STOP");
				}else if(errCode == HTTPFileDownloadTask.ERR_UNKNOWN) {
					Toast.makeText(getApplicationContext(), getString(R.string.error_display), Toast.LENGTH_LONG).show();
				}

				if(!mIsCancelDownload && retryCount-- >0)
				{
					Log.v("FileDownloadTask","download error and autoretry,retry count ="+retryCount);
					startDownLoad(false);
					
				}else{
					mState = STATE_STOPED;
					updateDownloadState();

					setNotificationPause();
					showNotification();
					if(mWakeLock.isHeld()){
						mWakeLock.release();
					}

					if(mIsCancelDownload) {
						finish();
					}
				}
			}
			break;
			default:
				break;
			}
		}  	
	}
	
	public void updateDownloadState() {
		
	}
	
	public int  getDownloadState() {
		return mState;
	}

	private void showNotification() {
		mNotifyManager.notify(notification_id, mNotify);
	}

	private void clearNotification() {
		mNotifyManager.cancel(notification_id);
	}

	private void setNotificationProgress(int percent) {
		mNotify.contentView.setProgressBar(R.id.pb_download, 100, percent, false);
		mNotify.contentView.setTextViewText(R.id.pb_percent, String.valueOf(percent) + "%");
	}

	private void setNotificationPause() {
		mNotify.contentView.setTextViewText(R.id.pb_title, mContext.getString(R.string.pb_title_pause));
		mNotify.contentView.setViewVisibility(R.id.image_pause, View.VISIBLE);
	}

	private void setNotificationStrat() {
		mNotify.contentView.setTextViewText(R.id.pb_title, mContext.getString(R.string.pb_title_downloading));
		mNotify.contentView.setViewVisibility(R.id.image_pause, View.GONE);
	}

	private void setDownloadInfo(long contentLength, long receivedCount, long receivedPerSecond) {
		int percent = (int)(receivedCount * 100 / contentLength);

		String percentString = String.valueOf(percent) + "%";
		String rate = "";
		if(receivedPerSecond < 1024) {
			rate = String.valueOf(receivedPerSecond) + "B/S";
		}else if(receivedPerSecond/1024 > 0 && receivedPerSecond/1024/1024 == 0) {
			rate = String.valueOf(receivedPerSecond/1024) + "KB/S";
		}else if(receivedPerSecond/1024/1024 > 0) {
			rate = String.valueOf(receivedPerSecond/1024/1024) + "MB/S";
		}
		
		int remainSecond = (receivedPerSecond == 0) ? 0 : (int)((contentLength - receivedCount) / receivedPerSecond);
		String remainSecondString = "";
		if(remainSecond < 60) {
			remainSecondString = String.valueOf(remainSecond) + "s";
		}else if(remainSecond/60 > 0 && remainSecond/60/60 == 0) {
			remainSecondString = String.valueOf(remainSecond/60) + "min";
		}else if(remainSecond/60/60 > 0) {
			remainSecondString = String.valueOf(remainSecond/60/60) + "h";
		}
		remainSecondString = mContext.getString(R.string.remain_time) + " " + remainSecondString;
		
		setDownloadInfoViews(percent,rate,remainSecondString);
	}
	
	public void  setDownloadInfoViews(int  percent , String rateString,String remainSecondString) {
		
	}

	@Override
	public void onDestroy() {
		Log.d(TAG, "ondestroy");

		if(mWakeLock.isHeld()){
			mWakeLock.release();
		}
		clearNotification();
		if(mBinder != null) {
			mBinder.unLockWorkHandler();
		}
		mContext.unbindService(mConnection);
		super.onDestroy();
	}
	


	@Override
	public void onStop() {
		Log.d(TAG, "onStop");	
		super.onStop();
		if(mState == STATE_IDLE || mState == STATE_STOPED) {
			finish();
		}else {
			stopDownLoad();
			finish();
		}
	}

	public boolean onKeyDown(int keyCode, KeyEvent event) {  
		if (keyCode == KeyEvent.KEYCODE_BACK) {  
			ActivityInfo ai = homeInfo.activityInfo;  
			Intent startIntent = new Intent(Intent.ACTION_MAIN);  
			startIntent.addCategory(Intent.CATEGORY_LAUNCHER);  
			startIntent.setComponent(new ComponentName(ai.packageName, ai.name));  
			startActivitySafely(startIntent);  
			return true;  
		} else { 
			return super.onKeyDown(keyCode, event);  
		}  
	}

	void startActivitySafely(Intent intent) {  
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);  
		try {  
			startActivity(intent);  
		} catch (ActivityNotFoundException e) {    

		} catch (SecurityException e) {  

		}  
	}

	private FTPRequestInfo parseFtpUri(String uri) {
		FTPRequestInfo info = new FTPRequestInfo();
		try {	    	
			String[] s = uri.split("//");
			if(s[1].contains("@")) {
				String[] s2 = s[1].split("@", 2);
				String[] s3 = s2[0].split(":", 2);
				info.setUsername(s3[0]);
				info.setPassword(s3[1]);

				String[] s4 = s2[1].split(":", 2);
				if(s4.length > 1) {
					info.setHost(s4[0]);
					info.setPort(Integer.valueOf(s4[1].substring(0, s4[1].indexOf("/"))));
					info.setRequestPath(s4[1].substring(s4[1].indexOf("/")));
				}else {
					info.setHost(s4[0].substring(0, s4[0].indexOf("/")));
					info.setRequestPath(s4[0].substring(s4[0].indexOf("/")));
				}
			}else {
				String[] str = s[1].split(":", 2);
				if(str.length > 1) {
					info.setHost(str[0]);
					info.setPort(Integer.valueOf(str[1].substring(0, str[1].indexOf("/"))));
					info.setRequestPath(str[1].substring(str[1].indexOf("/")));
				}else {
					info.setHost(str[0].substring(0, str[0].indexOf("/")));
					info.setRequestPath(str[0].substring(str[0].indexOf("/")));
				}
			}
		}catch (Exception e) {
			Log.e(TAG, "parseFtpUri error....!");
		}

		info.dump();
		return info;
	}
}