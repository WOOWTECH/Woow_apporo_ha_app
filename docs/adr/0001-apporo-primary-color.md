# apporo 主色使用 #8B6B24 而非品牌方色票 #C49E53

品牌方提供的原始主色 `#C49E53`（金黃）在 WCAG 對比下白字為 2.50:1，遠低於 AA 標準 4.5:1；
App 內大量使用「主色按鈕 + 白字」的 Material 元件，直接沿用會讓按鈕文字整條讀不清楚。

決策：主色改為 `#8B6B24`（深褐金），白字對比 4.88:1 過 AA，保留同色系但確保可讀性。
`#C49E53` 只留在文件與品牌宣傳素材，不進 App 資源檔。

## Considered options

- **A. 沿用 `#C49E53`，改「on-primary」語意色為黑字**：品牌色最忠實，但偏離 Material
  dark-content-on-light-primary 慣例，深色模式下要另外處理，維護成本較高。
- **B. 沿用 `#C49E53`，關掉 preflight 對比檢查**：品牌色最忠實，代價是每個使用者每天
  都要面對讀不清的按鈕，Play Store accessibility 掃描會扣分。
- **C. 改用 `#8B6B24`**（本決策）：品牌感相近但整條 UI 會較沉，一次到位不留技術債。

## Consequences

- `preflight.py` 通過，不需 `--allow-low-contrast` 之類的 override。
- 品牌方若堅持看到 `#C49E53`，可用於 Play Store 商店頁截圖、marketing 網站，但 App
  內部保持 `#8B6B24`。
- 未來若品牌方調整 CI 手冊、允許更深的金色為官方色，可直接把此值當正式主色，不必再改。
