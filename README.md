
# Epic_Tool
仅记录对Epic加固的一些研究成果


一个基于 Android 平台开发的 Epic 加固分析工具。
项目使用 Kotlin 编写，主要用于个人学习与记录 Epic 加固相关的静态分析过程。

目前实现内容主要围绕：

DEX 数据恢复

AndroidManifest.xml 修复

Assets 资源还原

ELF 配置解析


项目仍在持续完善中。


---

当前实现

Dex 保护还原

针对 Epic 加固中的 DEX 数据进行解析与导出。

目前已完成：

多 DEX 处理

加密 DEX 提取

DEX 数据恢复

批量导出



---

AXML 资源修复（仅 V2）

Epic V2 会对 AndroidManifest.xml 的 AXML 结构进行破坏。

当前实现：

AXML 魔数修复

Header 恢复

Manifest 基础结构修复



---

Assets 资源还原（仅 V2）

针对 V2 的资源保护进行解析。

包括：

Assets 映射解析

资源解密

文件导出

原目录结构恢复



---

ELF 配置分析

静态解析 SO 中的 .ArmEpic 配置段。

用于提取：

Key

配置参数

DEX 信息

资源映射数据


方便后续分析。


---

开发环境

Android Code Studio

Kotlin

Material Design 3


项目主要在 Android 环境下完成开发与测试。


---

界面

当前版本采用：

树状文件选择

实时日志输出

分析状态显示


方便直接在移动端进行基础分析。


---

使用说明

git clone https://github.com/你的用户名/EpicShield-Unpacker-Kotlin.git

导入 Android Code Studio 后即可编译。

程序需要：

MANAGE_EXTERNAL_STORAGE

权限用于读取 APK 与导出文件。


---

项目说明

这个项目本质上更偏向个人学习记录。

很多逻辑实现都来源于：

对 Epic 加固样本的分析

ELF 结构研究

Android 资源结构学习

静态还原尝试


并不是一个面向商业用途的“全自动脱壳工具”。


---

免责声明

本项目仅用于：

Android 安全研究
加固分析学习
技术交流

请勿用于任何非法用途。