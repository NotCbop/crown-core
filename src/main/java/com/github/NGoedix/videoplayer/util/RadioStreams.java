package com.github.NGoedix.videoplayer.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RadioStreams {

    private static final Map<String, List<RadioStream>> radioStreams = new HashMap<>();

    public static void prepareRadios() {
        addRadio("Spain", "Cadena SER", "https://playerservices.streamtheworld.com/api/livestream-redirect/CADENASER.mp3");
        addRadio("Spain", "COPE", "https://flucast09-h-cloud.flumotion.com/cope/net1.mp3");
        addRadio("Spain", "Onda Cero", "https://atres-live.ondacero.es/live/ondacero/master.m3u8");
        addRadio("Spain", "Los 40", "https://playerservices.streamtheworld.com/api/livestream-redirect/Los40.mp3");
        addRadio("Spain", "Los 40 Urban", "https://playerservices.streamtheworld.com/api/livestream-redirect/LOS40_URBAN.mp3");
        addRadio("Spain", "Los 40 Classic", "https://playerservices.streamtheworld.com/api/livestream-redirect/LOS40_CLASSIC.mp3");
        addRadio("Spain", "Cadena 100", "https://cadena100-cope.flumotion.com/chunks.m3u8");
        addRadio("Spain", "Cadena Dial", "https://playerservices.streamtheworld.com/api/livestream-redirect/CADENADIAL.mp3");
        addRadio("Spain", "Kiss FM", "https://kissfm.kissfmradio.cires21.com/kissfm.mp3");
        addRadio("Spain", "Rock FM", "https://rockfm-cope.flumotion.com/playlist.m3u8");

        addRadio("Mexico", "Los 40 Principales", "http://sigmaradio.stream:9004/index.mp3");
        addRadio("Mexico", "Ke Buena", "http://sigmaradio.stream:9002/index.mp3");
        addRadio("Mexico", "La Z", "https://26273.live.streamtheworld.com/XHEM_FM.mp3");
        addRadio("Mexico", "EXA FM", "https://26373.live.streamtheworld.com/XHEXA_SC");
        addRadio("Mexico", "Amor FM", "https://18243.live.streamtheworld.com/XHSHFMAAC");
        addRadio("Mexico", "Radio Fórmula", "https://us-b4-p-e-qg12-audio.cdn.mdstrm.com/live-audio-aw-bkp/61e1dfd2658baf082814e25d");
        addRadio("Mexico", "MVS Noticias", "https://playerservices.streamtheworld.com/api/livestream-redirect/XHMVSFM_SC");
        addRadio("Mexico", "Stereo Joya", "https://playerservices.streamtheworld.com/api/livestream-redirect/XEJP_FMAAC_SC?dist=tg&pname=TDSdk");
        addRadio("Mexico", "Beat FM", "https://playerservices.streamtheworld.com/api/livestream-redirect/XHSONFMAAC_SC");
        addRadio("Mexico", "Imagen Radio", "https://26363.live.streamtheworld.com/XEDAFM_SC");

        addRadio("United States", "NPR", "https://npr-ice.streamguys1.com/live.mp3");
        addRadio("United States", "The Jazz Grove", "http://audio-edge-5bkfj.fra.h.radiomast.io/8a384ff3-6fd1-4e5d-b47d-0cbefeffe8d7");
        addRadio("United States", "Top 40 hits - 2000", "http://bigrradio-edge1.cdnstream.com/5106_128");
        addRadio("United States", "Top 40 - 100hitz", "https://pureplay.cdnstream1.com/6025_128.mp3");
        addRadio("United States", "Hard Radio", "http://2217.cloudrad.io/;stream");
        addRadio("United States", "Hip Hop - 100hitz", "https://pureplay.cdnstream1.com/6042_128.mp3");
        addRadio("United States", "Reggae141", "http://hestia2.cdnstream.com/1301_128");

        addRadio("United Kingdom", "BBC World Service", "http://stream.live.vc.bbcmedia.co.uk/bbc_world_service");
        addRadio("United Kingdom", "KMFM", "https://listen-kmfm.sharp-stream.com/kmfmdab.mp3");

        addRadio("Germany", "Kiss FM", "http://topradio-stream03.radiohost.de/kissfm_mp3-128");
        addRadio("Germany", "Big FM", "https://stream.bigfm.de/deutschland/aac-128/twl");
        addRadio("Germany", "Radio Paloma", "https://pool.radiopaloma.de/RADIOPALOMA.mp3");
        addRadio("Germany", "FFN", "http://ffn-stream21.radiohost.de/ffn_mp3-192");
        addRadio("Germany", "Energy Berlin", "https://edge69.streamonkey.net/energy-berlin/stream/mp3");

        addRadio("France", "Europe 1", "https://europe1.lmn.fm/europe1.mp3");
        addRadio("France", "RFM", "https://rfm.lmn.fm/rfm-live/playlist.m3u8");
        addRadio("France", "Skyrock", "https://icecast.skyrock.net/s/natio_aac_128k");
        addRadio("France", "Fun Radio", "http://streamer-02.rtl.fr/fun-1-44-128");

        addRadio("Italy", "Radio Italia", "https://radioitaliasmi.akamaized.net/hls/live/2093120/RISMI/stream01/streamPlaylist.m3u8");
        addRadio("Italy", "Venice Classic", "https://uk2.streamingpulse.com/ssl/vcr2");
        addRadio("Italy", "Radio kiss kiss", "https://kisskiss.fluidstream.eu/KKItalia.aac");
        addRadio("Italy", "Latte Miele", "https://sr15.inmystream.it/stream/lattemiele");

        addRadio("Brazil", "Radio Globo", "https://playerservices.streamtheworld.com/api/livestream-redirect/RADIO_GLOBO_RJAAC.aac");
        addRadio("Brazil", "Radio Azul", "https://r16.ciclano.io:15004/stream?1722982939300");
        addRadio("Brazil", "Radio Antena", "https://antenaone.crossradio.com.br/stream/1/");

        addRadio("Custom", "", "");
    }

    public static void addRadio(String countryName, String radioName, String streamLink) {
        radioStreams.computeIfAbsent(countryName, k -> new ArrayList<>()).add(new RadioStream(radioName, streamLink));
    }

    public static Map<String, List<RadioStream>> getRadioStreams() {
        return radioStreams;
    }

    public static class RadioStream {
        private final String radioName;
        private final String streamLink;

        public RadioStream(String radioName, String streamLink) {
            this.radioName = radioName;
            this.streamLink = streamLink;
        }

        public String getRadioName() {
            return radioName;
        }

        public String getStreamLink() {
            return streamLink;
        }
    }
}