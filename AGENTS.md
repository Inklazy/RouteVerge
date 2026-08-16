# RouteVerge UI/UX 重构任务

你现在负责对当前 RouteVerge Android 项目进行一次系统性的 UI/UX 重构。

## 重要上下文

这是一个已有功能的项目，不是从零开发。

当前项目：

* Jetpack Compose
* Material 3
* 已有自定义 Theme
* 已有 Light / Dark Theme
* 已有 RouteVerge / MiuixSkin 设计变量
* `MainActivity.kt` 当前非常庞大
* 项目包含地图、路线、位置模拟、路线保存、路线播放、设置等功能

仓库：

`https://github.com/ZekTy/qunide-campus-run`

项目根目录存在 `DESIGN.md` 时，必须将它视为本项目 UI 的最高视觉规范之一。

---

# 第一原则

这不是“给 App 换个皮肤”。

目标是：

> 在尽可能不改变现有功能行为的前提下，把当前 vibe coding 形成的 UI 重构成一个真正统一、成熟、可长期维护的 Android UI 系统。

设计优先级：

1. 用户理解
2. 信息层级
3. 交互清晰度
4. 视觉一致性
5. 视觉精致度
6. 动画

不要为了视觉效果牺牲可用性。

---

# 第二原则：先审计，再写代码

在开始修改任何 UI 之前：

1. 阅读项目结构。
2. 阅读 `DESIGN.md`。
3. 阅读现有 `Theme.kt`。
4. 阅读 `MainActivity.kt`。
5. 检查现有所有 Screen / Composable。
6. 检查 Navigation。
7. 检查 Dialog / BottomSheet / Snackbar。
8. 检查地图相关 UI。
9. 检查 Settings。
10. 检查 Light / Dark Theme。

然后输出一份简短的 UI Audit。

Audit 至少说明：

* 当前主要页面
* 当前 UI 的主要问题
* 当前 Theme 哪些值得保留
* 当前哪些 UI Component 重复
* 哪些代码属于明显的 vibe coding 遗留
* 哪些地方严重违反 DESIGN.md
* 哪些地方应该优先重构
* 哪些业务逻辑不要碰

审计完成前不要大规模修改代码。

---

# 第三原则：不要重新选择技术栈

不要把整个 UI 改成 Miuix。

不要引入另一套完整 UI Component System。

当前项目应该保持：

> Jetpack Compose + Material 3

Material 3 是基础。

Miuix 仅作为视觉参考，不作为第二套组件系统。

可以借鉴 Miuix 的：

* Preference row
* Settings layout
* compact controls
* soft surface

但最终组件应该使用现有 Material 3 能力或 RouteVerge 自己的封装。

---

# 第四原则：严格遵循 DESIGN.md

所有视觉决策必须参考：

`DESIGN.md`

特别是：

* Color
* Typography
* Spacing
* Radius
* Surface
* Button
* Card
* Navigation
* Map
* BottomSheet
* Settings
* Empty State
* Error State
* Dark Mode

不要根据自己的偏好重新发明另一套设计。

如果当前代码与 `DESIGN.md` 冲突，应优先把代码逐步调整到 DESIGN.md。

---

# 第五原则：先建立 Design System，再修改页面

首先整理：

```text
ui/theme/
ui/components/
```

确保存在统一的：

* color tokens
* spacing tokens
* shape tokens
* typography
* semantic states
* common component styles

尽量消灭：

* hard-coded color
* random padding
* random radius
* random font size
* duplicated button style
* duplicated card style

例如不要继续产生：

```kotlin
RoundedCornerShape(17.dp)
RoundedCornerShape(19.dp)
RoundedCornerShape(23.dp)
```

而应该使用统一 token。

---

# 第六原则：建立真正可复用的 Components

在 `ui/components` 中整理适合全局复用的组件。

根据项目实际需要选择，不要为了架构而架构。

可以包括：

* RouteVergeCard
* SectionHeader
* PrimaryAction
* SecondaryAction
* StatusBadge
* SettingRow
* SettingSection
* InfoRow
* EmptyState
* ErrorState
* LoadingState
* AppDialog
* AppBottomSheet
* AppIconButton
* RouteSummary
* FloatingMapControl

不要一次性创建几十个组件。

创建一个组件前先判断：

> 它是否有两个以上的合理使用场景？

如果只有一个 Screen 使用，就优先保留在 Screen 内部。

---

# 第七原则：拆分 MainActivity.kt

`MainActivity.kt` 当前已经非常庞大。

不要简单地把 4000 行拆成几个更大的文件。

真正进行 UI separation：

```text
ui/
├── components/
├── navigation/
├── screens/
│   ├── home/
│   ├── route/
│   ├── playback/
│   ├── history/
│   └── settings/
└── theme/
```

目标：

* Activity 负责 App entry / window
* Navigation 负责页面导航
* Screen 负责页面组合
* Component 负责可复用 UI
* business logic 不放进 reusable UI component

不要为了“拆文件”而改变业务逻辑。

---

# 第八原则：首页重新设计

首页不是 Card 展览会。

首页首先回答两个问题：

> 我现在是什么状态？

> 我下一步应该做什么？

信息优先级：

1. 当前状态
2. Primary Action
3. 重要路线信息
4. 最近数据
5. 次要功能

减少：

* 大量独立 Card
* 重复标题
* 无意义装饰
* 过多彩色 Icon
* 同级别的大按钮

首页应该让用户很快找到主要操作。

---

# 第九原则：地图页面

地图是 RouteVerge 的核心场景。

地图应该是视觉主体。

不要：

```text
地图
+ 大卡片
+ 大卡片
+ 大卡片
+ 大卡片
```

而应该：

```text
MAP

small floating controls

route

compact contextual sheet
```

地图页面参考：

* Google Maps 的 map-first
* Uber 的 action hierarchy
* Material 3 的 bottom sheet / floating controls

但是不要复制它们的品牌视觉。

---

# 第十原则：路线 UI

路线应该成为视觉重点。

需要统一：

* route stroke
* start point
* destination
* current position
* selected state

不要使用过度装饰的 Marker。

路线信息应该优先显示：

* 名称
* 距离
* 时间
* 状态
* 主要操作

---

# 第十一原则：运行 / Playback UI

运行或播放状态应该进入“低干扰模式”。

重要内容：

* 当前状态
* 当前位置
* 当前路线
* 播放进度
* Primary Control

用户进入 active playback 后：

> 减少无关信息。

不要继续展示大量设置和装饰。

---

# 第十二原则：History / Saved Routes

历史数据应该偏 Linear 风格：

* 清晰
* 紧凑
* 信息优先

每一行尽量在一个视觉单元内表达：

```text
Route name
date / time / distance
status
action
```

不要把每条历史记录做成巨大的卡片。

---

# 第十三原则：Settings

Settings 应该明显体现 Android 原生感。

优先：

```text
Section
  SettingRow
  SettingRow
  SettingRow
```

而不是：

```text
巨大 Card
巨大 Card
巨大 Card
```

使用 Material 的：

* Switch
* Radio / selection
* Slider
* Navigation row
* Dialog

Miuix 可以作为 Settings 的体验参考，但不要复制 MIUI。

---

# 第十四原则：Dark Mode

Light Theme 和 Dark Theme 都必须重新检查。

不要简单：

```text
white -> black
```

而应该保持：

* background hierarchy
* surface hierarchy
* readable secondary text
* visible active state
* semantic colors

尤其注意：

* cards
* borders
* dialogs
* bottom sheets
* map overlays
* status badges

---

# 第十五原则：状态设计

所有状态应该统一：

* Ready
* Running
* Paused
* Completed
* Error
* Disabled
* Loading
* Empty

不要每个 Screen 单独设计一套颜色和 Badge。

统一语义。

---

# 第十六原则：动画

只增加真正有用的动画：

* 页面变化
* 展开/收起
* 状态变化
* 播放状态
* BottomSheet
* Loading / success feedback

不要为了“高级感”给每个 Card 加入进入动画。

不要加入大量 bounce。

---

# 第十七原则：不要破坏业务

本次工作主要是 UI/UX 重构。

默认不要修改：

* Route logic
* location logic
* playback logic
* service
* NFC
* data persistence
* API behavior
* external integration
* data format

如果 UI 重构确实要求架构调整：

> 最小范围修改。

任何业务行为改变都必须有明确理由。

---

# 第十八原则：禁止 vibe coding

严格禁止以下行为：

* 一个页面一个颜色
* 一个按钮一个特殊 style
* 随机圆角
* 随机 padding
* 到处增加 Card
* 为了看起来高级添加渐变
* 大量玻璃拟态
* 大量阴影
* 大量 blur
* 每个地方都用品牌蓝
* 为了 UI 重构顺手重写业务
* 复制粘贴相似 Composable
* 创建大量只使用一次的抽象
* 凭感觉发明 Material 以外的新交互

每次 UI 决策都问：

> 这个设计是否符合 DESIGN.md？

---

# 第十九原则：截图验收

这是本次任务的重要组成部分。

每完成一个主要 Screen：

1. Build。
2. 启动应用。
3. 运行对应页面。
4. 截图。
5. 检查视觉效果。
6. 修复。
7. 再截图。

必须重点检查：

* alignment
* spacing
* typography
* contrast
* card density
* button hierarchy
* map visibility
* bottom sheet height
* dialog size
* navigation
* dark mode
* small screen

不要只以：

> build successful

作为 UI 完成标准。

---

# 第二十原则：执行顺序

严格按这个顺序：

## Phase 1 — Audit

只分析。

## Phase 2 — Design System

整理 Theme / tokens。

## Phase 3 — Components

整理通用组件。

## Phase 4 — Architecture

逐步拆分 MainActivity 的 UI。

## Phase 5 — Home

重构首页。

## Phase 6 — Route / Map

重构地图和路线交互。

## Phase 7 — Playback

重构运行 / 播放状态。

## Phase 8 — History

重构历史 / 保存数据。

## Phase 9 — Settings

重构设置。

## Phase 10 — Global UI

统一：

* dialogs
* bottom sheets
* snackbars
* navigation
* loading
* errors
* empty states

## Phase 11 — Visual QA

全面检查：

* Light
* Dark
* compact screen
* normal screen
* large text

## Phase 12 — Build/Test

执行项目现有测试和 debug build。

---

# 最终验收标准

完成后，我希望打开 RouteVerge 时看到的是：

> 一个真正经过设计的现代 Android 工具 App。

而不是：

> 一堆 AI 生成的 Card、Button、圆角和蓝色。

最终应该体现：

* Material 3 Android 原生感
* Google Maps / Uber 的地图工作流
* Linear 的信息层级
* Miuix 的部分设置体验
* RouteVerge 自己的品牌色和设计语言

但不要直接复制任何品牌。

最终 UI 必须：

* 统一
* 清晰
* 克制
* 易用
* 可维护
* 支持 Light / Dark
* 支持不同屏幕尺寸
* 不破坏原有业务功能

开始工作时，**第一步只做 Audit，不要立即大规模修改代码。**

视觉能力限制：

如果当前模型无法直接查看图片 / Screenshot，不得声称自己完成了视觉检查。

对于需要真实视觉判断的任务：

1. 完成代码实现与静态检查。
2. 明确报告 runtime screenshot unavailable 或 vision unavailable。
3. 不得根据代码推测截图结果并声称视觉验收通过。
4. 等待用户提供截图或由具备 Vision 能力的模型进行视觉审查。