# Rokid 眼镜智能安装 APK

本页给出两种安装方式：

1. 直连安装（推荐用于开发/测试）：PC 通过 ADB 直接把 APK 安装到眼镜。
2. 手机协同安装（用于量产/发放场景）：通过 Rokid AI App 的 CustomApp 能力远程安装并启动眼镜端 APK（需要手机端集成 CXR-L；本仓库不包含手机端工程）。

## 方式一：ADB 直连安装（推荐）

### 前置条件

- 眼镜系统已开启 ADB（通常在手机 Rokid AI App 中打开眼镜 ADB 开关）。
- 使用开发线/数据线连接眼镜到电脑（部分随盒线只支持充电，不支持数据）。
- 电脑已安装 Android Platform Tools（`adb`）。

### 操作步骤

1. 确认设备在线

```bash
adb devices
```

2. 安装 APK（覆盖安装）

```bash
adb install -r path/to/your.apk
```

3. 首次运行（启动 Launcher Activity）

```bash
adb shell monkey -p com.tailscale.rokid -c android.intent.category.LAUNCHER 1
```

4. 查看日志（排查启动/权限问题）

```bash
adb logcat | findstr /i "tailscale rokid"
```

### 常见问题

- `INSTALL_FAILED_VERSION_DOWNGRADE`：先卸载再装

```bash
adb uninstall com.tailscale.rokid
adb install path/to/your.apk
```

- `INSTALL_FAILED_USER_RESTRICTED`：眼镜侧未允许安装，或系统限制未知来源安装。
- 连接不上：确认眼镜 ADB 开关已开启，且线材支持数据；必要时重插并重新授权。

## 方式二：手机协同“智能安装”（CustomApp）

Sprite 文档里提到两种协同模式：CustomView 与 CustomApp。要实现“手机端一键把 APK 安装到眼镜并启动”，用 CustomApp 更贴合。

### 依赖与限制

- 需要手机端应用集成 CXR-L SDK，并通过 Rokid AI App/Hi Rokid 鉴权拿到 token。
- 眼镜端 APK 通常需要配合 CXR-S（cxr-service-bridge）以便在 CustomApp 会话中运行并接收指令通道。
- 该仓库仅提供 Tailscale 核心与眼镜端 APK 外壳骨架，不包含 Rokid CXR-L/CXR-S 依赖与实现。

### 推荐落地方式（后续要做的工程）

- 手机端新增 `rokid-installer` 应用：
  - 负责与 Rokid AI App 建链、鉴权
  - 调用 CustomApp API：远程安装 APK、启动/停止、传递配置（auth key / tag / exit node 等）
- 眼镜端 `Tailscale for Rokid`：
  - 提供状态 UI/HUD
  - 提供指令接收（用于手机端下发“连接/断开/进入省电/查询状态”等）

## 仓库内脚本（Windows）

已提供一个 ADB 安装脚本模板，支持“自动找到最新构建产物并安装到眼镜”：

- [install-rokid-apk.ps1](file:///c:/Users/takano/OneDrive%20-%20Aderans%20Company%20Limited/httpsgithub.comtailscaletailscale/tailscale/android/rokid/scripts/install-rokid-apk.ps1)

