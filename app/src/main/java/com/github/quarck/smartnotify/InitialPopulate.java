package com.github.quarck.smartnotify;

import android.content.Context;
import android.content.pm.ApplicationInfo;

public class InitialPopulate
{	
	public void populate(Context ctx, PackageSettings pkgSettings)
	{
		CommonAppsRegistry.initRegistry(ctx);
		
		for (ApplicationInfo appInfo : CommonAppsRegistry.getApplications())
		{
			if (!pkgSettings.getIsListed(appInfo.packageName))
			{
				pkgSettings.set(appInfo.packageName, 5*60/*default*/, true);
			}
		}
	}	
}
