# NightOff In-App Update Distribution Guide

This directory manages in-app updates for NightOff via GitHub.

## How In-App Updates Work:

1. **Detection**:
   - On app startup (and in Developer settings), NightOff queries:
     `https://raw.githubusercontent.com/BIJOY-CYBER-404/NightOff/main/version.json`
   - It also checks the GitHub Releases API:
     `https://api.github.com/repos/BIJOY-CYBER-404/NightOff/releases/latest`

2. **Triggering an Update**:
   - To push a new version update to all users, simply:
     1. Build the APK (`gradle assembleDebug` or assemble release).
     2. Update `version.json` in the root of your GitHub repository with the new `latestVersionName` and `latestVersionCode`.
     3. Go to `https://github.com/BIJOY-CYBER-404/NightOff/releases/new`.
     4. Tag the release (e.g. `v1.12`) and attach `NightOff.apk`.
   - Users opening the NightOff app will immediately see the **"New Update Available"** dialog!

3. **Auto-Installation**:
   - Tapping **"Update Now"** downloads the APK via Android `DownloadManager`.
   - Once the download finishes, `UpdateDownloadReceiver` invokes `FileProvider` and launches Android's native package installer to update the app without losing data.
