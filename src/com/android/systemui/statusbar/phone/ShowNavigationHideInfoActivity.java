package com.android.systemui.statusbar.phone;

import android.app.Activity;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;

import com.example.liuwenrong.navigationbarview.R;


public class ShowNavigationHideInfoActivity extends Activity {
	private static final String TAG = "ShowNavigationHideInfoActivity";
	private ImageView mImgView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.shownavigationhideinfo_activity);
        this.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
    }
    
    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
	   	if (event.getAction() == MotionEvent.ACTION_UP){
	    	finish();
	    	return true;
	    }
	    super.onTouchEvent(event);
	    return true;
    }
}
