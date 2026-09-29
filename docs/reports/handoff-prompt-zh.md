# 主账号接手用的提示词

在主账号的 Claude Code（网页版）里选择仓库 `staunch0515/ubos-unit`（`staunch0515/ubos-platform` 也可以，两边的交接文件相同），然后把下面这段话原样发出去：

---

你接手 UBOS 规格书的工作。这个仓库之前由另一个 Claude 会话维护，那段对话你看不到，所需的上下文都写在仓库里。请按顺序做：

1. 先读根目录的 `CLAUDE.md`（工作约定，必须遵守），再读 `docs/HANDOFF.md`（当前状态、已定决策、下一步）。
2. 再读 `docs/50-system/README.md` 和 `docs/50-system/00-meta/05-ai-reading-guide.md`，了解规格书的结构。
3. 运行 `bash docs/tools/regen_all.sh`，确认系统规格书和财务需求两部分都是 0 错误。只运行校验，不要改任何文件。
4. 用中文向我汇报：
   - 你对项目现状的理解（一页以内）；
   - 校验结果；
   - 你建议的下一步（参考 HANDOFF.md 第 4 节）。
5. 汇报后等我确认，再做任何修改。

---

说明：
- 如果选的是 `ubos-platform`，工作要在它的 `main` 分支上继续。
- 以后你给新会话布置任务时，照常说"先提计划、我确认后再做"即可，`CLAUDE.md` 里已经写了这条规则。
