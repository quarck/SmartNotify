package com.github.quarck.smartnotify;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

public class CommonAppsRegistry
{
	private static final String[] packages = {
		"com.google.android.calendar",
		"com.google.android.talk",
		"com.google.android.apps.plus",
		"com.android.phone",
		"com.skype.raider",
		"com.google.android.email",
		"com.google.android.gm",
		"com.facebook.katana",
		"com.facebook.orca",
		"com.viber.voip",
		"com.whatsapp",
		"com.vkontakte.android",
		"com.csipsimple",
		"unibilling.sipfone",
		"org.sipdroid.sipua",
		"com.yahoo.mobile.client.android.im",
		"com.instagram.android",
		"com.bbm",
		"com.linkedin.android",
		"com.fsck.k9",
		"org.kman.AquaMail",
		"net.daum.android.solmail",
		"de.shapeservices.impluslite",
		"de.shapeservices.implusfull",
		"com.sec.chaton"
	};
	
	private static int numFoundApps = 0;
	
	private static boolean[] presenseMap = null;

	private static ApplicationInfo[] applicationInfos = null;
	
	
	public static void initRegistry(Context ctx)
	{
		synchronized(CommonAppsRegistry.class)
		{
			if (presenseMap != null && applicationInfos != null)
				return;
			
			presenseMap = new boolean[packages.length];
			applicationInfos = new ApplicationInfo[packages.length];
			
			PackageManager packageManager = ctx.getPackageManager();

			for (int i = 0; i < packages.length; ++i)
			{
				String pkg = packages[i];
				try 
				{
					ApplicationInfo app = packageManager.getApplicationInfo(pkg, 0/*PackageManager.GET_META_DATA*/);
					
					if (app != null)
					{
						presenseMap[i] = true;
						applicationInfos[i] = app;
						numFoundApps ++;
					}
					else
					{
						presenseMap[i] = false;
					}	
				}
				catch (Exception ex)
				{
				}
			}
		}
	}

	public static int getNumApplications()
	{
		synchronized(CommonAppsRegistry.class)
		{
			return numFoundApps;
		}
	}
	
	public static ApplicationInfo[] getApplications()
	{
		try
		{
			ApplicationInfo[] ret = new ApplicationInfo[numFoundApps];
			
			synchronized(CommonAppsRegistry.class)
			{
				int idx = 0;
				
				for(ApplicationInfo ai : applicationInfos)
				{
					if (ai == null)
						continue;
					
					ret[idx++] = ai;
				}
			}
			
			return ret;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
		return null;
	}
}
