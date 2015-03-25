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
package fm.last.android.activity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import fm.last.android.utils.AsyncTaskEx;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;
import android.widget.AdapterView.OnItemClickListener;
import fm.last.android.AndroidLastFmServerFactory;
import fm.last.android.LastFMApplication;
import fm.last.android.LastFm;
import fm.last.android.R;
import fm.last.android.adapter.ListAdapter;
import fm.last.android.adapter.ListEntry;
import fm.last.android.player.RadioPlayerService;
import fm.last.android.utils.ImageCache;
import fm.last.android.widget.ProfileBubble;
import fm.last.android.widget.QuickContactProfileBubble;
import fm.last.api.Album;
import fm.last.api.Artist;
import fm.last.api.ImageUrl;
import fm.last.api.LastFmServer;
import fm.last.api.Session;
import fm.last.api.Tag;
import fm.last.api.Track;
import fm.last.api.User;
import fm.last.api.WSError;

public class Profile_ChartsTab extends ListActivity {
	// Java doesn't let you treat enums as ints easily, so we have to have this
	// mess
	private static final int PROFILE_RECOMMENDED = 0;
	private static final int PROFILE_TOPARTISTS = 1;
	private static final int PROFILE_TOPALBUMS = 2;
	private static final int PROFILE_TOPTRACKS = 3;
	private static final int PROFILE_RECENTLYPLAYED = 4;
	private static final int PROFILE_FRIENDS = 5;
	private static final int PROFILE_TAGS = 6;

	private ListAdapter mProfileAdapter;
	private String mUsername; // store this separate so we have access to it
								// before User obj is retrieved
	LastFmServer mServer = AndroidLastFmServerFactory.getServer();

	ViewFlipper mNestedViewFlipper;
	private Stack<Integer> mViewHistory;

	View previousSelectedView = null;

	// Animations
	Animation mPushRightIn;
	Animation mPushRightOut;
	Animation mPushLeftIn;
	Animation mPushLeftOut;

	ListView[] mProfileLists = new ListView[7];

	private ImageCache mImageCache = null;

	private IntentFilter mIntentFilter;
	
	ProfileBubble mProfileBubble;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.charts);

		mUsername = getIntent().getStringExtra("user");
		
		try {
			mProfileBubble = new QuickContactProfileBubble(this);
		} catch (java.lang.VerifyError e) {
			mProfileBubble = new ProfileBubble(this);
		} catch (Exception e) {
			mProfileBubble = new ProfileBubble(this);
		}
		mProfileBubble.setTag("header");
		mProfileBubble.setClickable(false);
		getListView().addHeaderView(mProfileBubble, null, false);

		new LoadUserTask().execute((Void)null);

		mViewHistory = new Stack<Integer>();
		mNestedViewFlipper = (ViewFlipper) findViewById(R.id.NestedViewFlipper);
		mNestedViewFlipper.setAnimateFirstView(false);
		mNestedViewFlipper.setAnimationCacheEnabled(false);

		getListView().requestFocus();
		
		RebuildChartsMenu();
		
		// TODO should be functions and not member variables, caching is evil
		mProfileLists[PROFILE_RECOMMENDED] = (ListView) findViewById(R.id.recommended_list_view);
		mProfileLists[PROFILE_RECOMMENDED].setOnItemClickListener(mArtistListItemClickListener);
		TextView title = new TextView(this);
		title.setBackgroundDrawable(getResources().getDrawable(R.drawable.tab_bar_rest));
		title.setText(getString(R.string.profile_myrecs));
		title.setPadding(6, 6, 6, 6);
		title.setTextSize(16);
		title.setTextColor(0xFFFFFFFF);
		title.setGravity(Gravity.CENTER);
		title.setTypeface(Typeface.DEFAULT_BOLD);
		title.setShadowLayer(4, 4, 4, 0xFF000000);
		mProfileLists[PROFILE_RECOMMENDED].addHeaderView(title);
		
		
		mProfileLists[PROFILE_TOPARTISTS] = (ListView) findViewById(R.id.topartists_list_view);
		mProfileLists[PROFILE_TOPARTISTS].setOnItemClickListener(mArtistListItemClickListener);
		title = new TextView(this);
		title.setBackgroundDrawable(getResources().getDrawable(R.drawable.tab_bar_rest));
		title.setText(getString(R.string.profile_topartists));
		title.setPadding(6, 6, 6, 6);
		title.setTextSize(16);
		title.setTextColor(0xFFFFFFFF);
		title.setGravity(Gravity.CENTER);
		title.setTypeface(Typeface.DEFAULT_BOLD);
		title.setShadowLayer(4, 4, 4, 0xFF000000);
		mProfileLists[PROFILE_TOPARTISTS].addHeaderView(title);

		mProfileLists[PROFILE_TOPALBUMS] = (ListView) findViewById(R.id.topalbums_list_view);
		mProfileLists[PROFILE_TOPALBUMS].setOnItemClickListener(mAlbumListItemClickListener);
		title = new TextView(this);
		title.setBackgroundDrawable(getResources().getDrawable(R.drawable.tab_bar_rest));
		title.setText(getString(R.string.profile_topalbums));
		title.setPadding(6, 6, 6, 6);
		title.setTextSize(16);
		title.setTextColor(0xFFFFFFFF);
		title.setGravity(Gravity.CENTER);
		title.setTypeface(Typeface.DEFAULT_BOLD);
		title.setShadowLayer(4, 4, 4, 0xFF000000);
		mProfileLists[PROFILE_TOPALBUMS].addHeaderView(title);

		mProfileLists[PROFILE_TOPTRACKS] = (ListView) findViewById(R.id.toptracks_list_view);
		mProfileLists[PROFILE_TOPTRACKS].setOnItemClickListener(mTrackListItemClickListener);
		title = new TextView(this);
		title.setBackgroundDrawable(getResources().getDrawable(R.drawable.tab_bar_rest));
		title.setText(getString(R.string.profile_toptracks));
		title.setPadding(6, 6, 6, 6);
		title.setTextSize(16);
		title.setTextColor(0xFFFFFFFF);
		title.setGravity(Gravity.CENTER);
		title.setTypeface(Typeface.DEFAULT_BOLD);
		title.setShadowLayer(4, 4, 4, 0xFF000000);
		mProfileLists[PROFILE_TOPTRACKS].addHeaderView(title);

		mProfileLists[PROFILE_RECENTLYPLAYED] = (ListView) findViewById(R.id.recenttracks_list_view);
		mProfileLists[PROFILE_RECENTLYPLAYED].setOnItemClickListener(mTrackListItemClickListener);
		title = new TextView(this);
		title.setBackgroundDrawable(getResources().getDrawable(R.drawable.tab_bar_rest));
		title.setText(getString(R.string.profile_recentlyplayed));
		title.setPadding(6, 6, 6, 6);
		title.setTextSize(16);
		title.setTextColor(0xFFFFFFFF);
		title.setGravity(Gravity.CENTER);
		title.setTypeface(Typeface.DEFAULT_BOLD);
		title.setShadowLayer(4, 4, 4, 0xFF000000);
		mProfileLists[PROFILE_RECENTLYPLAYED].addHeaderView(title);

		mProfileLists[PROFILE_FRIENDS] = (ListView) findViewById(R.id.profilefriends_list_view);
		mProfileLists[PROFILE_FRIENDS].setOnItemClickListener(mUserItemClickListener);
		title = new TextView(this);
		title.setBackgroundDrawable(getResources().getDrawable(R.drawable.tab_bar_rest));
		title.setText(getString(R.string.profile_friends));
		title.setPadding(6, 6, 6, 6);
		title.setTextSize(16);
		title.setTextColor(0xFFFFFFFF);
		title.setGravity(Gravity.CENTER);
		title.setTypeface(Typeface.DEFAULT_BOLD);
		title.setShadowLayer(4, 4, 4, 0xFF000000);
		mProfileLists[PROFILE_FRIENDS].addHeaderView(title);

		mProfileLists[PROFILE_TAGS] = (ListView) findViewById(R.id.profiletags_list_view);
		mProfileLists[PROFILE_TAGS].setOnItemClickListener(mTagListItemClickListener);
		title = new TextView(this);
		title.setBackgroundDrawable(getResources().getDrawable(R.drawable.tab_bar_rest));
		title.setText(getString(R.string.profile_tags));
		title.setPadding(6, 6, 6, 6);
		title.setTextSize(16);
		title.setTextColor(0xFFFFFFFF);
		title.setGravity(Gravity.CENTER);
		title.setTypeface(Typeface.DEFAULT_BOLD);
		title.setShadowLayer(4, 4, 4, 0xFF000000);
		mProfileLists[PROFILE_TAGS].addHeaderView(title);

		// Loading animations
		mPushLeftIn = AnimationUtils.loadAnimation(this, R.anim.push_left_in);
		mPushLeftOut = AnimationUtils.loadAnimation(this, R.anim.push_left_out);
		mPushRightIn = AnimationUtils.loadAnimation(this, R.anim.push_right_in);
		mPushRightOut = AnimationUtils.loadAnimation(this, R.anim.push_right_out);

		mIntentFilter = new IntentFilter();
		mIntentFilter.addAction(RadioPlayerService.PLAYBACK_ERROR);
		mIntentFilter.addAction(RadioPlayerService.STATION_CHANGED);
		mIntentFilter.addAction("fm.last.android.ERROR");
	}
	
	private void RebuildChartsMenu() {
		String[] mStrings;
		
		SharedPreferences settings = getSharedPreferences(LastFm.PREFS, 0);

		if(!settings.getBoolean("remove_tags", false)) {
			if(mUsername.equals(LastFMApplication.getInstance().session.getName()))
				mStrings = new String[] { getString(R.string.profile_myrecs), getString(R.string.profile_topartists), getString(R.string.profile_topalbums),
					getString(R.string.profile_toptracks), getString(R.string.profile_recentlyplayed),
					getString(R.string.profile_friends), getString(R.string.profile_tags) }; // this
																							// order
																							// must
																							// match
																							// the
																							// ProfileActions
																							// enum
			else
				mStrings = new String[] { getString(R.string.profile_topartists), getString(R.string.profile_topalbums),
					getString(R.string.profile_toptracks), getString(R.string.profile_recentlyplayed),
					getString(R.string.profile_friends), getString(R.string.profile_tags) }; // this
																							// order
																							// must
																							// match
																							// the
																							// ProfileActions
																							// enum
		} else {
			if(mUsername.equals(LastFMApplication.getInstance().session.getName()))
				mStrings = new String[] { getString(R.string.profile_myrecs), getString(R.string.profile_topartists), getString(R.string.profile_topalbums),
					getString(R.string.profile_toptracks), getString(R.string.profile_recentlyplayed),
					getString(R.string.profile_friends) }; // this
																							// order
																							// must
																							// match
																							// the
																							// ProfileActions
																							// enum
			else
				mStrings = new String[] { getString(R.string.profile_topartists), getString(R.string.profile_topalbums),
					getString(R.string.profile_toptracks), getString(R.string.profile_recentlyplayed),
					getString(R.string.profile_friends) }; // this
																							// order
																							// must
																							// match
																							// the
																							// ProfileActions
																							// enum
		}
		
		mProfileAdapter = new ListAdapter(Profile_ChartsTab.this, mStrings);
		getListView().setAdapter(mProfileAdapter);
	}
	
	private class LoadUserTask extends AsyncTaskEx<Void, Void, Boolean> {
		User mUser = null;
		
		@Override
		public Boolean doInBackground(Void... params) {
			LastFmServer server = AndroidLastFmServerFactory.getServer();
			try {
				mUser = server.getUserInfo(mUsername, null);
			} catch (WSError e) {
				return false;
			} catch (Exception e) {
				return false;
			}
			return true;
		}
		
		@Override
		public void onPostExecute(Boolean result) {
			if(result) {
				mProfileBubble.setUser(mUser);
			}
		}
	}

	@Override
	protected void onSaveInstanceState(Bundle outState) {
		outState.putInt("displayed_view", mNestedViewFlipper.getDisplayedChild());
		outState.putSerializable("view_history", mViewHistory);

		HashMap<Integer, ListAdapter> adapters = new HashMap<Integer, ListAdapter>(mProfileLists.length);
		for (int i = 0; i < mProfileLists.length; i++) {
			ListView lv = mProfileLists[i];
			if (lv.getAdapter() == null)
				continue;
			if (lv.getAdapter().getClass() == ListAdapter.class)
				adapters.put(i, (ListAdapter) lv.getAdapter());
		}

		outState.putSerializable("adapters", adapters);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void onRestoreInstanceState(Bundle state) {
		mNestedViewFlipper.setDisplayedChild(state.getInt("displayed_view"));

		if (state.containsKey("view_history"))
			try {
				Object viewHistory = state.getSerializable("view_history");
				if (viewHistory instanceof Stack)
					mViewHistory = (Stack<Integer>) viewHistory;
				else {
					// For some reason when the process gets killed and then
					// resumed,
					// the serializable becomes an ArrayList
					for (Integer i : (ArrayList<Integer>) state.getSerializable("view_history"))
						mViewHistory.push(i);
				}
			} catch (ClassCastException e) {

			}

		// Restore the adapters and disable the spinner for all the profile
		// lists
		HashMap<Integer, ListAdapter> adapters = (HashMap<Integer, ListAdapter>) state.getSerializable("adapters");

		for (int key : adapters.keySet()) {
			ListAdapter adapter = adapters.get(key);
			if (adapter != null) {
				adapter.setContext(this);
				adapter.setImageCache(getImageCache());
				adapter.disableLoadBar();
				adapter.refreshList();
				mProfileLists[key].setAdapter(adapter);
			}
		}
	}

	@Override
	public void onResume() {
		registerReceiver(mStatusListener, mIntentFilter);

		getListView().setEnabled(true);

		for (ListView list : mProfileLists) {
                        if (list.getAdapter() != null && list.getAdapter().getClass().equals(ListAdapter.class))
                                ((ListAdapter) list.getAdapter()).disableLoadBar();
		}

		super.onResume();
	}

	@Override
	protected void onPause() {
		unregisterReceiver(mStatusListener);
		super.onPause();
	}

	private BroadcastReceiver mStatusListener = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {

			String action = intent.getAction();
			if (action.equals(RadioPlayerService.PLAYBACK_ERROR) || action.equals("fm.last.android.ERROR")) {
				for (ListView list : mProfileLists) {
					if (list.getAdapter() != null && list.getAdapter().getClass().equals(ListAdapter.class))
						((ListAdapter) list.getAdapter()).disableLoadBar();
				}
			} else if(action.equals(RadioPlayerService.STATION_CHANGED)) {
				RebuildChartsMenu();
				
				if (!mViewHistory.isEmpty()) {
					setPreviousAnimation();
					mProfileAdapter.disableLoadBar();
					mNestedViewFlipper.setDisplayedChild(mViewHistory.pop());
				}
			}
		}
	};


	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (keyCode == KeyEvent.KEYCODE_BACK) {
			if (!mViewHistory.isEmpty()) {
				setPreviousAnimation();
				mProfileAdapter.disableLoadBar();
				mNestedViewFlipper.setDisplayedChild(mViewHistory.pop());
				return true;
			}
			if (event.getRepeatCount() == 0) {
				finish();
				return true;
			}
		}
		return false;
	}

	private void setNextAnimation() {
		mNestedViewFlipper.setInAnimation(mPushLeftIn);
		mNestedViewFlipper.setOutAnimation(mPushLeftOut);
	}

	private void setPreviousAnimation() {
		mNestedViewFlipper.setInAnimation(mPushRightIn);
		mNestedViewFlipper.setOutAnimation(mPushRightOut);
	}

	public void onListItemClick(ListView l, View v, int position, long id) {
		setNextAnimation();
		mProfileAdapter.enableLoadBar(position-1);
		if(!mUsername.equals(LastFMApplication.getInstance().session.getName()))
			position++;
		switch (position-1) {
		case PROFILE_RECOMMENDED: // "Top Artists"
			try {
				LastFMApplication.getInstance().tracker.trackPageView("/Profile/Charts/RecommendedArtists");
			} catch (Exception e) {
				//Google Analytics doesn't appear to be thread safe
			}
			new LoadRecommendedArtistsTask().execute((Void) null);
			break;
		case PROFILE_TOPARTISTS: // "Top Artists"
			try {
				LastFMApplication.getInstance().tracker.trackPageView("/Profile/Charts/TopArtists");
			} catch (Exception e) {
				//Google Analytics doesn't appear to be thread safe
			}
			new LoadTopArtistsTask().execute((Void) null);
			break;
		case PROFILE_TOPALBUMS: // "Top Albums"
			try {
				LastFMApplication.getInstance().tracker.trackPageView("/Profile/Charts/TopAlbums");
			} catch (Exception e) {
				//Google Analytics doesn't appear to be thread safe
			}
			new LoadTopAlbumsTask().execute((Void) null);
			break;
		case PROFILE_TOPTRACKS: // "Top Tracks"
			try {
				LastFMApplication.getInstance().tracker.trackPageView("/Profile/Charts/TopTracks");
			} catch (Exception e) {
				//Google Analytics doesn't appear to be thread safe
			}
			new LoadTopTracksTask().execute((Void) null);
			break;
		case PROFILE_RECENTLYPLAYED: // "Recently Played"
			try {
				LastFMApplication.getInstance().tracker.trackPageView("/Profile/Charts/Recent");
			} catch (Exception e) {
				//Google Analytics doesn't appear to be thread safe
			}
			new LoadRecentTracksTask().execute((Void) null);
			break;
		case PROFILE_FRIENDS: // "Friends"
			try {
				LastFMApplication.getInstance().tracker.trackPageView("/Profile/Friends");
			} catch (Exception e) {
				//Google Analytics doesn't appear to be thread safe
			}
			new LoadFriendsTask().execute((Void) null);
			break;
		case PROFILE_TAGS: // "Tags"
			try {
				LastFMApplication.getInstance().tracker.trackPageView("/Profile/Tags");
			} catch (Exception e) {
				//Google Analytics doesn't appear to be thread safe
			}
			new LoadTagsTask().execute((Void) null);
			break;
		default:
			break;

		}
	}

	private OnItemClickListener mArtistListItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> l, View v, int position, long id) {
			try {
				Artist artist = (Artist) l.getAdapter().getItem(position);
				if(artist != null) {
					Intent i = new Intent(Profile_ChartsTab.this, Metadata.class);
					i.putExtra("artist", artist.getName());
					startActivity(i);
				}
			} catch (ClassCastException e) {
				// fine.
			}
		}

	};

	private OnItemClickListener mAlbumListItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> l, View v, int position, long id) {
			try {
				Album album = (Album) l.getAdapter().getItem(position);
				if(album != null) {
					Intent i = new Intent(Profile_ChartsTab.this, PopupActionActivity.class);
					i.putExtra("lastfm.artist", album.getArtist());
					i.putExtra("lastfm.album", album.getTitle());
					startActivity(i);
				}
			} catch (ClassCastException e) {
				// (Album) cast can fail, like when the list contains a string
				// saying: "no items"
			}
		}

	};

	private OnItemClickListener mTrackListItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> l, View v, int position, long id) {
			try {
				Track track = (Track) l.getAdapter().getItem(position);
				if(track != null) {
					Intent i = new Intent(Profile_ChartsTab.this, PopupActionActivity.class);
					i.putExtra("lastfm.artist", track.getArtist().getName());
					i.putExtra("lastfm.track", track.getName());
					startActivity(i);
				}
			} catch (ClassCastException e) {
				// (Track) cast can fail, like when the list contains a string
				// saying: "no items"
			}
		}

	};

	private OnItemClickListener mTagListItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> l, View v, int position, long id) {
			try {
				Session session = LastFMApplication.getInstance().session;
				Tag tag = (Tag) l.getAdapter().getItem(position);
				if(tag != null) {
					if (session.getSubscriber().equals("1"))
						LastFMApplication.getInstance().playRadioStation(Profile_ChartsTab.this, "lastfm://usertags/" + mUsername + "/" + Uri.encode(tag.getName()), true);
					else
						LastFMApplication.getInstance().playRadioStation(Profile_ChartsTab.this, "lastfm://globaltags/" + Uri.encode(tag.getName()), true);
				}
			} catch (ClassCastException e) {
				// when the list item is not a tag
			}
		}

	};

	private OnItemClickListener mUserItemClickListener = new OnItemClickListener() {
		public void onItemClick(AdapterView<?> l, View v, int position, long id) {
			try {
				User user = (User) l.getAdapter().getItem(position);
				if(user != null) {
					Intent profileIntent = new Intent(Profile_ChartsTab.this, fm.last.android.activity.Profile.class);
					profileIntent.putExtra("lastfm.profile.username", user.getName());
					startActivity(profileIntent);
				}
			} catch (ClassCastException e) {
				// when the list item is not a User
			}
		}
	};

	private class LoadRecommendedArtistsTask extends AsyncTaskEx<Void, Void, ArrayList<ListEntry>> {

		@Override
		public ArrayList<ListEntry> doInBackground(Void... params) {

			try {
				Artist[] recartists = mServer.getUserRecommendedArtists(mUsername, LastFMApplication.getInstance().session.getKey());
				if (recartists.length == 0)
					return null;
				ArrayList<ListEntry> iconifiedEntries = new ArrayList<ListEntry>();
				for (int i = 0; i < ((recartists.length < 10) ? recartists.length : 10); i++) {
					String url = null;
					try {
						ImageUrl[] urls = recartists[i].getImages();
						url = urls[0].getUrl();
					} catch (ArrayIndexOutOfBoundsException e) {
					}

					ListEntry entry = new ListEntry(recartists[i], R.drawable.artist_icon, recartists[i].getName(), url);
					iconifiedEntries.add(entry);
				}
				return iconifiedEntries;
			} catch (Exception e) {
				e.printStackTrace();
			} catch (WSError e) {
				LastFMApplication.getInstance().presentError(Profile_ChartsTab.this, e);
			}
			return null;
		}

		@Override
		public void onPostExecute(ArrayList<ListEntry> iconifiedEntries) {
			if (iconifiedEntries != null) {
				ListAdapter adapter = new ListAdapter(Profile_ChartsTab.this, getImageCache());
				adapter.setSourceIconified(iconifiedEntries);
				mProfileLists[PROFILE_RECOMMENDED].setAdapter(adapter);
			} else {
				String[] strings = new String[] { getString(R.string.profile_notopartists) };
				ListAdapter adapter = new ListAdapter(Profile_ChartsTab.this, strings);
				adapter.disableDisclosureIcons();
				adapter.setDisabled();
				mProfileLists[PROFILE_RECOMMENDED].setAdapter(adapter);
			}
			mViewHistory.push(mNestedViewFlipper.getDisplayedChild()); // Save
																		// the
																		// current
																		// view
			mNestedViewFlipper.setDisplayedChild(PROFILE_RECOMMENDED + 1);
		}
	}

	private class LoadTopArtistsTask extends AsyncTaskEx<Void, Void, ArrayList<ListEntry>> {

		@Override
		public ArrayList<ListEntry> doInBackground(Void... params) {

			try {
				Artist[] topartists = mServer.getUserTopArtists(mUsername, "overall");
				if (topartists.length == 0)
					return null;
				ArrayList<ListEntry> iconifiedEntries = new ArrayList<ListEntry>();
				for (int i = 0; i < ((topartists.length < 10) ? topartists.length : 10); i++) {
					String url = null;
					try {
						ImageUrl[] urls = topartists[i].getImages();
						url = urls[0].getUrl();
					} catch (ArrayIndexOutOfBoundsException e) {
					}

					ListEntry entry = new ListEntry(topartists[i], R.drawable.artist_icon, topartists[i].getName(), url);
					iconifiedEntries.add(entry);
				}
				return iconifiedEntries;
			} catch (Exception e) {
				e.printStackTrace();
			} catch (WSError e) {
				LastFMApplication.getInstance().presentError(Profile_ChartsTab.this, e);
			}
			return null;
		}

		@Override
		public void onPostExecute(ArrayList<ListEntry> iconifiedEntries) {
			if (iconifiedEntries != null) {
				ListAdapter adapter = new ListAdapter(Profile_ChartsTab.this, getImageCache());
				adapter.setSourceIconified(iconifiedEntries);
				mProfileLists[PROFILE_TOPARTISTS].setAdapter(adapter);
			} else {
				String[] strings = new String[] { getString(R.string.profile_notopartists) };
				ListAdapter adapter = new ListAdapter(Profile_ChartsTab.this, strings);
				adapter.disableDisclosureIcons();
				adapter.setDisabled();
				mProfileLists[PROFILE_TOPARTISTS].setAdapter(adapter);
			}
			mViewHistory.push(mNestedViewFlipper.getDisplayedChild()); // Save
																		// the
																		// current
																		// view
			mNestedViewFlipper.setDisplayedChild(PROFILE_TOPARTISTS + 1);
		}
	}

	private class LoadTopAlbumsTask extends AsyncTaskEx<Void, Void, ArrayList<ListEntry>> {

		@Override
		public ArrayList<ListEntry> doInBackground(Void... params) {

			try {
				Album[] topalbums = mServer.getUserTopAlbums(mUsername, "overall");
				if (topalbums.length == 0)
					return null;
				ArrayList<ListEntry> iconifiedEntries = new ArrayList<ListEntry>();
				for (int i = 0; i < ((topalbums.length < 10) ? topalbums.length : 10); i++) {
					String url = null;
					try {
						ImageUrl[] urls = topalbums[i].getImages();
						url = urls[urls.length > 1 ? 1 : 0].getUrl();
					} catch (ArrayIndexOutOfBoundsException e) {
					}

					ListEntry entry = new ListEntry(topalbums[i], R.drawable.no_artwork, topalbums[i].getTitle(), url, topalbums[i].getArtist());
					iconifiedEntries.add(entry);
				}
				return iconifiedEntries;
			} catch (Exception e) {
				e.printStackTrace();
			} catch (WSError e) {
				LastFMApplication.getInstance().presentError(Profile_ChartsTab.this, e);
			}
			return null;
		}

		@Override
		public void onPostExecute(ArrayList<ListEntry> iconifiedEntries) {
			if (iconifiedEntries != null) {
				ListAdapter adapter = new ListAdapter(Profile_ChartsTab.this, getImageCache());
				adapter.setSourceIconified(iconifiedEntries);
				mProfileLists[PROFILE_TOPALBUMS].setAdapter(adapter);
			} else {
				String[] strings = new String[] { getString(R.string.profile_notopalbums) };
				ListAdapter adapter = new ListAdapter(Profile_ChartsTab.this, strings);
				adapter.disableDisclosureIcons();
				adapter.setDisabled();
				mProfileLists[PROFILE_TOPALBUMS].setAdapter(adapter);
			}
			mViewHistory.push(mNestedViewFlipper.getDisplayedChild()); // Save
																		// the
																		// current
																		// view
			mNestedViewFlipper.setDisplayedChild(PROFILE_TOPALBUMS + 1);
		}
	}

	private class LoadTopTracksTask extends AsyncTaskEx<Void, Void, ArrayList<ListEntry>> {

		@Override
		public ArrayList<ListEntry> doInBackground(Void... params) {
			try {
				Track[] toptracks = mServer.getUserTopTracks(mUsername, "overall");
				if (toptracks.length == 0)
					return null;
				ArrayList<ListEntry> iconifiedEntries = new ArrayList<ListEntry>();
				for (int i = 0; i < ((toptracks.length < 10) ? toptracks.length : 10); i++) {
					ListEntry entry = new ListEntry(toptracks[i], R.drawable.song_icon, toptracks[i].getName(), toptracks[i].getImages().length == 0 ? ""
							: toptracks[i].getURLforImageSize("extralarge"), // some
																	// tracks
																	// don't
																	// have
																	// images
							toptracks[i].getArtist().getName());
					iconifiedEntries.add(entry);
				}
				return iconifiedEntries;
			} catch (Exception e) {
				e.printStackTrace();
			} catch (WSError e) {
				LastFMApplication.getInstance().presentError(Profile_ChartsTab.this, e);
			}
			return null;
		}

		@Override
		public void onPostExecute(ArrayList<ListEntry> iconifiedEntries) {
			if (iconifiedEntries != null) {
				ListAdapter adapter = new ListAdapter(Profile_ChartsTab.this, getImageCache());
				adapter.setSourceIconified(iconifiedEntries);
				mProfileLists[PROFILE_TOPTRACKS].setAdapter(adapter);
			} else {
				String[] strings = new String[] { getString(R.string.profile_notoptracks) };
				ListAdapter adapter = new ListAdapter(Profile_ChartsTab.this, strings);
				adapter.disableDisclosureIcons();
				adapter.setDisabled();
				mProfileLists[PROFILE_TOPTRACKS].setAdapter(adapter);
			}
			mViewHistory.push(mNestedViewFlipper.getDisplayedChild()); // Save
																		// the
																		// current
																		// view
			mNestedViewFlipper.setDisplayedChild(PROFILE_TOPTRACKS + 1);
		}
	}

	private class LoadRecentTracksTask extends AsyncTaskEx<Void, Void, ArrayList<ListEntry>> {

		@Override
		public ArrayList<ListEntry> doInBackground(Void... params) {
			try {
				Track[] recenttracks = mServer.getUserRecentTracks(mUsername, "true", 10);
				if (recenttracks.length == 0)
					return null;

				ArrayList<ListEntry> iconifiedEntries = new ArrayList<ListEntry>();
				for (Track track : recenttracks) {
					ListEntry entry = new ListEntry(track, R.drawable.song_icon, track.getName(), track.getImages().length == 0 ? "" : track.getImages()[0]
							.getUrl(), // some tracks don't have images
							track.getArtist().getName());
					iconifiedEntries.add(entry);
				}
				return iconifiedEntries;
			} catch (Exception e) {
				e.printStackTrace();
			} catch (WSError e) {
			}
			return null;
		}

		@Override
		public void onPostExecute(ArrayList<ListEntry> iconifiedEntries) {
			if (iconifiedEntries != null) {
				ListAdapter adapter = new ListAdapter(Profile_ChartsTab.this, getImageCache());
				adapter.setSourceIconified(iconifiedEntries);
				mProfileLists[PROFILE_RECENTLYPLAYED].setAdapter(adapter);
			} else {
				String[] strings = new String[] { getString(R.string.profile_norecenttracks) };
				ListAdapter adapter = new ListAdapter(Profile_ChartsTab.this, strings);
				adapter.disableDisclosureIcons();
				adapter.setDisabled();
				mProfileLists[PROFILE_RECENTLYPLAYED].setAdapter(adapter);
			}
			mViewHistory.push(mNestedViewFlipper.getDisplayedChild()); // Save
																		// the
																		// current
																		// view
			mNestedViewFlipper.setDisplayedChild(PROFILE_RECENTLYPLAYED + 1);
		}
	}

	private class LoadTagsTask extends AsyncTaskEx<Void, Void, ArrayList<ListEntry>> {

		@Override
		public ArrayList<ListEntry> doInBackground(Void... params) {
			try {
				Tag[] tags = mServer.getUserTopTags(mUsername, 10);
				if (tags.length == 0)
					return null;

				ArrayList<ListEntry> iconifiedEntries = new ArrayList<ListEntry>();
				for (int i = 0; i < ((tags.length < 10) ? tags.length : 10); i++) {
					ListEntry entry = new ListEntry(tags[i], -1, tags[i].getName(), R.drawable.list_icon_station);
					iconifiedEntries.add(entry);
				}
				return iconifiedEntries;
			} catch (Exception e) {
				e.printStackTrace();
			} catch (WSError e) {
				LastFMApplication.getInstance().presentError(Profile_ChartsTab.this, e);
			}
			return null;
		}

		@Override
		public void onPostExecute(ArrayList<ListEntry> iconifiedEntries) {
			if (iconifiedEntries != null) {
				ListAdapter adapter = new ListAdapter(Profile_ChartsTab.this, getImageCache());
				adapter.setSourceIconified(iconifiedEntries);
				mProfileLists[PROFILE_TAGS].setAdapter(adapter);
			} else {
				String[] strings = new String[] { getString(R.string.profile_notags) };
				ListAdapter adapter = new ListAdapter(Profile_ChartsTab.this, strings);
				adapter.disableDisclosureIcons();
				adapter.setDisabled();
				mProfileLists[PROFILE_TAGS].setAdapter(adapter);
			}
			mViewHistory.push(mNestedViewFlipper.getDisplayedChild()); // Save
																		// the
																		// current
																		// view
			mNestedViewFlipper.setDisplayedChild(PROFILE_TAGS + 1);
		}
	}

	private class LoadFriendsTask extends AsyncTaskEx<Void, Void, ArrayList<ListEntry>> {

		@Override
		public ArrayList<ListEntry> doInBackground(Void... params) {
			try {
				User[] friends = mServer.getFriends(mUsername, null, "1024").getFriends();
				if (friends.length == 0)
					return null;
				ArrayList<ListEntry> iconifiedEntries = new ArrayList<ListEntry>();
				for (int i = 0; i < friends.length; i++) {
					ListEntry entry = new ListEntry(friends[i], R.drawable.profile_unknown, friends[i].getName(), friends[i].getImages().length == 0 ? ""
							: friends[i].getURLforImageSize("extralarge")); // some
																	// tracks
																	// don't
																	// have
																	// images
					iconifiedEntries.add(entry);
				}
				return iconifiedEntries;
			} catch (Exception e) {
				e.printStackTrace();
			} catch (WSError e) {
				LastFMApplication.getInstance().presentError(Profile_ChartsTab.this, e);
			}
			return null;
		}

		@Override
		public void onPostExecute(ArrayList<ListEntry> iconifiedEntries) {
			if (iconifiedEntries != null) {
				ListAdapter adapter = new ListAdapter(Profile_ChartsTab.this, getImageCache());
				adapter.setSourceIconified(iconifiedEntries);
				mProfileLists[PROFILE_FRIENDS].setAdapter(adapter);
			} else {
				String[] strings = new String[] { getString(R.string.profile_nofriends) };
				ListAdapter adapter = new ListAdapter(Profile_ChartsTab.this, strings);
				adapter.disableDisclosureIcons();
				adapter.setDisabled();
				mProfileLists[PROFILE_FRIENDS].setAdapter(adapter);
			}
			mViewHistory.push(mNestedViewFlipper.getDisplayedChild()); // Save
																		// the
																		// current
																		// view
			mNestedViewFlipper.setDisplayedChild(PROFILE_FRIENDS + 1);
		}
	}

	private ImageCache getImageCache() {
		if (mImageCache == null) {
			mImageCache = new ImageCache();
		}
		return mImageCache;
	}

}
