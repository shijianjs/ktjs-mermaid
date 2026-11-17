---
sidebar_position: 2
---

# 快速开始

## 环境要求

- JDK 21 及以上；Gradle wrapper 已提交（9.4.1）。
- Node 与 npm 由 Kotlin Gradle 插件下载并驱动，不需要记 `npm install` 这一步；`gradle.properties`
  里的 `kotlin.js.yarn=false` 保证包管理器仍是 npm。
- 第一次跑浏览器测试时由 Playwright 下载 Chromium。

## 依赖

```kotlin
dependencies {
    implementation("io.github.shijianjs:ktjs-mermaid:12.3.0-kt.1")
}
```

`mermaid` 本身通过模块的 `npm(...)` 声明带进来，Kotlin/JS 应用不需要再声明一次。

## 三个关键调用

```kotlin
import io.github.shijianjs.ktjs.mermaid.mermaid
import io.github.shijianjs.ktjs.mermaid.config.MermaidConfig
import io.github.shijianjs.ktjs.mermaid.config.MermaidConfigThemes
import js.objects.unsafeJso

// 1. 配置一次。这里的 startOnLoad 关着：没有任何东西会自己渲染。
mermaid.initialize(
    unsafeJso<MermaidConfig> {
        startOnLoad = false
        theme = MermaidConfigThemes.forest
    },
)

// 2. 渲染成 SVG。id 就是 svg 在 DOM 里的 id，每张图都要唯一。
val host = document.createElement("div")
document.body.append(host)
mermaid.render("graph-1", "graph TD; A-->B", host).then { result ->
    host.innerHTML = HtmlSource(result.svg)
    // 3. 绑定渲染期间注册的监听器（tooltip、链接点击等）。
    result.bindFunctions?.invoke(host)
    println(result.diagramType) // "flowchart-v2"
}
```

`render` 返回 `RenderResult`：SVG 文本、检测到的图型、绑定函数。插入 DOM 这一步由你完成 ——
wrapper 除了传入的测量容器之外不碰你的页面。

## 只解析不渲染

`parse` 校验文本并报告 mermaid 的理解；加 `suppressErrors` 时它用 `false` 代替抛异常，而 Kotlin
没法把这个 `false` 表达进联合类型，所以 wrapper 提供了 `parseOrNull`：

```kotlin
mermaid.parseOrNull("graph TD; A-->[", unsafeJso { suppressErrors = true }).then { parsed ->
    if (parsed == null) {
        println("不是合法的 mermaid 文本")
    } else {
        println("${parsed.diagramType}，主题 ${parsed.config.theme}")
    }
}
```

## 渲染页面上已有的标记

`run` 自己找元素，所以一个直接输出 `<pre class="mermaid">` 的页面只需要一次调用：

```kotlin
mermaid.run(unsafeJso { querySelector = ".mermaid" })
```

渲染过的元素会被标记 `data-processed`，新增标记后再调用 `run` 只会处理新出现的图表。

## 配置

`MermaidConfig` 是上游完整的配置树，由 mermaid 自己的 `.d.ts` 生成：各图型的配置块
（`flowchart`、`sequence`、`gantt` 等）、带常量对象的字面量联合（`MermaidConfigThemes.dark`、
`MermaidConfigSecurityLevels.loose`）、CSS 长度（`StringOrDouble("12px")`）。用 `unsafeJso` 构造：

```kotlin
mermaid.initialize(
    unsafeJso<MermaidConfig> {
        theme = MermaidConfigThemes.dark
        look = MermaidConfigLook("handDrawn")
        flowchart = unsafeJso {
            htmlLabels = false
            curve = FlowchartDiagramConfigCurves.linear
            nodeSpacing = 30.0
        }
    },
)
```

## 图标包与布局

图标包是 Iconify 的图标集：可以随包一起打进产物，也可以在第一次用到时再取。

```kotlin
mermaid.registerIconPacks(
    arrayOf(
        IconLoader(
            unsafeJso<SyncIconLoader> {
                name = "logos"
                icons = unsafeJso<IconifyJSON> {
                    prefix = "logos"
                    icons = unsafeJso { }
                }
            },
        ),
    ),
)
```

自定义布局算法用惰性 loader 注册 —— mermaid 自己也是这么让 ELK 保持可选的：

```kotlin
mermaid.registerLayoutLoaders(
    arrayOf(
        unsafeJso<LayoutLoaderDefinition> {
            name = "elk"
            loader = { importElk().then { elk -> elkLayout(elk) } }
        },
    ),
)
```

## 构建与测试

```bash
./gradlew build                    # 编译、Detekt、浏览器测试
./gradlew :ktjs-mermaid:jsBrowserTest
./gradlew :demo:e2eTest            # 针对打包后演示页的 Playwright 套件
./gradlew :docs:docusaurusBuild    # 文档站
./gradlew pagesSite                # 文档在 /，demo 在 /demo/，Dokka 在 /api/
```

`build` 不跑演示页的端到端套件，也不组装 Pages 站点：两者都需要浏览器，在 CI 里是独立步骤。

## 开发

```bash
./gradlew :docs:docusaurusStart          # 本站的开发服务器
./gradlew :demo:jsBrowserDevelopmentRun  # 演示页的开发服务器
```

Docusaurus 监视站点目录，改 Markdown 会自动刷新；改 Kotlin 要先跑
`./gradlew :docs:docusaurusSite`，因为站点 import 的是编译产物。

## 目录结构

| 路径 | 内容 |
| --- | --- |
| `package.json` | npm 版本的唯一来源（Dependabot 维护） |
| `wrapper-revisions.properties` | 每个模块的 `-kt.N` 修订号 |
| `ktjs-mermaid/src/jsMain/kotlin` | wrapper 声明 |
| `ktjs-mermaid/src/jsTest/kotlin` | 对应的浏览器测试 |
| `demo/src/jsMain` | 演示页与它的 Kotlin 入口 |
| `docs/src/jsMain/kotlin` | 站点配置与侧边栏 |
| `docs/src/jsMain/resources` | Markdown 页面、MDX 首页、样式、译文 |

## 锁文件

整个 workspace 的 npm 依赖只解析一次，钉在 `kotlin-js-store/package-lock.json`。新增或升级 npm 依赖、
或者改了模块版本之后要重新生成：

```bash
./gradlew kotlinUpgradePackageLock
```

CI 会校验提交的锁文件，忘了重新生成会直接构建失败，而不是悄悄漂移。
