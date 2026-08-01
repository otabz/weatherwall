# Uploading with GitHub's website

Upload everything at the project root:

```text
.github/
app/
gradle/
.gitignore
build.gradle.kts
gradle.properties
gradlew
gradlew.bat
LICENSE
PROJECT_TREE.txt
README.md
settings.gradle.kts
THIRD_PARTY_NOTICES.md
UPLOAD_TO_GITHUB.md
```

Some browsers refuse to upload the hidden `.github` folder. In that case:

1. Upload every other file and folder.
2. On GitHub choose **Add file → Create new file**.
3. Enter `.github/workflows/build-apk.yml` as the filename.
4. Copy the contents of the local workflow file into the GitHub editor.
5. Commit to the `main` branch.
6. Open **Actions → Build SkyFlow Live Weather APK → Run workflow**.
7. Download the `SkyFlow-Live-Weather-APK` artifact after it succeeds.
