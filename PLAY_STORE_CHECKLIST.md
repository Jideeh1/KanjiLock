# Google Play release checklist

Things to sort out before publishing KanjiLock on Google Play. Based on Google Play's policies and help pages as of October 2026. This is a practical checklist, not legal advice. If you plan to make money from the app or are worried about a specific risk, talk to a lawyer.

## 1. App name

- [ ] An iOS app called "KanjiLock: App Blocker & Focus" (Velzie LLC) already exists in the same Japanese-learning niche. Using the same name risks a trademark complaint and Play's impersonation policy. Pick a different name.
- [ ] Search the name in the trademark databases for where you live and where you will sell (IPOS for Singapore, WIPO Global Brand Database, USPTO) and in the Play Store and App Store.
- [ ] Rename: edit the `<!ENTITY app "...">` line at the top of `app/src/main/res/values/strings.xml`. Also change the Drive folder name (`DriveSync` in Engine.kt) and the Anki deck name (`AcceptedStore` in Data.kt) if you want.
- [ ] Consider changing the package name `com.jideeh.kanjilock` before the first upload. It can never be changed after publishing. If you change it, update the OAuth client too.

## 2. Developer account

- [ ] Create a Play Console developer account (USD 25 one-time fee), complete identity verification and the device verification in the Play Console app.
- [ ] Personal accounts created after 13 November 2023 must run a closed test with at least 12 testers who stay opted in for 14 days in a row before applying for production access. Recruit testers early.
- [ ] After the test, apply for production access from the Dashboard (Google says reviews usually take up to 7 days).

## 3. Android developer verification

- [ ] Since 30 September 2026, certified Android devices in Brazil, Indonesia, Singapore and Thailand only install apps from verified developers with registered package names, including APKs installed outside Play. Global rollout follows in 2027.
- [ ] Publishing on Play with a verified Play Console account covers this. Register the package name in Play Console.
- [ ] If you only want to share the APK with friends without Play, a free "limited distribution" account allows up to 20 devices without identity verification.

## 4. Signing

- [ ] Create an upload key (Android Studio: Build > Generate Signed App Bundle). Keep the keystore and passwords backed up. Remove the debug `signingConfig` from the release build type in `app/build.gradle.kts`.
- [ ] Upload an Android App Bundle (.aab), not an APK, and enroll in Play App Signing.
- [ ] Add the Play App Signing SHA-1 (Play Console > Test and release > App integrity) as another Android OAuth client in Google Cloud, otherwise Drive linking fails for Play installs.

## 5. Target API level

- [x] New apps and updates must target Android 16 (API 36) or higher since 31 August 2026. The app targets API 37.

## 6. Privacy policy

- [ ] Fill in the effective date and contact email in `PRIVACY_POLICY.md`.
- [ ] Host it at a public, non-PDF URL that anyone can open without signing in. GitHub Pages works.
- [ ] Paste the URL in Play Console (App content > Privacy policy) and on the Google OAuth consent screen.
- [x] The policy is also inside the app (Settings > Privacy policy).

## 7. Data safety form (App content > Data safety)

Suggested answers, based on what the code does today. Review them yourself; you are responsible for accuracy.

- Does your app collect or share user data? **Yes** (only because optional Drive sync sends data off the device, even though it goes to the user's own Drive).
- Is all data encrypted in transit? **Yes** (HTTPS).
- Can users request deletion? **Yes** (Delete Drive copy in Settings, clearing app data).
- Data types collected:
  - App activity > Other user-generated content (word lists, decks) and App interactions (review history, streak days).
  - Collection is **optional**. Purpose: **App functionality**. Not used for advertising or analytics.
- Shared with third parties: **No**. Transfers the user starts themselves (Drive, export) do not count as sharing.
- No ads, no analytics SDKs, no crash reporting.

If you later add ads, analytics or crash reporting, update this form and the privacy policy before the update ships.

## 8. Google Drive (OAuth)

- [ ] Google Cloud project with the Drive API enabled.
- [ ] OAuth consent screen: app name (must match the Play listing name), support email, home page URL, privacy policy URL, authorized domain.
- [ ] Scope: only `drive.file`. It is a non-sensitive scope, so Google's verification review is not needed. Brand verification is optional.
- [ ] Set the publishing status to **In production**. In "Testing", only listed test users can sign in and tokens expire after 7 days.
- [x] Limited Use disclosure is in the privacy policy and the app.

## 9. Permissions

- [x] POST_NOTIFICATIONS, RECEIVE_BOOT_COMPLETED, INTERNET: normal use, no declaration form needed.
- [x] No exact alarms (`setWindow` / inexact repeating), no foreground service, no QUERY_ALL_PACKAGES (only a targeted `<queries>` entry for AnkiDroid).
- [x] AnkiDroid permission is requested only after an in-app explanation dialog.

## 10. Content rating and audience

- [ ] Fill in the IARC content rating questionnaire honestly. The dictionary is a full Japanese dictionary and can show crude or offensive words if the user searches for them. Vulgar, derogatory and sensitive entries are already excluded from the random word of the day.
- [ ] Target audience: choose 13+ or 18+ only. Do not select under-13 age groups, which would bring in the Families policy.
- [ ] Category: Education. No ads: answer "No" to "Contains ads".

## 11. Store listing

- [ ] Real screenshots from the app.
- [ ] Describe the lock-screen feature accurately, for example "home-screen widget, lock-screen widget on supported phones (Samsung with Good Lock, Android 16 QPR2 and newer), and a lock-screen notification". Do not promise it works on every phone.
- [ ] Use other brands only to describe compatibility ("imports Anki .apkg decks", "syncs reviews through AnkiDroid"). Do not use their logos or put their names in the app title or icon.
- [ ] Mention the data sources in the description, for example "Dictionary data from JMdict/KANJIDIC2 (EDRDG, CC BY-SA 4.0) and Tatoeba (CC BY 2.0 FR)".
- [ ] AI-assisted code does not need to be disclosed.

## 12. Licenses for bundled content

- [x] EDRDG statement, links, CC BY-SA 4.0 and a description of the changes are in the app (Settings > Sources and licenses) and in THIRD_PARTY_NOTICES.md.
- [x] Tatoeba credited, each sentence links back to its page.
- [x] Inter font OFL notice and full license text are in the app.
- [ ] EDRDG asks apps to update the dictionary data regularly. Rebuild the dictionary before each release (README, "updating the dictionary").
- [ ] The dictionary database is CC BY-SA 4.0. If you ever sell the app, that is still allowed, but the database itself must stay under CC BY-SA and unencrypted.

## Useful links

- Developer Program Policy: https://play.google.com/about/developer-content-policy/
- Testing requirements for new personal accounts: https://support.google.com/googleplay/android-developer/answer/14151465
- Data safety form: https://support.google.com/googleplay/android-developer/answer/10787469
- Target API requirements: https://developer.android.com/google/play/requirements/target-sdk
- Android developer verification: https://developer.android.com/developer-verification/guides
- Google API Services User Data Policy: https://developers.google.com/terms/api-services-user-data-policy
- EDRDG licence: https://www.edrdg.org/edrdg/licence.html
- Tatoeba terms: https://tatoeba.org/en/terms_of_use
