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
package fm.last.boffin.activity;

import java.io.IOException;
import java.util.Formatter;
import java.util.concurrent.RejectedExecutionException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.app.SearchManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.database.sqlite.SQLiteException;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.PowerManager;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Animation.AnimationListener;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import fm.last.boffin.player.IRadioPlayer;
import fm.last.api.Album;
import fm.last.api.Event;
import fm.last.api.ImageUrl;
import fm.last.api.LastFmServer;
import fm.last.api.Station;
import fm.last.api.WSError;
import fm.last.boffin.Amazon;
import fm.last.boffin.AndroidLastFmServerFactory;
import fm.last.boffin.LastFMApplication;
import fm.last.boffin.R;
import fm.last.boffin.player.RadioPlayerService;
import fm.last.boffin.utils.AsyncTaskEx;
import fm.last.boffin.widget.AdArea;
import fm.last.boffin.widget.AlbumArt;

public class Player extends Activity {

	private ImageButton mLoveButton;
	private ImageButton mBanButton;
	private ImageButton mStopButton;
	private ImageButton mNextButton;
	private AlbumArt mAlbum;
	private TextView mCurrentTime;
	private TextView mTotalTime;
	private TextView mArtistName;
	private TextView mTrackName;
	private TextView mTrackContext;
	private ProgressBar mProgress;
	private long mDuration;
	private boolean paused;
	private boolean loved = false;

	private ProgressDialog mTuningDialog;

	private String mCachedArtist = null;
	private String mCachedTrack = null;
	private Bitmap mCachedBitmap = null;

	private static final int REFRESH = 1;

	private PowerManager.WakeLock wakelock = null;
	
	LastFmServer mServer = AndroidLastFmServerFactory.getServer();

	private IntentFilter mIntentFilter;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		requestWindowFeature(Window.FEATURE_NO_TITLE);

		setContentView(R.layout.audio_player);
		setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);

		mCurrentTime = (TextView) findViewById(R.id.currenttime);
		mTotalTime = (TextView) findViewById(R.id.totaltime);
		mProgress = (ProgressBar) findViewById(android.R.id.progress);
		mProgress.setMax(1000);
		mAlbum = (AlbumArt) findViewById(R.id.album);
		LayoutParams params = mAlbum.getLayoutParams();
		if (AdArea.adsEnabled(this)) {
			params.width -= 54;
			params.height -= 54;
		}
		mAlbum.setLayoutParams(params);
		mArtistName = (TextView) findViewById(R.id.track_artist);
		mTrackName = (TextView) findViewById(R.id.track_title);
		mTrackContext = (TextView) findViewById(R.id.track_context);
		mTrackContext.setVisibility(View.GONE);

		mLoveButton = (ImageButton) findViewById(R.id.love);
		mLoveButton.setOnClickListener(mLoveListener);
		mBanButton = (ImageButton) findViewById(R.id.ban);
		mBanButton.setOnClickListener(mBanListener);
		mStopButton = (ImageButton) findViewById(R.id.stop);
		mStopButton.requestFocus();
		mStopButton.setOnClickListener(mStopListener);
		mNextButton = (ImageButton) findViewById(R.id.skip);
		mNextButton.setOnClickListener(mNextListener);

		LastFMApplication.getInstance().bindService(
				new Intent(LastFMApplication.getInstance(),
						fm.last.boffin.player.RadioPlayerService.class),
				new ServiceConnection() {
					public void onServiceConnected(ComponentName comp,
							IBinder binder) {
						IRadioPlayer player = IRadioPlayer.Stub
								.asInterface(binder);
						try {
							String url = player.getStationUrl();
							
							if(url != null &&
									(url.startsWith("lastfm://playlist/") || url.startsWith("lastfm://usertags/") || url.endsWith("/loved"))) {
								findViewById(R.id.noticeContainer).setVisibility(View.VISIBLE);
								TextView notice = (TextView) findViewById(R.id.notice);
								notice.setSelected(true);
								notice.setOnClickListener(new View.OnClickListener() {
	
									public void onClick(View v) {
										Intent i = new Intent(Intent.ACTION_VIEW);
										i.setData(Uri.parse("http://www.last.fm/stationchanges2010"));
										startActivity(i);
									}
									
								});
							}
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						LastFMApplication.getInstance().unbindService(this);
					}

					public void onServiceDisconnected(ComponentName comp) {
					}
				}, Context.BIND_AUTO_CREATE);
		
		ImageButton dismiss = (ImageButton) findViewById(R.id.dismiss);
		dismiss.setOnClickListener(new View.OnClickListener() {

			public void onClick(View v) {
				findViewById(R.id.noticeContainer).setVisibility(View.GONE);
			}
			
		});
		
		mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(RadioPlayerService.META_CHANGED);
		mIntentFilter.addAction(RadioPlayerService.PLAYBACK_FINISHED);
		mIntentFilter.addAction(RadioPlayerService.PLAYBACK_STATE_CHANGED);
		mIntentFilter.addAction(RadioPlayerService.STATION_CHANGED);
		mIntentFilter.addAction(RadioPlayerService.PLAYBACK_ERROR);
		mIntentFilter.addAction("fm.last.boffin.ERROR");

		if (icicle != null) {
			mCachedArtist = icicle.getString("artist");
			mCachedTrack = icicle.getString("track");
			mCachedBitmap = icicle.getParcelable("artwork");
			loved = icicle.getBoolean("loved", false);
			if (loved) {
				mLoveButton.setImageResource(R.drawable.loved);
			} else {
				mLoveButton.setImageResource(R.drawable.love);
			}
		}
	}

	@Override
	public void onStart() {
		super.onStart();
		paused = false;
	}

	@Override
	public void onStop() {

		paused = true;
		mHandler.removeMessages(REFRESH);

		super.onStop();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		outState.putBoolean("configchange", getChangingConfigurations() != 0);
		outState.putString("artist", mArtistName.getText().toString());
		outState.putString("track", mTrackName.getText().toString());
		outState.putParcelable("artwork", mAlbum.getBitmap());
		outState.putBoolean("loved",
				loved);
		super.onSaveInstanceState(outState);
	}

	@Override
	protected void onPause() {
		try {
			unregisterReceiver(mStatusListener);
		} catch(IllegalArgumentException e) {
			//The listener wasn't registered yet
		}
		mHandler.removeMessages(REFRESH);
		if (LastFMApplication.getInstance().player != null)
			LastFMApplication.getInstance().unbindPlayerService();
		if(wakelock != null && wakelock.isHeld())
			wakelock.release();
		super.onPause();
	}

	@Override
	public void onResume() {
		super.onResume();

		if(PreferenceManager.getDefaultSharedPreferences(LastFMApplication.getInstance()).getBoolean("screen_wakelock", false)) {
			PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
			wakelock = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "Last.fm");
			wakelock.acquire();
		}

		registerReceiver(mStatusListener, mIntentFilter);
		if (LastFMApplication.getInstance().player == null)
			LastFMApplication.getInstance().bindPlayerService();
		updateTrackInfo();

		try {
			new RefreshTask().execute((Void)null);
		} catch (RejectedExecutionException e) {
			queueNextRefresh(500);
		}
		
		try {
			LastFMApplication.getInstance().tracker.trackPageView("/Player");
		} catch (Exception e) {
			//Google Analytics doesn't appear to be thread safe
		}
	}

	@Override
	public void onDestroy() {
		mAlbum.cancel();
		super.onDestroy();
	}

	private View.OnClickListener mLoveListener = new View.OnClickListener() {

		public void onClick(View v) {
			Intent i = new Intent("fm.last.boffin.LOVE");
			sendBroadcast(i);
			bindService(new Intent(Player.this,
					fm.last.boffin.player.RadioPlayerService.class),
					new ServiceConnection() {
						public void onServiceConnected(ComponentName comp,
								IBinder binder) {
							IRadioPlayer player = IRadioPlayer.Stub
									.asInterface(binder);
							try {
								if (player.isPlaying())
									player.setLoved(true);
							} catch (RemoteException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							unbindService(this);
						}

						public void onServiceDisconnected(ComponentName comp) {
						}
					}, 0);
			mLoveButton.setImageResource(R.drawable.loved);
			loved = true;
			
			try {
				LastFMApplication.getInstance().tracker.trackEvent("Clicks", // Category
						"player-love", // Action
						"", // Label
						0); // Value
			} catch (Exception e) {
				//Google Analytics doesn't appear to be thread safe
			}
		}
	};

	private View.OnClickListener mBanListener = new View.OnClickListener() {

		public void onClick(View v) {
			Intent i = new Intent("fm.last.boffin.BAN");
			sendBroadcast(i);
			try {
				LastFMApplication.getInstance().tracker.trackEvent("Clicks", // Category
						"player-ban", // Action
						"", // Label
						0); // Value
			} catch (Exception e) {
				//Google Analytics doesn't appear to be thread safe
			}
			bindService(new Intent(Player.this,
					fm.last.boffin.player.RadioPlayerService.class),
					new ServiceConnection() {
						public void onServiceConnected(ComponentName comp,
								IBinder binder) {
							IRadioPlayer player = IRadioPlayer.Stub
									.asInterface(binder);
							try {
								if (player.isPlaying())
									player.skip();
							} catch (RemoteException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							unbindService(this);
						}

						public void onServiceDisconnected(ComponentName comp) {
						}
					}, 0);
		}
	};

	private View.OnClickListener mNextListener = new View.OnClickListener() {

		public void onClick(View v) {
			try {
				LastFMApplication.getInstance().tracker.trackEvent("Clicks", // Category
						"player-skip", // Action
						"", // Label
						0); // Value
			} catch (Exception e) {
				//Google Analytics doesn't appear to be thread safe
			}
			bindService(new Intent(Player.this,
					fm.last.boffin.player.RadioPlayerService.class),
					new ServiceConnection() {
						public void onServiceConnected(ComponentName comp,
								IBinder binder) {
							IRadioPlayer player = IRadioPlayer.Stub
									.asInterface(binder);
							try {
								if (player.isPlaying() || player.getState() == RadioPlayerService.STATE_PAUSED)
									player.skip();
							} catch (RemoteException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							unbindService(this);
						}

						public void onServiceDisconnected(ComponentName comp) {
						}
					}, 0);
		}
	};

	private View.OnClickListener mStopListener = new View.OnClickListener() {

		public void onClick(View v) {
			try {
				LastFMApplication.getInstance().tracker.trackEvent("Clicks", // Category
						"player-stop", // Action
						"", // Label
						0); // Value
			} catch (Exception e) {
				//Google Analytics doesn't appear to be thread safe
			}

			bindService(new Intent(Player.this,
					fm.last.boffin.player.RadioPlayerService.class),
					new ServiceConnection() {
						public void onServiceConnected(ComponentName comp,
								IBinder binder) {
							IRadioPlayer player = IRadioPlayer.Stub
									.asInterface(binder);
							try {
								if (player.getState() == RadioPlayerService.STATE_PAUSED)
									LastFMApplication.getInstance().playRadioStation(Player.this, player.getStationUrl(), false);
								else if (player.getState() != RadioPlayerService.STATE_STOPPED)
									player.pause();
							} catch (RemoteException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
							unbindService(this);
						}

						public void onServiceDisconnected(ComponentName comp) {
						}
					}, 0);
		}
	};

	private BroadcastReceiver mStatusListener = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();
			if (action.equals(RadioPlayerService.META_CHANGED)) {
				// redraw the artist/title info and
				// set new max for progress bar
				updateTrackInfo();
			} else if (action.equals(RadioPlayerService.PLAYBACK_FINISHED)) {
				finish();
			} else if (action.equals(RadioPlayerService.STATION_CHANGED)) {
				// FIXME: this *should* be handled by the metadata activity now
				// if(mDetailFlipper.getDisplayedChild() == 1)
				// mDetailFlipper.showPrevious();
			} else if (action.equals(RadioPlayerService.PLAYBACK_ERROR) || action.equals("fm.last.boffin.ERROR")) {
				// TODO add a skip counter and try to skip 3 times before
				// display an error message
				if (mTuningDialog != null) {
					mTuningDialog.dismiss();
					mTuningDialog = null;
				}
				WSError error = intent.getParcelableExtra("error");
				if (error != null) {
					LastFMApplication.getInstance().presentError(Player.this,
							error);
				} else {
					LastFMApplication.getInstance().presentError(
							Player.this,
							getResources().getString(
									R.string.ERROR_PLAYBACK_FAILED_TITLE),
							getResources().getString(
									R.string.ERROR_PLAYBACK_FAILED));
				}
			}
		}
	};

	private void updateTrackInfo() {
		LastFMApplication.getInstance().bindService(
				new Intent(LastFMApplication.getInstance(),
						fm.last.boffin.player.RadioPlayerService.class),
				new ServiceConnection() {
					public void onServiceConnected(ComponentName comp,
							IBinder binder) {
						IRadioPlayer player = IRadioPlayer.Stub
								.asInterface(binder);
						try {
							String artistName = player.getArtistName();
							String trackName = player.getTrackName();
							String[] trackContext = player.getContext();
							String stationURL = player.getStationUrl();
							loved = player.getLoved();
							
							
							if (loved) {
								mLoveButton.setImageResource(R.drawable.loved);
							} else {
								mLoveButton.setImageResource(R.drawable.love);
							}

							if ((mArtistName != null && mArtistName.getText() != null && mTrackName != null && mTrackName.getText() != null) && (!mArtistName.getText().equals(artistName)
									|| !mTrackName.getText().equals(trackName))) {
								if (artistName == null || artistName
										.equals(RadioPlayerService.UNKNOWN)) {
									mArtistName.setText("");
								} else {
									mArtistName.setText(artistName);
								}
								if (trackName == null || trackName
										.equals(RadioPlayerService.UNKNOWN)) {
									mTrackName.setText("");
								} else {
									mTrackName.setText(trackName);
								}
								if (trackContext == null || trackContext.length == 0) {
									mTrackContext.setVisibility(View.GONE);
									mTrackContext.setText("");
								} else {
									String context = "";
									if(stationURL.endsWith("/friends") || stationURL.endsWith("/neighbours") || stationURL.contains("/friends/") || stationURL.contains("/neighbours/"))
										context += "From ";
									else
										context += "Similar to ";
									
									context += trackContext[0];
									if(stationURL.endsWith("/friends") || stationURL.endsWith("/neighbours") || stationURL.contains("/friends/") || stationURL.contains("/neighbours/"))
										if(context.endsWith("s"))
											context += "'";
										else
											context += "'s";

									if(trackContext.length > 1) {
										context += " and " + trackContext[1];
										if(stationURL.endsWith("/friends") || stationURL.endsWith("/neighbours") || stationURL.contains("/friends/") || stationURL.contains("/neighbours/"))
											if(context.endsWith("s"))
												context += "'";
											else
												context += "'s";
									}
									if(stationURL.endsWith("/friends") || stationURL.endsWith("/neighbours") || stationURL.contains("/friends/") || stationURL.contains("/neighbours/"))
										if(trackContext.length > 1)
											context += " libraries";
										else
											context += " library";
									
									mTrackContext.setVisibility(View.VISIBLE);
									mTrackContext.setText(context);
								}

								if (mTuningDialog != null
										&& player.getState() == RadioPlayerService.STATE_TUNING) {
									mTuningDialog = ProgressDialog.show(
											Player.this, "",
											getString(R.string.player_tuning),
											true, false);
									mTuningDialog
											.setVolumeControlStream(android.media.AudioManager.STREAM_MUSIC);
									mTuningDialog.setCancelable(true);
								}

								if (mCachedArtist != null
										&& mCachedArtist.equals(artistName)
										&& mCachedTrack != null
										&& mCachedTrack.equals(trackName)) {
									if (mCachedBitmap != null) {
										mAlbum.setImageBitmap(mCachedBitmap);
										mCachedBitmap = null;
									} else {
										new LoadAlbumArtTask().execute(player
												.getArtUrl(), player
												.getArtistName(), player
												.getAlbumName());
									}
								} else {
									new LoadAlbumArtTask().execute(player
											.getArtUrl(), player
											.getArtistName(), player
											.getAlbumName());
								}
							}
						} catch (java.util.concurrent.RejectedExecutionException e) {
							e.printStackTrace();
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						LastFMApplication.getInstance().unbindService(this);
					}

					public void onServiceDisconnected(ComponentName comp) {
					}
				}, Context.BIND_AUTO_CREATE);
	}

	private void queueNextRefresh(long delay) {
		if (!paused) {
			Message msg = mHandler.obtainMessage(REFRESH);
			mHandler.removeMessages(REFRESH);
			mHandler.sendMessageDelayed(msg, delay);
		}
	}

	private long refreshNow() {
		LastFMApplication.getInstance().bindService(
				new Intent(LastFMApplication.getInstance(),
						fm.last.boffin.player.RadioPlayerService.class),
				new ServiceConnection() {
					public void onServiceConnected(ComponentName comp,
							IBinder binder) {
						IRadioPlayer player = IRadioPlayer.Stub
								.asInterface(binder);
						try {
							if(player.getState() == RadioPlayerService.STATE_PAUSED) {
								mStopButton.setImageResource(R.drawable.play);
							} else {
								mStopButton.setImageResource(R.drawable.pause);
							}
							mDuration = player.getDuration();
							long pos = player.getPosition();
							if ((pos >= 0) && (mDuration > 0)
									&& (pos <= mDuration)) {
								mCurrentTime.setText(makeTimeString(
										Player.this, pos / 1000));
								mTotalTime.setText(makeTimeString(Player.this,
										mDuration / 1000));
								mProgress
										.setProgress((int) (1000 * pos / mDuration));
								mProgress.setSecondaryProgress(player.getBufferPercent() * 10);
								if (mTuningDialog != null) {
									mTuningDialog.dismiss();
									mTuningDialog = null;
								}
							} else {
								mCurrentTime.setText("--:--");
								mTotalTime.setText("--:--");
								mProgress.setProgress(0);
								mProgress.setSecondaryProgress(player.getBufferPercent() * 10);
								if (player.isPlaying() && mTuningDialog != null) {
									mTuningDialog.dismiss();
									mTuningDialog = null;
								}
							}
							// return the number of milliseconds until the next
							// full second, so
							// the counter can be updated at just the right time
						} catch (RemoteException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						LastFMApplication.getInstance().unbindService(this);
					}

					public void onServiceDisconnected(ComponentName comp) {
					}
				}, Context.BIND_AUTO_CREATE);

		return 500;
	}

	private class RefreshTask extends AsyncTaskEx<Void, Void, Void> {
		@Override
		protected Void doInBackground(Void... params) {
			long next = refreshNow();
			queueNextRefresh(next);
			return null;
		}
		
	}
	
	private final Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			case REFRESH:
				try {
					new RefreshTask().execute((Void)null);
				} catch (RejectedExecutionException e) {
					queueNextRefresh(500);
				}
				break;
			default:
				break;
			}
		}
	};

	/*
	 * Try to use String.format() as little as possible, because it creates a
	 * new Formatter every time you call it, which is very inefficient. Reusing
	 * an existing Formatter more than tripled the speed of makeTimeString().
	 * This Formatter/StringBuilder are also used by makeAlbumSongsLabel()
	 * 
	 * Hi I changed this due to a bug I managed to make at time zero. But
	 * honestly, this kind of optimisation is a bit much. --mxcl
	 */

	public static String makeTimeString(Context context, long secs) {
		return new Formatter().format("%02d:%02d", secs / 60, secs % 60)
				.toString();
	}

	private class LoadAlbumArtTask extends AsyncTaskEx<String, Void, Boolean> {
		String artUrl;

		@Override
		public void onPreExecute() {
			mAlbum.clear();
		}

		@Override
		public Boolean doInBackground(String... params) {
			Album album;
			boolean success = false;

			artUrl = params[0];
			Log.i("LastFm", "Art URL from playlist: " + artUrl);

			try {
				String artistName = params[1];
				String albumName = params[2];
				if (!artistName.equals(RadioPlayerService.UNKNOWN) && albumName != null && albumName.length() > 0) {
					album = mServer.getAlbumInfo(artistName, albumName);
					if (album != null) {
						DisplayMetrics metrics = new DisplayMetrics();
						getWindowManager().getDefaultDisplay().getMetrics(metrics);
						int width = metrics.widthPixels;
						if(metrics.heightPixels < width)
							width = metrics.heightPixels;
						
						Log.i("LastFm", "Current screen width: " + width);
						
						if(width > 320)
							artUrl = album.getURLforImageSize("mega");
						else
							artUrl = album.getURLforImageSize("extralarge");
					}
				}
				success = true;
			} catch (Exception e) {
				e.printStackTrace();
			} catch (WSError e) {
			}
			return success;
		}

		@Override
		public void onPostExecute(Boolean result) {
			if (artUrl != RadioPlayerService.UNKNOWN) {
				mAlbum.fetch(artUrl);
			} else {
				mAlbum.setDefaultImageResource(R.drawable.no_artwork);
			}
		}
	}
}
