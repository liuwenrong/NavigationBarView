package com.example.liuwenrong.navigationbarview;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.ArrayList;

import android.animation.LayoutTransition;
import android.animation.LayoutTransition.TransitionListener;
import android.animation.ObjectAnimator;
import android.animation.TimeInterpolator;
import android.animation.ValueAnimator;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.ContentObserver;
import android.graphics.Color;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.hardware.display.DisplayManager;
import android.os.Handler;
import android.os.Message;
import android.os.UserHandle;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.android.systemui.helper.LogHelper;
import com.android.systemui.statusbar.DelegateViewHelper;
import com.android.systemui.statusbar.phone.BarTransitions;
import com.android.systemui.statusbar.phone.NavigationBarTransitions;
import com.android.systemui.statusbar.phone.NavigationBarViewTaskSwitchHelper;
import com.android.systemui.statusbar.phone.PhoneStatusBar;
import com.android.systemui.statusbar.phone.ShowNavigationHideInfoActivity;
import com.android.systemui.statusbar.policy.CurrentUserTracker;
import com.android.systemui.statusbar.policy.DeadZone;
import com.android.systemui.statusbar.policy.KeyButtonView;

public class BSNavigationBarView extends LinearLayout {
    final static boolean DEBUG = false;
    final static String TAG = "xxx------------PhoneStatusBar/BSNavigationBarView";
    DeadZone mDeadZone;
    public static final String NAVIGATIONBAR_FIXED = "is_navigationbar_fixed";
    public static final String NAVIGATIONBAR_COMBINATION = "virtual_keys_combination_type";

    // slippery nav bar when everything is disabled, e.g. during setup
    final static boolean SLIPPERY_WHEN_DISABLED = true;
    private static boolean isForceShown = false;
    private static boolean mDisableHome = false;
    final Display mDisplay;
    View mCurrentView = null;
    View[][] mRotatedTypeViews = new View[4][4];
    View[] mRotatedViews = new View[4];

    int mBarSize;
    boolean mVertical;
    boolean mScreenOn;

    boolean mShowMenu;
    int mDisabledFlags = 0;
    int mNavigationIconHints = 0;
    int mNavigationType = 0;

    private  int quide = 0;
    
	int navigationBarColor1 = 0xFF000000;
    int greyTransColor1 = 0xFFd4d4d4;
    public static final String NAVIGATIONBAR_BACKGROUND = "virtual_keys_background";
    
    private int mNavigationBarColor = Color.TRANSPARENT;
    
    private Drawable mBackIcon, mBackLandIcon, mBackAltIcon, mBackAltLandIcon;
    private Drawable mRecentIcon;
    private Drawable mRecentLandIcon;

    private NavigationBarViewTaskSwitchHelper mTaskSwitchHelper;
    private DelegateViewHelper mDelegateHelper;
//    private DeadZone mDeadZone;
    private final NavigationBarTransitions mBarTransitions;

    // workaround for LayoutTransitions leaving the nav buttons in a weird state (bug 5549288)
    final static boolean WORKAROUND_INVALID_LAYOUT = true;
    final static int MSG_CHECK_INVALID_LAYOUT = 8686;

    // performs manual animation in sync with layout transitions
    private final NavTransitionListener mTransitionListener = new NavTransitionListener();

    private OnVerticalChangedListener mOnVerticalChangedListener;
    private boolean mIsLayoutRtl;
    private boolean mDelegateIntercepted;
	private NavigationBarSettingObserver NavigationBarObserver;
    private boolean mLayoutTransitionsEnabled;
	private boolean mIsOverSea= false;
	
	private static int mOrientation = Configuration.ORIENTATION_PORTRAIT;
	private StatusBarManager statusBM;

    private class NavTransitionListener implements TransitionListener {
        private boolean mBackTransitioning;
        private boolean mHomeAppearing;
        private long mStartDelay;
        private long mDuration;
        private TimeInterpolator mInterpolator;

        @Override
        public void startTransition(LayoutTransition transition, ViewGroup container,
                View view, int transitionType) {
            if (view.getId() == R.id.back) {
                mBackTransitioning = true;
            } else if (view.getId() == R.id.home && transitionType == LayoutTransition.APPEARING) {
                mHomeAppearing = true;
                mStartDelay = transition.getStartDelay(transitionType);
                mDuration = transition.getDuration(transitionType);
                mInterpolator = transition.getInterpolator(transitionType);
            }
        }

        @Override
        public void endTransition(LayoutTransition transition, ViewGroup container,
                View view, int transitionType) {
            if (view.getId() == R.id.back) {
                mBackTransitioning = false;
            } else if (view.getId() == R.id.home && transitionType == LayoutTransition.APPEARING) {
                mHomeAppearing = false;
            }
        }

        public void onBackAltCleared() {
            // When dismissing ime during unlock, force the back button to run the same appearance
            // animation as home (if we catch this condition early enough).
            if (!mBackTransitioning && getBackButton().getVisibility() == VISIBLE
                    && mHomeAppearing && getHomeButton().getAlpha() == 0) {
                getBackButton().setAlpha(0);
                ValueAnimator a = ObjectAnimator.ofFloat(getBackButton(), "alpha", 0, 1);
                a.setStartDelay(mStartDelay);
                a.setDuration(mDuration);
                a.setInterpolator(mInterpolator);
                a.start();
            }
        }
    }

    private final OnClickListener mImeSwitcherClickListener = new OnClickListener() {
        @Override
        public void onClick(View view) {
            ((InputMethodManager) mContext.getSystemService(Context.INPUT_METHOD_SERVICE))
                    .showInputMethodPicker();
        }
    };

    private class H extends Handler {
        public void handleMessage(Message m) {
            switch (m.what) {
                case MSG_CHECK_INVALID_LAYOUT:
                    final String how = "" + m.obj;
                    final int w = getWidth();
                    final int h = getHeight();
                    final int vw = mCurrentView.getWidth();
                    final int vh = mCurrentView.getHeight();

                    if (h != vh || w != vw) {
                        Log.w(TAG, String.format(
                            "*** Invalid layout in navigation bar (%s this=%dx%d cur=%dx%d)",
                            how, w, h, vw, vh));
                        if (WORKAROUND_INVALID_LAYOUT) {
                            requestLayout();
                        }
                    }
                    break;
            }
        }
    }

    public BSNavigationBarView(Context context, AttributeSet attrs) {
        super(context, attrs);

        LogHelper.logE(TAG, "构造方法");
        mDisplay = ((WindowManager)context.getSystemService(
                Context.WINDOW_SERVICE)).getDefaultDisplay();
//        
//        LogHelper.logE(TAG, "defaultDisplay : "+mDisplay.getName());
        
//        DisplayManager mDisplayManager = (DisplayManager) mContext.getSystemService(Context.DISPLAY_SERVICE);
//		Display[] displays = mDisplayManager.getDisplays();
//		LogHelper.logE(TAG, "display.length=="+displays.length);
//		if(displays.length < 2){
//			mDisplay = displays[0];
//		}else{
//			mDisplay = displays[1];
//		}

        final Resources res = getContext().getResources();
        mBarSize = res.getDimensionPixelSize(R.dimen.navigation_bar_size);
        mVertical = false;
        mShowMenu = false;
        mDelegateHelper = new DelegateViewHelper(this);
        mTaskSwitchHelper = new NavigationBarViewTaskSwitchHelper(context);
//        mIsOverSea = QuickSettingsModel.getMiscInterfaceResult("is_support_oversea");
        getIcons(res);

        mBarTransitions = new NavigationBarTransitions(this);
        
        statusBM = (StatusBarManager) mContext
                .getSystemService(Context.STATUS_BAR_SERVICE);
        
        //setBackgroundColor(Color.BLACK);
        register();
    }

    public BarTransitions getBarTransitions() {
        return mBarTransitions;
    }

    public void setDelegateView(View view) {
        mDelegateHelper.setDelegateView(view);
    }
    PhoneStatusBar mPhoneStatusBar;
    public void setBar(PhoneStatusBar phoneStatusBar) {
    	mPhoneStatusBar = phoneStatusBar;
        mTaskSwitchHelper.setBar(phoneStatusBar);
        mDelegateHelper.setBar(phoneStatusBar);
    }

    public void setOnVerticalChangedListener(OnVerticalChangedListener onVerticalChangedListener) {
        mOnVerticalChangedListener = onVerticalChangedListener;
        notifyVerticalChangedListener(mVertical);
    }

    private void setNavigationBarColor(){
//    	int colorType = CurrentUserTracker.getIntForCurrentUser(NAVIGATIONBAR_BACKGROUND, 1);
//		LogHelper.sv(TAG,"........PhoneStatusBar...........NAVIGATIONBAR_BACKGROUND colorType="+colorType);
//		if(mBarTransitions == null) return;
//		if(colorType == 1){
//		
//
//			mNavigationBarColor = navigationBarColor1;
//		}else if(colorType == 2){
//
//			mNavigationBarColor = greyTransColor1;
//		}else{
//
//			mNavigationBarColor = navigationBarColor1;
//		}
//
//		mBarTransitions.refreshVirtualColor(mNavigationBarColor);
//		
//		LogHelper.sv(TAG,"........PhoneStatusBar...........NAVIGATIONBAR_BACKGROUND is change");
    }
    
    float oldy,newy,deltay;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        
//        if (!QuickSettingLauncher.isPopuped && mOrientation != Configuration.ORIENTATION_LANDSCAPE){
//            QuickSettingLauncher.getInstance(mContext).dispatchTouchListener(event);
//        }
//        LogHelper.logE(TAG, "onTouchEvent");
        initDownStates(event);
//        if (!mDelegateIntercepted && mTaskSwitchHelper.onTouchEvent(event)) {
//            return true;
//        }
//        if (mDeadZone != null && event.getAction() == MotionEvent.ACTION_OUTSIDE) {
//            mDeadZone.poke(event);
//        }
//        if (mDelegateHelper != null && mDelegateIntercepted) {
//            boolean ret = mDelegateHelper.onInterceptTouchEvent(event);
//            if (ret) return true;
//        }
        return super.onTouchEvent(event);
    }

    private void initDownStates(MotionEvent ev) {
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            mDelegateIntercepted = false;
        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent event) {
//        if (!QuickSettingLauncher.isPopuped && mOrientation != Configuration.ORIENTATION_LANDSCAPE){
//            QuickSettingLauncher.getInstance(mContext).dispatchTouchListener(event);
//        }
//    	LogHelper.logE(TAG, "onInterceptTouchEvent");
        initDownStates(event);
//        boolean intercept = mTaskSwitchHelper.onInterceptTouchEvent(event);
//        if (!intercept) {
//            mDelegateIntercepted = mDelegateHelper.onInterceptTouchEvent(event);
//            intercept = mDelegateIntercepted;
//        } else {
//            MotionEvent cancelEvent = MotionEvent.obtain(event);
//            cancelEvent.setAction(MotionEvent.ACTION_CANCEL);
//            mDelegateHelper.onInterceptTouchEvent(cancelEvent);
//            cancelEvent.recycle();
//        }
//        return intercept;
        return false;  // not interceptTouchEvent
    }

    private H mHandler = new H();

    public View getCurrentView() {
        return mCurrentView;
    }

    public View getRecentsButton() {
        View v = mCurrentView.findViewById(R.id.recent_apps);
        if (v == null){
        	v = mRotatedViews[0].findViewById(R.id.recent_apps);
        }
//        LogHelper.logE(TAG, "recent_apps_view" + v.toString());
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

    private void getIcons(Resources res) {
        mBackIcon = res.getDrawable(R.drawable.ic_sysbar_back);
//        mBackLandIcon = res.getDrawable(R.drawable.ic_sysbar_back_land);
//        mBackAltIcon = res.getDrawable(R.drawable.ic_sysbar_back_ime);
//        mBackAltLandIcon = res.getDrawable(R.drawable.ic_sysbar_back_ime);
        mRecentIcon = res.getDrawable(R.drawable.ic_sysbar_recent);
//        mRecentLandIcon = res.getDrawable(R.drawable.ic_sysbar_recent_land);
    }

    @Override
    public void setLayoutDirection(int layoutDirection) {
        getIcons(getContext().getResources());

        super.setLayoutDirection(layoutDirection);
    }

    public void notifyScreenOn(boolean screenOn) {
        mScreenOn = screenOn;
        setDisabledFlags(mDisabledFlags, true);
    }

    public void setNavigationIconHints(int hints) {
        setNavigationIconHints(hints, false,mNavigationType);
    }

    private void setNavigationIconHints(int hints, boolean force,int combileType) {
        if (!force && hints == mNavigationIconHints && combileType == mNavigationType) return;
        final boolean backAlt = (hints & StatusBarManager.NAVIGATION_HINT_BACK_ALT) != 0;
        if ((mNavigationIconHints & StatusBarManager.NAVIGATION_HINT_BACK_ALT) != 0 && !backAlt) {
            mTransitionListener.onBackAltCleared();
        }
        if (DEBUG) {
            android.widget.Toast.makeText(getContext(),
                "Navigation icon hints = " + hints,
                500).show();
        }

        mNavigationIconHints = hints;

        ((ImageView)getBackButton()).setImageDrawable(backAlt
                ? (mVertical ? mBackAltLandIcon : mBackAltIcon)
                : (mVertical ? mBackLandIcon : mBackIcon));

        ((ImageView)getRecentsButton()).setImageDrawable(mVertical ? mRecentLandIcon : mRecentIcon);

        final boolean showImeButton = ((hints & StatusBarManager.NAVIGATION_HINT_IME_SHOWN) != 0);
        getImeSwitchButton().setVisibility(showImeButton ? View.VISIBLE : View.INVISIBLE);
        // Update menu button in case the IME state has changed.
        setMenuVisibility(mShowMenu, true);


        setDisabledFlags(mDisabledFlags, true);
    }

    public void setDisabledFlags(int disabledFlags) {
        setDisabledFlags(disabledFlags, false);
    }

    public void setDisabledFlags(int disabledFlags, boolean force) {
        if (!force && mDisabledFlags == disabledFlags) return;

        mDisabledFlags = disabledFlags;

        final boolean disableHome = ((disabledFlags & View.STATUS_BAR_DISABLE_HOME) != 0);
        final boolean disableRecent = ((disabledFlags & View.STATUS_BAR_DISABLE_RECENT) != 0);
        final boolean disableBack = ((disabledFlags & View.STATUS_BAR_DISABLE_BACK) != 0)
                && ((mNavigationIconHints & StatusBarManager.NAVIGATION_HINT_BACK_ALT) == 0);
        final boolean disableSearch = ((disabledFlags & View.STATUS_BAR_DISABLE_SEARCH) != 0);

        if (SLIPPERY_WHEN_DISABLED) {
            setSlippery(disableHome && disableRecent && disableBack && disableSearch);
        }

		ViewGroup navButtons = null;
		if (mCurrentView != null) {
			navButtons = (ViewGroup) mCurrentView.findViewById(R.id.nav_buttons);
		}
        if (navButtons != null) {
            LayoutTransition lt = navButtons.getLayoutTransition();
            if (lt != null) {
                if (!lt.getTransitionListeners().contains(mTransitionListener)) {
                    lt.addTransitionListener(mTransitionListener);
                }
                if (!mScreenOn && mCurrentView != null) {
                    lt.disableTransitionType(
                            LayoutTransition.CHANGE_APPEARING |
                            LayoutTransition.CHANGE_DISAPPEARING |
                            LayoutTransition.APPEARING |
                            LayoutTransition.DISAPPEARING);
                }
            }
        }
		if (mCurrentView != null) {
			getBackButton().setVisibility(disableBack ? View.INVISIBLE : View.VISIBLE);
			getHomeButton().setVisibility(disableHome ? View.INVISIBLE : View.VISIBLE);
			getRecentsButton().setVisibility(disableRecent ? View.INVISIBLE : View.VISIBLE);
			getHideButton().setVisibility(disableHome || isForceShown ? View.INVISIBLE : View.VISIBLE);
		}
		mDisableHome = disableHome;
		if (mCurrentView != null) {
			showOutsideMenu();
		}
        mBarTransitions.applyBackButtonQuiescentAlpha(mBarTransitions.getMode(), true /*animate*/);
    }

    private void setVisibleOrGone(View view, boolean visible) {
        if (view != null) {
            view.setVisibility(visible ? VISIBLE : GONE);
        }
    }
private void showOutsideMenu(){
    	if(mNavigationType == 0 || mNavigationType ==3){
    		if(getYulongMenuButton() != null){
    			getYulongMenuButton().setVisibility(mShowMenu && !mDisableHome ? View.VISIBLE : View.INVISIBLE);
    		}
    	}else{
    		if(getYulongMenuButton() != null){
    			getYulongMenuButton().setVisibility(!mDisableHome ? View.VISIBLE : View.INVISIBLE);
    		}
    	}
    }

    public void setSlippery(boolean newSlippery) {
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
        if (lp != null) {
            boolean oldSlippery = (lp.flags & WindowManager.LayoutParams.FLAG_SLIPPERY) != 0;
            if (!oldSlippery && newSlippery) {
                lp.flags |= WindowManager.LayoutParams.FLAG_SLIPPERY;
            } else if (oldSlippery && !newSlippery) {
                lp.flags &= ~WindowManager.LayoutParams.FLAG_SLIPPERY;
            } else {
                return;
            }
            WindowManager wm = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
            wm.updateViewLayout(this, lp);
        }
    }

    public void setMenuVisibility(final boolean show) {
        setMenuVisibility(show, false);
    }

    public void setMenuVisibility(final boolean show, final boolean force) {
        if (!force && mShowMenu == show) return;

        mShowMenu = show;

        // Only show Menu if IME switcher not shown.
        final boolean shouldShow = mShowMenu &&
                ((mNavigationIconHints & StatusBarManager.NAVIGATION_HINT_IME_SHOWN) == 0);
        getMenuButton().setVisibility(shouldShow ? View.VISIBLE : View.INVISIBLE);
        showOutsideMenu();
    }

	final int typeId[] = {R.id.type0,R.id.type1,R.id.type2,R.id.type3};
    @Override
    public void onFinishInflate() {
    	super.onFinishInflate();
    	for(int type =0 ; type < 4; type++){    	
	        mRotatedTypeViews[Surface.ROTATION_0][type] =
	        mRotatedTypeViews[Surface.ROTATION_180][type] = findViewById(R.id.rot0).findViewById(typeId[type]);
	        mRotatedViews[Surface.ROTATION_0] =
	        mRotatedViews[Surface.ROTATION_180]	= findViewById(R.id.rot0);	
	        
	        mRotatedTypeViews[Surface.ROTATION_90][type] = findViewById(R.id.rot90).findViewById(typeId[type]);		        
	        mRotatedTypeViews[Surface.ROTATION_270][type] = mRotatedTypeViews[Surface.ROTATION_90][type];
	        mRotatedViews[Surface.ROTATION_90] =
	        mRotatedViews[Surface.ROTATION_270]	= findViewById(R.id.rot90);	
	        
    	}
    	typeNavigation();
        mCurrentView = mRotatedTypeViews[Surface.ROTATION_0][mNavigationType];
//    	mCurrentView = mRotatedViews[Surface.ROTATION_0];

        getImeSwitchButton().setOnClickListener(mImeSwitcherClickListener);

        updateRTLOrder();
    	Handler handler = new Handler();
    	NavigationBarObserver = new NavigationBarSettingObserver(handler);
    	NavigationBarObserver.startObserving();        
    	setNavigationBarColor();
    	getHideButton().setOnClickListener(mHideClickListener);
    }
    
    
private void typeNavigation(){
	 final int settingType = CurrentUserTracker.getIntForCurrentUser(NAVIGATIONBAR_COMBINATION, 0);

     int combileType = 0;
     switch(settingType){
     case 1:
     	combileType = 2;
     	break;
     case 2:
     	combileType = 1;
     	break;
     case 3:
     	combileType = 0;
     	break;
     case 4:
     	combileType = 3;
     	break;
     default:
     	combileType = 0;
     }
     if(combileType < 0 || combileType > 3)
     	combileType = 0;
     mNavigationType = combileType;
}

    public boolean isVertical() {
        return mVertical;
    }

	private class NavigationBarSettingObserver extends ContentObserver {

		public NavigationBarSettingObserver(Handler handler) {
			super(handler);
		}
		
		public void startObserving() {
			CurrentUserTracker.registerContentObserver(NAVIGATIONBAR_FIXED, true, this);
			
			CurrentUserTracker.registerContentObserver(NAVIGATIONBAR_COMBINATION, true, this);
			
			CurrentUserTracker.registerContentObserver(NAVIGATIONBAR_BACKGROUND, true, this);
			
			LogHelper.sd(TAG,"...................NAVIGATIONBAR_COMBINATION is startObserving()");
			update();		
		}
		

		@Override
		public void onChange(boolean selfChange) {
			update();
			reorient();
			setNavigationBarColor();
			super.onChange(selfChange);
		}

	}
	
	
	private void update(){
		if(mPhoneStatusBar != null){
			mPhoneStatusBar.repositionNavigationBar();
		}
		 int isfixed = CurrentUserTracker.getIntForCurrentUser(NAVIGATIONBAR_FIXED, 0);
		 isForceShown = (isfixed == 1) ? true : false;
		 LogHelper.sv(TAG,NAVIGATIONBAR_FIXED + " = " + isfixed + " isForceShown = " + isForceShown);
		 getHideButton().setVisibility(isForceShown ? View.INVISIBLE : View.VISIBLE);
		 reorient();	
	}
	
	  private View.OnClickListener mHideClickListener = new View.OnClickListener() {
	        public void onClick(View v) {
	            LogHelper.logE(TAG, "605--mHideClickListener hide navigationbar");
	            //mNavigationBarView.setVisibility(View.GONE);
	            quide = CurrentUserTracker.getIntForCurrentUser("hideNavigationBar.quide", 0);
	           //fangzhengru
	            Intent localIntent = new Intent("com.yulong.navigationbar.statuschange");
	            localIntent.putExtra("hideNavigationBar", false);
	            mContext.sendBroadcast(localIntent);
	            
	            if( quide== 0){
	            	if(mIsOverSea){
	            		Toast.makeText(mContext, mContext.getResources().getString(R.string.over_sea_hideNavigationBar_info), Toast.LENGTH_SHORT).show();
	            	}else{
	            	  startActivityForQuide();
	            	}
	            	CurrentUserTracker.putIntForCurrentUser("hideNavigationBar.quide",1);
	            }
	        }
	    };
	    // // 隐藏系统NavigationBar 之后开启指导页面, 指导中启动Act 
	    private void startActivityForQuide(){
	    	Intent newintent = new Intent();
			newintent.setClass(mContext, ShowNavigationHideInfoActivity.class);
			newintent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
			mContext.startActivityAsUser(newintent,UserHandle.CURRENT);
	    }
	//重新定位
    public void reorient() {
        final int settingType = CurrentUserTracker.getIntForCurrentUser(NAVIGATIONBAR_COMBINATION, 0);
//        //settingType
//        //combileType 
        LogHelper.logE(TAG, "NavigationBar 定位布局,选择类型type");
        int combileType = 0;
        switch(settingType){
        case 1:
        	combileType = 2;
        	break;
        case 2:
        	combileType = 1;
        	break;
        case 3:
        	combileType = 0;
        	break;
        case 4:
        	combileType = 3;
        	break;
        default:
        	combileType = 0;
        }
        if(combileType < 0 || combileType > 3)
        	combileType = 0;
        mNavigationType = combileType;
//        
//        final int rot = mDisplay.getRotation();
        final int rot = mDisplay.getRotation();
        LogHelper.logE(TAG, " reorient  rot:" + rot + " (virtual_keys_combination_type)settingType:" + settingType + " combileType:" + combileType);
        for (int i=0; i<4; i++) {
        	mRotatedViews[i].setVisibility(View.GONE);
        	for(int type=0; type < 4; type++){
        		mRotatedTypeViews[i][type].setVisibility(View.INVISIBLE);
        	}
        }
        mCurrentView = mRotatedTypeViews[rot][combileType];
        mRotatedViews[rot].setVisibility(View.VISIBLE);
        mCurrentView.setVisibility(View.VISIBLE);

        getImeSwitchButton().setOnClickListener(mImeSwitcherClickListener);
        getHideButton().setOnClickListener(mHideClickListener);
        mDeadZone = (DeadZone) mCurrentView.findViewById(R.id.deadzone);

        // force the low profile & disabled states into compliance
        mBarTransitions.init(mVertical);
        setDisabledFlags(mDisabledFlags, true /* force */);
        setMenuVisibility(mShowMenu, true /* force */);

        LogHelper.logE(TAG, "reorient(): rot=" + mDisplay.getRotation());

        // swap to x coordinate if orientation is not in vertical
        if (mDelegateHelper != null) {
            mDelegateHelper.setSwapXY(mVertical);
        }
        updateTaskSwitchHelper();
        setNavigationIconHints(mNavigationIconHints,true,mNavigationType);
    }
    
    
    private void register(){
        final IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_USER_SWITCHED);
        mContext.registerReceiver(new BroadcastReceiver(){

			@Override
			public void onReceive(Context context, Intent intent) {
				 final String action = intent.getAction();
				 Log.d(TAG, "...........register() action = " + action);
     
					update();
					setNavigationBarColor();
			}
        	
        }, filter);
    }

    private void updateTaskSwitchHelper() {
//        boolean isRtl = (getLayoutDirection() == View.LAYOUT_DIRECTION_RTL);
//        mTaskSwitchHelper.setBarState(mVertical, isRtl);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        super.onLayout(changed, l, t, r, b);
        mDelegateHelper.setInitialTouchRegion(getHideButton(),getHomeButton(), getBackButton(), getRecentsButton());
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        LogHelper.logE(TAG, String.format(
                    "onSizeChanged: (%dx%d) old: (%dx%d)", w, h, oldw, oldh));

        final boolean newVertical = w > 0 && h > w;
        if (newVertical != mVertical) {
            mVertical = newVertical;
            //Log.v(TAG, String.format("onSizeChanged: h=%d, w=%d, vert=%s", h, w, mVertical?"y":"n"));
            reorient();
            notifyVerticalChangedListener(newVertical);
        }

        postCheckForInvalidLayout("sizeChanged");
        super.onSizeChanged(w, h, oldw, oldh);
    }

    private void notifyVerticalChangedListener(boolean newVertical) {
        if (mOnVerticalChangedListener != null) {
            mOnVerticalChangedListener.onVerticalChanged(newVertical);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateRTLOrder();
        updateTaskSwitchHelper();
        
        //yulong add: add for orientation judge,shenyupeng,2016.03.07
        mOrientation = newConfig.orientation;
        //yulong end
    }

    /**
     * In landscape, the LinearLayout is not auto mirrored since it is vertical. Therefore we
     * have to do it manually
     */
    private void updateRTLOrder() {
        boolean isLayoutRtl = getResources().getConfiguration()
                .getLayoutDirection() == LAYOUT_DIRECTION_RTL;
        if (mIsLayoutRtl != isLayoutRtl) {

            // We swap all children of the 90 and 270 degree layouts, since they are vertical
            View rotation90 = mRotatedViews[Surface.ROTATION_90];
            swapChildrenOrderIfVertical(rotation90.findViewById(R.id.nav_buttons));

            View rotation270 = mRotatedViews[Surface.ROTATION_270];
            if (rotation90 != rotation270) {
                swapChildrenOrderIfVertical(rotation270.findViewById(R.id.nav_buttons));
            }
            mIsLayoutRtl = isLayoutRtl;
        }
    }


    /**
     * Swaps the children order of a LinearLayout if it's orientation is Vertical
     *
     * @param group The LinearLayout to swap the children from.
     */
    private void swapChildrenOrderIfVertical(View group) {
        if (group instanceof LinearLayout) {
            LinearLayout linearLayout = (LinearLayout) group;
            if (linearLayout.getOrientation() == VERTICAL) {
                int childCount = linearLayout.getChildCount();
                ArrayList<View> childList = new ArrayList<View>(childCount);
                for (int i = 0; i < childCount; i++) {
                    childList.add(linearLayout.getChildAt(i));
                }
                linearLayout.removeAllViews();
                for (int i = childCount - 1; i >= 0; i--) {
                    linearLayout.addView(childList.get(i));
                }
            }
        }
    }

    /*
    @Override
    protected void onLayout (boolean changed, int left, int top, int right, int bottom) {
        if (DEBUG) Log.d(TAG, String.format(
                    "onLayout: %s (%d,%d,%d,%d)",
                    changed?"changed":"notchanged", left, top, right, bottom));
        super.onLayout(changed, left, top, right, bottom);
    }

    // uncomment this for extra defensiveness in WORKAROUND_INVALID_LAYOUT situations: if all else
    // fails, any touch on the display will fix the layout.
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (DEBUG) Log.d(TAG, "onInterceptTouchEvent: " + ev.toString());
        if (ev.getAction() == MotionEvent.ACTION_DOWN) {
            postCheckForInvalidLayout("touch");
        }
        return super.onInterceptTouchEvent(ev);
    }
    */


    private String getResourceName(int resId) {
        if (resId != 0) {
            final android.content.res.Resources res = getContext().getResources();
            try {
                return res.getResourceName(resId);
            } catch (android.content.res.Resources.NotFoundException ex) {
                return "(unknown)";
            }
        } else {
            return "(null)";
        }
    }

    private void postCheckForInvalidLayout(final String how) {
        mHandler.obtainMessage(MSG_CHECK_INVALID_LAYOUT, 0, 0, how).sendToTarget();
    }

    private static String visibilityToString(int vis) {
        switch (vis) {
            case View.INVISIBLE:
                return "INVISIBLE";
            case View.GONE:
                return "GONE";
            default:
                return "VISIBLE";
        }
    }

    //丢弃, 垃圾, 仓库
    public void dump(FileDescriptor fd, PrintWriter pw, String[] args) {
        pw.println("NavigationBarView {");
        final Rect r = new Rect();
        final Point size = new Point();
        mDisplay.getRealSize(size);

        pw.println(String.format("      this: " + PhoneStatusBar.viewInfo(this)
                        + " " + visibilityToString(getVisibility())));

        getWindowVisibleDisplayFrame(r);
        final boolean offscreen = r.right > size.x || r.bottom > size.y;
        pw.println("      window: "
                + r.toShortString()
                + " " + visibilityToString(getWindowVisibility())
                + (offscreen ? " OFFSCREEN!" : ""));

        pw.println(String.format("      mCurrentView: id=%s (%dx%d) %s",
                        getResourceName(mCurrentView.getId()),
                        mCurrentView.getWidth(), mCurrentView.getHeight(),
                        visibilityToString(mCurrentView.getVisibility())));

        pw.println(String.format("      disabled=0x%08x vertical=%s menu=%s",
                        mDisabledFlags,
                        mVertical ? "true" : "false",
                        mShowMenu ? "true" : "false"));

        dumpButton(pw, "back", getBackButton());
        dumpButton(pw, "home", getHomeButton());
        dumpButton(pw, "rcnt", getRecentsButton());
        dumpButton(pw, "menu", getMenuButton());

        pw.println("    }");
    }

    private static void dumpButton(PrintWriter pw, String caption, View button) {
        pw.print("      " + caption + ": ");
        if (button == null) {
            pw.print("null");
        } else {
            pw.print(PhoneStatusBar.viewInfo(button)
                    + " " + visibilityToString(button.getVisibility())
                    + " alpha=" + button.getAlpha()
                    );
            if (button instanceof KeyButtonView) {
                pw.print(" drawingAlpha=" + ((KeyButtonView)button).getDrawingAlpha());
                pw.print(" quiescentAlpha=" + ((KeyButtonView)button).getQuiescentAlpha());
            }
        }
        pw.println();
    }

    public interface OnVerticalChangedListener {
        void onVerticalChanged(boolean isVertical);
    }
    
    private void setLayoutTransitionsEnabled(boolean enabled) {
        mLayoutTransitionsEnabled = enabled;
        ViewGroup navButtons = (ViewGroup) mCurrentView.findViewById(R.id.nav_buttons);
        LayoutTransition lt = navButtons.getLayoutTransition();
        if (lt != null) {
            if (enabled) {
                lt.enableTransitionType(LayoutTransition.APPEARING);
                lt.enableTransitionType(LayoutTransition.DISAPPEARING);
                lt.enableTransitionType(LayoutTransition.CHANGE_APPEARING);
                lt.enableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
            } else {
                lt.disableTransitionType(LayoutTransition.APPEARING);
                lt.disableTransitionType(LayoutTransition.DISAPPEARING);
                lt.disableTransitionType(LayoutTransition.CHANGE_APPEARING);
                lt.disableTransitionType(LayoutTransition.CHANGE_DISAPPEARING);
            }
        }
    }
    
    private void setUseFadingAnimations(boolean useFadingAnimations) {
        WindowManager.LayoutParams lp = (WindowManager.LayoutParams) getLayoutParams();
        if (lp != null) {
            boolean old = lp.windowAnimations != 0;
            if (!old && useFadingAnimations) {
                lp.windowAnimations = R.style.Animation_NavigationBarFadeIn;
            } else if (old && !useFadingAnimations) {
                lp.windowAnimations = 0;
            } else {
                return;
            }
            WindowManager wm = (WindowManager)getContext().getSystemService(Context.WINDOW_SERVICE);
            wm.updateViewLayout(this, lp);
        }
    }
    
    public void setWakeAndUnlocking(boolean wakeAndUnlocking) {
        setUseFadingAnimations(wakeAndUnlocking);
        setLayoutTransitionsEnabled(!wakeAndUnlocking);
    }

}
