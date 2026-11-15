# Wrapper Coverage

`ktjs-mermaid` 的包装范围清单与状态。统计规则：总数 = 已完成 + 明确不做 + 待完成，每项只属于一个状态。

## 来源与范围

- 上游 npm 包：`mermaid@12.3.0`（`package.json` 锁定，`wrapper-revisions.properties` 的
  `ktjs-mermaid=1`）。
- 声明来源：发布的 `dist/mermaid.d.ts`（入口）及其引用到的 `dist/*.d.ts`。
- 源码核对：本地克隆 `S:\workspace\github\mermaid-js\mermaid`（分支 `develop`，commit
  `a0fd6b8ce`，`packages/mermaid` 版本与 npm 发布的 12.3.0 一致），核对日期 2026-11-15。
- 纳入统计：
  1. 入口对象 `mermaid`（默认导出）的全部成员；
  2. 入口文件导出的全部类型；
  3. 这些成员签名依赖、虽未从入口 re-export 但消费方必须能表达的类型；
  4. `MermaidConfig` 整棵树（生成，`config/ConfigTypes.kt`）；
  5. 为上表服务、但上游没有的便利 API（单独列出，不计入上游项）。
- 不纳入统计：mermaid 内部 diagram 实现（`diagrams/**` 里各图型的 DB / renderer）、
  `themes/**` 的主题变量模块、只服务内部管线的工具函数 —— 它们不在包入口导出，消费方拿不到。

## 统计

| 状态 | 数量 |
|---|---:|
| 总计 | 67 |
| 已完成 | 63 |
| 明确不做 | 4 |
| 待完成 | 0 |

分类明细（每类列条目名，便于核对）：

| 类别 | 条目数 | 状态 | 出处 |
|---|---:|---|---|
| 入口成员 | 15 | 已完成 | `Mermaid.kt` 的 `Mermaid` 接口 |
| 入口本地声明 | 2 | 已完成 | `Mermaid`、`RunOptions`（`MermaidTypes.kt`）、默认导出 `mermaid` |
| 入口 re-export 类型 | 17 | 已完成 | 见下「re-export 类型」 |
| 签名依赖类型 | 25 | 已完成 | 见下「签名依赖类型」 |
| wrapper 便利 API | 2 | 已完成 | `parseOrNull`、`isDetailedError` / `detailedError` |
| `MermaidConfig` 树 | 1 | 已完成 | `config/ConfigTypes.kt`（生成） |
| 明确不做 | 4 | —— | 见「明确不做」 |

> 「已完成」的判定：声明存在、`./gradlew :ktjs-mermaid:jsBrowserTest` 通过，且测试里实际调用了
> 上游库（渲染 / 解析 / 读取配置）。仅写出声明不算完成。

## 入口成员（15）

`startOnLoad`、`parseError`、`mermaidAPI`、`parse`、`render`、`init`、`run`、`registerLayoutLoaders`、
`registerExternalDiagrams`、`initialize`、`contentLoaded`、`setParseErrorHandler`、`detectType`、
`registerIconPacks`、`getRegisteredDiagramsMetadata`。

## re-export 类型（17）

`AsyncIconLoader`、`DetailedError`、`ExternalDiagramDefinition`、`IconLoader`、`InternalHelpers`、
`LayoutData`、`LayoutLoaderDefinition`、`MermaidConfig`、`ParseErrorFunction`、`ParseOptions`、
`ParseResult`、`RenderOptions`、`RenderResult`、`SVG`、`SVGGroup`、`SyncIconLoader`、
`UnknownDiagramError`。

## 签名依赖类型（25）

`Diagram`、`DiagramCode`、`DiagramDB`、`DiagramDbPick`、`DiagramDefinition`、`DiagramDetector`、
`DiagramLoader`、`DiagramMetadata`、`DiagramOrientation`、`DiagramRenderer`、`DiagramStyleClassDef`、
`DoubleOrDoubleArray`、`DrawDefinition`、`IconifyIcon`（`ExtendedIconifyIcon`）、
`IconifyAlias`（`ExtendedIconifyAlias`）、`IconifyJSON`、`IconifyJSONInfo`（含 author / license 块）、
`InitNodes`、`LoadedDiagram`、`MermaidAPI`、`ParseResultOrFalse`、`ParserDefinition`、
`RegisteredDiagramMetadata`、`RegisterExternalDiagramsOptions`、`StyleClasses`。

`Diagram.fromText` 未包装（见「明确不做」），`Diagram` 只经 `mermaidAPI.getDiagramFromText` 取得。

## 明确不做（4）

| 名称 | 出处 | 排除理由 |
|---|---|---|
| `themes` 模块（`themes/index.d.ts` 默认导出 `{base: {getThemeVariables}}`） | `dist/themes/index.d.ts` | 2372 行声明里几乎全部是 `any`，包装出来只是一棵 `JsAny` 树，没有类型价值；主题变量本身通过 `MermaidConfig.themeVariables`（上游同样声明为 `any`）传入更直接。 |
| `Diagram.fromText`（静态方法） | `dist/Diagram.d.ts` | 包入口不导出 `Diagram` 类，消费方拿不到类对象，静态方法无从调用；等价路径 `mermaidAPI.getDiagramFromText` 已包装。 |
| `detectType` 模块的 `detectors` / `addDetector` / `getDiagramLoader` / `registerLazyLoadedDiagrams` | `dist/diagram-api/detectType.d.ts` | 入口只 re-export 了 `detectType` 函数本身；其余是内部注册表，注册外部图型的公开入口是 `mermaid.registerExternalDiagrams`。 |
| logger（`LogLevel`、`LEVELS`、`log`、`setLogLevel`） | `dist/logger.d.ts` | 入口未导出；日志级别通过 `MermaidConfig.logLevel` 配置，`LEVELS` 之类的表也已被 `MermaidConfigLogLevel` 的字面量联合覆盖。 |

## 以不透明类型建模（已完成，但有取舍）

以下类型在上游是几百行、且只用于内部管线的结构，Kotlin 侧以不透明 `external interface` 建模，
并通过辅助函数提供最小可用的读入口：

- `LayoutData`：自定义 layout 从 mermaid 收到后原样回传，不需要 Kotlin 侧构造。
- `InternalHelpers`：上游标注 deprecated + internal（"definitions will change without notice"），
  提供 `InternalHelpers.helper(name)` 读取单个 helper。
- `SVG` / `SVGGroup` / `HTML`：d3 selection，Kotlin 侧不建模 d3，只作句柄透传。
- `StyleClasses`：上游是 JS `Map<string, DiagramStyleClassDef>`；`JsMap` 的类型参数上界是
  `JsAny?`，`String` 键无法表达，故用不透明接口 + `StyleClasses.styleClass(id)`。

这些取舍的理由与「以后要不要收紧」记在 [technical-decisions.md](technical-decisions.md)。
