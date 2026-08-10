# Brand HA App (whitelabel)

這份詞彙表限定於 Woow_*_ha_app 白牌系列（`Woow_apporo_ha_app`、`Woow_simon_ha_app`
與未來新品牌）。這幾個 repo 都是 `WOOWTECH/woow_ha_app` 的變體，只換視覺識別與後端指向。

## Language

**Brand**:
一個商業/行銷單位。一個 Brand 對應**唯一**：品牌 repo、Android App、Play Store 上架項、
Firebase 專案、簽章 keystore、HA 伺服器網域（BRAND_HOST）、URL scheme。
目前有 `woowtech`、`apporo`、`simon`。
_Avoid_: flavor, variant, edition — 這幾個字在此專案有 Gradle 專門的技術意涵。

**Upstream**:
特指 `WOOWTECH/woow_ha_app`（Home Assistant Android fork）。所有品牌 repo 從這裡種入
完整 git 歷史，共享的變更都從這裡流下來。品牌 repo 內建 remote 名稱是 `upstream`。
_Avoid_: parent, base, source, origin —— origin 在品牌 repo 已被指到自己的 GitHub。

**Brand repo**:
一個 Brand 專屬的 GitHub repo（`Woow_<brand>_ha_app`）。與 Upstream 不合併、不 cherry-pick，
彼此獨立；共同變更透過 Upstream 流通。
_Avoid_: brand fork, downstream fork —— 「fork」在 GitHub 有另一個 UI 意涵，避免混淆。

**Rebrand**:
把 Upstream 換裝成一個 Brand 的自動化流程，由 `tools/brand/rebrand.sh` 執行。
腳本設計前提是「對未換裝的程式碼跑一次」，第二次跑不會有效果，因為原始關鍵字已被替換。
要改參數要 `git reset --hard <換裝前 commit>` 再重跑。
_Avoid_: theming, styling —— 這詞在 HA/Android 各有另外的技術意涵。

**Mark vs Wordmark**:
- **Mark**：品牌識別圖形本身（apporo 的鳥、simon 的抽象字圖）。launcher icon 只放 Mark。
- **Wordmark**：品牌名稱字樣。不進 launcher icon（Android 已在圖示下方顯示 APP_NAME），
  可用於行銷素材、Play Store 商店頁、onboarding 大圖。

**Placeholder logo**:
`gen_brand_assets.py` 在 `LOGO_SRC` 缺失/失敗時產出的「品牌首字母 + 白圓底」佔位圖。
只讓 pipeline 跑通（能編譯、能裝機、能過 preflight），**絕對不能上架**。
_Avoid_: default logo, fallback icon —— 那些暗示「可上線」，此圖不可。

**BRAND_HOST**:
單一 Brand 的 Home Assistant 伺服器完整網域，不含 scheme、不含尾斜線。
會被替換 140 處：deep link host、推播端點、感測器文件連結、NFC tag URL。
一次性替換，之後要改就是全 repo 大搜尋。上架後不建議變更。

## Cross-cutting rules

- **一律不動 Kotlin `namespace`、`R` class、Manifest component 類別名稱**
  （由 Upstream 的 `9fdd038e` 解耦 applicationId 與 namespace 已保證，
  改了會讓使用者桌面捷徑失效，也會讓每次同步 Upstream 變全檔衝突）
- **不從 apporo 的目錄種 simon**（rebrand 腳本靠原始關鍵字比對替換，換過裝的程式碼會全部比對不到，
  結果是一個混品牌 App）
- **URL_SCHEME 各 Brand 必須不同**（否則使用者手機同時裝多品牌時 deep link 會跳系統選單）
