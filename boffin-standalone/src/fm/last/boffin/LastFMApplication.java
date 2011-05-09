/***************************************************************************
 *   Copyright 2005-2009 Last.fm Ltd.                                      *
 *   Portions contributed by Casey Link, Lukasz Wisniewski,                *
 *   Mike Jennings, and Michael Novak Jr.                                  *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.         *
 ***************************************************************************/
package fm.last.boffin;

import java.util.Locale;

import android.app.AlertDialog;
import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager.NameNotFoundException;
import android.net.Uri;
import android.os.IBinder;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.apps.analytics.GoogleAnalyticsTracker;

import fm.last.api.Session;
import fm.last.api.WSError;
import fm.last.boffin.activity.Player;
import fm.last.boffin.player.RadioPlayerService;
import fm.last.util.UrlUtil;

public class LastFMApplication extends Application {

	public Session session;
	public fm.last.boffin.player.IRadioPlayer player = null;
	public Context mCtx;
	public GoogleAnalyticsTracker tracker;

	private static LastFMApplication instance = null;

	public static LastFMApplication getInstance() {
		if(instance != null) {
			return instance;
		} else {
			return new LastFMApplication();
		}
	}

	@Override
	public void onCreate() {
		super.onCreate();
		instance = this;

		String version;
		try {
			version = "/" + LastFMApplication.getInstance().getPackageManager().getPackageInfo("fm.last.boffin", 0).versionName;
		} catch (Exception e) {
			version = "";
		}
		
		UrlUtil.useragent = "MobileLastFM" + version + " (" + android.os.Build.MODEL + "; " + Locale.getDefault().getCountry().toLowerCase() + "; "
				+ "Android " + android.os.Build.VERSION.RELEASE + ")";

		tracker = GoogleAnalyticsTracker.getInstance();
		tracker.start(PrivateAPIKey.ANALYTICS_ID, this);
		
		version = "0.1";
		try {
			version = getPackageManager().getPackageInfo("fm.last.boffin", 0).versionName;
		} catch (NameNotFoundException e) {
		}
	}

	private ServiceConnection mConnection = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			player = fm.last.boffin.player.IRadioPlayer.Stub.asInterface(service);
		}

		public void onServiceDisconnected(ComponentName className) {
			player = null;
		}
	};

	public void bindPlayerService() {
		// start our media player service
		Intent mpIntent = new Intent(this, fm.last.boffin.player.RadioPlayerService.class);
		boolean b = bindService(mpIntent, mConnection, BIND_AUTO_CREATE);
		if (!b) {
			// something went wrong
			// mHandler.sendEmptyMessage(QUIT);
			System.out.println("Binding to service failed " + mConnection);
		}
	}

	public void unbindPlayerService() {
		try {
		if(player != null && player.asBinder().isBinderAlive())
			unbindService(mConnection);
		} catch (Exception e) {
		}
		player = null;
	}

	public void playRadioStation(Context ctx, String url, boolean showPlayer) {
		mCtx = ctx;
				
		final Intent out = new Intent(this, RadioPlayerService.class);
		out.setAction("fm.last.boffin.PLAY");
		out.putExtra("station", url);
		out.putExtra("session", (Parcelable) session);
		startService(out);
		if (showPlayer) {
			IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(RadioPlayerService.STATION_CHANGED);
			intentFilter.addAction("fm.last.boffin.ERROR");

			BroadcastReceiver statusListener = new BroadcastReceiver() {

				@Override
				public void onReceive(Context context, Intent intent) {
					try {
						unregisterReceiver(this);
					} catch (Exception e) {
						e.printStackTrace(); //Sometimes this can throw an IllegalArgumentException
					}
					
					String action = intent.getAction();
					if (action.equals(RadioPlayerService.STATION_CHANGED)) {
						Intent i = new Intent(LastFMApplication.this, Player.class);
						i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
						startActivity(i);
					} else if (action.equals("fm.last.boffin.ERROR")) {
						WSError e = intent.getParcelableExtra("error");
						if(e != null) {
							Log.e("Last.fm", "Tuning error: " + e.getMessage());
						}
						presentError(mCtx, e);
					}
				}
			};
			registerReceiver(statusListener, intentFilter);
		}
	}


	@Override
	public void onTerminate() {
		session = null;
		instance = null;
		tracker.stop();
		super.onTerminate();
	}

	public void presentError(Context ctx, WSError error) {
		int title = 0;
		int description = 0;

		if(error != null) {
			Log.e("Last.fm", "Received a webservice error during method: " + error.getMethod() + ", message: " + error.getMessage());
			try {
				LastFMApplication.getInstance().tracker.trackEvent("Errors", // Category
						error.getMethod(), // Action
						error.getMessage(), // Label
						0); // Value
			} catch (Exception e) {
				//Google Analytics doesn't appear to be thread safe
			}

			if (error.getMethod().startsWith("radio.")) {
				title = R.string.ERROR_STATION_TITLE;
				switch (error.getCode()) {
				case WSError.ERROR_NotEnoughContent:
					title = R.string.ERROR_INSUFFICIENT_CONTENT_TITLE;
					description = R.string.ERROR_INSUFFICIENT_CONTENT;
					break;
	
				case WSError.ERROR_NotEnoughFans:
					description = R.string.ERROR_INSUFFICIENT_FANS;
					break;
	
				case WSError.ERROR_NotEnoughMembers:
					description = R.string.ERROR_INSUFFICIENT_MEMBERS;
					break;
	
				case WSError.ERROR_NotEnoughNeighbours:
					description = R.string.ERROR_INSUFFICIENT_NEIGHBOURS;
					break;
					
				case WSError.ERROR_RadioUnavailable:
				case WSError.ERROR_AuthenticationFailed:
				case WSError.ERROR_GeoRestricted:
					description = R.string.ERROR_RADIO_UNAVAILABLE;
					break;
					
				case WSError.ERROR_TrialExpired:
					title = R.string.ERROR_TRIAL_EXPIRED_TITLE;
					description = R.string.ERROR_TRIAL_EXPIRED;
					AlertDialog.Builder d = new AlertDialog.Builder(ctx);
					d.setTitle(getResources().getString(title));
					d.setMessage(getResources().getString(description));
					d.setIcon(android.R.drawable.ic_dialog_alert);
					d.setPositiveButton("Subscribe", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
							Intent i = new Intent(Intent.ACTION_VIEW);
							i.setData(Uri.parse("http://www.last.fm/subscribe"));
							i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
							startActivity(i);
						}
					});
					d.setNegativeButton("Later", new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int whichButton) {
						}
					});
					try {
						d.show();
					} catch (Exception e) {
					}
					return;
					
				case WSError.ERROR_Deprecated:
					title = R.string.ERROR_DEPRECATED_TITLE;
					Intent i = new Intent(RadioPlayerService.STATION_CHANGED);
					sendBroadcast(i);
					break;
				}
			}
	
			if (error.getMethod().equals("user.signUp")) {
				title = R.string.ERROR_SIGNUP_TITLE;
				switch (error.getCode()) {
				case WSError.ERROR_InvalidParameters:
					presentError(ctx, getResources().getString(title), error.getMessage());
					return;
	
				}
			}
		}
		
		if (title == 0)
			title = R.string.ERROR_SERVER_UNAVAILABLE_TITLE;

		if (description == 0) {
			if(error != null) {
				switch (error.getCode()) {
				case WSError.ERROR_AuthenticationFailed:
				case WSError.ERROR_InvalidSession:
					title = R.string.ERROR_SESSION_TITLE;
					description = R.string.ERROR_SESSION;
					break;
				case WSError.ERROR_InvalidAPIKey:
					title = R.string.ERROR_UPGRADE_TITLE;
					description = R.string.ERROR_UPGRADE;
					break;
				case WSError.ERROR_SubscribersOnly:
					title = R.string.ERROR_SUBSCRIPTION_TITLE;
					description = R.string.ERROR_SUBSCRIPTION;
					break;
				default:
					presentError(ctx, getResources().getString(title), getResources().getString(R.string.ERROR_SERVER_UNAVAILABLE) + "\n\n" + error.getMethod() + ": " + error.getMessage());
					return;
				}
			} else {
				description = R.string.ERROR_SERVER_UNAVAILABLE;
			}
		}

		presentError(ctx, getResources().getString(title), getResources().getString(description));
	}

	public void presentError(Context ctx, String title, String description) {
		AlertDialog.Builder d = new AlertDialog.Builder(ctx);
		d.setTitle(title);
		d.setMessage(description);
		d.setIcon(android.R.drawable.ic_dialog_alert);
		d.setNeutralButton("OK", new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int whichButton) {
			}
		});
		try {
			d.show();
		} catch (Exception e) {
			Intent intent = new Intent(LastFMApplication.this, Player.class);
			intent.putExtra("ERROR_TITLE", title);
			intent.putExtra("ERROR_DESCRIPTION", description);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
		}
	}
	
	public void logout() {
	}
}
