# Play 手機截圖

六張，1080×2400，Pixel 7a 實機，連 `https://family-demo.apporo.ai`（2026-09-16 產出）。
Google Play 要求每種裝置類型至少 2 張。

| 檔案 | 內容 |
|---|---|
| `01-overview.png` | 總覽：摘要卡片（Repairs / Lights / Climate / Energy）與樓層分區 |
| `02-areas.png` | 兩層樓的房間卡片 |
| `03-living-room.png` | 單一房間：燈光亮度、空調、風扇 |
| `04-light-control.png` | 燈光細部控制：亮度滑桿、色溫／顏色 |
| `05-sidebar.png` | 側邊欄，顯示伺服器名稱與各面板 |
| `06-app-settings.png` | App 自己的設定頁（原生畫面）：Assist、伺服器與裝置、感測器 |

## 產出方式（可重跑）

前五張是 HA 前端，語言跟著**伺服器上的使用者設定**；第六張是 App 原生畫面，
語言跟著**手機系統語言**。這台手機是繁中，所以第六張原本是中文，與前五張不一致。
用 per-app 語言覆寫解決，**不動使用者的系統語言**：

```bash
adb shell cmd locale set-app-locales com.apporo.aiot.debug --locales en-US
# ...拍完...
adb shell cmd locale set-app-locales com.apporo.aiot.debug --locales ""   # 還原
```

狀態列用 SystemUI demo mode 清乾淨（固定 09:30、滿電、滿訊號、無通知圖示）：

```bash
adb shell settings put global sysui_demo_allowed 1
adb shell am broadcast -a com.android.systemui.demo -e command enter
adb shell am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0930
adb shell am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false
adb shell am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4
adb shell am broadcast -a com.android.systemui.demo -e command network -e mobile hide
adb shell am broadcast -a com.android.systemui.demo -e command notifications -e visible false
# ...拍完一定要還原,否則使用者的狀態列會一直停在假的 09:30...
adb shell am broadcast -a com.android.systemui.demo -e command exit
adb shell settings put global sysui_demo_allowed 0
```

## 兩個踩過的坑

1. **`am start -n` 會說「Activity class does not exist」**。不是真的不存在 ——
   App 被 force-stop 之後處於 stopped 狀態，`am start` 預設不喚醒 stopped package。
   加 `-f 0x20`（`FLAG_INCLUDE_STOPPED_PACKAGES`）就好。
2. **不要用 `monkey -p` 啟動**。debug 版有兩個 launcher（多一個 LeakCanary），
   `monkey` 會挑到 LeakCanary 的那一個。要指定完整 component。

## 上傳

`fastlane/Fastfile` 的所有 lane 目前都設 `skip_upload_images: true` /
`skip_upload_screenshots: true`，所以即使圖放在這裡，CI 也不會上傳 ——
第一次請用 Play Console 網頁手動上傳，或先把那些旗標改掉。

## 待確認（不是缺陷，是取捨）

商店文案主打的是 **AP-1004S / AP-1005S 樓宇控制器**（KNX/BACnet/Modbus/DALI，
辦公樓層、零售、飯店），但這組截圖是**住宅** demo（客廳、廚房、主臥、長輩房…）。
文案裡有「或私人住宅」所以不算矛盾，但如果要跟主打情境一致，
需要在 demo 主機上另建一個商用儀表板再重拍。
