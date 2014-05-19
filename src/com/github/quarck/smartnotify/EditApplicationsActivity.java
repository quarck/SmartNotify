package com.github.quarck.smartnotify;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import android.app.ActionBar;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
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
	
	private ArrayList<AppSelectionInfo> listApps = new ArrayList<AppSelectionInfo>();
	
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

		List<ApplicationInfo> applications = packageManager.getInstalledApplications(PackageManager.GET_META_DATA);

		ArrayList<AppSelectionInfo> handledApps = new ArrayList<AppSelectionInfo>();
		ArrayList<AppSelectionInfo> visibleApps = new ArrayList<AppSelectionInfo>();
		ArrayList<AppSelectionInfo> otherApps = new ArrayList<AppSelectionInfo>();
		
		Lw.d(TAG, "Loading applications");
		
		for (ApplicationInfo app : applications)
		{
			AppSelectionInfo asi = new AppSelectionInfo();
			
			if (app.packageName == null)
				continue;
			
			asi.packageName = app.packageName;			
			asi.isSelected = pkgSettings.getIsListed( app.packageName );
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
		
		Comparator<AppSelectionInfo> comparator = new Comparator<AppSelectionInfo>() 
		{
			@Override
	        public int compare(AppSelectionInfo  app1, AppSelectionInfo  app2)
	        {
	            return  app1.name.compareTo(app2.name);
	        }
	    };
		
		Collections.sort(handledApps, comparator);
		Collections.sort(visibleApps, comparator);
		
		synchronized (this)
		{
			listApps.clear();
			listApps.addAll(handledApps);
			listApps.addAll(visibleApps);
			//listApps.addAll(otherApps);
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
			ArrayList<AppSelectionInfo> applications = null;
			
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
				apps = listApps;
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

		ArrayList<AppSelectionInfo> listApplications;

		PackageManager packageManager = getPackageManager();
		
		public ListApplicationsAdapter(Context ctx, int textViewResourceId, ArrayList<AppSelectionInfo> applications)
		{
			super();
			
			context = ctx;
			
			listApplications = applications;
			
			Lw.d(TAG, "Adding list of applications:");
			for(AppSelectionInfo asi : listApplications)
			{
				Lw.d(TAG, " ... " + asi.packageName );
			}
		}

		public int getCount()
		{
			return listApplications.size();
		}

		public Object getItem(int position)
		{
			return listApplications.get(position);
		}

		public long getItemId(int position)
		{
			return position;
		}

		
		@Override
		public View getView(int position, View convertView, ViewGroup parent)
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
				//viewHolder.checkBoxEnableForApp = (CheckBox) rowView.findViewById(R.id.checkBoxEnableForApp);				


				rowView.setTag(viewHolder);
			}
						
			ViewHolder viewHolder = (ViewHolder)rowView.getTag();
			
			final AppSelectionInfo appInfo = listApplications.get(position);
			
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
					
					if (((CheckBox)btn).isChecked() )
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
	}

	public class ViewHolder
	{
		TextView textViewAppName;
		ImageView imageViewAppIcon;
		
		CheckBox btnShowHide;
	}
}
