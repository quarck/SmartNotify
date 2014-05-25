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
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements ServiceClient.Callback
{
	private static String TAG = "MainActivity";
	
	private ServiceClient serviceClient = null;

	private boolean serviceEnabled = false;
	
	private class ApplicationPkgInfo
	{
		PackageSettings.Package pkgInfo;
		Drawable icon;
		String name;
	}

	private ArrayList<ApplicationPkgInfo> handledApplications = null;
	
	private ToggleButton toggleButtonEnableService = null;
	private ListView listHandledApplications = null;
	private TextView textViewlonelyHere = null;
	private TextView textViewListSmallPrint = null;
	
	private ListApplicationsAdapter listAdapter = null;
	
	private OnClickListener saveSettingsOnClickListener = null;

	private PackageSettings pkgSettings = null;
	private Settings settings = null;

	private LoadPackagesTask listApplicationsLoader = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		Lw.d("main activity created");
		
		Lw.d(TAG, "onCreateView");

		settings = new Settings(this);
		pkgSettings = new PackageSettings(this);

		setContentView(R.layout.activity_main);

		toggleButtonEnableService = (ToggleButton) findViewById(R.id.toggleButtonEnableService);
		listHandledApplications = (ListView) findViewById(R.id.listApplications);
		textViewlonelyHere = (TextView) findViewById(R.id.textViewLonelyHere);
		textViewListSmallPrint = (TextView) findViewById(R.id.textViewLblEnablePerAppSmallprint);

		listHandledApplications.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, final View view, int position, long id)
			{
				((ListApplicationsAdapter)listHandledApplications.getAdapter()).onItemClicked(position);
			}
		});
		
		serviceEnabled = settings.isServiceEnabled();
		toggleButtonEnableService.setChecked(serviceEnabled);

		synchronized(this)
		{
			listApplicationsLoader = new LoadPackagesTask();
			listApplicationsLoader.execute();
		}
		
		saveSettingsOnClickListener = new OnClickListener()
		{
			public void onClick(View arg0)
			{
				Lw.d("saveSettingsOnClickListener.onClick()");

				saveSettings();

				if (serviceEnabled)
					serviceClient.checkPermissions();
			}
		};

		toggleButtonEnableService.setOnClickListener(saveSettingsOnClickListener);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item)
	{
		int id = item.getItemId();
		if (id == R.id.action_settings)
		{
			Intent intent = new Intent(this, SettingsActivity.class);
			startActivity(intent);
		}
		else if (id == R.id.action_edit_applications)
		{
			Intent intent = new Intent(this, EditApplicationsActivity.class);
			startActivity(intent);
		}
		return super.onOptionsItemSelected(item);
	}
	
	
	@Override
	public void onNoPermissions()
	{
		Lw.d(TAG, "onNoPermissions()!!!");

		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(R.string.application_has_no_access).setCancelable(false)
				.setPositiveButton(R.string.open_settings, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
						startActivity(intent);
					}
				}).setNegativeButton(R.string.cancel_quit, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, int id)
					{
						finish();
					}
				});

		// Create the AlertDialog object and return it
		builder.create().show();
	}

	private void saveSettings()
	{
		Lw.d(TAG, "Saving current settings");

		serviceEnabled = toggleButtonEnableService.isChecked();	
		settings.setServiceEnabled(serviceEnabled);

		if (serviceClient != null)
			serviceClient.forceReloadConfig();
		
		synchronized(this)
		{
			listAdapter.notifyDataSetChanged();
		}
	}

	public class LoadPackagesTask extends AsyncTask<Void, Void, Void>
	{
		@Override
		protected Void doInBackground(Void... params)
		{
			Lw.d(TAG, "LoadPackagesTask::doInBackground");
			
			PackageSettings pkgSettings = new PackageSettings(MainActivity.this);

			if (!settings.isInitialPopulated())
			{
				new InitialPopulate().populate(MainActivity.this, pkgSettings);
				settings.setInitialPopulated(true);
			}
			
			PackageManager packageManager = getPackageManager();

			List<PackageSettings.Package> allPackages = pkgSettings.getAllPackages();
			
			ArrayList<ApplicationPkgInfo> applications = new ArrayList<ApplicationPkgInfo>();
			
			for (PackageSettings.Package pkg : allPackages)
			{
				ApplicationPkgInfo ai = new ApplicationPkgInfo();
				ai.pkgInfo = pkg;
				
				ApplicationInfo pmAppInfo;
				try
				{
					pmAppInfo = packageManager.getApplicationInfo(pkg.getPackageName(), PackageManager.GET_META_DATA);
					ai.name = packageManager.getApplicationLabel(pmAppInfo).toString();
					ai.icon = pmAppInfo.loadIcon(packageManager);
				}
				catch (NameNotFoundException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}			
				
				applications.add(ai);
			}
			
			Comparator<ApplicationPkgInfo> comparator = new Comparator<ApplicationPkgInfo>() 
			{
				@Override
		        public int compare(ApplicationPkgInfo  app1, ApplicationPkgInfo  app2)
		        {
		            return  app1.name.compareTo(app2.name);
		        }
		    };
			
			Collections.sort(applications, comparator);

			
			synchronized(this)
			{
				handledApplications =  applications;
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
			ArrayList<ApplicationPkgInfo> applications = null;

			ListApplicationsAdapter adapter = null;
			
			synchronized (MainActivity.this)
			{
				applications = handledApplications;
			}
			
			if (applications.isEmpty())
			{
				textViewlonelyHere.setVisibility(View.VISIBLE);
				listHandledApplications.setVisibility(View.GONE);
				textViewListSmallPrint.setVisibility(View.GONE);
			}
			else
			{			
				listHandledApplications.setVisibility(View.VISIBLE);
				textViewlonelyHere.setVisibility(View.VISIBLE);			
				textViewlonelyHere.setVisibility(View.GONE);	
				
				adapter = new ListApplicationsAdapter(MainActivity.this, applications);
				listHandledApplications.setAdapter(	adapter);
				listHandledApplications.setSelection(0);
			}
			
			synchronized (MainActivity.this)
			{
				listApplicationsLoader = null; // job is done, dispose
				listAdapter = adapter;
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
		serviceClient = new ServiceClient(this);
		serviceClient.bindService(getApplicationContext());
	}

	@Override
	public void onStop()
	{
		Lw.d(TAG, "onStop()");
		serviceClient.unbindService(getApplicationContext());
		super.onStop();
	}
	
	@Override 
	public void onPause()
	{
		Lw.d(TAG, "onPause");

		LoadPackagesTask loader;
		
		synchronized(this)
		{
			loader = listApplicationsLoader;
			listApplicationsLoader = null;
		}
		
		if (loader != null)
			loader.cancel(false);

		super.onPause();
	}
	
	@Override 
	public void onResume()
	{
		Lw.d(TAG, "onResume");

		super.onResume();
		
		synchronized(this)
		{
			listApplicationsLoader = new LoadPackagesTask();
			listApplicationsLoader.execute();
		}
	}

	@Override
	public void onNotificationList(String[] notifications)
	{
		Lw.d(TAG, "OnNotificationList()");

		if (notifications != null)
		{
			StringBuilder sb = new StringBuilder();

			if (notifications != null)
				for (String ntf : notifications)
				{
					sb.append(ntf);
					sb.append("\n");
				}

			Toast.makeText(this, sb.toString(), Toast.LENGTH_LONG).show();
		}
		else
		{
			onNoPermissions();
		}
	}

	private class ListApplicationsAdapter extends BaseAdapter
	{
		private final Context context;
		
		ArrayList<ApplicationPkgInfo> listApplications;
		
		public ListApplicationsAdapter(Context ctx, ArrayList<ApplicationPkgInfo> applications)
		{
			super();
			context = ctx;			
			listApplications = applications;
		}

		public void onItemClicked(int position)
		{
			if (!serviceEnabled)
			{
				Lw.d(TAG, "ListApplicationsAdapter::onItemClicked, service is disbaled");
				return;
			}
			
			Lw.d(TAG, "ListApplicationsAdapter::onItemClicked, pos=" + position);
	
			final ApplicationPkgInfo appInfo = listApplications.get(position); 
			
			AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);

			alert.setTitle("Remind interval");
			
			LayoutInflater inflater = MainActivity.this.getLayoutInflater();
			
			View dialogView = inflater.inflate(R.layout.dlg_remind_interval, null);
			
			alert.setView(dialogView);
			
			final NumberPicker picker = (NumberPicker)dialogView.findViewById(R.id.numberPickerRemindInterval);

			picker.setMinValue(1);
			picker.setMaxValue(120);
			picker.setValue(appInfo.pkgInfo.getRemindIntervalSeconds() / 60);
			
			alert.setPositiveButton("Ok", new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int whichButton)
				{
					int interval = picker.getValue();
					
					Lw.d(TAG, "got val: " + interval );//value.toString());
					
					try
					{
						appInfo.pkgInfo.setRemindIntervalSeconds( interval * 60 );						
						pkgSettings.updatePackage(appInfo.pkgInfo);
						
						Lw.d(TAG, "remind interval updated to " + interval + " for package " + appInfo.pkgInfo);
					}
					catch (Exception ex)
					{
					}
					
					notifyDataSetChanged();
				}
			});

			alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
			{
				public void onClick(DialogInterface dialog, int whichButton)
				{
				}
			});

			alert.show();
		}

		@Override
		public int getCount()
		{
			return listApplications.size();
		}

		@Override
		public Object getItem(int position)
		{
			return listApplications.get(position);
		}

		@Override
		public long getItemId(int position)
		{
			return position;
		}
		
		@Override
		public int getViewTypeCount()
		{
			return 2;
		}

		@Override
		public int getItemViewType(int position)	
		{
			return 0;
		}
		
		@Override
		public View getView(int position, View convertView, ViewGroup parent)
		{
			View rowView = convertView;
			
			ViewHolder viewHolder = rowView != null ? (ViewHolder)rowView.getTag() : null;

			if (viewHolder == null)
			{
				LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				
				rowView = inflater.inflate(R.layout.list_item, parent, false);

				viewHolder = new ViewHolder();

				viewHolder.textViewRemindInterval = (TextView) rowView.findViewById(R.id.textViewIntervalLabel);				
				viewHolder.textViewAppName = (TextView) rowView.findViewById(R.id.textViewAppName);				
				viewHolder.imageViewAppIcon = (ImageView) rowView.findViewById(R.id.icon);
				viewHolder.btnEnableForApp = (ToggleButton) rowView.findViewById(R.id.toggleButtonEnableForApp);				
				
				rowView.setTag(viewHolder);
			}
			
			final ApplicationPkgInfo appInfo = listApplications.get(position); // this would not change as well - why lookup twice then?
						
			viewHolder.btnEnableForApp.setChecked( appInfo.pkgInfo.isHandlingThis() );
			viewHolder.textViewRemindInterval.setText("every " + (appInfo.pkgInfo.getRemindIntervalSeconds() / 60) + " mins (click to change)");
			
			if (appInfo.name != null)
				viewHolder.textViewAppName.setText(appInfo.name);
			else
				viewHolder.textViewAppName.setText(appInfo.pkgInfo.getPackageName());
			
			if ( appInfo.icon != null)
				viewHolder.imageViewAppIcon.setImageDrawable( appInfo.icon );
			
			viewHolder.btnEnableForApp.setEnabled(serviceEnabled);
			viewHolder.textViewRemindInterval.setEnabled(serviceEnabled);
			viewHolder.imageViewAppIcon.setEnabled(serviceEnabled);
			viewHolder.textViewAppName.setEnabled(serviceEnabled);

			viewHolder.btnEnableForApp.setOnClickListener(new OnClickListener()
			{
				public void onClick(View btn)
				{
					Lw.d("saveSettingsOnClickListener.onClick()");

					appInfo.pkgInfo.setHandlingThis( ((ToggleButton)btn).isChecked() );					
					pkgSettings.updatePackage(appInfo.pkgInfo);					
					saveSettings();
				}
			});
			
			return rowView;
		}	

		public class ViewHolder
		{
			ToggleButton btnEnableForApp;
			TextView textViewRemindInterval;
			TextView textViewAppName;
			ImageView imageViewAppIcon;
		}
	}

	@Override
	public void onRecetNotificationsList(String[] recentNotifications)
	{
		// TODO Auto-generated method stub
	}
}
