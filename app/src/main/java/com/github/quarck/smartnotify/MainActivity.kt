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

import android.annotation.SuppressLint
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator

import android.app.Activity
import android.app.AlertDialog
import android.app.NotificationManager
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.PackageManager.NameNotFoundException
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.NumberPicker
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import org.jetbrains.annotations.NotNull

class MainActivity : Activity(), ServiceClient.Callback
{
	private var serviceClient: ServiceClient? = null

	private var serviceEnabled = false

	private inner class ApplicationPkgInfo(
		val pkgInfo: PackageSettings.Package,
		val name: String,
		var icon: Drawable? = null)
	{
	}

	private var handledApplicationsInternal: ArrayList<ApplicationPkgInfo>? = null

	private var handledApplications: ArrayList<ApplicationPkgInfo>?
		get()
		{
			var ret : ArrayList<ApplicationPkgInfo>? = null
			synchronized (this)
			{
				ret = handledApplicationsInternal
			}
			return ret
		}
		set(value)
		{
			synchronized (this)
			{
				handledApplicationsInternal = value;
			}
		}

	private var toggleButtonEnableService: ToggleButton? = null
	private var listHandledApplications: ListView? = null
	private var textViewlonelyHere: TextView? = null
	private var textViewListSmallPrint: TextView? = null
	private var listAdapter: ListApplicationsAdapter? = null

	private var saveSettingsOnClickListener: OnClickListener? = null

	private var pkgSettings: PackageSettings? = null
	private var settings: Settings? = null

	private var listApplicationsLoader: LoadPackagesTask? = null

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)

		Lw.d("main activity created")

		Lw.d(TAG, "onCreateView")

		settings = Settings(this)
		pkgSettings = PackageSettings(this)

		setContentView(R.layout.activity_main)

		toggleButtonEnableService = findViewById(R.id.toggleButtonEnableService) as ToggleButton
		listHandledApplications = findViewById(R.id.listApplications) as ListView
		textViewlonelyHere = findViewById(R.id.textViewLonelyHere) as TextView
		textViewListSmallPrint = findViewById(R.id.textViewLblEnablePerAppSmallprint) as TextView

		listHandledApplications!!.onItemClickListener =
			object : AdapterView.OnItemClickListener {
					override fun onItemClick(parent: AdapterView<*>?, view: View?, position: Int, id: Long)
					{
						(listHandledApplications!!.adapter as ListApplicationsAdapter).onItemClicked(position);
					}
				}

		serviceEnabled = settings!!.isServiceEnabled
		toggleButtonEnableService!!.isChecked = serviceEnabled

		synchronized (this) {
			listApplicationsLoader = LoadPackagesTask()
			listApplicationsLoader!!.execute()
		}

		saveSettingsOnClickListener = OnClickListener {
			Lw.d("saveSettingsOnClickListener.onClick()")

			saveSettings()

			(getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(Consts.notificationIdUpdated)

			if (serviceEnabled) {
				val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
				startActivity(intent)
			}
		}

		toggleButtonEnableService!!.setOnClickListener(saveSettingsOnClickListener)
	}

	override fun onCreateOptionsMenu(menu: Menu): Boolean
	{
		menuInflater.inflate(R.menu.main, menu)
		return true
	}

	@SuppressLint("BatteryLife")
	private fun checkPermissions() {
		val hasPermissions = PermissionsManager.hasAllPermissions(this)

		if (!hasPermissions) {
			if (PermissionsManager.shouldShowRationale(this)) {
				AlertDialog.Builder(this)
					.setMessage(R.string.application_has_no_access)
					.setCancelable(false)
					.setPositiveButton(android.R.string.ok) {
							_, _ ->
						PermissionsManager.requestPermissions(this)
					}
					.setNegativeButton(getString(R.string.exit)) {
							_, _ ->
						this@MainActivity.finish()
					}
					.create()
					.show()
			}
			else {
				PermissionsManager.requestPermissions(this)
			}
		}
		else {
			// if we have essential permissions - now check for power manager optimisations
			if (!powerManager.isIgnoringBatteryOptimizations(BuildConfig.APPLICATION_ID)) {
				AlertDialog.Builder(this)
					.setTitle(getString(R.string.ignore_batter_optimisations_title))
					.setMessage(getString(R.string.ignore_batter_optimisations_details))
					.setPositiveButton(getString(R.string.ignore_batter_optimisations_yes)) {
							_, _ ->
						val intent = Intent()
							.setAction(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
							.setData(Uri.parse("package:" + BuildConfig.APPLICATION_ID))
						startActivity(intent)
					}
					.setNeutralButton(getString(R.string.ignore_batter_optimisations_later)) {
							_, _ ->
					}
					.create()
					.show()
			}

		}
	}

	override fun onRequestPermissionsResult(requestCode: Int, @NotNull permissions: Array<out String>, @NotNull grantResults: IntArray) {
//		for (result in grantResults) {
//			if (result != PackageManager.PERMISSION_GRANTED) {
//				DevLog.error(LOG_TAG, "Permission is not granted!")
//			}
//		}
	}

	override fun onOptionsItemSelected(item: MenuItem): Boolean
	{
		val id = item.itemId
		if (id == R.id.action_settings)
		{
			val intent = Intent(this, SettingsActivity::class.java)
			startActivity(intent)
		}
		else if (id == R.id.action_edit_applications)
		{
			val intent = Intent(this, EditApplicationsActivity::class.java)
			startActivity(intent)
		}
		return super.onOptionsItemSelected(item)
	}


	override fun onNoPermissions()
	{
		Lw.d(TAG, "onNoPermissions()!!!")

		val builder = AlertDialog.Builder(this)
		builder
			.setMessage(R.string.application_has_no_access)
			.setCancelable(false)
			.setPositiveButton(R.string.open_settings) {
				x, y ->
					val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
					startActivity(intent)
				}
			.setNegativeButton(R.string.cancel_quit) {
				DialogInterface, Int -> finish()
			}

		// Create the AlertDialog object and return it
		builder.create().show()
	}

	private fun saveSettings()
	{
		Lw.d(TAG, "Saving current settings")

		serviceEnabled = toggleButtonEnableService!!.isChecked
		settings!!.isServiceEnabled = serviceEnabled

		if (serviceClient != null)
			serviceClient!!.forceReloadConfig()

		synchronized (this) {
			listAdapter!!.notifyDataSetChanged()
		}
	}

	inner class LoadPackagesTask : AsyncTask<Void?, Void?, Void?>()
	{
		override fun doInBackground(vararg params: Void?): Void?
		{
			Lw.d(TAG, "LoadPackagesTask::doInBackground")

			val pkgSettings = PackageSettings(this@MainActivity)

			if (!settings!!.isInitialPopulated)
			{
				InitialPopulate().populate(this@MainActivity, pkgSettings)
				settings!!.isInitialPopulated = true
			}

			val packageManager = packageManager

			val allPackages = pkgSettings.allPackages

			val applications = ArrayList<ApplicationPkgInfo>()

			for (pkg in allPackages)
			{
				try
				{
					val pmAppInfo = packageManager.getApplicationInfo(pkg.packageName ?: "", PackageManager.GET_META_DATA)
					val icon = pmAppInfo.loadIcon(packageManager)
					var name = packageManager.getApplicationLabel(pmAppInfo).toString()

					applications.add(ApplicationPkgInfo(pkg, name, icon))
				}
				catch (e: NameNotFoundException)
				{
					e.printStackTrace()
				}
			}

			val comparator = Comparator<com.github.quarck.smartnotify.MainActivity.ApplicationPkgInfo>
			{
				app1, app2 -> app1.name.compareTo(app2.name)
			}

			Collections.sort(applications, comparator)

			handledApplications = applications
			return null
		}

		override fun onPreExecute()
		{
		}

		override fun onPostExecute(result: Void?)
		{
			var applications: ArrayList<ApplicationPkgInfo>? = handledApplications

			var adapter: ListApplicationsAdapter? = null

			if (applications != null)
			{
				if (applications.isEmpty())
				{
					textViewlonelyHere!!.visibility = View.VISIBLE
					listHandledApplications!!.visibility = View.GONE
					textViewListSmallPrint!!.visibility = View.GONE
				}
				else
				{
					listHandledApplications!!.visibility = View.VISIBLE
					textViewlonelyHere!!.visibility = View.VISIBLE
					textViewlonelyHere!!.visibility = View.GONE

					adapter = ListApplicationsAdapter(this@MainActivity, applications)
					listHandledApplications!!.adapter = adapter
					listHandledApplications!!.setSelection(0)
				}
			}

			synchronized (this@MainActivity) {
				listApplicationsLoader = null // job is done, dispose
				listAdapter = adapter
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
		serviceClient = ServiceClient(this)
		serviceClient!!.bindService(applicationContext)
	}

	public override fun onStop()
	{
		Lw.d(TAG, "onStop()")
		serviceClient!!.unbindService(applicationContext)
		super.onStop()
	}

	public override fun onPause()
	{
		Lw.d(TAG, "onPause")

		var loader: LoadPackagesTask? = null

		synchronized (this) {
			loader = listApplicationsLoader
			listApplicationsLoader = null
		}

		if (loader != null)
			loader!!.cancel(false)

		super.onPause()
	}

	public override fun onResume()
	{
		Lw.d(TAG, "onResume")

		super.onResume()

	//	checkPermissions()

		OngoingNotificationManager.updateNotification(this)

		synchronized (this) {
			listApplicationsLoader = LoadPackagesTask()
			listApplicationsLoader!!.execute()
		}

	}

	override fun onNotificationList(notifications: Array<String>)
	{
		Lw.d(TAG, "OnNotificationList()")

		if (notifications != null)
		{
			val sb = StringBuilder()

			if (notifications != null)
				for (ntf in notifications)
				{
					sb.append(ntf)
					sb.append("\n")
				}

			Toast.makeText(this, sb.toString(), Toast.LENGTH_LONG).show()
		}
		else
		{
			onNoPermissions()
		}
	}

	private inner class ListApplicationsAdapter(
			private val context: Context,
			internal var listApplications: ArrayList<ApplicationPkgInfo>
		) : BaseAdapter()
	{

		fun onItemClicked(position: Int)
		{
			if (!serviceEnabled)
			{
				Lw.d(TAG, "ListApplicationsAdapter::onItemClicked, service is disbaled")
				return
			}

			Lw.d(TAG, "ListApplicationsAdapter::onItemClicked, pos=" + position)

			val appInfo = listApplications[position]

			val alert = AlertDialog.Builder(this@MainActivity)

			alert.setTitle("Remind interval")

			val inflater = this@MainActivity.layoutInflater

			val dialogView = inflater.inflate(R.layout.dlg_remind_interval, null)

			alert.setView(dialogView)

			val picker = dialogView.findViewById(R.id.numberPickerRemindInterval) as NumberPicker

			picker.minValue = 1
			picker.maxValue = 120
			picker.value = appInfo.pkgInfo!!.remindIntervalSeconds / 60

			alert.setPositiveButton(android.R.string.ok) {
				x,y ->
				val interval = picker.value

				Lw.d(TAG, "got val: " + interval)//value.toString());

				try
				{
					appInfo.pkgInfo!!.remindIntervalSeconds = interval * 60
					pkgSettings!!.updatePackage(appInfo.pkgInfo!!)

					Lw.d(TAG, "remind interval updated to " + interval + " for package " + appInfo.pkgInfo)
				}
				catch (ex: Exception)
				{
					ex.printStackTrace()
				}

				notifyDataSetChanged()
			}

			alert.setNegativeButton(android.R.string.cancel) { x,y ->  }

			alert.show()
		}

		override fun getCount(): Int
		{
			return listApplications.size
		}

		override fun getItem(position: Int): Any
		{
			return listApplications[position]
		}

		override fun getItemId(position: Int): Long
		{
			return position.toLong()
		}

		override fun getViewTypeCount(): Int
		{
			return 2
		}

		override fun getItemViewType(position: Int): Int
		{
			return 0
		}

		override fun getView(position: Int, convertView: View?, parent: ViewGroup): View
		{
			var rowView: View? = convertView

			var viewHolder: ViewHolder? = if (rowView != null) rowView.tag as ViewHolder else null

			if (viewHolder == null)
			{
				val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater

				rowView = inflater.inflate(R.layout.list_item, parent, false)

				viewHolder = ViewHolder()

				viewHolder.textViewRemindInterval = rowView!!.findViewById(R.id.textViewIntervalLabel) as TextView
				viewHolder.textViewAppName = rowView.findViewById(R.id.textViewAppName) as TextView
				viewHolder.imageViewAppIcon = rowView.findViewById(R.id.icon) as ImageView
				viewHolder.btnEnableForApp = rowView.findViewById(R.id.toggleButtonEnableForApp) as ToggleButton

				rowView.tag = viewHolder
			}

			val appInfo = listApplications[position] // this would not change as well - why lookup twice then?

			viewHolder.btnEnableForApp!!.isChecked = appInfo.pkgInfo!!.isHandlingThis

			val text = getString(R.string.every_nmin_fmt).format((appInfo.pkgInfo!!.remindIntervalSeconds / 60))
			viewHolder.textViewRemindInterval!!.setText(text)

			if (appInfo.name != null)
				viewHolder.textViewAppName!!.text = appInfo.name
			else
				viewHolder.textViewAppName!!.text = appInfo.pkgInfo!!.packageName

			if (appInfo.icon != null)
				viewHolder.imageViewAppIcon!!.setImageDrawable(appInfo.icon)

			viewHolder.btnEnableForApp!!.isEnabled = serviceEnabled
			viewHolder.textViewRemindInterval!!.isEnabled = serviceEnabled
			viewHolder.imageViewAppIcon!!.isEnabled = serviceEnabled
			viewHolder.textViewAppName!!.isEnabled = serviceEnabled

			viewHolder.btnEnableForApp!!.setOnClickListener {
				btn ->
				Lw.d("saveSettingsOnClickListener.onClick()")

				appInfo.pkgInfo!!.isHandlingThis = (btn as ToggleButton).isChecked
				pkgSettings!!.updatePackage(appInfo.pkgInfo!!)
				saveSettings()
			}

			return rowView!!
		}

		inner class ViewHolder
		{
			internal var btnEnableForApp: ToggleButton? = null
			internal var textViewRemindInterval: TextView? = null
			internal var textViewAppName: TextView? = null
			internal var imageViewAppIcon: ImageView? = null
		}
	}

	override fun onRecetNotificationsList(recentNotifications: Array<String>)
	{
		// TODO Auto-generated method stub
	}

	companion object
	{
		private val TAG = "MainActivity"
	}
}
