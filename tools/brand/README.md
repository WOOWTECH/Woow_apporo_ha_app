# tools/brand — 白牌換裝工具

## 先讀這段：這些工具對「本 repo」已經不能用了

`rebrand.sh` 的設計是「對一份**尚未換裝**的 upstream 程式碼跑一次」（CONTEXT.md 有寫）。
它整支腳本靠比對 `woowtech` / `com.woowtech.home` / `aiot.woowtech.io` 這些關鍵字做取代。

`Woow_apporo_ha_app` 已經換裝完畢 —— `main` 是 `com.apporo.home`，
`feat/apporo-aiot-batch1` 是 `com.apporo.aiot`，兩者都沒有 `com.woowtech.home`。
腳本在這裡**永遠比對不到東西**，所以自 2026-09-14 起它會在動任何檔案之前拒絕執行並說明原因。

為什麼要擋而不是讓它自然失敗：舊版腳本第 3 步才會因為找不到 applicationId 而 `exit 1`，
但第 1、2 步已經重新產生了 launcher icon、並把整組 11 階色票 sed 過一輪。
失敗時留下的是一棵改了一半的工作樹，而終端只印一行「applicationId 替換失敗」。

**這些檔案不要刪。** 它們是從 upstream 種下一個新品牌時的工具，換裝流程本身沒有壞。

## 各檔案現在的用途

| 檔案 | 對本 repo | 對新品牌 |
|---|---|---|
| `rebrand.sh` | 已停用（守門會拒絕） | 主要工具，對 upstream 乾淨 checkout 跑一次 |
| `apporo.conf` | **apporo 目前生效品牌值的單一事實來源**，餵給 `preflight.py --verify-repo` | 照抄結構當範本 |
| `preflight.py` | 合併上游後驗證品牌值沒被蓋回去 | 換裝前驗證設定檔、擋撞號 |
| `gen_brand_assets.py` | 單獨重生 icon / 色階時可用 | 由 `rebrand.sh` 呼叫 |
| `assets/` | apporo logo 原始檔 | — |

## 本 repo 還會用到的指令

合併上游、或懷疑品牌值被蓋掉之後：

```bash
python3 tools/brand/preflight.py --verify-repo tools/brand/apporo.conf
```

它會比對 applicationId、app_name（en / zh-rTW）、colorPrimary、推播網域這五處，
並列出仍含 `woowtech` 字樣的檔案。
`woowtech.github.io` 不算殘留 —— OAuth client_id 頁掛在 WOOWTECH 的 GitHub Pages 上，
是刻意保留的（見 `docs/android/index.html`）。

**改品牌值請直接改原始檔，改完跑上面這行確認改全了。不要為了改一個值回頭跑 `rebrand.sh`。**

## 要從 upstream 種一個新品牌

```bash
# 1. 準備一份「未換裝」的 upstream 乾淨 checkout（applicationId 仍是 com.woowtech.home）
git checkout -b brand/<id> <upstream-main>

# 2. 寫設定檔，先驗證，順便跟已上線的品牌比對撞號
cp tools/brand/apporo.conf tools/brand/<id>.conf   # 每個值都要改
python3 tools/brand/preflight.py tools/brand/<id>.conf tools/brand/apporo.conf

# 3. 換裝
bash tools/brand/rebrand.sh tools/brand/<id>.conf
```

`preflight.py` 裡的 `TAKEN` 表列出已經被佔用的 applicationId / 網域 / 主色 / URL scheme。
新品牌撿到別人的值會直接報錯。**每次有品牌上線或改身分，記得同步更新那張表。**

## 換裝後一定要人工處理的兩件事

腳本改不到，但漏了就是壞掉：

1. **OAuth client_id 頁** —— `docs/android/index.html` 裡的 `rel="redirect_uri"` 必須寫新的
   URL scheme。Home Assistant 會從伺服器端抓 client_id 網址、比對這頁宣告的 callback，
   對不上就完全登不進去。而且 GitHub Pages 從預設分支發布，所以這個檔案**必須合進 `main`**
   才會生效；還在功能分支上的新 scheme 是登不進去的。
2. **`OAUTH_CLIENT_ID` 只能指向「現在就連得到」的網址。** 指向還沒架好的品牌網域
   （沒有 DNS 紀錄、或需要登入才讀得到）等於把登入關掉。apporo 就是因為這樣才把
   client_id 留在 GitHub Pages，遷移條件寫在 `AuthenticationService.kt` 的註解裡。
