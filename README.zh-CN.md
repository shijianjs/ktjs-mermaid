# ktjs-mermaid

<p align="center"><img src="docs/src/jsMain/resources/static/img/ktjs-mermaid-logo.png" width="120" alt="ktjs-mermaid"></p>

[`mermaid`](https://www.npmjs.com/package/mermaid) 12.3.0 的 Kotlin/JS wrapper：把 npm 包的公开 API
全部写成编译器可检查的 `external` 声明，另外还有演示页、用
[ktjs-docusaurus](https://github.com/shijianjs/ktjs-docusaurus) 构建的文档站，以及围绕它们的测试与发布
流水线。

[English](README.md) | [简体中文](README.zh-CN.md)

**[文档](https://shijianjs.github.io/ktjs-mermaid/)** ·
**[在线演示](https://shijianjs.github.io/ktjs-mermaid/demo/)** ·
**[API 文档](https://shijianjs.github.io/ktjs-mermaid/api/)**

## 模块

| 模块 | npm 包 | wrapper 版本 |
| --- | --- | --- |
| [ktjs-mermaid](ktjs-mermaid/README.zh-CN.md) | `mermaid` | `12.3.0-kt.1` |

仓库按 npm 包（或一组共享上游版本与发版节奏的包）划分 Gradle 模块。终端应用打包时本来就会摇树，
把相关包合并到一个模块，比每个包各建一个模块更省发布额度和版本噪音。

## wrapper 覆盖了什么

- 入口对象 `mermaid`：`initialize` / `render` / `parse` / `run` / `detectType` /
  `registerIconPacks` / `registerExternalDiagrams` / `registerLayoutLoaders`，以及它背后的
  `MermaidAPI` 单例和返回的 `Diagram`；
- 这些调用交回的结果类型（`RenderResult`、`ParseResult`、`ParseOptions`、`RunOptions`、
  `DetailedError`、`UnknownDiagramError`）；
- 外部图型要实现的那套契约（`DiagramDefinition`、`DiagramDB`、`DiagramRenderer`、
  `ParserDefinition`）；
- Iconify 的数据结构与两种图标加载器，以及布局 loader 的契约；
- `MermaidConfig`，由 mermaid 自己的 `.d.ts` 生成：36 个接口、572 个成员，每个字面量联合都有常量对象。

刻意没包的（全是 `any` 的主题模块、`Diagram.fromText`、detector 注册表、logger）在
[coverage.md](docs/development/coverage.md) 里逐条写了理由。

## 环境要求

- JDK 21 或更高（构建使用 Gradle 9.4.1）。
- Node 与 npm 由 Kotlin Gradle 插件自动下载；`gradle.properties` 里的 `kotlin.js.yarn=false` 保证包管理器
  是 npm。
- 第一次跑浏览器测试时由 Playwright 下载 Chromium。

## 构建与测试

```bash
./gradlew build                    # 编译、Detekt、浏览器测试
./gradlew :ktjs-mermaid:jsBrowserTest

# 演示页的端到端检查（Playwright，打打包产物）
./gradlew :demo:e2eTest

# 文档站：Kotlin 配置 + Markdown 页面，由 ktjs-docusaurus 构建
./gradlew :docs:docusaurusBuild

# GitHub Pages 站点（根目录是文档，demo 在 /demo/，API 文档在 /api/）
./gradlew pagesSite
```

`build` 不含演示页的端到端套件，也不组装 Pages 站点：两者都要浏览器，在 CI 里是单独的步骤。

新增或升级 npm 依赖后，刷新共享锁文件：

```bash
./gradlew kotlinUpgradePackageLock
```

`docs` 模块同样是 Kotlin/JS 项目。三个 ktjs-docusaurus 版本都写在 `gradle.properties` 里：wrapper klib、
Gradle 插件、以及首页分区用到的工具箱——那个项目的每个模块各自带 `-kt.N` 修订号。写 SNAPSHOT 时从
`mavenLocal` 解析。工具箱的浏览器那一半不是独立的 Maven 产物，而是 kit klib 里的资源，由
`extractKitBundle` 解到站点的 `static/ktjs-client/`。

## 版本约定

每个模块的版本为 `{上游 npm 版本}-kt.{修订号}`。上游 npm 版本只在根 [`package.json`](package.json)
中声明；每个模块的修订号在 [`wrapper-revisions.properties`](wrapper-revisions.properties) 中维护。
Gradle 从这两份文件推导基础版本和 `npm(...)` 依赖：

- 只发布带 `-kt.N` 后缀的版本，不存在不带后缀的版本。
- 工作树里的版本一律再带 `-SNAPSHOT`：推导出的是「下一个要发的版本」，Central 上还没有，开发构建不冒用它。
  发布时才用 `-Pktjs.release=true` 去掉后缀。
- 上游升级：改 `package.json` 里的版本，基础版本自动跟随，修订号保持不变。
- 仅 wrapper 修复：只把对应模块的修订号加一（`kt.2`、`kt.3`……）。
- 修订号按模块维护，只增不减，永不重置为 `kt.1`。发布时只发修订号变化的模块。

## 发布

发布配置统一在根 [`build.gradle.kts`](build.gradle.kts) 里，对所有模块生效。`.github/workflows/release.yml`
签名、按 Maven 坐标打包并注册到共享的 Maven Central hub，由 hub 合并成一次 deployment。派生项目如果不想
发布，删掉那条工作流和发布块即可，其它部分不依赖它。

## 许可证

[MIT](LICENSE)
