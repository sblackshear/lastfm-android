package fm.last.boffin.activity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fm.last.boffin.LastFMApplication;
import fm.last.boffin.R;
import fm.last.boffin.adapter.ListAdapter;
import fm.last.boffin.db.LocalCollection;
import fm.last.boffin.db.LocalCollection.TopTagsResult;

import android.app.ListActivity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListView;

public class TopLocalTags extends ListActivity implements OnClickListener, TextWatcher {
	String[] tags;
	ListAdapter adapter;
	private EditText mSearchText;
	private ImageButton mSearchButton;
	private ImageButton mVoiceButton;
	
	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
		setTitle("Boffin: Top Tags");
		setContentView(R.layout.search);

        List<TopTagsResult> topTags = LocalCollection.getInstance().getTopTags(100);
        List<String> tagsList = new ArrayList<String>(topTags.size());
        Iterator<TopTagsResult> i = topTags.iterator();
        while(i.hasNext()) {
        	TopTagsResult r = i.next();
        	tagsList.add(r.tag);
        }
		mSearchText = (EditText)findViewById(R.id.station_editbox);
		mSearchText.addTextChangedListener(this);
		mSearchText.setHint("Enter a tag");
		
		mSearchButton = (ImageButton)findViewById(R.id.search);
		mSearchButton.setVisibility(View.GONE);
		
		mVoiceButton = (ImageButton)findViewById(R.id.voice);
		// Check to see if a recognition activity is present
		PackageManager pm = getPackageManager();
		List<ResolveInfo> activities = pm.queryIntentActivities(
		  new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
		if (activities.size() != 0) {
			mVoiceButton.setOnClickListener(this);
		} else {
			mVoiceButton.setVisibility(View.GONE);
		}

        tags = tagsList.toArray(new String[tagsList.size()]);
        adapter = new ListAdapter(this, tags);
        adapter.disableDisclosureIcons();
        setListAdapter(adapter);
        getListView().setVisibility(View.VISIBLE);
    }
	
	private void createPlaylistForTags(String[] tags) {
		List<LocalCollection.FilesWithTagResult> files = LocalCollection.getInstance().getFilesWithTags(tags, 100);
		Log.i("Last.fm", "Got " + files.size() + " tracks");
		if(files.size() > 0) {
			File sdCard = Environment.getExternalStorageDirectory();
			File dir = new File (sdCard.getAbsolutePath() + "/Music");
			dir.mkdirs();
			String filename = "Music tagged ";
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
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	private void search() {
		String query = mSearchText.getText().toString();
		if(query.length() > 0) {
			List<String> filteredTags = new ArrayList<String>(tags.length);
			for(int i = 0; i < tags.length; i++) {
				if(tags[i].contains(query.toLowerCase()))
					filteredTags.add(tags[i]);
			}
	        adapter = new ListAdapter(this, filteredTags.toArray(new String[filteredTags.size()]));
	        adapter.disableDisclosureIcons();
	        setListAdapter(adapter);
		} else {
	        adapter = new ListAdapter(this, tags);
	        adapter.disableDisclosureIcons();
	        setListAdapter(adapter);
		}
	}
	
	protected void onListItemClick(ListView l, View v, int position, long id) {
		createPlaylistForTags(new String[] { (String)adapter.getItem(position), "90s", "female vocalists" });
		//LastFMApplication.getInstance().playRadioStation(this, "boffin-tag://" + adapter.getItem(position), true);
	}
	
	public void onClick(View v) {
		if(v.getId() == R.id.voice) {
			try {
		        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
		        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
		                RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
		        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say a tag");
		        startActivityForResult(intent, 1234);
			} catch (ActivityNotFoundException e) {
				LastFMApplication.getInstance().presentError(this, getString(R.string.ERROR_VOICE_TITLE), getString(R.string.ERROR_VOICE));
			}
		} else {
			search();
		}
	}
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1234 && resultCode == RESULT_OK) {
            // Fill the list view with the strings the recognizer thought it could have heard
            ArrayList<String> matches = data.getStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS);
            mSearchText.setText(matches.get(0));
            search();
        }

        super.onActivityResult(requestCode, resultCode, data);
    }
    
	public void afterTextChanged(Editable arg0) {
	}

	public void beforeTextChanged(CharSequence arg0, int arg1, int arg2,
			int arg3) {
	}

	public void onTextChanged(CharSequence s, int start, int before, int count) {
		search();
	}
}
