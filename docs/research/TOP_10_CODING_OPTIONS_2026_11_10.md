# Ten coding choices for this Android task (research snapshot 2026-11-10)

**Important:** This is an editorial **fitness shortlist**, not a scientifically ranked benchmark and NOT success probabilities. Model != harness. Best outcome depends on Windows device, screenshot tooling, actual emulator, context, constraints, user approvals, and loops. "One prompt" can coordinate a long autonomous multi-stage run but can never guarantee error-free production.

| Decision order | Model / environment | Why it is in shortlist | Caveat |
|---|---|---|---|
| 1 | **Codex** with current available strongest coding model | User wants Codex, repo edits, test orchestration and approval gates | Verify specific model entitlement from Codex UI; model catalog and Codex models are not identical |
| 2 | **Google Gemini 4 Carbon** via Android Studio / Antigravity if enabled | Google announced it 2026-11-10 for long-horizon agent workflows, Android proximity | Newly released; API paid/Ultra eligibility can vary; not independently validated on this exact app |
| 3 | **Claude Fable 5.2** with suitable coding harness (if enabled) | Anthropic launch 2026-11-09, frontier reasoning/vision/coding claims | Entitlement and tool integration must be verified; cannot assume Claude Code access or preferred model |
| 4 | **GPT-6 Astra** via a tool-enabled coding agent if accessible | OpenAI lists flagship model for complex reasoning/coding | General API model isn't necessarily selectable in Codex app; cost/latency |
| 5 | **GPT-6.1 Sol** in supported Codex/Work workflow | OpenAI notes strong coding and available in eligible product variants | Rollout and model selection vary; check current session |
| 6 | **Gemini 4 Argon** with Android Studio agent/Antigravity | Google positions Argon for routine software-engineering work | Need actual Android Studio/emulator tool access |
| 7 | **Claude Opus 5.5** in Claude Code (where available) | Anthropic Sept 2026 model for coding and agentic work | Different harness and token budgets than top-tier offerings |
| 8 | **GitHub Copilot Agent mode** with best supported underlying model | Mature codebase/IDE/terminal workflow and GitHub PR integration | Agent is a host, not its own single underlying model |
| 9 | **Cursor Agent** with supported frontier model | file/shell/search and UI workflows, IDE integration | Depends strongly on configured model and permissions |
| 10 | **Gemini CLI** or **Windsurf Cascade** with verified supported model | Local terminal/repository exploration, cost/workflow alternatives | Quality depends on model and access; cannot promise a 10-screen native app in one unattended pass |

### How to pick empirically

Run a fair **paid small acceptance benchmark** across the same Android project and 1-2 representative screens, with identical PNGs, `D:` access, no extra design changes, identical tests and time budget. Compare: screenshot similarity, compile success, navigation correctness, missing components, genuine runtime, API policy observance, non-destructive Git behavior, and verifiable evidence. Don't assign pseudo-precise completion percentages to unrun tasks.

### Primary sources

- https://developers.openai.com/learn/codex
- https://help.openai.com/en/articles/20001275-chatgpt-work-and-codex
- https://developers.openai.com/api/docs/models
- https://blog.google/innovation-and-ai/technology/developers-tools/gemini-4-carbon-developers/
- https://blog.google/innovation-and-ai/technology/developers-tools/build-with-gemini-4-argon/
- https://www.anthropic.com/claude-fable-and-mythos-5-2
- https://www.anthropic.com/claude/opus
- https://docs.github.com/en/copilot/how-tos/copilot-in-your-ide/use-copilot-agents/use-agent-mode
- https://docs.cursor.com/en/agent/tools
- https://geminicli.com/docs/

Model names, supported modes and access are **subject to verification** on the owner's subscription and 2026-11-10 region. No independent comparative data covering this particular 10-image Android reconstruction was found; this table should not be interpreted as such.
