package fm.last.android;

import fm.last.android.activity.MediaScanner;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

public class Boffin extends Activity {
	public void onCreate(Bundle icicle) {
		super.onCreate(icicle);
		Intent intent = new Intent(Boffin.this, MediaScanner.class);
		startActivity(intent);
		finish();
	}
}
