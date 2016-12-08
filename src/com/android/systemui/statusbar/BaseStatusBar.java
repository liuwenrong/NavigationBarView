/*
 * Copyright (C) 2010 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import android.animation.TimeInterpolator;
import android.app.ActivityManager;
import android.app.ActivityManagerNative;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.UserInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.hardware.display.DisplayManager;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.service.dreams.DreamService;
import android.service.dreams.IDreamManager;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.IWindowManager;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.view.WindowManagerGlobal;
import android.view.accessibility.AccessibilityManager;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.RemoteViews;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.statusbar.IStatusBarService;
import com.android.internal.statusbar.StatusBarIcon;
import com.android.internal.statusbar.StatusBarIconList;
import com.android.internal.util.NotificationColorUtil;
import com.android.internal.widget.LockPatternUtils;
import com.android.systemui.SystemUI;
import com.android.systemui.Utils;
import com.android.systemui.helper.LogHelper;
import com.android.systemui.statusbar.phone.StatusBarWindowView;
import com.android.systemui.statusbar.policy.CurrentUserTracker;
import com.example.liuwenrong.navigationbarview.BSNavigationBarView;
//import com.android.systemui.statusbar.preferences.NotificationCollapseManage;
//import com.android.systemui.statusbar.preferences.PackageSettingsActivity;
//import com.android.systemui.statusbar.preferences.SysAppProvider;
//import com.android.systemui.statusbar.stack.NotificationStackScrollLayout;

public abstract class BaseStatusBar extends SystemUI implements
        CommandQueue.Callbacks{
    public static final String TAG = "xx--lwr--BaseStatusBar";
    public static final boolean DEBUG = Log.isLoggable(TAG, Log.DEBUG);
    public static final boolean MULTIUSER_DEBUG = false;
    public static final boolean ENABLE_CHILD_NOTIFICATIONS = Build.IS_DEBUGGABLE
            && SystemProperties.getBoolean("debug.child_notifs", false);
    
    protected static final int MSG_SHOW_RECENT_APPS = 1019;
    protected static final int MSG_HIDE_RECENT_APPS = 1020;
    protected static final int MSG_TOGGLE_RECENTS_APPS = 1021;
    protected static final int MSG_PRELOAD_RECENT_APPS = 1022;
    protected static final int MSG_CANCEL_PRELOAD_RECENT_APPS = 1023;
    protected static final int MSG_SHOW_NEXT_AFFILIATED_TASK = 1024;
    protected static final int MSG_SHOW_PREV_AFFILIATED_TASK = 1025;
    protected static final int MSG_CLOSE_SEARCH_PANEL = 1027;
    protected static final int MSG_SHOW_HEADS_UP = 1028;
    protected static final int MSG_HIDE_HEADS_UP = 1029;
    protected static final int MSG_ESCALATE_HEADS_UP = 1030;
    protected static final int MSG_DECAY_HEADS_UP = 1031;
    protected static final int MSG_UPDATE_STATUSBAR_ICON_COLOR = 150515;
    protected static final int MSG_HIDE_STATUSBAR_ICON = 150616;
    protected static final int MSG_SHOW_STATUSBAR_ICON = 150617;
    public boolean mIsSecurityMode = false;
    final int SCREENSHOT_NOTIFICATION_ID = 789;

    protected static final boolean ENABLE_HEADS_UP = true;
    // scores above this threshold should be displayed in heads up mode.
    protected static final int INTERRUPTION_THRESHOLD = 10;
    protected static final String SETTING_HEADS_UP_TICKER = "ticker_gets_heads_up";

    // Should match the value in PhoneWindowManager
    public static final String SYSTEM_DIALOG_REASON_RECENT_APPS = "recentapps";

    public static final int EXPANDED_LEAVE_ALONE = -10000;
    public static final int EXPANDED_FULL_OPEN = -10001;

    private static final int HIDDEN_NOTIFICATION_ID = 10000;
	private static final String BANNER_ACTION_CANCEL = "com.android.systemui.statusbar.banner_action_cancel";
	private static final String BANNER_ACTION_SETUP = "com.android.systemui.statusbar.banner_action_setup";
	private static final String USER_SWITCHING = "yulong.intent.action.USER_SWITCHING";
    protected static final int SCURITY_MODE_USER_ID = 10;
    
    protected CommandQueue mCommandQueue;
    protected IStatusBarService mBarService;

    public ImageView  mImageBlur ;
    protected View mSuperStatusBg;
    protected View mPanelBg;
    protected RelativeLayout mClearAllLayout;
    public ImageView mClearAllGussBg;
    protected ImageView mClearAllButton;

    // used to notify status bar for suppressing notification LED
    protected boolean mPanelSlightlyVisible;

    protected StatusBarWindowView mStatusBarWindow;
    
    protected int mCurrentUserId = 0;
    final protected SparseArray<UserInfo> mCurrentProfiles = new SparseArray<UserInfo>();

    protected int mLayoutDirection = -1; // invalid
    protected AccessibilityManager mAccessibilityManager;

    // on-screen navigation buttons
    protected BSNavigationBarView mNavigationBarView = null;
    private Locale mLocale;
    private float mFontScale;

    protected boolean mUseHeadsUp = false;
    protected boolean mHeadsUpTicker = false;
    protected boolean mDisableNotificationAlerts = false;

    protected DevicePolicyManager mDevicePolicyManager;
    protected IDreamManager mDreamManager;
    PowerManager mPowerManager;
    protected int mRowMinHeight;
    protected int mRowMaxHeight;

    // public mode, private notifications, etc
    private boolean mLockscreenPublicMode = false;
    private final SparseBooleanArray mUsersAllowingPrivateNotifications = new SparseBooleanArray();
    private NotificationColorUtil mNotificationColorUtil;

    private UserManager mUserManager;

    // UI-specific methods

    /**
     * Create all windows necessary for the status bar (including navigation, overlay panels, etc)
     * and add them to the window manager.
     */
    protected abstract void createAndAddWindows();

    protected WindowManager mWindowManager;
    protected IWindowManager mWindowManagerService;

    protected abstract void refreshLayout(int layoutDirection);

    protected Display mDisplay;

    private boolean mDeviceProvisioned = false;

    protected int mZenMode;

    private TimeInterpolator /*mLinearOutSlowIn, */mFastOutLinearIn;

    /**
     * The {@link StatusBarState} of the status bar.
     */
    protected int mState;
    protected boolean mBouncerShowing;
//    protected DismissView mDismissView;
//    protected EmptyShadeView mEmptyShadeView;
//    protected SpeedBumpView mSpeedBumpView;
//    protected AssistManager mAssistManager;
  	private final int SECOND = 1000; 
  	//private final int MINUTE = 60*SECOND; 
  	//private final String mShowFlowNotification = "show_flow_notification";
  	private final float  SCALING = 1.06f; 
  	//private final String COUNT_SWITCH_ONOFF = "countSwitchOnOff";
  	private final static String STATUSBAR_SHOW_NOTIFICATIONS = "notification_display_application_icon_status";
	public static ArrayMap<String, Integer> mPackageNotificationMap = new ArrayMap<String, Integer>();
	public static ArrayMap<String, Boolean> mPackageChatMapFirst = new ArrayMap<String, Boolean>();
  	public static boolean isScreenHorizontal = true;
    
    private RemoteViews.OnClickHandler mOnClickHandler = new RemoteViews.OnClickHandler() {
        @Override
        public boolean onClickHandler(
                final View view, final PendingIntent pendingIntent, final Intent fillInIntent) {
            if (DEBUG) {
                Log.v(TAG, "Notification click handler invoked for intent: " + pendingIntent);
            }
            // The intent we are sending is for the application, which
            // won't have permission to immediately start an activity after
            // the user switches to home.  We know it is safe to do at this
            // point, so make sure new activity switches are now allowed.
            try {
                ActivityManagerNative.getDefault().resumeAppSwitches();
            } catch (RemoteException e) {
            }
            final boolean isActivity = pendingIntent.isActivity();
            if (isActivity) {
                return true;
            } else {
                return super.onClickHandler(view, pendingIntent, fillInIntent);
            }
        }

        private boolean superOnClickHandler(View view, PendingIntent pendingIntent,
                Intent fillInIntent) {
            return super.onClickHandler(view, pendingIntent, fillInIntent);
        }
    };

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (/*Intent.ACTION_USER_SWITCHED*/USER_SWITCHING.equals(action)) {
                mCurrentUserId = intent.getIntExtra(Intent.EXTRA_USER_HANDLE, -1);
                updateCurrentProfilesCache();
                if (true) Log.v(TAG, "userId " + mCurrentUserId + " is in the house");

                userSwitched(mCurrentUserId);
            } else if (Intent.ACTION_USER_ADDED.equals(action)) {
                updateCurrentProfilesCache();
            } else if (DevicePolicyManager.ACTION_DEVICE_POLICY_MANAGER_STATE_CHANGED.equals(
                    action)) {
                mUsersAllowingPrivateNotifications.clear();
                updateNotifications();
            } else if (Intent.ACTION_USER_PRESENT.equals(action)) {
                List<ActivityManager.RecentTaskInfo> recentTask = null;
                try {
                    recentTask = ActivityManagerNative.getDefault().getRecentTasks(1,
                            ActivityManager.RECENT_WITH_EXCLUDED
                            | ActivityManager.RECENT_INCLUDE_PROFILES,
                            mCurrentUserId);
                } catch (RemoteException e) {
                }
                if (recentTask != null && recentTask.size() > 0) {
                    UserInfo user = mUserManager.getUserInfo(recentTask.get(0).userId);
                    if (user != null && user.isManagedProfile() && !"Dual_app".equals(user.name)) {
                        Toast toast = Toast.makeText(mContext,
                        		"您当前正在使用工作资料",
                                Toast.LENGTH_SHORT);
                        TextView text = (TextView) toast.getView().findViewById(
                                android.R.id.message);
//                        text.setCompoundDrawablesRelativeWithIntrinsicBounds(
//                                R.drawable.stat_sys_managed_profile_status, 0, 0, 0);
//                        int paddingPx = mContext.getResources().getDimensionPixelSize(
//                                R.dimen.managed_profile_toast_padding);
//                        text.setCompoundDrawablePadding(paddingPx);
                        toast.show();
                    }
                }
            } else if (BANNER_ACTION_CANCEL.equals(action) || BANNER_ACTION_SETUP.equals(action)) {
                NotificationManager noMan = (NotificationManager)
                        mContext.getSystemService(Context.NOTIFICATION_SERVICE);
                noMan.cancel(HIDDEN_NOTIFICATION_ID);

                Settings.Secure.putInt(mContext.getContentResolver(),
                        Settings.Secure.SHOW_NOTE_ABOUT_NOTIFICATION_HIDING, 0);
                if (BANNER_ACTION_SETUP.equals(action)) {
                    animateCollapsePanels(CommandQueue.FLAG_EXCLUDE_NONE, true /* force */);
                    mContext.startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_REDACTION)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                    );
                }
            }
        }
    };

    private final BroadcastReceiver mAllUsersReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (DevicePolicyManager.ACTION_DEVICE_POLICY_MANAGER_STATE_CHANGED.equals(action) &&
                    isCurrentProfile(getSendingUserId())) {
                mUsersAllowingPrivateNotifications.clear();
//                updateNotifications();
            }
        }
    };
  
	private PackageManager mPm;

    private void updateCurrentProfilesCache() {
        synchronized (mCurrentProfiles) {
            mCurrentProfiles.clear();
            if (mUserManager != null) {
                for (UserInfo user : mUserManager.getProfiles(mCurrentUserId)) {
                    mCurrentProfiles.put(user.id, user);
                }
            }
        }
    }

    public void start() {
    	LogHelper.logE(TAG, "start方法开始执行");
    	LogHelper.logE(TAG, "mContext=" + mContext.toString());
        mWindowManager = (WindowManager)mContext.getSystemService(Context.WINDOW_SERVICE);
        mWindowManagerService = WindowManagerGlobal.getWindowManagerService();
        
        mDisplay = mWindowManager.getDefaultDisplay();
        
        LogHelper.logE(TAG, "defaultDisplay : "+mDisplay.getName());
        
        DisplayManager mDisplayManager = (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);
		Display[] displays = mDisplayManager.getDisplays();
		LogHelper.logE(TAG, "display.length=="+displays.length);
		if(displays.length < 2){
			mDisplay = displays[0];
		}else{
			mDisplay = displays[1];
		}
        mPm = mContext.getPackageManager();
        
//        SysAppProvider.getInstance(mContext).mapAllSysPackage(mContext);
        CurrentUserTracker.Init(mContext);
//        notificationCollapseManage = NotificationCollapseManage.getDefault(mContext);
        mDevicePolicyManager = (DevicePolicyManager)mContext.getSystemService(
                Context.DEVICE_POLICY_SERVICE);

        mNotificationColorUtil = NotificationColorUtil.getInstance(mContext);

//        mNotificationData = new NotificationData(this);

        mAccessibilityManager = (AccessibilityManager)
                mContext.getSystemService(Context.ACCESSIBILITY_SERVICE);

        mDreamManager = IDreamManager.Stub.asInterface(
                ServiceManager.checkService(DreamService.DREAM_SERVICE));
        mPowerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);

        mBarService = IStatusBarService.Stub.asInterface(
                ServiceManager.getService(Context.STATUS_BAR_SERVICE));

        final Configuration currentConfig = mContext.getResources().getConfiguration();
        mLocale = currentConfig.locale;
        mLayoutDirection = TextUtils.getLayoutDirectionFromLocale(mLocale);
        mFontScale = currentConfig.fontScale;

        mUserManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);

        /*mLinearOutSlowIn = AnimationUtils.loadInterpolator(mContext,
                android.R.interpolator.linear_out_slow_in);*/
        mFastOutLinearIn = AnimationUtils.loadInterpolator(mContext,
                android.R.interpolator.fast_out_linear_in);

        // Connect in to the status bar manager service
        StatusBarIconList iconList = new StatusBarIconList();
        mCommandQueue = new CommandQueue(this, iconList);

        int[] switches = new int[8];
        ArrayList<IBinder> binders = new ArrayList<IBinder>();
        try {
            mBarService.registerStatusBar(mCommandQueue, iconList, switches, binders);
        } catch (RemoteException ex) {
            // If the system process isn't there we're doomed anyway.
        }
        
        createAndAddWindows();

        disable(switches[0], false /* animate */);
        setSystemUiVisibility(switches[1], 0xffffffff);
        topAppWindowChanged(switches[2] != 0);
        // StatusBarManagerService has a back up of IME token and it's restored here.
        setImeWindowStatus(binders.get(0), switches[3], switches[4], switches[5] != 0);

        // Set up the initial icon state
        int N = iconList.size();
        int viewIndex = 0;
        for (int i=0; i<N; i++) {
            StatusBarIcon icon = iconList.getIcon(i);
            if (icon != null) {
                addIcon(iconList.getSlot(i), i, viewIndex, icon);
                viewIndex++;
            }
        }

        if (DEBUG) {
            Log.d(TAG, String.format(
                    "init: icons=%d disabled=0x%08x lights=0x%08x menu=0x%08x imeButton=0x%08x",
                   iconList.size(),
                   switches[0],
                   switches[1],
                   switches[2],
                   switches[3]
                   ));
        }

        mCurrentUserId = ActivityManager.getCurrentUser();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_SWITCHED);
        filter.addAction(Intent.ACTION_USER_ADDED);
        filter.addAction(Intent.ACTION_USER_PRESENT);
        filter.addAction(BANNER_ACTION_CANCEL);
        filter.addAction(BANNER_ACTION_SETUP);
        filter.addAction(DevicePolicyManager.ACTION_DEVICE_POLICY_MANAGER_STATE_CHANGED);
        filter.addAction(USER_SWITCHING);
        mContext.registerReceiver(mBroadcastReceiver, filter);

        updateCurrentProfilesCache();
        LogHelper.logE(TAG, "start方法结束");
    }

    protected void notifyUserAboutHiddenNotifications() {
        if (0 != Settings.Secure.getInt(mContext.getContentResolver(),
                Settings.Secure.SHOW_NOTE_ABOUT_NOTIFICATION_HIDING, 1)) {
            Log.d(TAG, "user hasn't seen notification about hidden notifications");
            final LockPatternUtils lockPatternUtils = new LockPatternUtils(mContext);
            Log.d(TAG, "disabling lockecreen notifications and alerting the user");

            final String packageName = mContext.getPackageName();
            PendingIntent cancelIntent = PendingIntent.getBroadcast(mContext, 0,
                    new Intent(BANNER_ACTION_CANCEL).setPackage(packageName),
                    PendingIntent.FLAG_CANCEL_CURRENT);
            PendingIntent setupIntent = PendingIntent.getBroadcast(mContext, 0,
                    new Intent(BANNER_ACTION_SETUP).setPackage(packageName),
                    PendingIntent.FLAG_CANCEL_CURRENT);

            final Resources res = mContext.getResources();
            final int colorRes = com.android.internal.R.color.system_notification_accent_color;
        }
    }

    public void userSwitched(int newUserId) {
        // should be overridden
    }

    protected boolean isCurrentProfile(int userId) {
        synchronized (mCurrentProfiles) {
            return userId == UserHandle.USER_ALL || mCurrentProfiles.get(userId) != null;
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
        	isScreenHorizontal = false;
        	final Resources res = mContext.getResources();
        }else {
        	isScreenHorizontal = true;
            final Resources res = mContext.getResources();
		}
        final Locale locale = mContext.getResources().getConfiguration().locale;
        final int ld = TextUtils.getLayoutDirectionFromLocale(locale);
        final float fontScale = newConfig.fontScale;

        if (! locale.equals(mLocale) || ld != mLayoutDirection || fontScale != mFontScale) {
            if (DEBUG) {
                Log.v(TAG, String.format(
                        "config changed locale/LD: %s (%d) -> %s (%d)", mLocale, mLayoutDirection,
                        locale, ld));
            }
            mLocale = locale;
            mLayoutDirection = ld;
            refreshLayout(ld);
        }
    }


    public void onHeadsUpDismissed() {
    }

    protected abstract WindowManager.LayoutParams getSearchLayoutParams(
            LayoutParams layoutParams);

    static void sendCloseSystemWindows(Context context, String reason) {
        if (ActivityManagerNative.isSystemReady()) {
            try {
                ActivityManagerNative.getDefault().closeSystemDialogs(reason);
            } catch (RemoteException e) {
            }
        }
    }

    protected abstract View getStatusBarView();

    protected View.OnTouchListener mRecentsPreloadOnTouchListener = new View.OnTouchListener() {
        // additional optimization when we have software system buttons - start loading the recent
        // tasks on touch down
        @Override
        public boolean onTouch(View v, MotionEvent event) {
            int action = event.getAction() & MotionEvent.ACTION_MASK;
            if (action == MotionEvent.ACTION_DOWN) {
//                preloadRecents();
            } else if (action == MotionEvent.ACTION_CANCEL) {
//                cancelPreloadingRecents();
            } else if (action == MotionEvent.ACTION_UP) {
                if (!v.isPressed()) {
//                    cancelPreloadingRecents();
                }

            }
            return false;
        }
    };

    public abstract void resetHeadsUpDecayTimer();

    public abstract void scheduleHeadsUpOpen();

    public abstract void scheduleHeadsUpClose();

    public abstract void scheduleHeadsUpEscalation();

    /**
     * Save the current "public" (locked and secure) state of the lockscreen.
     */
    public void setLockscreenPublicMode(boolean publicMode) {
        mLockscreenPublicMode = publicMode;
    }

    public boolean isLockscreenPublicMode() {
        return mLockscreenPublicMode;
    }

    /**
     * Has the given user chosen to allow their private (full) notifications to be shown even
     * when the lockscreen is in "public" (secure & locked) mode?
     */
	public boolean userAllowsPrivateNotificationsInPublic(int userHandle) {
		if (userHandle == UserHandle.USER_ALL) {
			return true;
		}

		if (mUsersAllowingPrivateNotifications.indexOfKey(userHandle) < 0) {
			final boolean allowedByUser = 0 != Settings.Secure.getIntForUser(mContext.getContentResolver(),
					Settings.Secure.LOCK_SCREEN_ALLOW_PRIVATE_NOTIFICATIONS, 0, userHandle);
			final int dpmFlags = mDevicePolicyManager.getKeyguardDisabledFeatures(null /* admin */, userHandle);
			final boolean allowedByDpm = (dpmFlags & DevicePolicyManager.KEYGUARD_DISABLE_UNREDACTED_NOTIFICATIONS) == 0;
			/*Log.v("trace", "userAllowsPrivateNotificationsInPublic() + allowedByUser = " + allowedByUser + ", dpmFlags = "
					+ dpmFlags + ", allowedByDpm = " + allowedByDpm);*/
			final boolean allowed = allowedByUser && allowedByDpm;
			mUsersAllowingPrivateNotifications.append(userHandle, allowed);
			return allowed;
		}

		return mUsersAllowingPrivateNotifications.get(userHandle);
	}

    protected void onUpdateSystemIconColor() {
		
	}
    protected void setHideIconAnimator() {
	}
    protected void setShowIconAnimator() {
	}

//    public class TouchOutsideListener implements View.OnTouchListener {
//        private int mMsg;
//        private StatusBarPanel mPanel;
//
//        public TouchOutsideListener(int msg, StatusBarPanel panel) {
//            mMsg = msg;
//            mPanel = panel;
//        }
//
//        public boolean onTouch(View v, MotionEvent ev) {
//            final int action = ev.getAction();
//            if (action == MotionEvent.ACTION_OUTSIDE
//                || (action == MotionEvent.ACTION_DOWN
//                    && !mPanel.isInContentArea((int)ev.getX(), (int)ev.getY()))) {
//                mHandler.removeMessages(mMsg);
//                mHandler.sendEmptyMessage(mMsg);
//                return true;
//            }
//            return false;
//        }
//    }

    protected void workAroundBadLayerDrawableOpacity(View v) {
    }

    public void animateCollapsePanels(int flags, boolean force) {
    }

    public void overrideActivityPendingAppTransition(boolean keyguardShowing) {
        if (keyguardShowing) {
            try {
                mWindowManagerService.overridePendingAppTransition(null, 0, 0, null);
            } catch (RemoteException e) {
                Log.w(TAG, "Error overriding app transition: " + e);
            }
        }
    }

    /**
     * The LEDs are turned o)ff when the notification panel is shown, even just a little bit.
     * This was added last-minute and is inconsistent with the way the rest of the notifications
     * are handled, because the notification isn't really cancelled.  The lights are just
     * turned off.  If any other notifications happen, the lights will turn back on.  Steve says
     * this is what he wants. (see bug 1131461)
     */
    protected void visibilityChanged(boolean visible) {
        if (mPanelSlightlyVisible != visible) {
            mPanelSlightlyVisible = visible;
            if (!visible) {
//                dismissPopups();
            }
            try {
                if (visible) {
                } else {
                    mBarService.onPanelHidden();
                }
            } catch (RemoteException ex) {
                // Won't fail unless the world has ended.
            }
        }
    }



    protected abstract boolean isCoolShowThemeMode();
    protected abstract boolean isCoverMode();
    protected abstract void haltTicker();
    protected abstract void setAreThereNotifications();
    protected abstract void updateNotifications();
    protected abstract void tick(StatusBarNotification n, boolean firstTime);
    protected abstract void updateExpandedViewPos(int expandedPosition);
    protected abstract boolean shouldDisableNavbarGestures();

    public void setInteracting(int barWindow, boolean interacting) {
        // hook for subclasses
    }

    public void setBouncerShowing(boolean bouncerShowing) {
        mBouncerShowing = bouncerShowing;
    }

    /**
     * @return Whether the security bouncer from Keyguard is showing.
     */
    public boolean isBouncerShowing() {
        return mBouncerShowing;
    }
    /**
     * @return a PackageManger for userId or if userId is < 0 (USER_ALL etc) then
     *         return PackageManager for mContext
     */
    protected PackageManager getPackageManagerForUser(int userId) {
        Context contextForUser = mContext;
        // UserHandle defines special userId as negative values, e.g. USER_ALL
        if (userId >= 0) {
            try {
                // Create a context for the correct user so if a package isn't installed
                // for user 0 we can still load information about the package.
                contextForUser =
                        mContext.createPackageContextAsUser(mContext.getPackageName(),
                        Context.CONTEXT_RESTRICTED,
                        new UserHandle(userId));
            } catch (NameNotFoundException e) {
                // Shouldn't fail to find the package name for system ui.
            }
        }
        return contextForUser.getPackageManager();
    }
	Bitmap drawable2Bitmap(Drawable drawable) {  
        if (drawable instanceof BitmapDrawable) {  
            return ((BitmapDrawable) drawable).getBitmap();  
        } else if (drawable instanceof NinePatchDrawable) {  
            Bitmap bitmap = Bitmap  
                    .createBitmap(  
                            drawable.getIntrinsicWidth(),  
                            drawable.getIntrinsicHeight(),  
                            drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888  
                                    : Bitmap.Config.RGB_565);  
            Canvas canvas = new Canvas(bitmap);  
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(),  
                    drawable.getIntrinsicHeight());  
            drawable.draw(canvas);  
            return bitmap;  
        } else {  
            return null;  
        }  
    }   
	
	public Bitmap blurBitmap(Bitmap bitmap,float radius){  
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
        blurScript.setRadius(radius);  
         
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
	public  int screenWidth = 0;
	public  int screenHeight = 0;
    protected  void getScreenValue(Context mContext){
    	WindowManager wm = (WindowManager) mContext.getSystemService(Context.WINDOW_SERVICE);
    	Display display = wm.getDefaultDisplay();
    	screenWidth = display.getWidth();
    	screenHeight = display.getHeight();
    	float ratio = 110f/(float)screenWidth;
    	Utils.compressRatio = Math.min(Utils.compressRatioMax, ratio);
    	if (Utils.isNeedLog) {
    		Log.w(TAG, "Utils.compressRatio ="+Utils.compressRatio);
    		Log.d(TAG,"screenWidth= " +screenWidth+" screenHeight="+screenHeight);
    	}
    }
	private AlertDialog mAlertDialog = null;
	private AlertDialog mScreenshotAlertDialog = null;
	
    public void hideClearAllButton(){
    	if (mClearAllLayout != null) {
    		mClearAllLayout.setVisibility(View.INVISIBLE);
		}
    	//mClearAllButton.setVisibility(View.GONE);
    }
	
    public static boolean isNetworkAvailable(Context context) { 
        ConnectivityManager cm = (ConnectivityManager) context 
                .getSystemService(Context.CONNECTIVITY_SERVICE); 
        if (cm == null || cm.getActiveNetworkInfo() == null) { 
            return false; 
        } else { 
            return cm.getActiveNetworkInfo().isAvailable(); 
        } 
    }
    
    
    protected NotificationManager mNotificationManager;
    //private static final int LAUNCH_FROM_360_OS_TRAFFIC_MONITOR = 501; 
    boolean isFirstTimeShow = true;
    public void showTrafficNotify(){
    	
    	}
    //init the list of need show notfication's app package
    //private static final String NEED_WHITE_LIST = "NeedWhiteList";
    static boolean hasWhiteList = true;//open or close White List
    
	/*
	 */
	public void getNotificationWhiteList(){
	}
    
    void setNotificationsBanned(String packageName,Context context){
   	 PackageManager pmUser =  context.getPackageManager()/*getPackageManagerForUser(
                sbn.getUser().getIdentifier())*/;
   	int appUid = -1;
       try {
           final ApplicationInfo info = pmUser.getApplicationInfo(packageName,
                   PackageManager.GET_UNINSTALLED_PACKAGES
                           | PackageManager.GET_DISABLED_COMPONENTS);
           if (info != null) {
               appUid = info.uid;
           }
       } catch (Exception e) {
           // app is gone, just show package name and generic icon
       	if (Utils.isNeedLog) {
       		Log.d(TAG, "NameNotFoundException:"+e);
       	}
       }
       try {
			NotificationManager.getService().setNotificationsEnabledForPackage(packageName,appUid,false);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			if (Utils.isNeedLog) {
				Log.d(TAG, "RemoteException:"+e);
			}
		}
       if (Utils.isNeedLog) {
       	Log.d(TAG, "setNotificationsBanned packageName is "+packageName);
       }
   }
    //notification is need show
    private boolean isNeedShow(String packageName){
    	return true;
   }
   
    protected boolean setStatusBarNotificationsVisible() {
    	boolean showNotificaitons = 0 != Settings.Secure.getInt(
                mContext.getContentResolver(), STATUSBAR_SHOW_NOTIFICATIONS, 1);
    	return showNotificaitons;
	}
    
  
    protected void setSecurityPayMode(boolean isSecurityMode) {
		
	}
	public void getDarkModeList(){
	}
}
