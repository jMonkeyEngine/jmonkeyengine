FROM registry.fedoraproject.org/fedora:43

RUN dnf -y update && \
    dnf -y install \
      git curl unzip which procps-ng psmisc findutils \
    alsa-lib alsa-plugins-pulseaudio \
    pulseaudio-libs pulseaudio-utils \
      # X + desktop
      xorg-x11-server-Xvfb xterm dbus-x11 \
      xfce-polkit \
      xfce4-session xfce4-panel xfce4-settings xfce4-terminal thunar \
      xfwm4 \
      feh \
      # VNC + web VNC
      x11vnc novnc python3-websockify \
      # Xpra + dependencies (xpra-html5 not in Fedora repos; installed below from source)
      xpra python3-gobject python3-paramiko \
      # OpenGL (Mesa) + GLVND + tools (glxinfo)
      mesa-dri-drivers mesa-libGL mesa-libEGL mesa-libGLU \
      libglvnd-glx libglvnd-egl \
      glx-utils zip weston xorg-x11-server-Xwayland wayland-utils \
    && dnf clean all



ENV SDKMAN_DIR=/root/.sdkman
RUN curl -s "https://get.sdkman.io" | bash \
  && source "$SDKMAN_DIR/bin/sdkman-init.sh" \
  && sdk install java 25-tem \
  && sdk default java 25-tem

ENV JAVA_HOME=${SDKMAN_DIR}/candidates/java/current
ENV PATH=${JAVA_HOME}/bin:${PATH}


COPY assets/wallpaper.jpg /root/wallpaper.jpg
COPY assets/novnc-index.html /usr/share/novnc/index.html
 