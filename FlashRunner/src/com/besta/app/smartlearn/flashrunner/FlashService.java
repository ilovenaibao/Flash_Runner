package com.besta.app.smartlearn.flashrunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;

import com.besta.app.syncourses.main.GetfileMethod;

import android.app.ActivityManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

public class FlashService extends Service {

	Context mContext = this;
	boolean DebugFlag = false;
	String TAG = "FlashService";

	/*-------------------------------------------------------------------
	 * Name :			CheckName
	 * Author :			Taylor Gu
	 * Date:				2011-10-14
	 * Description:		This is a class that myself define for check
	 * 					flash name about :
	 * 					"bxc:05|m102030.swf /frame 3099 4387"
	 * 					String->flashName: only flash name and not include
	 * 									   .bfh
	 * 					int->start: this flash begin point
	 * 					int->end: this flash end point
	--------------------------------------------------------------------*/
	class CheckName {
		String flashName;
		int start;
		int end;

		public CheckName() {
			flashName = "";
			start = 0;
			end = 0;
		}
	};

	static final int MSG_REGISTER_CLIENT = 1;
	static final int MSG_UNREGISTER_CLIENT = 2;
	static final int MSG_GET_ZIPPEDFLASHPATH = 3;
	static final int MSG_CANCEL = 10;
	static final String KEY_ZIPPEDFLASHPATH = "zippedflashpath";

	static final int MSG_ACTION_PLAYFLASH = 4;
	static final String KEY_APP_PKG_NAME = "KEY_APP_PKG_NAME";
	static final String KEY_FLASHFILENAME = "flashfilename";
	static final String KEY_BEGINFRAME = "begin";
	static final String KEY_ENDFRAME = "end";
	static final String KEY_TITLEBARSTRING = "titlebarstring";

	static final int MSG_PLAYFLASH_RESULT = 5;
	static final int MSG_PLAYOTHER_RESULT = 8;
	static final String KEY_PLAYFLASH_RESULT = "playflashresult";
	static final String KEY_PLAYFLASH_SUCCESS = "success";
	static final String KEY_PLAYFLASH_FAIL = "fail";
	static int MSG_PROGRESS = 1;

	static final int MSG_ACTION_CHECKFLASHNAME = 6;
	static final int MSG_ACTION_OTHER_APP_CALLED = 7;
	static final int MSG_ACTION_OTHER_APP_DISCONNECT = 9;

	static final String KEY_CALLER_PASSWORD = "callerpassword";

	String titleBarName = "";
	String callMePkgName = null;
	String flashfilename = null;
	String callerpassword = null;
	Context nowAppContext = this;
	int appPackageCount = 0;
	int nowAppPackage = 0;
	boolean flag = false;
	boolean checkPlayFlashEnd = false;
	Context nowContext = this;
	int begin = 0;
	int end = 0;
	int whatToDo = 0;
	ArrayList<String> allPackage = new ArrayList<String>();
	ArrayList<String> flashListPath = new ArrayList<String>();
	static String flashListPathName = "";
	ArrayList<String> flashNameCollect = new ArrayList<String>();
	int bindRef = 0;

	ArrayList<Messenger> mClients = new ArrayList<Messenger>();

	IntentFilter filter = new IntentFilter();

	class IncomingHandler extends Handler {
		Messenger tmpMessenger = null;

		public void handleMessage(Message msg) {
			Bundle bundle = null;
			if (DebugFlag) {
				Log.i("FlashRunner----> msg = ", "" + msg.what);
			}
			switch (msg.what) {
			case MSG_REGISTER_CLIENT:
				mClients.add(msg.replyTo);
				break;
			case MSG_UNREGISTER_CLIENT:
				mClients.remove(msg.replyTo);
				break;
			case MSG_ACTION_PLAYFLASH:
				bundle = msg.getData();
				if (null == mySearchApp) {
					mySearchApp = new RunService(bundle);
					mySearchApp.start();
				} else {
					if (!startSearchFlag) {
						mySearchApp.start();
					}
				}

				break;
			case MSG_ACTION_CHECKFLASHNAME:
				Intent activity = new Intent(FlashService.this,
						FlashRunnerCheckFlashName.class);
				activity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
				startActivity(activity);
				break;
			default:
				super.handleMessage(msg);
			}
		}
	}

	RunService mySearchApp = null;
	boolean startSearchFlag = false;

	private Handler mRunServiceHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			if (DebugFlag) {
				Log.d(">>>>>mRunServiceHandler", "Start handleMessage"
						+ msg.what);
			}
			switch (msg.what) {
			case -1:
				// 找不到data,　創建不了hasmap
				CharSequence outText = (CharSequence) getText(R.string.unFindFlash);
				try {
					Toast.makeText(FlashService.this, outText,
							Toast.LENGTH_SHORT).show();
				} catch (Exception e) {
					e.printStackTrace();
				}
				break;
			case 1: // 開始搜索
				startSearchFlag = true;
				break;
			case 2: // 搜索結束
				// checkPlayFlashEnd = false;
				startSearchFlag = false;
				break;
			}
			super.handleMessage(msg);
		}
	};

	public static boolean isServiceStarted(Context context, String PackageName) {
		boolean isStarted = false;

		try {
			int intGetTastCounter = 1000;

			ActivityManager mActivityManager = (ActivityManager) context
					.getSystemService(Context.ACTIVITY_SERVICE);

			List<ActivityManager.RunningServiceInfo> mRunningService = mActivityManager
					.getRunningServices(intGetTastCounter);

			for (ActivityManager.RunningServiceInfo amService : mRunningService) {
				if (0 == amService.service.getPackageName().compareTo(
						PackageName)) {
					isStarted = true;
					break;
				}
			}
		} catch (SecurityException e) {
			e.printStackTrace();
		}

		return isStarted;
	}

	private class RunService extends Thread {
		Bundle bundle = null;

		public RunService(Bundle bundle) {
			this.bundle = bundle;
			checkPlayFlashEnd = false;
		}

		public void run() {
			Log.e(TAG, "start FlashRunner!");
			if (null != mRunServiceHandler) {
				mRunServiceHandler.sendEmptyMessage(1);
			}

			if (DebugFlag) {
				Log.i("FlashFunner------->start flag--->", ""
						+ checkPlayFlashEnd);
			}
			if (checkPlayFlashEnd) {
				return;
			} else {
			}
			// get data from dic link
			filter.addAction("finish service");
			// registerReceiver(mReceiver, filter);
			if (bundle != null) {
				String password = bundle.getString(KEY_CALLER_PASSWORD);
				if (password != null) {
					callerpassword = password;
				}
				callMePkgName = bundle.getString(KEY_APP_PKG_NAME);
				flashfilename = bundle.getString(KEY_FLASHFILENAME);
				if (DebugFlag) {
					Log.d(FlashService.class.getSimpleName(), "inputname="
							+ flashfilename);
					Log.d("input==", flashfilename);
				}
				begin = bundle.getInt(KEY_BEGINFRAME);
				end = bundle.getInt(KEY_ENDFRAME);
				titleBarName = bundle.getString(KEY_TITLEBARSTRING);
				int length = flashfilename.length();
				// check flashFileName format
				if (length > 7
						&& (flashfilename.subSequence(0, 7).equals("bxc:05|"))) {
					flashfilename = flashfilename.substring(7);
					CheckName checkFlashName = new CheckName();
					checkFlashName = analyseFlashName(flashfilename);
					flashfilename = checkFlashName.flashName.toUpperCase();
					begin = checkFlashName.start;
					end = checkFlashName.end;
				}

				if (null == callMePkgName || callMePkgName.equals("")) {
					// check input falshName first word is '0' or not and
					// convert normal name
					flashfilename = convertFlashNameToReal(flashfilename);
					String chosAppName = "";
					chosAppName = selectCreateHashMapOrSearch(mContext,
							flashfilename);
					// chosAppName = "not Found !";
					if (chosAppName.equals("not Found !")) {
						File hashMapFile = new File(
								FlashRunnerCheckFlashName.createFilePath,
								FlashRunnerCheckFlashName.createHashMapName);
						File packageNameFile = new File(
								FlashRunnerCheckFlashName.createFilePath,
								FlashRunnerCheckFlashName.createAppPackageName);
						if (hashMapFile.exists()) {
							hashMapFile.delete();
						}
						if (packageNameFile.exists()) {
							packageNameFile.delete();
						}
						makeHashMapInfo();
						// return;
					} else if (chosAppName.equals("null")) {
						makeHashMapInfo();
					} else {
						whatToDo = MSG_ACTION_PLAYFLASH;
						virClass.doBindService(chosAppName);
					}
				} else {
					// add other app called
				}
			}

			// Debug.stopMethodTracing();
			// try {
			// Thread.sleep(1000);
			// } catch (InterruptedException e) {
			// e.printStackTrace();
			// }

			// this.mHandler.sendEmptyMessage(0);
			// Looper.loop();
		}

		Messenger mService = null;
		boolean mIsBound = false;

		class IncomingHandler2 extends Handler {
			public void handleMessage(Message msg) {
				Bundle bundle = null;
				ArrayList<String> multiPathStr = null;
				String strPath = null;
				switch (msg.what) {
				case MSG_GET_ZIPPEDFLASHPATH:
					bundle = msg.getData();
					if (bundle != null) {
						String password = bundle.getString(KEY_CALLER_PASSWORD);
						if (password != null
								&& password.equalsIgnoreCase(mMessenger2
										.toString())) {
							multiPathStr = bundle
									.getStringArrayList(KEY_ZIPPEDFLASHPATH);
							if (multiPathStr == null) {
								strPath = bundle.getString(KEY_ZIPPEDFLASHPATH);
								if (strPath != null) {
									multiPathStr = new ArrayList<String>();
									multiPathStr.add(strPath);
								}
							}
							bindNext(multiPathStr);
							flag = true;
						}
					}
					break;
				case MSG_PLAYFLASH_RESULT:
					// doUnbindService();
					Log.e(TAG, "start FlashRunner!-> PlayFlash");
					bundle = msg.getData();
					if (DebugFlag) {
						Log.d("======== msg.getdata = ", "" + bundle);
					}
					if (bundle != null) {
						String password = bundle.getString(KEY_CALLER_PASSWORD);
						if (password != null
								&& password.equalsIgnoreCase(mMessenger2
										.toString())) {
							String str = bundle.getString(KEY_PLAYFLASH_RESULT);
							Bundle bundle2 = new Bundle();
							bundle2.putString(KEY_PLAYFLASH_RESULT, str);
							Message msgTo = Message.obtain(null,
									MSG_PLAYFLASH_RESULT);
							msgTo.setData(bundle2);
							checkPlayFlashEnd = false;
							// for (int i = mClients.size() - 1; i >= 0; i--) {
							// try {
							// mClients.get(i).send(msgTo);
							// } catch (RemoteException e) {
							// mClients.remove(i);
							// }
							// }
						}
					}
					break;
				case MSG_PLAYOTHER_RESULT:
					Message sendToSuperService = new Message();
					sendToSuperService.what = MSG_ACTION_OTHER_APP_DISCONNECT;

					Message msgTo = Message.obtain(null, MSG_PLAYFLASH_RESULT);
					for (int i = mClients.size() - 1; i >= 0; i--) {
						try {
							mClients.get(i).send(msgTo);
						} catch (RemoteException e) {
							mClients.remove(i);
						}
					}
					break;
				default:
					super.handleMessage(msg);
				}
			}
		}

		/*-------------------------------------------------------------------------------
		 * Name :			doUnbindService
		 * Author :			Taylor Gu
		 * Date:			2011-10-19
		 * Description:		unbindService for this app
		 * Input:    		none 
		 * output:			none
		--------------------------------------------------------------------------------*/
		void doUnbindService() {
			Log.i("doUnbindService()------->", "mIsBound = " + mIsBound);
			if (mIsBound) {
				// if (mService != null) {
				// try {
				// Message msg = Message.obtain(null,
				// MSG_UNREGISTER_CLIENT);
				// msg.replyTo = mMessenger2;
				// mService.send(msg);
				// } catch (RemoteException e) {
				// }
				// }
				if (null != mConnection2) {
					try {
						mContext.unbindService(mConnection2);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
				mIsBound = false;
				bindRef--;
				Log.d(VirtualActivity.class.getSimpleName(), "UnBind:bindRef="
						+ bindRef);
			} else {
			}
		}

		public String listFilePathConvert(String listPath) {
			// Log.d("listFilePathConvert() =======", listPath);
			String retConvert = "";
			int i = 0;
			byte[] oneListPath = null;
			oneListPath = listPath.getBytes();
			i = listPath.length() - 1;
			for (; i >= 0; i--) {
				if (oneListPath[i] == '/') {
					break;
				}
			}
			retConvert = listPath.substring(i + 1);

			return retConvert;
		}

		/*-------------------------------------------------------------------------------
		 * Name :			CreateNewFlashNameFile
		 * Author :			Taylor Gu
		 * Date:			2011-10-19
		 * Description:		create new flash name hashMap
		 * Input:    		none 
		 * output:			none
		--------------------------------------------------------------------------------*/
		public void CreateNewFlashNameFile() {
			Log.e(TAG, "start FlashRunner!-> create hasmap");
			appPackageCount = allPackage.size();
			if (appPackageCount == 0) {
				File newFile = new File(
						FlashRunnerCheckFlashName.createFilePath + "/"
								+ FlashRunnerCheckFlashName.createHashMapName);
				if (newFile.exists()) {
					newFile.delete();
				}
				return;
			}
			int allFlashCount = flashNameCollect.size();
			BA001_FlashRunnerActivity.CreateFlashNameHashMap(flashNameCollect,
					allFlashCount, allPackage);

			// makeAllFlashNameOutPut(flashNameCollect); // for debug only.
		}

		/*-------------------------------------------------------------------------------
		 * Name :			doBindService
		 * Author :			Taylor Gu
		 * Date:			2011-10-19
		 * Description:		bind this packageName's FlashOwnerService.class for get 
		 * 					flash name list of path. 
		 * Input:    		String->packageName: for bind's package 
		 * output:			none
		--------------------------------------------------------------------------------*/
		void doBindService(String packageName) {
			if (mIsBound) {
				doUnbindService();
			}
			if (!mIsBound) {
				if (whatToDo == MSG_ACTION_CHECKFLASHNAME) {

				} else {
					boolean bindServiceFlag = false;
					if (packageName != null && !packageName.isEmpty()) {
						Intent service = new Intent();
						service.setClassName(packageName, packageName
								+ ".FlashOwnerService");
						bindServiceFlag = mContext.bindService(service,
								mConnection2, Context.BIND_AUTO_CREATE);
						Log.i("FlashRunner--->bindServiceFlag",
								"--->bindServiceFlag: " + bindServiceFlag);
					}

					if (!bindServiceFlag) {
						nowAppPackage += 1;
						if (nowAppPackage <= appPackageCount - 1) {
							doBindService(allPackage.get(nowAppPackage));
						} else {
							CreateNewFlashNameFile();
							whatToDo = MSG_ACTION_PLAYFLASH;
							String appName = selectCreateHashMapOrSearch(
									mContext, flashfilename);
							if (appName.equals("not Found !")
									|| appName.equals("null")) {
								Bundle bundle = new Bundle();
								bundle.putString(KEY_PLAYFLASH_RESULT,
										KEY_PLAYFLASH_FAIL);
								Message msgTo = Message.obtain(null,
										MSG_PLAYFLASH_RESULT);
								msgTo.setData(bundle);
								for (int i = mClients.size() - 1; i >= 0; i--) {
									try {
										mClients.get(i).send(msgTo);
									} catch (RemoteException e) {
										mClients.remove(i);
									}
								}
								CharSequence outText = (CharSequence) getText(R.string.unFindFlash);
								Toast.makeText(FlashService.this, outText,
										Toast.LENGTH_SHORT).show();
								checkPlayFlashEnd = false;
								Log.d(VirtualActivity.class.getSimpleName(),
										"End(1):bindRef=" + bindRef);
							} else {
								doBindService(appName);
							}

							Intent brdcst = new Intent("Broadcast one!");
							// send broad cast for finish()
							sendBroadcast(brdcst);

						}
					} else {
						mIsBound = true;
						bindRef++;
						Log.d(VirtualActivity.class.getSimpleName(),
								"dobindservice->Bind:bindRef=" + bindRef + "["
										+ packageName + "]");
					}
				}
			}
		}

		/*-------------------------------------------------------------------------------
		 * Name :			bindNext
		 * Author :			Taylor Gu
		 * Date:			2011-10-19
		 * Description:		If pre bindservice success and dobind next service, else 
		 * 					create new hashMap
		 * Input:    		String->path: flash name list of path 
		 * output:			none
		--------------------------------------------------------------------------------*/
		public void bindNext(ArrayList<String> pathArray) {
			if (pathArray == null) {
				return;
			}
			if (mIsBound) {
				doUnbindService();
			}

			int count = pathArray.size();
			String path;
			for (int i = 0; i < count; i++) {
				path = pathArray.get(i);
				if (path == null) {
					continue;
				}
				Log.d(VirtualActivity.class.getSimpleName(), "pathArray["
						+ (i + 1) + "/" + count + "]=" + path);
				if (path.equals("") || flashListPath.contains(path)) {
					path = "0";
					Log.d("SystemClock", "NULL===" + SystemClock.uptimeMillis());
				}
				flashListPath.add(path);
				BA001_FlashRunnerActivity.flashListPath = "|"
						+ listFilePathConvert(path);
				// Log.d("flashListPath =======",
				// FlashRunnerActivity.flashListPath);
				flashNameCollect.addAll(BA001_FlashRunnerActivity
						.OpenOneAppFlashList(path, nowAppPackage));
			}

			nowAppPackage += 1;
			if (nowAppPackage <= appPackageCount - 1) {
				doBindService(allPackage.get(nowAppPackage));
			} else {
				CreateNewFlashNameFile();
				whatToDo = MSG_ACTION_PLAYFLASH;
				String appName = "";
				// added by Leo Wei for debug only.
				if (flashfilename.equalsIgnoreCase("********")) {
					int j = 0;
					for (String flashName : flashNameCollect) {
						if (-1 != (j = flashName.indexOf('|'))) {
							flashName = flashName.substring(2, j);
						} else {
							flashName = flashName.substring(2);
						}

						appName = selectCreateHashMapOrSearch(mContext,
								flashName);
						if (appName.equals("not Found !")
								|| appName.equals("null")) {
							Log.d("auto2", flashName + "appName=" + appName);
						}
					}
				} else {
					// added end.

					// find playflash's app Package name
					appName = selectCreateHashMapOrSearch(mContext,
							flashfilename);

					Intent brdcst = new Intent("Broadcast one!");
					// send broad cast for finish()
					sendBroadcast(brdcst);

					if (appName.equals("not Found !") || appName.equals("null")) {
						// send playflash result for fail
						Bundle bundle = new Bundle();
						bundle.putString(KEY_PLAYFLASH_RESULT,
								KEY_PLAYFLASH_FAIL);
						Message msgTo = Message.obtain(null,
								MSG_PLAYFLASH_RESULT);
						msgTo.setData(bundle);
						for (int i = mClients.size() - 1; i >= 0; i--) {
							try {
								mClients.get(i).send(msgTo);
							} catch (RemoteException e) {
								mClients.remove(i);
							}
						}
						CharSequence outText = (CharSequence) getText(R.string.unFindFlash);
						Toast.makeText(FlashService.this, outText,
								Toast.LENGTH_SHORT).show();

						checkPlayFlashEnd = false;
						Log.d(VirtualActivity.class.getSimpleName(),
								"End(2):bindRef=" + bindRef);
					} else {
						// playflash success
						doBindService(appName);
					}
				}

			}
		}

		final Messenger mMessenger2 = new Messenger(new IncomingHandler2());

		private ServiceConnection mConnection2 = new ServiceConnection() {
			public void onServiceConnected(ComponentName className,
					IBinder service) {
				mIsBound = true;
				mService = new Messenger(service);
				try {
					Message msg = Message.obtain(null, MSG_REGISTER_CLIENT);
					msg.replyTo = mMessenger2;
					mService.send(msg);
					switch (whatToDo) {
					case MSG_GET_ZIPPEDFLASHPATH:
						doCallServiceForResult();
						break;
					case MSG_ACTION_PLAYFLASH:
						doCallServiceForResult2();
						break;
					case MSG_ACTION_CHECKFLASHNAME:
						doCallServiceForResult3();
						break;
					case MSG_ACTION_OTHER_APP_CALLED:
						// doCallServiceForOtherAppService(callMePkgName);
						break;
					}
				} catch (RemoteException e) {
				}
			}

			public void onServiceDisconnected(ComponentName className) {
				mService = null;
				mIsBound = false;
			}
		};

		void doCallServiceForResult() {
			if (!mIsBound) {
				return;
			}
			Message msg = Message.obtain(null, MSG_GET_ZIPPEDFLASHPATH);
			Bundle bundle = new Bundle();
			bundle.putString(KEY_CALLER_PASSWORD, mMessenger2.toString());
			msg.setData(bundle);
			try {
				mService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		void doCallServiceForResult2() {
			// if (!mIsBound) {
			// return;
			// } else {
			// doUnbindService();
			// }
			Message msg = null;
			Log.d("PlayFlash_Flag ===", "" + playFlash_Flag);
			if (playFlash_Flag) {
				// if (mReceiver != null) {
				// unregisterReceiver(mReceiver);
				// }
				Log.d("MSG==========", "PlayFlash");
				msg = Message.obtain(null, MSG_ACTION_PLAYFLASH);
				Bundle bundle = new Bundle();
				bundle.putString(KEY_CALLER_PASSWORD, mMessenger2.toString());
				bundle.putString(KEY_FLASHFILENAME, flashfilename);
				bundle.putInt(KEY_BEGINFRAME, begin);
				bundle.putInt(KEY_ENDFRAME, end);
				bundle.putString(KEY_ZIPPEDFLASHPATH,
						flashListPathName.substring(1));
				Log.d(VirtualActivity.class.getSimpleName(), "["
						+ flashfilename + "][" + begin + "][" + end + "]["
						+ flashListPathName.substring(1) + "]");
				msg.setData(bundle);
				try {
					mService.send(msg);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				// doUnbindService();
				// Message msg = Message.obtain(null, MSG_ACTION_PLAYFLASH);
			}
		}

		void doCallServiceForResult3() {
			if (!mIsBound) {
				return;
			}
			Message msg = Message.obtain(null, MSG_ACTION_CHECKFLASHNAME);
			Bundle bundle = new Bundle();
			bundle.putString(KEY_CALLER_PASSWORD, mMessenger2.toString());
			bundle.putString(KEY_FLASHFILENAME, flashfilename);
			bundle.putInt(KEY_BEGINFRAME, begin);
			bundle.putInt(KEY_ENDFRAME, end);
			msg.setData(bundle);
			try {
				mService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		void doCallServiceForOtherAppService(String pkg_name) {
			Intent service = new Intent();
			service.setClassName(pkg_name, pkg_name + ".FlashOwnerService");
			boolean bindServiceFlag = mContext.bindService(service,
					mConnection2, Context.BIND_AUTO_CREATE);

			Log.i("FlashRunner--->bindServiceFlag", "--->bindServiceFlag: "
					+ bindServiceFlag);
		}
	}

	boolean playFlash_Flag = true;
	protected BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals("finish service")) {
				playFlash_Flag = false;
				unregisterReceiver(mReceiver);
			}
		}
	};

	/*-------------------------------------------------------------------------------
	 * Name :			startMyActivity
	 * Author :			Taylor Gu
	 * Date:				2011-10-14
	 * Description:		start myself Activity
	 * Input:    		titleBar: receive one app Name for set my App Name
	 * output:			none
	--------------------------------------------------------------------------------*/
	public void startMyActivity(String titleBar) {
		Intent prgress = new Intent(FlashService.this,
				FlashRunnerCheckFlashName.class);
		prgress.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		// prgress.putExtra(KEY_TITLEBARSTRING, titleBarName);
		try {
			startActivity(prgress);
		} catch (Exception e) {

		}
	}

	/*-------------------------------------------------------------------------------
	 * Name :			analyseFlashName
	 * Author :			Taylor Gu
	 * Date:				2011-10-14
	 * Description:		Check receive the flash name, if it length < max
	 * 					flash name length, add "." in pre of this flash
	 * 					name. 
	 * Input:    		String->flashName: recieve a original flash name 
	 * output:			class->CheckName: a class for store flash name 
	 * 					information
	--------------------------------------------------------------------------------*/
	public CheckName analyseFlashName(String flashName) {
		CheckName retCheckName = new CheckName();
		int nameLength = flashName.length();
		int beginStart = 0;
		int beginOver = 0;
		int endStart = 0;
		int endOver = flashName.length();
		int spaceCount = 0;
		int count = 0;

		for (count = 0; count < nameLength; count++) {
			if (flashName.substring(count, count + 1).equals(".")) {
				retCheckName.flashName = flashName.substring(0, count);
			} else if (flashName.substring(count, count + 1).equals("/")) {
				beginStart = count + 7;
			} else if (flashName.substring(count, count + 1).equals(" ")) {
				spaceCount += 1;
				if (3 == spaceCount) {
					beginOver = count;
					endStart = count + 1;
				}
			}
		}
		String start = flashName.substring(beginStart, beginOver);
		String Over = flashName.substring(endStart, endOver);

		retCheckName.start = convertStringToInt(start);
		retCheckName.end = convertStringToInt(Over);

		return retCheckName;
	}

	/*-------------------------------------------------------------------------------
	 * Name :			convertStringToInt
	 * Author :			Taylor Gu
	 * Date:				2011-10-14
	 * Description:		Convert a string that include flash begin and end
	 * 					information to int style for store
	 * Input:    		String->tmpStr: recieve a original flash information 
	 * output:			int->ret: divide information to begin and end
	--------------------------------------------------------------------------------*/
	public int convertStringToInt(String tmpStr) {
		int ret = 0;
		int tmpStrLength = tmpStr.length();
		byte[] tmpStrByte = new byte[tmpStrLength];
		tmpStrByte = tmpStr.getBytes();
		int byteCount = 0;
		int dacadeCount = 1;

		for (byteCount = tmpStrLength - 1; byteCount >= 0; byteCount--) {
			tmpStrByte[byteCount] = (byte) (tmpStrByte[byteCount] - 48);
			ret = ret + tmpStrByte[byteCount] * dacadeCount;
			dacadeCount *= 10;
		}

		return ret;
	}

	public String convertFlashNameToReal(String flashName) {
		String retResultName = "";
		if (flashName.substring(0, 1).equals("0")) {
			retResultName = "." + flashName.substring(1);
		} else {
			retResultName = flashName;
		}

		return retResultName;
	}

	/*-------------------------------------------------------------------------------
	 * Name :			selectCreateHashMapOrSearch
	 * Author :			Taylor Gu
	 * Date:			2011-10-19
	 * Description:		Select create hashMap or search in hashMap. If hashMap exist
	 * 					then search flashName in this map, else create new hashMap 
	 * Input:    		String->flashName: recieve a flashName for check 
	 * output:			String->CheckName: "null"->hashMap not exist
	 * 									   "not Found !"->flashName not Found
	 * 									   "others"->output app PackageName for 
	 * 									   playflash
	--------------------------------------------------------------------------------*/
	public static String selectCreateHashMapOrSearch(Context context,
			String flashName) {
		boolean openHashMapFile = false;
		openHashMapFile = BA001_FlashRunnerActivity.OpenFlashNameHashMap();
		String chosAppName = "null";
		if (openHashMapFile) {
			chosAppName = selectHasMap(flashName);
		} else {
			// "null"->hashMap not exist
			chosAppName = "null";
			// copy hasMap file from asset dir
			String fileAndPath = "besta/data/" + context.getPackageName() + "/"
					+ FlashRunnerCheckFlashName.createHashMapName;
			fileAndPath = GetfileMethod.getfilepath(fileAndPath, context);
			if (null == fileAndPath) {
				// 內建目錄
				fileAndPath = "/besta/data/" + context.getPackageName() + "/"
						+ FlashRunnerCheckFlashName.createHashMapName;
				File tmpFile = new File(fileAndPath);
				if (!tmpFile.exists()) {
					fileAndPath = null;
				}
			}
			// fileAndPath = null;
			if (null != fileAndPath) {
				openHashMapFile = copyAssetDirFileIntoPrivateDir_2(context,
						fileAndPath);
				if (openHashMapFile) {
					chosAppName = selectHasMap(flashName);
				}
			}
		}

		return chosAppName;
	}

	private static String selectHasMap(String flashName) {
		String chosAppName = "";
		// this file exist and find the name directly
		String fileAndPath = FlashRunnerCheckFlashName.createFilePath + "/"
				+ FlashRunnerCheckFlashName.createHashMapName;
		RandomAccessFile fileHashMap = null;
		int maxNameLength = 0;

		try {
			fileHashMap = new RandomAccessFile(fileAndPath, "r");
			try {
				fileHashMap.seek(fileHashMap.length() - 33);
				maxNameLength = fileHashMap.readByte();
			} catch (IOException e) {
				e.printStackTrace();
			}
			// change flashName to UpperCase
			flashName = flashName.toUpperCase();
			chosAppName = BA001_FlashRunnerActivity.checkChosAppName(
					fileHashMap, flashName);
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		}

		return chosAppName;
	}

	private static boolean copyAssetDirFileIntoPrivateDir_2(Context context,
			String res_name) {
		boolean ret = true;
		String fileAndPath = FlashRunnerCheckFlashName.createFilePath;
		File out = new File(fileAndPath);
		out.mkdirs();
		fileAndPath = fileAndPath + "/"
				+ FlashRunnerCheckFlashName.createHashMapName;
		out = new File(fileAndPath);
		out.delete();
		try {
			out.createNewFile();
		} catch (IOException e1) {
			ret = false;
			return ret;
		}
		try {
			RandomAccessFile outFile = new RandomAccessFile(fileAndPath, "rw");
			RandomAccessFile rf = new RandomAccessFile(res_name, "r");
			long file_len = rf.length();
			int oneBufferLen = 1024;
			long file_pointer = rf.getFilePointer();
			while (file_pointer < file_len) {
				long divide_value = file_len - file_pointer;
				byte[] oneBuffer = null;
				if (divide_value > oneBufferLen) {
					oneBuffer = new byte[oneBufferLen];
				} else {
					oneBuffer = new byte[(int) divide_value];
				}
				rf.read(oneBuffer);
				outFile.write(oneBuffer);
				file_pointer = rf.getFilePointer();
			}
			rf.close();
			outFile.close();
		} catch (Exception e) {
			// e.printStackTrace();
			ret = false;
		}

		return ret;
	}

	private static void copyAssetDirFileIntoPrivateDir(Context context,
			String res_name) {
		AssetManager asset = context.getAssets();
		byte[] buf = new byte[1024];
		FileInputStream read = null;
		try {
			read = context.openFileInput(res_name);
			FileOutputStream fos = null;
			fos = context.openFileOutput(res_name, Context.MODE_APPEND);
			while (read.read(buf) != -1) {
				fos.write(buf);
			}
			fos.close();
			read.close();
		} catch (FileNotFoundException e) {
			InputStream fis = null;
			try {
				fis = asset.open(res_name);
				FileOutputStream fos = null;
				fos = context.openFileOutput(res_name, Context.MODE_APPEND);
				while (fis.read(buf) != -1) {
					fos.write(buf);
				}
				fis.close();
				fos.close();
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*-------------------------------------------------------------------------------
	 * Name :			makeHashMapInfo
	 * Author :			Taylor Gu
	 * Date:			2011-10-19
	 * Description:		pre create hashMap for collect information about all flash 
	 * 					name list of path
	 * Input:    		none
	 * output:			none
	--------------------------------------------------------------------------------*/
	public void makeHashMapInfo() {
		checkPlayFlashEnd = true;
		// start myself activity for display searching window
		startMyActivity(titleBarName);
		// receive all package name
		allPackage = ListAllPackages();
		appPackageCount = allPackage.size();
		whatToDo = MSG_GET_ZIPPEDFLASHPATH;
		nowAppPackage = 0;
		String packageName = null;
		// connect one of these packages for get flashNameList's path
		if (appPackageCount > 0) {
			packageName = allPackage.get(nowAppPackage);
		}
		virClass.doBindService(packageName);
	}

	/*-------------------------------------------------------------------------------
	 * Name :			ListAllPackages
	 * Author :			Taylor Gu
	 * Date:			2011-10-19
	 * Description:		Get all packages that had been installed
	 * Input:    		none
	 * output:			ArrayList<String>->store all package name in ArrayList<>
	--------------------------------------------------------------------------------*/
	public ArrayList<String> ListAllPackages() {
		PackageInfo info = null;
		String strTemp = "Name :";
		int infoSize = 0; // Package count
		int sizeCount = 0; // Package count for ( for(...) func )

		PackageManager pkgmanager = mContext.getPackageManager();
		List<PackageInfo> pkginfo = pkgmanager.getInstalledPackages(0);
		infoSize = pkginfo.size();
		ArrayList<String> appPackage = new ArrayList<String>();
		int correspondPackage = 0;
		File newFile = new File(FlashRunnerCheckFlashName.createFilePath);
		if (!newFile.exists()) {
			newFile.mkdirs();
		}

		List<ResolveInfo> serviceInfoList = getPackageManager()
				.queryIntentServices(
						new Intent(
								"com.besta.app.smartlearn.engsupertutor.gzcourse.gztestlisten.nouse"),
						0);

		ArrayList<String> appServiceName = new ArrayList<String>();
		for (sizeCount = 0; sizeCount < serviceInfoList.size(); sizeCount++) {
			ResolveInfo oneInfo = serviceInfoList.get(sizeCount);
			if (null != oneInfo.serviceInfo) {
				String tmpPkgName = oneInfo.serviceInfo.packageName;
				if (null != tmpPkgName) {
					String a = oneInfo.serviceInfo.name;
					if (0 < appServiceName.size()) {
						if (0 > appServiceName.indexOf(a)) {
							appServiceName.add(a);
							appPackage.add(tmpPkgName);
						}
					} else {
						appServiceName.add(a);
						appPackage.add(tmpPkgName);
					}
					// appPackage.add(a);
				}
			}
			Log.d("ListAllPackages", "=" + strTemp);
			correspondPackage += 1;
		}

		return appPackage;
	} // End of Func

	final Messenger mMessenger = new Messenger(new IncomingHandler());

	public IBinder onBind(Intent intent) {
		return mMessenger.getBinder();
	}

	VirtualActivity virClass = new VirtualActivity();

	class VirtualActivity {
		Messenger mService = null;
		boolean mIsBound = false;

		class IncomingHandler2 extends Handler {
			public void handleMessage(Message msg) {
				Bundle bundle = null;
				ArrayList<String> multiPathStr = null;
				String strPath = null;
				switch (msg.what) {
				case MSG_GET_ZIPPEDFLASHPATH:
					bundle = msg.getData();
					if (bundle != null) {
						String password = bundle.getString(KEY_CALLER_PASSWORD);
						if (password != null
								&& password.equalsIgnoreCase(mMessenger2
										.toString())) {
							multiPathStr = bundle
									.getStringArrayList(KEY_ZIPPEDFLASHPATH);
							if (multiPathStr == null) {
								strPath = bundle.getString(KEY_ZIPPEDFLASHPATH);
								if (strPath != null) {
									multiPathStr = new ArrayList<String>();
									multiPathStr.add(strPath);
								}
							}
							bindNext(multiPathStr);
							flag = true;
						}
					}
					break;
				case MSG_PLAYFLASH_RESULT:
					bundle = msg.getData();
					Log.e(TAG, "start FlashRunner!-> PlayFlash");
					Log.d("======== msg.getdata = ", "" + bundle);
					if (bundle != null) {
						String password = bundle.getString(KEY_CALLER_PASSWORD);
						if (password != null
								&& password.equalsIgnoreCase(mMessenger2
										.toString())) {
							String str = bundle.getString(KEY_PLAYFLASH_RESULT);
							Bundle bundle2 = new Bundle();
							bundle2.putString(KEY_PLAYFLASH_RESULT, str);
							Message msgTo = Message.obtain(null,
									MSG_PLAYFLASH_RESULT);
							msgTo.setData(bundle2);
							doUnbindService();
							checkPlayFlashEnd = false;
							for (int i = mClients.size() - 1; i >= 0; i--) {
								try {
									mClients.get(i).send(msgTo);
								} catch (RemoteException e) {
									mClients.remove(i);
								}
							}
							// doUnbindService();
						}
					}
					break;
				case MSG_PLAYOTHER_RESULT:
					Message sendToSuperService = new Message();
					sendToSuperService.what = MSG_ACTION_OTHER_APP_DISCONNECT;

					Message msgTo = Message.obtain(null, MSG_PLAYFLASH_RESULT);
					for (int i = mClients.size() - 1; i >= 0; i--) {
						try {
							mClients.get(i).send(msgTo);
						} catch (RemoteException e) {
							mClients.remove(i);
						}
					}
					break;
				default:
					super.handleMessage(msg);
				}
			}
		}

		// return : false->not exist; true->exist
		public boolean checkFlashPath(String flashPath) {
			boolean retCheckResult = false;
			File flashNameListPath = new File(flashPath);
			retCheckResult = flashNameListPath.exists();

			return retCheckResult;
		}

		final Messenger mMessenger2 = new Messenger(new IncomingHandler2());

		private ServiceConnection mConnection2 = new ServiceConnection() {
			public void onServiceConnected(ComponentName className,
					IBinder service) {
				mIsBound = true;
				mService = new Messenger(service);
				try {
					Message msg = Message.obtain(null, MSG_REGISTER_CLIENT);
					msg.replyTo = mMessenger2;
					mService.send(msg);
					switch (whatToDo) {
					case MSG_GET_ZIPPEDFLASHPATH:
						doCallServiceForResult();
						break;
					case MSG_ACTION_PLAYFLASH:
						// unBindService from lnktutor
						Bundle bundle2 = new Bundle();
						bundle2.putString(KEY_PLAYFLASH_RESULT,
								KEY_PLAYFLASH_SUCCESS);
						Message msgTo = Message.obtain(null, 100);
						msgTo.setData(bundle2);
						if (null != mClients && 0 < mClients.size()) {
							try {
								mClients.get(0).send(msgTo);
							} catch (RemoteException e) {
								mClients.remove(0);
							}
						}
						if (null != mRunServiceHandler) {
							startSearchFlag = false;
							mRunServiceHandler.sendEmptyMessage(2);
						}
						doCallServiceForResult2();
						break;
					case MSG_ACTION_CHECKFLASHNAME:
						doCallServiceForResult3();
						break;
					case MSG_ACTION_OTHER_APP_CALLED:
						// doCallServiceForOtherAppService(callMePkgName);
						break;
					}
				} catch (RemoteException e) {
				}
			}

			public void onServiceDisconnected(ComponentName className) {
				mService = null;
				mIsBound = false;
			}
		};

		/*-------------------------------------------------------------------------------
		 * Name :			doBindService
		 * Author :			Taylor Gu
		 * Date:			2011-10-19
		 * Description:		bind this packageName's FlashOwnerService.class for get 
		 * 					flash name list of path. 
		 * Input:    		String->packageName: for bind's package 
		 * output:			none
		--------------------------------------------------------------------------------*/
		void doBindService(String packageName) {
			if (mIsBound) {
				doUnbindService();
				// mIsBound = false;
			}
			if (!mIsBound) {
				if (whatToDo == MSG_ACTION_CHECKFLASHNAME) {

				} else {
					doUnbindService();
					boolean bindServiceFlag = false;
					if (packageName != null && !packageName.isEmpty()) {
						Log.i("start FlashOwnerService------>", "start");
						Intent service = new Intent();
						String myUsingServicesName = "com.besta.app.new_bxctreelist";
						service.setClassName(packageName, myUsingServicesName
								+ ".FlashOwnerService");
						bindServiceFlag = mContext.bindService(service,
								mConnection2, Context.BIND_AUTO_CREATE);
						// bindServiceFlag = false;
						if (!bindServiceFlag) {
							doUnbindService();
							service.setClassName(packageName, packageName
									+ ".FlashOwnerService");
							bindServiceFlag = mContext.bindService(service,
									mConnection2, Context.BIND_AUTO_CREATE);
						}
						Log.i("FlashRunner--->bindServiceFlag",
								"--->bindServiceFlag: " + bindServiceFlag);
					}

					if (!bindServiceFlag) {
						nowAppPackage += 1;
						if (nowAppPackage <= appPackageCount - 1) {
							doBindService(allPackage.get(nowAppPackage));
						} else {
							CreateNewFlashNameFile();
							whatToDo = MSG_ACTION_PLAYFLASH;
							String appName = selectCreateHashMapOrSearch(
									mContext, flashfilename);
							if (appName.equals("not Found !")
									|| appName.equals("null")) {
								Bundle bundle = new Bundle();
								bundle.putString(KEY_PLAYFLASH_RESULT,
										KEY_PLAYFLASH_FAIL);
								Message msgTo = Message.obtain(null,
										MSG_PLAYFLASH_RESULT);
								msgTo.setData(bundle);
								for (int i = mClients.size() - 1; i >= 0; i--) {
									try {
										mClients.get(i).send(msgTo);
									} catch (RemoteException e) {
										mClients.remove(i);
									}
								}
								CharSequence outText = (CharSequence) getText(R.string.unFindFlash);
								try {
									Toast.makeText(FlashService.this, outText,
											Toast.LENGTH_SHORT).show();
								} catch (Exception e) {
									mRunServiceHandler.sendEmptyMessage(-1);
									// e.printStackTrace();
								}
								checkPlayFlashEnd = false;
								Log.d(VirtualActivity.class.getSimpleName(),
										"End(1):bindRef=" + bindRef);
							} else {
								doBindService(appName);
							}

							Intent brdcst = new Intent("Broadcast one!");
							// send broad cast for finish()
							sendBroadcast(brdcst);

						}
					} else {
						mIsBound = true;
						bindRef++;
						Log.d(VirtualActivity.class.getSimpleName(),
								"dobindservice->Bind:bindRef=" + bindRef + "["
										+ packageName + "]");
					}
				}
			}
		}

		/*-------------------------------------------------------------------------------
		 * Name :			bindNext
		 * Author :			Taylor Gu
		 * Date:			2011-10-19
		 * Description:		If pre bindservice success and dobind next service, else 
		 * 					create new hashMap
		 * Input:    		String->path: flash name list of path 
		 * output:			none
		--------------------------------------------------------------------------------*/
		public void bindNext(ArrayList<String> pathArray) {
			if (pathArray == null) {
				return;
			}
			if (mIsBound) {
				doUnbindService();
			}

			int count = pathArray.size();
			String path;
			for (int i = 0; i < count; i++) {
				path = pathArray.get(i);
				if (path == null) {
					continue;
				}
				Log.d(VirtualActivity.class.getSimpleName(), "pathArray["
						+ (i + 1) + "/" + count + "]=" + path);
				if (path.equals("") || flashListPath.contains(path)) {
					path = "0";
					Log.d("SystemClock", "NULL===" + SystemClock.uptimeMillis());
				}
				flashListPath.add(path);
				BA001_FlashRunnerActivity.flashListPath = "|"
						+ listFilePathConvert(path);
				// Log.d("flashListPath =======",
				// FlashRunnerActivity.flashListPath);
				flashNameCollect.addAll(BA001_FlashRunnerActivity
						.OpenOneAppFlashList(path, nowAppPackage));
			}

			nowAppPackage += 1;
			if (nowAppPackage <= appPackageCount - 1) {
				doBindService(allPackage.get(nowAppPackage));
			} else {
				CreateNewFlashNameFile();
				whatToDo = MSG_ACTION_PLAYFLASH;
				String appName = "";
				// added by Leo Wei for debug only.
				if (flashfilename.equalsIgnoreCase("********")) {
					int j = 0;
					for (String flashName : flashNameCollect) {
						if (-1 != (j = flashName.indexOf('|'))) {
							flashName = flashName.substring(2, j);
						} else {
							flashName = flashName.substring(2);
						}

						appName = selectCreateHashMapOrSearch(mContext,
								flashName);
						if (appName.equals("not Found !")
								|| appName.equals("null")) {
							Log.d("auto2", flashName + "appName=" + appName);
						}
					}
				} else {
					// added end.

					// find playflash's app Package name
					appName = selectCreateHashMapOrSearch(mContext,
							flashfilename);

					if (appName.equals("not Found !") || appName.equals("null")) {
						// send playflash result for fail
						Bundle bundle = new Bundle();
						bundle.putString(KEY_PLAYFLASH_RESULT,
								KEY_PLAYFLASH_FAIL);
						Message msgTo = Message.obtain(null,
								MSG_PLAYFLASH_RESULT);
						msgTo.setData(bundle);
						for (int i = mClients.size() - 1; i >= 0; i--) {
							try {
								mClients.get(i).send(msgTo);
							} catch (RemoteException e) {
								mClients.remove(i);
							}
						}
						CharSequence outText = (CharSequence) getText(R.string.unFindFlash);
						Toast.makeText(FlashService.this, outText,
								Toast.LENGTH_SHORT).show();

						checkPlayFlashEnd = false;
						Log.d(VirtualActivity.class.getSimpleName(),
								"End(2):bindRef=" + bindRef);
					} else {
						// playflash success
						doBindService(appName);
					}

					Intent brdcst = new Intent("Broadcast one!");
					// send broad cast for finish()
					sendBroadcast(brdcst);
				}

			}
		}

		public String listFilePathConvert(String listPath) {
			// Log.d("listFilePathConvert() =======", listPath);
			String retConvert = "";
			int i = 0;
			byte[] oneListPath = null;
			oneListPath = listPath.getBytes();
			i = listPath.length() - 1;
			for (; i >= 0; i--) {
				if (oneListPath[i] == '/') {
					break;
				}
			}
			retConvert = listPath.substring(i + 1);

			return retConvert;
		}

		/*-------------------------------------------------------------------------------
		 * Name :			CreateNewFlashNameFile
		 * Author :			Taylor Gu
		 * Date:			2011-10-19
		 * Description:		create new flash name hashMap
		 * Input:    		none 
		 * output:			none
		--------------------------------------------------------------------------------*/
		public void CreateNewFlashNameFile() {
			appPackageCount = allPackage.size();
			if (appPackageCount == 0) {
				File newFile = new File(
						FlashRunnerCheckFlashName.createFilePath + "/"
								+ FlashRunnerCheckFlashName.createHashMapName);
				if (newFile.exists()) {
					newFile.delete();
				}
				return;
			}
			int allFlashCount = flashNameCollect.size();
			BA001_FlashRunnerActivity.CreateFlashNameHashMap(flashNameCollect,
					allFlashCount, allPackage);

			// makeAllFlashNameOutPut(flashNameCollect); // for debug only.
		}

		/*-------------------------------------------------------------------------------
		 * Name :			makeAllFlashNameOutPut
		 * Author :			Taylor Gu
		 * Date:			2011-10-19
		 * Description:		output allFlashName in /sdcard/*.txt for test program
		 * Input:    		none 
		 * output:			none
		--------------------------------------------------------------------------------*/
		/*
		 * public void makeAllFlashNameOutPut(ArrayList<String> allFlashName) {
		 * File newFile = new File(
		 * FlashRunnerCheckFlashName.allFlashNameOutPutPath); if
		 * (!newFile.exists()) { newFile.mkdirs(); } try { RandomAccessFile
		 * allFlashNameFile = new RandomAccessFile(
		 * FlashRunnerCheckFlashName.allFlashNameOutPutPath + "/" +
		 * FlashRunnerCheckFlashName.allFlashNameOutPutFileName, "rw"); int
		 * maxCount = allFlashName.size(); int i = 0; for (i = 0; i < maxCount;
		 * i++) { try { allFlashNameFile.writeBytes(allFlashName.get(i) + "\n");
		 * } catch (IOException e) { e.printStackTrace(); } } } catch
		 * (FileNotFoundException e) { e.printStackTrace(); } }
		 */
		/*-------------------------------------------------------------------------------
		 * Name :			doUnbindService
		 * Author :			Taylor Gu
		 * Date:			2011-10-19
		 * Description:		unbindService for this app
		 * Input:    		none 
		 * output:			none
		--------------------------------------------------------------------------------*/
		void doUnbindService() {
			Log.i("doUnbindService()------->", "mIsBound = " + mIsBound);
			if (mIsBound) {
				// if (mService != null) {
				// try {
				// Message msg = Message.obtain(null,
				// MSG_UNREGISTER_CLIENT);
				// msg.replyTo = mMessenger2;
				// mService.send(msg);
				// } catch (RemoteException e) {
				// }
				// }
				// try {
				// mContext.unbindService(mConnection2);
				// } catch (Exception e) {
				// e.printStackTrace();
				// }
				// mIsBound = false;
				// bindRef--;
				// Log.d(VirtualActivity.class.getSimpleName(),
				// "UnBind:bindRef="
				// + bindRef);
			} else {
			}
			try {
				mContext.unbindService(mConnection2);
			} catch (Exception e) {
				e.printStackTrace();
			}
			mIsBound = false;
			bindRef--;
			Log.d(VirtualActivity.class.getSimpleName(), "UnBind:bindRef="
					+ bindRef);
		}

		void doCallServiceForResult() {
			if (!mIsBound) {
				return;
			}
			Message msg = Message.obtain(null, MSG_GET_ZIPPEDFLASHPATH);
			Bundle bundle = new Bundle();
			bundle.putString(KEY_CALLER_PASSWORD, mMessenger2.toString());
			msg.setData(bundle);
			try {
				mService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		void doCallServiceForResult2() {
			// if (!mIsBound) {
			// return;
			// } else {
			// doUnbindService();
			// mIsBound = false;
			// }
			Message msg = null;
			Log.d("PlayFlash_Flag ===", "" + playFlash_Flag);
			if (playFlash_Flag) {
				Log.d("MSG==========", "PlayFlash");
				msg = Message.obtain(null, MSG_ACTION_PLAYFLASH);
				Bundle bundle = new Bundle();
				bundle.putString(KEY_CALLER_PASSWORD, mMessenger2.toString());
				bundle.putString(KEY_FLASHFILENAME, flashfilename);
				bundle.putInt(KEY_BEGINFRAME, begin);
				bundle.putInt(KEY_ENDFRAME, end);
				bundle.putString(KEY_ZIPPEDFLASHPATH,
						flashListPathName.substring(1));
				Log.d(VirtualActivity.class.getSimpleName(), "["
						+ flashfilename + "][" + begin + "][" + end + "]["
						+ flashListPathName.substring(1) + "]");
				msg.setData(bundle);
				try {
					mService.send(msg);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
				// doUnbindService();
				// Message msg = Message.obtain(null, MSG_ACTION_PLAYFLASH);
			}
		}

		/*-------------------------------------------------------------------------------
		 * Name :			analyseFlashName
		 * Author :			Taylor Gu
		 * Date:			2011-10-19
		 * Description:		Check flash Name like that flash name about :
		 * 					"bxc:05|m102030.swf /frame 3099 4387"
		 * Input:    		String->flashName: for check of name 
		 * output:			String[]->retCheckFlashName : divide check flash name for 
		 * 						flashName: m102030.swf
		 * 						begin: 3099
		 * 						end: 4387
		--------------------------------------------------------------------------------*/
		public String[] analyseFlashName(String flashName) {
			String[] retCheckFlashName = new String[3];
			int nameLength = flashName.length();
			int beginStart = 0;
			int beginOver = 0;
			int endStart = 0;
			int endOver = flashName.length();
			int spaceCount = 0;
			int count = 0;

			for (count = 0; count < nameLength; count++) {
				if (flashName.substring(count, count).equals(".")) {
					retCheckFlashName[0] = flashName.substring(0, count);
					count += 1;
				} else if (flashName.substring(count, count).equals("/")) {
					beginStart = count + 5;
				} else if (flashName.substring(count, count).equals(" ")) {
					spaceCount += 1;
					if (3 == spaceCount) {
						beginOver = count;
						endStart = count + 1;
					}
				}
			}
			retCheckFlashName[1] = flashName.substring(beginStart, beginOver);
			retCheckFlashName[2] = flashName.substring(endStart, endOver);

			return retCheckFlashName;
		}

		void doCallServiceForResult3() {
			if (!mIsBound) {
				return;
			}
			Message msg = Message.obtain(null, MSG_ACTION_CHECKFLASHNAME);
			Bundle bundle = new Bundle();
			bundle.putString(KEY_CALLER_PASSWORD, mMessenger2.toString());
			bundle.putString(KEY_FLASHFILENAME, flashfilename);
			bundle.putInt(KEY_BEGINFRAME, begin);
			bundle.putInt(KEY_ENDFRAME, end);
			msg.setData(bundle);
			try {
				mService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		void doCallServiceForSendMsg() {
			if (!mIsBound) {
				return;
			}
			Message msg = Message.obtain(null, MSG_ACTION_CHECKFLASHNAME);
			Bundle bundle = new Bundle();
			bundle.putString(KEY_CALLER_PASSWORD, mMessenger2.toString());
			bundle.putString(KEY_FLASHFILENAME, flashfilename);
			bundle.putInt(KEY_BEGINFRAME, begin);
			bundle.putInt(KEY_ENDFRAME, end);
			msg.setData(bundle);
			try {
				mService.send(msg);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	}
}
