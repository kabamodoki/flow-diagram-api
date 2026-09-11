# flow-diagram-api

「ステータス／手続き（大枠）」と「その中のアクション」「アクション後の遷移先」を JSON で受け取り、
状態遷移図を描いた **HTML（＋CSS）** を返す Java 製 REST API。

図は **画像（SVG / PNG / canvas）を一切使わず、`div` と CSS だけ**で組み立てられている。

**v3.0からの方針**: 大枠の種別（`kind`）やアクションの種別（`type`）が何を意味するか、
どんな色にするかを、プログラム側は一切知らない。すべて **リクエストJSONの `kinds`/`types` で定義**する。
APIは「渡された定義に従って描画する」処理に徹し、設定はできる限りJSON側で完結させる。

## 動かす

```bash
mvn spring-boot:run
```

ブラウザで <http://localhost:8080/> を開くと、左に JSON、右にリアルタイムプレビューの画面が出る
（「サンプル1」「サンプル2」ボタンでサンプルを読み込める）。

```bash
# 1枚もののHTMLを取得
curl -X POST http://localhost:8080/api/diagram \
     -H "Content-Type: application/json" \
     -d @samples/sample1.json -o out.html
```

## API

| メソッド | パス | 返すもの |
|---------|------|---------|
| POST | `/api/diagram` | ブラウザでそのまま開ける完結した HTML（JSなし。APIレスポンスをそのまま使う用途向け） |
| POST | `/api/diagram/fragment` | `<style>` + 図本体だけの HTML 断片（既存ページへの埋め込み用） |
| GET | `/api/sample` | サンプル1と同内容（後方互換） |
| GET | `/api/sample/1` | サンプル1のJSON（ボタン型。`kinds`/`types`定義入り。1アクションから複数遷移の実例入り） |
| GET | `/api/sample/2` | サンプル2のJSON（フロー型。`kinds`/`types`定義入り） |
| GET | `/` | プレビュー画面 |

エラー時は 400 と `{"error":"VALIDATION_ERROR","messages":[...]}`。違反は全件まとめて返る。

## 入力 JSON

```json
{
  "title": "申請ワークフロー",
  "kinds": {
    "ステータス": { "headerBackground": "#eaf4ff", "background": "#f8fbfe", "border": "#cfe3f5", "textColor": "#2c6291" },
    "手続き":     { "headerBackground": "#eaf8ee", "background": "#f8fcf9", "border": "#cdeada", "textColor": "#2f7a52" }
  },
  "types": {
    "button": { "background": "#ffffff", "border": "#c4c9d0", "borderWidth": 1.5, "textColor": "#2d3748" },
    "flow":   { "background": "#f8fbfe", "border": "#7fb8ee", "borderWidth": 1.5, "textColor": "#2c6291" }
  },
  "states": [
    {
      "label": "未申請",
      "kind": "ステータス",
      "actions": [
        { "type": "button", "label": "申請する", "next": "審査中" },
        { "type": "button", "label": "破棄", "next": "破棄" }
      ]
    },
    { "label": "審査中", "kind": "手続き" },
    { "label": "破棄", "kind": "ステータス" }
  ]
}
```

### states[]（大枠）
**`id` は無い。`label` がそのまま識別子を兼ねる**（`states[]` 内で重複不可）。

| フィールド | 必須 | 説明 |
|-----------|------|------|
| `label` | ○ | 表示名。**同時に識別子でもある** |
| `kind` | | **任意の文字列**。`kinds` のキーと対応させる（下記）。省略時は内部的に `"default"` |
| `actions` | | アクション配列（省略時は終端） |

### actions[]
| フィールド | 必須 | 説明 |
|-----------|------|------|
| `label` | ○ | アクション名（例: 申請する） |
| `type` | | **任意の文字列**。`types` のキーと対応させる（下記）。省略時は内部的に `"default"` |
| `next` | | 遷移先の **`label`**。文字列でも配列でも指定できる（下記）。省略すると矢印を描かない |

### 1つのアクションから複数の遷移先へ
`next` は単一の文字列だけでなく配列も受け付ける。単一の文字列を渡した場合は自動的に1件の配列として
扱われる（後方互換）。配列で複数指定すると、そのアクション1つから複数の矢印が出る。

```json
{ "type": "button", "label": "承認", "next": ["承認済み", "通知送信"] }
```

### 自己ループ・後戻りの扱い（重要）
`next` が自分自身（自己ループ）、または前の列に戻る遷移（差し戻し等）は、
**実際のボックスへ線を引き直さない**。代わりに遷移先ラベルを軽量なテキスト参照
（「↩ ラベル名」とだけ表示、ホバーで元のボックスが強調される）として右側に描画する。

### 列を2つ以上飛び越す遷移
1つのアクションの遷移先が、隣の列ではなくさらに先の列にある場合（例: 途中の手続きを飛ばして
最後のステータスへ直接遷移する等）、線は**すべてのボックスより下を通る迂回経路**で描かれる。
中間のボックスの背後を線が通り抜けて見えなくなる（＝繋がりが分かりにくくなる）のを防ぐため。

## 見た目を変える

見た目は大きく3つに分かれる。**色（kind/type）はJSONで、レイアウトの数値もJSONで、
それ以外の共通色（関係線・未接続マーカー）だけソースコードで**、という切り分け。

### kind（大枠の種別）の見た目: ルートの `kinds`
キー＝`states[].kind` で使う文字列。値はすべて任意項目。

| フィールド | 説明 |
|-----------|------|
| `headerBackground` | ヘッダー行の背景色 |
| `background` | ボックス本体の背景色 |
| `border` | ボックス枠の色 |
| `textColor` | ヘッダー文字・バッジ文字の色 |

`kinds` に無いキーを使う（または `kind` 自体を省略する）と、中立な既定スタイル（灰色系）に
フォールバックする。バッジの文言は `kind` の値そのもの（別名フィールドは無い）。

### type（アクションの種別）の見た目: ルートの `types`
キー＝`actions[].type` で使う文字列。値はすべて任意項目。

| フィールド | 説明 |
|-----------|------|
| `background` | アクションチップの背景色 |
| `border` | アクションチップの枠色 |
| `borderWidth` | 枠の太さ(px)。小数可（例 `1.5`） |
| `textColor` | チップ文字の色 |
| `shadow` | チップの影（CSSのbox-shadow値）。省略可 |

`types` に無いキーを使う（または `type` 自体を省略する）と、こちらも中立な既定スタイルに
フォールバックする。

### レイアウト・文字サイズなど: ルートの `theme`
```json
{ "theme": { "nodeWidth": 280, "columnGap": 200, "background": "#ffffff" } }
```

| 分類 | フィールド |
|------|-----------|
| レイアウト | `nodeWidth` `headerHeight` `actionRowHeight` `actionGap` `nodePaddingTop` `nodePaddingBottom` `columnGap` `rowGap` `canvasPadding` |
| 表示 | `fontFamily` `titleFontSize` `badgeFontSize` `stateFontSize` `actionFontSize` `background` `titleColor` `nodeShadow` `edgeWidth` `arrowSize` `showLegend` |

### 関係線の色: ソースコードの `Palette` クラス
kind/typeに属さない、図全体で共通の色（関係線）だけは、
あえて JSON の対象から外し `src/main/java/com/example/flowdiagram/core/Palette.java` に置いている。

```java
public static final String EDGE_COLOR = "#7fb8ee";          // すべての関係線（薄い青の実線）
```

## テスト

```bash
mvn test
```

## 使用ライブラリ（すべて商用利用可）

| ライブラリ | ライセンス |
|-----------|-----------|
| Spring Boot 3.2.5 | Apache-2.0 |
| Jackson | Apache-2.0 |
| JUnit 5（test スコープのみ） | EPL-2.0 |

外部の描画ライブラリ・CDN・画像は一切使っていない。生成 HTML はオフラインで開ける。

## 設計書

`.company/projects/flow-diagram-api/engineering/basic-design.md` が唯一の真実。
仕様変更は設計書を先に更新すること。
