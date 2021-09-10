package android.rockchip.update.service;


import android.app.Activity;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnSystemUiVisibilityChangeListener;


public class FullscreenActivity extends Activity implements OnSystemUiVisibilityChangeListener {
	
	private static final int SYSTEM_UI_FLAG_SHOW_FULLSCREEN = 0x00000008;  //View.SYSTEM_UI_FLAG_SHOW_FULLSCREEN
	
	public static final int SYSTEM_UI_FLAG_LAYOUT_ALWAYS_HIDE_SYSTEMBAR = 0x00004000;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
//		getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN);
//		getWindow().getDecorView().setSystemUiVisibility(SYSTEM_UI_FLAG_SHOW_FULLSCREEN);
//		getWindow().getDecorView().setSystemUiVisibility(SYSTEM_UI_FLAG_LAYOUT_ALWAYS_HIDE_SYSTEMBAR);
		hideSystemUI();
		getWindow().getDecorView().setOnSystemUiVisibilityChangeListener(this);
		
		
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		super.onWindowFocusChanged(hasFocus);
		 if (hasFocus) {
		 	hideSystemUI();
		 }
	}

	@Override
	public void onSystemUiVisibilityChange(int visibility) {
		hideSystemUI();

	}
	private void hideSystemUI()
	{
		getWindow().getDecorView().setSystemUiVisibility(
				 View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
	                		| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
	                		| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
	                		| View.SYSTEM_UI_FLAG_FULLSCREEN
	                		| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
	                		| SYSTEM_UI_FLAG_LAYOUT_ALWAYS_HIDE_SYSTEMBAR );
//		getWindow().getDecorView().setSystemUiVisibility(
//			View.SYSTEM_UI_FLAG_LAYOUT_STABLE
//                		| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
//                		| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
//                		| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
//                		| View.SYSTEM_UI_FLAG_FULLSCREEN
//                		| View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
//                		| SYSTEM_UI_FLAG_LAYOUT_ALWAYS_HIDE_SYSTEMBAR );
	}

	

}
