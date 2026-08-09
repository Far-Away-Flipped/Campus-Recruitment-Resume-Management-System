# design-orchestrator Agent 可靠性优化方案 V1.0

> 编制日期：2026-08-10　编制依据：四次实际失联事件根因分析 + 内部对比(execution-orchestrator定义) + 外部调研(Anthropic SDK能力边界/GitHub后台Agent僵尸Bug/ARS Hooks崩溃恢复模式)

---

## 一、问题诊断

### 1.1 故障统计

`design-orchestrator` 在近几个月实际使用中至少失联 **4 次**，表现一致：

1. 作为后台 `Agent` 被拉起（`run_in_background: true`）
2. 顺利派发发现层的 3 个子 agent（researcher/tech-evaluator/data-explorer）
3. 在等待子 agent 返回结果时，本体再也没有"醒来"
4. `TaskOutput` 查询返回 "No task found"——任务被系统回收，无任何报错通知
5. 子 agent 的输出仍然存在且有效，但无编排器消费

### 1.2 四层根因

| 层 | 根因 | 对应症状 | 外部证据 |
|----|------|---------|---------|
| **激活方式** | depth-2 嵌套委托（主对话→Agent→Agent），与已知不稳定模式一致 | 后台 Agent 生命周期不可控 | execution-orchestrator 已通过 depth-1 约束规避 |
| **平台层** | Claude Code 后台 Agent 任务在进程重启后变为僵尸，无回收机制 | "No task found" | GitHub anthropics/claude-code#65925 确认 |
| **状态管理** | 无进度持久化，编排器只依赖会话上下文和内存状态 | 崩溃后零恢复能力 | ARS orchestrator 已实现 artifact-as-contract 写盘模式 |
| **编排链** | 三层串行（发现→设计→审查），3 次往返，每次有故障概率 | 乘性积累失败率 | execution-orchestrator 逐任务 Pipeline 更可靠 |

### 1.3 与 execution-orchestrator 的关键差异

| 维度 | design-orchestrator v1.0 | execution-orchestrator |
|------|-------------------------|----------------------|
| 激活方式 | 可被嵌套为 depth-2 后台 Agent | **禁止嵌套，必须 depth-0 主对话采用** |
| 委托深度 | 主对话 → agent → 子agent（depth-2） | 主对话 → 工作agent（depth-1） |
| 进度持久化 | **无** | JSONL append-only |
| 降级兜底 | **无** | 输出"分派待办队列" |
| 恢复机制 | **无** | 门禁快照每步写盘 |
| max_retries | 1 | 1 |
| 编排链长度 | 3 次往返 | 多次往返但有快照保护 |

---

## 二、优化方案

### 2.1 核心策略：借鉴 ARS "artifact-as-contract" 模式 + execution-orchestrator "depth-1" 约束

不引入新组件。改动范围仅限 `design-orchestrator.md` 一个文件（frontmatter + 正文）。

### 2.2 四大优化

#### 优化 1：激活方式（α 阶段，最关键）

**现在**：design-orchestrator 可被当作任意后台 Agent 拉起，形成不稳定的 depth-2 委托链。

**改为**：在 Agent 定义第 20 行后（frontmatter 结束后）新增"运行前提"章节，与 execution-orchestrator 第 23-29 行完全一致：

```markdown
## 运行前提（必读）

**本剧本必须在主对话中被采用，由主对话亲自扮演协调器角色**，再用 `Agent` 工具分派
Researcher / SystemArchitect / DesignAuditor 等**工作型子Agent**。这样"主对话 → 设计Agent"
是单层委派（depth-1）。

- **正确激活方式**：用户说"按 design-orchestrator 设计 `<任务描述>`"。主对话读本文件、采用本剧本、开始分派。
- **禁止的方式**：把本 orchestrator 通过 `Agent` 工具作为嵌套子Agent拉起。设计层子Agent
  不持有 `Agent` 工具，确保委托链始终为 depth-1。
- **降级兜底**：若发现自己是被 `Agent` 工具拉起的（`Agent` 工具不可用），
  **立即停止扮演协调器**，改为产出一份"分派规划"返回给主对话，并标注：
  `[本Agent被误作嵌套子Agent调用，已降级为规划器输出，请在主对话中激活本剧本]`。
```

#### 优化 2：进度持久化（β 阶段，解决恢复能力）

**现在**：编排器所有中间状态只存在于会话上下文和内存中，本体失联后全部丢失。

**改为**：每个阶段边界将中间结果写入文件系统。进度文件格式复用 UEAS 体系已有的 JSONL append-only 标准。

**进度文件路径**：`<项目根目录>/outputs/design-progress-<任务名>-<时间戳>.jsonl`

**写入时机**（强制规则）：

| 事件 | 写入类型 | 理由 |
|------|---------|------|
| 编排计划确定 | `plan_anchor` | 恢复时知道启用了哪些 Agent |
| 每拉起一个子 Agent | `agent_dispatched` | 知道哪些已派发 |
| 每收到一个子 Agent 输出 | `agent_result` + 中间结果文件 | 崩溃不丢失产出 |
| 每完成一个阶段 | `phase_complete` + `heartbeat` | 阶段边界标记 |
| 每次矛盾裁决 | `contradiction_resolved` | 裁决不可丢失 |
| 每次回炉 | `remelt` | 追踪回炉轮次 |
| 最终文档产出 | `plan_complete` | 完成标记 |

**恢复算法**：

```
1. 读取 outputs/design-progress-*.jsonl 最新文件
2. 定位最后一条完整行 → 确定中断点类型
3. 分支处理：
   - plan_anchor 后中断 → 从头开始（无损失）
   - phase_complete 后中断 → 从下一阶段继续，读已有中间结果
   - agent_dispatched 有但无 agent_result → 重拉起该 Agent
   - heartbeat 显示某 Agent 超时 → 标记缺失，降级继续
4. 从磁盘读已有中间结果（不依赖会话上下文）
```

**借鉴来源**：ARS orchestrator hooks 的 Path A/B 双路径恢复协议——"编排器启动时先检查文件系统是否有已完成的产物，若有则从断点继续"。

#### 优化 3：编排链压缩（γ 阶段，减少故障面）

**现在**：三层串行（发现→设计→审查），3 次等待往返。

**改为**：两层（发现→设计+审查嵌入）。审查层不再作为独立阶段——DesignAuditor 与设计 Agent 同时启动，每收到一个设计输出立即送审。消除了审查层的独立等待往返。

**流程对比**：

```
v1.0（三层）:  发现层(等3个) → 设计层(等5个) → 审查层 → 最终输出
               往返1          往返2          往返3

v1.1（两层）:  发现层(等3个) → 设计层(等5个) + 审查层(嵌入) → 交叉整合 → 最终输出
               往返1          往返2（合并）
```

#### 优化 4：超时后自动降级（δ 阶段，主对话侧监控）

主对话采用剧本后，每 3 分钟检查进度文件的最新 heartbeat：

```bash
latest=$(ls -t outputs/design-progress-*.jsonl 2>/dev/null | head -1)
if [ -n "$latest" ]; then
  last_heartbeat=$(grep '"type":"heartbeat"' "$latest" | tail -1)
  echo "$(date -Iseconds) $last_heartbeat"
fi
```

若超过预期时间 + 5 分钟缓冲仍无新进展，触发恢复流程。

### 2.3 Frontmatter 修改

```yaml
# v1.0.0 → v1.1.0 的变更
version: "1.0.0" → "1.1.0"
max_retries: 1 → 0
requires_agents: 补齐发现层的 researcher / tech-evaluator / data-explorer（v1.0.0 遗漏）
description: 追加 "必须在主对话中被采用（depth-0），禁止嵌套委托"
# 新增字段
progress_persistence: true
heartbeat_interval_seconds: 180
degradation_mode: dispatch_planner
```

### 2.4 降级输出格式（嵌套调用时的兜底）

若发现自己被 `Agent` 工具嵌套拉起，立即降级为规划器输出：

```markdown
[本Agent被误作嵌套子Agent调用，已降级为规划器输出，请在主对话中激活本剧本]

## 编排计划

### 阶段 1：发现层（并行启动）
| Agent | 上下文包 |
|-------|---------|
| Researcher | [完整 prompt] |
| TechEvaluator | [完整 prompt] |
| DataExplorer | [完整 prompt] |

### 阶段 2：设计层+审查层
| Agent | 上下文包 |
|-------|---------|
| SystemArchitect | [完整 prompt + 设计简报] |
| InterfaceDesigner | [完整 prompt + 设计简报] |
| DataArchitect | [完整 prompt + 设计简报] |
| DesignAuditor | [每完成一个设计即送审] |

### 阶段 3：交叉整合
主对话执行逐对交叉审查，矛盾裁决，回炉。

### 阶段 4：最终输出
主对话整合最终设计文档。
```

---

## 三、实施计划

### 3.1 分阶段实施

| 阶段 | 内容 | 解决什么 | 工作量 | 风险 |
|------|------|---------|--------|------|
| **α** | frontmatter 修改 + "运行前提"章节 | 根因 #1（depth-2 委托） | 30 分钟 | 低 |
| **β** | 进度持久化机制（JSONL + 恢复算法） | 根因 #2（无恢复能力） | 2 小时 | 中 |
| **γ** | 编排链压缩（三层→两层）+ 降级格式 | 根因 #3（往返过多） | 1 小时 | 低 |
| **δ** | 主对话侧监控循环 | 根因 #4（无超时自检） | 1 小时 | 低 |

**最小可行交付**：α + β — 仅"运行前提"和"进度持久化"两项就解决 80% 的失联问题（杜绝 depth-2 不稳定 + 崩溃后可恢复）。

### 3.2 改动文件清单

| 文件 | 操作 | 说明 |
|------|------|------|
| `C:/Users/张/.claude/agents/ueas/design-orchestrator.md` | 修改 | frontmatter + 新增运行前提 + 重构工作流 + 新增进度持久化 + 降级格式 |
| `C:/Users/张/.claude/agents/ueas/UEAS-通用工程Agent体系设计方案.md` | 修改（可选） | 更新 design-orchestrator 描述，与修改后定义对齐 |

### 3.3 兼容性

- 子 Agent 定义（researcher/system-architect/design-auditor 等）**零修改**——它们的调用方式和上下文包格式不变
- execution-orchestrator **零修改**——它已有的 depth-1 约束和进度持久化模式正是本次对齐的目标
- 已有项目中的 design-orchestrator 调用方式**不变**——主对话仍然是 `Agent(design-orchestrator, ...)`，变化的是 design-orchestrator 内部的行为

---

## 四、验证方法

1. **α 阶段**：将修改后的 design-orchestrator 用于一个新设计任务，确认主对话能成功采用剧本并完成编排
2. **β 阶段**：模拟中断——在发现层完成后手动停止编排，确认进度文件写入完整，重新启动后能从断点恢复
3. **γ 阶段**：对比 α+β 版本的编排完成率和耗时，确认压缩后无质量损失
4. **δ 阶段**：挂起监控循环，观察 heartbeat 是否正常写入

---

## 五、关键启示：为什么 orchestrator 不能在 prompt 层面修复所有问题

外部调研揭示了三个必须在基础设施层面解决的平台缺陷（非本方案覆盖范围，但必须记录）：

1. **Claude Code 后台 Agent 僵尸任务**（GitHub #65925，Open）：后台 Agent 完成后，服务器端任务状态机不会通知编排器。这是设计缺陷——任务回收依赖 `TaskStop` 主动调用，而非被动超时回收。
2. **Agent 消息传递丢失**（GitHub #25254，Open）：子 Agent 的返回结果可能未送达编排器，导致编排器永久等待。
3. **IPC socket 断开无超时**（GitHub #33043，Open）：Agent Teams 模式下 socket 断开后无重连机制。

当前方案在 prompt 层面务实地利用了 `Agent`/`TaskOutput`/文件系统三大可用原语，在平台缺陷修复前提供一个可工作的编排模式。
