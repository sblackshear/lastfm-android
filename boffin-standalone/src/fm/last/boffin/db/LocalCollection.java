package fm.last.boffin.db;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import fm.last.api.RadioTrack;
import fm.last.boffin.LastFMApplication;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class LocalCollection extends SQLiteOpenHelper 
{
	/**
	 * The name of the database.
	 */
	public static final String DB_NAME = "LocalCollection";
	
	/**
	 * The DB's version number.
	 * This needs to be increased on schema changes.
	 */
	public static final int DB_VERSION = 1;
	
	/**
	 * Singleton instance of {@link LocalCollection}.
	 */
	private static LocalCollection instance = null;

	/** 
	 * @return the {@link LocalCollection} singleton.
	 */
	public static LocalCollection getInstance() {
		if(instance != null) {
			return instance;
		} else {
			return new LocalCollection();
		}
	}
	
	/* An object to recieve progress updates */
	public LocalCollectionProgressCallback callback;
	
	public void clearDatabase()	{
		SQLiteDatabase db = getReadableDatabase();
		db.execSQL("DELETE FROM files");
		db.execSQL("DELETE FROM artists");
		db.execSQL("DELETE FROM simartists");
		db.execSQL("DELETE FROM tags");
		db.execSQL("DELETE FROM tracktags");
		db.close();
	}

	
	private LocalCollection() {
		super(LastFMApplication.getInstance().getApplicationContext(), DB_NAME, null, DB_VERSION);
	}

	/*
	 * (non-Javadoc)
	 * @see android.database.sqlite.SQLiteOpenHelper#onCreate(android.database.sqlite.SQLiteDatabase)
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		db.execSQL( "CREATE TABLE files (" +
                "id                INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                "filename          TEXT NOT NULL," +
                "modification_date INTEGER," +
                "lowercase_title   TEXT NOT NULL," +
                "artist            INTEGER," +
                "album             TEXT NOT NULL," +
                "duration          INTEGER," +
                "tag_time          INTEGER);" );
		db.execSQL( "CREATE INDEX files_artist_idx ON files ( artist );" );

		db.execSQL( "CREATE TABLE artists (" +
                "id                 INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
                "lowercase_name     TEXT NOT NULL UNIQUE );" );
		db.execSQL( "CREATE INDEX artists_name_idx ON artists ( lowercase_name );" );

		// artist a has similar artist b with weight
		db.execSQL( "CREATE TABLE simartists (" +
                "artist_a           INTEGER," +
                "artist_b           INTEGER," +
                "weight             INTEGER );" );
		db.execSQL( "CREATE INDEX simartists_artist_a_idx ON simartists ( artist_a );" );
    
		// create the table for the tags
		db.execSQL("CREATE TABLE tags (" +
				"id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT," +
				"name TEXT UNIQUE NOT NULL );");
		db.execSQL("CREATE INDEX tags_name_idx ON tags ( name );");

        // file has tag with weight
		db.execSQL( "CREATE TABLE tracktags (" +
                    "file               INTEGER NOT NULL," +     // files foreign key
                    "tag                INTEGER NOT NULL," +     // tags foreign key
                    "weight             FLOAT NOT NULL);" );    // 0-1
		db.execSQL( "CREATE INDEX tracktags_file_idx ON tracktags ( file ); ");
		db.execSQL( "CREATE INDEX tracktags_tag_idx ON tracktags ( tag ); ");
}

	/*
	 * (non-Javadoc)
	 * @see android.database.sqlite.SQLiteOpenHelper#onUpgrade(android.database.sqlite.SQLiteDatabase, int, int)
	 */
	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) 
	{
		// for now we just drop everything and create it again
		db.execSQL("DROP TABLE IF EXISTS files");
		db.execSQL("DROP TABLE IF EXISTS artists");
		db.execSQL("DROP TABLE IF EXISTS similarartists");
		db.execSQL("DROP TABLE IF EXISTS tags");
		db.execSQL("DROP TABLE IF EXISTS tracktags");

		onCreate(db);
	}
	
	public List<File> getFiles() {
		String query = "SELECT id, filename, modification_date FROM files";
		SQLiteDatabase db = null;
		Cursor c = null;
		try {
			db = getReadableDatabase();
			c = db.rawQuery(query, null);
			List<File> result = new ArrayList<File>(c.getCount());
			if (c.getCount() > 0) {
				c.moveToFirst();				
				// Loop through all Results
				do {
					File f = new File(c.getLong(c.getColumnIndex("id")),
							c.getString(c.getColumnIndex("filename")),
							c.getInt(c.getColumnIndex("modification_date")));
					result.add(f);
				} while (c.moveToNext());
			}
			c.close();
			db.close();
			return result;
		} finally {
			if(c != null)
				c.close();
			if(db != null)
				db.close();
		}
	}
	
	public File getFile(String artist, String title) {
		String query = "SELECT files.id AS id, filename, modification_date FROM files " +
			"INNER JOIN artists ON artists.id = files.artist " +
			"WHERE lowercase_name = ? AND lowercase_title = ?";
		SQLiteDatabase db = null;
		Cursor c = null;
		try {
			db = getReadableDatabase();
			c = db.rawQuery(query, new String[] { artist.toLowerCase(), title.toLowerCase() });
			File result = null;
			if (c.getCount() > 0) {
				c.moveToFirst();				
				result = new File(c.getLong(c.getColumnIndex("id")),
							c.getString(c.getColumnIndex("filename")),
							c.getInt(c.getColumnIndex("modification_date")));
			}
			c.close();
			db.close();
			return result;
		} finally {
			if(c != null)
				c.close();
			if(db != null)
				db.close();
		}
	}
	
	public void removeFile(long id) {
		SQLiteDatabase db = null;
		try {
			db = getWritableDatabase();
			db.delete("files", "id = ?", new String[] { String.valueOf(id) });
			db.delete("tracktags", "file = ?", new String[] { String.valueOf(id) });
			db.close();
		} finally {
			if(db != null)
				db.close();
		}
	}
	
	public int getArtistId(String artistName, boolean create) {
		int result = 0;
		String query = "SELECT id FROM artists WHERE lowercase_name = ?";
		SQLiteDatabase db = null;
		Cursor c = null;
		try {
			db = getReadableDatabase();
			c = db.rawQuery(query, new String[] { artistName.toLowerCase() });
			if (c.getCount() > 0) {
				c.moveToFirst();				
				result = c.getInt(c.getColumnIndex("id"));
			}
			c.close();
			db.close();
			if(result <= 0 && create) {
				db = getWritableDatabase();
				ContentValues values = new ContentValues();
				values.put("lowercase_name", artistName.toLowerCase());
				result = (int)db.insertOrThrow("artists", null, values);
				callback.artistAdded(artistName.toLowerCase());
				return result;
			}
		} finally {
			if(c != null)
				c.close();
			if(db != null)
				db.close();
		}
		return result;
	}

	public void addFile(String filename, long lastModified, FileMeta info) {
		SQLiteDatabase db = null;
		ContentValues values = new ContentValues();
		try {
			int artist_id = getArtistId(info.m_artist, true);
			db = getWritableDatabase();
			values.put("filename", filename);
			values.put("modification_date", lastModified);
			values.put("lowercase_title", info.m_title.toLowerCase());
			values.put("artist", artist_id);
			values.put("album", info.m_album);
			values.put("duration", info.m_duration);
			db.insertOrThrow("files", null, values);
		} finally {
			if(db != null)
				db.close();
		}
	}
	
	public List<FilesToTagResult> getFilesToTag(int maxTagAgeDays) {
	    long oldTagAge = (System.currentTimeMillis() / 1000) - maxTagAgeDays * 24* 60* 60;

		String query = "SELECT files.id, artists.lowercase_name, files.album, files.lowercase_title " +
	        "FROM files " +
	        "INNER JOIN artists ON artists.id = files.artist " +
	        "WHERE tag_time IS NULL " +
	        "OR tag_time < " + oldTagAge;
		SQLiteDatabase db = null;
		Cursor c = null;
		try {
			db = getReadableDatabase();
			c = db.rawQuery(query, null);
			List<FilesToTagResult> result = new ArrayList<FilesToTagResult>(c.getCount());
			if (c.getCount() > 0) {
				c.moveToFirst();				
				// Loop through all Results
				do {
					FilesToTagResult f = new FilesToTagResult();
					f.fileId = c.getLong(c.getColumnIndex("id"));
					f.artist = c.getString(c.getColumnIndex("lowercase_name"));
					f.album = c.getString(c.getColumnIndex("album"));
					f.title = c.getString(c.getColumnIndex("lowercase_title"));
					result.add(f);
				} while (c.moveToNext());
			}
			c.close();
			db.close();
			return result;
		} finally {
			if(c != null)
				c.close();
			if(db != null)
				db.close();
		}
	}

	long getTagId(String tag, boolean create) {
		int result = 0;
		String query = "SELECT id FROM tags WHERE name = ?";
		SQLiteDatabase db = null;
		Cursor c = null;
		try {
			db = getReadableDatabase();
			c = db.rawQuery(query, new String[] { tag.toLowerCase() });
			if (c.getCount() > 0) {
				c.moveToFirst();				
				result = c.getInt(c.getColumnIndex("id"));
			}
			c.close();
			db.close();
			if(result <= 0 && create) {
				db = getWritableDatabase();
				ContentValues values = new ContentValues();
				values.put("name", tag.toLowerCase());
				result = (int)db.insertOrThrow("tags", null, values);
				callback.tagAdded(tag.toLowerCase());
				return result;
			}
		} finally {
			if(c != null)
				c.close();
			if(db != null)
				db.close();
		}
		return result;
	}

	
	public List<Long> resolveTags(List<String> tags, HashMap<String, Long> map) {
	    // too slow hitting the db, build our own cache in front of it
	    List<Long> result = new ArrayList<Long>();
	    Iterator<String> i = tags.iterator();
	    int count = 0;
	    while(i.hasNext()) {
	    	String tag = i.next();
	    	if(map.containsKey(tag)) {
	    		result.add(map.get(tag));
	    	} else {
	    		Long id = new Long(getTagId(tag, true));
	    		map.put(tag, id);
	    		result.add(id);
	    	}
			callback.localCollectionProgress(count++, tags.size());
	    }
	    return result;
	}
	
	public void updateTrackTags(List<Long> fileIds, List<Long> tagIds, List<Float> weights) {		
		SQLiteDatabase db = null;
		try {
			db = getWritableDatabase();
			db.beginTransaction();
			for(int i = 0; i < fileIds.size(); i++) {
				ContentValues values = new ContentValues();
				values.put("file", fileIds.get(i));
				values.put("tag", tagIds.get(i));
				values.put("weight", weights.get(i));
				db.insertOrThrow("tracktags", null, values);
				callback.localCollectionProgress(i, fileIds.size());
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
			if(db != null)
				db.close();
		}
	}

	public void updateFileTagTime(List<Long> fileIds) {		
		SQLiteDatabase db = null;
		long time = System.currentTimeMillis() / 1000;
		try {
			db = getWritableDatabase();
			db.beginTransaction();
			for(int i = 0; i < fileIds.size(); i++) {
				ContentValues values = new ContentValues();
				values.put("tag_time", time);
				db.update("files", values, "id = ?", new String[] { String.valueOf(fileIds.get(i)) });
			}
			db.setTransactionSuccessful();
		} finally {
			db.endTransaction();
			if(db != null)
				db.close();
		}
	}

	// returns a list of files matching the specified tag
	public List<FilesWithTagResult>filesWithTag(String tag) {
        String query = "SELECT file, filename, modification_date, duration, lowercase_name, album, lowercase_title, weight FROM tracktags " +
        	"INNER JOIN files on tracktags.file = files.id " +
        	"INNER JOIN tags on tags.id = tracktags.tag " +
        	"INNER JOIN artists on files.artist = artists.id " +
        	"WHERE tags.name = ? ORDER BY weight ASC";

		SQLiteDatabase db = null;
		Cursor c = null;
		try {
			db = getReadableDatabase();
			c = db.rawQuery(query, new String[] { tag });
			List<FilesWithTagResult> result = new ArrayList<FilesWithTagResult>(c.getCount());
			if (c.getCount() > 0) {
				c.moveToFirst();				
				// Loop through all Results
				do {
					FilesWithTagResult r = new FilesWithTagResult();
					File f = new File(c.getLong(c.getColumnIndex("file")), c.getString(c.getColumnIndex("filename")), c.getLong(c.getColumnIndex("modification_date")));
					FileMeta m = new FileMeta(c.getString(c.getColumnIndex("lowercase_name")), c.getString(c.getColumnIndex("album")), c.getString(c.getColumnIndex("lowercase_title")), c.getLong(c.getColumnIndex("duration")));
					r.file = f;
					r.meta = m;
					r.weight = c.getFloat(c.getColumnIndex("weight"));
					result.add(r);
				} while (c.moveToNext());
			}
			c.close();
			db.close();
			return result;
		} finally {
			if(c != null)
				c.close();
			if(db != null)
				db.close();
		}
	}
	
	//Returns true if this file has the specified tag
	public boolean fileHasTag(long file, String tag) {
        String query = "SELECT file FROM tracktags " +
        	"INNER JOIN tags on tags.id = tracktags.tag " +
        	"WHERE tags.name = ? AND tracktags.file = ? ORDER BY weight ASC";

		SQLiteDatabase db = null;
		Cursor c = null;
		try {
			db = getReadableDatabase();
			c = db.rawQuery(query, new String[] { tag, String.valueOf(file) });
			boolean result = false;
			if (c.getCount() > 0) {
				result = true;
			}
			c.close();
			db.close();
			return result;
		} finally {
			if(c != null)
				c.close();
			if(db != null)
				db.close();
		}
	}
	
	// returns a list (of up to 'limit') of the weightiest tags
	public List<TopTagsResult>getTopTags(int limit) {
	    String query = "SELECT name, sum(weight) AS weight FROM tracktags " +
            "INNER JOIN tags ON tracktags.tag = tags.id " +
            "INNER JOIN files ON tracktags.file = files.id " +
            "GROUP BY tags.id " +
            "ORDER BY sum(weight) DESC ";

	    if( limit > 0 )
	    	query += "LIMIT " + limit;
	    
		SQLiteDatabase db = null;
		Cursor c = null;
		try {
			db = getReadableDatabase();
			c = db.rawQuery(query, null);
			List<TopTagsResult> result = new ArrayList<TopTagsResult>(c.getCount());
			if (c.getCount() > 0) {
				c.moveToFirst();				
				// Loop through all Results
				do {
					TopTagsResult f = new TopTagsResult();
					f.tag = c.getString(c.getColumnIndex("name"));
					f.weight = c.getFloat(c.getColumnIndex("weight"));
					result.add(f);
				} while (c.moveToNext());
			}
			c.close();
			db.close();
			return result;
		} finally {
			if(c != null)
				c.close();
			if(db != null)
				db.close();
		}
	}

	private List<String> recentTracks = new ArrayList<String>(25);

	public List<FilesWithTagResult> getFilesWithTags(String[] tags, int count) {
		String tag = tags[0];
		List<LocalCollection.FilesWithTagResult> files = LocalCollection.getInstance().filesWithTag(tag);
		List<LocalCollection.FilesWithTagResult> result = new ArrayList<LocalCollection.FilesWithTagResult>(20);
		List<String> artists = new ArrayList<String>();

		if(tags.length > 1) {
			for(int t = 1; t < tags.length; t++) {
				Iterator<LocalCollection.FilesWithTagResult>i = new ArrayList<LocalCollection.FilesWithTagResult>(files).iterator();
				while(i.hasNext()) {
					LocalCollection.FilesWithTagResult r = i.next();
					if(!LocalCollection.getInstance().fileHasTag(r.file.id(), tags[t])) {
						files.remove(r);
					}
				}
			}
		}
		
		for(int x = 0; x < count; x++) {
			float totalWeight;
			double p = Math.random();

			totalWeight = 0;
			Iterator<LocalCollection.FilesWithTagResult> i = files.iterator();
			while(i.hasNext()) {
				LocalCollection.FilesWithTagResult r = i.next();
				totalWeight += r.weight;
			}
			
			i = files.iterator();
			while(i.hasNext() && p > 0) {
				LocalCollection.FilesWithTagResult r = i.next();
				float pushDown = 1.0f;
				if(recentTracks.contains(r.meta.m_album+r.meta.m_artist+r.meta.m_title))
					pushDown *= 0.001f;

				if(artists.contains(r.meta.m_artist))
					pushDown *= 0.001f;
				
				
				if(p < ((r.weight * pushDown) / totalWeight) && r.weight > 1) {
					result.add(r);
					artists.add(r.meta.m_artist);
					if(artists.size() >= 5)
						artists.remove(0);
					recentTracks.add(r.meta.m_album+r.meta.m_artist+r.meta.m_title);
					if(recentTracks.size() >= 25)
						recentTracks.remove(0);
					files.remove(r);
					break;
				} else {
					p -= (r.weight / totalWeight);
				}
			}
		}
		if(result.size() < 1 && recentTracks.size() > 1) {
			recentTracks.clear();
			return getFilesWithTags(tags, count);
		}
		return result;
	}
	
	public interface LocalCollectionProgressCallback {
		public void localCollectionProgress(int current, int total);
		public void artistAdded(String artist);
		public void tagAdded(String tag);
	}
	
    public class File {
        public File(long id, String name, long lastModified) {
        	m_id = id;
        	m_name = name;
        	m_lastModified = lastModified;
        }

        public long id() { return m_id; }
        public String name() { return m_name; }
        public long lastModified() { return m_lastModified; }

        private long m_id;
        private String m_name;
        private long m_lastModified;
    };
    
    public class FileMeta {
        public FileMeta(String artist, String album, String title, long duration) {
        	m_artist = artist;
        	m_album = album;
        	m_title = title;
        	m_duration = duration;
        }

        public String m_artist;
        public String m_album;
        public String m_title;
        public long m_duration;
    };

    public class ResolveResult extends FileMeta {
    	public ResolveResult(
            String artist, 
            String album, 
            String title, 
            float artistMatch, 
            float titleMatch,
            String filename, 
            long duration) {
            	super(artist, album, title, duration);
            	m_matchQuality = artistMatch * titleMatch;
            	m_filename = filename;
        }

        public float m_matchQuality;
        public String m_filename;
    };

    public class FilesToTagResult {
        public long fileId;
        public String artist;
        public String album;
        public String title;
    };
    
    public class TopTagsResult {
    	public String tag;
    	public float weight;
    };

    public class FilesWithTagResult {
        public File file;
        public FileMeta meta;
        public float weight;
        
        private String cap( String s ) {
        	for (int i = 0; i < s.length(); i++) {

                if (i == 0) {
                    // Capitalize the first letter of the string.
                    s = String.format( "%s%s",
                                 Character.toUpperCase(s.charAt(0)),
                                 s.substring(1) );
                }

                // Is this character a non-letter or non-digit?  If so
                // then this is probably a word boundary so let's capitalize
                // the next character in the sequence.
                if (!Character.isLetterOrDigit(s.charAt(i))) {
                    if (i + 1 < s.length()) {
                        s = String.format( "%s%s%s",
                                     s.subSequence(0, i+1),
                                     Character.toUpperCase(s.charAt(i + 1)),
                                     s.substring(i+2) );
                    }
                }

            }
                
            return s;
                        
        }
        
        public RadioTrack toRadioTrack() {
        	//	public RadioTrack(String locationUrl, String title, String identifier, String album, String creator, String duration, String imageUrl, String trackAuth, Boolean loved, String[] context) {
        	return new RadioTrack(file.name(), cap(meta.m_title), "", cap(meta.m_album), cap(meta.m_artist), String.valueOf(meta.m_duration), "", "", false, null);
        }
    };
}
