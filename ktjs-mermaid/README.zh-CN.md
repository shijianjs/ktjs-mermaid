# ktjs-mermaid

[`mermaid`](https://www.npmjs.com/package/mermaid) 12.3.0 的 Kotlin/JS wrapper —— 把
`graph TD; A-->B` 变成 SVG 图表的那个库。这是仓库的 wrapper 模块：一个 npm 包、一个 Gradle 模块、
一个 Kotlin 包。

## 安装

```kotlin
dependencies {
    implementation("io.github.shijianjs:ktjs-mermaid:12.3.0-kt.1")
}
```

`mermaid` 本身通过模块的 `npm(...)` 依赖带进来，不需要另外声明。

## 用法

```kotlin
import io.github.shijianjs.ktjs.mermaid.mermaid
import io.github.shijianjs.ktjs.mermaid.config.MermaidConfig
import io.github.shijianjs.ktjs.mermaid.config.MermaidConfigThemes
import js.objects.unsafeJso
import web.html.HtmlSource

mermaid.initialize(
    unsafeJso<MermaidConfig> {
        startOnLoad = false
        theme = MermaidConfigThemes.forest
    },
)

val host = document.createElement("div")
document.body.append(host)
mermaid.render("graph-1", "graph TD; A-->B", host).then { result ->
    host.innerHTML = HtmlSource(result.svg)
    result.bindFunctions?.invoke(host)
}
```

- `render(id, text, container?)` 返回 SVG 文本、检测到的图型与绑定函数；把标记插进 DOM 是调用方的事。
  容器 id 不要和图 id 相同 —— mermaid 会按那个 id 找回它自己的 svg。
- `parse` 校验文本；wrapper 额外提供 `parseOrNull`，把 `suppressErrors` 产生的 `false` 换成 `null`，
  因为 Kotlin 表达不了联合里的这一支。
- `run(...)` 按选择器渲染页面上已有的元素，适合直接输出 `<pre class="mermaid">` 的页面。
- `MermaidConfig` 是上游完整的配置树，由 mermaid 自己的声明生成；`registerIconPacks` 与
  `registerLayoutLoaders` 分别接收 Iconify 图标集与布局算法。

## 构建与测试

```bash
./gradlew :ktjs-mermaid:jsBrowserTest
```

测试跑在 Chromium 里：一张图只有被量过、排过版才算验证过。

新增或升级 npm 依赖后，刷新共享锁文件：

```bash
./gradlew kotlinUpgradePackageLock
```
