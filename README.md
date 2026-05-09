# 🎤 錄音鬧鐘 (RecorderAlarm)

Android 鬧鐘 App，支援**用自己的聲音當鈴聲**！

---

## ✨ 功能

| 功能 | 說明 |
|------|------|
| 🎤 錄音鈴聲 | 直接在 App 內錄音，設定為鬧鐘鈴聲 |
| ▶ 試聽錄音 | 儲存前可先播放預覽 |
| ⏰ 多鬧鐘 | 可設定多個不同時間的鬧鐘 |
| 🔁 重複設定 | 可選擇每週固定星期重複 |
| 🔔 全螢幕提醒 | 鬧鐘響時跳出全螢幕，鎖定畫面也顯示 |
| 💤 貪睡模式 | 按「再睡 5 分鐘」延後 |
| 📱 開機自動恢復 | 重開機後鬧鐘自動重新排程 |

---

## 🛠️ 開發環境需求

- **Android Studio** Hedgehog (2023.1.1) 或更新版本
- **JDK 17**
- **Android SDK** API 26–34
- **Kotlin** 1.9.x

---

## 🚀 開啟專案

1. 解壓縮此資料夾
2. 開啟 Android Studio → `File > Open` → 選擇此資料夾
3. 等待 Gradle sync 完成
4. 連接 Android 手機（Android 8.0+）或啟動模擬器
5. 點擊 ▶ Run

> ⚠️ 若出現 `SCHEDULE_EXACT_ALARM` 相關錯誤，請到手機**設定 > 應用程式 > 錄音鬧鐘 > 特殊應用程式存取 > 鬧鐘 & 提醒**，開啟允許。

---

## 📁 專案結構

```
app/src/main/java/com/example/recorderalarm/
├── data/
│   └── AlarmData.kt          # Room 資料庫、DAO、Repository
├── receiver/
│   └── Receivers.kt          # AlarmReceiver、BootReceiver
├── service/
│   └── AlarmService.kt       # 前景服務（播音 + 震動）
├── ui/
│   ├── MainActivity.kt       # 鬧鐘列表主畫面
│   ├── AlarmViewModel.kt     # ViewModel
│   ├── AlarmAdapter.kt       # RecyclerView Adapter
│   ├── AlarmEditorFragment.kt # 新增/編輯 + 錄音
│   └── AlarmRingActivity.kt  # 鬧鐘響時全螢幕
└── utils/
    └── AlarmScheduler.kt     # AlarmManager 排程工具
```

---

## 🔐 所需權限

| 權限 | 用途 |
|------|------|
| `RECORD_AUDIO` | 錄製鈴聲 |
| `SCHEDULE_EXACT_ALARM` | 精確鬧鐘排程 |
| `RECEIVE_BOOT_COMPLETED` | 開機後恢復鬧鐘 |
| `FOREGROUND_SERVICE` | 播放鬧鐘音效 |
| `POST_NOTIFICATIONS` | Android 13+ 通知 |
| `WAKE_LOCK` / `VIBRATE` | 喚醒螢幕、震動 |

---

## 🎨 自訂

- **字體**：在 `res/font/` 放入 `.ttf` 字體並更新 `mono_font.xml`
- **配色**：修改 `res/values/colors.xml`
- **貪睡時間**：修改 `AlarmRingActivity.kt` 的 snooze 邏輯（目前固定 5 分鐘）
