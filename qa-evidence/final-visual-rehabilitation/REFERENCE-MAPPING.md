# REFERENCE IMAGE MAPPING

## Reference 01 Analysis — Device Tools Screen

**Image:** `design/final-ten-references/ref-01.jpg`  
**Dimensions:** 852x1846 pixels  
**Mapped Route:** `Page.TOOLS` (Tools tab, outer index screen)

### Critical Measurements from Reference

#### Header (Top Bar)
- **Total height:** ~88px (~52-56dp equivalent at reference density)
- **Back button:** Left-aligned, standard touch target
- **Logo:** Small K mark + "knoux ai" wordmark, compact sizing
- **Pro badge:** Gold outline, rounded, "👑 Pro" text
- **Settings gear:** Right-aligned icon
- **Background:** Deep purple glass with subtle glow
- **NO DUPLICATE LOGO** - Logo appears ONLY in header, not in page content

#### Hero Section
- **Central artwork:** Premium volumetric 3D composition
  - Central glowing K mark in sphere
  - 4 floating service icons (database, people, shield, battery)
  - Holographic orbital rings
  - Neon light trails (magenta→cyan gradient)
  - Background stars/particles
- **Height:** ~480px (~35-40% of viewport)
- **Title:** "Device **Tools**" (split color: white + gradient pink)
- **Subtitle:** "Your device intelligence workspace." (muted gray)
- **Positioning:** Centered, with breathing space

#### Service Cards (6 visible)
1. **Storage Analyzer** (FEATURED card - larger, with inline art)
   - Neon cyan star badge "⭐ FEATURED"
   - 3D database cylinder stack artwork (right side)
   - "128 GB used of 256 GB" metric
   - Gradient progress bar (cyan→pink)
   - "50%" percentage indicator
   - Chevron right arrow

2-6. **Standard Service Cards** (App Manager, Privacy Sweep, Battery Diagnostics, AI Optimization, Storage Breakdown)
   - **Icon:** Left side, 3D glass-backed premium icon (~72px)
   - **Title:** Bold white, clear hierarchy
   - **Description:** 2-line gray text, readable
   - **Metric badge:** Neon pill (cyan/mint) with value
   - **Chevron:** Right arrow for navigation
   - **Card background:** Deep purple glass with glow border
   - **Spacing:** Consistent 12-16px between cards

#### Bottom Navigation
- **Height:** ~88px
- **4 tabs:** Home, Clean, Tools (active/glowing), Reports
- **Active indicator:** Bright magenta/pink glow pill
- **Icons:** Clear, good size (~24dp)
- **Labels:** Readable below icons
- **Background:** Dark purple glass

### Viewport Efficiency Analysis

**Total screen height:** 1846px  
**Distribution:**
- Status bar: ~60px (3%)
- Header: ~88px (5%)
- Hero section: ~480px (26%)
- Service cards area: ~1040px (56%)
- Bottom nav: ~88px (5%)
- Bottom padding: ~90px (5%)

**Cards visible without scrolling:** 6 cards fit comfortably  
**Root scroll required?** YES - but intentionally for a long service list  
**Essential UI cropped?** NO - all primary controls visible

### Key Observations

✅ **GOOD:**
- Header is compact and professional
- Logo appears ONLY in header (not duplicated in content)
- Premium 3D iconography throughout
- Clear visual hierarchy
- Service-specific identity per card
- Featured card has special treatment
- Metrics are compact pills, not oversized
- Bottom nav is fixed and clear

⚠️ **DESIGN INTENT:**
- This is a **service index/directory screen**, not a primary service screen
- Scrolling is EXPECTED here because it's listing many services
- The "no scrolling" rule applies to PRIMARY SERVICE SCREENS (detail views)
- Hero artwork is appropriately sized for this context

### Current Implementation Comparison

**File:** `MainActivity.kt` → `ToolIndex` composable

**Current issues:**
1. Uses simple generic `AccentButton` list - NO premium cards
2. No Featured card treatment
3. No inline 3D artwork
4. No metric badges visible on cards
5. Missing neon star badge
6. Icons might be generic placeholders vs. premium 3D versions
7. Hero might be oversized for this context

**Required changes:**
1. Design premium service card component with:
   - 3D glass-backed icon (left)
   - Title + description
   - Metric badge (neon pill)
   - Chevron arrow
   - Glow border
2. Add Featured card variant with inline artwork
3. Ensure header stays compact (already close, verify)
4. Add proper 3D hero artwork for Tools page
5. Verify bottom nav is fixed (already implemented)

---

## Reference Mapping Table

| Ref | Screen | Page Route | Status | Priority |
|-----|--------|-----------|--------|----------|
| 01 | Device Tools | `TOOLS` | ⚠️ Needs premium cards | HIGH |
| 02 | ? | TBD | Not analyzed | - |
| 03 | ? | TBD | Not analyzed | - |
| 04 | ? | TBD | Not analyzed | - |
| 05 | ? | TBD | Not analyzed | - |
| 06 | ? | TBD | Not analyzed | - |
| 07 | ? | TBD | Not analyzed | - |
| 08 | ? | TBD | Not analyzed | - |
| 09 | ? | TBD | Not analyzed | - |
| 10 | ? | TBD | Not analyzed | - |

---

## Next Steps

1. ✅ Analyze ref-01 (Device Tools) - COMPLETE
2. Load and analyze remaining 9 reference images
3. Map each to current Page routes
4. Identify reference for HOME screen
5. Identify references for detail screens vs. index screens
6. Measure header dimensions across all references
7. Identify biometric/auth reference
8. Create detailed measurement table
9. Begin implementation starting with header

---

**Status:** Reference 01 mapped successfully  
**Key finding:** This is an INDEX screen (scrolling expected), not a PRIMARY SERVICE screen  
**Action:** Continue analyzing remaining references to find PRIMARY service screen examples
