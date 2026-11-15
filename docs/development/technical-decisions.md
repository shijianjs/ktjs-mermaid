# 技术决策

影响后续实施、且不能被「看起来更自然」的写法推翻的决定。每条记录：背景、选择、代价。覆盖清单本身在
[coverage.md](coverage.md)。

## 1. 配置树由生成器产出，不在产物里手改

`MermaidConfig` 有 36 个接口、572 个成员，且随上游 minor 版本持续增删。手抄会在下一次升级时失真，而且
「少了一个字段」不会报错，只会在运行时被 mermaid 忽略。

- 选择：`scripts/generate-config.py` 读 `mermaid/dist/config.type.d.ts` 生成 `config/ConfigTypes.kt`，
  产出的字面量联合同时给出 `X(String)` 工厂与 `X` 常量对象。
- 代价：生成器不是通用 TS 解析器，遇到不认识的结构直接失败。要支持新写法就扩展脚本（`flatten_unions`、
  `inherited_members`、`new_union` 都是这么加出来的），不要在生成文件里手改。
- 验证：字面量常量在 `ConfigTypesTest` 里逐条对照上游字符串。

## 2. default 导出用顶层 `external val`，不用 `external object`

mermaid 入口只有一个 `default` 导出。`@file:JsModule("mermaid")` + `external object MermaidModule` +
`@JsName("default")` 会编译成**具名**导入，webpack 报
`export 'MermaidModule' (imported as 'MermaidModule') was not found in 'mermaid' (possible exports: default)`。

- 选择：`@JsName("default") internal external val mermaidDefault: Mermaid`，公开访问器
  `val mermaid: Mermaid` 放在另一个文件（`@JsModule` 文件只允许 external 声明）。
- 代价：default 导出只能有一个这样的声明，其他入口成员要直接挂在 `Mermaid` 接口上。

## 3. `render` 的容器 id 必须与图 id 不同

`mermaid.render(id, text, svgContainingElement?)` 用 `id` 在文档里找回自己的 svg。容器若已带同一个 id，
mermaid 量到的是 HTML 容器，渲染死在 `svg.node(...)?.getBBox is not a function`。

- 选择：这是上游行为，wrapper 不做 id 改写（改写会让调用方拿不到自己指定的 svg）；测试与 demo 都用
  `ktjs-mermaid-*-host` 这类独立 id，demo 还按渲染次数递增图 id。
- 代价：这条约束只能靠文档与用例守住（`MermaidTest.renderReturnsSvgAndTheDiagramType` 的 KDoc、
  `getting-started` 与 AGENTS.md 都写明）。

## 4. `parseOrNull` 是 wrapper 便利，不退化成 `JsAny`

`parse` 在 `suppressErrors` 下用 `false` 代替抛异常，上游类型是 `ParseResultOrFalse`。Kotlin 表达不了
「对象或 `false`」这一支，让 `parse` 返回 `JsAny` 会把整条调用链变成无类型。

- 选择：保留 `parse` 的 `ParseOptions` 声明，另加扩展 `parseOrNull`，内部按 `false` 判定后返回
  `ParseResult?`（`ParseResultOrFalse` 只作为上游类型的对应物保留，带两个工厂）。
- 代价：多一个 API；文档与 `coverage.md` 把它记为「wrapper 便利」，不计入上游项。

## 5. 不透明类型的边界

`LayoutData`、`InternalHelpers`、`SVG` / `SVGGroup` / `HTML`、`StyleClasses` 用不透明 `external interface`
句柄，只提供最小读取入口（`InternalHelpers.helper(name)`、`StyleClasses.styleClass(id)`）。

- 理由：前四者是 d3 selection 或上游内部管线结构，消费方只透传不构造；`StyleClasses` 上游是
  `Map<string, DiagramStyleClassDef>`，而 kotlin-wrappers 的 `JsMap` 类型参数上界是 `JsAny?`，`String` 键
  表达不了。
- 代价：这些位置的成员访问需要调用方自己的 `unsafeCast`。收紧顺序：先看上游是否给了结构化声明，再看
  kotlin-wrappers 是否已有可复用类型，最后才考虑自建。

## 6. Node 目标不跑测试

mermaid 渲染要量 DOM（`getBBox`），Node 下没有 DOM。`:ktjs-mermaid:jsNodeTest` 在模块脚本里
`enabled = false`，浏览器测试是唯一的验证路径。

- 代价：本地验证必须能下载 Chromium（本机用 `PLAYWRIGHT_DOWNLOAD_HOST` 走 npmmirror，CI 走官方源）。

## 7. 文档站依赖两个「看起来可删」的配置

- `package.json` 的 `browserslist`：没有它 Babel 按 ES5 处理，`@docusaurus/theme-mermaid` 里
  `__DOCUSAURUS_MERMAID_LAYOUT_ELK_ENABLED__` 守卫不再是死代码，webpack 去解析故意没装的可选 peer
  `@mermaid-js/layout-elk`，`docusaurusBuild` 直接失败。
- `Config.kt` 的 `clientModules` 里 `../../node_modules/d3-transition` 副作用导入：`d3` 声明
  `sideEffects: false`，webpack 会把 `transition()` 摇掉，流程图节点 tooltip 抛
  `tooltipElem.transition is not a function`。

两条都写了注释说明原因，清理「多余配置」时不要动。

## 8. 锁文件只在官方源下刷新

npm 镜像只在本机通过 `~/.npmrc` 生效；锁里没有的包会按当前 registry 记录，CI 用官方源重装立刻报
`Lock file was changed`。刷新流程固定为：设 `npm_config_registry=https://registry.npmjs.org/` →
`./gradlew --stop` → `kotlinUpgradePackageLock --rerun-tasks`。
