package com.github.quarck.smartnotify;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;

public class CallStateTracker extends PhoneStateListener
{
	private static final String TAG = "SmartNotify CallStateTracker";
	
	private static CallStateTracker instance = null;

	private GlobalState global = null;
	
	@Override
	public void onCallStateChanged(int state, String incomingNumber)
	{
		boolean isOnCall = false;
		
		switch (state)
		{
		case TelephonyManager.CALL_STATE_RINGING:
			isOnCall = true;
			Log.d(TAG, "CALL_STATE_RINGING");
			break;
		case TelephonyManager.CALL_STATE_OFFHOOK:
			isOnCall = true;
			Log.d(TAG, "CALL_STATE_OFFHOOK");
			break;
			
		case TelephonyManager.CALL_STATE_IDLE:
			isOnCall = false;
			Log.d(TAG, "CALL_STATE_IDLE");
			break;
		}

		if (global != null)
			global.setIsOnCall(isOnCall);
		else
			Log.e(TAG, "Can't access global state");

	}
	
	private void register(Context ctx)
	{
		Log.d(TAG, "Registering listener");
		
		TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
		tm.listen(this, PhoneStateListener.LISTEN_CALL_STATE);
		
		global = (GlobalState)ctx.getApplicationContext();
	}

	@SuppressWarnings("unused")
	private void unregister(Context ctx)
	{
		TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
		tm.listen(this, PhoneStateListener.LISTEN_NONE);
	}
	
	public static void start(Context ctx)
	{
		synchronized(CallStateTracker.class)
		{
			if (instance == null)
				instance = new CallStateTracker();
			
			instance.register(ctx);			
		}
	}
}
