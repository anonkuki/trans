@echo off
chcp 65001 >nul
title PDF翻译服务 - 本地运行
echo 正在启动 PDF 翻译服务...
echo 启动完成后访问: http://localhost:8040/docs
echo 按 Ctrl+C 可停止服务。
echo.
wsl -d Ubuntu -- bash -lc "cd /mnt/d/codeC/python/翻译系统/pdf-translation && set -a && source .env && set +a && source .venv-wsl/bin/activate && python app.py"
echo.
echo 服务已停止。
pause
