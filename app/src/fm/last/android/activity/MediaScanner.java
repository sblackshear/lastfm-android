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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Window;
import android.widget.ProgressBar;
import android.widget.TextView;
import fm.last.android.AndroidLastFmServerFactory;
import fm.last.android.R;
import fm.last.android.db.LocalCollection;
import fm.last.android.db.LocalCollection.FilesToTagResult;
import fm.last.android.db.LocalCollection.LocalCollectionProgressCallback;
import fm.last.android.utils.AsyncTaskEx;
import fm.last.android.widget.AlbumArt;
import fm.last.api.Artist;
import fm.last.util.UrlUtil;

public class MediaScanner extends Activity implements LocalCollectionProgressCallback {

	private TextView mActivityName;
	private ProgressBar mProgress;
	LocalCollection collection;
	private AlbumArt[][] mArtistImages = new AlbumArt[5][4];
	private TextView mTagLabel;
	private long lastArtistFetch = 0;
	private long lastTagFetch = 0;
	int artist_x = -1; //omghax
	int artist_y = 0;
	int artistCount = 0;

	@Override
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.media_scanner);

		mActivityName = (TextView) findViewById(R.id.activityName);
		mProgress = (ProgressBar) findViewById(R.id.progress);
		mTagLabel = (TextView) findViewById(R.id.tagLabel);
		
		mArtistImages[0][0] = (AlbumArt) findViewById(R.id.artistImage1_1);
		mArtistImages[0][1] = (AlbumArt) findViewById(R.id.artistImage1_2);
		mArtistImages[0][2] = (AlbumArt) findViewById(R.id.artistImage1_3);
		mArtistImages[0][3] = (AlbumArt) findViewById(R.id.artistImage1_4);
		
		mArtistImages[1][0] = (AlbumArt) findViewById(R.id.artistImage2_1);
		mArtistImages[1][1] = (AlbumArt) findViewById(R.id.artistImage2_2);
		mArtistImages[1][2] = (AlbumArt) findViewById(R.id.artistImage2_3);
		mArtistImages[1][3] = (AlbumArt) findViewById(R.id.artistImage2_4);
		
		mArtistImages[2][0] = (AlbumArt) findViewById(R.id.artistImage3_1);
		mArtistImages[2][1] = (AlbumArt) findViewById(R.id.artistImage3_2);
		mArtistImages[2][2] = (AlbumArt) findViewById(R.id.artistImage3_3);
		mArtistImages[2][3] = (AlbumArt) findViewById(R.id.artistImage3_4);
		
		mArtistImages[3][0] = (AlbumArt) findViewById(R.id.artistImage4_1);
		mArtistImages[3][1] = (AlbumArt) findViewById(R.id.artistImage4_2);
		mArtistImages[3][2] = (AlbumArt) findViewById(R.id.artistImage4_3);
		mArtistImages[3][3] = (AlbumArt) findViewById(R.id.artistImage4_4);
		
		mArtistImages[4][0] = (AlbumArt) findViewById(R.id.artistImage5_1);
		mArtistImages[4][1] = (AlbumArt) findViewById(R.id.artistImage5_2);
		mArtistImages[4][2] = (AlbumArt) findViewById(R.id.artistImage5_3);
		mArtistImages[4][3] = (AlbumArt) findViewById(R.id.artistImage5_4);
		
		collection = LocalCollection.getInstance();
		collection.callback = this;
		//collection.clearDatabase();
	}

	@Override
	public void onResume() {
		super.onResume();
		/*try {
			LastFMApplication.getInstance().tracker.trackPageView("/Boffin/MediaScanner");
		} catch (Exception e) {
			//Google Analytics doesn't appear to be thread safe
		}*/
		new MediaScannerTask().execute((Void)null);
	}

	@Override
	protected void onStop() {
		super.onStop();
	}
	
	class ProgressRunnable implements Runnable {
		int progress = 0;
		boolean indeterminate = false;
		
		public ProgressRunnable(int p, boolean i) {
			progress = p;
			indeterminate = i;
		}
		
	    public void run() {
    		mProgress.setProgress(progress);
	    	mProgress.setIndeterminate(indeterminate);
	    }
	}
	
	class TagRunnable implements Runnable {
		String name;
		
		public TagRunnable(String n) {
			name = n;
		}
		
	    public void run() {
	    	mTagLabel.setText(name);
	    }
	}

	public void localCollectionProgress(int current, int total) {
		int progress = (int)((float)current * (100.0f / (float)total));
		runOnUiThread(new ProgressRunnable(progress, false));
	}

	public void artistAdded(String name) {
		runOnUiThread(new TagRunnable(name));
		if(System.currentTimeMillis() > (lastArtistFetch + 500) || (++artistCount < 20)) {
			new ArtistFetchTask(name).execute((Void)null);
			lastArtistFetch = System.currentTimeMillis();
		}
	}
	
	public void tagAdded(String name) {
		if(System.currentTimeMillis() > (lastTagFetch + 500)) {
			runOnUiThread(new TagRunnable(name));
			lastTagFetch = System.currentTimeMillis();
		}
	}
	
	private class MediaObject {
		public String title;
		public String artist;
		public String album;
		public long duration;
		public long modified;
	}
	
	private class ArtistFetchTask extends AsyncTaskEx<Void, Void, String> {
		private String artist = "";
		
		public ArtistFetchTask(String a) {
			artist = a;
		}
		
		@Override
		public String doInBackground(Void... params) {
			try {
				Artist a = AndroidLastFmServerFactory.getServer().getArtistInfo(artist, "", "", "");
				return a.getURLforImageSize("medium");
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		@Override
		public void onPostExecute(String result) {
			if(result != null) {
				int x = (int)(Math.random() * 4);
				int y = (int)(Math.random() * 5);
				if(artist_y * 4 + artist_x < 20) {
					if(++artist_x > 3) {
						artist_y++;
						artist_x = 0;
					}
					x = artist_x;
					if(artist_y < 5)
						y = artist_y;
				}
				if(!mArtistImages[y][x].isRunning())
					mArtistImages[y][x].fetch(result, false);
			}
		}
	}
	
	private class MediaScannerTask extends AsyncTaskEx<Void, String, Void> {
		HashMap<String, MediaObject> map;
		
		private void populate_map(Uri URI) {
			String [] cols={MediaStore.Audio.Media.TITLE,MediaStore.Audio.Media.ARTIST,MediaStore.Audio.Media.ALBUM,MediaStore.Audio.Media.DURATION,MediaStore.Audio.Media.DATA,MediaStore.Audio.Media.DATE_MODIFIED};
			Cursor cursor = getContentResolver().query( URI, cols, "is_music = 1",null,null);
			
			while(cursor.moveToNext()) {
				MediaObject m = new MediaObject();
				m.title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
				m.artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));
				m.album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
				m.duration = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION));
				m.modified = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED));
				
				map.put(cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)), m);
			}
		}
		
		@Override
		public void onPreExecute() {
			runOnUiThread(new ProgressRunnable(0, false));
			mActivityName.setText("Scanning Your Music");
		}
		
		@Override
		public Void doInBackground(Void... params) {
			try {
				//Fetch the list of files in our db
				List<LocalCollection.File> files = collection.getFiles();
				
				//Build a map of the Android media files
				map = new HashMap<String, MediaObject>();

				//Scan both internal and external storage
				populate_map(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI);
				populate_map(MediaStore.Audio.Media.INTERNAL_CONTENT_URI);
				
				//Iterate over the files in the DB
				Iterator<LocalCollection.File> i = files.iterator();
				while(i.hasNext()) {
					LocalCollection.File f = i.next();

					if(map.containsKey(f.name())) {
						if(f.lastModified() < map.get(f.name()).modified) {
							//Old File - Rescan
						}
						map.remove(f.name());
					} else {
						//Deleted file - remove
						collection.removeFile(f.id());
					}
				}
				
				//Files remaining in the map are new
				Iterator<String> keys = map.keySet().iterator();
				int count = 0;
				while(keys.hasNext()) {
					String key = keys.next();
					MediaObject m = map.get(key);
					
					collection.addFile(key, m.modified, collection.new FileMeta(m.artist, m.album, m.title, m.duration));
					localCollectionProgress(count++, map.size());
				}
				
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		protected void onProgressUpdate(String... progress) {
			mActivityName.setText(progress[0]);
		}
		
		@Override
		public void onPostExecute(Void result) {
			new TagResolverTask().execute((Void)null);
		}
	}

	private class TagResolverTask extends AsyncTaskEx<Void, String, Void> {
		HashMap<String, Long> tagmap = new HashMap<String, Long>();
		
		@Override
		public void onPreExecute() {
			mActivityName.setText("Analyzing Your Music");
			runOnUiThread(new TagRunnable(""));
		}
		
		private void doResolveQuery(String postData) throws MalformedURLException, IOException {
			List<Long> fileIds = new ArrayList<Long>();
			List<String> tags = new ArrayList<String>();
			List<Float> weights = new ArrayList<Float>();
			publishProgress("Waiting for Last.fm");
			runOnUiThread(new ProgressRunnable(0, true));
			String response = UrlUtil.doPost(new URL("http://musiclookup.last.fm/trackresolve"), postData);
			publishProgress("Processing Results");
			String[] items = response.split("\n");
			for(int x = 0; x < items.length; x++) {
				String[] fields = items[x].split("\t");
				for(int y = 1; y < fields.length; y += 2) {
					fileIds.add(Long.parseLong(fields[0]));
					tags.add(fields[y]);
					weights.add(Float.parseFloat(fields[y+1]));
				}
			}
			publishProgress("Tagging Your Music");
			runOnUiThread(new ProgressRunnable(0, false));
			List<Long>tagIds = collection.resolveTags(tags, tagmap);
			publishProgress("Saving Tags");
			collection.updateTrackTags(fileIds, tagIds, weights);
		}
		
		@Override
		public Void doInBackground(Void... params) {
			int count = 0;
			
			try {
				List<FilesToTagResult> filesToTag = collection.getFilesToTag(100);
				if(filesToTag.size() > 0) {
					Iterator<FilesToTagResult> i = filesToTag.iterator();
					List<Long> fileIds = new ArrayList<Long>();
					String postData = "";
					
					while(i.hasNext()) {
						FilesToTagResult file = i.next();
						postData += file.fileId + "\t" + file.artist + "\t" + file.album + "\t" + file.title + "\n";
						fileIds.add(file.fileId);
						localCollectionProgress(count, (filesToTag.size() > 5000) ? 5000 : filesToTag.size());
						if(count++ > 5000 || !i.hasNext()) {
							doResolveQuery(postData);
							postData = "";
							count = 0;
						}
					}
					runOnUiThread(new ProgressRunnable(0, true));
					collection.updateFileTagTime(fileIds);
				}				
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}

		protected void onProgressUpdate(String... progress) {
			mActivityName.setText(progress[0]);
		}
		
		@Override
		public void onPostExecute(Void result) {
			publishProgress("Database update complete");
			Intent intent = new Intent(MediaScanner.this, TopLocalTags.class);
			startActivity(intent);
			finish();
		}
	}
}
