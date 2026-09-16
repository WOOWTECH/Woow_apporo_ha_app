# Play 手機截圖（尚未產出）

Google Play 要求**每種裝置類型至少 2 張**截圖，否則商店資訊無法送出。

iOS 的截圖產線已經修好並可重跑（見 `Tests/UI/HomeAssistantUITests.swift` 的
`StoreScreenshotTests`，連的是 `https://family-demo.apporo.ai`）。
Android 這邊還沒有對應的產線。

## 注意

`fastlane/Fastfile` 的所有 lane 目前都設 `skip_upload_images: true` /
`skip_upload_screenshots: true`，所以即使把圖放進來，CI 也不會上傳 ——
要一併把那些旗標改掉，或第一次用 Play Console 網頁手動上傳。
