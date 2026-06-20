package com.github.NGoedix.videoplayer.util.math;

public class VideoMathUtil {

    public static float calculateVolume(float volume, float distance, float minDistance, float maxDistance) {
        if (distance <= minDistance) {
            return volume;
        } else if (distance >= maxDistance) {
            return 0;
        } else {
            float fraction = (distance - minDistance) / (maxDistance - minDistance);
            return volume * (1 - fraction);
        }
    }

    private static final int SIN_SIZE = 65536;
    private static final float[] SIN = new float[SIN_SIZE];

    static {
        for (int i = 0; i < SIN_SIZE; i++) {
            SIN[i] = (float) Math.sin(i * Math.PI * 2.0 / SIN_SIZE);
        }
    }

    public static double easeIn(double start, double end, double t) {
        return start + (end - start) * t * t;
    }

    public static double easeOut(double start, double end, double t) {
        return start + (end - start) * (1 - Math.pow(1 - t, 2));
    }

    public static double easeInOut(double start, double end, double t) {
        return t < 0.5 ? easeIn(start, end / 2, t * 2) : easeOut(start + (end / 2), end, (t - 0.5) * 2);
    }

    public static double easeOutIn(double start, double end, double t) {
        return t < 0.5 ? easeOut(start, end / 2, t * 2) : easeIn(start + (end / 2), end, (t - 0.5) * 2);
    }

    public static double easeInCircle(double start, double end, double t) {
        return start + (end - start) * (1 - Math.sqrt(1 - t * t));
    }

    public static double easyEase(double start, double end, double t) {
        return start + (end - start) * ((t < 0.5) ? 2 * t * t : -1 + 2 * t * (2 - t));
    }

    public static double easeInSine(double start, double end, double t) {
        return (end - start) * (1 - cos((float) (t * Math.PI / 2))) + start;
    }

    public static double easeInCubic(double start, double end, double t) {
        return (end - start) * (t * t * t) + start;
    }

    public static double easeInQuint(double start, double end, double t) {
        return (end - start) * (t * t * t * t * t) + start;
    }

    public static double easeInElastic(double start, double end, double t) {
        if (t == 0) {
            return start;
        } else if (t == 1) {
            return end;
        }
        double c4 = (2 * Math.PI) / 3;
        return start - Math.pow(2, 10 * t - 10) * sin((float) ((t * 10 - 10.75) * c4)) * (end - start);
    }

    public static double easeOutSine(double start, double end, double t) {
        return (end - start) * sin((float) (t * Math.PI / 2)) + start;
    }

    public static double easeOutCubic(double start, double end, double t) {
        return (end - start) * (1 - Math.pow(1 - t, 3)) + start;
    }

    public static double easeOutQuint(double start, double end, double t) {
        return (end - start) * (1 - Math.pow(1 - t, 5)) + start;
    }

    public static double easeOutCircle(double start, double end, double t) {
        return (end - start) * Math.sqrt(1 - Math.pow(t - 1, 2)) + start;
    }

    public static double easeOutElastic(double start, double end, double t) {
        if (t == 0) {
            return start;
        } else if (t == 1) {
            return end;
        }
        return (end - start) * (Math.pow(2, -10 * t) * sin((float) ((t * 10 - 0.75) * ((2 * Math.PI) / 3))) + 1) + start;
    }

    public static double easeInOutSine(double start, double end, double t) {
        return (end - start) * (-(cos((float) (Math.PI * t)) - 1) / 2) + start;
    }

    public static double easeInOutCubic(double start, double end, double t) {
        if (t < 0.5) {
            return (end - start) * (4 * t * t * t) + start;
        } else {
            return (end - start) * (1 - Math.pow(-2 * t + 2, 3) / 2) + start;
        }
    }

    public static double easeInOutQuint(double start, double end, double t) {
        if (t < 0.5) {
            return (end - start) * (16 * Math.pow(t, 5)) + start;
        } else {
            return (end - start) * (1 - Math.pow(-2 * t + 2, 5) / 2) + start;
        }
    }

    public static double easeInOutCircle(double start, double end, double t) {
        if (t < 0.5) {
            return start + (end - start) * (1 - Math.sqrt(1 - Math.pow(2 * t, 2))) / 2;
        } else {
            return start + (end - start) * (Math.sqrt(1 - Math.pow(-2 * t + 2, 2)) + 1) / 2;
        }
    }

    public static double easeInOutElastic(double start, double end, double t) {
        if (t == 0) {
            return start;
        } else if (t == 1) {
            return end;
        }
        double c5 = (2 * Math.PI) / 4.5;
        if (t < 0.5) {
            return start - (Math.pow(2, 20 * t - 10) * sin((float) ((20 * t - 11.125) * c5))) / 2;
        } else {
            return start + (Math.pow(2, -20 * t + 10) * sin((float) ((20 * t - 11.125) * c5))) / 2 + (end - start);
        }
    }

    public static double easeInQuad(double start, double end, double t) {
        return (end - start) * (t * t) + start;
    }

    public static double easeInQuart(double start, double end, double t) {
        return (end - start) * (t * t * t * t) + start;
    }

    public static double easeInExpo(double start, double end, double t) {
        if (t == 0) {
            return start;
        }
        return (end - start) * (Math.pow(2, 10 * t - 10)) + start;
    }

    public static double easeInBack(double start, double end, double t) {
        final double c1 = 1.70158;
        final double c3 = c1 + 1;
        return (end - start) * (c3 * t * t * t - c1 * t * t) + start;
    }

    public static double easeInBounce(double start, double end, double t) {
        return start + (end - start) * (1 - easeOutBounce(0, 1, 1 - t));
    }

    public static double easeOutBounce(double start, double end, double t) {
        final double n1 = 7.5625;
        final double d1 = 2.75;
        double value;

        if (t < 1 / d1) {
            value = n1 * t * t;
        } else if (t < 2 / d1) {
            t -= 1.5 / d1;
            value = n1 * t * t + 0.75;
        } else if (t < 2.5 / d1) {
            t -= 2.25 / d1;
            value = n1 * t * t + 0.9375;
        } else {
            t -= 2.625 / d1;
            value = n1 * t * t + 0.984375;
        }

        return start + (end - start) * value;
    }

    public static double easeOutQuad(double start, double end, double t) {
        return start + (end - start) * (1 - Math.pow(1 - t, 2));
    }

    public static double easeOutQuart(double start, double end, double t) {
        return start + (end - start) * (1 - Math.pow(1 - t, 4));
    }

    public static double easeOutExpo(double start, double end, double t) {
        if (t == 1) {
            return end;
        }
        return start + (end - start) * (1 - Math.pow(2, -10 * t));
    }

    public static double easeOutBack(double start, double end, double t) {
        final double c1 = 1.70158;
        final double c3 = c1 + 1;
        double adjustedT = t - 1;
        return start + (end - start) * (1 + c3 * Math.pow(adjustedT, 3) + c1 * Math.pow(adjustedT, 2));
    }

    public static double easeInOutQuad(double start, double end, double t) {
        if (t < 0.5) {
            return start + (end - start) * (2 * t * t);
        } else {
            return start + (end - start) * (1 - Math.pow(-2 * t + 2, 2) / 2);
        }
    }

    public static double easeInOutQuart(double start, double end, double t) {
        if (t < 0.5) {
            return start + (end - start) * (8 * Math.pow(t, 4));
        } else {
            return start + (end - start) * (1 - Math.pow(-2 * t + 2, 4) / 2);
        }
    }

    public static double easeInOutExpo(double start, double end, double t) {
        if (t == 0) return start;
        if (t == 1) return end;

        if (t < 0.5) {
            return start + (end - start) * (Math.pow(2, 20 * t - 10) / 2);
        } else {
            return start + (end - start) * ((2 - Math.pow(2, -20 * t + 10)) / 2);
        }
    }

    public static double easeInOutBack(double start, double end, double t) {
        final double c1 = 1.70158;
        final double c2 = c1 * 1.525;

        if (t < 0.5) {
            return start + (Math.pow(2 * t, 2) * ((c2 + 1) * 2 * t - c2)) / 2 * (end - start);
        } else {
            return start + (Math.pow(2 * t - 2, 2) * ((c2 + 1) * (2 * t - 2) + c2) + 2) / 2 * (end - start);
        }
    }

    public static double easeInOutBounce(double start, double end, double t) {
        if (t < 0.5) {
            return start + (end - start) * (1 - easeOutBounce(0, 1, 1 - 2 * t)) / 2;
        } else {
            return start + (end - start) * (1 + easeOutBounce(0, 1, 2 * t - 1)) / 2;
        }
    }

    public static float sin(float pValue) {
        return SIN[(int)(pValue * 10430.378F) & 0xFFFF];
    }

    public static float cos(float pValue) {
        return SIN[(int)(pValue * 10430.378F + 16384.0F) & '\uffff'];
    }
}
