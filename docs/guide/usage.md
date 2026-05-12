# 使用说明

本章节介绍如何获取 **EpicTool** 源码并完成环境编译。

## 源码获取

首先，使用 Git 将项目仓库克隆至本地：

```bash
git clone [https://github.com/XingHuaiYa/EpicTool](https://github.com/XingHuaiYa/EpicTool)

```
## 编译环境配置
项目基于 Android 环境开发，推荐使用官方 IDE 进行构建：
 1. **IDE 选择**：导入 **Android Studio** (推荐) 或 **Android Code Studio**。
 2. **编译步骤**：
   * 等待 Gradle 自动同步（Sync）完成。
   * 点击工具栏的 **Build** -> **Make Project**。
   * 编译完成后，生成的 APK文件将位于 app/build/outputs/ 目录下。
## 静态分析操作
完成编译并运行后，按照以下逻辑进行加固包分析：
### 1. 样本准备
将需要脱壳的 APK 样本进行解压，重点定位 lib/ 目录下的核心保护库（通常为 libEPIC.so）。
### 2. 自动化还原
 * **ELF 解析**：工具将首先识别并解密 .ArmEpic 节区。
 * **DEX 恢复**：根据解析出的密钥，对 assets 中的加密 DEX 进行流解密。
 * **资源修复**：一键修复 AXML 魔数并根据映射表重构 Assets 目录结构。
## 常见问题排查
 * **编译失败**：请检查 Gradle 版本是否匹配，建议点击 File -> Invalidate Caches / Restart。
 * **解析无数据**：确认所选样本确实为 Epic 加固版本，并检查 .so 文件是否被混淆或破坏。
```