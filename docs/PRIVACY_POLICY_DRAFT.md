# Crisis AI (SmolChat Android) – Draft Privacy Policy (Test Version)

Last updated: 01 Oct 2025
Status: DRAFT FOR TESTING (not final). You may use this temporarily in Google Play Internal / Closed testing. Replace or refine before Production.

## 1. Summary
Crisis AI does **not collect, transmit, or store any personal data on external servers**. All AI processing runs fully **offline on your device** (local model). We do not run a backend for chat content, user profiles, tracking, or analytics.

## 2. Data We Do NOT Collect
We do NOT collect:
- Personal identifiers (name, email, phone number)
- Location data
- Contact lists
- Messages / prompts you type
- Device identifiers (Android ID, Advertising ID) for tracking
- Usage analytics or crash reports (no third‑party analytics SDK)

## 3. Local-Only Operation
All model inference and conversation context stay on the device. Conversation history (if you save it) is stored only in local app storage controlled by the OS sandbox. If you uninstall the app, that local data is removed.

## 4. Subscription / Billing
Payments are handled solely by **Google Play Billing**. We do not receive or store your full payment method details. Google provides us only the minimum subscription status signals (e.g. active / expired) needed to confirm access.

## 5. Network Use
The core AI chat works offline. The app may access the network only for:
- Verifying subscription status with Google Play (when connectivity exists)
- Optional model downloads or updates (if you choose to fetch or update a model – future feature)
No other calls (telemetry, tracking, analytics) are intentionally performed.

## 6. Children’s Privacy
The app is not directed to children under 13. We do not knowingly collect any data from children. Since no personal data is collected at all, there is no profile creation.

## 7. Data Sharing
We do not share any personal data because we do not collect it.

## 8. Security
Because no remote storage of your content occurs, exposure risk is limited to your own device. Protect your device with standard OS security (screen lock, encryption). We recommend not pasting extremely sensitive secrets into any AI system, even offline.

## 9. User Controls
- Uninstalling the app removes locally stored chat history and settings.
- Clearing app storage (Android Settings > Apps > Crisis AI > Storage & cache > Clear storage) wipes local data.

## 10. Offline Survival Scenario Philosophy
The app is purposely designed to remain functional during extended network outages. Lack of connectivity will not disable previously entitled access. Subscription re-validation occurs only when a network is available again.

## 11. Third-Party Components
- Google Play Billing Library (for subscription handling)
- Android / Jetpack libraries
These libraries may internally contact Google services strictly to process billing. We do not intercept or extend that data flow.

## 12. Internationalization / Localization
Current draft is English only. Future localized text will not alter the privacy posture.

## 13. Changes to This Draft
This is a **draft** and may be updated before public (production) release. A final version will include a proper hosting URL and change log of revisions.

## 14. Contact
Draft stage: No dedicated support site. You may open an issue in the public repository GitHub Issues for privacy-related questions.

Repository: https://github.com/konashevich/SmolChat-Android

---
DRAFT ONLY – NOT LEGAL ADVICE. Replace with a finalized reviewed version before production launch.
