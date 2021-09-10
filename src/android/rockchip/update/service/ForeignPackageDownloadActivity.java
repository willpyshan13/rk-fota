package android.rockchip.update.service;

import android.app.Dialog;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class ForeignPackageDownloadActivity extends BasePackageDownloadActivity implements OnClickListener{


	View mPageUpgradeInfoView;
	View mPageDownLoadView;

	Button mBtnUpdate;
	Button mBtnCancel;
	Button mBtnDownload;

	TextView mUpdateVersionTv;
	TextView mDownloadVersionTv;

	View mDescriptionLayout;
	Dialog mDownloadDialog;
	
	private ProgressBar mProgressBar;
	private TextView mDescriptionTV;
	private TextView mDownloadProgressTV;
	
	@Override
	protected void initContentView() {
		// TODO Auto-generated method stub
		super.initContentView();
		setContentView(R.layout.foreign_package_download);

		mPageUpgradeInfoView = (View)findViewById(R.id.page_upgrade_info);

		mDownloadDialog = new Dialog(this,R.style.Theme_NewFonts_Holo_Light_Dialog_Notitle);
		LayoutInflater layoutinflater = LayoutInflater.from(this);
		mPageDownLoadView =  (View) layoutinflater.inflate(R.layout.foreign_download, null);
		mDownloadDialog.setContentView(mPageDownLoadView);


		mBtnUpdate = (Button)findViewById(R.id.update_btn);
		mBtnCancel = (Button)findViewById(R.id.cancel_btn);
		mBtnDownload = (Button)mPageDownLoadView.findViewById(R.id.download_btn);


		mUpdateVersionTv =(TextView)findViewById(R.id.update_version);
		mDownloadVersionTv =(TextView)mPageDownLoadView.findViewById(R.id.download_version);


		mProgressBar = (ProgressBar)mPageDownLoadView.findViewById(R.id.progress_horizontal);
		mDescriptionTV = (TextView)findViewById(R.id.description);

		mProgressBar.setIndeterminate(false);
		mProgressBar.setProgress(0);
		mDownloadProgressTV = (TextView)mPageDownLoadView.findViewById(R.id.download_progress_info);

		mBtnUpdate.setOnClickListener(this);
		mBtnDownload.setOnClickListener(this);
		mBtnCancel.setOnClickListener(this);

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
	

	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub
		switch (v.getId()) {
		case R.id.update_btn://点击开始下载
			setCurrentPage(PAGE_DOWNLOAD);
			if(getDownloadState() == STATE_IDLE || getDownloadState() == STATE_STOPED) {
				//try to start
				startDownLoad();
				mBtnDownload.setText(getString(R.string.cancel));
			}else if(getDownloadState() == STATE_STARTED) {
				//try to stop
				//retryCount = 0;
				//mTask.stopDownload();
				//mBtnDownload.setText(getString(R.string.stoping));
			}
			break;
		case R.id.cancel_btn:
			finish();
			break;
		case R.id.download_btn:
			if(getDownloadState() == STATE_IDLE || getDownloadState() == STATE_STOPED) {
				startDownLoad();
				mBtnDownload.setText(getString(R.string.cancel));
			}else if(getDownloadState() == STATE_STARTED) {
				//try to stop
				stopDownLoad();
				mDownloadDialog.dismiss();
			}
			break;
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


	@Override
	public void updateDownloadState() {
		// TODO Auto-generated method stub
		super.updateDownloadState();
		int state = getDownloadState() ;
		switch (state) {
		case STATE_IDLE:
			mBtnDownload.setText(getString(R.string.start));
			break;
		case STATE_STOPED:
			mBtnDownload.setText(getString(R.string.retry));
			break;

		default:
			break;
		}
	}


	@Override
	public void setDownloadInfoViews(int percent, String rateString,
			String remainSecondString) {
		// TODO Auto-generated method stub
		super.setDownloadInfoViews(percent, rateString, remainSecondString);
		String percentString = String.valueOf(percent) + "%";
		mDownloadProgressTV.setText(percentString);
		mProgressBar.setProgress(percent);
	}
	
	

}
