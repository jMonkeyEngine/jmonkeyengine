FROM registry.fedoraproject.org/fedora:43

RUN dnf -y update && \
    dnf -y install \
      git curl unzip which procps-ng psmisc findutils \
      java-21-openjdk-devel \
      # X + desktop
      xorg-x11-server-Xvfb xterm dbus-x11 \
      xfce4-session xfce4-panel xfce4-settings xfce4-terminal thunar \
      # VNC + web VNC
      x11vnc novnc python3-websockify \
      # OpenGL (Mesa) + GLVND + tools (glxinfo)
      mesa-dri-drivers mesa-libGL mesa-libEGL mesa-libGLU \
      libglvnd-glx libglvnd-egl \
      glx-utils \
    && dnf clean all


RUN dnf -y install weston xorg-x11-server-Xwayland wayland-utils \
    && dnf clean all