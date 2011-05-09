/*
 * This file is auto-generated.  DO NOT MODIFY.
 * Original file: /home/sam/lastfm-android/boffin-standalone/src/fm/last/boffin/player/IRadioPlayer.aidl
 */
package fm.last.boffin.player;
public interface IRadioPlayer extends android.os.IInterface
{
/** Local-side IPC implementation stub class. */
public static abstract class Stub extends android.os.Binder implements fm.last.boffin.player.IRadioPlayer
{
private static final java.lang.String DESCRIPTOR = "fm.last.boffin.player.IRadioPlayer";
/** Construct the stub at attach it to the interface. */
public Stub()
{
this.attachInterface(this, DESCRIPTOR);
}
/**
 * Cast an IBinder object into an fm.last.boffin.player.IRadioPlayer interface,
 * generating a proxy if needed.
 */
public static fm.last.boffin.player.IRadioPlayer asInterface(android.os.IBinder obj)
{
if ((obj==null)) {
return null;
}
android.os.IInterface iin = (android.os.IInterface)obj.queryLocalInterface(DESCRIPTOR);
if (((iin!=null)&&(iin instanceof fm.last.boffin.player.IRadioPlayer))) {
return ((fm.last.boffin.player.IRadioPlayer)iin);
}
return new fm.last.boffin.player.IRadioPlayer.Stub.Proxy(obj);
}
public android.os.IBinder asBinder()
{
return this;
}
@Override public boolean onTransact(int code, android.os.Parcel data, android.os.Parcel reply, int flags) throws android.os.RemoteException
{
switch (code)
{
case INTERFACE_TRANSACTION:
{
reply.writeString(DESCRIPTOR);
return true;
}
case TRANSACTION_setSession:
{
data.enforceInterface(DESCRIPTOR);
fm.last.api.Session _arg0;
if ((0!=data.readInt())) {
_arg0 = fm.last.api.Session.CREATOR.createFromParcel(data);
}
else {
_arg0 = null;
}
this.setSession(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_tune:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _arg0;
_arg0 = data.readString();
fm.last.api.Session _arg1;
if ((0!=data.readInt())) {
_arg1 = fm.last.api.Session.CREATOR.createFromParcel(data);
}
else {
_arg1 = null;
}
boolean _result = this.tune(_arg0, _arg1);
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_pause:
{
data.enforceInterface(DESCRIPTOR);
this.pause();
reply.writeNoException();
return true;
}
case TRANSACTION_stop:
{
data.enforceInterface(DESCRIPTOR);
this.stop();
reply.writeNoException();
return true;
}
case TRANSACTION_startRadio:
{
data.enforceInterface(DESCRIPTOR);
this.startRadio();
reply.writeNoException();
return true;
}
case TRANSACTION_skip:
{
data.enforceInterface(DESCRIPTOR);
this.skip();
reply.writeNoException();
return true;
}
case TRANSACTION_getArtistName:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getArtistName();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getAlbumName:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getAlbumName();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getTrackName:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getTrackName();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getArtUrl:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getArtUrl();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getDuration:
{
data.enforceInterface(DESCRIPTOR);
long _result = this.getDuration();
reply.writeNoException();
reply.writeLong(_result);
return true;
}
case TRANSACTION_getLoved:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.getLoved();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_setLoved:
{
data.enforceInterface(DESCRIPTOR);
boolean _arg0;
_arg0 = (0!=data.readInt());
this.setLoved(_arg0);
reply.writeNoException();
return true;
}
case TRANSACTION_getPosition:
{
data.enforceInterface(DESCRIPTOR);
long _result = this.getPosition();
reply.writeNoException();
reply.writeLong(_result);
return true;
}
case TRANSACTION_getBufferPercent:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getBufferPercent();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_getContext:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String[] _result = this.getContext();
reply.writeNoException();
reply.writeStringArray(_result);
return true;
}
case TRANSACTION_isPlaying:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.isPlaying();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_getState:
{
data.enforceInterface(DESCRIPTOR);
int _result = this.getState();
reply.writeNoException();
reply.writeInt(_result);
return true;
}
case TRANSACTION_getStationName:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getStationName();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getStationUrl:
{
data.enforceInterface(DESCRIPTOR);
java.lang.String _result = this.getStationUrl();
reply.writeNoException();
reply.writeString(_result);
return true;
}
case TRANSACTION_getError:
{
data.enforceInterface(DESCRIPTOR);
fm.last.api.WSError _result = this.getError();
reply.writeNoException();
if ((_result!=null)) {
reply.writeInt(1);
_result.writeToParcel(reply, android.os.Parcelable.PARCELABLE_WRITE_RETURN_VALUE);
}
else {
reply.writeInt(0);
}
return true;
}
case TRANSACTION_getPauseButtonPressed:
{
data.enforceInterface(DESCRIPTOR);
boolean _result = this.getPauseButtonPressed();
reply.writeNoException();
reply.writeInt(((_result)?(1):(0)));
return true;
}
case TRANSACTION_pauseButtonPressed:
{
data.enforceInterface(DESCRIPTOR);
this.pauseButtonPressed();
reply.writeNoException();
return true;
}
}
return super.onTransact(code, data, reply, flags);
}
private static class Proxy implements fm.last.boffin.player.IRadioPlayer
{
private android.os.IBinder mRemote;
Proxy(android.os.IBinder remote)
{
mRemote = remote;
}
public android.os.IBinder asBinder()
{
return mRemote;
}
public java.lang.String getInterfaceDescriptor()
{
return DESCRIPTOR;
}
public void setSession(fm.last.api.Session session) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
if ((session!=null)) {
_data.writeInt(1);
session.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_setSession, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public boolean tune(java.lang.String url, fm.last.api.Session session) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeString(url);
if ((session!=null)) {
_data.writeInt(1);
session.writeToParcel(_data, 0);
}
else {
_data.writeInt(0);
}
mRemote.transact(Stub.TRANSACTION_tune, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public void pause() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_pause, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void stop() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_stop, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void startRadio() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_startRadio, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public void skip() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_skip, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public java.lang.String getArtistName() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getArtistName, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public java.lang.String getAlbumName() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getAlbumName, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public java.lang.String getTrackName() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getTrackName, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public java.lang.String getArtUrl() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getArtUrl, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public long getDuration() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
long _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getDuration, _data, _reply, 0);
_reply.readException();
_result = _reply.readLong();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public boolean getLoved() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getLoved, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public void setLoved(boolean loved) throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
_data.writeInt(((loved)?(1):(0)));
mRemote.transact(Stub.TRANSACTION_setLoved, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
public long getPosition() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
long _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getPosition, _data, _reply, 0);
_reply.readException();
_result = _reply.readLong();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public int getBufferPercent() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getBufferPercent, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public java.lang.String[] getContext() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String[] _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getContext, _data, _reply, 0);
_reply.readException();
_result = _reply.createStringArray();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public boolean isPlaying() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_isPlaying, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public int getState() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
int _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getState, _data, _reply, 0);
_reply.readException();
_result = _reply.readInt();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public java.lang.String getStationName() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getStationName, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public java.lang.String getStationUrl() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
java.lang.String _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getStationUrl, _data, _reply, 0);
_reply.readException();
_result = _reply.readString();
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public fm.last.api.WSError getError() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
fm.last.api.WSError _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getError, _data, _reply, 0);
_reply.readException();
if ((0!=_reply.readInt())) {
_result = fm.last.api.WSError.CREATOR.createFromParcel(_reply);
}
else {
_result = null;
}
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public boolean getPauseButtonPressed() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
boolean _result;
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_getPauseButtonPressed, _data, _reply, 0);
_reply.readException();
_result = (0!=_reply.readInt());
}
finally {
_reply.recycle();
_data.recycle();
}
return _result;
}
public void pauseButtonPressed() throws android.os.RemoteException
{
android.os.Parcel _data = android.os.Parcel.obtain();
android.os.Parcel _reply = android.os.Parcel.obtain();
try {
_data.writeInterfaceToken(DESCRIPTOR);
mRemote.transact(Stub.TRANSACTION_pauseButtonPressed, _data, _reply, 0);
_reply.readException();
}
finally {
_reply.recycle();
_data.recycle();
}
}
}
static final int TRANSACTION_setSession = (android.os.IBinder.FIRST_CALL_TRANSACTION + 0);
static final int TRANSACTION_tune = (android.os.IBinder.FIRST_CALL_TRANSACTION + 1);
static final int TRANSACTION_pause = (android.os.IBinder.FIRST_CALL_TRANSACTION + 2);
static final int TRANSACTION_stop = (android.os.IBinder.FIRST_CALL_TRANSACTION + 3);
static final int TRANSACTION_startRadio = (android.os.IBinder.FIRST_CALL_TRANSACTION + 4);
static final int TRANSACTION_skip = (android.os.IBinder.FIRST_CALL_TRANSACTION + 5);
static final int TRANSACTION_getArtistName = (android.os.IBinder.FIRST_CALL_TRANSACTION + 6);
static final int TRANSACTION_getAlbumName = (android.os.IBinder.FIRST_CALL_TRANSACTION + 7);
static final int TRANSACTION_getTrackName = (android.os.IBinder.FIRST_CALL_TRANSACTION + 8);
static final int TRANSACTION_getArtUrl = (android.os.IBinder.FIRST_CALL_TRANSACTION + 9);
static final int TRANSACTION_getDuration = (android.os.IBinder.FIRST_CALL_TRANSACTION + 10);
static final int TRANSACTION_getLoved = (android.os.IBinder.FIRST_CALL_TRANSACTION + 11);
static final int TRANSACTION_setLoved = (android.os.IBinder.FIRST_CALL_TRANSACTION + 12);
static final int TRANSACTION_getPosition = (android.os.IBinder.FIRST_CALL_TRANSACTION + 13);
static final int TRANSACTION_getBufferPercent = (android.os.IBinder.FIRST_CALL_TRANSACTION + 14);
static final int TRANSACTION_getContext = (android.os.IBinder.FIRST_CALL_TRANSACTION + 15);
static final int TRANSACTION_isPlaying = (android.os.IBinder.FIRST_CALL_TRANSACTION + 16);
static final int TRANSACTION_getState = (android.os.IBinder.FIRST_CALL_TRANSACTION + 17);
static final int TRANSACTION_getStationName = (android.os.IBinder.FIRST_CALL_TRANSACTION + 18);
static final int TRANSACTION_getStationUrl = (android.os.IBinder.FIRST_CALL_TRANSACTION + 19);
static final int TRANSACTION_getError = (android.os.IBinder.FIRST_CALL_TRANSACTION + 20);
static final int TRANSACTION_getPauseButtonPressed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 21);
static final int TRANSACTION_pauseButtonPressed = (android.os.IBinder.FIRST_CALL_TRANSACTION + 22);
}
public void setSession(fm.last.api.Session session) throws android.os.RemoteException;
public boolean tune(java.lang.String url, fm.last.api.Session session) throws android.os.RemoteException;
public void pause() throws android.os.RemoteException;
public void stop() throws android.os.RemoteException;
public void startRadio() throws android.os.RemoteException;
public void skip() throws android.os.RemoteException;
public java.lang.String getArtistName() throws android.os.RemoteException;
public java.lang.String getAlbumName() throws android.os.RemoteException;
public java.lang.String getTrackName() throws android.os.RemoteException;
public java.lang.String getArtUrl() throws android.os.RemoteException;
public long getDuration() throws android.os.RemoteException;
public boolean getLoved() throws android.os.RemoteException;
public void setLoved(boolean loved) throws android.os.RemoteException;
public long getPosition() throws android.os.RemoteException;
public int getBufferPercent() throws android.os.RemoteException;
public java.lang.String[] getContext() throws android.os.RemoteException;
public boolean isPlaying() throws android.os.RemoteException;
public int getState() throws android.os.RemoteException;
public java.lang.String getStationName() throws android.os.RemoteException;
public java.lang.String getStationUrl() throws android.os.RemoteException;
public fm.last.api.WSError getError() throws android.os.RemoteException;
public boolean getPauseButtonPressed() throws android.os.RemoteException;
public void pauseButtonPressed() throws android.os.RemoteException;
}
