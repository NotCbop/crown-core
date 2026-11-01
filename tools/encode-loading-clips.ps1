# Encodes raw gameplay clips into the small loading-screen backgrounds bundled in the mod.
#
#   loading-clips-src/<game>/<anything>.mp4|mov|mkv|webm      (raw, any resolution/fps; gitignored)
#     -> src/main/resources/assets/crown/loading_clips/<game>/<name>.mp4
#
# Output is what the mod's pure-Java decoder (JCodec) plays well: grayscale (the screen shows them
# gray anyway, so no colour is stored), 640x360, 30 fps, H.264 *baseline* profile, no audio.
# Don't change the profile/fps without updating ClipPlayer.FPS.
#
# <game> is the loading screen's game file name: ace_race, battle_box, bingo_but_fast, build_mart,
# dodge_bolt, grid_runners, hole_in_the_wall, meltdown, parkour_tag, parkour_warrior,
# railroad_rush, rocket_spleef, sands_of_time, sky_battle, survival_games, tgttosawaf — or "hub",
# which plays for games with no clips of their own (and loading screens with no game).
#
# Only the first -Seconds of each clip is kept (a clip is on screen ~5 s, and it loops). Already
# encoded clips are skipped unless the source is newer; pass -Force to redo everything.
#
#   powershell -ExecutionPolicy Bypass -File tools\encode-loading-clips.ps1
param(
    [string]$Source = "loading-clips-src",
    [int]$Seconds = 10,
    [int]$Crf = 30,
    [switch]$Force
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$src = Join-Path $root $Source
$dst = Join-Path $root "src\main\resources\assets\crown\loading_clips"

if (-not (Get-Command ffmpeg -ErrorAction SilentlyContinue)) {
    throw "ffmpeg not found on PATH (winget install Gyan.FFmpeg)"
}
if (-not (Test-Path $src)) {
    throw "No source folder: $src (make $Source\<game>\ and put raw clips in it)"
}

foreach ($gameDir in Get-ChildItem $src -Directory) {
    $game = $gameDir.Name.ToLower()
    $outDir = Join-Path $dst $game
    New-Item -ItemType Directory -Force $outDir | Out-Null

    $clips = Get-ChildItem $gameDir.FullName -File | Where-Object { $_.Extension -match '^\.(mp4|mov|mkv|webm)$' }
    foreach ($clip in $clips) {
        # Resource paths must be lowercase [a-z0-9_.-].
        $name = ($clip.BaseName.ToLower() -replace '[^a-z0-9_.-]', '_') + ".mp4"
        $out = Join-Path $outDir $name
        if (-not $Force -and (Test-Path $out) -and (Get-Item $out).LastWriteTime -gt $clip.LastWriteTime) {
            Write-Host "skip  $game/$name"
            continue
        }
        Write-Host "encode $game/$name"
        & ffmpeg -v error -y -i $clip.FullName -t $Seconds `
            -vf "scale=640:360:force_original_aspect_ratio=increase:flags=lanczos,crop=640:360,fps=30,format=gray,format=yuv420p" `
            -c:v libx264 -profile:v baseline -preset slow -crf $Crf -an -movflags +faststart $out
        if ($LASTEXITCODE -ne 0) { throw "ffmpeg failed on $($clip.FullName)" }
    }
}

$total = 0
foreach ($f in Get-ChildItem $dst -Recurse -File -Filter *.mp4) { $total += $f.Length }
Write-Host ("Bundled clips: {0:N1} MB" -f ($total / 1MB))
