package fm.last.boffin.activity;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fm.last.boffin.LastFMApplication;
import fm.last.boffin.R;
import fm.last.boffin.db.LocalCollection;
import fm.last.boffin.db.LocalCollection.TopTagsResult;
import fm.last.boffin.player.RadioPlayerService;
import fm.last.boffin.utils.AsyncTaskEx;
import fm.last.boffin.widget.TagCloud;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;

public class LocalTagCloud extends Activity implements OnClickListener {
	String[] tags;
	private TagCloud mTagCloud;
	private Button mPlayButton;
	private Button mExportButton;
	private ProgressBar mProgress;
	
	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
		mTagCloud = new TagCloud(this);
		setTitle("Boffin: Top Tags");
		setContentView(R.layout.local_tag_cloud);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); 
		LinearLayout layout = (LinearLayout)findViewById(R.id.layout);
		layout.addView(mTagCloud, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		
        List<TopTagsResult> topTags = LocalCollection.getInstance().getTopTags(80);
        List<String> tagsList = new ArrayList<String>(topTags.size());
        mTagCloud.clear();
        Iterator<TopTagsResult> i = topTags.iterator();
        while(i.hasNext()) {
        	TopTagsResult r = i.next();
        	//tagsList.add(r.tag);
        	mTagCloud.addTag(r);
        }
        mTagCloud.normalizeSizes();
		
		mPlayButton = (Button)findViewById(R.id.play);
		mPlayButton.setOnClickListener(this);
		mExportButton = (Button)findViewById(R.id.export);
		mExportButton.setOnClickListener(this);
		mProgress = (ProgressBar)findViewById(R.id.progress);

        tags = tagsList.toArray(new String[tagsList.size()]);
        //setTags(tags);
    }
	
	@Override
	public void onResume() {
		super.onResume();
		mProgress.setVisibility(View.GONE);
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(RadioPlayerService.STATION_CHANGED);
		intentFilter.addAction("fm.last.boffin.ERROR");
		registerReceiver(statusListener, intentFilter);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		unregisterReceiver(statusListener);
	}
	
	private boolean createPlaylistForTags(String[] tags) {
		List<LocalCollection.FilesWithTagResult> files = LocalCollection.getInstance().getFilesWithTags(tags, 100);
		Log.i("Last.fm", "Got " + files.size() + " tracks");
		if(files.size() > 0) {
			File sdCard = Environment.getExternalStorageDirectory();
			File dir = new File (sdCard.getAbsolutePath() + "/Music");
			dir.mkdirs();
			String filename = "";
			for(int x = 0; x < tags.length; x++) {
				if(x >= 1 && !(x == 1 && tags.length == 2))
					filename += ", ";
				if(x > 0 && x == tags.length - 1) {
					if(x == 1)
						filename += " ";
					filename += "and ";
				}
				filename += tags[x];
			}
			filename += ".m3u";
			File file = new File(dir, filename);
			try {
				FileOutputStream f = new FileOutputStream(file);
				Iterator<LocalCollection.FilesWithTagResult> i = files.iterator();
				while(i.hasNext()) {
					LocalCollection.FilesWithTagResult r = i.next();
					f.write((r.file.name() + "\n").getBytes());
					Log.i("Last.fm", r.file.name() + "\n");
				}
				f.close();
				sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())));
				return true;
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return false;
	}
	
	BroadcastReceiver statusListener = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			mProgress.setVisibility(View.GONE);
		}
	};

	
	public void onClick(View v) {
		List<String> tags = mTagCloud.getSelectedTags();

		mProgress.setVisibility(View.VISIBLE);

		if(v.getId() == R.id.play) {
			Iterator<String> i = tags.iterator();
			String url = "boffin-tag://";
			while(i.hasNext()) {
				String tag = i.next();
				url += tag;
				if(i.hasNext())
					url += "*";
			}

			LastFMApplication.getInstance().playRadioStation(this, url, true);
		} else if(v.getId() == R.id.export) {
			new PlaylistExportTask(tags.toArray(new String[tags.size()])).execute((Void)null);
		}
	}
	
	private class PlaylistExportTask extends AsyncTaskEx<Void, Void, Boolean> {
		String[] tags;
		
		public PlaylistExportTask(String[] t) {
			tags = t;
		}
		
		@Override
		public void onPreExecute() {
		}
		
		@Override
		public Boolean doInBackground(Void... params) {
			return createPlaylistForTags(tags);
		}

		@Override
		public void onPostExecute(Boolean result) {
			mProgress.setVisibility(View.GONE);
			if(result) {
				LastFMApplication.getInstance().presentError(LocalTagCloud.this, "Export Complete", "Your playlist has been successfully exported and should be available in your Music app.");
			} else {
				LastFMApplication.getInstance().presentError(LocalTagCloud.this, "Export Failed", "Unable to export your playlist.  Please try another combination of tags.");
			}
		}
	}

}
