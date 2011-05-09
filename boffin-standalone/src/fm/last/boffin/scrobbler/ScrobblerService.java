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
package fm.last.boffin.scrobbler;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;
import fm.last.api.LastFmServer;
import fm.last.api.RadioTrack;
import fm.last.api.Session;
import fm.last.api.WSError;
import fm.last.boffin.AndroidLastFmServerFactory;
import fm.last.boffin.LastFMApplication;
import fm.last.boffin.R;
import fm.last.boffin.db.ScrobblerQueueDao;
import fm.last.boffin.utils.AsyncTaskEx;

/**
 * A Last.fm scrobbler for Android
 *
 * @author Sam Steele <sam@last.fm>
 *
 *         This is a scrobbler that can scrobble both our radio player as well
 *         as the built-in media player and other 3rd party apps that broadcast
 *         fm.last.boffin.metachanged notifications. We can't rely on
 *         com.android.music.metachanged due to a bug in the built-in media
 *         player that does not broadcast this notification when playing the
 *         first track, only when starting the next track.
 *
 *         Scrobbles and Now Playing data are serialized between launches, and
 *         will be sent when the track or network state changes. This service
 *         has a very short lifetime and is only started for a few seconds at a
 *         time when there's work to be done. This server is started when music
 *         state or network state change.
 *
 *         Scrobbles are submitted to the server after Now Playing info is sent,
 *         or when a network connection becomes available.
 *
 *         Sample code for a 3rd party to integrate with us is located at
 *         http://wiki.github.com/c99koder/lastfm-android/scrobbler-interface
 *
 */
public class ScrobblerService extends Object {
	public static final String META_CHANGED = "fm.last.android.metachanged";
	public static final String PLAYBACK_FINISHED = "fm.last.android.playbackcomplete";
	public static final String PLAYBACK_STATE_CHANGED = "fm.last.android.playstatechanged";
	public static final String STATION_CHANGED = "fm.last.android.stationchanged";
	public static final String PLAYBACK_ERROR = "fm.last.android.playbackerror";
	public static final String PLAYBACK_PAUSED = "fm.last.android.playbackpaused";
	public static final String UNKNOWN = "fm.last.android.unknown";
}
