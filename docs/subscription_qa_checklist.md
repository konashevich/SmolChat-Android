# Subscription & Survival Mode Manual QA Checklist
Date: 2025-10-01
Scope: Annual subscription (on-launch-only verification) + Survival Mode ethics.

## Legend
Result columns suggestion when running manually: PASS / FAIL / NOTES.

---
## 1. Fresh Install – No Purchase
| Step | Action | Expected |
|------|--------|----------|
| 1 | Install app (clean device / uninstall first) | Launch -> Paywall shown (NOT_ENTITLED) |
| 2 | Tap Exit on paywall | App closes |
| 3 | Relaunch | Paywall again |

## 2. Purchase Flow – Online
| Step | Action | Expected |
|------|--------|----------|
| 1 | From Paywall tap Subscribe | Billing UI appears |
| 2 | Complete purchase | Return to app -> Transition to ACTIVE, routed to main flow |
| 3 | Close & relaunch (still online) | Direct to Chat / Download screen (no paywall) |

## 3. Restore Flow (Reinstall Scenario)
| Step | Action | Expected |
|------|--------|----------|
| 1 | Uninstall app | App data cleared |
| 2 | Reinstall & launch (online) | Paywall may flash briefly -> ACTIVE after billing connection sync |
| 3 | Tap Restore (if still NOT_ENTITLED after a short wait) | State becomes ACTIVE |

## 4. Offline Use During Valid Paid Period
| Step | Action | Expected |
|------|--------|----------|
| 1 | Ensure ACTIVE online | Chat accessible |
| 2 | Disable network completely | Stay in Chat, still ACTIVE locally |
| 3 | Relaunch still offline (before expiry) | Still ACTIVE |

## 5. Expiry While Offline (Simulated)
(Manual date / time change OR debug build forcing record expiry.)
| Step | Action | Expected |
|------|--------|----------|
| 1 | Make subscription ACTIVE | Baseline |
| 2 | Simulate time jump past 1 year (device clock forward by > 365d) while offline | On launch evaluate -> SURVIVAL_MODE |
| 3 | Survival banner visible | Shows explanation + Renew button |
| 4 | Tap Renew while still offline | (No billing) remains SURVIVAL_MODE |
| 5 | Re‑enable network & relaunch | Billing sync extends expiry -> ACTIVE |

## 6. Auto-Renew While User Absent
| Step | Action | Expected |
|------|--------|----------|
| 1 | Purchase subscription (ACTIVE) | Active baseline |
| 2 | Do NOT open app for > 366d (simulate by adjusting system date forward, then back) | No app activity |
| 3 | Return online & launch | Active (expiry recalculated 1y ahead) |

## 7. Clock Drift Forward Abuse Attempt
| Step | Action | Expected |
|------|--------|----------|
| 1 | ACTIVE baseline | — |
| 2 | Set device clock forward +70 days (still within paid period) | Launch -> Remains ACTIVE, clockSuspicious flag set (debug overlay) |
| 3 | Set clock back correct | Remains ACTIVE |

## 8. Clock Drift Backward Abuse Attempt
| Step | Action | Expected |
|------|--------|----------|
| 1 | ACTIVE baseline | — |
| 2 | Set clock forward +10 days, then immediately backward 10 days | Launch -> ACTIVE; if backward diff > 3 days total, clockSuspicious flagged |

## 9. Cancellation / Refund (Manual Server Simulation)
NOTE: Requires issuing refund/cancel in Play Console test environment.
| Step | Action | Expected |
|------|--------|----------|
| 1 | ACTIVE baseline | — |
| 2 | Cancel in Play Console test account | (No immediate local change) |
| 3 | Relaunch after billing connection | If still before current local expiry -> stays ACTIVE (until expiry) |
| 4 | After local expiry passes | SURVIVAL_MODE |

## 10. Survival Mode -> Renewal
| Step | Action | Expected |
|------|--------|----------|
| 1 | In SURVIVAL_MODE offline | Banner present |
| 2 | Re-enable network & tap Renew | Purchase flow; after success -> ACTIVE, banner gone |

## 11. Transition Log Inspection (Debug Overlay)
| Step | Action | Expected |
|------|--------|----------|
| 1 | Enter SURVIVAL_MODE then back to ACTIVE | Debug overlay log shows state lines, truncated to last ~50 |

## 12. Data Persistence
| Step | Action | Expected |
|------|--------|----------|
| 1 | ACTIVE; force stop app | Relaunch -> still ACTIVE |
| 2 | Clear app storage (not uninstall) | Paywall returns (record erased) |

## 13. No Model Present First Launch After Purchase
| Step | Action | Expected |
|------|--------|----------|
| 1 | Fresh install, purchase | ACTIVE then redirected to DownloadModel screen if no models installed |

## 14. Survival Mode With No Models (Edge Case)
| Step | Action | Expected |
|------|--------|----------|
| 1 | Remove all models manually | Launch with expired subscription & offline | SURVIVAL_MODE state but user must download/import model later when network returns |

---
## Notes on Ethics Messaging
Validate wording in:
- Paywall copy
- Survival banner
- Renewal prompt dialog

Ensure tone: user-first, no scare tactics, encourages support when possible.

---
## Open Manual QA Items (Track Here)
| Item | Status | Notes |
|------|--------|-------|
| Refund flow test | Pending | Need Play test profile |
| Long absence auto-renew real clock wait | Skipped | Simulated via clock change |
| Multi-device restore (2nd device) | Pending | Add test account second device |

---
## Sign-off Criteria
PASS all critical: purchase, expiry offline survival, renewal return to active, no hard lockout, clock drift soft flag only.

If any FAIL: fix before release.
