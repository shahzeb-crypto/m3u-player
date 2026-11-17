# M3U Player (Android 15, libVLC)

A small player for local .m3u files and video files, built on libVLC.
Works with a remote (Android TV / box) or a touchscreen.

## Build the APK with GitHub (no Android Studio needed)
1. Create a free account at github.com and make a new repository.
2. Upload every file in this folder. The `.github/workflows/build.yml` file must keep that exact path.
   (If drag-and-drop skips the hidden `.github` folder, use Add file > Create new file,
   type `.github/workflows/build.yml` as the name, and paste the file's contents.)
3. Open the Actions tab. The "Build APK" run starts by itself (or click Run workflow).
4. When it turns green, open the run and download the `m3u-player-apk` artifact (a zip containing app-debug.apk).
5. Copy app-debug.apk to your box (USB drive or cloud), open it with a file manager,
   and allow "Install unknown apps" when asked.

## Build with Android Studio instead
Open this folder in Android Studio, then Build > Build APK(s).

## Using the app
- "Open .m3u file": choose your playlist. Entries must be web links (http/https) or absolute Android paths.
- "Pick video files": choose videos directly, which is the best option for local files.
- Leave "hardware decoding" OFF if you get sound but no picture.

Remote keys while playing:
- OK/Enter: pause or play
- Left/Right: back or forward 10 s
- Up/Down: next or previous item
- Back: return to the list

## Notes
- Playlist entries like `C:\Videos\a.mp4` can't work on Android and are skipped.
  Use "Pick video files" instead.
- This builds a debug APK, which is fine for personal use.
