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



public class MiYueCheckUpdateActivity extends Activity {
	private static final String TAG = "RKUpdateService.Setting";
	private Context mContext;
	private CheckBox mCH_AutoCheck;
	private Button mBtn_CheckNow;
	private SharedPreferences mAutoCheckSet;
	private TextView mVersionTv;
    private	Resources mResources;
    
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
		setContentView(R.layout.miyue_check_update);
		mContext = this;

		mContext.bindService(new Intent(mContext, RKUpdateService.class), mConnection, Context.BIND_AUTO_CREATE);

		mCH_AutoCheck = (CheckBox)this.findViewById(R.id.swh_auto_check);
		mBtn_CheckNow = (Button)this.findViewById(R.id.btn_check_now);
		
		mAutoCheckSet = getSharedPreferences("auto_check", MODE_PRIVATE);


        mResources = getResources();

		mVersionTv = (TextView)this.findViewById(R.id.version_text);
        mVersionTv.setText(mResources.getString(R.string.last_version, OTAConfig.getSystemVersion()));

		
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
			
		});
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

 			showCheckDialog();
			Intent serviceIntent;
			serviceIntent = new Intent(mContext,RKUpdateService.class);
			serviceIntent.putExtra("command", RKUpdateService.COMMAND_CHECK_REMOTE_UPDATING_BY_HAND);
			mContext.startService(serviceIntent);
			handler.postDelayed(task, 100); 

		}else{
			AlertDialog.Builder builder = new Builder(mContext); 
			builder.setTitle(mResources.getString(R.string.error)); 
			builder.setMessage(mResources.getString(R.string.network_error)); 

			builder.setPositiveButton(mResources.getString(R.string.settings),new DialogInterface.OnClickListener() { 

				@Override 
				public void onClick(DialogInterface dialog, int which) { 

					//intent = new Intent( android.provider.Settings.ACTION_WIRELESS_SETTINGS); 
					Intent intent = new Intent( android.provider.Settings.ACTION_SETTINGS); 

					mContext.startActivity(intent); 
				} 
			}); 
			builder.setNegativeButton(mResources.getString(R.string.cancel), new DialogInterface.OnClickListener() { 

				@Override 
				public void onClick(DialogInterface dialog, int which) { 

				} 
			}); 
			builder.create().show(); 
		}  

	}

   public void showCheckDialog()
	{
		checkDialog =  
		new Builder(mContext,AlertDialog.THEME_HOLO_DARK).setTitle(R.string.check_update).setMessage(R.string.check_updating).setNegativeButton(null,null).create();
		checkDialog.show();
	}

	public void dissCheckDialog()
	{
		if(checkDialog != null)
		{
			checkDialog.dismiss();
			checkDialog = null;
		}

	}

    public void showNoUpdateDialog()
    {
    	AlertDialog.Builder builder = new Builder(mContext,AlertDialog.THEME_HOLO_DARK); 
			builder.setTitle(R.string.check_update); 
			builder.setMessage(R.string.no_update); 

			builder.setPositiveButton(R.string.ok,null); 

			builder.create().show(); 
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
            	case ConStants.STATUS_CHECKEND: dissCheckDialog();  
            	        break;
            	case ConStants.STATUS_CHECKEND_NO_NEW: dissCheckDialog();  
            	        showNoUpdateDialog();
            	        break;  
            	case ConStants.STATUS_CHECKEND_LOCKED: dissCheckDialog();
            }  
        }    
    };   
}
