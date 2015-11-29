/*
 * Copyright (c) 2014, Sergey Parshin, quarck@gmail.com
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of developer (Sergey Parshin) nor the
 *       names of other project contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */


package com.github.quarck.smartnotify

import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.HashMap

import android.app.ActionBar
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.drawable.Drawable
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.CheckBox

class EditApplicationsActivity : Activity()
{

	private var listApplications: ListView? = null

	//private ArrayList<AppSelectionInfo> listApps = new ArrayList<AppSelectionInfo>();

	private var menuTitles: Array<String>? = null

	private class Applications(
		private val handledApps: ArrayList<AppSelectionInfo>,
		private var recentApps: ArrayList<AppSelectionInfo>,
		private val commonApps: ArrayList<AppSelectionInfo>,
		private val visibleApps: ArrayList<AppSelectionInfo>)
	{

		val all: ArrayList<ArrayList<AppSelectionInfo>>

		init
		{
			all = ArrayList<ArrayList<AppSelectionInfo>>()

			all.add(handledApps)
			all.add(recentApps)
			all.add(commonApps)
			all.add(visibleApps)
		}

		fun setRecent(recent: ArrayList<AppSelectionInfo>)
		{
			recentApps = recent
			all[1] = recentApps
		}

		val allFlat: ArrayList<AppSelectionInfo>
			get()
			{
				val ret = ArrayList<AppSelectionInfo>()

				for (list in all)
					ret.addAll(list)

				return ret
			}
	}

	private inner class AppSelectionInfo
	{
		internal var loadComplete = false

		internal var name: String? = null
		internal var packageName: String = ""
		internal var icon: Drawable? = null

		internal var app: ApplicationInfo? = null
	}

	private var app1stLoader: LoadApplications1stStageTask? = null
	private var app2ndLoader: LoadApplications2ndStageTask? = null


	private var pkgSettings: PackageSettings? = null

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)

		Lw.d(TAG, "onCreate")

		pkgSettings = PackageSettings(this)

		setContentView(R.layout.activity_edit_apps)

		listApplications = findViewById(R.id.listAddApplications) as ListView

		val actionBar = actionBar
		actionBar.setHomeButtonEnabled(true)
		actionBar.title = "Show/Hide applications"

		listApplications!!.emptyView = findViewById(R.id.progressBarLoading) as ProgressBar
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean
	{
		when (item.itemId)
		{
			android.R.id.home ->
			{
				// app icon in action bar clicked; goto parent activity.
				this.finish()
				return true
			}
			else -> return super.onOptionsItemSelected(item)
		}
	}


	private fun loadApplications()
	{
		CommonAppsRegistry.initRegistry(this)

		var onlyRefreshRecent = false
		synchronized (EditApplicationsActivity::class.java) {
			if (listApps != null)
				onlyRefreshRecent = !forceReloadApplications
			forceReloadApplications = false
		}

		val pkgSettings = PackageSettings(this)

		val packageManager = packageManager

		val handledApps = ArrayList<AppSelectionInfo>()
		val recentApps = ArrayList<AppSelectionInfo>()
		val commonApps = ArrayList<AppSelectionInfo>()
		val visibleApps = ArrayList<AppSelectionInfo>()


		val alreadyLoadedAppsHash = HashMap<String, Int>()

		Lw.d(TAG, "Loading applications")

		Lw.d(TAG, "Loading configured applications first")

		for (pkg in pkgSettings.allPackages)
		{
			if (alreadyLoadedAppsHash.containsKey(pkg.packageName))
				continue // already loaded by somebody else (by whooom??)

			try
			{
				val asi = AppSelectionInfo()

				asi.packageName = pkg.packageName!!
				asi.app = packageManager.getApplicationInfo(pkg.packageName, 0/*PackageManager.GET_META_DATA*/)
				asi.name = packageManager.getApplicationLabel(asi.app).toString()
				handledApps.add(asi)
				alreadyLoadedAppsHash.put(asi.packageName, 1)
			}
			catch (e: NameNotFoundException)
			{
				// TODO Auto-generated catch block
				e.printStackTrace()
			}

		}

		Lw.d(TAG, "Loading Recent applications below:")

		for (recent in NotificationReceiverService.getRecentNotifications()!!)
		{
			Lw.d(TAG, "Recent app: " + recent)

			if (alreadyLoadedAppsHash.containsKey(recent))
				continue // already loaded by somebody else

			val asi = AppSelectionInfo()

			asi.packageName = recent

			try
			{
				val app = packageManager.getApplicationInfo(recent, 0/*PackageManager.GET_META_DATA*/)

				asi.name = packageManager.getApplicationLabel(app).toString()
				asi.app = app
				recentApps.add(asi)
				alreadyLoadedAppsHash.put(asi.packageName, 1)
			}
			catch (e: NameNotFoundException)
			{
				// TODO Auto-generated catch block
				e.printStackTrace()
			}

		}

		Lw.d(TAG, "Loading COMMON applications below:")
		for (appInfo in CommonAppsRegistry.applications!!)
		{
			Lw.d(TAG, "Recent app: " + appInfo!!.packageName)

			if (alreadyLoadedAppsHash.containsKey(appInfo!!.packageName))
				continue // already loaded by somebody else

			val asi = AppSelectionInfo()

			asi.packageName = appInfo!!.packageName

			asi.name = packageManager.getApplicationLabel(appInfo).toString()
			asi.app = appInfo

			if (asi.packageName == Consts.packageName)
				commonApps.add(asi)
			alreadyLoadedAppsHash.put(asi.packageName, 1)
		}

		if (!onlyRefreshRecent)
		{
			Lw.d(TAG, "Loading all other applications")

			for (app in packageManager.getInstalledApplications(0))
			{
				val asi = AppSelectionInfo()

				if (app.packageName == null)
					continue

				if (alreadyLoadedAppsHash.containsKey(app.packageName))
					continue // already loaded by somebody else

				asi.packageName = app.packageName

				alreadyLoadedAppsHash.put(asi.packageName, 1)

				asi.name = packageManager.getApplicationLabel(app).toString()
				asi.app = app

				val launchActivity = packageManager.getLaunchIntentForPackage(app.packageName)

				if (launchActivity != null)
				{
					visibleApps.add(asi)
				}
			}
		}

		val comparator = Comparator<com.github.quarck.smartnotify.EditApplicationsActivity.AppSelectionInfo>
		{
			app1, app2 -> app1.name!!.compareTo(app2.name!!)
		}

		if (!onlyRefreshRecent)
		{
			Collections.sort(handledApps, comparator)
			Collections.sort(commonApps, comparator)
			Collections.sort(recentApps, comparator)
			Collections.sort(visibleApps, comparator)

			synchronized (EditApplicationsActivity::class.java) {
				listApps = Applications(handledApps, recentApps, commonApps, visibleApps)
			}
		}
		else
		{
			Collections.sort(recentApps, comparator)

			synchronized (EditApplicationsActivity::class.java) {
				listApps!!.setRecent(recentApps)
			}
		}
	}

	inner class LoadApplications1stStageTask : AsyncTask<Void, Void, Void>()
	{
		override fun doInBackground(vararg params: Void): Void?
		{
			loadApplications()
			return null
		}

		override fun onPreExecute()
		{
		}

		override fun onPostExecute(result: Void)
		{
			var applications: Applications? = null

			synchronized (this@EditApplicationsActivity) {
				applications = listApps
			}

			listApplications!!.adapter = ListApplicationsAdapter(
				this@EditApplicationsActivity, R.layout.edit_list_item, applications!!)


			listApplications!!.setSelection(0)

			synchronized (this@EditApplicationsActivity) {
				app1stLoader = null // job is done, dispose 
				app2ndLoader = LoadApplications2ndStageTask()
				app2ndLoader!!.execute()
			}
		}

		override fun onCancelled()
		{
		}
	}

	inner class LoadApplications2ndStageTask : AsyncTask<Void, Void, Void>()
	{
		private val packageManager = getPackageManager()

		override fun doInBackground(vararg params: Void): Void?
		{
			var apps: ArrayList<AppSelectionInfo>? = null
			synchronized (this@EditApplicationsActivity) {
				apps = listApps!!.allFlat
			}

			for (appInfo in apps!!)
			{
				if (!appInfo.loadComplete)
					synchronized (appInfo) {
						if (!appInfo.loadComplete)
						{
							//appInfo.name = packageManager.getApplicationLabel(appInfo.app).toString();							
							appInfo.icon = appInfo.app!!.loadIcon(packageManager)
							appInfo.loadComplete = true
						}
					}
			}
			return null
		}

		override fun onPreExecute()
		{
		}

		override fun onPostExecute(result: Void)
		{
			synchronized (this@EditApplicationsActivity) {
				app2ndLoader = null // job is done, dispose 
			}
		}

		override fun onCancelled()
		{
		}
	}


	public override fun onStart()
	{
		Lw.d(TAG, "onStart()")
		super.onStart()
	}

	public override fun onStop()
	{
		Lw.d(TAG, "onStop()")
		super.onStop()
	}

	public override fun onPause()
	{
		var loader1st: LoadApplications1stStageTask? = null
		var loader2nd: LoadApplications2ndStageTask? = null

		synchronized (this) {
			loader1st = app1stLoader
			app1stLoader = null
			loader2nd = app2ndLoader
		}

		if (loader1st != null)
			loader1st!!.cancel(false)

		if (loader2nd != null)
			loader2nd!!.cancel(false)

		super.onPause()
	}

	public override fun onResume()
	{
		super.onResume()

		synchronized (this) {
			app1stLoader = LoadApplications1stStageTask()
			app1stLoader!!.execute()
		}
	}

	private inner class ListApplicationsAdapter(private val context: Context, textViewResourceId: Int, internal var applications:

	Applications) : BaseAdapter()
	{

		internal var packageManager = getPackageManager()

		init
		{

			Lw.d(TAG, "Adding list of applications:")
			for (asi in applications.allFlat)
			{
				Lw.d(TAG, " ... " + asi.packageName)
			}
		}

		override fun getViewTypeCount(): Int
		{
			return 2
		}

		override fun getCount(): Int
		{
			var size = 0

			for (list in applications.all)
			{
				if (list.size > 0)
				{
					size += 1 + list.size
				}
			}
			return size
		}

		override fun getItemViewType(position: Int): Int
		{
			var position = position
			for (list in applications.all)
			{
				if (list.size > 0)
				{
					if (position == 0)
						return 1
					position--

					if (position < list.size)
						return 0

					position -= list.size
				}
			}

			return 0
		}


		private var menuTitles: Array<String>? = null
			get()
			{
				synchronized (this) {
					if (field == null)
					{
						field = arrayOf<String>(
							getString(R.string.menutitle_handled_apps), getString(R.string.menutitle_recent_notificatoins), getString(R.string.menutitle_common_notifications), getString(R.string.menutitle_other_apps)
						)
					}
				}

				return field
			}


		override fun getItem(position: Int): Any?
		{
			var position = position
			val titles = menuTitles

			var titleIdx = 0
			for (list in applications.all)
			{
				if (list.size > 0)
				{
					if (position == 0)
						return titles!![titleIdx]
					position--

					if (position < list.size)
						return list[position]

					position -= list.size
				}
				titleIdx++
			}

			return null
		}

		override fun getItemId(position: Int): Long
		{
			return position.toLong()
		}

		fun getItemView(position: Int, convertView: View, parent: ViewGroup): View
		{
			var rowView: View? = convertView

			if (rowView == null)
			{
				val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

				rowView = inflater.inflate(R.layout.edit_list_item, parent, false)

				val viewHolder = ViewHolder()

				viewHolder.btnShowHide = rowView!!.findViewById(R.id.checkBoxShowApp) as CheckBox
				viewHolder.textViewAppName = rowView.findViewById(R.id.textViewAppName) as TextView
				viewHolder.imageViewAppIcon = rowView.findViewById(R.id.editIcon) as ImageView

				rowView.tag = viewHolder
			}

			val viewHolder = rowView.tag as ViewHolder

			val appInfo = getItem(position) as AppSelectionInfo?

			if (!appInfo!!.loadComplete)
			{
				synchronized (appInfo) {
					if (!appInfo.loadComplete)
					{
						appInfo.icon = appInfo.app!!.loadIcon(packageManager)
						appInfo.loadComplete = true
					}
				}
			}

			viewHolder.btnShowHide!!.isChecked = pkgSettings!!.getIsListed(appInfo.packageName)

			if (appInfo.name != null)
				viewHolder.textViewAppName!!.text = appInfo.name
			else
				viewHolder.textViewAppName!!.text = appInfo.packageName

			if (appInfo.icon != null)
				try
				{
					viewHolder.imageViewAppIcon!!.setImageDrawable(appInfo.icon)
				}
				catch (ex: Exception)
				{
					ex.printStackTrace()
				}

			viewHolder.btnShowHide!!.setOnClickListener()
				{
					btn ->

					Lw.d("saveSettingsOnClickListener.onClick()")

					if ((btn as CheckBox).isChecked)
					{
						pkgSettings!!.lookupEverywhereAndMoveOrInsertNew(appInfo.packageName, true, 0)
					}
					else
					{
						// must hide
						val pkg = pkgSettings!!.getPackage(appInfo.packageName)
						if (pkg != null)
							pkgSettings!!.hidePackage(pkg)
					}

					forceReloadApplications = true // force to reload applist on the next open of the activity
				}

			return rowView
		}

		fun getTitleView(position: Int, convertView: View, parent: ViewGroup): View
		{
			var rowView: View? = convertView

			if (rowView == null)
			{
				val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
				rowView = inflater.inflate(R.layout.edit_list_item_title, parent, false)
			}

			val text = rowView!!.findViewById(R.id.textViewGroupTitle) as TextView

			val title = getItem(position) as String?
			text.text = title

			return rowView
		}

		override fun getView(position: Int, convertView: View, parent: ViewGroup): View
		{
			if (getItemViewType(position) == 0)
			{
				return getItemView(position, convertView, parent)
			}
			else
			{
				return getTitleView(position, convertView, parent)
			}
		}
	}

	inner class ViewHolder
	{
		internal var textViewAppName: TextView? = null
		internal var imageViewAppIcon: ImageView? = null

		internal var btnShowHide: CheckBox? = null
	}

	companion object
	{
		private val TAG = "EditApplicationsActivity"

		private var listApps: Applications? = null
		private var forceReloadApplications = false
	}
}
