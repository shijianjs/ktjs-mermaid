# TODO

## 任务一：ktjs-mermaid 的 mermaid wrapper

把 npm 包 [`mermaid@12.3.0`](https://www.npmjs.com/package/mermaid) 的公开 API 完整包装成
Kotlin/JS external 声明。

- 总计：67
- 已完成：63
- 明确不做：4
- 待完成：0

详细清单：[docs/development/coverage.md](docs/development/coverage.md)
技术决策：[docs/development/technical-decisions.md](docs/development/technical-decisions.md)

### 当前待办

- （无）

### 注意事项

- 统计口径与三态规则见 `coverage.md`；「已写出但未跑通测试」一律算待完成。
- 明确不做的事项必须写理由，不得把「暂时没做」计成排除。
- 定义「完成」的证据是 `ktjs-mermaid/build/test-results/jsBrowserTest/*.xml` 里
  `failures=0`，加上测试真的调用了上游库。

## 任务二：项目交付（演示、文档、Pages、仓库改写）

围绕 wrapper 的工程交付，与上面的 API 覆盖分开计数。

- 总计：11
- 已完成：10
- 明确不做：0
- 待完成：1

### 清单

1. ✅ 模板改名（模块 `ktjs-mermaid`、包 `io.github.shijianjs.ktjs.mermaid`、`package.json` 依赖
   `mermaid@12.3.0`、`wrapper-revisions.properties`、`publishedPrefix`）。
2. ✅ 配置类型生成器 `scripts/generate-config.py` 与产物 `config/ConfigTypes.kt`（42 声明 / 27 联合）。
3. ✅ 手写声明 6 个文件（入口、结果类型、图型契约、图标、布局、default 导入）。
4. ✅ 浏览器测试 21 例全绿（`:ktjs-mermaid:jsBrowserTest`）。
5. ✅ `kotlin-js-store/package-lock.json` 在官方源下重建。
6. ✅ demo 演示页重写（6 个预置图型、主题选择、状态行）+ Playwright e2e 6 例全绿。
7. ✅ 文档站页面改写：`intro.md`、`guide/getting-started.md`、`guide/design.md`、`pages/index.mdx`
   及 4 个 `zh-Hans` 译文；侧边栏与 tagline 同步；`:docs:docusaurusBuild` 通过。
8. ✅ 根 `AGENTS.md` 语义化改写（模板的 Vaadin/Web Component 段落替换为 mermaid 约定与踩坑）。
9. ✅ 两套 README（根 + 模块，含 `zh-CN`）改写；`dependabot.yml` 的 `upstream` 分组改成 `mermaid`。
10. ✅ GitHub Pages 源切到 GitHub Actions（`gh api`），`pages.yml` 手动触发。
11. ⬜ 首次 `./gradlew pagesSite` 全量组装 + 首次 `gh workflow run pages.yml` 上线验证。
    阻塞：需要一次提交推上默认分支之后才有意义。

### 注意事项

- `build` 不含 demo 的 e2e 与 `pagesSite`：CI 里是分开的步骤，本地全量验证要三条命令都跑。
- 本机依赖走国内镜像（`~/.npmrc`、`~/.gradle/init.d`、`PLAYWRIGHT_DOWNLOAD_HOST`），不提交进仓库、
  不影响 CI。
