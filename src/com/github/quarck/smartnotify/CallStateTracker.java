package com.github.quarck.smartnotify;

import android.content.Context;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;

public class CallStateTracker extends PhoneStateListener
{
	private static CallStateTracker instance = null;
	
	private boolean isOnCall = false;
	
	@Override
	public void onCallStateChanged(int state, String incomingNumber)
	{
		switch (state)
		{
		case TelephonyManager.CALL_STATE_RINGING:
			isOnCall = true;
			break;
		case TelephonyManager.CALL_STATE_OFFHOOK:
			isOnCall = true;
			break;
			
		case TelephonyManager.CALL_STATE_IDLE:
			isOnCall = false;
			break;
		}
	}
	
	private void register(Context ctx)
	{
		TelephonyManager tm = (TelephonyManager) ctx.getSystemService(Context.TELEPHONY_SERVICE);
		tm.listen(this, PhoneStateListener.LISTEN_CALL_STATE);
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
	
	public static boolean isOnCall(Context ctx)
	{
		start(ctx); // would start if required only
		return instance.isOnCall;
	}
}
