# Crisis AI – Subscription & Offline Survival Mode Plan
Date: 2025-10-01
Version: 1.1 (Simplified Verification – On Launch Only)
Status: Updated – IMPLEMENTATION ALIGNMENT

---
## 1. Purpose
Free-to-download, annual paid subscription at $9.99 that never blocks user in prolonged infrastructure collapse. Survival ethics first.

---
## 2. Business Model Summary
- Single auto-renewing annual subscription.
- No free trial.
- Manual refund window: 30 days (developer mediated).
- No background billing polling; verification only when app is opened (billing connection) or user purchases.
- Offline expiry → Survival Mode immediately.

---
## 3. Key Principles (unchanged)
| Principle | Implementation Consequence |
|-----------|----------------------------|
| Never abandon user in catastrophe | Local record + Survival Mode fallback |
| Minimal friction in normal times | Standard Play Billing purchase flow |
| Respect cancellations & refunds when online | Sync on launch updates state |
| Resist trivial cheating without harming survival | Light clock heuristics only |
| Transparent messaging | Clear banner/prompt copy |

---
## 4. Core States
ACTIVE, SURVIVAL_MODE, NOT_ENTITLED (GRACE omitted; treat as direct SURVIVAL_MODE if offline at/after expiry).

---
## 5. Local Subscription Record (JSON Structure)
```json
{
  "purchaseStartUtc": 0,
  "lastKnownExpiryUtc": 0,
  "lastVerificationUtc": 0,
  "purchaseToken": "",
  "entitlementState": "NOT_ENTITLED",
  "survivalModeActivatedAtUtc": null,
  "clockSuspicious": false,
  "systemElapsedRealtimeAtVerification": 0,
  "autoRenewing": true
}
```
Removed: verificationDebt (no periodic schedule now).

---
## 6. Time & Threshold Constants (Revised)
| Constant | Value | Purpose |
|----------|-------|---------|
| SURVIVAL_MODE_AFTER_EXPIRY_OFFLINE | 0 days | Immediate survival access |
| REMIND_EXPIRED_ON_RETURN_DAYS | 7 days | Optional soft prompt window |
| CLOCK_SUSPICIOUS_DRIFT_BACK | > 3 days backward | Flag only |
| CLOCK_SUSPICIOUS_DRIFT_FORWARD | > 60 days forward | Flag only |
| YEAR_MS | 365 days | Local expiry calc |

Removed: VERIFY_INTERVAL_MIN (no background schedule).

---
## 7. Verification Model (Simplified)
- Attempt silent sync ONLY when:
  1. App process starts & billing connection established.
  2. User completes a purchase flow.
  3. App explicitly calls refresh(force = true) (manual developer trigger e.g. via settings screen if ever added).
- No WorkManager / no periodic background job.
- If never opened around renewal time, Google auto-renews server-side; expiry extended locally next time user opens while online.

---
## 8. Event Handling (Adjusted)
Same as prior table except periodic silent verification events removed.

---
## 9. Survival Mode UX
Unchanged intent.

---
## 10. UI Components
Same list; no scheduler UI, no verification debt indicators. (Legacy VerificationDebtBar removed.)

---
## 11. Public API
Unchanged interface. Behavior: refreshIfNeeded(force=false) only re-evaluates local record; force=true also queries billing.

---
## 12. Purchase Flow
Unchanged.

---
## 13. Verification Logic (Updated Pseudocode)
```kotlin
onAppStart {
  evaluateLocal()
  connectBilling { syncFromBilling() }
}

fun refresh(force: Boolean) {
  evaluateLocal()
  if (force) syncFromBilling()
}
```

---
## 14. Tamper / Clock Handling
Unchanged, still soft.

---
## 15. Testing Matrix (Adjusted)
Add cases:
| Scenario | Expectation |
|----------|-------------|
| Annual auto-renew happened while user absent months | On next launch online → expiry extended, ACTIVE |
| Expiry passed while app unopened & offline | On launch offline → SURVIVAL_MODE |

---
## 16. Implementation To-Do Checklist (Updated)
- [x] Add Google Play Billing dependency (verify present).
- [x] Create subscription package & manager.
- [x] EntitlementState + constants.
- [x] JSON persistence helper.
- [x] Integrate into DI (Koin) (verify usage site pending).
- [x] Gate main entry (paywall vs chat).
- [x] PaywallScreen – functional copy placeholder.
- [x] SurvivalModeBanner – integrated.
- [x] Renewal prompt dialog – integrated.
- [x] Logging transitions.
- [x] Removed periodic scheduler & verificationDebt.
- [ ] Store listing wording (external – not in repo).
- [x] QA scenarios: automated unit tests (core transitions, drift, survival); manual checklist in `subscription_qa_checklist.md`.

---
## 17. Rationale for Simplification
- Eliminates background network behavior aligning with privacy & user expectation of a crisis tool.
- Reduces code paths (no stale interval logic, no debt state).
- Auto-renew remains reliable because Play handles charge server-side.
- Survival priority preserved with immediate offline fallback.

---
## 18. Future Optional Enhancements
- Manual “Check Subscription Now” debug button.
- Lightweight test harness to simulate expiry & offline.
- Analytics-free internal counters (count launches since last sync) if ever needed – currently omitted.