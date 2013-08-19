package com.besta.app.smartlearn.flashrunner;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

public class BA001_FlashRunnerActivity extends Activity {
	final static int prePackageLength = 25;
	static int maxFlashNameLength = 1;
	static int maxHashMapOneArryLength = 3 + maxFlashNameLength;
	static String flashListPath = "|";
	static byte byteReadTmp[] = new byte[4];

	/*-------------------------------------------------------------------------------
	 * Name :			HashMapArry
	 * Author :			Taylor Gu
	 * Date:			2011-10-19
	 * Description:		Defined by myself for write in file of class(like a struct{})
	 * class member:    byte->count: count for this hashMap array's count
	 * 					byte->appHighBit: store this flashName belong to which app
	 * 					byte->appLowBit: same with appHighBit
	 * 					byte[]->tempString: store this flash name
	--------------------------------------------------------------------------------*/
	public static class HashMapArry {
		byte count;
		byte appHighBit;
		byte appLowBit;
		byte[] tempString = null;

		/*-------------------------------------------------------------------------------
		 * Method :			HashMapArry()
		 * Author :			Taylor Gu
		 * Date:			2011-10-19
		 * Description:		this class's construct
		--------------------------------------------------------------------------------*/
		public HashMapArry() {
			count = 0;
			appHighBit = 0;
			appLowBit = 0;
			tempString = new byte[maxFlashNameLength];
		}

		/*-------------------------------------------------------------------------------
		 * Method :			ReadOneInt
		 * Author :			Taylor Gu
		 * Date:			2011-10-19
		 * Description:		Read a int type in the hashMap
		 * Input:			RanDomAccessFile->hashMapFile: read's file
		 * 					int->bitCount: how long(4bit) for read from this file
		 * Output:			byte[]->checkBit: return byte[] that convert from int 
		--------------------------------------------------------------------------------*/
		public byte[] ReadOneInt(RandomAccessFile hashMapFile, int bitCount) {
			byte[] checkBit = new byte[4];
			int Count = 0;
			try {
				for (Count = 0; Count < bitCount; Count++) {
					checkBit[Count] = hashMapFile.readByte();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
			return checkBit;
		}

		public byte[] intToByte(int intValue) {
			byte[] byteBit = new byte[4];
			int b = 0xFFFFFF00;

			byteBit[0] = (byte) intValue;
			intValue = (int) intValue & b;
			intValue = intValue >> 8;
			byteBit[1] = (byte) intValue;
			intValue = (int) intValue & b;
			intValue = intValue >> 8;
			byteBit[2] = (byte) intValue;
			intValue = (int) intValue & b;
			intValue = intValue >> 8;
			byteBit[3] = (byte) (intValue >> 8);
			return byteBit;
		};
	};

	String edtText = "";

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		// File xxx = getFilesDir();
		List<ResolveInfo> serviceInfoList = getPackageManager()
				.queryIntentServices(
						new Intent(
								"com.besta.app.smartlearn.engsupertutor.gzcourse.gztestlisten.nouse"),
						0);

		for (int sizeCount = 0; sizeCount < serviceInfoList.size(); sizeCount++) {
			String pkg_name = serviceInfoList.get(sizeCount).serviceInfo.packageName;
			pkg_name = "";
		}

		bt_1_fun();
		btCheckFlashName();
	}

	public void selectHashMapOrSearch() {
		boolean openHashMapFile = false;
		openHashMapFile = BA001_FlashRunnerActivity.OpenFlashNameHashMap();

		if (openHashMapFile) {
			// this file exist and find the name directly
			try {
				openFileInput(FlashRunnerCheckFlashName.createHashMapName);
			} catch (FileNotFoundException e2) {
				e2.printStackTrace();
			}
			String fileAndPath = FlashRunnerCheckFlashName.createFilePath + "/"
					+ FlashRunnerCheckFlashName.createHashMapName;
			RandomAccessFile fileHashMap = null;
			int maxNameLength = 0;
			String chosAppName = "0";
			try {
				fileHashMap = new RandomAccessFile(fileAndPath, "r");
				try {
					fileHashMap.seek(fileHashMap.length() - 33);
					maxNameLength = fileHashMap.readByte();
				} catch (IOException e) {
					e.printStackTrace();
				}
				String flashName = edtText;
				// String flashName = "wd01B041";
				flashName = flashName.toUpperCase();
				int flshNameLength = flashName.length();
				if (flshNameLength < maxNameLength) {
					int i = 0;
					for (i = 0; i < maxNameLength - flshNameLength; i++) {
						flashName = "0" + flashName;
					}
				}
				// choose the app for play flash; return:app name
				chosAppName = checkChosAppName(fileHashMap, flashName);
			} catch (FileNotFoundException e1) {
				e1.printStackTrace();
			}
			TextView tv = (TextView) findViewById(R.id.main_tv_1);
			tv.setText(chosAppName);
			// finish();
		} else {
			FlashService reFreshFile = new FlashService();
			reFreshFile.mContext = BA001_FlashRunnerActivity.this;
			reFreshFile.nowAppContext = BA001_FlashRunnerActivity.this;
			reFreshFile.flashfilename = " ";
			reFreshFile.makeHashMapInfo();
		}
	} // End of Func

	public void btCheckFlashName() {
		Button bt_1 = (Button) findViewById(R.id.main_bt_checkFlashName);
		bt_1.setOnClickListener(btCheckFlashName_listener);
	}

	public Button.OnClickListener btCheckFlashName_listener = new Button.OnClickListener() {
		public void onClick(View v) {
			Intent activity = new Intent(BA001_FlashRunnerActivity.this,
					FlashRunnerCheckFlashName.class);
			startActivity(activity);
		}
	};

	/*-------------------------------------------------------------------------------
	 * Name :			bt_1_fun
	 * Author :			Taylor Gu
	 * Date:			2011-10-19
	 * Description:		bt_1's function for search input flash name
	 * Input:			none
	 * Output:			none
	--------------------------------------------------------------------------------*/
	public void bt_1_fun() {
		Button bt_1 = (Button) findViewById(R.id.main_bt_1);
		bt_1.setOnClickListener(bt1_listener);
	}

	public Button.OnClickListener bt1_listener = new Button.OnClickListener() {
		public void onClick(View v) {
			EditText edittext = (EditText) findViewById(R.id.main_et_1);
			String edittext_value = edittext.getText().toString();
			edtText = edittext_value;
			selectHashMapOrSearch();
		}
	};

	/*-------------------------------------------------------------------------------
	 * Name :			checkChosAppName
	 * Author :			Taylor Gu
	 * Date:			2011-10-19
	 * Description:		Check flash name use which app open and play
	 * Input:			RandomAccessFile->fileHashMap: this file
	 * Output:			String->chooseOpenAppName: app package name
	--------------------------------------------------------------------------------*/
	public static String checkChosAppName(RandomAccessFile fileHashMap,
			String flashName) {
		String chooseOpenAppName = "";
		int hash1 = 0;
		int hash2 = 0;
		int[] hashSize = new int[2];
		// read hashMapSize and addHashMapSize from fileHashMap file
		hashSize = ReadHashMaxSize(fileHashMap);
		int[] prime = new int[2];
		// read hashMap's max prime and addHashMap's max prime
		prime = ReadOneIntOfPrime(fileHashMap);
		if (prime[1] == 0) {
			chooseOpenAppName = "null";
			return chooseOpenAppName;
		}
		// hash func
		hash1 = hashFunction(flashName, 1);
		int hashArray1 = hash1 % prime[1];

		// Log.d("hash1 ==========", "" + hash1);
		// Log.d("prime[1] ==========", "" + prime[1]);
		// select from hashMap file with this flash name
		if (hashArray1 >= hashSize[1]) {
			chooseOpenAppName = "not Found !";
		} else {
			HashMapArry checkOneName = new HashMapArry();
			// read one class(like a struct{}) from hashMap file
			checkOneName = readMarkInHashMap(fileHashMap, hashArray1);
			int a = 0;
			int start = 0;
			int end = 0;
			int a_count = new String(checkOneName.tempString).length();
			end = a_count - 1;
			for (a = 0; a < a_count; a++) {
				if (checkOneName.tempString[a] == '*') {
					start = a + 1;
					continue;
				} else if (checkOneName.tempString[a] == '|') {
					end = a;
					break;
				}
			}
			// Log.d("checkOneName.tempString ======", new
			// String(checkOneName.tempString) + ",count=" +
			// checkOneName.count);
			String checkFlashName = "";
			if (1 == checkOneName.count) {
				checkFlashName = new String(checkOneName.tempString).substring(
						start, end);
				// Log.d("checkFlashName ======", checkFlashName);
				if (checkFlashName.equals(flashName)) {
					FlashService.flashListPathName = new String(
							checkOneName.tempString).substring(end);
					// Log.d("flashListPathName", new
					// String(checkOneName.tempString) + "[253=" + end);
					chooseOpenAppName = ChooseOpenApp(fileHashMap, checkOneName);
				} else {
					chooseOpenAppName = "not Found !";
				}
			} else if (0 == checkOneName.count) {
				chooseOpenAppName = "not Found !";
			} else if (1 < checkOneName.count) {
				checkFlashName = new String(checkOneName.tempString).substring(
						start, end);
				if (checkFlashName.equals(flashName)) {
					FlashService.flashListPathName = new String(
							checkOneName.tempString).substring(end);
					// Log.d("flashListPathName", new
					// String(checkOneName.tempString) + "[264=" + end);
					chooseOpenAppName = ChooseOpenApp(fileHashMap, checkOneName);
				} else {
					hash2 = hashFunction(flashName, 2);
					int hashArray2 = hash2 % prime[0];

					if (hashArray2 >= hashSize[0]) {
						chooseOpenAppName = "not Found !";
					} else {
						try {
							fileHashMap.seek(fileHashMap.length() - 4);
							byte[] tmpByte = new byte[4];
							int firstHashMapSize = 0;
							int i = 0;

							for (i = 0; i < 4; i++) {
								tmpByte[i] = fileHashMap.readByte();
								if (0 > tmpByte[i]) {
									firstHashMapSize = firstHashMapSize
											+ (tmpByte[i] + 256);
								} else {
									firstHashMapSize = firstHashMapSize
											+ tmpByte[i];
								}

								if (3 > i) {
									firstHashMapSize = firstHashMapSize << 8;
								}
							}
							hashArray2 = hashArray2 + firstHashMapSize;
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						HashMapArry checkOneName2 = new HashMapArry();
						checkOneName2 = readMarkInHashMap(fileHashMap,
								hashArray2);
						// added by Leo Wei on 2011-10-22
						a_count = new String(checkOneName2.tempString).length();
						end = a_count - 1;
						start = 0;
						end = 0;
						for (a = 0; a < a_count; a++) {
							if (checkOneName2.tempString[a] == '*') {
								start = a + 1;
								continue;
							} else if (checkOneName2.tempString[a] == '|') {
								end = a;
								break;
							}
						}
						// added end.
						// Log.d("checkOneName2.tempString ======", new
						// String(checkOneName2.tempString) + "count=" +
						// checkOneName2.count);
						if (1 == checkOneName2.count) {
							checkFlashName = new String(
									checkOneName2.tempString).substring(start,
									end);
							if (checkFlashName.equals(flashName)) {
								FlashService.flashListPathName = new String(
										checkOneName2.tempString)
										.substring(end);
								// Log.d("flashListPathName", new
								// String(checkOneName2.tempString) + "[319=" +
								// end);
								chooseOpenAppName = ChooseOpenApp(fileHashMap,
										checkOneName2);
							} else {
								chooseOpenAppName = "not Found !";
							}
						} else if (0 == checkOneName2.count) {
							chooseOpenAppName = "not Found !";
						} else if (1 < checkOneName2.count) {
							String name = new String(checkOneName2.tempString)
									.substring(start, end);
							if (name.equals(flashName)) {
								FlashService.flashListPathName = new String(
										checkOneName2.tempString)
										.substring(end);
								// Log.d("flashListPathName", new
								// String(checkOneName2.tempString) + "[331=" +
								// end);
								chooseOpenAppName = ChooseOpenApp(fileHashMap,
										checkOneName2);
							} else {
								try {
									fileHashMap.seek(fileHashMap.length() - 16);
									int otherFlash = 0;
									otherFlash = fileHashMap.readInt();

									if (0 == otherFlash) {
										chooseOpenAppName = "not Found !";
									} else {
										long otherNameAdrss = 0;
										fileHashMap
												.seek(fileHashMap.length() - 24);
										otherNameAdrss = fileHashMap.readLong();
										int count = 0;
										String tmp = "";
										byte[] buf = new byte[maxFlashNameLength + 2];
										byte[] bufExterName = new byte[4];
										fileHashMap.seek(otherNameAdrss);
										for (count = 0; count < otherFlash; count++) {
											fileHashMap.read(buf);
											fileHashMap.read(bufExterName);
											tmp = new String(buf);
											a_count = tmp.length();
											// start = 2;
											end = a_count - 1;
											start = 2;
											end = 0;
											// added by Leo Wei on 2011-10-24
											// for searching "b100070" in GZPC.
											for (a = 0; a < a_count; a++) {
												if (buf[a] == '*') {
													start = a + 1;
													continue;
												} else if (buf[a] == '|') {
													end = a;
													break;
												}
											}
											// Log.d("otherFlashNameCount",
											// "otherFlashNameCount=" +
											// otherFlash + "+=" +
											// tmp.substring(start, end) + "+="
											// + flashName);
											// added end.
											if (tmp.substring(start, end)
													.equals(// replace "2" by
															// "start" by Leo
															// Wei on 2011-11-1
															// for
															// BUG-DC1381-2011-1101-090358-Zheng-Lang
													flashName)) {
												HashMapArry checkOtherOne = new HashMapArry();
												checkOtherOne.appHighBit = buf[0];
												checkOtherOne.appLowBit = buf[1];
												FlashService.flashListPathName = tmp
														.substring(end);
												// Log.d("flashListPathName",
												// tmp + "[376=" + end);
												chooseOpenAppName = ChooseOpenApp(
														fileHashMap,
														checkOtherOne);
												break;
											} else {
												chooseOpenAppName = "not Found !";
											}
										}
									}
								} catch (IOException e) {
									e.printStackTrace();
								}
							}
						}
					}
				}
			}
		}
		return chooseOpenAppName;
	}

	/*-------------------------------------------------------------------------------
	 * Name :			ReadHashMaxSize
	 * Author :			Taylor Gu
	 * Date:			2011-10-19
	 * Description:		read hashmap and add hashMap size from hashMap file
	 * Input:			RandomAccessFile->hashMapFile: this file
	 * Output:			int[]->hashSize: hashSize[0] addHashMapSize
	 * 									 hashSize[1] hashMapSize
	--------------------------------------------------------------------------------*/
	public static int[] ReadHashMaxSize(RandomAccessFile hashMapFile) {
		int[] hashSize = new int[2];
		hashSize[0] = 0;
		hashSize[1] = 0;
		byte[] size = new byte[8];
		try {
			hashMapFile.seek(hashMapFile.length() - 33);
			maxFlashNameLength = hashMapFile.readByte();
			maxHashMapOneArryLength = 3 + maxFlashNameLength;
			int i = 0;
			for (i = 0; i < 8; i++) {
				size[i] = hashMapFile.readByte();
				if (size[i] < 0) {
					if (4 > i) {
						hashSize[0] = hashSize[0] + (size[i] + 256);
					} else {
						hashSize[1] = hashSize[1] + (size[i] + 256);
					}
				} else {
					if (4 > i) {
						hashSize[0] = hashSize[0] + size[i];
					} else {
						hashSize[1] = hashSize[1] + size[i];
					}
				}

				if (3 > i) {
					hashSize[0] = hashSize[0] << 8;
				} else if (3 < i && 7 > i) {
					hashSize[1] = hashSize[1] << 8;
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return hashSize;
	}

	/*-------------------------------------------------------------------------------
	 * Name :			ChooseOpenApp
	 * Author :			Taylor Gu
	 * Date:			2011-10-19
	 * Description:		Select which app for palyflash
	 * Input:			RandomAccessFile->hashMap: this file
	 * 					HashMapArry->checkOne: the array in this hashMap file
	 * Output:			String->retApp: app package name
	--------------------------------------------------------------------------------*/
	public static String ChooseOpenApp(RandomAccessFile hashMap,
			HashMapArry checkOne) {
		String retApp = "";
		byte[] AppCount = new byte[2];
		int openAppCount = 0;

		AppCount[0] = checkOne.appHighBit;
		AppCount[1] = checkOne.appLowBit;
		openAppCount = (10 * (AppCount[0] - 48)) + (AppCount[1] - 48);
		try {
			hashMap.seek(hashMap.length() - 41 - (openAppCount * 9));
			byte[] openAppInfo = new byte[9];
			long appNameAddress = 0;
			int appNameLength = 0;
			int count = 0;

			for (count = 0; count < 9; count++) {
				openAppInfo[count] = hashMap.readByte();
				if (0 > openAppInfo[count]) {
					if (8 > count) {
						appNameAddress = appNameAddress
								+ (openAppInfo[count] + 256);
					} else {
						appNameLength = appNameLength
								+ (openAppInfo[count] + 256);
					}
				} else {
					if (8 > count) {
						appNameAddress = appNameAddress + openAppInfo[count];
					} else {
						appNameLength = appNameLength + openAppInfo[count];
					}
				}

				if (7 > count) {
					appNameAddress = appNameAddress << 8;
				}
			}

			byte[] appNameBuffer = new byte[appNameLength];
			int i = 0;
			hashMap.seek(appNameAddress);

			for (i = 0; i < appNameLength; i++) {

				appNameBuffer[i] = hashMap.readByte();
			}

			retApp = new String(appNameBuffer);

		} catch (IOException e) {

			e.printStackTrace();
		}

		return retApp;

	}

	/*-------------------------------------------------------------------------------
	 * Name :			OpenFlashNameHashMap
	 * Author :			Taylor Gu
	 * Date:			2011-10-19
	 * Description:		Select which app for palyflash
	 * Input:			none
	 * Output:			boolean->openFile: true->exist hashMap; false->not exist
	--------------------------------------------------------------------------------*/
	public static boolean OpenFlashNameHashMap() {
		boolean openFile = false;
		String hashMapName = FlashRunnerCheckFlashName.createHashMapName;
		String hashMapPath = FlashRunnerCheckFlashName.createFilePath;
		String hashMapNameAndPath = FlashRunnerCheckFlashName.createFilePath
				+ "/" + FlashRunnerCheckFlashName.createHashMapName;

		File hashMapFile = new File(hashMapPath, hashMapName);
		openFile = hashMapFile.exists();

		return openFile;
	}

	/*-------------------------------------------------------------------------------
	 * Name :			ReadOneIntOfPrime
	 * Author :			Taylor Gu
	 * Date:			2011-10-19
	 * Description:		Read hashMap and addHashMap of prime from hashMap file
	 * Input:			RandomAccessFile->hashMap: this hashMap file
	 * Output:			int[]->ret: ret[0] addHashMap prime
	 * 								ret[1] hashMap prime
	--------------------------------------------------------------------------------*/
	public static int[] ReadOneIntOfPrime(RandomAccessFile hashMap) {
		int[] ret = new int[2];
		byte[] primeByte = new byte[8];
		ret[0] = 0;
		ret[1] = 0;
		try {
			hashMap.seek(hashMap.length() - 12);
			int count = 0;
			for (count = 0; count < 8; count++) {
				primeByte[count] = hashMap.readByte();
				if (primeByte[count] < 0) {
					if (count <= 3) {
						ret[0] = ret[0] + (int) (primeByte[count] + 256);
						if (count < 3) {
							ret[0] = ret[0] << 8;
						}
					} else if (count > 3 && count < 8) {
						ret[1] = ret[1] + (int) (primeByte[count] + 256);
						if (count < 7) {
							ret[1] = ret[1] << 8;
						}
					}
				} else {
					if (count <= 3) {
						ret[0] = ret[0] + primeByte[count];
						if (count < 3) {
							ret[0] = ret[0] << 8;
						}
					} else if (count > 3 && count < 8) {
						ret[1] = ret[1] + primeByte[count];
						if (count < 7) {
							ret[1] = ret[1] << 8;
						}
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return ret;
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
		String flashName = "";
		int appCount = 0;
		int infoSize = 0; // Package count
		int sizeCount = 0; // Package count for ( for(...) func )
		String subStrTemp = " ";

		PackageManager pkgmanager = getPackageManager();
		List<PackageInfo> pkginfo = pkgmanager.getInstalledPackages(0);
		infoSize = pkginfo.size();
		ArrayList<String> appPackage = new ArrayList<String>();
		int correspondPackage = 0;
		try {
			RandomAccessFile allPackageFile = new RandomAccessFile(
					(FlashRunnerCheckFlashName.createFilePath + "/" + FlashRunnerCheckFlashName.createAppPackageName),
					"rw");

			for (sizeCount = 0; sizeCount < infoSize; sizeCount++) {
				strTemp = "";
				flashName = "";
				info = pkginfo.get(sizeCount);
				strTemp = info.packageName;

				if (prePackageLength >= strTemp.length()) {
					continue; // package name length <=prePackageLength is not
								// select
				} else {
					subStrTemp = strTemp.substring(0, prePackageLength);
					flashName = strTemp.substring(prePackageLength);
					if ((FlashRunnerCheckFlashName.conTextString
							.equals(subStrTemp))) {
						appPackage.add(strTemp);
						correspondPackage += 1;
						try {
							allPackageFile.writeBytes(strTemp + "\n");
						} catch (IOException e) {
							e.printStackTrace();
						}
					} else {
					}
				}
			}
			try {
				allPackageFile.writeInt(correspondPackage);
				allPackageFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return appPackage;
	} // End of Func

	/*-------------------------------------------------------------------------------
	 * Name :			CreateFlashNameHashMap
	 * Author :			Taylor Gu
	 * Date:			2011-10-19
	 * Description:		create new hashMap file
	 * Input:    		ArrayList<String>->allFlashName: all flash name
	 * 					int->allFlashNameCount: all flash name count
	 * 					ArrayList<String>->allPackage: all package name
	 * output:			none
	--------------------------------------------------------------------------------*/
	public static void CreateFlashNameHashMap(ArrayList<String> allFlashName,
			int allFlashNameCount, ArrayList<String> allPackage) {
		String flashName = "";
		int hash = 0;
		int hashArryCount = 0;
		int totalFlashNameCount = 0;
		int sizeOfHashMap = 0;
		int firstHashMapSize = 0;
		long appNameStartAddress = 0;
		int flashCountMaxPrime = 0;
		int writeCount = 0; // count write the flash names
		int otherFlashNameCount = 0;
		long otherFlashNameAddress = 0; // other flash name for store start
										// address
		ArrayList<String> addHashMapFlashName = new ArrayList<String>();
		// Flash total count
		totalFlashNameCount = allFlashNameCount;
		Log.d("AAA", "TotalFlashCount ==========" + totalFlashNameCount + "");
		// Reset HashMap
		sizeOfHashMap = ResetHashMapSize(totalFlashNameCount);
		firstHashMapSize = sizeOfHashMap;
		// initialization the HashMap
		reSetHashMap(sizeOfHashMap);
		// select max prime in sizeOfHashMap
		flashCountMaxPrime = SelectMaxPrime(sizeOfHashMap);

		File newFile = new File(FlashRunnerCheckFlashName.createFilePath);
		if (!newFile.exists()) {
			newFile.mkdirs();
		}
		String fileName = FlashRunnerCheckFlashName.createFilePath + "/"
				+ FlashRunnerCheckFlashName.createHashMapName;
		try {
			RandomAccessFile hashMapFile = new RandomAccessFile(fileName, "rw");
			// write the arry mark in HashMap

			for (writeCount = 0; writeCount < totalFlashNameCount; writeCount++) {
				// Log.d("flashName ========", flashName);
				flashName = GetOneFlashName(allFlashName, writeCount);
				// Log.d("flashName ========", flashName);
				hash = hashFunction(flashName.substring(2), 1); // using for();
																// writen in
				// HashMap out a arry mark
				hashArryCount = hash % flashCountMaxPrime;
				// Log.d("hash ========", flashName);
				// Log.d("flashCountMaxPrime ========", "" +
				// flashCountMaxPrime);
				// Log.d("hash ========", "" + hash);
				addHashMapFlashName.addAll(writeMarkInHashMap(flashName,
						hashMapFile, sizeOfHashMap, hashArryCount));
			}

			int addHashMapSizeCount = addHashMapFlashName.size();
			int addHashMapReadCount = 0;
			int addHash = 0;
			int addFlashCountMaxPrime = 0;
			int addHashArryCount = 0;
			String addOneFlashName = "";
			int addHashSize = 0;
			Log.e("AAA", "----->" + addHashMapSizeCount);

			if (0 != addHashMapSizeCount) {
				// add the HashMap size and initialize this scope
				addFlashCountMaxPrime = AddHashMapSize(hashMapFile,
						addHashMapSizeCount + 1);
				addHashSize = addFlashCountMaxPrime;
				ArrayList<String> otherFlashName = new ArrayList<String>();

				for (addHashMapReadCount = 0; addHashMapReadCount < addHashMapSizeCount; addHashMapReadCount++) {
					addOneFlashName = readAddFlashName(addHashMapFlashName,
							addHashMapReadCount);
					// Log.d("addOneFlashName ========", addOneFlashName);
					addHash = hashFunction(addOneFlashName.substring(2), 2);
					addFlashCountMaxPrime = SelectMaxPrime(addFlashCountMaxPrime);
					// now array position + old size
					// int x = addHash % addFlashCountMaxPrime;
					addHashArryCount = ((addHash % addFlashCountMaxPrime) * maxHashMapOneArryLength)
							+ sizeOfHashMap * maxHashMapOneArryLength;

					otherFlashName
							.addAll(writeMarkInHashMap2(addOneFlashName,
									hashMapFile, sizeOfHashMap
											* maxHashMapOneArryLength,
									addHashArryCount));

				}

				otherFlashNameCount = otherFlashName.size();
				Log.e("AAA", "otherFlashNameCount----->" + otherFlashNameCount);
				int nowOtherFlashName = 0;
				if (0 == otherFlashNameCount) {
					otherFlashNameAddress = 0;
				} else {
					try {
						otherFlashNameAddress = hashMapFile.length();
						hashMapFile.seek(otherFlashNameAddress);
						for (nowOtherFlashName = 0; nowOtherFlashName < otherFlashNameCount; nowOtherFlashName++) {
							hashMapFile.writeBytes(otherFlashName
									.get(nowOtherFlashName) + ".bfh");
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				} // End if
			}

			try {
				int appPackageCount = allPackage.size();
				int nowAppPackage = 0;
				long[] chooseAppPackageAddress = new long[appPackageCount];
				hashMapFile.seek(hashMapFile.length());

				for (nowAppPackage = appPackageCount - 1; nowAppPackage >= 0; nowAppPackage--) {
					String AppCount = "";
					AppCount = String.valueOf(nowAppPackage + 1);
					chooseAppPackageAddress[nowAppPackage] = hashMapFile
							.length();
					hashMapFile.writeBytes(allPackage.get(nowAppPackage));
				}

				// obtain appNameString's address
				appNameStartAddress = hashMapFile.length();

				for (nowAppPackage = appPackageCount - 1; nowAppPackage >= 0; nowAppPackage--) {
					// write one app package name of address
					hashMapFile
							.writeLong(chooseAppPackageAddress[nowAppPackage]);
					// write length of one app package
					String nowPackageString = allPackage.get(nowAppPackage);
					int length = nowPackageString.length();
					hashMapFile.writeByte(length);
				}

				// 8 byte(long) for save appPackageName start Address
				hashMapFile.writeLong(appNameStartAddress);
				// 1 byte for save maxFlashNameLength
				hashMapFile.writeByte(maxFlashNameLength);
				// 4 byte for save addhashSize
				hashMapFile.writeInt(addHashSize);
				// 4 byte(int) for save firstHashMapSize
				hashMapFile.writeInt(firstHashMapSize);
				// 8 byte(long) for save otherFlashNameAddress
				hashMapFile.writeLong(otherFlashNameAddress);
				// 4 byte(int) for save otherFlashNameCount
				hashMapFile.writeInt(otherFlashNameCount);
				// 4 byte(int) for save addFlashCountMaxPrime
				hashMapFile.writeInt(addFlashCountMaxPrime);
				// 4 byte(int) for save flashCountMaxPrime
				hashMapFile.writeInt(flashCountMaxPrime);
				// 4 byte(int) for save firstHashMapSize
				hashMapFile.writeInt(firstHashMapSize);

				hashMapFile.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	}// End of Func

	/*-------------------------------------------------------------------------------
	 * Name :			GetOneFlashName
	 * Author :			Taylor Gu
	 * Date:			2011-10-19
	 * Description:		Format get one flashName from ArrayList<String> flashName
	 * Input:    		ArrayList<String>->flashName: allFlashName
	 * 					int->arrayListCount: one of flash name in allFlashName
	 * output:			String->oneFlashName: return one flash name that already format
	--------------------------------------------------------------------------------*/
	// read a flash name from ArrayList
	public static String GetOneFlashName(ArrayList<String> flashName,
			int arrayListCount) {

		String oneFlashName = flashName.get(arrayListCount);
		oneFlashName = oneFlashName.substring(2, oneFlashName.length());
		int oneFlashNameLength = oneFlashName.length();
		if ((oneFlashNameLength > 4)
				&& (oneFlashName.substring(oneFlashNameLength - 4)
						.equals(".BFH"))) {
			oneFlashName = oneFlashName.substring(0, oneFlashNameLength - 4);
		}

		int realFlashNameLength = oneFlashName.length();
		byte[] tmByte = oneFlashName.getBytes();

		if (maxFlashNameLength > realFlashNameLength) {
			int i = 0;
			int j = 0;
			byte[] flashNameString = new byte[maxFlashNameLength];

			for (i = 0; i < maxFlashNameLength - realFlashNameLength; i++) {
				flashNameString[i] = '*';// tempStrByte[0];
			}
			j = i;
			for (i = 0; i < realFlashNameLength; j++, i++) {
				flashNameString[j] = tmByte[i];
			}

			oneFlashName = new String(flashNameString);
		}

		oneFlashName = flashName.get(arrayListCount).substring(0, 2)
				+ oneFlashName;
		// Log.d("flashName ========", oneFlashName);
		// Log.d("maxFlashNameLength ========", " " + maxFlashNameLength);
		return oneFlashName;
	}

	// open one app flashlist and return all flash name
	public static ArrayList<String> OpenOneAppFlashList(String listPath,
			int whichApp) {
		ArrayList<String> oneAppFlashName = new ArrayList<String>();
		if (listPath.equals("0")) {
			String AppCount = "";
			AppCount = String.valueOf(whichApp + 1);
			if ((0 < (whichApp + 1)) && (10 > (whichApp + 1))) {
				AppCount = "0" + AppCount;
			}
			oneAppFlashName.add(AppCount + "x");
		} else {
			try {
				RandomAccessFile nameListFile = new RandomAccessFile(listPath,
						"r");// "rw");
				oneAppFlashName = ReadOneAppNameList(nameListFile, listPath,
						whichApp);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
		}

		return oneAppFlashName;
	}

	// read one app flashlist and return all flash name
	public static ArrayList<String> ReadOneAppNameList(
			RandomAccessFile nameListFile, String fileName, int whichApp) {

		ArrayList<String> oneAppAllName = new ArrayList<String>();
		String AppCount = "";
		String oneFlashName = "";
		AppCount = String.valueOf(whichApp + 1);
		if ((0 < (whichApp + 1)) && (10 > (whichApp + 1))) {
			AppCount = "0" + AppCount;
		}
		try {
			String res;
			int data[] = new int[2];
			RandomAccessFile file = new RandomAccessFile(fileName, "r");
			if (file == null || 4 > file.length()) {
				return oneAppAllName;
			}
			if (!(fileName.substring(fileName.length() - 5)
					.equalsIgnoreCase(".BFES"))) {
				// added by Leo Wei on 2011-10-25 for supporting another flash
				// list file's format.
				file.seek(0);
				int count = readMyInt(file);
				int step = 2 * readMyInt(file);
				int len = (int) (file.length() - 8);
				byte[] buf = new byte[len];
				file.read(buf);
				for (int i = 0; i < count; i++) {
					res = new String(buf, i * step, step,
							Charset.forName("UnicodeLittle"));
					res = res.toUpperCase();
					if ((res.length() > 4)
							&& (res.substring(res.length() - 4).equals(".BFH"))) {
						res = res.substring(0, res.length() - 4);
					}
					oneFlashName = AppCount + res + flashListPath;
					// oneFlashName = AppCount + res;

					if ((oneFlashName.length() - 2) > maxFlashNameLength) {
						maxFlashNameLength = oneFlashName.length() - 2;
						maxHashMapOneArryLength = 3 + maxFlashNameLength;
					}
					// Log.d("flashName ========", oneFlashName);
					// Log.d("maxFlashNameLength ========", "" +
					// maxFlashNameLength);
					oneAppAllName.add(oneFlashName);
					// Log.d("!bfes's flash", oneFlashName);
				}
			} else {
				int m_lNameTableOffset = getOffset(file, 0, 0, 2, data);
				int i;

				for (i = 1; i < 10000; i++) {
					int t = getOffset(file, 0, m_lNameTableOffset, i, data);
					if (t == 0) {
						break;
					}

					int len = data[1] - data[0];
					byte p[] = new byte[len];

					file.seek(t);
					file.read(p, 0, len);

					res = new String(p);
					res = res.toUpperCase();
					if ((res.length() > 4)
							&& (res.substring(res.length() - 4).equals(".BFH"))) {
						res = res.substring(0, res.length() - 4);
					}
					len = res.length();
					oneFlashName = AppCount + res + flashListPath;
					// oneFlashName = AppCount + res;

					if ((oneFlashName.length() - 2) > maxFlashNameLength) {
						maxFlashNameLength = oneFlashName.length() - 2;
						maxHashMapOneArryLength = 3 + maxFlashNameLength;
					}
					// Log.d("flashName ========", oneFlashName);
					// Log.d("maxFlashNameLength ========", "" +
					// maxFlashNameLength);
					oneAppAllName.add(oneFlashName);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return oneAppAllName;

	}

	// format int to 4 byte Array
	public static int toInt(byte b) {
		if (b >= 0) {
			return (int) b;
		} else {
			return (int) (b + 256);
		}
	}

	// use my method to read flashNameList file
	public static int readMyInt(RandomAccessFile file) {
		try {
			file.read(byteReadTmp);
			return (((int) (byteReadTmp[3] >= 0 ? byteReadTmp[3]
					: byteReadTmp[3] + 256)) << 24)
					| (((int) (byteReadTmp[2] >= 0 ? byteReadTmp[2]
							: byteReadTmp[2] + 256)) << 16)
					| (((int) (byteReadTmp[1] >= 0 ? byteReadTmp[1]
							: byteReadTmp[1] + 256)) << 8)
					| (((int) byteReadTmp[0] >= 0 ? byteReadTmp[0]
							: byteReadTmp[0] + 256));
		} catch (Exception e) {
			e.printStackTrace();
		}

		return 0;
	}

	// read flashNameList file
	public static int getOffset(RandomAccessFile file, int foucc, int l,
			int nIndex, int data[]) {
		int offset = 0;
		try {
			if (l == 0) {
				int pos = 0;
				file.seek(file.length() - 4);
				pos = readMyInt(file);
				file.seek(file.length() - pos);
			} else {
				file.seek(l);
			}

			int buffer[] = new int[2];
			buffer[0] = readMyInt(file);
			buffer[1] = readMyInt(file);
			if ((foucc == 0 || foucc == buffer[0])
					&& (nIndex > 0 && nIndex < buffer[1])) {
				file.skipBytes((nIndex - 1) * 4);
				buffer[0] = readMyInt(file);
				buffer[1] = readMyInt(file);
				offset = buffer[0];
				data[0] = buffer[0];
				data[1] = buffer[1];
			} else {
			}

		} catch (Exception e) {
			e.printStackTrace();
		}

		return offset;
	}

	// hash function
	public static int hashFunction(String constString, int selectHashFunc) {
		int hash = 0;
		byte[] strByte = new byte[maxFlashNameLength];
		int count = 0;
		strByte = constString.getBytes();
		int i = 0;
		int j = 0;
		int flashNameLength = constString.length();
		for (i = 0; i < flashNameLength; i++) {
			if (strByte[i] == '|') {
				break;
			} else if (strByte[i] == '*') {
				j = i + 1;
			}
		}
		constString = constString.substring(j, i);
		byte[] RealStrByte = new byte[i - j];
		RealStrByte = constString.getBytes();
		int realNameLength = constString.length();
		// Log.d("addOneFlashName ========", constString);
		if (1 == selectHashFunc) {
			// SDBM_HashFunction
			while ((null != RealStrByte) && (realNameLength > count)) {
				// equivalent to:
				hash = 65599 * hash + RealStrByte[count];
				hash = RealStrByte[count] + (hash << 6) + (hash << 16) - hash;
				count++;
			}
		} else if (2 == selectHashFunc) {
			// MyCreate_HashMap
			while ((null != RealStrByte) && (realNameLength > count)) {
				int bitCount = 0;
				int decade = 1;
				for (bitCount = 0; bitCount < realNameLength; bitCount++) {
					hash = hash
							+ (RealStrByte[bitCount] * /* ((bitCount + 1) * */decade);
					decade = decade * 10;
				}
				count++;
			}
		} else if (3 == selectHashFunc) {
			// JS_HashMap
			hash = 1315423911;
			count = 0;
			while ((null != RealStrByte) && (realNameLength > count)) {
				hash ^= ((hash << 5) + (RealStrByte[count++]) + (hash >> 2));
			}
		} else if (4 == selectHashFunc) {
			// BKDR_HashMap
			hash = 0;
			int seed = 131313; // 31 131 1313 13131 131313
			count = 0;
			while ((null != RealStrByte) && (maxFlashNameLength > count)) {
				hash = hash * seed + (RealStrByte[count++]);
			}
		}

		hash = hash & 0x7FFFFFFF;

		return hash;
	}

	// Reset hashMap size
	public static int ResetHashMapSize(int sizeOfHashMap) {
		int lengthCount = 0;
		int reSetHashMapSize = 2;
		if (1 == sizeOfHashMap) {

		} else if (1 < sizeOfHashMap) {
			sizeOfHashMap = (int) (sizeOfHashMap * 0.75) * 2;
			for (lengthCount = 0; sizeOfHashMap > 2; lengthCount++) {
				sizeOfHashMap = sizeOfHashMap / 2;
				reSetHashMapSize *= 2;
			}
			sizeOfHashMap = 2 * reSetHashMapSize; // added "2*" by Leo Wei on
													// 2011-11-1 for reduce the
													// items number that be
													// saved behind the two hash
													// tables(items that be
													// conflicted twice).
		}

		return sizeOfHashMap;
	}

	// Reset HashMap : return one < sizeOfHashMap
	public static void reSetHashMap(int sizeOfHashMap) {
		int lengthCount = 0;
		String fileName = FlashRunnerCheckFlashName.createFilePath + "/"
				+ FlashRunnerCheckFlashName.createHashMapName;
		try {
			File makeFlashListFile = new File(fileName);
			makeFlashListFile.delete();
			makeFlashListFile.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
		}

		try {
			RandomAccessFile file = new RandomAccessFile(fileName, "rw");
			byte[] byteResetHashMapInfo = new byte[maxHashMapOneArryLength
					* sizeOfHashMap];
			try {
				file.write(byteResetHashMapInfo);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	} // End of Func

	public static int SelectMaxPrime(int sizeOfHashMap) {
		int maxPrime = 0;
		int count = 0;
		int sqrtScope = 0;

		if (1 == sizeOfHashMap) {
			maxPrime = sizeOfHashMap;
		} else if (2 == sizeOfHashMap) {
			maxPrime = sizeOfHashMap;
		} else if ((3 <= sizeOfHashMap) && (5 > sizeOfHashMap)) {
			maxPrime = 3;
		} else if ((5 <= sizeOfHashMap) && (7 > sizeOfHashMap)) {
			maxPrime = 5;
		} else if ((7 <= sizeOfHashMap) && (11 > sizeOfHashMap)) {
			maxPrime = 7;
		} else if (10 < sizeOfHashMap) {
			for (; sizeOfHashMap > 10;) {
				if (1 == (sizeOfHashMap & 1)) {
					sqrtScope = (int) Math.sqrt((double) sizeOfHashMap);
					for (count = 2; count <= sqrtScope; count++) {
						if (0 == (sizeOfHashMap % count)) {
							maxPrime = 0;
							break;
						} else {
							maxPrime = sizeOfHashMap;
						}
					}

					if (0 != maxPrime) {
						break;
					}
					sizeOfHashMap -= 2;
				} else if (0 == (sizeOfHashMap & 1)) {
					sizeOfHashMap -= 1;
				}
			}
		}

		return maxPrime;
	}

	// read one flash name for next HashMap
	public static String readAddFlashName(ArrayList<String> addOneName,
			int addCount) {

		String flashNameStr = "";

		flashNameStr = addOneName.get(addCount);

		return flashNameStr;

	} // End of Func

	// check read mark for write new
	public static HashMapArry readMarkInHashMap(RandomAccessFile hashMapFile,
			int hashArryCount) {
		HashMapArry locateHashArry = new HashMapArry();
		try {
			hashMapFile.seek(hashArryCount * maxHashMapOneArryLength);
			byte[] byteTmp = new byte[maxHashMapOneArryLength];
			hashMapFile.read(byteTmp);
			locateHashArry.count = byteTmp[0];
			locateHashArry.appHighBit = byteTmp[1];
			locateHashArry.appLowBit = byteTmp[2];

			int i = 3, j = 0, k = 0, m = 0;
			for (i = 3, j = 0; i < maxHashMapOneArryLength; i++, j++) {
				if (byteTmp[i] == '|') {
					m = i;
				}
				locateHashArry.tempString[j] = byteTmp[i];
			}

			byte[] newListPath = new byte[maxHashMapOneArryLength - m];
			for (k = 0; m < maxHashMapOneArryLength; m++, k++) {
				newListPath[k] = byteTmp[m];
			}
			FlashService.flashListPathName = new String(newListPath);
			hashMapFile.seek(0);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return locateHashArry;
	}

	public static HashMapArry readMarkInHashMap2(RandomAccessFile hashMapFile,
			int hashArryCount) {
		HashMapArry locateHashArry = new HashMapArry();
		try {
			hashMapFile.seek(hashArryCount);
			byte[] byteTmp = new byte[maxHashMapOneArryLength];
			hashMapFile.read(byteTmp);
			locateHashArry.count = byteTmp[0];
			locateHashArry.appHighBit = byteTmp[1];
			locateHashArry.appLowBit = byteTmp[2];
			int i = 3, j = 0, k = 0, m = 0;
			for (i = 3, j = 0; i < maxHashMapOneArryLength; i++, j++) {
				if (byteTmp[i] == '|') {
					m = i;
				}
				locateHashArry.tempString[j] = byteTmp[i];
			}

			byte[] newListPath = new byte[maxHashMapOneArryLength - m];
			for (k = 0; m < maxHashMapOneArryLength; m++, k++) {
				newListPath[k] = byteTmp[m];
			}
			FlashService.flashListPathName = new String(newListPath);

			// locateHashArry.count = hashMapFile.readByte();
			// locateHashArry.appHighBit = hashMapFile.readByte();
			// locateHashArry.appLowBit = hashMapFile.readByte();
			// hashMapFile.read(locateHashArry.tempString);
			hashMapFile.seek(0);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return locateHashArry;
	}

	// write data in HashMap
	public static ArrayList<String> writeMarkInHashMap(String flashName,
			RandomAccessFile hashMapFile, int sizeOfHashMap, int hashArryCount) {

		ArrayList<String> addHashMapFlashName = new ArrayList<String>();

		int addHashMapSize = 0; // return a size of hashmap2
		HashMapArry locateHashArry = new HashMapArry();
		try {
			locateHashArry = readMarkInHashMap(hashMapFile, hashArryCount);
			String realFlashName = flashName.substring(2);
			// Log.d("realFlashName ========", realFlashName);
			byte[] appBitTmp = flashName.getBytes();

			if ((0 == locateHashArry.count) && (0 == locateHashArry.appHighBit)
					&& (0 == locateHashArry.appLowBit)) {

				addHashMapSize = 0;
				locateHashArry.count = 1;
				locateHashArry.appHighBit = appBitTmp[0];
				locateHashArry.appLowBit = appBitTmp[1];
				locateHashArry.tempString = realFlashName.getBytes();

				hashMapFile.seek(hashArryCount * maxHashMapOneArryLength);

				hashMapFile.write(locateHashArry.count);
				hashMapFile.write(locateHashArry.appHighBit);
				hashMapFile.write(locateHashArry.appLowBit);
				hashMapFile.write(locateHashArry.tempString);

			} else if (0 < locateHashArry.count) {
				addHashMapSize = 1;
				locateHashArry.count += 1;

				byte[] appBitByte = new byte[2];
				appBitByte[0] = locateHashArry.appHighBit;
				appBitByte[1] = locateHashArry.appLowBit;
				String appBitString = "";
				appBitString = new String(appBitByte);
				String tmp = new String(locateHashArry.tempString);
				tmp = appBitString + tmp;
				// Log.d("tmp ========", tmp);
				addHashMapFlashName.add(tmp);

				locateHashArry.tempString = realFlashName.getBytes();
				locateHashArry.appHighBit = appBitTmp[0];
				locateHashArry.appLowBit = appBitTmp[1];
				hashMapFile.seek(hashArryCount * maxHashMapOneArryLength);

				hashMapFile.write(locateHashArry.count);
				hashMapFile.write(locateHashArry.appHighBit);
				hashMapFile.write(locateHashArry.appLowBit);
				hashMapFile.write(locateHashArry.tempString);

			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		return addHashMapFlashName;
	}// End of Func

	public static ArrayList<String> writeMarkInHashMap2(String flashName,
			RandomAccessFile hashMapFile, int sizeOfHashMap, int hashArryCount) {
		ArrayList<String> addHashArray = new ArrayList<String>();
		int addHashMapSize = 0; // return a size of hashmap2
		HashMapArry locateHashArry = new HashMapArry();

		try {
			locateHashArry = readMarkInHashMap2(hashMapFile, hashArryCount);
			String realFlashName = flashName.substring(2);

			if ((0 == locateHashArry.count) && (0 == locateHashArry.appHighBit)
					&& (0 == locateHashArry.appLowBit)) {
				addHashMapSize = 0;
				locateHashArry.count = 1;
				locateHashArry.tempString = flashName.getBytes();
				locateHashArry.appHighBit = locateHashArry.tempString[0];
				locateHashArry.appLowBit = locateHashArry.tempString[1];
				locateHashArry.tempString = realFlashName.getBytes();

				hashMapFile.seek(hashArryCount);
				hashMapFile.write(locateHashArry.count);
				hashMapFile.write(locateHashArry.appHighBit);
				hashMapFile.write(locateHashArry.appLowBit);
				hashMapFile.write(locateHashArry.tempString);
			} else if (0 < locateHashArry.count) {
				addHashMapSize = 1;
				locateHashArry.count += 1;

				byte[] appBitByte = new byte[2];
				appBitByte[0] = locateHashArry.appHighBit;
				appBitByte[1] = locateHashArry.appLowBit;
				String appBitString = "";
				appBitString = new String(appBitByte);
				String tmp = new String(locateHashArry.tempString);
				tmp = appBitString + tmp;
				// Log.d("addtmp ========", tmp);
				addHashArray.add(tmp);

				locateHashArry.tempString = flashName.getBytes();
				locateHashArry.appHighBit = locateHashArry.tempString[0];
				locateHashArry.appLowBit = locateHashArry.tempString[1];
				locateHashArry.tempString = realFlashName.getBytes();

				hashMapFile.seek(hashArryCount);

				hashMapFile.write(locateHashArry.count);
				hashMapFile.write(locateHashArry.appHighBit);
				hashMapFile.write(locateHashArry.appLowBit);
				hashMapFile.write(locateHashArry.tempString);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return addHashArray;
	}// End of Func

	// add size in HashMap
	public static int AddHashMapSize(RandomAccessFile hashMapFile,
			int flashCount) {
		int addSizeOfHashMap = 0;
		int addWriteCount = 0;
		int ret = 0;
		ret = ResetHashMapSize(flashCount);
		addSizeOfHashMap = ret * maxHashMapOneArryLength;
		try {
			hashMapFile.seek(hashMapFile.length());
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		for (addWriteCount = 0; addWriteCount < addSizeOfHashMap; addWriteCount++) {
			try {
				hashMapFile.writeByte(0);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return ret;
	} // End of Func
}