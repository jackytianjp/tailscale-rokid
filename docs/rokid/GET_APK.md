# 我只需要 APK（用于 Rokid 商店上传）

本仓库环境当前没有安装 JDK/Android SDK，因此无法在这里直接产出 APK 文件；但仓库里已经准备好了可构建的 Android 工程，你可以用以下两种方式拿到 APK：

## 方式 A：本地用 Android Studio 生成 APK（最快）

1. 用 Android Studio 打开目录：
   - `android/rokid/`
2. 生成 Debug APK（可快速验证安装，不适合商店上架）：
   - Build → Build Bundle(s) / APK(s) → Build APK(s)
   - 输出路径通常在：`android/rokid/app/build/outputs/apk/debug/`
3. 生成 Release 签名 APK（用于商店上传）：
   - Build → Generate Signed Bundle / APK → APK
   - 第一次需要创建 Keystore
   - 输出路径通常在：`android/rokid/app/build/outputs/apk/release/`

## 方式 B：用 GitHub Actions 自动打包 APK（适合团队/CI）

仓库可以增加一个工作流，通过手动触发生成 APK 并下载产物：

- Debug：无需密钥，直接生成 `app-debug.apk`
- Release：需要在仓库 Secrets 配置签名信息，生成 `app-release.apk`

签名所需 Secrets（Base64 编码 keystore 文件）：

- `ROKID_KEYSTORE_BASE64`
- `ROKID_KEYSTORE_PASSWORD`
- `ROKID_KEY_ALIAS`
- `ROKID_KEY_PASSWORD`

Release 签名参数也支持通过环境变量读取（见 `android/rokid/app/build.gradle.kts`）。

### GitHub Actions 操作步骤（手把手）

1. 确认工作流文件已在仓库里：
   - `.github/workflows/rokid-apk.yml`
2. 在 GitHub 网页打开你的仓库：
   - 进入 Actions → 选择 `rokid-apk`
3. 先跑 Debug（无需签名，验证流程通不通）：
   - 点击 Run workflow → Run
   - 等待 job 完成后，在页面底部 Artifacts 下载 `app-debug`
4. 准备 Release 签名所需的 keystore（你本地执行一次即可）：
   - 生成 keystore（示例，命令会提示输入密码与别名）：
     - `keytool -genkeypair -v -keystore release.jks -alias rokid -keyalg RSA -keysize 2048 -validity 10000`
   - 把 `release.jks` 转成 Base64（Windows PowerShell 示例）：
     - `[Convert]::ToBase64String([IO.File]::ReadAllBytes('release.jks')) | Set-Clipboard`
5. 配置仓库 Secrets：
   - 进入 Settings → Secrets and variables → Actions → New repository secret
   - 添加 4 个 secret：
     - `ROKID_KEYSTORE_BASE64`：把第 4 步生成的 Base64 内容粘贴进去
     - `ROKID_KEYSTORE_PASSWORD`：keystore 密码
     - `ROKID_KEY_ALIAS`：alias（例如 `rokid`）
     - `ROKID_KEY_PASSWORD`：key 密码（通常可与 keystore 相同）
6. 再次触发工作流生成 Release：
   - Actions → `rokid-apk` → Run workflow
   - job 完成后下载 artifact：`app-release`
   - 里面的 `app-release.apk` 即可上传 Rokid 商店

### 常见问题

- 看不到 Run workflow：需要仓库默认分支包含该工作流文件，且你有触发权限（对私有仓库尤其常见）。
- 只生成 Debug、不生成 Release：通常是 secrets 没配齐或 `ROKID_KEYSTORE_BASE64` 为空。
- 商店提示版本重复：提高 `android/rokid/app/build.gradle.kts` 里的 `versionCode`，再重新出包。

## Rokid 商店上传检查清单（建议）

- 使用 Release 签名 APK（不要上传 Debug APK）。
- 每次更新提高 `versionCode`（`android/rokid/app/build.gradle.kts`）。
- 确认 `minSdk/targetSdk` 满足 Rokid 设备与商店要求（当前工程 `minSdk=31`）。
- 权限尽量最小化：商店审核通常会关注录音、位置、悬浮窗、后台运行等权限的必要性与说明。
- 如果 APK 内需要包含 `tailscaled`（ARM64），请先把二进制打包进 assets（下一节）。

## 重要说明

- Rokid 商店一般要求 APK 已签名（Release 签名）。Debug APK 不建议用于上架。
- 如果你希望 APK 内包含 `tailscaled`（ARM64），需要先在有 Go 1.26+ 的环境里交叉编译并把二进制放进：
  - `android/rokid/app/src/main/assets/bin/arm64-v8a/tailscaled`
  - 脚本在：`android/rokid/scripts/build-tailscaled-android-arm64.ps1`
