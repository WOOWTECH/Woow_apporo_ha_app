# iOS 版 App 開發交接文件（Apporo / Simon SmartHome）

**版本**：2026-08-10
**Android 版對照**：v2026.8.3-alpha7（已完成部署與功能驗收）
**交接對象**：iOS 工程師 / Xcode 建置者

---

## 一句話說明要做什麼

把 [`home-assistant/iOS`](https://github.com/home-assistant/iOS)（Home Assistant iOS Companion 官方 App）**fork 成兩個獨立 iOS 品牌 repo**，功能完全等同 Android 版的 apporo/simon，
只換視覺識別、Bundle ID 與後端指向。**Xcode 15+ / macOS 14+ 必備**（iOS App 無法在 Linux 建置）。

---

## 你不用重新決定的事（Android session 已定案，iOS 直接沿用）

這 8 個決策 2026-08-10 已與需求方 grill 完成，iOS 直接繼承：

| # | 項目 | apporo | simon |
|---|---|---|---|
| 1 | App 顯示名稱 | `Apporo SmartHome` | `Simon SmartHome` |
| 2 | 品牌主色 | `#8B6B24`（深褐金，白字對比 4.88:1 過 AA）**不是**品牌方 `#C49E53`—— 見 ADR-0001 | `#0060A6`（藍，與 icon 內建色一致） |
| 3 | 品牌 HA 伺服器網域 | `aiot.apporo.io` | `aiot.simon.io` |
| 4 | URL scheme | `apporohome://` | `simonhome://` |
| 5 | 主 logo 檔 | 在 [tools/brand/assets/apporo-logo-full.png](../tools/brand/assets/apporo-logo-full.png) （鳥形 + wordmark） | 在 simon repo 的 [tools/brand/assets/simon-icon.png](https://github.com/WOOWTECH/Woow_simon_ha_app/blob/main/tools/brand/assets/simon-icon.png)（藍底 SmnI） |
| 6 | Launcher icon 底色 | 白 | 藍（`#0060A6`，與 SVG 內建色一致） |
| 7 | Firebase Push | **不做**（Android minimal 版關 FCM；iOS 對應是 APNs，先不設） | 同 |
| 8 | 後端資源準備狀態 | Keystore / assetlinks / 真 HA server 都**未準備** | 同 |

**「Home Assistant」→ 品牌 SmartHome 全文替換** 已在 Android strings.xml 完成 85 處替換；iOS `Localizable.strings` 需做同樣替換。

---

## iOS 特有、你必須新做的決策

### 1. Bundle Identifier（永久性決定，等同 Android APPLICATION_ID）

Android 已用：
- `com.apporo.home.minimal.debug`（debug 有 `.debug` 尾碼；release 會是 `com.apporo.home`）
- `com.simon.home.minimal.debug` / `com.simon.home`

iOS 建議與 Android release 版對齊：
- **apporo** iOS Bundle ID → 建議 `com.apporo.home` 或 `io.apporo.home`
- **simon** iOS Bundle ID → 建議 `com.simon.home` 或 `io.simon.home`

**⚠ 上架 App Store 後永遠不能改**。跟需求方確認 domain 慣例：apporo/simon 官網用 `.io` 或 `.com`。

### 2. Apple Developer 帳號 / Team ID

每個品牌需要一個 Apple Developer Program 帳號（$99/年）：
- 若品牌方（apporo Inc. / simon Inc.）自己有 Apple Team，用他們的 Team ID
- 若沒有，需要品牌方申請（審核 1-2 週）
- **絕對不要**用 WOOWTECH 的 Team 上架其他品牌 App —— 上架者顯示為 WOOWTECH，品牌方要收回會很麻煩

### 3. APNs（Apple Push Notification service）

iOS 沒有 Firebase FCM，改用 APNs：
- 每個品牌需要一支 `.p8` Auth Key（在 Apple Developer 後台生成，一次生成之後**只能下載一次**，弄丟就要重新生成）
- HA 官方支援 iOS APNs via `mobile_app` integration 的 `push_token` 欄位
- HA 對 iOS 送通知走 Apple 官方 APNs endpoint（不用自架推播 server）

**若先不做**（跟 Android Q5 一樣延後）：iOS App 可以完全不設 APNs 建置，跟 Android minimal flavor 對應。

### 4. Universal Links vs Custom URL Scheme

iOS 有兩種 deep link 機制，兩個都要設定：

- **Custom URL Scheme**（跟 Android URL scheme 對應）：
  - 定義在 `Info.plist` → `CFBundleURLTypes`
  - apporo: `apporohome://` / simon: `simonhome://`
  - 這是**必備**，App 內部 OAuth callback 依賴它
- **Universal Links**（跟 Android App Links 對應）：
  - 在品牌網域根目錄放 `/.well-known/apple-app-site-association` JSON 檔（跟 Android assetlinks.json 對照）
  - iOS 從 Safari 點 `https://aiot.apporo.io/xxx` 會直接開 App（不跳 Safari）
  - **暫緩**：需要 aiot.apporo.io 網域先架起來，跟 Android assetlinks 是同樣的延後項

### 5. Xcode 建置環境

- Xcode 15.0+
- macOS Sonoma 14+（Xcode 不能跑 Linux）
- iOS Deployment Target: 建議跟上游 home-assistant/iOS 一致（目前 iOS 16+）
- Swift 5.9+

---

## 建議的 repo 拓撲（跟 Android 完全平行）

```
home-assistant/iOS                       (上游)
  ↓ fork 一次
WOOWTECH/woow_ha_ios                     (woowtech 白牌基礎版，做 Woow 專屬修正)
  ↓ seed 兩次（保留完整 git 歷史）
WOOWTECH/Woow_apporo_ha_ios              (apporo 版)
WOOWTECH/Woow_simon_ha_ios               (simon 版)
```

**建議命名**沿用 Android 的：`Woow_<brand>_ha_ios`。

---

## Android → iOS 檔案對照表（rebrand 需要改的東西）

| Android 檔案 | iOS 對應 | 說明 |
|---|---|---|
| `AndroidManifest.xml` label / applicationId | `Info.plist` → `CFBundleDisplayName` / Bundle ID | App 顯示名稱 |
| `strings.xml` (values/, values-zh-rTW/) | `Localizable.strings` (Base.lproj/, zh-Hant.lproj/) | 所有 UI 文字 |
| `strings.xml` `app_name` | `Info.plist` → `CFBundleDisplayName` | 桌面顯示的名稱 |
| `strings.xml` `Home Assistant` → 品牌名 全文替換 | `Localizable.strings` 同樣替換 | iOS 上游檔案 |
| `colors.xml` `colorPrimary` / `colorLauncherBackground` | `Assets.xcassets/AccentColor.colorset/Contents.json` + `Colors.xcassets/*` | 主題色 |
| `mipmap-*/ic_launcher.png` | `Assets.xcassets/AppIcon.appiconset/*` | App icon（20pt ~ 1024pt 共 15 種尺寸） |
| `drawable/app_icon_launch.png` + `themes.xml` splash | `LaunchScreen.storyboard` 或 `Assets.xcassets/LaunchImage.imageset/` | 啟動畫面 |
| `drawable/ic_<brand>_branding.png` | `Assets.xcassets/BrandLogo.imageset/*` | 品牌大 logo（onboarding 用） |
| `AndroidManifest.xml` `<data android:scheme="apporohome"/>` | `Info.plist` → `CFBundleURLTypes` → `CFBundleURLSchemes: [apporohome]` | URL scheme |
| `manifest applicationId ".provider"` FileProvider | iOS 沒有直接對應（用 UIDocumentPicker） | — |
| `AndroidManifest.xml` `<meta-data android:name="firebase_..."/>` | `GoogleService-Info.plist`（如果啟 FCM），或不用（純 APNs） | 推播設定 |
| `mock-google-services.json` for CI | 用 XCode Auto-signing debug provisioning | 建置環境 |
| `common/data/authentication/impl/AuthenticationService.kt` CLIENT_ID | `Sources/Shared/API/Authentication/*.swift` client_id 常數 | OAuth client_id URL |

---

## 已產出的 artifacts iOS 可以直接複用

### 1. Logo 向量檔（Xcode 直接吃 SVG 或先轉 PDF）

- **apporo full logo (含 wordmark)**：[Woow_apporo_ha_app/tools/brand/assets/apporo-logo-with-wordmark.svg](../tools/brand/assets/apporo-logo-with-wordmark.svg) — 原始 1024×1024 向量
- **apporo mark-only**（僅鳥形）：[Woow_apporo_ha_app/tools/brand/assets/apporo-mark-source.svg](../tools/brand/assets/apporo-mark-source.svg) — viewBox `160 40 704 640`
- **simon icon**：[Woow_simon_ha_app/tools/brand/assets/simon-icon.svg](https://github.com/WOOWTECH/Woow_simon_ha_app/blob/main/tools/brand/assets/simon-icon.svg) — 自帶藍底圓角

Xcode `AppIcon.appiconset` 需要生成 15+ 尺寸（可用 [App Icon Generator](https://appicon.co/) 或 Sketch/Figma 一鍵匯出）。

### 2. OAuth client_id 頁（跨平台可共用）

Android 已為 OAuth 建 GitHub Pages 頁面：
- https://woowtech.github.io/Woow_apporo_ha_app/android
- https://woowtech.github.io/Woow_simon_ha_app/android

**iOS 需要另外建 iOS 版**（因為 redirect_uri scheme 不同）：
- 在同一個 repo 加 `docs/ios/index.html`，裡面：
  ```html
  <link rel="redirect_uri" href="apporohome://auth-callback">
  ```
- 給 iOS 版 App 的 OAuth CLIENT_ID 用 `https://woowtech.github.io/Woow_apporo_ha_app/ios`

⚠ 若 iOS App 走跟 Android **同樣的 URL scheme**（`apporohome://`），可以直接用 Android 版那頁：
`https://woowtech.github.io/Woow_apporo_ha_app/android`

**建議走同 scheme**（省一頁維護），iOS/Android 只是 platform 不同，URL scheme 用同一組沒衝突。

### 3. 品牌參數設定檔（可以照抄結構）

Android 用 `.conf` 檔記錄品牌參數（[apporo.conf](../tools/brand/apporo.conf)）：
```
BRAND_ID="apporo"
APP_NAME="Apporo SmartHome"
APPLICATION_ID="com.apporo.home"
BRAND_HOST="aiot.apporo.io"
PRIMARY_COLOR="#8B6B24"
URL_SCHEME="apporohome"
```

iOS 建議做等效的 `.xcconfig` 檔（Xcode 原生機制）：
```
// apporo.xcconfig
PRODUCT_BUNDLE_IDENTIFIER = com.apporo.home
PRODUCT_NAME = Apporo SmartHome
BRAND_HOST = aiot.apporo.io
PRIMARY_COLOR_HEX = 8B6B24
URL_SCHEME = apporohome
```
在 target 的 Build Settings 引用即可。

### 4. 已 grill 完成的架構決策文件

- [CONTEXT.md](../CONTEXT.md) — 白牌相關詞彙表（Brand / Upstream / Rebrand / Placeholder logo）
- [docs/adr/0001-apporo-primary-color.md](adr/0001-apporo-primary-color.md) — 為何 apporo 主色是 `#8B6B24` 而非品牌方 `#C49E53`（WCAG 對比理由）

---

## Android 血淚教訓 iOS 也要注意

### 1. 「Home Assistant」字樣全文替換
- Android 找到 **85 處**（en + zh-rTW 各一份 strings.xml）
- iOS `Localizable.strings` 幾乎肯定也有類似規模，一開始就用 sed 全替換掉
- ⚠ 保留技術性字樣（`homeassistant://` scheme、integration 名稱、開源 attribution 等），只換品牌文案部分

### 2. Icon 安全區
- Android 有 adaptive icon 66/108 safe area
- iOS 沒這問題（icon 是 fully-designed 圖）—— 但**桌面 icon 必須是 1024×1024 PNG，不能有 alpha channel**，否則 App Store 會拒審

### 3. OAuth client_id 相容性
- Android 一開始踩到「Invalid redirect URI」— 因為 client_id URL 指到 `https://home-assistant.io/android` 但 redirect_uri 已改成品牌 scheme
- iOS OAuth 流程一樣，**一定要**設 `CLIENT_ID = https://woowtech.github.io/Woow_<brand>_ha_app/android` 或建 iOS 專屬頁

### 4. 上游 pre-existing bug
- Android 修過一個 upstream 的 `vectorResource(PNG)` crash（LinkActivity.kt）
- iOS 上游有沒有類似 asset-mismatch bug 未知，建議 iOS dev 用 Xcode Analyze + 完整 UI 冒煙測試找一輪

### 5. Bundle 首字母大小寫
- iOS Bundle ID **大小寫敏感**，全部小寫（`com.apporo.home` 不是 `com.Apporo.Home`）
- Bundle Display Name 是**顯示用**，大小寫隨意（`Apporo SmartHome`）

---

## 上架流程差異（Play Store vs App Store）

| 項目 | Google Play（Android 已知） | Apple App Store（iOS 需知） |
|---|---|---|
| 上架費 | $25 一次 | $99/年 |
| 審核 | 通常 <48h、自動+人工 | 通常 24-72h、100% 人工 |
| 上架 debug 版 | 可（internal test track） | 可（TestFlight，需 review 才能對外） |
| 上架後改 Bundle ID | 不可 | 不可 |
| 白牌被拒常見原因 | 侵權投訴後才處理 | 上架時就審 trademark，logo 太像官方 HA app 會被拒 |
| Push 前置 | Firebase project | APNs Auth Key |
| Deep link 驗證 | assetlinks.json | apple-app-site-association |

**特別注意**：Apple 對「跟已上架 App 高度相似」很敏感。Home Assistant Companion 已在 App Store 上架，iOS 白牌版一定要：
- 品牌 logo 與 HA 官方 logo **視覺明顯不同**（apporo 鳥形、simon SmnI 這兩個都夠不同，OK）
- App 描述文案不能直接複製 HA Companion（要重寫，強調品牌方特色）
- 若被 Apple 質疑侵權，需備妥「授權於 Apache 2.0 開源、修改後獨立品牌」的說明

---

## 建議的 iOS 開發啟動順序

1. **確認 Apple Developer 帳號**（品牌方是否已有 / 是否要 WOOWTECH 幫忙申請）
2. **決定 Bundle ID**（永久性，跟 Android release ID 對齊）
3. **Fork 上游**：`home-assistant/iOS` → `WOOWTECH/woow_ha_ios`
4. **在 macOS + Xcode 打開**、能編譯出 unbranded IPA
5. **從 upstream/main 種入品牌 repo**：`WOOWTECH/Woow_apporo_ha_ios` / `WOOWTECH/Woow_simon_ha_ios`
6. **寫 rebrand 腳本**（iOS 版，跟 Android `rebrand.sh` 對應）：
   - sed 換 `Localizable.strings` 內品牌字樣
   - swap Assets.xcassets/AppIcon.appiconset
   - 改 Info.plist `CFBundleDisplayName`, `CFBundleURLTypes`
   - 改 xcconfig 內 Bundle ID
7. **TestFlight 內部測試** → 給需求方測 → App Store 上架

---

## 現在有的實質交付物

- ✅ [Woow_apporo_ha_app](https://github.com/WOOWTECH/Woow_apporo_ha_app) — Android v2026.8.3-alpha7（可裝機測試）
- ✅ [Woow_simon_ha_app](https://github.com/WOOWTECH/Woow_simon_ha_app) — Android v2026.8.3-alpha7（可裝機測試）
- ✅ CONTEXT.md 詞彙表（兩個 repo 各一份）
- ✅ apporo 主色決策 ADR-0001
- ✅ Logo 向量檔（SVG + PNG）
- ✅ OAuth client_id 頁（GitHub Pages 已上線）
- ✅ 8 大功能分類的靜態 + 互動驗證報告

以上 Android 版所有 asset 與決策，iOS dev 拿到就可以直接開工，不用再 grill。

---

## 問題排除聯繫路徑

- Android 相關疑問 → 直接看 [Woow_apporo_ha_app](https://github.com/WOOWTECH/Woow_apporo_ha_app) 的 commit log 有詳細 rationale
- 品牌決策原因 → 見 `CONTEXT.md` + `docs/adr/`
- iOS 上游本身問題 → [home-assistant/iOS issues](https://github.com/home-assistant/iOS/issues)
- Apple Developer 問題 → https://developer.apple.com/support/
- OAuth Invalid redirect URI（若 iOS 也踩到）→ 看本文「已產出的 artifacts iOS 可以直接複用 § 2」
