package com.github.quarck.smartnotify

import android.content.Context
import android.content.pm.ApplicationInfo

class InitialPopulate
{
	fun populate(ctx: Context, pkgSettings: PackageSettings)
	{
		CommonAppsRegistry.initRegistry(ctx)

		for (appInfo in CommonAppsRegistry.applications!!)
		{
			if (!pkgSettings.getIsListed(appInfo!!.packageName))
			{
				pkgSettings.set(appInfo.packageName, 5 * 60/*default*/, true)
			}
		}
	}
}
