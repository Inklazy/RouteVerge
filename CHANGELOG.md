# 更新日志 (CHANGELOG)

本项目遵循 [Semantic Versioning](https://semver.org/lang/zh-CN/) 规范。

## [2.1.3] - 2026-09-26 — v2.1.3 - Stability & UI Polish

### 优化与修复

#### UI 与交互体验优化
- 优化按钮按压反馈效果。
- 修复部分控件按压反馈区域与实际大小不一致的问题。
- 统一按钮、卡片、列表项交互表现。
- 优化模式切换和记录列表操作反馈。
- 优化整体视觉风格：
  - 更简洁的暖白色界面；
  - 优化颜色层级；
  - 改进主要操作按钮视觉表现。

#### 地图模拟性能优化
- 优化路线模拟期间地图刷新逻辑。
- 避免重复重绘完整路线，模拟过程中仅更新当前位置标记。
- 提升长路线模拟流畅度、地图交互稳定性和低性能设备体验。

#### 路线模拟修复
- 修复 OUT_AND_BACK 往返路线返程方向异常问题。
- 修复返程阶段设备朝向未正确更新的问题。
- 增加路线方向相关测试。

#### 模拟服务稳定性增强
- 优化模拟定位服务生命周期，增强异常启动和停止情况下资源清理。
- 修复可能残留的 Mock Provider、WakeLock 和错误运行状态。
- 增强 Session 生命周期保护：防止旧任务影响新模拟任务，防止旧停止流程关闭新的模拟会话，提升 Provider Recovery 安全性。

#### 数据与状态优化
- 优化模拟进度保存策略，减少运行期间不必要磁盘写入。
- 增加位置缓存有效期检查。
- 优化路线数据序列化兼容：完善 speedMps 保存，并保持旧数据兼容。

#### NFC 支付入口优化
- 优化 NFC 支付链接验证逻辑，增强支付宝链接识别安全性。

#### 启动流程优化
- 优化 Activity 重建后的状态恢复。
- 改进更新检查状态恢复、用户协议状态恢复和路线编辑状态保存。

#### 开发说明
本版本主要针对稳定性、模拟准确性、地图性能和 UI 交互体验进行优化。未引入大型架构变化，保持现有数据结构和使用方式兼容。

## [2.1.2] - 2026-09-22

### 升级说明
v2.1.2 更换了正式 Android `applicationId` 为 `com.inklazy.routeverge`。已安装旧版本的用户无法直接覆盖升级，新旧版本可能会同时存在；旧版本保存的点位、保存路线等 App 私有数据不会自动迁移。安装新版本后，请在 Android 开发者选项中重新选择 RouteVerge 作为“模拟位置信息应用”。

### 工程质量与品牌统一
- 统一 Android package / applicationId 为 `com.inklazy.routeverge`，完成 RouteVerge 品牌迁移。
- 瘦身 `MainActivity`，引入 ViewModel、UiState 与 StateFlow，Service 状态改为响应式同步。
- 移除 UI 对 Service 状态的约 1.2 秒轮询，路线计时改用 monotonic clock，并改进 Session Restore。
- 增加相关单元测试，更新 README / DESIGN.md、真实应用截图及 GitHub 项目展示。
- 优化 GitHub Actions Release：检测到已有同名 Tag 或 Release 时输出明确日志并成功跳过，不再让 Workflow 失败。

## [2.1.1] - 2026-09-20

### 更新检查重构
- GitHub 仓库地址收敛为单一配置来源：`gradle.properties` 的 `githubRepository`，经 `BuildConfig.GITHUB_REPOSITORY` 注入，Kotlin 中不再硬编码仓库 slug。
- 版本比较改为逐段数值比较，正确处理 `2.1.9 < 2.1.10 < 2.2.0 < 3.0.0`；当 Release 正文带 `routeverge-version-code` 标记时优先按 `versionCode` 判断。
- 更新弹窗优先直接下载 Release 中的正式 APK 附件，没有 APK 附件时回退到 Release 页面。
- 更新检查失败仍为不阻塞流程，可重试或继续使用当前版本。

### 自动发布
- 新增 `.github/workflows/release.yml`（Android Release）：push 到 main 或手动触发后自动执行单元测试、Lint、签名构建、APK 校验（applicationId / versionName / versionCode / 签名 / SHA-256），并创建 `v{versionName}` Tag 与 GitHub Release。
- 若同名 Tag 或 Release 已存在，工作流直接失败，不覆盖、不删除、不强制推送。
- 正式签名通过 GitHub Actions Secrets 注入，keystore 仅在 Runner 中临时还原，构建结束后删除且不会上传为 Artifact。
- `build.gradle` 新增 `printVersionInfo` 任务供发布流程读取版本；版本号的唯一来源仍是 `build.gradle`。
## [2.1.0] - 2026-09-13

### 体验与交互优化
- 全面优化 Claude 暖色设计体系交互：重构定点/路线双模切换为单胶囊滑动动画与触觉反馈（Haptic Feedback）联动。
- 优化配速选择器与按钮交互，消除按压时矩形高亮溢出，保持圆角边缘整洁。
- 完善主界面与控制面板滚动响应与焦点感知，提升小屏幕设备适配体验。

### 稳定性与兼容性修复
- 修复 Android 14 及以下设备在撤销轨迹点时因调用 List.removeLast() 导致的潜在 NoSuchMethodError 兼容性问题。
- 规范属性配置路径格式，通过全部标准 Lint 静态检查。

### 构建与发布规范
- 递增 ersionCode 至 21，ersionName 升级至 2.1.0。
- 正式版启用 R8 混淆、资源精简与 APK Signature Scheme v2 签名。
- 规范 GitHub 仓库 Issue 模板与 GitHub Actions CI 持续集成流程。
## [2.0.0] - 2026-08-16

### 重大变更
- 全面重构 UI 架构，升级至现代 Jetpack Compose 与 Material 3 体系。
- 引入 Claude 风格暖色设计语言，建立统一的色彩层级与微交互动效系统。
- 完善双模模拟体系：支持单点定点模拟与多点路线运动模拟。

### 新增功能
- **路线编辑与模板绘制**：支持自由绘制路径，以及标准 400 米跑道椭圆模板快速生成。
- **配速选择器**：提供步行、慢跑、快跑预设配速及精准自定义配速输入。
- **路线控制**：支持路线运动过程中随时暂停、继续与停止，保持运动位置不跳变。
- **数据持久化**：支持独立保存常用点位与路线历史，随时一键重用。
- **支付宝 NFC 工具**：支持 NFC 状态检测与前后台无缝跳转唤起。
- **前台保活服务**：完善 MockLocationService 前台通知，保证锁屏与切后台时模拟正常运行。
- **双地图引擎支持**：支持高德地图（国内高精）与 Google Maps 视图切换。

## [1.6.4] - 2026-06-15
- 优化高德地图底图渲染与坐标纠偏逻辑。
- 改进通知栏快捷操作与后台保活策略。

## [1.6.2] - 2026-05-18
- 完善定位权限检测与开发者选项模拟位置引导。
- 优化路线编辑撤销与点位微调交互。

## [1.6.1] - 2026-05-15
- 增加首次启动用户协议与免责合规声明弹窗。

## [1.5.0] - 2026-05-15
- 正式开源，支持基础模拟定位与简单路线规划。