package fm.last.android.activity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import fm.last.android.LastFMApplication;
import fm.last.android.db.LocalCollection;
import fm.last.android.db.LocalCollection.TopTagsResult;

import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

public class TopLocalTags extends ListActivity {
	String[] tags;
	
	@Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
		setTitle("Boffin: Top Tags");

        List<TopTagsResult> topTags = LocalCollection.getInstance().getTopTags(25);
        List<String> tagsList = new ArrayList<String>(topTags.size());
        Iterator<TopTagsResult> i = topTags.iterator();
        while(i.hasNext()) {
        	TopTagsResult r = i.next();
        	tagsList.add(r.tag);
        }
        
        tags = tagsList.toArray(new String[tagsList.size()]);
        
        // Create an ArrayAdapter, that will actually make the Strings above appear in the ListView
        setListAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, tags));
        
    }
	
	protected void onListItemClick(ListView l, View v, int position, long id) {
		LastFMApplication.getInstance().playRadioStation(this, "boffin-tag://" + tags[position], true);
	}
}
