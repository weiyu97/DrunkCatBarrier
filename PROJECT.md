# 醉猫结界（DrunkCat Barrier）项目文档

**版本：v4.0 | 状态：核心功能完成，可构建 | 许可证：Apache 2.0**


## 一、项目概述

### 1.1 一句话定义

一个纯安卓、本地运行的开源「酒后防冲动/防社死系统」。**后台自动检测醉酒风险，达到阈值后自动上锁**，用户无需任何操作。打开被封印的应用需通过清醒验证。

### 1.2 核心公式

> **清醒时不必记得开，醉酒时自动帮你锁。**

### 1.3 核心设计转变

| 旧思路（错） | 新思路（对） |
|---|---|
| 用户清醒时手动开启保护 | 后台自动检测 + 手动兜底 |
| 醉酒后需用户操作 | 醉酒后无需操作，已自动上锁 |
| 检测靠用户自觉 | 检测靠行为信号加权评分 |

**关键洞察**：醉酒的人无法完成任何操作。保护必须在用户还能操作时自动部署。


## 二、问题与证据

### 2.1 核心痛点

酒精损害前额叶皮层，导致抑制控制下降：

- 深夜给前任发消息
- 社交平台发布不当内容
- 冲动消费、乱转账
- 打「长电话」

### 2.2 现有方案缺口

iOS 有 SafeDrunk、Drunk Lock 等，核心为「预设锁定 + 数学验证」。但都需要用户**提前手动开启**——而真正喝多的人根本不会去开。安卓无对等开源方案。


## 三、核心设计原则

1. **自动优先**：后台持续监测，超阈值自动上锁，无需用户操作。
2. **宁可误锁**：误锁清醒用户损失 30 秒（过一次验证），漏判醉酒用户损失无法挽回。
3. **预警缓冲**：自动上锁前给 30 秒预警，清醒者可取消。
4. **Device Owner 为主，无障碍为降级**。
5. **安全底线不可妥协**：电话、地图、打车、代驾永远可用。
6. **所有数据本地存储**，不读消息内容，不记录支付信息。
7. **轨迹与日志不自动删除**，用户手动管理。


## 四、技术架构

### 4.1 技术栈

| 层面 | 选择 |
|---|---|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material 3 |
| 最低版本 | Android 8.0（API 26） |
| 目标版本 | Android 15（API 35） |
| 存储 | DataStore（偏好）+ JSON Lines（轨迹/日志） |
| 定位 | 系统 LocationManager（不依赖 Google Play 服务） |

### 4.2 模块结构

\`\`\`
com.drunkcat.barrier/
├── MainActivity.kt              # 主界面 + 导航
├── admin/                       # Device Owner 管理
│   ├── DrunkCatAdminReceiver.kt
│   └── DeviceOwnerManager.kt
├── data/                        # 数据层
│   ├── Prefs.kt                 # DataStore 偏好
│   └── InstalledApp.kt          # 已安装应用查询
├── risk/                        # 风险评分引擎
│   ├── RiskSignal.kt            # 信号定义与权重
│   ├── RiskScorer.kt            # 评分算法（纯逻辑，可测）
│   └── RiskMonitor.kt           # 运行时信号累计
├── service/                     # 后台服务
│   ├── AppWatchService.kt       # 无障碍：监测 + 拦截
│   ├── LockService.kt           # 前台服务：保活 + 轨迹
│   ├── WarningState.kt          # 预警状态
│   ├── RiskWarningNotifier.kt   # 预警通知
│   └── AutoProtectionNotifier.kt
├── track/                       # 轨迹记录
│   ├── TrackPoint.kt
│   ├── TrackStore.kt
│   └── TrackRecorder.kt
├── report/                      # 复盘日志
│   └── InterceptLog.kt
├── verify/                      # 清醒验证
│   ├── ArithmeticGenerator.kt   # 两位数加减乘除
│   ├── IdiomGenerator.kt        # 成语填空
│   ├── DotMemoryGenerator.kt    # 点阵记忆
│   ├── IdiomBank.kt             # 成语库（50 条）
│   ├── CooldownPolicy.kt        # 失败冷却阶梯
│   └── CognitiveVerifier.kt
└── ui/                          # 界面
    ├── WarningActivity.kt       # 30 秒预警页
    ├── VerifyActivity.kt        # 清醒验证页
    └── theme/                   # 琉璃风 + 简约风双主题
\`\`\`


## 五、风险评分引擎（核心）

### 5.1 信号与权重

| 信号 | 权重 | 说明 |
|---|---|---|
| LATE_NIGHT | 30 | 深夜时段 22:00–06:00 |
| FREQUENT_APP_SWITCH | 25 | 10 分钟内切换 ≥15 次 |
| HIGH_RISK_APP | 20 | 打开通讯录/短信/社交/支付类应用 |
| DEVICE_SHAKING | 15 | 加速度计检测持续晃动 |
| TYPING_ANOMALY | 10 | 打字删除率异常（仅统计数量，不读内容） |
| SUDDEN_ACTIVITY | 10 | 长时间无操作后突然活跃 |

### 5.2 判定规则

- 信号有效期 30 分钟（TTL）
- 同类型信号只计一次权重
- 总分 = 所有激活信号权重之和，上限 100
- **总分 ≥ 60 → 进入 30 秒预警**

### 5.3 预警机制

1. 风险分超阈值
2. 弹出通知 + 预警界面，30 秒倒计时
3. 用户点「我没醉」→ 清空信号，取消
4. 倒计时结束 → 自动开启保护

### 5.4 高风险包名识别

关键词匹配（避免硬编码具体包名）：
- 通讯录：contact
- 短信：sms / mms / messaging
- 社交：wechat / telegram / whatsapp / messenger / weibo / douyin
- 支付：alipay / wallet / pay / bank
- 购物：taobao / jd / pinduoduo / meituan


## 六、清醒验证机制

### 6.1 目标

**清醒人觉得烦，醉汉过不了。**

### 6.2 三种题型（随机抽 2 种组合）

| 题型 | 内容 | 原理 |
|---|---|---|
| 算术选择题 | 两位数加减乘除，5 题 4 选 1，限时 15 秒 | 酒精降低心算与反应 |
| 成语填空 | 挖去一字，4 个候选字 | 考察语言记忆 |
| 点阵记忆 | 3×3 点亮 4 格，按序还原 | 酒精损害工作记忆 |

### 6.3 判定规则

- 每题限时 15 秒，超时算错
- 答错 2 题立即失败
- 答对 ≥ 总题数−1 通过

### 6.4 失败冷却阶梯

| 失败次数 | 冷却时间 |
|---|---|
| 1 | 30 秒 |
| 2 | 2 分钟 |
| 3 | 10 分钟 |
| 4 | 30 分钟 |
| 5 | 锁定至次日 8:00 |


## 七、轨迹记录

### 7.1 采样策略

| 条件 | 记录 |
|---|---|
| 移动 ≥ 50 米 | 记录一次 |
| 间隔 ≥ 3 分钟 | 记录一次 |

### 7.2 存储

- 按天分文件，JSON Lines 格式
- 存于私有目录 \`files/tracks/\`
- 仅本地，不联网
- **不自动删除**，用户手动清理

### 7.3 权限

\`ACCESS_FINE_LOCATION\` + \`ACCESS_COARSE_LOCATION\`，运行时请求。


## 八、复盘报告

### 8.1 展示内容

- **统计卡片**：拦截次数、通过次数、失败次数
- **拦截时间线**：时间 + 应用名 + 通过/拦截
- **暗夜行迹**：轨迹点时间 + 坐标（或地点名）

### 8.2 原则

- 默认不显示敏感内容
- 所有数据仅本地
- 空状态有友好提示


## 九、双主题系统

### 9.1 琉璃风（默认）

- 深紫背景 #07040F + 红粉主色 #FF3D6E
- 顶部极光区 + 六芒星魔法阵（60 秒旋转 + 循环脉冲）
- 衬线字体，字距宽
- 卡片顶部红光线、金色标题、胶囊标签

### 9.2 简约拟物

- 白灰背景 #F4F5F7 + 紫主色 #7B5CFF
- 系统无衬线字体
- 圆角卡片、轻阴影

### 9.3 切换

设置中一键切换，偏好持久化。


## 十、三种权限模式

### 10.1 核心结论：无障碍是主力

**用户真正想锁的是主用户里的微信、支付宝、抖音。**
能锁这些应用的只有两种方式：

| 方式 | 能否锁主用户应用 | 门槛 |
|---|---|---|
| **无障碍拦截** | ✅ 能 | 开启无障碍服务即可 |
| **Device Owner** | ✅ 能 | 需无账户设备 |
| Profile Owner | ❌ 只能锁工作资料内的应用 | 一键创建 |

因此：**无障碍拦截是默认主力方案**。Device Owner 是"最强但门槛高"的可选路径。
Profile Owner 只适用于"主动把某些应用隔离到工作资料"的特定场景。

### 10.2 无障碍拦截（主力）

监听窗口变化，检测到被封印的应用被打开时弹出清醒验证。

- 门槛最低：设置 → 无障碍 → 开启
- 能拦截主用户内所有应用
- 局限：无法阻止用户强制停止 App（有健康检查提醒）

隐私声明：不读取输入内容，不联网，仅检测前台包名。

### 10.3 Device Owner（可选，最强）

激活命令：
\`\`\`
adb shell dpm set-device-owner com.drunkcat.barrier/.admin.DrunkCatAdminReceiver
\`\`\`

前置条件：设备无任何账户，无次要用户。

能力：LockTask 模式、隐藏应用、禁用安全模式/恢复出厂、天然防卸载。

### 10.4 Profile Owner（特定场景，非通用方案）

一键创建工作资料，无需电脑。但工作资料是独立用户空间（User 10），
**只能管理其中的应用，管不到主用户的微信、支付宝**。

适用场景：主动想把某些高风险应用隔离到工作资料。
**不适用**：想锁主用户应用——请用无障碍拦截或 Device Owner。


## 十一、单元测试

| 测试类 | 用例 | 覆盖 |
|---|---|---|
| RiskScorerTest | 10 | 评分、阈值、TTL、深夜判断、包名识别 |
| ArithmeticGeneratorTest | 5 | 选项唯一性、答案正确性、非负 |
| IdiomGeneratorTest | 4 | 选项唯一性、挖空、答案匹配 |
| CooldownPolicyTest | 2 | 冷却阶梯 |

**共 21 个用例，全部通过。**


## 十二、构建与安装

### 12.1 环境

- JDK 17
- Gradle 8.7
- Android SDK（build-tools 35，platform 35）

### 12.2 统一签名（重要）

本项目 **debug 和 release 共用同一份 release.jks 签名**，避免以下问题：

- 手机上已装 debug 包，换 release 包时签名不匹配导致必须先卸载（丢数据）
- 调试时看到的行为和发布包不一致

**签名文件约定**：

| 项 | 值 |
|---|---|
| 文件路径 | 项目根目录 `release.jks` |
| 已被 `.gitignore` 排除 | ✅ （绝不提交） |
| Key Store 密码 | `drunckat2026` |
| Key Alias | `drunckat` |
| Key 密码 | `drunckat2026` |

**如果本地没有 `release.jks`**，会自动回退到 Android 默认 debug 签名——此时 debug 构建能跑，但装不上已用 release 签名的旧包。

**生成新签名文件（仅首次）**：
\`\`\`
keytool -genkeypair -v \
  -keystore release.jks \
  -alias drunckat \
  -keyalg RSA -keysize 2048 -validity 3650 \
  -storepass drunckat2026 -keypass drunckat2026
\`\`\`

### 12.3 命令

\`\`\`
# 构建（debug / release 都用统一签名，包名相同）
gradle :app:assembleDebug       # 输出 app/build/outputs/apk/debug/app-debug.apk
gradle :app:assembleRelease     # 输出 app/build/outputs/apk/release/app-release.apk

# 一键安装到已连接手机（debug 包，覆盖安装无需卸载）
adb install -r app/build/outputs/apk/debug/app-debug.apk

# 运行单测
gradle :app:testDebugUnitTest
\`\`\`

Release APK 约 8.45 MB，已关闭 R8 混淆（避免 Compose 动画丢失）。

### 12.4 Device Owner 激活

\`\`\`
adb shell dpm set-device-owner com.drunkcat.barrier/.admin.DrunkCatAdminReceiver
\`\`\`

### 12.5 无障碍开启

设置 → 无障碍 → 醉猫结界 → 开启
或：\`adb shell settings put secure enabled_accessibility_services com.drunkcat.barrier/.service.AppWatchService\`


## 十三、隐私边界

1. 所有数据本地存储，不上传
2. 不读取消息内容
3. 不记录支付信息
4. 不常开麦克风/摄像头
5. 无障碍服务仅检测前台包名
6. 轨迹坐标仅本地，逆地理编码为可选功能
7. 用户可一键删除全部轨迹与日志


## 十四、当前状态与待办

### 已完成

**核心机制**
- [x] Device Owner 激活与锁定
- [x] 无障碍降级拦截
- [x] 风险评分引擎（6 信号：深夜/切换/高风险App/晃动/打字异常/突然活跃）
- [x] 自动监测 + 30 秒预警 + 自动上锁
- [x] 自动保护总开关（用户可关闭）
- [x] 「今晚不喝」暂停开关

**验证与解锁**
- [x] 清醒验证（算术/成语/点阵三选）
- [x] 验证难度自适应（按风险分 EASY/NORMAL/HARD）
- [x] 全局放行窗口（一次验证通过，短时免重复验证）
- [x] PIN + 冷却阶梯（真实生效）
- [x] 验证失败反馈页（含冷却倒计时）
- [x] 紧急呼叫入口（安全底线）

**锁定策略**
- [x] 契约到期自动解锁（可自定义 6:00–12:00）
- [x] LockTask 白名单（永久白名单 + 用户自定义）
- [x] 默认锁定清单（自动触发时套用）
- [x] 停止保护需 PIN 验证（防醉酒绕过）

**数据与复盘**
- [x] 轨迹记录 + 逆地理编码（系统 Geocoder）
- [x] 轨迹可视化（Canvas 绘制）
- [x] 拦截日志 + 复盘报告
- [x] 数据清理入口（两步确认）

**界面与交互**
- [x] 双主题 + Logo + 自适应图标
- [x] 动态魔法阵特效（多层反向旋转/流光扫描/星尘粒子）
- [x] 首次启动三步引导
- [x] 一键体检（所有配置缺口可跳转修复）
- [x] 长按图标快捷方式（开启保护/一键体检）
- [x] 健康检查提示（通知权限/无障碍状态）

**工程**
- [x] Release 签名（R8 已关闭，避免丢失 Compose 动画）
- [x] Git 仓库 + Apache 2.0 LICENSE
- [x] 79 个单元测试

### 待办

- [ ] 真机 Device Owner 验证（需无账户设备）
- [ ] 开源发布（GitHub / F-Droid）
- [ ] 轨迹地图底图（当前为 Canvas 简化绘制）


## 十五、参考项目

| 项目 | 参考点 |
|---|---|
| LockLock | 无障碍锁定 + Device Admin 防卸载 |
| AppLock (Device Owner) | Device Owner 真锁定 |
| OwnDroid | Device Owner 权限管理 |


## 十六、许可证

Apache 2.0

---

**文档结束。**
