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


package com.github.quarck.smartnotify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.CheckBox;

public class EditApplicationsActivity extends Activity
{
	private static final String TAG = "EditApplicationsActivity";
	
	private ListView listApplications = null;
	
	//private ArrayList<AppSelectionInfo> listApps = new ArrayList<AppSelectionInfo>();
	
	private static class Applications
	{
		private ArrayList<AppSelectionInfo> handledApps;
		private ArrayList<AppSelectionInfo> recentApps;
		private ArrayList<AppSelectionInfo> commonApps;
		private ArrayList<AppSelectionInfo> visibleApps;
		
		private ArrayList<ArrayList<AppSelectionInfo>> all;
				
		public Applications(
				ArrayList<AppSelectionInfo> handled, 
				ArrayList<AppSelectionInfo> recent,
				ArrayList<AppSelectionInfo> common,
				ArrayList<AppSelectionInfo> visible
			)
		{
			all = new ArrayList<ArrayList<AppSelectionInfo>>();
			
			handledApps = handled;
			recentApps = recent;
			commonApps = common;
			visibleApps = visible;
			
			all.add(handledApps);
			all.add(recentApps);
			all.add(commonApps);
			all.add(visibleApps);
		}
				
		public void setRecent(ArrayList<AppSelectionInfo> recent)
		{
			recentApps = recent;
			all.set(1, recentApps);
		}
		
		public ArrayList<ArrayList<AppSelectionInfo>> getAll() { return all; }
		
		public ArrayList<AppSelectionInfo> getAllFlat()
		{
			ArrayList<AppSelectionInfo> ret = new ArrayList<AppSelectionInfo>();

			for(ArrayList<AppSelectionInfo> list : all)
				ret.addAll(list);

			return ret;
		}
	}
	
	private static Applications listApps = null;
	private static boolean forceReloadApplications = false;
	
	private class AppSelectionInfo
	{		
		boolean loadComplete = false;
	
		String name;
		String packageName;
		Drawable icon;
		
		ApplicationInfo app;
	}
	
	private LoadApplications1stStageTask app1stLoader = null;
	private LoadApplications2ndStageTask app2ndLoader = null;
	

	private PackageSettings pkgSettings = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		Lw.d(TAG, "onCreate");

		pkgSettings = new PackageSettings(this);

		setContentView(R.layout.activity_edit_apps);

		listApplications = (ListView) findViewById(R.id.listAddApplications);

		ActionBar actionBar = getActionBar();
		actionBar.setHomeButtonEnabled(true);
		actionBar.setTitle("Show/Hide applications");
		
        listApplications.setEmptyView((ProgressBar)findViewById(R.id.progressBarLoading));
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) 
	{
	    switch (item.getItemId()) {
	        case android.R.id.home:
	            // app icon in action bar clicked; goto parent activity.
	            this.finish();
	            return true;
	        default:
	            return super.onOptionsItemSelected(item);
	    }
	}	



	private void loadApplications()
	{
		CommonAppsRegistry.initRegistry(this);
		
		boolean onlyRefreshRecent = false;
		synchronized (EditApplicationsActivity.class)
		{
			if (listApps != null)
				onlyRefreshRecent = !forceReloadApplications;
			forceReloadApplications = false;
		}
		
		
		PackageSettings pkgSettings = new PackageSettings(this);
	
		PackageManager packageManager = getPackageManager();

		ArrayList<AppSelectionInfo> handledApps = new ArrayList<AppSelectionInfo>();
		ArrayList<AppSelectionInfo> recentApps = new ArrayList<AppSelectionInfo>();
		ArrayList<AppSelectionInfo> commonApps = new ArrayList<AppSelectionInfo>();
		ArrayList<AppSelectionInfo> visibleApps = new ArrayList<AppSelectionInfo>();
		
		
		HashMap<String,Integer> alreadyLoadedAppsHash = new HashMap<String, Integer>();

		Lw.d(TAG, "Loading applications");
	
		Lw.d(TAG, "Loading configured applications first");
				
		for (PackageSettings.Package pkg : pkgSettings.getAllPackages())
		{
			if (alreadyLoadedAppsHash.containsKey(pkg.getPackageName()))
				continue; // already loaded by somebody else (by whooom??)

			try
			{
				AppSelectionInfo asi = new AppSelectionInfo();
				
				asi.packageName = pkg.getPackageName();	
				asi.app = packageManager.getApplicationInfo(pkg.getPackageName(), 0/*PackageManager.GET_META_DATA*/);				
				asi.name = packageManager.getApplicationLabel(asi.app).toString();
				handledApps.add(asi);
				alreadyLoadedAppsHash.put(asi.packageName, 1);
			}
			catch (NameNotFoundException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	
		}

		Lw.d(TAG, "Loading Recent applications below:");

		for (String recent : NotificationReceiverService.getRecentNotifications())
		{
			Lw.d(TAG, "Recent app: " + recent);

			if (alreadyLoadedAppsHash.containsKey(recent))
				continue; // already loaded by somebody else
			
			AppSelectionInfo asi = new AppSelectionInfo();
			
			asi.packageName = recent;				

			try
			{
				ApplicationInfo app = packageManager.getApplicationInfo(recent, 0/*PackageManager.GET_META_DATA*/);
				 
				asi.name = packageManager.getApplicationLabel(app).toString();
				asi.app = app;					
				recentApps.add(asi);
				alreadyLoadedAppsHash.put(asi.packageName, 1);
			}
			catch (NameNotFoundException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		Lw.d(TAG, "Loading COMMON applications below:");				
		for (ApplicationInfo appInfo : CommonAppsRegistry.getApplications())
		{
			Lw.d(TAG, "Recent app: " + appInfo.packageName);

			if (alreadyLoadedAppsHash.containsKey(appInfo.packageName))
				continue; // already loaded by somebody else
			
			AppSelectionInfo asi = new AppSelectionInfo();
			
			asi.packageName = appInfo.packageName;

			asi.name = packageManager.getApplicationLabel(appInfo).toString();
			asi.app = appInfo;
			
			if (asi.packageName.equals(Consts.packageName))
				commonApps.add(asi);
			alreadyLoadedAppsHash.put(asi.packageName, 1);
		}
				
		if (!onlyRefreshRecent)
		{
			Lw.d(TAG, "Loading all other applications");
		
			for (ApplicationInfo app : packageManager.getInstalledApplications(0))
			{
				AppSelectionInfo asi = new AppSelectionInfo();
				
				if (app.packageName == null)
					continue;
		
				if (alreadyLoadedAppsHash.containsKey(app.packageName))
					continue; // already loaded by somebody else

				asi.packageName = app.packageName;	
								
				alreadyLoadedAppsHash.put(asi.packageName, 1);
								
				asi.name = packageManager.getApplicationLabel(app).toString();
				asi.app = app;
				
				Intent launchActivity = packageManager.getLaunchIntentForPackage(app.packageName);
				
				if (launchActivity != null)
				{
					visibleApps.add(asi);
				}	 
			}
		}
		
		Comparator<AppSelectionInfo> comparator = new Comparator<AppSelectionInfo>() 
		{
			@Override
	        public int compare(AppSelectionInfo  app1, AppSelectionInfo  app2)
	        {
	            return  app1.name.compareTo(app2.name);
	        }
	    };

	    if (!onlyRefreshRecent)
	    {
			Collections.sort(handledApps, comparator);
			Collections.sort(commonApps, comparator);
			Collections.sort(recentApps, comparator);
			Collections.sort(visibleApps, comparator);
			
			synchronized (EditApplicationsActivity.class)
			{
				listApps = new Applications(handledApps, recentApps, commonApps, visibleApps);
			}
	    }
	    else
	    {
	    	Collections.sort(recentApps, comparator);

	    	synchronized (EditApplicationsActivity.class)
			{
				listApps.setRecent(recentApps);
			}
	    }
	}
	
	public class LoadApplications1stStageTask extends AsyncTask<Void, Void, Void>
	{
		@Override
		protected Void doInBackground(Void... params)
		{
			loadApplications();
			return null;
		}

		@Override
		protected void onPreExecute()
		{
		}

		@Override
		protected void onPostExecute(Void result)
		{
			Applications applications = null;
			
			synchronized (EditApplicationsActivity.this)
			{
				applications = listApps;
			}
			
			listApplications.setAdapter(
					new ListApplicationsAdapter(
							EditApplicationsActivity.this, R.layout.edit_list_item, applications));
			

			listApplications.setSelection(0);
			
			synchronized (EditApplicationsActivity.this)
			{
				app1stLoader = null; // job is done, dispose 
				app2ndLoader = new LoadApplications2ndStageTask();
				app2ndLoader.execute();
			}
		}

		@Override
		protected void onCancelled()
		{
		}
	}

	public class LoadApplications2ndStageTask extends AsyncTask<Void, Void, Void>
	{
		private PackageManager packageManager = getPackageManager();
		
		@Override
		protected Void doInBackground(Void... params)
		{
			ArrayList<AppSelectionInfo> apps = null;
			synchronized (EditApplicationsActivity.this)
			{
				apps = listApps.getAllFlat();
			}
			
			for (AppSelectionInfo appInfo: apps)
			{
				if (!appInfo.loadComplete)
					synchronized (appInfo)
					{
						if (!appInfo.loadComplete)
						{
							//appInfo.name = packageManager.getApplicationLabel(appInfo.app).toString();							
							appInfo.icon = appInfo.app.loadIcon(packageManager);
							appInfo.loadComplete = true;
						}
					}
			}
			return null;
		}

		@Override
		protected void onPreExecute()
		{
		}

		@Override
		protected void onPostExecute(Void result)
		{
			synchronized (EditApplicationsActivity.this)
			{
				app2ndLoader = null; // job is done, dispose 
			}
		}

		@Override
		protected void onCancelled()
		{
		}
	}
	
	
	@Override
	public void onStart()
	{
		Lw.d(TAG, "onStart()");
		super.onStart();
	}

	@Override
	public void onStop()
	{
		Lw.d(TAG, "onStop()");
		super.onStop();
	}
	
	@Override 
	public void onPause()
	{
		LoadApplications1stStageTask loader1st;
		LoadApplications2ndStageTask loader2nd;
		
		synchronized(this)
		{
			loader1st = app1stLoader;
			app1stLoader = null;
			loader2nd = app2ndLoader;
		}
		
		if (loader1st != null)
			loader1st.cancel(false);
		
		if (loader2nd != null)
			loader2nd.cancel(false);

		super.onPause();
	}
	
	@Override 
	public void onResume()
	{
		super.onResume();
		
		synchronized(this)
		{
			app1stLoader = new LoadApplications1stStageTask();
			app1stLoader.execute();
		}
	}

	private class ListApplicationsAdapter extends BaseAdapter
	{
		private final Context context;

		Applications applications;

		PackageManager packageManager = getPackageManager();
		
		public ListApplicationsAdapter(Context ctx, int textViewResourceId, Applications apps)
		{
			super();
			
			context = ctx;
			
			applications = apps;
			
			Lw.d(TAG, "Adding list of applications:");
			for(AppSelectionInfo asi : applications.getAllFlat())
			{
				Lw.d(TAG, " ... " + asi.packageName );
			}
		}
		
		@Override
		public int getViewTypeCount()
		{
			return 2;
		}

		@Override
		public int getCount()
		{
			int size = 0;

			for (ArrayList<AppSelectionInfo> list : applications.getAll())
			{
				if (list.size() > 0)
				{
					size += 1 + list.size();
				}
			}
			return size;
		}
		
		public int getItemViewType(int position)
		{
			for (ArrayList<AppSelectionInfo> list : applications.getAll())
			{
				if (list.size() > 0)
				{
					if (position == 0 )
						return 1;
					position --;
					
					if (position < list.size())
						return 0;
					
					position -= list.size();
				}
			}

			return 0;			
		}

		public Object getItem(int position)
		{
			String[] titles = {
					"HANDLED APPLICATIONS", 
					"RECENT NOTIFICATIONS", 
					"COMMON NOTIFICATIONS",
					"OTHER APPLICATIONS"
				};

			int titleIdx = 0;
			for (ArrayList<AppSelectionInfo> list : applications.getAll())
			{
				if (list.size() > 0)
				{
					if (position == 0 )
						return titles[titleIdx];
					position --;
					
					if (position < list.size())
						return list.get(position);
					
					position -= list.size();
				}
				titleIdx ++;
			}
			
			return null;
		}

		public long getItemId(int position)
		{
			return position;
		}

		public View getItemView(int position, View convertView, ViewGroup parent)
		{
			View rowView = convertView;
			
			if (rowView == null)
			{
				LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				
				rowView = inflater.inflate(R.layout.edit_list_item, parent, false);

				ViewHolder viewHolder = new ViewHolder();

				viewHolder.btnShowHide = (CheckBox) rowView.findViewById(R.id.checkBoxShowApp);				
				viewHolder.textViewAppName = (TextView) rowView.findViewById(R.id.textViewAppName);				
				viewHolder.imageViewAppIcon = (ImageView) rowView.findViewById(R.id.editIcon);

				rowView.setTag(viewHolder);
			}
						
			ViewHolder viewHolder = (ViewHolder)rowView.getTag();
			
			final AppSelectionInfo appInfo = (AppSelectionInfo)getItem(position);
			
			if (!appInfo.loadComplete)
			{
				synchronized (appInfo)
				{
					if (!appInfo.loadComplete)
					{
						appInfo.icon = appInfo.app.loadIcon(packageManager);
						appInfo.loadComplete = true;
					}
				}
			}
						
			viewHolder.btnShowHide.setChecked( pkgSettings.getIsListed(appInfo.packageName) );
			
			if (appInfo.name != null)
				viewHolder.textViewAppName.setText(appInfo.name);
			else
				viewHolder.textViewAppName.setText(appInfo.packageName);

			if ( appInfo.icon != null)
				try
				{
					viewHolder.imageViewAppIcon.setImageDrawable( appInfo.icon );
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
				}

			viewHolder.btnShowHide.setOnClickListener(new OnClickListener()
			{
				public void onClick(View btn)
				{
					Lw.d("saveSettingsOnClickListener.onClick()");
					
					if (((CheckBox)btn).isChecked() )
					{
						pkgSettings.lookupEverywhereAndMoveOrInsertNew(appInfo.packageName, true, 0);
					}
					else
					{
						// must hide
						PackageSettings.Package pkg = pkgSettings.getPackage(appInfo.packageName);
						if (pkg != null)
							pkgSettings.hidePackage(pkg);
					}
					
					forceReloadApplications = true; // force to reload applist on the next open of the activity
				}
			});

			return rowView;
		}

		public View getTitleView(int position, View convertView, ViewGroup parent)
		{
			View rowView = convertView;
			
			if (rowView == null)
			{
				LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);					
				rowView = inflater.inflate(R.layout.edit_list_item_title, parent, false);					
			}
			
			TextView text = (TextView)rowView.findViewById(R.id.textViewGroupTitle);
			
			final String title  = (String)getItem(position);
			text.setText(title);
								
			return rowView;				
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			if (getItemViewType(position) == 0 )
			{
				return getItemView(position, convertView, parent);
			}
			else
			{
				return getTitleView(position, convertView, parent);
			}
		}	
	}

	public class ViewHolder
	{
		TextView textViewAppName;
		ImageView imageViewAppIcon;
		
		CheckBox btnShowHide;
	}
}
