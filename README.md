# flow-diagram-api

「ステータス／手続き（大枠）」と「その中のアクション」「アクション後の遷移先」を JSON で受け取り、
状態遷移図を描いた **HTML（＋CSS）** を返す Java 製 REST API。

図は **画像（SVG / PNG / canvas）を一切使わず、`div` と CSS だけ**で組み立てられている。

## 動かす

```bash
mvn spring-boot:run
```

ブラウザで <http://localhost:8080/> を開くと、左に JSON、右にリアルタイムプレビューの画面が出る。

```bash
# 1枚もののHTMLを取得
curl -X POST http://localhost:8080/api/diagram \
     -H "Content-Type: application/json" \
     -d @samples/order-flow.json -o out.html
```

## API

| メソッド | パス | 返すもの |
|---------|------|---------|
| POST | `/api/diagram` | ブラウザでそのまま開ける完結した HTML（ツールバー付き） |
| POST | `/api/diagram/fragment` | `<style>` + 図本体だけの HTML 断片（既存ページへの埋め込み用） |
| GET | `/api/sample` | サンプル JSON |
| GET | `/` | プレビュー画面 |

エラー時は 400 と `{"error":"VALIDATION_ERROR","messages":[...]}`。違反は全件まとめて返る。

## 入力 JSON

```json
{
  "title": "注文フロー",
  "states": [
    {
      "label": "未注文",
      "kind": "status",
      "actions": [
        { "type": "button", "label": "注文を確定する", "next": "出荷手続き" },
        { "type": "button", "label": "カートを空にする", "next": "破棄" }
      ]
    },
    {
      "label": "出荷手続き",
      "kind": "procedure",
      "actions": [
        { "type": "button", "label": "出荷する", "next": "出荷済み" },
        { "type": "flow", "label": "在庫切れ", "next": "未注文" }
      ]
    },
    { "label": "出荷済み", "kind": "status" },
    { "label": "破棄", "kind": "status" }
  ]
}
```

### states[]（大枠）
**`id` は無い。`label` がそのまま識別子を兼ねる**（`states[]` 内で重複不可）。

| フィールド | 必須 | 説明 |
|-----------|------|------|
| `label` | ○ | 表示名。**同時に識別子でもある** |
| `kind` | | `"status"`（ステータス・薄い青）または `"procedure"`（手続き・薄い緑）。省略時は `status` |
| `actions` | | アクション配列（省略時は終端） |

### actions[]
| フィールド | 必須 | 説明 |
|-----------|------|------|
| `label` | ○ | アクション名（例: 申請する） |
| `type` | | `"button"`（白いボタン然とした見た目）または `"flow"`（薄い青枠）。省略時は `button` |
| `next` | | 遷移先の **`label`**。省略すると矢印を描かない |

### 自己ループ・後戻りの扱い（重要）
`next` が自分自身（自己ループ）、または前の列に戻る遷移（差し戻し等）は、
**実際のボックスへ線を引き直さない**。代わりに遷移先と同じ見た目のボックスを新規に右側へ複製し、
そこをアクションの無い終端として描画する。図が矢印だらけで交差するのを防ぐための仕様。

### 未接続ノードの表示
どこからも `next` で参照されず、自身にもアクションが無い（＝グラフ上どこにも繋がっていない）
ステータス／手続きは、点線の枠＋「未接続」バッジで他と区別して表示される。
矢印で繋がっている通常の終端（例: 「完了」「キャンセル」）は対象外で、実線のまま表示される。
JSONの書き漏れに気づきやすくするための機能。

## 見た目を変える

### レイアウト・文字サイズなど: JSON の `theme` で部分上書き
```json
{ "theme": { "nodeWidth": 280, "columnGap": 200, "background": "#ffffff" } }
```

| 分類 | フィールド |
|------|-----------|
| レイアウト | `nodeWidth` `headerHeight` `actionRowHeight` `actionGap` `nodePaddingTop` `nodePaddingBottom` `columnGap` `rowGap` `canvasPadding` |
| 表示 | `fontFamily` `titleFontSize` `badgeFontSize` `stateFontSize` `actionFontSize` `background` `titleColor` `nodeShadow` `edgeWidth` `arrowSize` `showLegend` |

### 色: ソースコードの `Palette` クラス（JSON からは変更不可）
色（ステータス／手続き／ボタン／フロー／関係線）は、あえて JSON テーマの対象から外し、
`src/main/java/com/example/flowdiagram/core/Palette.java` の定数として一箇所にまとめている。
配色を変えたいときはこのファイルの値だけを書き換えれば、全図に反映される。

```java
public static final String STATUS_HEADER_BG   = "#dbeeff"; // ステータス（薄い青）
public static final String PROCEDURE_HEADER_BG = "#ddf3e4"; // 手続き（薄い緑）
public static final String BUTTON_BG = "#ffffff";           // ボタン型アクション
public static final String FLOW_BORDER = "#8ec7f0";         // フロー型アクション（薄い青枠）
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
