//http://developer.android.com/about/versions/android-4.3.html#NotificationListener

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
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.widget.AdapterView.OnItemClickListener;

public class MainActivity extends Activity implements ServiceClient.Callback
{
	private static String TAG = "MainActivity";
	
	private ServiceClient serviceClient = null;

	private class ApplicationPkgInfo
	{
		PackageSettings.Package pkgInfo;
		Drawable icon;
		String name;
	}

	private ArrayList<ApplicationPkgInfo> handledApplications = null;
	
	private ToggleButton tbEnableService = null;

	private ListView listHandledApplications = null;

	
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

		setContentView(R.layout.activity_main);

		tbEnableService = (ToggleButton) findViewById(R.id.toggleButtonEnableService);

		listHandledApplications = (ListView) findViewById(R.id.listApplications);
		
		settings = new Settings(this);
		pkgSettings = new PackageSettings(this);

	
		listHandledApplications.setOnItemClickListener(new AdapterView.OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, final View view, int position, long id)
			{
				((ListApplicationsAdapter)listHandledApplications.getAdapter()).onItemClicked(position);
			}
		});
		
		tbEnableService.setChecked(settings.isServiceEnabled());

		
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

				if (tbEnableService.isChecked())
					serviceClient.checkPermissions();
			}
		};

		tbEnableService.setOnClickListener(saveSettingsOnClickListener);

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

		settings.setServiceEnabled(tbEnableService.isChecked());

		if (serviceClient != null)
			serviceClient.forceReloadConfig();
	}

	public class LoadPackagesTask extends AsyncTask<Void, Void, Void>
	{
		@Override
		protected Void doInBackground(Void... params)
		{
			Lw.d(TAG, "LoadPackagesTask::doInBackground");
			
			PackageSettings pkgSettings = new PackageSettings(MainActivity.this);
			
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
			
			synchronized (MainActivity.this)
			{
				applications = handledApplications;
			}
			
			listHandledApplications.setAdapter(	new ListApplicationsAdapter(MainActivity.this, applications));

			listHandledApplications.setSelection(0);
			
			synchronized (MainActivity.this)
			{
				listApplicationsLoader = null; // job is done, dispose 
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
			Lw.d(TAG, "ListApplicationsAdapter::onItemClicked, pos=" + position);
	
			final ApplicationPkgInfo appInfo = listApplications.get(position); 
			
			AlertDialog.Builder alert = new AlertDialog.Builder(MainActivity.this);

			alert.setTitle("Remind interval");
			
			LayoutInflater inflater = MainActivity.this.getLayoutInflater();
			
			View dialogView = inflater.inflate(R.layout.dlg_remind_interval, null);
			
			alert.setView(dialogView);
			
			final NumberPicker picker = (NumberPicker)dialogView.findViewById(R.id.numberPickerRemindInterval);

			picker.setMinValue(1);
			picker.setMaxValue(60);
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
				//viewHolder.checkBoxEnableForApp = (CheckBox) rowView.findViewById(R.id.checkBoxEnableForApp);				
				viewHolder.btnEnableForApp = (ToggleButton) rowView.findViewById(R.id.toggleButtonEnableForApp);				
				
				rowView.setTag(viewHolder);
			}
			
			final ApplicationPkgInfo appInfo = listApplications.get(position); // this would not change as well - why lookup twice then?
						
			//viewHolder.checkBoxEnableForApp.setChecked( appInfo.pkgInfo.isHandlingThis());
			viewHolder.btnEnableForApp.setChecked( appInfo.pkgInfo.isHandlingThis() );
			viewHolder.textViewRemindInterval.setText("every " + (appInfo.pkgInfo.getRemindIntervalSeconds() / 60) + " mins (click to change)");
			
			if (appInfo.name != null)
				viewHolder.textViewAppName.setText(appInfo.name);
			else
				viewHolder.textViewAppName.setText(appInfo.pkgInfo.getPackageName());
			
			if ( appInfo.icon != null)
				viewHolder.imageViewAppIcon.setImageDrawable( appInfo.icon );
			
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
