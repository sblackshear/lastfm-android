package fm.last.android.activity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fm.last.android.LastFMApplication;
import fm.last.android.R;
import fm.last.android.adapter.ListAdapter;
import fm.last.android.db.LocalCollection;
import fm.last.android.db.LocalCollection.TopTagsResult;
import fm.last.android.widget.TagCloud;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.AbsListView;
import android.widget.LinearLayout;
import android.widget.ScrollView;

public class LocalTagCloud extends Activity implements OnClickListener, TextWatcher {
	String[] tags;
	ListAdapter adapter;
	private EditText mSearchText;
	private TagCloud mTagCloud;
	private ImageButton mSearchButton;
	private ImageButton mVoiceButton;
	
	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
		mTagCloud = new TagCloud(this);
		setTitle("Boffin: Top Tags");
		setContentView(R.layout.search);

		LinearLayout layout = (LinearLayout)findViewById(R.id.layout);
		layout.removeAllViews();
		//ScrollView sv = new ScrollView( this );
		//sv.addView( mTagCloud, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		layout.addView(mTagCloud, new LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.WRAP_CONTENT));
		
        List<TopTagsResult> topTags = LocalCollection.getInstance().getTopTags(80);
        List<String> tagsList = new ArrayList<String>(topTags.size());
        Iterator<TopTagsResult> i = topTags.iterator();
        while(i.hasNext()) {
        	TopTagsResult r = i.next();
        	//tagsList.add(r.tag);
        	mTagCloud.addTag(r.tag, r.weight);
        }
        mTagCloud.normalizeSizes();
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
        //setTags(tags);
    }
	
	private void setTags( String[] tags ) {
        mTagCloud.clear();
        for (String tag : tags) {
        	//mTagCloud.addTag(tag);
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
	        setTags(filteredTags.toArray(new String[filteredTags.size()]));
		} else {
	        adapter = new ListAdapter(this, tags);
	        adapter.disableDisclosureIcons();
	        setTags( tags );
		}
	}
	
	protected void onListItemClick(AbsListView l, View v, int position, long id) {
		LastFMApplication.getInstance().playRadioStation(this, "boffin-tag://" + adapter.getItem(position), true);
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
