package com.github.quarck.smartnotify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

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
import android.widget.ToggleButton;

public class EditApplicationsActivity extends Activity
{
	private static final String TAG = "EditApplicationsActivity";
	
	private ListView listApplications = null;
	
	//private ArrayList<AppSelectionInfo> listApps = new ArrayList<AppSelectionInfo>();
	
	private static class Applications
	{
		public ArrayList<AppSelectionInfo> handledApps;
		public ArrayList<AppSelectionInfo> recentApps;
		public ArrayList<AppSelectionInfo> visibleApps;
		public ArrayList<AppSelectionInfo> otherApps;
		
		public ArrayList<AppSelectionInfo> getAll()
		{
			ArrayList<AppSelectionInfo> ret = new ArrayList<AppSelectionInfo>();
			
			if (handledApps != null)
				ret.addAll(handledApps);
			if (recentApps != null)
				ret.addAll(recentApps);
			if (visibleApps != null)
				ret.addAll(visibleApps);
			if (otherApps != null)
				ret.addAll(otherApps);
			
			return ret;
		}
	}
	
	private static Applications listApps = null;
	
	private class AppSelectionInfo
	{		
		boolean loadComplete = false;
	
		boolean isSelected;
		String name;
		String packageName;
		Drawable icon;
		
		ApplicationInfo app;
	}
	
	private LoadApplicationsTask appLoader = null;
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
		PackageSettings pkgSettings = new PackageSettings(this);
	
		PackageManager packageManager = getPackageManager();

		List<ApplicationInfo> applications = packageManager.getInstalledApplications(0/*PackageManager.GET_META_DATA*/);

		ArrayList<AppSelectionInfo> handledApps = new ArrayList<AppSelectionInfo>();
		ArrayList<AppSelectionInfo> recentApps = new ArrayList<AppSelectionInfo>();
		ArrayList<AppSelectionInfo> visibleApps = new ArrayList<AppSelectionInfo>();
		ArrayList<AppSelectionInfo> otherApps = new ArrayList<AppSelectionInfo>();
		
		Lw.d(TAG, "Loading applications");
	
		String[] recentApplications = NotificationReceiverService.getRecentNotifications();
		HashMap<String,Integer> recentAppsHash = new HashMap<String, Integer>();
		
		for(String recent : recentApplications)
			recentAppsHash.put(recent, new Integer(1)); // LINQ, I miss you!
		
		for (ApplicationInfo app : applications)
		{
			AppSelectionInfo asi = new AppSelectionInfo();
			
			if (app.packageName == null)
				continue;
			
			asi.packageName = app.packageName;	
			
			
			asi.isSelected = pkgSettings.getIsListed( app.packageName );

			if (!asi.isSelected && recentAppsHash.containsKey(asi.packageName))
				continue; // skip, would be loaded separately
			
			asi.name = packageManager.getApplicationLabel(app).toString();
			asi.app = app;
			
			Intent launchActivity = packageManager.getLaunchIntentForPackage(app.packageName);
			
			if (asi.isSelected)
			{
				handledApps.add(asi);
			}
			else if (launchActivity != null)
			{
				visibleApps.add(asi);
			}
			else
			{
				otherApps.add(asi);
			}	
		}
		
		Lw.d(TAG, "Recent applications below:");
		
		for (String recent : recentApplications)
		{
			Lw.d(TAG, "Recent app: " + recent);
			
			AppSelectionInfo asi = new AppSelectionInfo();
			
			asi.packageName = recent;	
			
			asi.isSelected = pkgSettings.getIsListed( recent );
			
			ApplicationInfo app;
			try
			{
				app = packageManager.getApplicationInfo(recent, 0/*PackageManager.GET_META_DATA*/);
				
				asi.name = packageManager.getApplicationLabel(app).toString();
				asi.app = app;
				
				Intent launchActivity = packageManager.getLaunchIntentForPackage(recent);
				
				recentApps.add(asi);
				
			}
			catch (NameNotFoundException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
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
		
		Collections.sort(handledApps, comparator);
		Collections.sort(recentApps, comparator);
		Collections.sort(visibleApps, comparator);
		
		synchronized (EditApplicationsActivity.class)
		{
			listApps = new Applications();
			
			listApps.handledApps = handledApps;
			listApps.recentApps = recentApps;
			listApps.visibleApps = visibleApps;
			listApps.otherApps = otherApps;
		}
	}
	
	public class LoadApplicationsTask extends AsyncTask<Void, Void, Void>
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
				appLoader = null; // job is done, dispose 
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
				apps = listApps.getAll();
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
		LoadApplicationsTask loader;
		LoadApplications2ndStageTask loader2nd;
		
		synchronized(this)
		{
			loader = appLoader;
			appLoader = null;
			loader2nd = app2ndLoader;
		}
		
		if (loader != null)
			loader.cancel(false);
		
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
			appLoader = new LoadApplicationsTask();
			appLoader.execute();
		}
	}

	private class ListApplicationsAdapter extends BaseAdapter
	{
		private final Context context;

		//ArrayList<AppSelectionInfo> listApplications;
		Applications applications;

		PackageManager packageManager = getPackageManager();
		
		public ListApplicationsAdapter(Context ctx, int textViewResourceId, Applications apps)
		{
			super();
			
			context = ctx;
			
//			listApplications = applications;
			applications = apps;
			
			Lw.d(TAG, "Adding list of applications:");
			for(AppSelectionInfo asi : applications.getAll())
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
			if (applications.handledApps != null)
			{
				if (applications.handledApps.size() > 0)
				{
					size += 1 + applications.handledApps.size();
				}
			}
			if (applications.recentApps != null)
			{
				if (applications.recentApps.size() > 0)
				{
					size += 1 + applications.recentApps.size();
				}
			}
			if (applications.visibleApps != null)
			{
				if (applications.visibleApps.size() > 0)
				{
					size += 1 + applications.visibleApps.size();
				}
			}
			
			return size;
		}
		
		public int getItemViewType(int position)
		{
			if (applications.handledApps != null)
			{
				if (applications.handledApps.size() > 0)
				{
					if (position == 0 )
						return 1;
					position --;
					
					if (position < applications.handledApps.size())
						return 0;
					
					position -= applications.handledApps.size();
				}
			}
			if (applications.recentApps != null)
			{
				if (applications.recentApps.size() > 0)
				{
					if (position == 0 )
						return 1;
					position --;
					
					if (position < applications.recentApps.size())
						return 0;
					
					position -= applications.recentApps.size();
				}
			}
			if (applications.visibleApps != null)
			{
				if (applications.visibleApps.size() > 0)
				{
					if (position == 0 )
						return 1;
					position --;
					
					if (position < applications.visibleApps.size())
						return 0;
					
					position -= applications.visibleApps.size();
				}
			}
			
			return 0;			
		}

		public Object getItem(int position)
		{
			Object ret = null;
			
			if (applications.handledApps != null)
			{
				if (applications.handledApps.size() > 0)
				{
					if (position == 0 )
						return new String("Handled Applications");
					position --;
					
					if (position < applications.handledApps.size())
						return applications.handledApps.get(position);
					
					position -= applications.handledApps.size();
				}
			}
			if (applications.recentApps != null)
			{
				if (applications.recentApps.size() > 0)
				{
					if (position == 0 )
						return new String("Recent Notifications");
					position --;
					
					if (position < applications.recentApps.size())
						return applications.recentApps.get(position);
					
					position -= applications.recentApps.size();
				}
			}
			if (applications.visibleApps != null)
			{
				if (applications.visibleApps.size() > 0)
				{
					if (position == 0 )
						return new String("Other Applications");
					position --;
					
					if (position < applications.visibleApps.size())
						return applications.visibleApps.get(position);
					
					position -= applications.visibleApps.size();
				}
			}
			
			return null;
		}

		public long getItemId(int position)
		{
			return position;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			if (getItemViewType(position) == 0 )
			{
				View rowView = convertView;
				
				if (rowView == null)
				{
					LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
					
					rowView = inflater.inflate(R.layout.edit_list_item, parent, false);
	
					ViewHolder viewHolder = new ViewHolder();
	
					viewHolder.btnShowHide = (ToggleButton) rowView.findViewById(R.id.toggleButtonShowApp);				
					viewHolder.textViewAppName = (TextView) rowView.findViewById(R.id.textViewAppName);				
					viewHolder.imageViewAppIcon = (ImageView) rowView.findViewById(R.id.editIcon);
					//viewHolder.checkBoxEnableForApp = (CheckBox) rowView.findViewById(R.id.checkBoxEnableForApp);				
	
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
						appInfo.icon.setBounds(0, 0, 12, 12);
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
						
						if (((ToggleButton)btn).isChecked() )
						{
							// must show
							pkgSettings.addPackage(
									pkgSettings.new Package(
											appInfo.packageName, false, 0
									));
						}
						else
						{
							// must hide
							PackageSettings.Package pkg = pkgSettings.getPackage(appInfo.packageName);
							if (pkg != null)
								pkgSettings.deletePackage(pkg);
						}
					}
				});
	
				return rowView;
			}
			else
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
		}	
	}

	public class ViewHolder
	{
		TextView textViewAppName;
		ImageView imageViewAppIcon;
		
		ToggleButton btnShowHide;
	}
}
