# iOS 版 App 從零到裝機 —— Xcode 執行步驟

**先讀 [ios-handoff.md](./ios-handoff.md)** 了解決策繼承後再照本文開工。

**目標**：從 upstream `home-assistant/iOS` 生出 `Apporo SmartHome` / `Simon SmartHome` iOS App，能在 iPhone 實機執行、連上 `https://woowtech-ha.woowtech.io` 或品牌自己的 HA server。

---

## 第 0 步：環境檢查（macOS 上 5 分鐘）

```bash
# 系統
sw_vers                              # macOS 14.0+ (Sonoma)
xcodebuild -version                  # Xcode 15.0+，Build 15A240d 以上
xcrun swift --version                # Swift 5.9+
xcrun simctl list runtimes | grep iOS  # 至少一個 iOS 17+ 模擬器 runtime

# 開發工具
brew --version                       # Homebrew（用來裝 tuist / mise 等）
git --version
gh --version                         # gh CLI（登入 WOOWTECH）
gh auth status
```

**缺什麼**：
- 沒 Xcode → App Store 搜「Xcode」下載（首次 8-12 GB）
- 沒 iOS Simulator → Xcode → Settings → Platforms → 下載 iOS 17+ runtime
- 沒 Apple Developer 帳號 → https://developer.apple.com/programs/ 申請 ($99/年)

---

## 第 1 步：Fork 上游 HA iOS 到 WOOWTECH（做一次）

```bash
# 用 gh CLI fork 到 WOOWTECH org
gh repo fork home-assistant/iOS --org WOOWTECH --clone=false --fork-name=woow_ha_ios

# 到 Woow 的 iOS 白牌基礎版 clone 下來
cd ~/Desktop
git clone https://github.com/WOOWTECH/woow_ha_ios.git
cd woow_ha_ios
git remote add upstream https://github.com/home-assistant/iOS.git
git fetch upstream
git remote -v
# 應該看到 origin=WOOWTECH/woow_ha_ios, upstream=home-assistant/iOS
```

**注意**：這個 `woow_ha_ios` 是所有品牌共用的**上游基底**（跟 Android 的 `woow_ha_app` 對應）。品牌 repo 從這裡再種入。

---

## 第 2 步：能不能編譯上游？（驗證環境，不動 code）

HA iOS 用 [Tuist](https://tuist.io) 管理 Xcode project：

```bash
cd ~/Desktop/woow_ha_ios
cat Tuist/README.md      # 讀上游環境需求
cat CONTRIBUTING.md      # 讀 build 指令

# 通常裝法：
brew install tuist       # 或用 mise：mise install tuist@$(cat .tuist-version)
tuist install            # 拉外部依賴
tuist generate           # 生成 HomeAssistant.xcworkspace
open HomeAssistant.xcworkspace
```

Xcode 開起來後：
1. **上方選單** → `HomeAssistant` scheme（app target）
2. **裝置選擇器** → `iPhone 15 Simulator`（先用模擬器）
3. **⌘R** 執行

**預期**：模擬器啟動，看到官方 Home Assistant Companion app onboarding 畫面。若失敗看 Xcode 底下的 Issue Navigator。

**若第一次 build 卡在 Team 錯誤**：
- Xcode → 左側 Navigator 選 project 檔（藍色 icon）
- Signing & Capabilities tab → Team → 選「None」或用你自己個人 Apple ID team（先用個人 team 測試，不要動到品牌方 team）
- Bundle ID 可能 conflict，改成 `io.homeassistant.mobileapp.<你的姓氏>`

---

## 第 3 步：種入品牌 repo（每個品牌做一次）

**apporo** 版：

```bash
# 建立 GitHub repo（如果還沒建）
gh repo create WOOWTECH/Woow_apporo_ha_ios --public --description "Apporo SmartHome iOS App"

# 從 woow_ha_ios 種入完整歷史
cd ~/Desktop
git clone https://github.com/WOOWTECH/woow_ha_ios.git Woow_apporo_ha_ios
cd Woow_apporo_ha_ios
git remote rename origin upstream
git remote add origin https://github.com/WOOWTECH/Woow_apporo_ha_ios.git
git push -f origin main
git push origin --tags
```

**simon** 版：**重新 clone 一份 woow_ha_ios**（不要複製 apporo 目錄）：

```bash
cd ~/Desktop
git clone https://github.com/WOOWTECH/woow_ha_ios.git Woow_simon_ha_ios
cd Woow_simon_ha_ios
git remote rename origin upstream
git remote add origin https://github.com/WOOWTECH/Woow_simon_ha_ios.git
git push -f origin main
git push origin --tags
```

---

## 第 4 步：設定 xcconfig（品牌參數集中管理）

在 apporo repo 建 `Configurations/apporo.xcconfig`：

```
// Configurations/apporo.xcconfig
PRODUCT_BUNDLE_IDENTIFIER = com.apporo.home
PRODUCT_NAME = Apporo SmartHome
DEVELOPMENT_TEAM = <apporo Apple Developer Team ID>
CODE_SIGN_STYLE = Manual
CODE_SIGN_IDENTITY = iPhone Distribution
PROVISIONING_PROFILE_SPECIFIER = <apporo 的 provisioning profile 名稱>
```

在 Xcode 內：
1. 選 project 檔 → target `HomeAssistant`
2. Info tab → Configurations → Debug / Release → **Based on Configuration File** 選 `apporo.xcconfig`

**simon** 同法建 `Configurations/simon.xcconfig`，Bundle ID `com.simon.home`、Team 用 simon 的、主色不同。

---

## 第 5 步：套 App icon 與 splash（15 分鐘）

### 5.1 App icon

Xcode 需要 `AppIcon.appiconset` 包含 15+ 尺寸 PNG。**最快**：

1. 到 https://appicon.co/ 上傳 1024×1024 PNG
2. 下載得到的 `AppIcon.appiconset` zip
3. 解壓、把整個 folder 拖到 Xcode 的 `Assets.xcassets` 內，覆蓋原本的 `AppIcon`

**素材位置**：
- apporo 用 [apporo-logo-full.png](../tools/brand/assets/apporo-logo-full.png)（1024×1024，鳥形+wordmark，透明背景）→ **上傳前**：先在 Preview.app 或 Photoshop 加白色背景（Apple 拒審 alpha channel）
- simon 用 [simon-icon.png](https://github.com/WOOWTECH/Woow_simon_ha_app/blob/main/tools/brand/assets/simon-icon.png)（已含藍底，可直接上傳）

⚠ 1024×1024 icon **禁止有 transparency**，否則 App Store Connect 上傳會被拒。

### 5.2 Launch Screen

上游 iOS 是用 `LaunchScreen.storyboard`：

1. Xcode → 左側 Navigator → `Resources/LaunchScreen.storyboard`
2. 中央的 `UIImageView` 拖到 Attributes Inspector → Image → 選你新加的 `BrandLogo` 資產
3. 背景色（`View` → Background）改成品牌色（apporo `#8B6B24`，simon `#0060A6`）

或直接改用 `Assets.xcassets/LaunchImage.imageset`（不同上游版本結構略不同）。

---

## 第 6 步：主題色與字串替換

### 6.1 主題色

搜尋 upstream 內 `Colors.xcassets` 或 `Assets.xcassets/AccentColor.colorset/Contents.json`：

```json
{
  "colors" : [
    {
      "color" : {
        "color-space" : "srgb",
        "components" : { "red" : "0x8B", "green" : "0x6B", "blue" : "0x24", "alpha" : "1.000" }
      },
      "idiom" : "universal"
    }
  ],
  "info" : { "author" : "xcode", "version" : 1 }
}
```

- apporo：`#8B6B24` → r=139/g=107/b=36
- simon：`#0060A6` → r=0/g=96/b=166

也有可能上游用 hard-coded `UIColor(red:green:blue:)` 分散在多個 Swift 檔，需要 grep：

```bash
grep -rn "UIColor(red:" Sources/ | head
grep -rn "\.blue\|homeAssistantBlue\|colorPrimary" Sources/ | head
```

### 6.2 字串替換（等同 Android 85 處替換）

```bash
# 找出所有含 "Home Assistant" 的 Localizable.strings（en + zh-Hant + …）
find Sources -name "Localizable.strings" -exec grep -l "Home Assistant" {} \;

# 全部替換為品牌名（apporo 為例）
find Sources -name "Localizable.strings" -exec sed -i '' 's|Home Assistant Companion|Apporo SmartHome|g' {} \;
find Sources -name "Localizable.strings" -exec sed -i '' 's|Home Assistant|Apporo SmartHome|g' {} \;

# ⚠ macOS sed 要 -i '' 空字串，不是 Linux 的 -i
```

**慎重**：不要動 `homeassistant://` scheme、integration 名稱、程式 log。

也要改 `Info.plist`：
- `CFBundleDisplayName` → `Apporo SmartHome`

### 6.3 URL Scheme

`Info.plist`（或 `Info-*.plist` per target）：

```xml
<key>CFBundleURLTypes</key>
<array>
    <dict>
        <key>CFBundleURLName</key>
        <string>com.apporo.home.auth</string>
        <key>CFBundleURLSchemes</key>
        <array>
            <string>apporohome</string>
        </array>
    </dict>
</array>
```

搜尋上游有沒有硬 code `homeassistant://` scheme：

```bash
grep -rn "homeassistant://" Sources/
```

找到就改成 `apporohome://` / `simonhome://`。

### 6.4 OAuth CLIENT_ID

同 Android 血淚教訓：找 `CLIENT_ID = "https://home-assistant.io/android"` 對等的 iOS 常數，通常在 `Sources/Shared/API/Authentication/AuthenticationController.swift` 之類的檔案：

```bash
grep -rn "home-assistant.io/android\|home-assistant.io/iOS\|redirect_uri" Sources/
```

改成 iOS 版的 GitHub Pages URL（或跟 Android 共用）：
```swift
static let clientID = "https://woowtech.github.io/Woow_apporo_ha_app/android"
```

若走 iOS 專屬 client_id 頁，先在 `Woow_apporo_ha_app` repo 加 `docs/ios/index.html`：
```html
<link rel="redirect_uri" href="apporohome://auth-callback">
```

---

## 第 7 步：模擬器跑一次

Xcode 上方 scheme + destination 選 `iPhone 15 Simulator`，**⌘R**。

**預期**：
- 模擬器開機，看到品牌 splash（apporo 白底鳥形 / simon 藍底 SmnI）
- Onboarding 頁面出現「Apporo SmartHome」或「Simon SmartHome」字樣
- URL 輸入框輸入 `https://woowtech-ha.woowtech.io` → 連線 → OAuth 授權頁 →（若 client_id 設對）→ Dashboard

**若卡在 OAuth "Invalid redirect URI"**：跟 Android alpha1→alpha2 一樣，client_id 頁沒宣告品牌 scheme。回第 6.4 檢查。

---

## 第 8 步：實機測試（第一次要 Apple Developer 帳號）

### 8.1 用你的個人 Apple ID 先測

1. Xcode → Settings → Accounts → 加你的 Apple ID
2. Signing & Capabilities → Team 選你的 Personal Team
3. Bundle ID 加尾綴避免 conflict：`com.apporo.home.<你的姓氏>`
4. iPhone 用 USB 接 Mac，Xcode 上方 destination 選你的實機
5. **⌘R** → 手機會跳「不受信任的開發者」→ 設定 → 一般 → 裝置管理 → 信任
6. App 開起來測試

### 8.2 品牌方 Apple Team 正式建置

拿到品牌方 Team ID + provisioning profile 後：

1. Xcode → Signing & Capabilities → Team 換成品牌方 Team
2. Bundle ID 用 `com.apporo.home`（或跟品牌方確定的）
3. 建 App Store Connect 上的 App record（先做這步 provisioning profile 才生成）
4. Product → Archive → Distribute App → TestFlight

**TestFlight 內部測試**：組員（<100 人）不用 Apple Review 就能裝，適合先給需求方試用。

---

## 第 9 步：APNs 推播（若要做）

若跟 Android 一樣延後推播，跳過本步。

要做的話：
1. Apple Developer → Keys → 建立 APNs Auth Key `.p8`（**只能下載一次**）
2. 記錄 Key ID 和 Team ID
3. 上傳 p8 到品牌方的 HA server（Settings → Mobile App → APNs config）
4. iOS App target → Signing & Capabilities → 加 `Push Notifications` capability
5. Xcode → Product → Archive → 出 IPA、上 TestFlight，通知走 APNs sandbox
6. 正式 release 走 APNs production

---

## 第 10 步：從模擬器/實機部署 IPA 給非開發者

**方案 A：TestFlight**（推薦）
- Xcode → Product → Archive → Distribute → App Store Connect → Upload
- App Store Connect → TestFlight → 加測試員 Apple ID
- 測試員收 email、下載 TestFlight app、裝

**方案 B：Ad Hoc IPA（給不會註冊 TestFlight 的人）**
- Distribute → Ad Hoc → 產生 IPA 檔
- 每台實機的 UDID 事先加進 provisioning profile 才能裝
- 用 iTunes/Finder 拖 IPA 到裝置

**方案 C：Enterprise 內部發佈**（企業計畫，$299/年）
- 不用 App Store 也能任意分發
- 適合 apporo/simon 自己內部員工測試

---

## 第 11 步：App Store 正式上架

**準備物**（比 Play Store 麻煩）：
- Bundle ID 已到 App Store Connect 註冊
- App 名稱在 App Store 全球唯一（App Store Connect 上輸入 `Apporo SmartHome` 確認可用）
- 至少 3 張 screenshot（iPhone 6.7"、iPad 12.9" 各一組）
- 隱私政策 URL（HA 官方隱私政策不能直接用，要品牌方自己重寫）
- 應用程式圖示 1024×1024（無 alpha）
- App Review Information：測試帳號密碼、備註（**強烈建議**寫「基於 Apache 2.0 授權的 Home Assistant Companion 修改版，白牌客戶為 apporo」避免被質疑侵權）

**送審**：Xcode → Archive → Distribute → App Store Connect
Apple Review 通常 24-72h。第一次被拒的機率 40%+，準備好回覆。

---

## 血淚教訓移植（從 Android 版學到的）

| 問題 | Android 是怎樣 | iOS 怎麼避免 |
|---|---|---|
| Icon 底色錯 | rebrand 把 launcher bg 誤設成主色 | Xcode 直接看 preview，錯了立刻改 |
| Icon 內容被裁 | Android adaptive icon safe area 66/108 | iOS 沒這問題（全 image 顯示） |
| OAuth Invalid URI | client_id 頁沒列品牌 scheme | client_id 頁**先建**再開始寫 code |
| App 內殘留 "Home Assistant" | strings.xml 85 處 | 一開始就 sed 全替換 |
| 上游 vectorResource crash | LinkActivity 用 vector API 讀 PNG | iOS 用 UIImage(named:) 通吃，較不會踩 |
| Bundle 大小寫 | Android 大小寫容忍 | iOS 一律小寫 |
| Splash 藍色 HA 房子殘留 | 上游多個位置存 | Xcode 全 project search `app_icon_launch` 或 `launch_image` |

---

## 什麼時候找 Android team 問

- **決策為什麼這樣定** → Android repo commit log + `docs/adr/`
- **品牌 assets 版本控管** → `tools/brand/assets/` 有 SVG 原始檔
- **後端 URL 決策** → `tools/brand/apporo.conf`（BRAND_HOST）
- **中文 strings 翻譯** → Android repo `common/src/main/res/values-zh-rTW/strings.xml`（可 sed 匯出對照 iOS `.strings`）

---

## 大約時程參考

| 階段 | 預估工時 |
|---|---|
| 環境準備 + fork clone | 半天 |
| 上游能編出 unbranded IPA | 半天 ~ 1 天 |
| 種入品牌 repo | 半天（兩個一起做） |
| Assets + icon + splash | 半天 |
| String 替換 + URL scheme + OAuth client_id | 半天 |
| 模擬器 build 過 | 半天 |
| 實機測試 + fix crash | 1-2 天 |
| TestFlight 內部測試循環 | 3-5 天 |
| App Store 上架 (含 review) | 1-2 週 |

**總計**：兩個品牌 iOS App 從零到 TestFlight，大約 **2 週全職** 可以完成（有 Android 版經驗當基準，比 Android 開發時間短）。

---

## 遇到問題的自救順序

1. 先看本文的相關段
2. 看 `ios-handoff.md` 決策繼承
3. 看對應 Android repo 的 commit log（`gh api repos/WOOWTECH/Woow_apporo_ha_app/commits`）—— 同樣問題 Android 怎麼解
4. 看上游 `home-assistant/iOS` issue tracker
5. 卡住超過 4 小時 → 直接問 Android team
