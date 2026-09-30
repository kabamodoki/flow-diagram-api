<#
.SYNOPSIS
  起動中のサーバーに samples/<name>.json を POST /api/diagram で投げ、
  結果のHTMLを samples/<name>.html として保存する動作確認用スクリプト。

.EXAMPLE
  .\test-request.ps1 sample1
  .\test-request.ps1 sample2 -BaseUrl http://localhost:8080
#>
param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$Name,

    [string]$BaseUrl = "http://localhost:8080"
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$InputJson = Join-Path $ScriptDir "$Name.json"
$OutputHtml = Join-Path $ScriptDir "$Name.html"

if (-not (Test-Path $InputJson)) {
    Write-Error "入力JSONが見つかりません: $InputJson"
    exit 1
}

Write-Host "POST $BaseUrl/api/diagram <- $InputJson"

try {
    $response = Invoke-WebRequest -Uri "$BaseUrl/api/diagram" `
        -Method Post `
        -ContentType "application/json; charset=utf-8" `
        -InFile $InputJson `
        -ErrorAction Stop

    $utf8NoBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllText($OutputHtml, $response.Content, $utf8NoBom)
    Write-Host "OK (status $($response.StatusCode)) -> $OutputHtml"
}
catch {
    $status = $_.Exception.Response.StatusCode.value__
    Write-Host "NG (status $status)"
    if ($_.ErrorDetails.Message) {
        Write-Host $_.ErrorDetails.Message
    }
    exit 1
}
