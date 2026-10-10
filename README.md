# 醉猫结界（DrunkCat Barrier）

> 清醒时不必记得开，醉酒时自动帮你锁。

一个纯安卓、本地运行的开源「酒后防冲动 / 防社死系统」。**后台自动检测醉酒风险，达到阈值后自动上锁**——用户无需任何操作。打开被封印的应用需通过清醒验证。

---

## 为什么做这个

酒精损害前额叶皮层，导致抑制控制下降：

- 深夜给前任发消息
- 社交平台发布不当内容
- 冲动消费、乱转账
- 打「长电话」

**现有方案的根本缺陷**：iOS 的 SafeDrunk、Drunk Lock 等都要求用户**提前手动开启**——但真正喝多的人根本不会去开，也操作不了。

**醉猫结界的解法**：后台持续监测行为信号，一旦风险分超阈值，自动上锁。全程用户无需操作。

---

## 核心机制

\`\`\`
后台监测 → 风险分累积 → 超阈值 → 30秒预警
   → 用户无操作 → 自动上锁
   → 打开被锁App → 弹清醒验证
   → 通过 → 临时放行 / 失败 → 挡回
\`\`\`

### 风险信号（加权评分）

| 信号 | 权重 |
|---|---|
| 深夜时段（22:00–06:00） | 30 |
| 10 分钟内切换应用 ≥15 次 | 25 |
| 打开通讯录/短信/社交/支付类应用 | 20 |
| 加速度计持续晃动（待实现） | 15 |
| 突然活跃（待实现） | 10 |

总分 ≥ 60 → 进入 30 秒预警。

### 清醒验证（三选二随机组合）

| 题型 | 内容 |
|---|---|
| 算术选择题 | 两位数加减乘除，5 题 4 选 1，限时 15 秒 |
| 成语填空 | 挖去一字，4 个候选字 |
| 点阵记忆 | 3×3 点亮 4 格，按序还原 |

答错 2 题立即失败，失败冷却阶梯：30 秒 → 2 分钟 → 10 分钟 → 30 分钟 → 锁定至次日。

---

## 技术栈

- **语言**：Kotlin
- **UI**：Jetpack Compose + Material 3
- **最低版本**：Android 7.0（API 24）
- **存储**：DataStore（偏好）+ JSON Lines（轨迹/日志）
- **定位**：系统 LocationManager（不依赖 Google Play 服务）

---

## 权限说明

| 权限 | 用途 |
|---|---|
| 无障碍服务 | 检测前台应用包名，用于拦截（不读取输入内容，不联网） |
| 定位 | 保护期间记录轨迹 |
| 通知 | 预警与状态提醒 |
| 设备管理（可选） | Device Owner 完整锁定 |

**隐私承诺**：所有数据本地存储，不上传；不读取消息内容；不记录支付信息。

---

## 两种运行模式

### Device Owner（首选，需 ADB 激活）

最强锁定能力：LockTask 模式、隐藏应用、禁用安全模式、天然防卸载。

**前置条件**：设备无任何账户，无次要用户。

\`\`\`bash
adb shell dpm set-device-owner com.drunkcat.barrier/.admin.DrunkCatAdminReceiver
\`\`\`

### 无障碍降级（无需特殊权限）

无法激活 Device Owner 时使用。监听窗口变化，检测到被锁 App 弹出验证。

开启：设置 → 无障碍 → 醉猫结界 → 开启

---

## 构建

### 环境

- JDK 17
- Android SDK（build-tools 34，platform 34）
- Gradle 8.2

### 命令

\`\`\`bash
gradle :app:assembleDebug      # 构建 debug
gradle :app:assembleRelease    # 构建 release（需 release.jks）
gradle :app:testDebugUnitTest  # 运行单元测试
\`\`\`

### 签名

Release 构建需在项目根目录放置 \`release.jks\`。生成方式：

\`\`\`bash
keytool -genkeypair -v -keystore release.jks -alias drunckat \
  -keyalg RSA -keysize 2048 -validity 10000
\`\`\`

签名文件已在 \`.gitignore\` 中排除，不会提交。

---

## 项目结构

\`\`\`
com.drunkcat.barrier/
├── admin/        Device Owner 管理
├── data/         偏好存储、应用查询
├── risk/         风险评分引擎（纯逻辑，可单测）
├── service/      无障碍监测、前台服务、预警
├── track/        轨迹记录与逆地理编码
├── report/       拦截日志
├── verify/       清醒验证（算术/成语/点阵）
└── ui/           界面（琉璃风 + 简约风双主题）
\`\`\`

---

## 测试

21 个单元测试，覆盖：

| 测试类 | 用例 |
|---|---|
| RiskScorerTest | 10 |
| ArithmeticGeneratorTest | 5 |
| IdiomGeneratorTest | 4 |
| CooldownPolicyTest | 2 |

\`\`\`bash
gradle :app:testDebugUnitTest
\`\`\`

---

## 界面

双主题，可在设置中切换：

- **琉璃风**（默认）：深紫背景 + 六芒星魔法阵 + 衬线字体，仪式感强
- **简约拟物**：白灰背景 + 紫主色 + 无衬线字体，类似系统 App

---

## 状态

核心功能已完成，可构建运行。

- [x] Device Owner 锁定
- [x] 无障碍降级拦截
- [x] 风险自动检测 + 预警 + 自动上锁
- [x] 清醒验证（三种题型）
- [x] 轨迹记录
- [x] 复盘报告
- [x] 双主题
- [ ] 加速度计步态检测
- [ ] 白名单应用到 LockTask
- [ ] 契约到期自动解锁
- [ ] 真机 Device Owner 验证

---

## 许可证

Apache 2.0

---

**免责声明**：本应用为辅助工具，不能替代个人判断与自我约束。醉酒后请务必确保自身安全，切勿酒驾。
