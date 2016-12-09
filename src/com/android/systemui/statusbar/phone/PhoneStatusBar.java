package com.android.systemui.statusbar.phone;

import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.IActivityManager;
import android.app.NotificationManager;
import android.app.StatusBarManager;
import android.content.Context;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.hardware.display.DisplayManager;
import android.hardware.input.InputManager;
import android.media.session.MediaSessionManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.SystemClock;
import android.service.notification.StatusBarNotification;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.view.WindowManagerImpl;
import android.view.accessibility.AccessibilityEvent;
import android.widget.TextView;

import com.android.internal.statusbar.StatusBarIcon;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.statusbar.BaseStatusBar;
import com.android.systemui.statusbar.StatusBarState;
import com.android.systemui.statusbar.policy.KeyButtonView;
import com.example.liuwenrong.navigationbarview.BSNavigationBarView;
import com.example.liuwenrong.navigationbarview.R;

public class PhoneStatusBar extends BaseStatusBar {
	/** Allow some time inbetween the long press for back and recents. */
	private static final int LOCK_TO_APP_GESTURE_TOLERENCE = 200;
	public static String TAG = "xxxx--------PhoneStatusBar";
	private boolean isShowRecents = true;
	private boolean mScrimSrcModeEnabled;
	public boolean mExpandedVisible;
	int mDisabled = 0;
	PhoneStatusBarView mStatusBarView;
	private MediaSessionManager mMediaSessionManager;
	//    PhoneStatusBarPolicy mIconPolicy;
	//    private UnlockMethodCache mUnlockMethodCache;
	DisplayMetrics mDisplayMetrics = new DisplayMetrics();
	Point mCurrentDisplaySize = new Point();
	public static final boolean DEBUG_GESTURES = false;
	// XXX: gesture research
	//    private final GestureRecorder mGestureRec = DEBUG_GESTURES
	//        ? new GestureRecorder("/sdcard/statusbar_gestures.dat")
	//        : null;

	Handler mHandler = new Handler(){
		public void handleMessage(android.os.Message msg) {
			switch(msg.what){
			case 0:
				wmiBS.addView(mNavigationBarView, getBSNavigationBarLayoutParams());
				break;
			case 1:
				wmi.addView(mNavigationBarView, getNavigationBarLayoutParams()); 
				break;
			}
		};
	};
	
	WindowManagerImpl outerWindowManager;
	// Type 为 NavigationBar是 removeView()后 在 add 报错 试着延时一秒加
	public void switchDisplay(){
		if(navIsAddBS()){
			wmiBS.removeViewImmediate(mNavigationBarView);
			wmi.addView(mNavigationBarView, getNavigationBarLayoutParams());
			navIsAddBS = false;
		}else{
			wmi.removeViewImmediate(mNavigationBarView);
			wmiBS.addView(mNavigationBarView, getBSNavigationBarLayoutParams());
			navIsAddBS = true;
		}
	}
	private boolean navIsAddBS = false;
	public boolean navIsAddBS(){
		return navIsAddBS;
	}
	Display[] displays;
	@Override
	public void start() {
		LogHelper.logE(TAG, "start方法开始执行");
		
		DisplayManager mDisplayManager = (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);
		displays = mDisplayManager.getDisplays();
		LogHelper.logE(TAG, "display.length=="+displays.length);
		if(displays.length < 2){
			mDisplay = displays[0];
		}else{
			mDisplay = displays[1];
		}
		mDisplay = displays[count];
		//        mDisplay = ((WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE))
		//        		.getDefaultDisplay();
//		wmi = new WindowManagerImpl(mDisplay);
		outerWindowManager = (WindowManagerImpl) mContext.getSystemService(Context.WINDOW_SERVICE);
		if(wmi == null){
			wmi = outerWindowManager.createPresentationWindowManager(displays[0]);
		}
		if(wmiBS == null){
			wmiBS = outerWindowManager.createPresentationWindowManager(displays[1]);
		}

		LogHelper.logE(TAG, "outerWindowManager = "+outerWindowManager.toString());
		LogHelper.logE(TAG, "wmi = "+wmi.toString());
		updateDisplaySize();
		mScrimSrcModeEnabled = mContext.getResources().getBoolean(
				R.bool.config_status_bar_scrim_behind_use_src);
			super.start(); // calls createAndAddWindows()

		mMediaSessionManager
		= (MediaSessionManager) mContext.getSystemService(Context.MEDIA_SESSION_SERVICE);
		// TODO: use MediaSessionManager.SessionListener to hook us up to future updates
		// in session state

		addNavigationBar();
		

		//        QuickSettingLauncher.getInstance(mContext).start();
		//        QuickSettingLauncher.getInstance(mContext).setStatusBar(this);

		// Lastly, call to the icon policy to install/update all the icons.
		//        mIconPolicy = new PhoneStatusBarPolicy(mContext, mCastController, mUserInfoController);
		//        if (mIconPolicy != null) {
		//        	mIconPolicy.setCurrentUserSetup(mUserSetup);
		//        }
		//        mSettingsObserver.onChange(false); // set up

		//        mHeadsUpObserver.onChange(true); // set up
		//        if (ENABLE_HEADS_UP) {
		//            mContext.getContentResolver().registerContentObserver(
		//                    Settings.Global.getUriFor(Settings.Global.HEADS_UP_NOTIFICATIONS_ENABLED), true,
		//                    mHeadsUpObserver);
		//            mContext.getContentResolver().registerContentObserver(
		//                    Settings.Global.getUriFor(SETTING_HEADS_UP_TICKER), true,
		//                    mHeadsUpObserver);
		//        }
		//        mUnlockMethodCache = UnlockMethodCache.getInstance(mContext);
		//        startKeyguard();

		//        mDozeServiceHost = new DozeServiceHost();
		//        putComponent(DozeHost.class, mDozeServiceHost);
		//        putComponent(PhoneStatusBar.class, this);

		//        setControllerUsers();

		notifyUserAboutHiddenNotifications();

		//        mScreenPinningRequest = new ScreenPinningRequest(mContext);
		LogHelper.logE(TAG, "start方法结束");
	}
	public WindowManagerImpl wmi, wmiBS;
	int count = 0;
	private void addNavigationBar() {
		LogHelper.logE(TAG, "addBSNavigationBar: about to add " + mNavigationBarView);
		if (mNavigationBarView == null) return;

		prepareNavigationBarView();

		WindowManager.LayoutParams mWindowParams = new WindowManager.LayoutParams();
		// 以下属性在Layout Params中常见重力、坐标，宽高  
		mWindowParams.gravity = Gravity.LEFT | Gravity. TOP;  
		mWindowParams.x = 0;  
		mWindowParams.y = 1700;

		mWindowParams.width = WindowManager.LayoutParams.MATCH_PARENT;
		//	    mWindowParams.height = WindowManager.LayoutParams.WRAP_CONTENT;
		mWindowParams.height = 150;

		TextView tv = new TextView(mContext);
		tv.setText("哈哈哈");
		mNavigationBarView.setClickable(true);
		mNavigationBarView.setEnabled(true);
		mNavigationBarView.setOnClickListener(new OnClickListener(){
			public void onClick(View view){
				LogHelper.logE(TAG, "Click NavigationBar");
			}
		});
//		mWindowManager.addView(mNavigationBarView, getNavigationBarLayoutParams()); //1.
		// 干掉系统SystemUI的时候可以用,否则报错 another window of this type already exists.

		DisplayManager mDisplayManager = (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);
		Display[] displays = mDisplayManager.getDisplays();
		if(displays.length < 2){
			mDisplay = displays[0];
		}else{
			mDisplay = displays[1];
		}
		
//		final WindowManagerImpl outerWindowManager =
//				(WindowManagerImpl) mContext.getSystemService(Context.WINDOW_SERVICE);
//		wmiBS = outerWindowManager.createPresentationWindowManager(displays[count]);
		wmi.addView(mNavigationBarView, getNavigationBarLayoutParams());
		
		// another window of this type already exists  把Params中的type改了就不会报错
		//		wmiBS.addView(mNavigationBarView, mWindowParams); // yes But context为Application时报错
		//		wmiBS.addView(tv, mWindowParams); // 可以添加  但会把下层点击事件覆盖掉.
//				mWindowManager.addView(tv, mWindowParams); // yes

		//        mWindowManager.addView(mNavigationBarView, getNavigationBarLayoutParams());
		//        final WindowManagerImpl outerWindowManager =
		//                (WindowManagerImpl) mContext.getSystemService(Context.WINDOW_SERVICE);
		//		wmi.addView(mNavigationBarView, getNavigationBarLayoutParams());
		//        ViewRootImpl vri = new ViewRootImpl(mContext, mDisplay);
		//		vri.setView(mNavigationBarView, getNavigationBarLayoutParams(), null);
//		LogHelper.logE(TAG, "mWindowManager=="+mWindowManager.toString());
		//		mWindowManager.addView(mNavigationBarView, mWindowParams);
	}
	public boolean isQsExpanded() {
		//        return mNotificationPanel.isQsExpanded();
		return true;
	}
	// called by makeStatusbar and also by PhoneStatusBarView
	void updateDisplaySize() {
		mDisplay.getMetrics(mDisplayMetrics);
		mDisplay.getSize(mCurrentDisplaySize);
		LogHelper.logE(TAG, "display  == " + String.format("%dx%d", mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels));
		//        if (DEBUG_GESTURES) {
		//            mGestureRec.tag("display",
		//                    String.format("%dx%d", mDisplayMetrics.widthPixels, mDisplayMetrics.heightPixels));
		//        }
	}

	/**
	 * The {@link StatusBarState} of the status bar.
	 */
	protected int mState;
	/**
	 * @return The {@link StatusBarState} the status bar is in.
	 */
	public int getBarState() {
		return mState;
	}
	public static String viewInfo(View v) {
		return "[(" + v.getLeft() + "," + v.getTop() + ")(" + v.getRight() + "," + v.getBottom()
				+ ") " + v.getWidth() + "x" + v.getHeight() + "]";
	}
	//重新配置，复位
	public void repositionNavigationBar() {
		if (mNavigationBarView == null || !mNavigationBarView.isAttachedToWindow()) return;

		prepareNavigationBarView();

		mWindowManager.updateViewLayout(mNavigationBarView, getNavigationBarLayoutParams());
	}
	private void prepareNavigationBarView() {
		mNavigationBarView.reorient();

		mNavigationBarView.getRecentsButton().setOnClickListener(mRecentsClickListener);
		mNavigationBarView.getRecentsButton().setOnTouchListener(mRecentsPreloadOnTouchListener);
		mNavigationBarView.getRecentsButton().setLongClickable(true);
		mNavigationBarView.getRecentsButton().setOnLongClickListener(mLongPressBackRecentsListener);
		mNavigationBarView.getBackButton().setLongClickable(true);
		mNavigationBarView.getBackButton().setOnLongClickListener(mLongPressBackRecentsListener);
		mNavigationBarView.getHomeButton().setOnTouchListener(mHomeActionListener);

		//        updateSearchPanel();
	}
	private long mLastLockToAppLongPress;
	private View.OnLongClickListener mLongPressBackRecentsListener =
			new View.OnLongClickListener() {
		@Override
		public boolean onLongClick(View v) {
			LogHelper.logE(TAG, "长按了返回键");
			handleLongPressBackRecents(v);
			return true;
		}
	};
	private View.OnClickListener mRecentsClickListener = new View.OnClickListener() {
		public void onClick(View v) {
			LogHelper.logE(TAG, "点击了最近apps键");
			if(isShowRecents){
				awakenDreams();
				toggleRecentApps();
			}
			isShowRecents = true;
		}
	};
	View.OnTouchListener mHomeActionListener = new View.OnTouchListener() {
		public boolean onTouch(View v, MotionEvent event) {
			LogHelper.logE(TAG, "home key event = " + event);
			switch(event.getAction()) {
			case MotionEvent.ACTION_DOWN:
				if (!shouldDisableNavbarGestures()) {
					//                    mHandler.removeCallbacks(mShowSearchPanel);
					//                    mHandler.postDelayed(mShowSearchPanel, mShowSearchHoldoff);
				}
				break;

			case MotionEvent.ACTION_UP:
			case MotionEvent.ACTION_CANCEL:
				//                mHandler.removeCallbacks(mShowSearchPanel);
				awakenDreams();
				break;
			}
			return false;
		}
	};
	private void awakenDreams() {
		if (mDreamManager != null) {
			try {
				mDreamManager.awaken();
			} catch (RemoteException e) {
				// fine, stay asleep then
			}
		}
	}
	/**
	 * This handles long-press of both back and recents.  They are
	 * handled together to capture them both being long-pressed
	 * at the same time to exit screen pinning (lock task).
	 *
	 * When accessibility mode is on, only a long-press from recents
	 * is required to exit.
	 *
	 * In all other circumstances we try to pass through long-press events
	 * for Back, so that apps can still use it.  Which can be from two things.
	 * 1) Not currently in screen pinning (lock task).
	 * 2) Back is long-pressed without recents.
	 */
	private void handleLongPressBackRecents(View v) {
		try {
			boolean sendBackLongPress = false;
			IActivityManager activityManager = ActivityManagerNative.getDefault();
			boolean isAccessiblityEnabled = mAccessibilityManager.isEnabled();
			if (activityManager.isInLockTaskMode() && !isAccessiblityEnabled) {
				long time = System.currentTimeMillis();
				// If we recently long-pressed the other button then they were
				// long-pressed 'together'
				if ((time - mLastLockToAppLongPress) < LOCK_TO_APP_GESTURE_TOLERENCE) {
					activityManager.stopLockTaskModeOnCurrent();
				} else if ((v.getId() == R.id.back)
						&& !mNavigationBarView.getRecentsButton().isPressed()) {
					// If we aren't pressing recents right now then they presses
					// won't be together, so send the standard long-press action.
					sendBackLongPress = true;
				}
				mLastLockToAppLongPress = time;
			} else {
				// If this is back still need to handle sending the long-press event.
				if (v.getId() == R.id.back) {
					sendBackLongPress = true;
				} else if (isAccessiblityEnabled && activityManager.isInLockTaskMode()) {
					// When in accessibility mode a long press that is recents (not back)
					// should stop lock task.
					activityManager.stopLockTaskModeOnCurrent();
				}
			}
			if (sendBackLongPress) {
				KeyButtonView keyButtonView = (KeyButtonView) v;
				keyButtonView.sendEvent(KeyEvent.ACTION_DOWN, KeyEvent.FLAG_LONG_PRESS);
				keyButtonView.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_LONG_CLICKED);
			}
			if(v.getId() == R.id.recent_apps){
				isShowRecents = false;
				sendEvent(KeyEvent.ACTION_DOWN, 0);
				sendEvent(KeyEvent.ACTION_UP, 0);

			}
		} catch (RemoteException e) {
			Log.d(TAG, "Unable to reach activity manager", e);
		}
	}
	void sendEvent(int action, int flags) {
		final int repeatCount = (flags & KeyEvent.FLAG_LONG_PRESS) != 0 ? 1 : 0;
		final KeyEvent ev = new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), action, 82, repeatCount,
				0, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
				flags | KeyEvent.FLAG_FROM_SYSTEM | KeyEvent.FLAG_VIRTUAL_HARD_KEY,
				InputDevice.SOURCE_KEYBOARD);
		InputManager.getInstance().injectInputEvent(ev,
				InputManager.INJECT_INPUT_EVENT_MODE_ASYNC);
	}
	private WindowManager.LayoutParams getNavigationBarLayoutParams() {
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
				LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT,
//				0,
//				WindowManager.LayoutParams.TYPE_TOAST,
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

		lp.setTitle("BSNavigationBar");
		lp.windowAnimations = 0;
		LogHelper.logE(TAG,"lp = " + lp.toString());
		return lp;
	}
	private WindowManager.LayoutParams getBSNavigationBarLayoutParams() {
		WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
				LayoutParams.WRAP_CONTENT, 60*3,
				WindowManager.LayoutParams.TYPE_TOAST,
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
		lp.gravity = Gravity.BOTTOM;
		lp.setTitle("BSNavigationBar");
		lp.windowAnimations = 0;
		LogHelper.logE(TAG,"lp = " + lp.toString());
		return lp;
	}

	@Override
	public void addIcon(String slot, int index, int viewIndex,
			StatusBarIcon icon) {
		// TODO Auto-generated method stub

	}

	@Override
	public void updateIcon(String slot, int index, int viewIndex,
			StatusBarIcon old, StatusBarIcon icon) {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeIcon(String slot, int index, int viewIndex) {
		// TODO Auto-generated method stub

	}

	@Override
	public void disable(int state, boolean animate) {
		// TODO Auto-generated method stub

	}

	@Override
	public void disable(int state1, int state2, boolean animate) {
		// TODO Auto-generated method stub

	}

	@Override
	public void animateExpandNotificationsPanel() {
		// TODO Auto-generated method stub

	}

	@Override
	public void animateCollapsePanels(int flags) {
		// TODO Auto-generated method stub

	}

	@Override
	public void animateExpandSettingsPanel() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setSystemUiVisibility(int vis, int mask) {
		// TODO Auto-generated method stub

	}

	@Override
	public void topAppWindowChanged(boolean visible) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setImeWindowStatus(IBinder token, int vis, int backDisposition,
			boolean showImeSwitcher) {
		// TODO Auto-generated method stub

	}

	@Override
	public void showRecentApps(boolean triggeredFromAltTab) {
		// TODO Auto-generated method stub

	}

	@Override
	public void hideRecentApps(boolean triggeredFromAltTab,
			boolean triggeredFromHomeKey) {
		// TODO Auto-generated method stub

	}

	@Override
	public void toggleRecentApps() {

	}

	@Override
	public void preloadRecentApps() {
		// TODO Auto-generated method stub

	}

	@Override
	public void cancelPreloadRecentApps() {
		// TODO Auto-generated method stub

	}

	@Override
	public void showSearchPanel() {
		// TODO Auto-generated method stub

	}

	@Override
	public void hideSearchPanel() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setWindowState(int window, int state) {
		// TODO Auto-generated method stub

	}

	@Override
	public void buzzBeepBlinked() {
		// TODO Auto-generated method stub

	}

	@Override
	public void notificationLightOff() {
		// TODO Auto-generated method stub

	}

	@Override
	public void notificationLightPulse(int argb, int onMillis, int offMillis) {
		// TODO Auto-generated method stub

	}

	@Override
	public void showScreenPinningRequest() {
		// TODO Auto-generated method stub

	}

	@Override
	public void appTransitionPending() {
		// TODO Auto-generated method stub

	}

	@Override
	public void appTransitionCancelled() {
		// TODO Auto-generated method stub

	}

	@Override
	public void appTransitionStarting(long startTime, long duration) {
		// TODO Auto-generated method stub

	}

	@Override
	public void showAssistDisclosure() {
		// TODO Auto-generated method stub

	}

	@Override
	public void startAssist(Bundle args) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void createAndAddWindows() {
		addStatusBarWindow();

	}
	private StatusBarWindowManager mStatusBarWindowManager;
	private void addStatusBarWindow() {
		makeStatusBarView();
		mStatusBarWindowManager = new StatusBarWindowManager(mContext);
		LogHelper.logE(TAG, "add StatusBarWindow");
		mStatusBarWindowManager.add(mStatusBarWindow, getStatusBarHeight(), wmi);
	}
	public boolean panelsEnabled() {
		return (mDisabled & StatusBarManager.DISABLE_EXPAND) == 0;
	}
	int mNaturalBarHeight = -1;
	public int getStatusBarHeight() {
		if (mNaturalBarHeight < 0) {
			final Resources res = mContext.getResources();
			mNaturalBarHeight =
					res.getDimensionPixelSize(com.android.internal.R.dimen.status_bar_height);
		}
		LogHelper.logE(TAG, "..................................mNaturalBarHeight="+mNaturalBarHeight); 
		return mNaturalBarHeight;
	}
	boolean isMSim() {
		//修改为true，永远使用多信号显示方式
		return true;//(TelephonyManager.getDefault().getPhoneCount() > 1);
	}
	protected PhoneStatusBarView makeStatusBarView() {
		final Context context = mContext;
		mNotificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);//

		YulongConfig config = YulongConfig.createDefault(mContext);
		config.init();
		//        Utilities.readLauncherThemeBitmap(mContext);
		getScreenValue(mContext);//
		Resources res = context.getResources();
		//mScreenWidth = (float) context.getResources().getDisplayMetrics().widthPixels;
		//        for(int i=0; i<chatPkg.length; i++){
		//			mPackageNotificationMap.put(chatPkg[i], 0);
		//		}
		updateDisplaySize(); // populates mDisplayMetrics
		//        updateResources();

		//        mIconSize = res.getDimensionPixelSize(R.dimen.status_bar_icon_size);
		//		STATUS_BAR_BLACK = mContext.getResources().getColor(R.color.status_bar_icon_color_black, null);
		//		STATUS_BAR_WHITE = mContext.getResources().getColor(R.color.status_bar_icon_color_white, null);
		//        mKeyguarSystemIconColor = STATUS_BAR_WHITE; 

		if (isMSim()) {
			mStatusBarWindow = (StatusBarWindowView) View.inflate(context,
					R.layout.msim_super_status_bar, null);
		}
		mStatusBarWindow.mService = this;
		mStatusBarWindow.setOnTouchListener(new View.OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				//                checkUserAutohide(v, event);
				if (event.getAction() == MotionEvent.ACTION_DOWN) {
					if (mExpandedVisible) {
						//                        animateCollapsePanels();
					}
				}
				return mStatusBarWindow.onTouchEvent(event);
			}});

		//        if (isMSim()) {
		//            mStatusBarView = (PhoneStatusBarView) mStatusBarWindow.findViewById(
		//                    R.id.msim_status_bar);
		//        } else {
		//            mStatusBarView = (PhoneStatusBarView) mStatusBarWindow.findViewById(R.id.status_bar);
		//        }
		//        mStatusBarView.setBar(this);

		//        PanelHolder holder;
		//        if (isMSim()) {
		//            holder = (PanelHolder) mStatusBarWindow.findViewById(R.id.msim_panel_holder);
		//        } else {
		//            holder = (PanelHolder) mStatusBarWindow.findViewById(R.id.panel_holder);
		//        }
		//        mStatusBarView.setPanelHolder(holder);

		//        mNotificationPanel = (NotificationPanelView) mStatusBarWindow.findViewById(
		//                R.id.notification_panel);
		//        mNotificationPanel.setStatusBar(this);
		//        mNotificationPanel.setStatusBarKeyguardViewManager(mStatusBarKeyguardViewManager);

		Boolean isHighEndGfx = !ActivityManager.isHighEndGfx();
		//LogHelper.sd(TAG,"bool="+bool);
		//        if (isHighEndGfx) {
		//            mStatusBarWindow.setBackground(null);
		//			mNotificationPanel.setBackground(new FastColorDrawable(context.getResources().getColor(
		//					R.color.notification_panel_solid_background, null)));
		//        }
		//        if (ENABLE_HEADS_UP) {
		//            mHeadsUpNotificationView =
		//                    (HeadsUpNotificationView) View.inflate(context, R.layout.heads_up, null);
		//            mHeadsUpNotificationView.setVisibility(View.GONE);
		//            mHeadsUpNotificationView.setBar(this);
		//        }
		//        if (MULTIUSER_DEBUG) {
		//            mNotificationPanelDebugText = (TextView) mNotificationPanel.findViewById(
		//                    R.id.header_debug_info);
		//            mNotificationPanelDebugText.setVisibility(View.VISIBLE);
		//        }
		//
		//        updateShowSearchHoldoff();

		try {
			boolean showNav = mWindowManagerService.hasNavigationBar();
			if (DEBUG) Log.v(TAG, "hasNavigationBar=" + showNav);
			if (showNav) {
				mNavigationBarView =  (BSNavigationBarView) View.inflate(context, R.layout.bs_navigation_bar, null);

				mNavigationBarView.setDisabledFlags(mDisabled);
				mNavigationBarView.setBar(this);
				mNavigationBarView.setOnVerticalChangedListener(
						new BSNavigationBarView.OnVerticalChangedListener() {
							@Override
							public void onVerticalChanged(boolean isVertical) {
								//                        if (mSearchPanelView != null) {
								//                            mSearchPanelView.setHorizontal(isVertical);
								//                        }
								//                        mNotificationPanel.setQsScrimEnabled(!isVertical);
							}
						});
				mNavigationBarView.setOnTouchListener(new View.OnTouchListener() {
					@Override
					public boolean onTouch(View v, MotionEvent event) {
						checkUserAutohide(v, event);
						return false;
					}});
			}
		} catch (RemoteException ex) {
			// no window manager? good luck with that
		}

		//        mAssistManager = new AssistManager(this, context);

		// figure out which pixel-format to use for the status bar.
		//        mPixelFormat = PixelFormat.OPAQUE;
		//
		//        mSystemIconArea = (LinearLayout) mStatusBarView.findViewById(R.id.system_icon_area);
		//        mSystemIcons = (LinearLayout) mStatusBarView.findViewById(R.id.system_icons);
		//        mStatusIcons = (LinearLayout)mStatusBarView.findViewById(R.id.statusIcons);
		//        mBatteryView = (BatteryViewGroup)mStatusBarView.findViewById(R.id.battery_field);
		////        mBatteryView = (BatteryMeterView)mStatusBarView.findViewById(R.id.battery);
		//        mNotificationIconArea = mStatusBarView.findViewById(R.id.notification_icon_area_inner);
		//        mNotificationIcons = (IconMerger)mStatusBarView.findViewById(R.id.notificationIcons);
		//        mNotificationArea = (LinearLayout) mStatusBarView.findViewById(R.id.notification_icon_area);
		//        mMoreIcon = (ImageView)mStatusBarView.findViewById(R.id.moreIcon);
		//        mNotificationIcons.setOverflowIndicator(mMoreIcon);
		//        mNotificationIcons.setOverflowRight(mSystemIconArea);
		//        mStatusBarContents = (LinearLayout)mStatusBarView.findViewById(R.id.status_bar_contents);

		//        try {
		//            mClock = (QsHeaderTextClock) mStatusBarView.findViewById(R.id.clock);
		//            mClock.setTextDp(Utilities.pixelToDip(mContext, mContext.getResources().getDimensionPixelSize(R.dimen.status_bar_clock_size)));
		//           /* if(mContext.getResources().getConfiguration().locale.getCountry().equals("MM")||mContext.getResources().getConfiguration().locale.getCountry().equals("ZG")){
		//            	LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) mClock.getLayoutParams();
		//            	params.setMargins(0, dip2px(mContext, -5), 0, 0);
		//            	mClock.setLayoutParams(params);
		//    		}*/
		//            } catch (Exception e) {
		//            }

		//int panelAddWidth = mContext.getResources().getDimensionPixelSize(R.dimen.panel_add_width);

		//        mPanelBg = mStatusBarWindow.findViewById(R.id.panel_bg);
		//        LayoutParams layoutParamsPanelBg = mPanelBg.getLayoutParams();
		//        layoutParamsPanelBg.width = screenHeight + panelAddWidth;
		//        layoutParamsPanelBg.height = screenHeight + panelAddWidth;
		//        mPanelBg.setLayoutParams(layoutParamsPanelBg);

		//        mImageBlur = (ImageView) mStatusBarWindow.findViewById(R.id.image_blur);
		//        mStackScroller = (NotificationStackScrollLayout) mStatusBarWindow.findViewById(
		//                R.id.notification_stack_scroller);
		//        mStackScroller.setLongPressListener(getNotificationLongClicker());
		//        mStackScroller.setPhoneStatusBar(this);
		//        mStackScroller.setGroupManager(mGroupManager);
		//        mGroupManager.setOnGroupChangeListener(mStackScroller);


		//        mKeyguardIconOverflowContainer =
		//                (NotificationOverflowContainer) LayoutInflater.from(mContext).inflate(
		//                        R.layout.status_bar_notification_keyguard_overflow, mStackScroller, false);
		//        mKeyguardIconOverflowContainer.setOnActivatedListener(this);
		//        mKeyguardIconOverflowContainer.setOnClickListener(mOverflowClickListener);
		//        mStackScroller.setOverflowContainer(mKeyguardIconOverflowContainer);
		//        mStackScroller.addViewForPackage(mKeyguardIconOverflowContainer,"com.android.systemui.statusbar.NotificationOverflowContainer");
		//
		//        mSpeedBumpView = (SpeedBumpView) LayoutInflater.from(mContext).inflate(
		//                        R.layout.status_bar_notification_speed_bump, mStackScroller, false);
		//        mStackScroller.setSpeedBumpView(mSpeedBumpView);
		//        mStackScroller.addViewForPackage(mSpeedBumpView,"com.android.systemui.statusbar.SpeedBumpView");

		//        mEmptyShadeView = (EmptyShadeView) LayoutInflater.from(mContext).inflate(
		//                R.layout.status_bar_no_notifications, mStackScroller, false);
		//        mStackScroller.setEmptyShadeView(mEmptyShadeView);
		//        mStackScroller.addViewForPackage(mEmptyShadeView,"com.android.systemui.statusbar.EmptyShadeView");
		//        
		//        mDismissView = (DismissView) LayoutInflater.from(mContext).inflate(
		//                R.layout.status_bar_notification_dismiss_all, mStackScroller, false);
		//        mDismissView.setOnButtonClickListener(new View.OnClickListener() {
		//            @Override
		//            public void onClick(View v) {
		//                clearAllNotifications();
		//            }
		//        });
		//        mDismissView.setVisibility(View.GONE);
		//        mStackScroller.setDismissView(mDismissView);
		//        mStackScroller.addViewForPackage(mDismissView,"com.android.systemui.statusbar.DismissView");
		//        
		//        mExpandedContents = mStackScroller;
		//
		//        mBackdrop = (BackDropView) mStatusBarWindow.findViewById(R.id.backdrop);
		//        mBackdropFront = (ImageView) mBackdrop.findViewById(R.id.backdrop_front);
		//        mBackdropBack = (ImageView) mBackdrop.findViewById(R.id.backdrop_back);
		//
		//        ScrimView scrimBehind = (ScrimView) mStatusBarWindow.findViewById(R.id.scrim_behind);
		//        ScrimView scrimInFront = (ScrimView) mStatusBarWindow.findViewById(R.id.scrim_in_front);
		//        mScrimController = new ScrimController(scrimBehind, scrimInFront, mScrimSrcModeEnabled);
		//        mScrimController.setBackDropView(mBackdrop);
		//        mStatusBarView.setScrimController(mScrimController);
		//        
		//        mSystemIconAreaKeyguard = (LinearLayout) mStatusBarWindow.findViewById(R.id.system_icons_super_container);
		//        mNotificationIconsKeyguard = (IconMerger)mStatusBarWindow.findViewById(R.id.notificationIcons_keyguard);
		//        mMoreIconKeyguard = (ImageView)mStatusBarWindow.findViewById(R.id.moreIcon_keyguard);
		//        mNotificationIconsKeyguard.setOverflowIndicator(mMoreIconKeyguard);
		//        mNotificationIconsKeyguard.setOverflowRight(mSystemIconAreaKeyguard);
		//        mHeader = (StatusBarHeaderView) mStatusBarWindow.findViewById(R.id.header);
		//        mHeader.setActivityStarter(this);
		//        mNotificationDelete = mHeader.findViewById(R.id.ic_notification_delete);
		//        mNotificationDelete.setOnClickListener(new View.OnClickListener() {
		//            @Override
		//            public void onClick(View v) {
		//                clearAllNotifications();
		//            }
		//        });
		//        mNotificationDeleteBottom = mStatusBarWindow.findViewById(R.id.ic_notification_delete_bottom);
		//        mNotificationDeleteBottom.setOnClickListener(new View.OnClickListener() {
		//            @Override
		//            public void onClick(View v) {
		//                clearAllNotifications();
		//            }
		//        });
		//        mSingleClockContainer = mStatusBarWindow.findViewById(R.id.keyguard_clock_container);
		//		mDoubleClockContainer = mStatusBarWindow.findViewById(R.id.keyguard_double_clock_container);
		//        mKeyguardStatusBar = (KeyguardStatusBarView) mStatusBarWindow.findViewById(R.id.keyguard_header);
		//		mNotificationContainerParent = (NotificationsQuickSettingsContainer) mStatusBarWindow.findViewById(R.id.notification_container_parent);
		//        mStatusIconsKeyguard = (LinearLayout) mKeyguardStatusBar.findViewById(R.id.statusIcons);
		//        mKeyguardBottomArea =
		//                (KeyguardBottomAreaView) mStatusBarWindow.findViewById(R.id.keyguard_bottom_area);
		//        mKeyguardBottomArea.setActivityStarter(this);
		//        mKeyguardBottomArea.setAssistManager(mAssistManager);
		//        mKeyguardIndicationController = new KeyguardIndicationController(mContext,
		//                (KeyguardIndicationTextView) mStatusBarWindow.findViewById(
		//                        R.id.keyguard_indication_text));
		//        mKeyguardBottomArea.setKeyguardIndicationController(mKeyguardIndicationController);
		//        mNotificationIconAreaKeyguard = mStatusBarWindow.findViewById(R.id.notification_icon_area_keyguard);
		//
		//        mTickerEnabled = res.getBoolean(R.bool.enable_ticker);
		//        if (mTickerEnabled) {
		//            final ViewStub tickerStub = (ViewStub) mStatusBarView.findViewById(R.id.ticker_stub);
		//            if (tickerStub != null) {
		//                mTickerView = tickerStub.inflate();
		//                mTicker = new MyTicker(context, mStatusBarView);
		//
		//                TickerView tickerView = (TickerView) mStatusBarView.findViewById(R.id.tickerText);
		//                tickerView.mTicker = mTicker;
		//            }
		//        }
		//
		//        mEdgeBorder = res.getDimensionPixelSize(R.dimen.status_bar_edge_ignore);
		//
		//        // set the inital view visibility
		//        setAreThereNotifications();

		// Other icons
		//        mLocationController = new LocationControllerImpl(mContext); // will post a notification
		//        mBatteryController = new BatteryController(mContext);
		//        mYulongController = new YulongController(context, mStatusBarView, mDisplayMetrics, getStatusBarHeight());
		//        mBatteryController.addStateChangedCallback(new BatteryStateChangeCallback() {
		//            @Override
		//            public void onPowerSaveChanged() {
		//                mHandler.post(mCheckBarModes);
		//                if (mDozeServiceHost != null) {
		//                    mDozeServiceHost.firePowerSaveChanged(mBatteryController.isPowerSave());
		//                }
		//            }
		//            @Override
		//            public void onBatteryLevelChanged(int level, boolean pluggedIn, boolean charging) {
		//                // noop
		//            }
		//        });
		//        mHotspotController = new HotspotControllerImpl(mContext);
		//        mBluetoothController = new BluetoothControllerImpl(mContext);
		//        mSecurityController = new SecurityControllerImpl(mContext);
		//        if (mContext.getResources().getBoolean(R.bool.config_showRotationLock)) {
		//            mRotationLockController = new RotationLockControllerImpl(mContext);
		//        }
		//        mUserInfoController = new UserInfoController(mContext);
		//        mVolumeComponent = getComponent(VolumeComponent.class);
		//        mZenModeController = mVolumeComponent.getZenController();
		//        mCastController = new CastControllerImpl(mContext);
		//
		//        mStatusVpnFooter = (StatusVpnFooter)mStatusBarWindow.findViewById(R.id.status_vpn_footer);
		//        mStatusVpnFooterKeyguard = (StatusVpnFooter)mStatusBarWindow.findViewById(R.id.status_vpn_footer_keyguard);
		//        mStatusVpnFooter.setSecurityController(mSecurityController);
		//        mStatusVpnFooterKeyguard.setSecurityController(mSecurityController);
		//        if (isMSim()) {
		//            mMSimNetworkController = new MSimNetworkControllerImpl(mContext);
		//            mMsimSignalCluster = (SignalClusterViewYuLong)
		//                    mStatusBarView.findViewById(R.id.msim_signal_cluster);
		//            mMSimSignalClusterKeyguard = (SignalClusterViewYuLong)
		//                    mKeyguardStatusBar.findViewById(R.id.msim_signal_cluster);            
		//            mMSimNetworkController.addNetworkSignalChangedCallback(DataNetworkController.getInstance(mContext, false));  
		//            mMSimNetworkController.addNetworkSignalChangedCallback(DataNetworkController.getInstance(mContext, true));
		//            for (int i = 0; i < TelephonyManager.getDefault().getPhoneCount(); i++) {
		//                mMSimNetworkController.addSignalCluster(mMsimSignalCluster, i);
		//                mMSimNetworkController.addSignalCluster(mMSimSignalClusterKeyguard, i);
		//            }
		//            signalCluster.setNetworkController(mMSimNetworkController);
		//            signalClusterKeyguard.setNetworkController(mMSimNetworkController);

		/*mMSimNetworkController.addEmergencyLabelView(mHeader);

            mCarrierLabel = (TextView)mStatusBarWindow.findViewById(R.id.carrier_label);
            mSubsLabel = (TextView)mStatusBarWindow.findViewById(R.id.subs_label);
            mShowCarrierInPanel = (mCarrierLabel != null);

            if (DEBUG) Log.v(TAG, "carrierlabel=" + mCarrierLabel + " show=" +
                                    mShowCarrierInPanel + "operator label=" + mSubsLabel);
            if (mShowCarrierInPanel) {
                mCarrierLabel.setVisibility(mCarrierLabelVisible ? View.VISIBLE : View.INVISIBLE);

                // for mobile devices, we always show mobile connection info here (SPN/PLMN)
                // for other devices, we show whatever network is connected
                if (mMSimNetworkController.hasMobileDataFeature()) {
                    mMSimNetworkController.addMobileLabelView(mCarrierLabel);
                } else {
                    mMSimNetworkController.addCombinedLabelView(mCarrierLabel);
                }
                //mSubsLabel.setVisibility(View.VISIBLE);
                mMSimNetworkController.addSubsLabelView(mSubsLabel);
                // set up the dynamic hide/show of the label
                //mPile.setOnSizeChangedListener(new OnSizeChangedListener() {
                //    @Override
                //    public void onSizeChanged(View view, int w, int h, int oldw, int oldh) {
                //        updateCarrierLabelVisibility(false);
                //    }
                //});
            }
        }*//* else {
            mNetworkController = new NetworkControllerImpl(mContext);
            mSignalCluster =
                (SignalClusterView)mStatusBarView.findViewById(R.id.signal_cluster);
            mSignalClusterKeyguard =
                (SignalClusterView) mKeyguardStatusBar.findViewById(R.id.signal_cluster);
            final SignalClusterView signalClusterQs =
                (SignalClusterView) mHeader.findViewById(R.id.signal_cluster);
            mNetworkController.addNetworkSignalChangedCallback(DataNetworkController.getInstance(mContext, true)); 
            mNetworkController.addNetworkSignalChangedCallback(DataNetworkController.getInstance(mContext, false));   
            mNetworkController.addSignalCluster(mSignalCluster);
            mNetworkController.addSignalCluster(mSignalClusterKeyguard);
            mNetworkController.addSignalCluster(signalClusterQs);
            mSignalCluster.setSecurityController(mSecurityController);
            mSignalCluster.setNetworkController(mNetworkController);
            mSignalClusterKeyguard.setSecurityController(mSecurityController);
            mSignalClusterKeyguard.setNetworkController(mNetworkController);
            signalClusterQs.setSecurityController(mSecurityController);
            signalClusterQs.setNetworkController(mNetworkController);
            final boolean isAPhone = mNetworkController.hasVoiceCallingFeature();
            if (isAPhone) {
                mNetworkController.addEmergencyLabelView(mHeader);
            }

            mCarrierLabel = (TextView)mStatusBarWindow.findViewById(R.id.carrier_label);
            mShowCarrierInPanel = (mCarrierLabel != null);
            if (DEBUG) Log.v(TAG, "carrierlabel=" + mCarrierLabel + " show=" + mShowCarrierInPanel);
            if (mShowCarrierInPanel) {
                mCarrierLabel.setVisibility(mCarrierLabelVisible ? View.VISIBLE : View.INVISIBLE);

                // for mobile devices, we always show mobile connection info here (SPN/PLMN)
                // for other devices, we show whatever network is connected
                if (mNetworkController.hasMobileDataFeature()) {
                    mNetworkController.addMobileLabelView(mCarrierLabel);
                } else {
                    mNetworkController.addCombinedLabelView(mCarrierLabel);
                }
            }*/

		// set up the dynamic hide/show of the label
		// TODO: uncomment, handle this for the Stack scroller aswell
		//                ((NotificationRowLayout) mStackScroller)
		// .setOnSizeChangedListener(new OnSizeChangedListener() {
		//                @Override
		//                public void onSizeChanged(View view, int w, int h, int oldw, int oldh) {
		//                    updateCarrierLabelVisibility(false);
		//        }

		//mFlashlightController = new FlashlightController(mContext);
		//mKeyguardBottomArea.setFlashlightController(mFlashlightController);
		//        mKeyguardBottomArea.setPhoneStatusBar(this);
		//        mKeyguardBottomArea.setUserSetupComplete(mUserSetup);
		//        mAccessibilityController = new AccessibilityController(mContext);
		//        mKeyguardBottomArea.setAccessibilityController(mAccessibilityController);
		//        mNextAlarmController = new NextAlarmController(mContext);
		//        mKeyguardMonitor = new KeyguardMonitor();
		//        mUserSwitcherController = new UserSwitcherController(mContext, mKeyguardMonitor);
		//
		//        mKeyguardUserSwitcher = new KeyguardUserSwitcher(mContext,
		//                (ViewStub) mStatusBarWindow.findViewById(R.id.keyguard_user_switcher),
		//                mKeyguardStatusBar, mNotificationPanel, mUserSwitcherController);


		// Set up the quick settings tile panel
		//        mFlipSettingsView = mStatusBarWindow.findViewById(R.id.setting_config_view);
		//        mFlipSettingsView.setVisibility(View.VISIBLE);
		//        mQSPanel = (QSPanel) mStatusBarWindow.findViewById(R.id.quick_settings_panel);
		//        QuickSettingLauncher.getInstance(mContext).start(this);
		//        if (mQSPanel != null) {
		//            if (isMSim()) {
		//                qsh = new QSTileHost(mContext, this,
		//                    mBluetoothController, 
		////                    mBluetoothController, mLocationController, mRotationLockController,
		//                		mMSimNetworkController, 
		////                    mZenModeController, mVolumeComponent,
		////                    mHotspotController, mCastController, mFlashlightController,
		//                    mUserSwitcherController, mKeyguardMonitor,
		//                    mSecurityController);
		//            } else {
		//                qsh = new QSTileHost(mContext, this,
		//                    mBluetoothController,
		////                    mBluetoothController, mLocationController, mRotationLockController,
		//                		mNetworkController, 
		////                    mZenModeController, mVolumeComponent,
		////                    mHotspotController, mCastController, mFlashlightController,
		//                    mUserSwitcherController, mKeyguardMonitor,
		//                    mSecurityController);
		//            }
		//            mQSPanel.setHost(qsh);
		//            mQSPanel.setTiles(qsh.getTiles());
		//            YulongQuickSettingsContain.getInstance(mContext,true).setStatusBar(this);
		//            YulongQuickSettingsContain.getInstance(mContext,false).setStatusBar(this);
		//            mBrightnessMirrorController = new BrightnessMirrorController(mStatusBarWindow);
		//            mQSPanel.setBrightnessMirror(mBrightnessMirrorController);
		//            mHeader.setQSPanel(mQSPanel);
		////            qsh.setCallback(new QSTileHost.Callback() {
		////                @Override
		////                public void onTilesChanged() {
		////                    mQSPanel.setTiles(qsh.getTiles());
		////                }
		////            });
		//        }

		// task manager
		//        if (mContext.getResources().getBoolean(R.bool.config_showTaskManagerSwitcher)) {
		//            mTaskManagerPanel =
		//                    (LinearLayout) mStatusBarWindow.findViewById(R.id.task_manager_panel);
		//            mTaskManager = new TaskManager(mContext, mTaskManagerPanel);
		//            mTaskManager.setActivityStarter(this);
		//            mTaskManagerButton = (ImageButton) mHeader.findViewById(R.id.task_manager_button);
		//            mTaskManagerButton.setOnClickListener(new OnClickListener() {
		//                @Override
		//                public void onClick(View arg0) {
		//                    showTaskList = !showTaskList;
		//                    mNotificationPanel.setTaskManagerVisibility(showTaskList);
		//                }
		//            });
		//        }

		// User info. Trigger first load.
		//        mHeader.setUserInfoController(mUserInfoController);
		//        mKeyguardStatusBar.setUserInfoController(mUserInfoController);
		//        mUserInfoController.reloadUserInfo();

		//        mHeader.setBatteryController(mBatteryController);//===modify by ty
		//        mBatteryView.setBatteryController(mBatteryController);
		//        ((BatteryMeterView) mStatusBarView.findViewById(R.id.battery)).setBatteryController(
		//                mBatteryController);
		//        ((BatteryViewGroup) mStatusBarView.findViewById(R.id.battery_field)).setBatteryController(
		//                mBatteryController);
		//        mKeyguardStatusBar.setBatteryController(mBatteryController);
		//        mHeader.setNextAlarmController(mNextAlarmController);
		//
		//        PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
		//        mBroadcastReceiver.onReceive(mContext,
		//                new Intent(pm.isInteractive() ? Intent.ACTION_SCREEN_ON : Intent.ACTION_SCREEN_OFF));

		//        getWifiTile();
		//        getBluetoothTile();
		// receive broadcasts
		//        IntentFilter filter = new IntentFilter();
		//        filter.addAction(Intent.ACTION_CLOSE_SYSTEM_DIALOGS);
		//        filter.addAction(Intent.ACTION_SCREEN_OFF);
		//        filter.addAction(Intent.ACTION_SCREEN_ON);
		//        filter.addAction(UPDATENOTIFCATION);
		//        filter.addAction(Intent.ACTION_CONFIGURATION_CHANGED);
		//        if (DEBUG_MEDIA_FAKE_ARTWORK) {
		//            filter.addAction("fake_artwork");
		//        }
		//        filter.addAction(ACTION_DEMO);
		//        filter.addAction(TOP_WINDOW_CHANGED);
		//        filter.addAction(ACTION_WALLPAPER_BRIGHTNESS);
		//        filter.addAction("android.intent.action.THEME_CHANGED");
		//        filter.addAction(NOTIFY_SHOW_SETTING);
		//        context.registerReceiver(mBroadcastReceiver, filter);
		//
		//		IntentFilter intentFilter = new IntentFilter();
		//		intentFilter.addAction(SWITCHED_TO_SED);
		//		intentFilter.addAction(SWITCHED_TO_PPD);
		//		context.registerReceiver(focusReceiver, intentFilter,"com.redbend.permission.EVENT_INTENT", null);
		// listen for USER_SETUP_COMPLETE setting (per-user)
		//        resetUserSetupObserver();
		//        startGlyphRasterizeHack();
		//        updateSystemIconColor();

		return mStatusBarView;
	}

	private void addHeadsUpView() {
		/*int headsUpHeight = mContext.getResources()
                .getDimensionPixelSize(R.dimen.heads_up_window_height);*/
		int bottom = 0;
		WindowManager mWm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
		mWm.getDefaultDisplay().getRealMetrics(mDisplayMetrics);

		//		final int w = mDisplayMetrics.widthPixels;
		//final int h = mDisplayMetrics.heightPixels;
		//		final int innerH = mContext.getResources().getDimensionPixelSize(R.dimen.notification_min_height);
		WindowManager.LayoutParams lp;
		lp = new WindowManager.LayoutParams();
		lp.type = WindowManager.LayoutParams.TYPE_MAGNIFICATION_OVERLAY;
		lp.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
				| WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
		lp.format = PixelFormat.RGBA_8888;
		lp.gravity = Gravity.TOP | Gravity.START;
		lp.setTitle("Heads Up");
		lp.packageName = mContext.getPackageName();
		//        lp.windowAnimations = R.style.Animation_StatusBar_HeadsUp;
		lp.width = WindowManager.LayoutParams.MATCH_PARENT;
		lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
		lp.x = 0;
		lp.y = bottom;

		//	    mHeadsUpNotificationView.setBackground(null);
		//		if (mHeadsUpNotificationView.getParent() == null) {
		//			mWindowManager.addView(mHeadsUpNotificationView, lp);
		//		}
	}

	// tracking calls to View.setSystemUiVisibility()
	int mSystemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE;
	private static final int STATUS_OR_NAV_TRANSIENT =
			View.STATUS_BAR_TRANSIENT | View.NAVIGATION_BAR_TRANSIENT;
	private void checkUserAutohide(View v, MotionEvent event) {
		if ((mSystemUiVisibility & STATUS_OR_NAV_TRANSIENT) != 0  // a transient bar is revealed
				&& event.getAction() == MotionEvent.ACTION_OUTSIDE // touch outside the source bar
				&& event.getX() == 0 && event.getY() == 0  // a touch outside both bars
				) {
			userAutohide();
		}
	}

	private void userAutohide() {
		cancelAutohide();
		//        mHandler.postDelayed(mAutohide, 350); // longer than app gesture -> flag clear
	}
	private boolean mAutohideSuspended;
	private void cancelAutohide() {
		mAutohideSuspended = false;
		//        mHandler.removeCallbacks(mAutohide);
	}

	private void scheduleAutohide() {
		cancelAutohide();
		//        mHandler.postDelayed(mAutohide, AUTOHIDE_TIMEOUT_MS);
	}
	@Override
	protected void refreshLayout(int layoutDirection) {
		// TODO Auto-generated method stub

	}

	@Override
	protected LayoutParams getSearchLayoutParams(
			android.view.ViewGroup.LayoutParams layoutParams) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected View getStatusBarView() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void resetHeadsUpDecayTimer() {
		// TODO Auto-generated method stub

	}

	@Override
	public void scheduleHeadsUpOpen() {
		// TODO Auto-generated method stub

	}

	@Override
	public void scheduleHeadsUpClose() {
		// TODO Auto-generated method stub

	}

	@Override
	public void scheduleHeadsUpEscalation() {
		// TODO Auto-generated method stub

	}

	@Override
	protected boolean isCoolShowThemeMode() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected boolean isCoverMode() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void haltTicker() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void setAreThereNotifications() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void updateNotifications() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void tick(StatusBarNotification n, boolean firstTime) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void updateExpandedViewPos(int expandedPosition) {
		// TODO Auto-generated method stub

	}

	@Override
	protected boolean shouldDisableNavbarGestures() {
		// TODO Auto-generated method stub
		return false;
	}

	// SystemUIService notifies SystemBars of configuration changes, which then calls down here
	@Override
	protected void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig); // calls refreshLayout

		if (DEBUG) {
			Log.v(TAG, "configuration changed: " + mContext.getResources().getConfiguration());
		}
		/*    LinearLayout.LayoutParams params = (android.widget.LinearLayout.LayoutParams) mClock.getLayoutParams();
        if(mContext.getResources().getConfiguration().locale.getCountry().equalsIgnoreCase("MM")||mContext.getResources().getConfiguration().locale.getCountry().equalsIgnoreCase("ZG")){
        	params.setMargins(0, dip2px(mContext, -5), 0, 0);
		}else{
        	params.setMargins(0, 0, 0, 0);
		}
        mClock.setLayoutParams(params);
		 */
		updateDisplaySize(); // populates mDisplayMetrics

		//        updateResources();
		//        updateClockSize();
		repositionNavigationBar();
		updateExpandedViewPos(EXPANDED_LEAVE_ALONE);
		//        updateShowSearchHoldoff();
		//        updateRowStates();
		//        updateCoverState(mStatusBarKeyguardViewManager.isCovered());
		//        
		//        if(mHeadsUpNotificationView != null && mHeadsUpNotificationView.isAttachedToWindow()){
		//        	removeHeadsUpView();
		addHeadsUpView();
		//        }
		//        if (true/*Feature Control*/){
		//        	QuickSettingLauncher.getInstance(mContext).onConfigurationChanged(newConfig);
		//        }
	}

}
