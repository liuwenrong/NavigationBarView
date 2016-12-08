package com.android.systemui;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.android.internal.os.BatterySipper;
import com.android.internal.os.BatteryStatsHelper;

import android.R.integer;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ActivityManager.RunningAppProcessInfo;
import android.app.ActivityManagerNative;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.Loader;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.graphics.Bitmap.Config;
import android.graphics.drawable.Drawable;
import android.net.INetworkStatsService;
import android.net.INetworkStatsSession;
import android.net.NetworkStats;
import android.net.NetworkStats.Entry;
import android.net.NetworkStatsHistory;
import android.net.NetworkTemplate;
import android.os.BatteryStats;
import android.os.Build;
import android.os.Bundle;
import android.os.Debug;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.telephony.TelephonyManager;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.util.Log;
import android.widget.TextView;

public class Utils {
	Context mContext;
	private static final int SECONDS_IN_HOUR = 60 * 60;
	private static final int MIN_POWER_THRESHOLD_MILLI_AMP = 5;
	
	public static Utils mInstance;
	private ArrayList<String> lockedPackages;
	private ArrayList<String> runnappPackages;
	private ArrayList<Integer> runnappUids;
	private ArrayList<Integer> runnappPids;
	private long all_pss[];
	private List<BatterySipper> usageList;
	private long oldusedmemory;
	private long nowusedmemory;
	private NetworkStats netstats;
	//zhouxinghua 20150613 add begin
	public static float compressRatio = 0.1f;
	public static float compressRatioMax = 0.16f;
    public static float defalutAlpha = 0.7f;
    public static int mTrafficNotificationID = 1226;
    public static int mEmptyNotificationID = 1227;
    public static  int radius = 15;
    final public static boolean  isNeedLog = true;
    final  String TAG ="Utils";
	private PackageManager pm;
    //zhouxinghua 20150613 add end
	public static Utils getInstance(Context context){
		if (mInstance == null) {
			mInstance = new Utils(context);			
			initTypeface();
		}
		return mInstance;
	}
	
	private Utils(Context context) {
		mContext = context;
		pm = mContext.getPackageManager();
		lockedPackages = new ArrayList<String>();
		runnappPackages = new ArrayList<String>();
		runnappUids = new ArrayList<Integer>();
		runnappPids = new ArrayList<Integer>();
		oldusedmemory = 0;
		nowusedmemory = 0;
	}
	
	public String getTotalMemoryInfo(){
		ActivityManager mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
		mActivityManager.getMemoryInfo(memInfo); 
		long total = memInfo.totalMem/(1024*1024*1024)+1;
        long memorysize = total*1024*1024*1024;//memInfo.totalMem;
        return Formatter.formatFileSize(mContext, memorysize);
	}
	
	public String getUsedMemoryInfo(){
		ActivityManager mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
		mActivityManager.getMemoryInfo(memInfo);	
        return Formatter.formatFileSize(mContext, memInfo.totalMem-memInfo.availMem);
	}
	
	public void setOldUsedMemory() {
		ActivityManager mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
		mActivityManager.getMemoryInfo(memInfo);
		oldusedmemory = memInfo.totalMem-memInfo.availMem;
	}
	
	public void setNowUsedMemory() {
		ActivityManager mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
		mActivityManager.getMemoryInfo(memInfo);
		nowusedmemory = memInfo.totalMem-memInfo.availMem;		
	}
	public String getCleanedMemoryInfo() {
		long degress = oldusedmemory-nowusedmemory;
		if (degress <= 0) {
			Random random = new Random();
			int max = 2*1024*1024;
			degress = random.nextInt(max)%2+1024*1024;
		}
		return Formatter.formatFileSize(mContext, degress);
	}
	public int getMemoryPercent() {
		ActivityManager mActivityManager = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		ActivityManager.MemoryInfo memInfo = new ActivityManager.MemoryInfo();
		mActivityManager.getMemoryInfo(memInfo);
		double percent = ((memInfo.totalMem-memInfo.availMem)/(double)memInfo.totalMem)*100;
		return (int)percent;
	}
	
	
	public void getAllPss() {
		runnappPackages.clear();
		runnappUids.clear();
		runnappPids.clear();
		ActivityManager am = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
		List<RunningAppProcessInfo> apps = am.getRunningAppProcesses();
		if (apps == null) {
			return;
		}
		int[] pids = new int[apps.size()];
		for (int i = 0; i < apps.size(); i++) {
			pids[i] = apps.get(i).pid;
			runnappPackages.add(apps.get(i).processName);
			runnappUids.add(apps.get(i).uid);
			runnappPids.add(apps.get(i).pid);
		}
		try {
			all_pss = ActivityManagerNative.getDefault().getProcessPss(pids);
		} catch (Exception e) {
			Log.v(TAG, "get process pss error : " + e);
		}
	}
	
	public String getPss(String processName) {
        for (int i = 0; i < runnappPackages.size(); i++) {
        	if (runnappPackages.get(i).equals(processName) || 
        			runnappPackages.get(i).indexOf(processName) >= 0) {
        		return Formatter.formatShortFileSize(mContext,all_pss[i]*1024);
        	}
        }
        return "0M";
    }


	public void getAllRunningAppProcessStats() {
		BatteryStatsHelper mStatsHelper = new BatteryStatsHelper(mContext, true);
		Bundle budle = null;
		mStatsHelper.create(budle);
		int mStatsType = BatteryStats.STATS_SINCE_CHARGED;
		BatteryStats stats = mStatsHelper.getStats();
		UserManager mUm = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
		final List<UserHandle> profiles = mUm.getUserProfiles();
		mStatsHelper.refreshStats(BatteryStats.STATS_SINCE_CHARGED, profiles);
		usageList = mStatsHelper.getUsageList();
        final int dischargeAmount = stats != null ? stats.getDischargeAmount(mStatsType) : 0;
        final int numSippers = usageList.size();
//        for (int i = 0; i < numSippers; i++) {
//            final BatterySipper sipper = usageList.get(i);
//            if (sipper == null) {
//            	continue;
//            }
//           // if ((sipper.value * SECONDS_IN_HOUR) < MIN_POWER_THRESHOLD_MILLI_AMP) {
//           //     continue;
//           // }
//            Log.v("hahajlu", "sipper.value="+sipper.value+"total="+mStatsHelper.getTotalPower()+"dischargeAmount="+dischargeAmount);
//            final double percentOfTotal =
//                    ((sipper.value / mStatsHelper.getTotalPower()) * dischargeAmount);
////            if (((int) (percentOfTotal + .5)) < 1) {
////                continue;
////            }
//
//            final double percentOfMax = (sipper.value * 100) / mStatsHelper.getMaxPower();
//            sipper.percent = percentOfTotal;
//            int percent = (int)(percentOfTotal*100);
//        }
	}
	
	public String getStatsPercent(String packages) {
		int index = 0;
		for (index = 0; index < runnappPackages.size(); index++) {
			if (runnappPackages.get(index).equals(packages) || 
        			runnappPackages.get(index).indexOf(packages) >= 0) {
				break;
			}
		}
		if (index == runnappPackages.size()) {
			return "0%";
		}
		int uid = runnappUids.get(index);
		for (int i = 0; i < usageList.size(); i++) {
			BatterySipper sipper = usageList.get(i);
			if (sipper.uidObj == null) {
				continue;
			}
			if (sipper.uidObj.getUid() == uid) {
				int percent = (int)(sipper.percent*100);
				return percent+"%";
			}
		}
		return "0%";
	}
	
	
	public void getProcessNetwork() {
		mStatsService = INetworkStatsService.Stub.asInterface(
                ServiceManager.getService(Context.NETWORK_STATS_SERVICE));
		long start = System.currentTimeMillis() -24*60*60*1000; 
		long end = System.currentTimeMillis();
		NetworkTemplate mTemplate = NetworkTemplate.buildTemplateMobileAll(getActiveSubscriberId(mContext));
		try {
            // wait a few seconds before kicking off
            //Thread.sleep(2 * DateUtils.SECOND_IN_MILLIS);
            mStatsService.forceUpdate();
        } catch (RemoteException e) {
        }
		try {
            mStatsSession = mStatsService.openSession();
            netstats = mStatsSession.getSummaryForAllUid(mTemplate, start, end, true);
            
        } catch (RemoteException e) {
            throw new RuntimeException(e);
        }

		
	}
	
	public String getDatausage(String packages) {
		if (netstats == null) {
			return "0M";
		}
		Entry entry = null;
		int index;
		for (index = 0; index < runnappPackages.size(); index++) {
			if (runnappPackages.get(index).equals(packages) || 
        			runnappPackages.get(index).indexOf(packages) >= 0) {
				break;
			}
		}
		
		if (index == runnappPackages.size()) {
			return "0M";
		}
		int uid = runnappUids.get(index);
		for (int i = 0; i < netstats.size(); i++) {
			entry = netstats.getValues(i, entry);
			if (entry.uid ==  uid) {
				return Formatter.formatShortFileSize(mContext,entry.rxBytes + entry.txBytes);
			}
		}
		
		return "0M";
		
	}
	
//    private final LoaderCallbacks<ChartData> mChartDataCallbacks = new LoaderCallbacks<
//            ChartData>() {
//        @Override
//        public Loader<ChartData> onCreateLoader(int id, Bundle args) {
//            return new ChartDataLoader(mContext, mStatsSession, args);
//        }
//
//        @Override
//        public void onLoadFinished(Loader<ChartData> loader, ChartData data) {
//            mChartData = data;
//            updateDetailData();
//        }
//
//        @Override
//        public void onLoaderReset(Loader<ChartData> loader) {
//            mChartData = null;
//        }
//    };
//    private void updateDetailData() {
//        final long start = 1;
//        final long end = System.currentTimeMillis();
//        final long now = System.currentTimeMillis();
//
//        NetworkStatsHistory.Entry entry = null;
//        /*if (mChartData != null && mChartData.detail != null) {
//            // and finally leave with summary data for label below
//            entry = mChartData.detail.getValues(start, end, now, null);
//        }*/
//        final long totalBytes = entry != null ? entry.rxBytes + entry.txBytes : 0;
//        final String totalPhrase = Formatter.formatFileSize(mContext, totalBytes);
//    }
    
    private static String getActiveSubscriberId(Context context) {
        final TelephonyManager tele = TelephonyManager.from(context);
        final String actualSubscriberId = tele.getSubscriberId();
        return SystemProperties.get(TEST_SUBSCRIBER_PROP, actualSubscriberId);
    }
    
    private static final String TEST_SUBSCRIBER_PROP = "test.subscriberid";
    private static final int LOADER_CHART_DATA = 2;
    private static final int LOADER_SUMMARY = 3;
//    private ChartData mChartData;
    private INetworkStatsSession mStatsSession;
    private INetworkStatsService mStatsService;
    public static class ChartData {
        public NetworkStatsHistory network;

        public NetworkStatsHistory detail;
        public NetworkStatsHistory detailDefault;
        public NetworkStatsHistory detailForeground;
    }
    
    
    public void readLockedTask() {
    	lockedPackages.clear();
    	SharedPreferences sp = mContext.getSharedPreferences("systemuilocked", Context.MODE_PRIVATE);
    	String readpackages = sp.getString("packages", "");
    	if (readpackages.length() > 0) {
    		Log.v("hahajlu", "readpackages = "+readpackages);
    		String[] temp = readpackages.split("#");
    		for (int i = 0; i < temp.length; i++) {
    			lockedPackages.add(temp[i]);
    		}
    	}
    }
    
    public void saveLockedTask() {
    	SharedPreferences sp = mContext.getSharedPreferences("systemuilocked", Context.MODE_PRIVATE);
    	Editor edit = sp.edit();
    	StringBuilder sb = new StringBuilder();
    	for(int i = 0; i < lockedPackages.size(); i++) {
    		sb.append(lockedPackages.get(i));
    		sb.append("#");
    	}
    	Log.v("hahajlu", "putpackages = "+sb.toString());
    	edit.putString("packages", sb.toString());
    	edit.commit();
    }
    
    public boolean isPackageLocked(String packages) {
    	return lockedPackages.contains(packages);
    }
    
    public void addPackagesLocked(String packages) {
    	if (!isPackageLocked(packages)) {
    		lockedPackages.add(packages);
    	}
    }
    
    public void removePackagesLocked(String packages) {
    	lockedPackages.remove(packages);
    }

	public Bitmap blurBitmap(Bitmap bitmap){  
        
        //Let's create an empty bitmap with the same size of the bitmap we want to blur   
        Bitmap outBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Config.ARGB_8888);  
          
        //Instantiate a new Renderscript   
        RenderScript rs = RenderScript.create(mContext);  
          
        //Create an Intrinsic Blur Script using the Renderscript   
        ScriptIntrinsicBlur blurScript = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));  
          
        //Create the Allocations (in/out) with the Renderscript and the in/out bitmaps   
        Allocation allIn = Allocation.createFromBitmap(rs, bitmap);  
        Allocation allOut = Allocation.createFromBitmap(rs, outBitmap);  
          
        //Set the radius of the blur   
        blurScript.setRadius(25.f);  
         
        //Perform the Renderscript   
        blurScript.setInput(allIn);  
        blurScript.forEach(allOut);  
          
        //Copy the final bitmap created by the out Allocation to the outBitmap   
       allOut.copyTo(outBitmap);  
          
        //recycle the original bitmap   
        //bitmap.recycle();  
          
       //After finishing everything, we destroy the Renderscript.   
        rs.destroy();  
         
        return outBitmap;  
         
         
    } 	    

	public Drawable getApplicationIcon(String packagesname) {
		try{
		Context packagecontext = mContext.createPackageContext(packagesname, 0);
		ApplicationInfo ai = packagecontext.getApplicationInfo();
		//PackageManager pm = (PackageManager) mContext.getPackageManager();
		return ai.loadIcon(pm)/*pm.getApplicationIcon(ai)*/;
		}catch(Exception e){
			if (isNeedLog) Log.d("zxh ", "getApplicationIcon Exception:"+e);
		}
		return null;
	}
	
	/*
	 * tangdehua add to select notification 20150630
	 */
	public boolean isQikuNotification(String sbnPackage){
//      return "android".equals(sbnPackage) || "com.android.systemui".equals(sbnPackage);
		if (sbnPackage == null) {
			return false;
		}
		return "android".equals(sbnPackage)
		  		|| sbnPackage.startsWith("com.android.") 
		  		|| sbnPackage.startsWith("com.qiku.") 
		  		|| sbnPackage.startsWith("com.yulong.")
		  		|| sbnPackage.startsWith("com.icoolme.android.weather");
		
	}
	
	
	/*
	 * tangdehua add to select notification 20150702
	 */
	public boolean isSystemNotification(String sbnPackage){
//      return "android".equals(sbnPackage) || "com.android.systemui".equals(sbnPackage);
		if (sbnPackage == null) {
			return false;
		}
		return "android".equals(sbnPackage)
		  		|| sbnPackage.startsWith("com.android.") 
		  		|| sbnPackage.startsWith("com.qiku.") 
		  		|| sbnPackage.startsWith("com.yulong.")
		  		|| "com.mediatek.mtklogger".equals(sbnPackage)
		  		|| "com.tencent.mobileqq".equals(sbnPackage)
		  		|| "com.sina.weibo".equals(sbnPackage)
		  		|| "com.tencent.mm".equals(sbnPackage)
		  		|| "com.immomo.momo".equals(sbnPackage)
		  		|| "com.whatsapp".equals(sbnPackage);
	}
	
	/*
	 * tangdehua add to select notification 20150702
	 */
	public boolean isDefaultApp(String appPackage){
//      return "android".equals(sbnPackage) || "com.android.systemui".equals(sbnPackage);
		if (appPackage == null) {
			return false;
		}
		return "android".equals(appPackage)
		  		|| appPackage.startsWith("com.android.") 
		  		|| appPackage.startsWith("com.qiku.")
		  		|| appPackage.startsWith("com.yulong.")
		  		|| appPackage.startsWith("com.qihoo")
		  		|| appPackage.startsWith("com.coolpad.")
		  	    || "com.newos.android.bbs".equals(appPackage);
//		  		|| "com.tencent.mobileqq".equals(appPackage)
//		  		|| "com.sina.weibo".equals(appPackage)
//		  		|| "com.tencent.mm".equals(appPackage)
//		  		|| "com.immomo.momo".equals(appPackage)
//		  		|| "com.whatsapp".equals(appPackage);
	}
	
	
	private static final Typeface SANS_REGULAR = Typeface.create("sans-serif", Typeface.NORMAL);
	private static final Typeface SANS_LIGHT = Typeface.create("sans-serif-light", Typeface.NORMAL);
	public static final void updateFondType(TextView view,String type){
		if(type.equals("sans-serif")){
			view.setTypeface(SANS_REGULAR);
		}else if(type.equals("sans-serif-light")){
			view.setTypeface(SANS_LIGHT);
		}
	}
	private static final int NOTOSANSCJKSCREGULAR=2;
	private static final int NOTOSANSCJKSCLIGHT=1;
	
	public static final void updateFondType(TextView view,int type){
//		 final  Typeface NotoSansCJKscRegular = Typeface.createFromFile("/system/fonts/NotoSansCJKsc-Regular.otf");
//		 final  Typeface NotoSansCJKscLight = Typeface.createFromFile("/system/fonts/NotoSansCJKsc-Light.otf");
//		if(type==NOTOSANSCJKSCLIGHT){
//			view.setTypeface(NotoSansCJKscLight);
//		}else if(type==NOTOSANSCJKSCLIGHT){
//			view.setTypeface(NotoSansCJKscRegular);
//		}
	}
	
	private static Typeface tf_regular = null;
	private static Typeface tf_light = null; 
	public static final int TYPE_REGULAR = 0;
	public static final int TYPE_LIGHT = 1;
	
	public void setTextViewTypeface(TextView tv, int type) {
		switch(type) {
		 case TYPE_REGULAR:
			 tv.setTypeface(tf_regular);
			 break;
		 case TYPE_LIGHT:
			 tv.setTypeface(tf_light);
			 break;	 			 
		}
		
	}
	
	public Typeface getTypeface(int type) {
		switch(type) {
			case TYPE_REGULAR:
				return tf_regular;
			case TYPE_LIGHT:
				return tf_light;
		}
		return tf_regular;
	}
	private static void initTypeface(){
		if (tf_regular == null || tf_light == null) {
			try {
				tf_regular = Typeface.createFromFile("/system/fonts/NotoSansCJKsc-Regular.otf");	
				tf_light = Typeface.createFromFile("/system/fonts/NotoSansCJKsc-Light.otf");
			} catch (Exception e) {
					// TODO: handle exception
				Log.e("utils", "initTypeface Exception:"+e);
			}
		}
	}
}
