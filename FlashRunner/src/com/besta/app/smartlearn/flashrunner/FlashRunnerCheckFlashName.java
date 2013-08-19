package com.besta.app.smartlearn.flashrunner;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.WindowManager;

import com.besta.util.titlebar.BestaTitleBar;

public class FlashRunnerCheckFlashName extends Activity {

	@Override
	public void onAttachedToWindow() {
		this.getWindow().setType(
				WindowManager.LayoutParams.FIRST_APPLICATION_WINDOW);
		super.onAttachedToWindow();
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		Intent brdcst = new Intent("finish service");
		switch (keyCode) {
		case KeyEvent.KEYCODE_POWER:
			sendBroadcast(brdcst);
			if (mReceiver != null) {
				unregisterReceiver(mReceiver);
				finish();
			}
			break;
		// return true;
		case KeyEvent.KEYCODE_HOME:
			// send broad cast for finish()
			sendBroadcast(brdcst);
			unregisterReceiver(mReceiver);
			Intent startMain = new Intent(Intent.ACTION_MAIN);
			startMain.addCategory(Intent.CATEGORY_HOME);
			startMain.setFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
			startActivity(startMain);
			System.exit(0);
			break;
		case KeyEvent.KEYCODE_BACK:
			sendBroadcast(brdcst);
			if (mReceiver != null) {
				// finish();
				unregisterReceiver(mReceiver);
				finish();
			}
			// break;
			return false;
			// System.exit(0);
			// finish();
			// break;
			// return true;
		}
		return super.onKeyDown(keyCode, event);
	}

	final static String allFlashNameOutPutPath = "/sdcard";
	final static String allFlashNameOutPutFileName = "allFlashNameOutPut.txt";

	final static String conTextString = "com.besta.app.smartlearn.";
	final static String conTextString2 = "flashrunner";
	final static String createFilePath = "/data/data/com.besta.app.smartlearn.flashrunner/files";
	// final static String createFilePath = "/sdcard";
	final static String createHashMapName = "javaHashMap.bin";
	final static String createAppPackageName = "allPackageName.bin";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.checkflashname);

		CharSequence titleBar = (CharSequence) getText(R.string.app_name);
		boolean isBestaLibExist = checkTargetClassExist(this,
				"com.besta.util.titlebar",
				"com.besta.util.titlebar.BestaTitleBar");
		if (isBestaLibExist) {
			BestaTitleBar.set(this, this.getPackageName(), titleBar.toString(),
					null);
		}

		// new a Intent-filter, for add a Receiver with service's broad cast
		IntentFilter filter = new IntentFilter();
		filter.addAction("Broadcast one!");
		registerReceiver(mReceiver, filter);
	}

	/**
	 * check library has exist
	 * 
	 * @param className
	 *            class name
	 * @return true: exist | false: not
	 */
	public static boolean checkTargetClassExist(Context context,
			String pkgName, String className) {
		boolean bRet = false;
		try {
			Class.forName(className);
			bRet = true;
		} catch (ClassNotFoundException e) {

		}
		return bRet;
	}

	protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals("Broadcast one!")) {
				Log.d("unregisterReceiver ==", "true");
				unregisterReceiver(mReceiver);
				finish();
			}
		}
	};
}
