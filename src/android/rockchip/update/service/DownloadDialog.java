package android.rockchip.update.service;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

public class DownloadDialog extends Dialog{

	public DownloadDialog(Context context, int theme) {
		super(context, theme);
		// TODO Auto-generated constructor stub
	}
	private Button mBtnDownload;
	private ProgressBar mProgressBar;
	private TextView mDownloadVersionTv;
	private TextView mDownloadProgressTV;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.foreign_download);
		mBtnDownload = (Button)findViewById(R.id.download_btn);
		mDownloadVersionTv =(TextView)findViewById(R.id.download_version);
		mProgressBar = (ProgressBar)findViewById(R.id.progress_horizontal);
		mDownloadProgressTV = (TextView)findViewById(R.id.download_progress_info);
		
	}
	
	
	

}
