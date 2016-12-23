package com.example.liuwenrong.navigationbarview;

import android.app.Activity;
import android.app.ActivityManager;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.WindowManagerImpl;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.systemui.helper.LogHelper;
import com.android.systemui.statusbar.phone.PhoneStatusBar;


public class MainActivity extends Activity {

	private static final String TAG = "xx---lwr---MainActivity";
	View mCurrentView;
	Button btn;
	Button btnShowBsNav;
	Button btnClose;
	Button btnSwitchDisplay;
	Context mContext, mContextBS;
	public static Handler sHandler;
	Display mDisplay;
	int count = 0;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mStatusBar = (PhoneStatusBar) getLastNonConfigurationInstance();
		mCurrentView = this.getCurrentView();
		mContext = this;
		sHandler = new Handler();  

		DisplayManager mDisplayManager = (DisplayManager)getSystemService(Context.DISPLAY_SERVICE);
		Display[] displays = mDisplayManager.getDisplays();
		LogHelper.logE(TAG, "display.length=="+displays.length);

		if(displays.length < 2){
			mDisplay = displays[0];
			//			mContext = getApplicationContext().createDisplayContext(displays[0]);
		}else{
			mDisplay = displays[1];
			//			mContext = getApplicationContext().createDisplayContext(displays[1]);
		}

		// addView 报错 token null is not for an application context 等于this和getApplicationContext()都报错
		//        mContextBS = mContext.createDisplayContext(mDisplay); // 解决报错: type类型改成 TYPE_TOAST 

		//        getHomeButton().setOnClickListener(onHomeClickListener);
		btn = (Button) findViewById(R.id.btn);
		btnClose = (Button) findViewById(R.id.btn_close);
		btnShowBsNav = (Button) findViewById(R.id.btn_show_bs_nav);
		btnSwitchDisplay = (Button) findViewById(R.id.btn_switch_display);
		btn.setText("oneKeyScreenOff");
		btn.setOnClickListener(onBtnClickListener);
		btnShowBsNav.setOnClickListener(onBtnShowBsNavClickListener);
		btnClose.setOnClickListener(onHomeClickListener);
		btnSwitchDisplay.setOnClickListener(onSwitchDisplayClickListener);

	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		// TODO Auto-generated method stub
//		outState.putInt("count", count);
		super.onSaveInstanceState(outState);
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
//		count = savedInstanceState.getInt("count");
		LogHelper.logE(TAG, "83--count = " + count);
		super.onRestoreInstanceState(savedInstanceState);
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		// TODO Auto-generated method stub
		
		return mStatusBar;
	}
	
	public View getCurrentView() {
		return mCurrentView;
	}

	public View getRecentsButton() {
		View v = mCurrentView.findViewById(R.id.recent_apps);
		return v;
	}

	public View getMenuButton() {
		return mCurrentView.findViewById(R.id.menu);
	}
	public View getYulongMenuButton() {
		return mCurrentView.findViewById(R.id.menu_yulong);
	}

	public View getBackButton() {
		return mCurrentView.findViewById(R.id.back);
	}

	public View getHomeButton() {
		return mCurrentView.findViewById(R.id.home);
	}

	public View getImeSwitchButton() {
		return mCurrentView.findViewById(R.id.ime_switcher);
	}
	public View getHideButton() {
		return mCurrentView.findViewById(R.id.hide);
	}

	private long exitTime = 0;

	View.OnClickListener onHomeClickListener = new View.OnClickListener(){
		@Override
		public void onClick(View v) {
			Intent intent= new Intent(Intent.ACTION_MAIN);

			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //如果是服务里调用，必须加入new task标识

			intent.addCategory(Intent.CATEGORY_HOME);

			startActivity(intent);
			MainActivity.this.finish();
		}
	};
	View.OnClickListener onSwitchDisplayClickListener = new View.OnClickListener(){
		@Override
		public void onClick(View v) {
			mStatusBar.switchDisplay();
		}
	};
	private RuntimeException andLog(String msg, Throwable t) {
		LogHelper.logE(TAG, msg, t);
		throw new RuntimeException(msg, t);
	}
	PhoneStatusBar mStatusBar;
	View.OnClickListener onBtnShowBsNavClickListener = new View.OnClickListener(){
		@Override
		public void onClick(View v){

			WindowManager.LayoutParams mWindowParams = new WindowManager.LayoutParams();
			mWindowParams.type = WindowManager.LayoutParams.TYPE_TOAST;
			// 以下属性在Layout Params中常见重力、坐标，宽高  
			mWindowParams.gravity = Gravity.LEFT | Gravity. TOP;  
			mWindowParams.x = 0;  
			mWindowParams.y = 1000;

			mWindowParams.width = WindowManager.LayoutParams.MATCH_PARENT;
			mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
			//    	    mWindowParams.height = 150;
			TextView tv = new TextView(mContext);
			tv.setText("哈哈哈哈HelloWorld");
			tv.setTextSize(24*3);
			tv.setLayoutParams(mWindowParams);
			tv.setOnClickListener(new OnClickListener(){
				public void onClick(View view){
					Toast.makeText(getApplicationContext(), "哈哈哈", Toast.LENGTH_SHORT).show();
					//点击事件 覆盖整个屏幕 除了PhoneStatusBar和NavigationBar
				}
			});

			WindowManager mWindowManager = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
			LogHelper.logE(TAG, "mWindowManager=="+mWindowManager.toString());



			//    		hideNav();
			
				showNav();
			


			//    		Toast.makeText(mContext, "显示背屏的BSNavigationBar", Toast.LENGTH_SHORT).show();

			btnShowBsNav.setClickable(false);
			btnShowBsNav.setEnabled(false);
			//    		mWindowManager.addView(tv, mWindowParams);


			//            WindowManager wmBS = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
			//            LogHelper.logE(TAG, "195--wmBS = " + wmBS.toString());
			//            wmBS.addView(tv, mWindowParams);

			WindowManagerImpl wmi, wmiBS;
			//          wmi = new WindowManagerImpl(displays[1]);//BadTokenException: Unable to add window -- token null is not for an application
			wmi = (WindowManagerImpl)getSystemService(Context.WINDOW_SERVICE);// 可以addView 但是不能选择屏幕
			//          wmi.addView(tv, mWindowParams); 
			LogHelper.logE(TAG, "wmi = "+wmi.toString());
			//          wmiBS = wmi.createPresentationWindowManager(displays[0]);// 可以选择屏幕
			//          LogHelper.logE(TAG, "wmi="+ wmi.toString() + "wmiBS==="+wmiBS.toString());
			//          BSNavigationBarView bsNav = new BSNavigationBarView(mContext, null);
			//          BSNavigationBarView mNavigationBarView =  (BSNavigationBarView) View.inflate(mContext, R.layout.bs_navigation_bar, null);
			//          wmiBS.addView(mNavigationBarView, mWindowParams);
			//          wmiBS.addView(mNavigationBarView, getNavigationBarLayoutParams());
			//          StatusBarManager sbm;
			//          wmiBS.addView(tv, mWindowParams);
			// Context = this 改成 getParent(); 并没有用 new TextView 出错

			//          WindowManagerGlobal wmg = WindowManagerGlobal.getInstance();
			//          wmg.addView(tv, mWindowParams, mDisplay, getWindow());

			//  		ViewRootImpl vri = new ViewRootImpl(mContext, mDisplay);
			//  		vri.setView(tv, mWindowParams, null);// BadTokenException
			//  		ViewRootImpl vri = new ViewRootImpl(mContext, mDisplay);
			//  		vri.setView(mStatusBarView, mLp, null);
		}

	};

	private void showNav() {
		if(mStatusBar == null){
			final String clsName = mContext.getString(R.string.config_statusBarComponent);

			if (clsName == null || clsName.length() == 0) {
				//            	Log.e("xxx---95", "No status bar component configured");
				throw andLog("No status bar component configured", null);

			}

			Class<?> cls = null;

			try {

				cls = mContext.getClassLoader().loadClass(clsName);

			}catch (Throwable t) {
				throw andLog("Error loading status bar component: " + clsName, t);
			}
			try {

				mStatusBar = (PhoneStatusBar) cls.newInstance();

			} catch (Throwable t) {
				throw andLog("Error creating status bar component: " + clsName, t);
			}
			mStatusBar.mContext = mContext;
			//这里是调用的BaseStatusBar的start()方法，当有子类继承时，直接调用．

			LogHelper.logE(TAG, "mContext="+mContext.toString() + "  getApplicationContext" + MainActivity.this.getApplicationContext());

			mStatusBar.start();
//			mStatusBar.registerReceiver();
		}
		//            mStatusBar.mComponents = mComponents;
//		goNotificationActivity();

	}
	//跳转 发送通知的Activity
	private void goNotificationActivity(){
		startNotificationListenerService();
		Intent intent = new Intent(this, NotificationActivity.class);
		startActivity(intent);
	}
	//开启监听通知的服务
		private void startNotificationListenerService(){
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
				Intent intent = new Intent(this, MyNotificationListenerService.class);
//				this.bindService(intent, conn, Context.BIND_AUTO_CREATE);
				startService(intent);
				
			}else{
				Toast.makeText(getApplicationContext(), "当前手机系统不支持此功能", Toast.LENGTH_SHORT).show();
			}
		}
		
		@Override
		public void onDestroy(){
			LogHelper.logE(TAG, "291-----onDestroy");
			stopNotificationListenerService();
			super.onDestroy();
		}
		private void stopNotificationListenerService(){
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2){
				Intent intent = new Intent(this, MyNotificationListenerService.class);
				stopService(intent);
			}else{
				Toast.makeText(getApplicationContext(), "当前手机系统不支持此功能", Toast.LENGTH_SHORT).show();
			}
		}
		
	private WindowManager.LayoutParams getNavigationBarLayoutParams() {
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT,
				WindowManager.LayoutParams.TYPE_NAVIGATION_BAR,
				0
				| WindowManager.LayoutParams.FLAG_TOUCHABLE_WHEN_WAKING
				| WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
				| WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
				| WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
				| WindowManager.LayoutParams.FLAG_SPLIT_TOUCH,
				PixelFormat.TRANSLUCENT);
		// this will allow the navbar to run in an overlay on devices that support this
		if (ActivityManager.isHighEndGfx()) {
			lp.flags |= WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED;
		}

		lp.setTitle("NavigationBar");
		lp.windowAnimations = 0;
		return lp;
	}

	View.OnClickListener onBtnClickListener = new View.OnClickListener(){
		@Override
		public void onClick(View v) {

			//        	PhoneWindowManager pwm = new PhoneWindowManager();
			//        	pwm.showRecentApps();
			//        	InputMethodManager imm = (InputMethodManager) getApplicationContext().getSystemService(Context.INPUT_METHOD_SERVICE);
//			hideNav();
			oneKeyScreenOff();

		}
	};
	
	public void oneKeyScreenOff(){
		DevicePolicyManager devicePolicyManager = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        ComponentName componentName = new ComponentName(this, DeviceAdminReceiver.class);

        if(!devicePolicyManager.isAdminActive(componentName)){
            Intent intent =
                    new  Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, componentName);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "----这是一键锁屏激活界面-----");
            startActivityForResult(intent, 0);
        }
        devicePolicyManager.lockNow();
	}

	public void hideNav(){
		sHandler.post(mHideRunnable); // hide the navigation bar  

		final View decorView = getWindow().getDecorView();  
		decorView.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener()  
		{  
			@Override  
			public void onSystemUiVisibilityChange(int visibility)  
			{  
				sHandler.post(mHideRunnable); // hide the navigation bar  
			}  
		});     

	}

	Runnable mHideRunnable = new Runnable() {
		@Override  
		public void run() {  
			int flags;    
			int curApiVersion = android.os.Build.VERSION.SDK_INT;  
			// This work only for android 4.4+  
			if(curApiVersion >= Build.VERSION_CODES.KITKAT){  
				// This work only for android 4.4+  
				// hide navigation bar permanently in android activity  
				// touch the screen, the navigation bar will not show  
				flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION  
						| View.SYSTEM_UI_FLAG_IMMERSIVE  
						| View.SYSTEM_UI_FLAG_FULLSCREEN;  

			}else{  
				// touch the screen, the navigation bar will show  
				flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
			}  

			// must be executed in main thread :)  
			getWindow().getDecorView().setSystemUiVisibility(flags);  
		}  
	};    

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {

//		Toast.makeText(getApplicationContext(), "点击了键----> KeyCode=="+keyCode, Toast.LENGTH_SHORT).show();

		if(keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN){
			if((System.currentTimeMillis()-exitTime) > 2000){
				Toast.makeText(getApplicationContext(), "再按一次退出程序", Toast.LENGTH_SHORT).show();
				exitTime = System.currentTimeMillis();
			} else {
				finish();
//				System.exit(0);
			}
			return true;
		}else if(keyCode == KeyEvent.KEYCODE_HOME && event.getAction() == KeyEvent.ACTION_DOWN){
			Toast.makeText(getApplicationContext(), "点击了Home键", Toast.LENGTH_SHORT).show();

			Intent intent= new Intent(Intent.ACTION_MAIN); 

			intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK); //如果是服务里调用，必须加入new task标识   

			intent.addCategory(Intent.CATEGORY_HOME);

			startActivity(intent);

		}else if(keyCode == KeyEvent.KEYCODE_MENU && event.getAction() == KeyEvent.ACTION_DOWN){
			Toast.makeText(getApplicationContext(), "点击了Menu键", Toast.LENGTH_SHORT).show();

		}

		return super.onKeyDown(keyCode, event);
	}



}
