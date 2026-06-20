package com.github.NGoedix.videoplayer.util.displayers;

import java.awt.*;

public interface IDisplay {

    String getUrl();

    int prepare(String url, boolean playing, boolean loop, int tick);

    void tick(String url, float volume, float minDistance, float maxDistance, boolean playing, boolean loop, int tick);

    default int maxTick() {
        return 0;
    }

    void pause(int tick);

    void resume(int tick);

    int getRenderTexture();

    boolean isPlaying();

    boolean isStopped();

    void release();

    void stop();

    Dimension getDimensions();
}
