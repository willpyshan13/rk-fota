package android.rockchip.update.service;

import java.net.URI;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

public class OtaUpdateNotifyActivity extends Activity {
	private String TAG = "OtaUpdateNotifyActivity";
	private Context mContext;
	private String mRemoteURI = null;
    private String mOtaPackageVersion = null;
    private String mSystemVersion = null;
    private String mOtaPackageName = null;
    private String mOtaPackageLength = null;
    private String mOtaVersionId = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mContext = this;
		requestWindowFeature(Window.FEATURE_LEFT_ICON);
		setContentView(R.layout.notify_dialog);
		setFinishOnTouchOutside(false);
        getWindow().setFeatureDrawableResource(Window.FEATURE_LEFT_ICON,
                android.R.drawable.ic_dialog_alert);
        Intent startIntent = getIntent();
        mRemoteURI = startIntent.getStringExtra("uri");
        mOtaPackageVersion = startIntent.getStringExtra("OtaPackageVersion");
        mSystemVersion = startIntent.getStringExtra("SystemVersion");
        mOtaPackageName = startIntent.getStringExtra("OtaPackageName");
        mOtaPackageLength = startIntent.getStringExtra("OtaPackageLength");
        mOtaVersionId = startIntent.getStringExtra("OtaVersionId");
        
        long packageSize = Long.valueOf(mOtaPackageLength);
        String packageSize_string = null;
        if(packageSize < 1024) {
        	packageSize_string = String.valueOf(packageSize) + "B";
        }else if(packageSize/1024 > 0 && packageSize/1024/1024 == 0) {
        	packageSize_string = String.valueOf(packageSize/1024) + "KB";
        }else if(packageSize/1024/1024 > 0) {
        	packageSize_string = String.valueOf(packageSize/1024/1024) + "MB";
        }
        TextView txt = (TextView)this.findViewById(R.id.notify);
        txt.setText(getString(R.string.ota_update) + mOtaPackageVersion + getString(R.string.ota_package_size) + packageSize_string);
        
        Button btn_ok = (Button)this.findViewById(R.id.button_ok);
		Button btn_cancel = (Button)this.findViewById(R.id.button_cancel);
		btn_ok.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				Intent intent = new Intent(mContext, PackageDownloadActivity.class);
    			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    			intent.putExtra("uri", mRemoteURI);
    			intent.putExtra("OtaPackageLength", mOtaPackageLength);
    			intent.putExtra("OtaPackageName", mOtaPackageName);
    			intent.putExtra("OtaPackageVersion", mOtaPackageVersion);
    			intent.putExtra("SystemVersion", mSystemVersion);
    			intent.putExtra("OtaVersionId", mOtaVersionId);
    			mContext.startActivity(intent);
    			finish();
			}
		});
		
		btn_cancel.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				finish();
			}
		});
	}
	
	@Override
	protected void onStop() {
		finish();
		super.onStop();
	}

}
