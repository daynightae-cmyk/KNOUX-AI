# KNOUX AI — VISUAL REHABILITATION BASELINE

## Repository State

**Location:** `D:\KNOUX-AI`  
**Branch:** `main`  
**HEAD:** `fd07024`  
**Status:** Clean working directory with untracked files  
**Date:** 2026-11-12  

### Recent Commits
```
* fd07024 (HEAD -> main) docs(evidence): record the final SHA and the residual owner actions
* 99e351f feat(ui): rebuild the ten approved reference screens and the identity gateway
* 7f4ce64 feat(navigation): Refactor page routing to granular detail screens and enhance dashboard reveal animation
```

### Untracked Files
- `app/src/androidTest/java/com/knoux/ai/ServiceWorldsTest.kt`
- `app/src/main/java/com/knoux/ai/ServiceWorlds.kt`
- `qa-evidence/design-closure-final/`
- `qa-evidence/service-worlds-20261111/`

### Disk Space
Drive D: available space verification pending

### Build Status
Last successful build: Pass 6 (2026-11-11)  
APK: `app/build/outputs/apk/debug/app-debug.apk`

## Mission Parameters

**CRITICAL PRIORITY:** Owner has rejected current visual quality  
**MODE:** Implementation / Build / Runtime Verification  
**VIDEO REFERENCE:** `C:\Users\day night\Downloads\54772 (1).mp4` (57 MB, ~119 seconds)

### Owner's Core Rejections

1. **Oversized Components**
   - Header too visually dominant
   - Central KNOUX artwork consumes excessive space
   - Service cards pushed below fold

2. **Excessive Scrolling**
   - Primary screens require page-level vertical scrolling
   - Essential controls extend beyond viewport
   - Decorative areas consume functional space

3. **Generic Iconography**
   - Flat, pale icon appearance
   - Generic white placeholders
   - Insufficient premium styling

4. **Biometric Quality**
   - Fingerprint resembles concentric circles, not realistic ridges
   - Lacks anatomical convincing geometry
   - Not production-grade visual quality

5. **Authentication Experience**
   - Large Bottom Sheets interrupt workflow
   - Missing authentic Google/Passkey branding
   - Generic person icon instead of proper Google identity

6. **Repeated Layouts**
   - Generic card templates across services
   - Lack of service-specific visual identity
   - Insufficient distinction between sections

## Current Implementation Inventory

### Source Files (Primary UI)
- `MainActivity.kt` - 470 lines, scaffold and navigation
- `HomeScreen.kt` - Home dashboard with service cards
- `DetailPreview.kt` - Interior detail screens (truncated at 484 lines)
- `ReferenceScreen.kt` - Reference outer screens
- `KnouxSplash.kt` - Brand signature animation
- `KnouxComponents.kt` - Shared UI components (truncated at 80 lines)
- `PreviewData.kt` - Page definitions and fixtures
- `DetailPreviewData.kt` - Detail screen data
- `NeonArt.kt` - Visual effects
- `PremiumArt.kt` - Premium visual elements
- `SplashAtmosphere.kt` - Splash background effects
- `SplashChime.kt` - Audio feedback
- `ServiceWorlds.kt` - New untracked file (needs inspection)

### Recent Changes (This Session)
1. Added splash→dashboard fluid transition (scale 0.95→1.0, 520ms)
2. Changed home cards to open detail screens directly:
   - Clean → CLEAN_CENTER ✓
   - Duplicates → DUPLICATE_REVIEW (was DUPLICATES)
   - Storage → STORAGE_BREAKDOWN (was STORAGE)
   - Privacy → PRIVACY_INSPECTOR (was PRIVACY)
   - Battery → BATTERY_DIAGNOSTICS (was BATTERY)
   - Optimize → AI_RECOMMENDATIONS (was OPTIMIZE)
3. Enhanced NavHost transitions with scaleIn/scaleOut
4. Deep Scan button → SCAN_SUMMARY (was SCAN)

### Known Visual Assets
- `design/brand/knoux-logo-owner.png` - Official logo (1125x1125)
- `design/final-ten-references/ref-01.jpg` through `ref-10.jpg`
- `design/reference/*.png` - 10 reference screens
- Sample app icons for Privacy/Apps screens

## Mandatory Requirements

### 1. FIXED PRIMARY VIEWPORT
- No page-level vertical scrolling on primary service screens
- Essential UI must fit in available viewport
- Use BoxWithConstraints, weight-based allocation
- Permitted: interior list scrolling in bounded panels

### 2. COMPACT FIXED HEADER
- Target height: 52-56dp content
- Logo mark: 28-36dp
- Touch targets: ≥48dp
- ONE canonical header per screen
- No repeated logo in page content

### 3. REALISTIC FINGERPRINT
- Natural flowing ridge lines
- Organic whorl/loop structure
- NOT concentric circles
- Premium biometric sensor appearance
- Multiple animation states (IDLE, AUTHENTICATING, SUCCESS, FAILED)

### 4. PROFESSIONAL AUTHENTICATION
- Correct Google identity branding
- Recognizable Passkey iconography
- Clear hierarchy
- No oversized Bottom Sheets as primary flow
- Real BiometricPrompt integration

### 5. PREMIUM ICONOGRAPHY
- Rich violet/cyan/magenta palette
- Glass-backed compositions
- Strong contrast
- Service-specific identity
- NO generic white placeholders for primary icons

### 6. SERVICE-SPECIFIC DESIGN
- Distinctive hero per service
- Unique accent and geometry
- Clear objectives
- Compact metrics
- Visible CTAs

## Execution Plan

**PASS 0:** ✓ Reality audit (this document)  
**PASS 1:** Video forensics and defect matrix  
**PASS 2:** Compact global header  
**PASS 3:** Fixed-viewport architecture  
**PASS 4:** Home visual hierarchy  
**PASS 5:** Premium iconography  
**PASS 6:** Realistic biometric + professional Login  
**PASS 7:** Service-specific reconstruction  
**PASS 8:** Bottom Sheets and interactions  
**PASS 9:** Motion, accessibility, RTL  
**PASS 10:** Emulator build and screenshot capture  
**PASS 11:** Visual comparison and regression correction  
**PASS 12:** Evidence-backed delivery  

## Safety Guarantees

✓ Working on `D:\KNOUX-AI` (authorized PC repository)  
✓ NOT touching `/root/workspace/KNOUX-AI` (mobile environment)  
✓ NOT controlling owner's physical phone via ADB  
✓ Using local Android emulator for QA  
✓ Preserving all unique assets and changes  
✓ No destructive Git operations  
✓ No rebuild from scratch  

## Next Steps

1. Extract and analyze video frames from `54772 (1).mp4`
2. Create VIDEO-DEFECT-MATRIX.md with timestamps
3. Map references to current implementation
4. Begin systematic visual rehabilitation

---

**Status:** Baseline established, ready for video analysis  
**Blockers:** None identified  
**Risk:** None - all unique work preserved
