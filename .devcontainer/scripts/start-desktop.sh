#!/usr/bin/env bash
set -euo pipefail

export DISPLAY="${DISPLAY:-:99}"
unset WAYLAND_DISPLAY WAYLAND_SOCKET XDG_RUNTIME_DIR
export GDK_BACKEND=x11
export SDL_VIDEODRIVER=x11
export GLFW_PLATFORM=x11  

# Fedora noVNC web root path
NOVNC_WEB="/usr/share/novnc"
if [ ! -d "$NOVNC_WEB" ]; then
  echo "ERROR: noVNC web root not found at $NOVNC_WEB"
  echo "Try checking where novnc installed files are:"
  rpm -ql novnc | sed -n '1,120p' || true
  exit 1
fi

# Start virtual X server
if ! pgrep -f "Xvfb ${DISPLAY}" >/dev/null 2>&1; then
  nohup Xvfb "${DISPLAY}" -screen 0 1280x720x24 +extension GLX +render -noreset >/tmp/xvfb.log 2>&1 &
fi

# Start a dbus session (XFCE wants it)
if ! pgrep -u "$(id -u)" -f "dbus-daemon.*--session" >/dev/null 2>&1; then
  # shellcheck disable=SC2046
  eval "$(dbus-launch --sh-syntax)"
fi

# Start XFCE session
if ! pgrep -u "$(id -u)" -x xfce4-session >/dev/null 2>&1; then
  nohup xfce4-session >/tmp/xfce4-session.log 2>&1 &
fi

# Start VNC server (no password; fine for Codespaces)
if ! pgrep -f "x11vnc.*-rfbport 5900" >/dev/null 2>&1; then
  nohup x11vnc -display "${DISPLAY}" -nopw -forever -shared -rfbport 5900 >/tmp/x11vnc.log 2>&1 &
fi

# Start noVNC (websockify)
if ! pgrep -f "websockify.*6080" >/dev/null 2>&1; then
  python3 -m websockify --web="${NOVNC_WEB}" 6080 localhost:5900
fi

