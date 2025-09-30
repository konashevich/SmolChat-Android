# Crisis AI – Subscription & Offline Survival Mode Plan
Date: 2025-09-29
Version: 1.0 (Initial Draft)
Status: Approved Concept – Pending Implementation

---
## 1. Purpose
Turn the app into a **free-to-download, annual paid subscription** product that **never locks out a user in a real, prolonged communication collapse** (nuclear war / infrastructure failure). Revenue enforcement is **best-effort when civilization signals are present**, but **survival access always wins**.

---
## 2. Business Model Summary
- Monetization: Single auto‑renewing annual subscription (USD $9.99 base – localized pricing allowed).
- No free trial. Immediate charge.
- Manual refund window: 30 days (user requests, dev may approve via Play Console).
- Enforcement: Normal while network + Google Play Billing reachable.
- Ethical Backstop: Survival Mode when verification impossible for extended periods.

---
## 3. Key Principles
| Principle | Implementation Consequence |
|-----------|----------------------------|
| Never abandon user in catastrophe | Local entitlement persistence + Survival Mode fallback |
| Minimal friction in normal times | Standard Google Play Billing purchase flow |
| Respect cancellations & refunds when online | Re-verification syncs and can revoke entitlement |
| Resist trivial cheating without harming survival | Light tamper signals; no hard lock if offline |
| Transparent messaging | Clear banners/prompts with rationale |

---
## 4. Core States
Internal entitlement states (string enum):
- `ACTIVE` – Confirmed subscription within paid/renewal period.
- `GRACE` – (Optional) Short post-expiry offline cushion (can be skipped – we may jump straight to SURVIVAL_MODE).
- `SURVIVAL_MODE` – Unlimited access due to prolonged inability to verify.
- `NOT_ENTITLED` – No valid purchase (normal paywall condition when verification possible).

---
## 5. Local Subscription Record (JSON Structure)
Stored e.g. in `SharedPreferences` key: `subscription_state.json`
```json
{
  "purchaseStartUtc": 0,                // ms epoch
  "lastKnownExpiryUtc": 0,              // ms epoch (from Play)
  "lastVerificationUtc": 0,             // ms epoch (successful server-side check)
  "purchaseToken": "",                 // Play Billing purchase token
  "entitlementState": "NOT_ENTITLED",  // one of the states above
  "survivalModeActivatedAtUtc": null,   // ms epoch or null
  "verificationDebt": false,            // true if must re-check soon
  "clockSuspicious": false,             // heuristic flag
  "systemElapsedRealtimeAtVerification": 0 // ms (monotonic)
}
```

### Notes
- Use plain JSON for resilience. Encryption optional; **must not risk unreadability in crisis**.
- Validate safely; on parse error fallback to permissive evaluation (prefer SURVIVAL_MODE over crash).

---
## 6. Time & Threshold Constants (Initial Values)
| Constant | Suggested Value | Purpose |
|----------|-----------------|---------|
| VERIFY_INTERVAL_MIN | 12 hours | Minimum interval between silent successful checks when online |
| VERIFICATION_DEBT_AFTER | 14 days | If last verification older & network OK → show gentle reminder |
| SURVIVAL_MODE_AFTER_EXPIRY_OFFLINE | 0 days | Immediately allow Survival Mode if expiry passes offline |
| OPTIONAL_GRACE_LENGTH | 0 days | Set >0 only if we want a named GRACE state before Survival Mode |
| REMIND_EXPIRED_ON_RETURN_DAYS | 7 days | Number of days to show soft renewal prompts before harder paywall (if desired) |
| CLOCK_SUSPICIOUS_DRIFT_BACK | > 3 days backward | Flag if user set clock far back |
| CLOCK_SUSPICIOUS_DRIFT_FORWARD | > 60 days forward | Flag if jumped far into future |

These can be put in a constants object (`SubscriptionPolicy`).

---
## 7. State Transition Summary (Text Diagram)
```
          (Purchase Success)
NOT_ENTITLED  ------------------>  ACTIVE
     ^                                 |
     |                                 | (Online verify: expired & not renewed)
     |                                 v
     |<---------  NOT_ENTITLED   <---  ACTIVE (true expiry)
     |
     | (Offline & expiry passed)
     +----> SURVIVAL_MODE (if cannot verify)

SURVIVAL_MODE --(Online verify & renewed)--> ACTIVE
SURVIVAL_MODE --(Online verify & truly expired)--> PROMPT USER (soft) -> (User renews) ACTIVE | (ignores) stay SURVIVAL_MODE or optionally go NOT_ENTITLED after reminder window
```

---
## 8. Event Handling Table
| Event | Online? | Action |
|-------|---------|--------|
| App Launch – no local record | Either | Show paywall (unless policy: allow limited preview offline) |
| Purchase success | Yes | Save record; set `ACTIVE`; schedule next verification |
| Silent verification success (renewed) | Yes | Update `lastKnownExpiryUtc`; keep `ACTIVE` |
| Silent verification shows canceled but still in period | Yes | Keep `ACTIVE` until expiry passes |
| Verification: expired & not renewed | Yes | If previously SURVIVAL_MODE (ethical branch) show renewal prompt; else set `NOT_ENTITLED` |
| Expiry passes while offline | No | Enter `SURVIVAL_MODE` immediately (or GRACE if enabled) |
| Return online from SURVIVAL_MODE | Yes | Re-verify; branch as above |
| Manual refund detected | Yes | Treat like expired path; same survival ethics if prolonged offline caused gap |
| Clock anomaly | Either | Set `clockSuspicious=true`; do not revoke; optionally annotate UI |
| Corrupt JSON | Either | Attempt re-verify if online; else default to SURVIVAL_MODE with warning |

---
## 9. Survival Mode UX Requirements
- Non-blocking top banner: context + gratitude + pledge request.
- Distinct palette (amber/yellow) not alarming red.
- Action button (when online & expired): “Renew Now”. Secondary: “Later”.
- Offline: no nag dialogs that block usage.
- Must NOT degrade model inference performance.

---
## 10. UI Components to Implement
| Component | Purpose |
|-----------|---------|
| PaywallScreen | Purchase messaging + Subscribe button |
| SurvivalModeBanner (Compose) | Persistent reminder + optional renew action |
| RenewalPromptDialog | Shown when connectivity restored & expired |
| DebugStatusOverlay (debug builds only) | Show raw state + timestamps |

---
## 11. Public API (Internal Kotlin Facade)
```kotlin
interface EntitlementProvider {
    fun isFeatureAccessAllowed(): Boolean
    fun currentState(): EntitlementState
    fun accessModeLabel(): String // Human label
    fun refreshIfNeeded(force: Boolean = false)
    fun beginPurchase(activity: Activity, onResult: (PurchaseOutcome) -> Unit)
}
```

`EntitlementState` sealed (or enum class) with values: ACTIVE, SURVIVAL_MODE, NOT_ENTITLED (and optional GRACE).

---
## 12. Purchase Flow (Simplified)
1. User taps Subscribe.
2. Launch Google Play Billing flow for product `crisis_ai_annual`.
3. On success: persist record, update state to ACTIVE.
4. On failure/cancel: remain in NOT_ENTITLED.

---
## 13. Verification Logic Pseudocode
```kotlin
fun evaluate(nowUtc: Long) {
  if (!hasLocalPurchase()) { state = NOT_ENTITLED; return }

  if (nowUtc < lastKnownExpiryUtc) {
     state = ACTIVE
     maybeScheduleSilentVerify()
     return
  }

  if (isOnline && playBillingReachable) {
     val server = queryPlay()
     if (server.active) { updateExpiry(server.expiry); state = ACTIVE }
     else { state = if (state == SURVIVAL_MODE) SURVIVAL_MODE else NOT_ENTITLED }
  } else { // offline or unreachable
     // Ethical fallback
     enterSurvivalModeIfNotAlready(nowUtc)
  }
}
```

---
## 14. Tamper / Clock Handling
- Compare wall clock progress vs `SystemClock.elapsedRealtime()` at each launch.
- If negative drift beyond threshold → `clockSuspicious = true`.
- UI: small, non-intrusive note in debug overlay; production may ignore.
- No forced lockout.

---
## 15. Testing Matrix
| Scenario | Expectation |
|----------|-------------|
| Fresh install online, no sub | Paywall |
| Purchase, relaunch | Direct access (ACTIVE) |
| Force offline, advance expiry (simulated debug) | SURVIVAL_MODE banner |
| Return online post-expiry | Renewal prompt |
| Manual refund (simulate via flag) | Next verify → NOT_ENTITLED or SURVIVAL_MODE (if offline) |
| Corrupt JSON | Attempt repair; offline → SURVIVAL_MODE |
| Clock back 10 days | `clockSuspicious=true`; still entitled |
| No network for 400 days after purchase | SURVIVAL_MODE all along; upon return online, prompt renewal |

---
## 16. Implementation To-Do Checklist
- [ ] Add Google Play Billing dependency in `app/build.gradle.kts`.
- [ ] Create `subscription` package with `SubscriptionManager` implementing API.
- [ ] Define `EntitlementState` enum + constants in `SubscriptionPolicy`.
- [ ] Add JSON persistence helper (read/write safely).
- [ ] Integrate manager into Koin module.
- [ ] Add gating in `MainActivity` prior to redirect (decide paywall vs chat).
- [ ] Implement PaywallScreen (Compose or Activity) with messaging for manual refund policy.
- [ ] Implement SurvivalModeBanner (Compose) to show in chat UI root.
- [ ] Add silent verification scheduler (on app start + periodic WorkManager or simple timestamp check).
- [ ] Add connectivity + play billing reachability checks.
- [ ] Add debug overlay (only in debug). 
- [ ] Add manual “Renew” CTA when returning online post-expiry.
- [ ] Logging (local only) for state transitions.
- [ ] QA all test matrix scenarios.
- [ ] Prepare Store Listing wording (refund instructions; survival philosophy optional but could inspire trust).

---
## 17. Store Listing / Policy Text Suggestions
Short line: "Annual subscription. 30-day money-back guarantee on request. If global connectivity is lost, Crisis AI remains available offline—renew when networks return." (Verify this doesn’t conflict with Play policies; avoid promising automatic refunds.)

---
## 18. Security & Privacy Notes
- No remote server dependency = resilient.
- Keep only minimal subscription metadata locally.
- Do not log user chat content for entitlement decisions.
- If adding analytics later, ensure offline survival path never stalls on telemetry.

---
## 19. Future Enhancements (Backlog)
| Idea | Value |
|------|-------|
| Server-side receipt validation (optional) | Stronger fraud deterrence (low priority; conflicts slightly with pure offline mission) |
| Offer discount renewal nudge after long SURVIVAL_MODE | Goodwill conversion |
| Local cryptographic signature of purchase record | Mild tamper hardening |
| Multi-device acknowledgement banner | Transparency |
| Optional user donation (non-sub) | Additional support channel |

---
## 20. Ethical Statement (Embed in Repo)
"Crisis AI prioritizes human survival over revenue. In prolonged infrastructure failure, subscription checks relax and core capabilities remain available. We rely on user honesty to renew once stability returns." 

---
## 21. Change Log (Append Below As Iterations Occur)
```
## 1.0 – 2025-09-29
- Initial documented architecture & policy.
```

---
## 22. Acceptance Criteria for Completion
- Code path never blocks model usage solely due to inability to reach Google for > 0 days after expiry.
- Returning online after expiry always results in a clear, respectful prompt to renew.
- All test matrix scenarios pass manually.
- No crashes when JSON missing or corrupt.
- Paywall unreachable when SURVIVAL_MODE active.

---
## 23. Implementation Order Recommendation
1. Data model & persistence.
2. State evaluation function (pure logic) + unit tests (if added later).
3. Billing integration (query + purchase).
4. UI gating + paywall.
5. Survival banner + transitions.
6. Edge case handling (clock, corruption).
7. Polish copy & logging.
8. QA matrix.
9. Release internal test.

---
## 24. Minimal Copy (Draft)
Paywall line: "Full offline AI assistance. Annual subscription $9.99. 30-day money-back guarantee (email support)."  
Survival banner: "Survival Mode: Unable to verify subscription. Full access preserved. Renew when connectivity returns."  
Return prompt: "Connectivity restored. Please renew to continue supporting Crisis AI."  

---
## 25. Risks & Mitigations
| Risk | Mitigation |
|------|------------|
| Abuse via perpetual airplane mode | Accept ethically; rely on goodwill prompts |
| User confusion about refund process | Clear wording: manual refund on request within 30 days |
| Corrupt local state causes lockout | Fallback to SURVIVAL_MODE |
| Overly aggressive renewal nag during disaster | Nag logic suppressed while offline |
| Development drift (future maintainer tightens policy) | Keep this document; code comments referencing ethic |

---
End of document.
