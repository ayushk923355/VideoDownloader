# Video Downloader (stage 2)

Kotlin + Jetpack Compose. Three tabs: Home (download + progress), Library (history + player), Settings (default quality).

## What it does
- Downloads with yt-dlp + FFmpeg (youtubedl-android). Only sites yt-dlp supports; DRM/login videos are refused.
- Quality: you pick 144p-1440p. If that isn't available, the highest one below it is used. Hard cap 2560x1440 (portrait rotated).
- Progress comes from real bytes. 100% / "Download Complete" only after the file is merged, verified and saved.
- Saves to Download/Video Downloader/ (MediaStore; the folder is recreated automatically on the next download).
- Library plays through a media service: keeps playing after you swipe the app away, with notification controls.

## Build with GitHub Actions
1. Create a new empty GitHub repo.
2. In Termux, inside this folder:
   git init && git add . && git commit -m "Video Downloader"
   git branch -M main
   git remote add origin https://github.com/<you>/<repo>.git
   git push -u origin main   (log in with `gh auth login` or a token prompt, never put a token in files)
3. Open the repo > Actions > "Build APK" > wait for green > download the artifact VideoDownloader-debug-apk.
4. Unzip, install app-debug.apk on your phone (allow install from this source).

## Known limits / unverified
- Never compiled yet. The youtubedl-android version (0.17.2) and its callback signature are from memory.
- No true pause (yt-dlp can't). Cancel and Retry only.
- Direct links with no readable resolution are refused (can't prove they are under 1440p).
- Sites change often; yt-dlp inside the app needs an update when a site breaks.
