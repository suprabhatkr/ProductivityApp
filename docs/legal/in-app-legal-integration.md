# In-App Legal and Store Compliance Checklist for [APP NAME]

This file lists the legal/compliance items that should be added to the application and release process based on the current codebase.

## 1. Add a visible Legal screen in the app

Add a **Legal** section in Settings or the onboarding flow that includes:

- **Privacy Policy** link
- **Terms of Service** link
- **Last updated / effective date**
- **Publisher legal name**
- **Contact email**

For Google Play health apps, the privacy policy must also be available on a **public, non-editable URL** outside the app.

## 2. Add first-run acceptance flow

Before full use of the app, add a short first-run screen that:

1. links to the Terms and Privacy Policy,
2. asks the user to affirmatively continue, and
3. stores the accepted legal version and timestamp locally.

Recommended copy:

> By continuing, you agree to the Terms of Service and acknowledge the Privacy Policy.

## 3. Add prominent in-app disclosures before sensitive permissions

The current app uses health/wellness data, activity recognition, location, background location, notifications, alarms, and boot-time rescheduling. Google Play requires **in-app prominent disclosure** in addition to the privacy policy when access may not be obvious to users.

### A. Activity recognition disclosure

Show before requesting `ACTIVITY_RECOGNITION`.

Recommended copy:

> [APP NAME] stores activity data from your device's motion sensors to track your steps and update your step history, including while background step tracking is active.

### B. Location disclosure

Show before requesting location for run/walk tracking.

Recommended copy:

> [APP NAME] collects precise location data to record your run or walk route, calculate distance and pace, and display route maps while tracking is active.

### C. Background location disclosure

Show before requesting `ACCESS_BACKGROUND_LOCATION` or directing users to enable it.

Recommended copy:

> [APP NAME] collects location data while a run or walk is actively being tracked, including when the app is minimized or not on screen, so tracking can continue until you stop the activity.

This disclosure should:

- appear in-app before the permission prompt or settings handoff;
- clearly mention background/minimized use;
- explain the user benefit;
- not be buried only inside settings or legal text.

If background location is **not** truly required for the core promoted feature set, remove the permission from the manifest instead of trying to justify it.

### D. Notification / alarm disclosure

Before requesting notification permission or wake/alarm related features, show a short explanation.

Recommended copy:

> [APP NAME] sends notifications for active tracking, reminders, and sleep alarms you enable.

## 4. Add health-app disclaimer in store listing and app

Because the app handles health and wellness information, include this disclaimer in:

- the Google Play listing description, and
- an in-app legal/about screen.

Recommended copy:

> [APP NAME] is not a medical device and does not diagnose, treat, cure, or prevent any medical condition. Consult a healthcare professional for medical advice, diagnosis, or treatment.

## 5. Make data controls more complete inside the app

The current app has some reset/delete paths, but a production release should also provide a clear **Manage My Data** area that lets users:

- reset profile data,
- delete runs and route history,
- delete sleep records,
- delete water history,
- delete workout history,
- delete mindfulness sessions and reflections,
- clear exported replay cache files,
- clear diagnostic log files,
- optionally export user data before deletion.

If you do not add in-app deletion controls for each data category, document the exact deletion path or support process.

## 6. Decide on backups and disclose them accurately

The manifest currently enables Android backup (`android:allowBackup="true"`), and the XML backup/data extraction files are not yet tightly configured.

Before production release, choose one:

1. **Keep backups enabled** and disclose that app data may be copied during Android backup/restore/device transfer; or
2. **Restrict/disable backups** for sensitive data and update the policy accordingly.

This decision matters because the app stores health/wellness and location-related information locally.

## 7. Align Google Play Console declarations

For release, make sure the store configuration matches the app and policy:

1. **Privacy Policy URL** in Play Console
2. **Health apps declaration**
3. **Data safety section**
4. **Background location declaration**, if the permission remains
5. Any required foreground-service or permissions declarations

Do not publish the policy until the console disclosures match the actual code and UX.

## 8. Add contact and support pathways

The app and both legal documents should consistently show:

- legal entity / publisher name,
- privacy/support email,
- mailing address if required by your jurisdiction or store terms.

Use the same details everywhere: app, website, Play listing, Terms, and Privacy Policy.

## 9. Add a sharing warning for replay export

The app can export and share a run replay video. Before the user shares, add a short warning such as:

> Sharing a replay may reveal route, location, time, and activity details to the app or people you choose.

## 10. Review third-party map disclosure

The run map/replay stack loads map styles from `tiles.openfreemap.org`. If you keep that behavior in production:

- mention map/tile providers in the privacy policy,
- verify their terms/privacy documentation,
- confirm your store disclosures remain accurate,
- ensure network access and privacy expectations are consistent with the published policy.

## 11. Keep the published policy in sync with the code

If you later add any of the following, update the Terms, Privacy Policy, and Play disclosures before release:

- accounts or cloud sync,
- crash reporting,
- analytics,
- ads,
- subscriptions or payments,
- social/community features,
- support chat or ticketing,
- new sensors or health integrations,
- third-party SDKs that collect data by default.
