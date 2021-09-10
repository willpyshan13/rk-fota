package android.rockchip.update.service;

import java.lang.reflect.Field;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.apache.http.client.HttpClient;








import android.app.Activity;
import android.app.Dialog;
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
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

public class BackupForeignPackageDownloadActivity extends Activity {
	private String TAG = "ForeignPackageDownloadActivity";
	private Context mContext;
	private static PowerManager.WakeLock mWakeLock;
	private final static int AUTO_RETRY_COUNT = 10;//下载过程错误时，自动重试的次数 
	public int retryCount = AUTO_RETRY_COUNT;//计数器

	private String WAKELOCK_KEY = "myDownload";
	private HttpClient mHttpClient;
	private ProgressBar mProgressBar;
	private HTTPdownloadHandler mHttpDownloadHandler;
	//	private Button mBtnControl;
	//	private Button mBtnCancel;
	private TextView mDescriptionTV;
	//private TextView mDownloadRateTV;
	private TextView mDownloadProgressTV;

	private TextView mCompletedTV;
	private int mState = STATE_IDLE;
	//private TextView mTxtState;
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

	View mPageUpgradeInfoView;
	View mPageDownLoadView;


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

		
		setContentView(R.layout.foreign_package_download);
		setFinishOnTouchOutside(false);

		//mViewPager = (VerticalViewPager)findViewById(R.id.viewpager);
		
        mContext = this;
        mHttpUri = null;
        Intent intent = getIntent();
        String uriStr = intent.getStringExtra("uri");
        if(uriStr == null) {
        	finish();
        }
        
        
        
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

		

		mVersionId = intent.getStringExtra("OtaVersionId");

		mFileName = intent.getStringExtra("OtaPackageName");
		//int index = mFileName!=null ? mFileName.lastIndexOf("/"):-1;
		//mFileName = mFileName.substring(index != -1?index:0, mFileName.length());
		mOtaPackageVersion = intent.getStringExtra("OtaPackageVersion");
		mSystemVersion = intent.getStringExtra("SystemVersion");
		mOtaPackageDescription = intent.getStringExtra("OtaPackageDescription");
		//not finish activity
		PackageManager pm = getPackageManager();  
		homeInfo = pm.resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);

		mNotifyManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		mNotify = new Notification(R.drawable.ota_update, getString(R.string.app_name), System.currentTimeMillis());
		mNotify.contentView = new RemoteViews(getPackageName(), R.layout.download_notify); 
		mNotify.contentView.setProgressBar(R.id.pb_download, 100, 0, false);
		Intent notificationIntent = new Intent(this, this.getClass()); 
		PendingIntent pIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0); 
		mNotify.contentIntent = pIntent;    

		PowerManager powerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
		mWakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKELOCK_KEY);
		/*	mProgressBar = (ProgressBar)findViewById(R.id.progress_horizontal);
		mBtnControl = (Button)findViewById(R.id.btn_control);
		mBtnCancel = (Button)findViewById(R.id.button_cancel);
		mRemainTimeTV = (TextView)findViewById(R.id.download_info_remaining);
		mDownloadRateTV = (TextView)findViewById(R.id.download_info_rate);
		mCompletedTV = (TextView)findViewById(R.id.progress_completed);*/
		//mTxtState = (TextView)findViewById(R.id.txt_state);

		//mTxtState.setText("");       
		//		mBtnControl.setOnClickListener(new View.OnClickListener() {
		//			public void onClick(View v) {
		//				if(mState == STATE_IDLE || mState == STATE_STOPED) {
		//					//try to start
		//					mTask = new FileDownloadTask(mHttpClient, mUri, RKUpdateService.FLASH_ROOT, mFileName, 3);
		//					mTask.setProgressHandler(mProgressHandler);
		//					mTask.start();
		//					retryCount = AUTO_RETRY_COUNT;
		//					mBtnControl.setText(getString(R.string.starting));
		//					mBtnControl.setClickable(false);
		//					mBtnControl.setFocusable(false);
		//					mBtnCancel.setClickable(false);
		//					mBtnCancel.setFocusable(false);
		//				}else if(mState == STATE_STARTED) {
		//					//try to stop
		//					retryCount = 0;
		//					mTask.stopDownload();
		//					mBtnControl.setText(getString(R.string.stoping));
		//					mBtnControl.setClickable(false);
		//					mBtnControl.setFocusable(false);
		//					mBtnCancel.setClickable(false);
		//					mBtnCancel.setFocusable(false);
		//				}
		//			}
		//		});
		//
		//		mBtnCancel.setOnClickListener(new View.OnClickListener() {
		//			public void onClick(View v) {
		//				if(mState == STATE_IDLE || mState == STATE_STOPED) {
		//					finish();
		//				}else {
		//					if(mTask != null) {
		//						mTask.stopDownload();
		//						mIsCancelDownload = true;
		//					}else {
		//						finish();
		//					}
		//				}
		//			}
		//		});
		//
		//		mProgressBar.setIndeterminate(false);
		//		mProgressBar.setProgress(0);
		//		mProgressHandler = new ProgressHandler();
		//		mHttpClient = CustomerHttpClient.getHttpClient();
		//
		//		//try to start
		//		mTask = new FileDownloadTask(mHttpClient, mUri, RKUpdateService.FLASH_ROOT, mFileName, 3);
		//		mTask.setProgressHandler(mProgressHandler);
		//		mTask.start();
		//		retryCount = AUTO_RETRY_COUNT ;
		//		mBtnControl.setText(getString(R.string.starting));
		//		mBtnControl.setClickable(false);
		//		mBtnControl.setFocusable(false);

		initView();
		init();
	} 


	Button mBtnUpdate;
	Button mBtnDownload;

	TextView mUpdateVersionTv;
	TextView mDownloadVersionTv;
	
	View mDescriptionLayout;
	Dialog mDownloadDialog;
	void initView()
	{
		//将要分页显示的View装入数组中
		//		LayoutInflater mLi = LayoutInflater.from(this);
		//		View view1 = mLi.inflate(R.layout.upload, null);
		//		View view2 = mLi.inflate(R.layout.download, null);

mPageUpgradeInfoView = (View)findViewById(R.id.page_upgrade_info);
		
		mDownloadDialog = new Dialog(this,R.style.Theme_NewFonts_Holo_Light_Dialog_Notitle);
		LayoutInflater layoutinflater = LayoutInflater.from(this);
		mPageDownLoadView =  (View) layoutinflater.inflate(R.layout.foreign_download, null);
		mDownloadDialog.setContentView(mPageDownLoadView);

		
		mBtnUpdate = (Button)findViewById(R.id.update_btn);
		mBtnDownload = (Button)mPageDownLoadView.findViewById(R.id.download_btn);


		mUpdateVersionTv =(TextView)findViewById(R.id.update_version);
		mDownloadVersionTv =(TextView)mPageDownLoadView.findViewById(R.id.download_version);


		mProgressBar = (ProgressBar)mPageDownLoadView.findViewById(R.id.progress_horizontal);
		mDescriptionTV = (TextView)findViewById(R.id.description);

		mProgressBar.setIndeterminate(false);
		mProgressBar.setProgress(0);
		mHttpClient = CustomerHttpClient.getHttpClient();
		//mDownloadRateTV = (TextView)findViewById(R.id.download_info_rate);
		mDownloadProgressTV = (TextView)mPageDownLoadView.findViewById(R.id.download_progress_info);

		mBtnUpdate.setOnClickListener(ocl);
		mBtnDownload.setOnClickListener(ocl);

		 mDescriptionLayout = (View)findViewById(R.id.description_layout);

		mUpdateVersionTv.setText(mOtaPackageVersion);
		mDownloadVersionTv.setText(mOtaPackageVersion);
		mDescriptionTV.setText(mOtaPackageDescription);
		mDescriptionTV.setMovementMethod(ScrollingMovementMethod.getInstance());
		mDescriptionTV.setOnFocusChangeListener(new OnFocusChangeListener() {
			 
			@Override
			public void onFocusChange(View arg0, boolean arg1) {
				// TODO Auto-generated method stub
				mDescriptionLayout.setBackgroundResource(arg1?R.drawable.update_content_custom:R.color.transparent);
				//latout.setBackgroundResource(arg1?R.drawable.content_pressed:R.drawable.content_normal);
			}
		});
		mBtnUpdate.requestFocus();
	}

	android.view.View.OnClickListener ocl = new android.view.View.OnClickListener() {

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch (v.getId()) {
			case R.id.update_btn://点击开始下载
				setCurrentPage(PAGE_DOWNLOAD);
				//mViewPager.setCurrentItem(1, true);
				if(mState == STATE_IDLE || mState == STATE_STOPED) {
					//try to start
					if(mDownloadProtocol == DOWNLOAD_PROTOCOL_HTTP){
						mHttpTask = new HTTPFileDownloadTask(mHttpClient, mHttpUri, RKUpdateService.FLASH_ROOT, mFileName, mVersionId,DOWNLOAD_THREAD_COUNT);
						mHttpTask.setProgressHandler(mHttpDownloadHandler);
						mHttpTask.start();
					}else {
						mFtpTask = new FTPFileDownloadTask(mFTPRequest, RKUpdateService.FLASH_ROOT, mFileName);
						mFtpTask.setProgressHandler(mHttpDownloadHandler);
						mFtpTask.start();
					}
					retryCount = AUTO_RETRY_COUNT;
					mBtnDownload.setText(getString(R.string.cancel));
				}else if(mState == STATE_STARTED) {
					//try to stop
					//retryCount = 0;
					//mTask.stopDownload();
					//mBtnDownload.setText(getString(R.string.stoping));
				}
				break;

			case R.id.download_btn:
				if(mState == STATE_IDLE || mState == STATE_STOPED) {
					if(mDownloadProtocol == DOWNLOAD_PROTOCOL_HTTP){
						mHttpTask = new HTTPFileDownloadTask(mHttpClient, mHttpUri, RKUpdateService.FLASH_ROOT, mFileName,mVersionId, DOWNLOAD_THREAD_COUNT);
						mHttpTask.setProgressHandler(mHttpDownloadHandler);
						mHttpTask.start();
					}else {
						mFtpTask = new FTPFileDownloadTask(mFTPRequest, RKUpdateService.FLASH_ROOT, mFileName);
						mFtpTask.setProgressHandler(mHttpDownloadHandler);
						mFtpTask.start();
					}
					retryCount = AUTO_RETRY_COUNT;
					mBtnDownload.setText(getString(R.string.cancel));
				}else if(mState == STATE_STARTED) {
					//try to stop
					if(mDownloadProtocol == DOWNLOAD_PROTOCOL_HTTP){
						mHttpTask.stopDownload();
					}else {
						mFtpTask.stopDownload();
					}
					mIsCancelDownload = true;
					mDownloadDialog.dismiss();
				}
				break;



			}
		}};
		private void init()
		{
			 mHttpDownloadHandler = new HTTPdownloadHandler();
		        mHttpClient = CustomerHttpClient.getHttpClient();
				
		        //try to start
		        if(mDownloadProtocol == DOWNLOAD_PROTOCOL_HTTP){
					mHttpTask = new HTTPFileDownloadTask(mHttpClient, mHttpUri, RKUpdateService.FLASH_ROOT, mFileName,mVersionId, DOWNLOAD_THREAD_COUNT);
					mHttpTask.setProgressHandler(mHttpDownloadHandler);
					mHttpTask.start();
				}else {
					mFtpTask = new FTPFileDownloadTask(mFTPRequest, RKUpdateService.FLASH_ROOT, mFileName);
					mFtpTask.setProgressHandler(mHttpDownloadHandler);
					mFtpTask.start();
				}
		}
		
		static final int PAGE_DOWNLOAD = 1;
		static final int PAGE_UPGRADE_INFO = 0;


		private void setCurrentPage(int page)
		{

			switch (page) {
			case PAGE_DOWNLOAD:
				mDownloadDialog.show();
				break;

			case PAGE_UPGRADE_INFO:
				if(mDownloadDialog != null)mDownloadDialog.dismiss();
				break;
			}

		}


		private class HTTPdownloadHandler extends Handler {

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

					setDownloadInfoViews(contentLength, receivedCount, receivedPerSecond);
					mProgressBar.setProgress(percent);
					setNotificationProgress(percent);
					showNotification();
				}
				break;
				case HTTPFileDownloadTask.PROGRESS_DOWNLOAD_COMPLETE : {
					//mTxtState.setText("State: download complete");
					mState = STATE_IDLE;
					mBtnDownload.setText(getString(R.string.start));
					//					mBtnControl.setClickable(true);
					//					mBtnControl.setFocusable(true);
					//					mBtnCancel.setClickable(true);
					//					mBtnCancel.setFocusable(true);
					Intent intent = new Intent();
					intent.setClass(mContext, UpdateAndRebootActivity.class);
					intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					intent.putExtra(RKUpdateService.EXTRA_IMAGE_PATH, RKUpdateService.FLASH_ROOT + "/" + mFileName);
					startActivity(intent);
					finish();
				}
				break;
				case HTTPFileDownloadTask.PROGRESS_START_COMPLETE : {
					//mTxtState.setText("");
					mState = STATE_STARTED;
					//mBtnDownload.setText(getString(R.string.pause));
					//					mBtnControl.setClickable(true);
					//					mBtnControl.setFocusable(true);
					//					mBtnCancel.setClickable(true);
					//					mBtnCancel.setFocusable(true);
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
						if(mDownloadProtocol == DOWNLOAD_PROTOCOL_HTTP){
							mHttpTask = new HTTPFileDownloadTask(mHttpClient, mHttpUri, RKUpdateService.FLASH_ROOT, mFileName, mVersionId,DOWNLOAD_THREAD_COUNT);
							mHttpTask.setProgressHandler(mHttpDownloadHandler);
							mHttpTask.start();
						}else {
							mFtpTask = new FTPFileDownloadTask(mFTPRequest, RKUpdateService.FLASH_ROOT, mFileName);
							mFtpTask.setProgressHandler(mHttpDownloadHandler);
							mFtpTask.start();
						}

					}else{


						mState = STATE_STOPED;
						//mRemainTimeTV.setText("");
						//mDownloadRateTV.setText("");
						mBtnDownload.setText(getString(R.string.retry));
						//						mBtnControl.setClickable(true);
						//						mBtnControl.setFocusable(true);
						//						mBtnCancel.setClickable(true);
						//						mBtnCancel.setFocusable(true);
						setNotificationPause();
						showNotification();
						if(mWakeLock.isHeld()){
							mWakeLock.release();
						}

						if(mIsCancelDownload) {
							//finish();
						}
					}
				}
				break;
				default:
					break;
				}
			}  	
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

		private void setDownloadInfoViews(long contentLength, long receivedCount, long receivedPerSecond) {
			int percent = (int)(receivedCount * 100 / contentLength);
			//mCompletedTV.setText(String.valueOf(percent) + "%");
			mDownloadProgressTV.setText(String.valueOf(percent) + "%");
			String rate = "";
			if(receivedPerSecond < 1024) {
				rate = String.valueOf(receivedPerSecond) + "B/S";
			}else if(receivedPerSecond/1024 > 0 && receivedPerSecond/1024/1024 == 0) {
				rate = String.valueOf(receivedPerSecond/1024) + "KB/S";
			}else if(receivedPerSecond/1024/1024 > 0) {
				rate = String.valueOf(receivedPerSecond/1024/1024) + "MB/S";
			}

			//mDownloadRateTV.setText(rate);

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
			//mRemainTimeTV.setText(remainSecondString);
		}

		@Override
		protected void onDestroy() {
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
		protected void onPause() {
			Log.d(TAG, "onPause");
			super.onPause();
		}

		@Override
		protected void onRestart() {
			Log.d(TAG, "onRestart");
			super.onRestart();
		}

		@Override
		protected void onStart() {
			Log.d(TAG, "onStart");
			super.onStart();
		}

		@Override
		protected void onStop() {
			Log.d(TAG, "onStop");	
			super.onStop();
			if(mState == STATE_IDLE || mState == STATE_STOPED) {
				finish();
			}else {
				if(mDownloadProtocol == DOWNLOAD_PROTOCOL_HTTP){
					mHttpTask.stopDownload();
				}else {
					mFtpTask.stopDownload();
				}
				mIsCancelDownload = true;
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