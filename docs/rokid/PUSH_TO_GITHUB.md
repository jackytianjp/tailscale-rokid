# 把代码推到 GitHub 以便 Actions 出 APK（我已在本地帮你准备好）

你只需要确保本地能 `git push` 到你的 GitHub 仓库。

## 目标仓库

- `https://github.com/jackytianjp/tailscale-rokid`

## 推送内容

为避免把整个 Tailscale 大仓库推上去，我会把以下最小集合整理成一个独立目录再推送：

- `.github/workflows/rokid-apk.yml`
- `android/rokid/`
- `docs/rokid/`

## 推送后你在 GitHub 上怎么拿 APK

1. 打开仓库页面
2. 点击 Actions → `rokid-apk` → Run workflow
3. 等跑完下载 Artifacts：
   - `app-debug`（一定会出）
   - `app-release`（配置签名 secrets 后会出，商店上传用它）

