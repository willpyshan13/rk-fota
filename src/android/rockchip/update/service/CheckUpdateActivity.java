package android.rockchip.update.service;


import java.io.File;

import android.rockchip.update.ConStants;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.CheckBox;
import android.rockchip.update.util.NetWork;
import android.os.IBinder;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.widget.TextView;
import android.os.Handler;
import android.util.Log;



public class CheckUpdateActivity extends Activity {
	private static final String TAG = "RKUpdateService.Setting";
	private Context mContext;
	private CheckBox mCH_AutoCheck;
	private Button mBtn_CheckNow;
	private SharedPreferences mAutoCheckSet;
	private TextView mVersionTv;
	private	Resources mResources;


	private View checkingLayout;
	private Button checkingCancelBtn;


	private View checkendLayout;
	private Button checkendBtn;
	private TextView checkendTv;
	private ImageView checkendImageView;


	AlertDialog checkDialog = null;

	private RKUpdateService.LocalBinder mBinder;
	private ServiceConnection mConnection = new ServiceConnection() { 
		public void onServiceConnected(ComponentName className, IBinder service) { 
			mBinder = (RKUpdateService.LocalBinder)service;
		} 

		public void onServiceDisconnected(ComponentName className) { 
			mBinder = null;
		} 


	}; 


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.check_update);
		mContext = this;
		mResources = getResources();

		mContext.bindService(new Intent(mContext, RKUpdateService.class), mConnection, Context.BIND_AUTO_CREATE);


		checkingLayout =(View)findViewById(R.id.checking_layout);
		checkingCancelBtn =(Button)findViewById(R.id.cancel_checking);

		checkendLayout =(View)findViewById(R.id.check_end_layout);
		checkendBtn =(Button)findViewById(R.id.check_end_btn);
		checkendTv = (TextView)findViewById(R.id.check_end_tv);
		checkendImageView = (ImageView)findViewById(R.id.check_end_iv);

		checkingCancelBtn.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent serviceIntent;
				serviceIntent = new Intent("android.rockchip.update.service");
				serviceIntent.setPackage(getPackageName());
				serviceIntent.putExtra("otaChecking", false);
				mContext.startService(serviceIntent);
			}
		});


		checkendBtn.setOnClickListener(new Button.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				finish();
			}
		});


		/*		mCH_AutoCheck = (CheckBox)this.findViewById(R.id.swh_auto_check);
		mBtn_CheckNow = (Button)this.findViewById(R.id.btn_check_now);

		mAutoCheckSet = getSharedPreferences("auto_check", MODE_PRIVATE);




		mVersionTv = (TextView)this.findViewById(R.id.version_text);
        mVersionTv.setText(mResources.getString(R.string.last_version, getSystemVersion()));


		mCH_AutoCheck.setChecked(mAutoCheckSet.getBoolean("auto_check", true));
		mCH_AutoCheck.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				Editor e = mAutoCheckSet.edit();
				e.putBoolean("auto_check", isChecked);
				e.commit();

				if(isChecked)// when autocheck new status is checked,then check once
				{
					Intent serviceIntent;
					serviceIntent = new Intent("android.rockchip.update.service");
                	serviceIntent.putExtra("command", RKUpdateService.COMMAND_CHECK_REMOTE_UPDATING_BY_HAND);
                	mContext.startService(serviceIntent);
                }
			}

		});

		mBtn_CheckNow.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				checkUpdateByUser();
			}

		});*/
	}



	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		handler.postAtTime(new Runnable() {

			@Override
			public void run() {
				// TODO Auto-generated method stub
				checkUpdateByUser();
			}
		}, 1000);

	}








	@Override
	public Context createPackageContextAsUser(String arg0, int arg1,
			UserHandle arg2) throws NameNotFoundException {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public File getSharedPrefsFile(String arg0) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public Intent registerReceiverAsUser(BroadcastReceiver arg0,
			UserHandle arg1, IntentFilter arg2, String arg3, Handler arg4) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public ComponentName startServiceAsUser(Intent arg0, UserHandle arg1) {
		// TODO Auto-generated method stub
		return null;
	}



	@Override
	public boolean stopServiceAsUser(Intent arg0, UserHandle arg1) {
		// TODO Auto-generated method stub
		return false;
	}






	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
		handler.removeCallbacks(task);
	}



	@Override
	protected void onDestroy() {
		Log.d(TAG, "ondestroy");
		//if(mBinder != null) {
		//	mBinder.unLockWorkHandler();
		//}
		mContext.unbindService(mConnection);
		super.onDestroy();
	}



	public void checkUpdateByUser()
	{
		if(NetWork.checkNetworkConnect(mContext))
		{

			Intent serviceIntent;
			serviceIntent = new Intent("android.rockchip.update.service");
			serviceIntent.putExtra("command", RKUpdateService.COMMAND_CHECK_REMOTE_UPDATING_BY_HAND);
			serviceIntent.setPackage(getPackageName());
			mContext.startService(serviceIntent);
			handler.postDelayed(task, 100); 

		}else{

			checkingLayout.setVisibility(View.GONE);
			checkendLayout.setVisibility(View.VISIBLE);
			checkendImageView.setImageResource(R.drawable.check_err_icon);
			checkendTv.setText(mResources.getString(R.string.network_error));
			checkendBtn.requestFocus();
		}  

	}


	public void showNoUpdateDialog()
	{
		
		checkingLayout.setVisibility(View.GONE);
		checkendLayout.setVisibility(View.VISIBLE);
		checkendImageView.setImageResource(R.drawable.checked_icon);
		checkendTv.setText(mResources.getString(R.string.no_update));
		checkendBtn.requestFocus();
	}




	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if(keyCode == KeyEvent.KEYCODE_BACK)
		{
			if(mBinder != null)
			{
				int status = mBinder.getCheckStatus();
				if(status == ConStants.STATUS_CHECKEND_NO_NEW || status == ConStants.STATUS_CHECKEND_ERROR)
				{
					finish();
				}else if(status == ConStants.STATUS_CHECKING)
				{

					Intent serviceIntent;
					serviceIntent = new Intent("android.rockchip.update.service");
					serviceIntent.setPackage(getPackageName());
					serviceIntent.putExtra("otaChecking", false);
					mContext.startService(serviceIntent);
					finish();

				}

			}
		}

		return super.onKeyDown(keyCode, event);
	}




	private final Handler handler = new Handler(); 
	private final Runnable task = new Runnable() {    

		public void run() {    
			// TODO Auto-generated method stub 
			int status = mBinder.getCheckStatus();//0 is  checking,1 is check end and have new_vewsion,2 1 is check end and have no new_vewsion
			switch(status)
			{
			case ConStants.STATUS_CHECKING: handler.postDelayed(this, 100);  
			break;
			case ConStants.STATUS_CHECKEND:finish();   
			break;
			case ConStants.STATUS_CHECKEND_NO_NEW:
				showNoUpdateDialog();
				break; 
			case ConStants.STATUS_CHECKEND_ERROR:
				showNoUpdateDialog();
				break;  
			case ConStants.STATUS_CHECKEND_LOCKED: 
				break;
			}  
		}    
	};   
}
