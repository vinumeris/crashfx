package controllers

//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// Colour space handling code for the pie chart, converted from the JavaFX code for the same.

data class HSB(val hue: Double, val saturation: Double, val brightness: Double) {
    fun derive(hueShift: Double, saturationFactor: Double, brightnessFactor: Double): HSB {
        /* Allow brightness increase of black color */
        var b = brightness
        if (b == 0.0 && brightnessFactor > 1.0) {
            b = 0.05
        }

        /* the tail "+ 360) % 360" solves shifts into negative numbers */
        val h = (((hue + hueShift) % 360) + 360) % 360
        val s = Math.max(Math.min(saturation * saturationFactor, 1.0), 0.0)
        b = Math.max(Math.min(b * brightnessFactor, 1.0), 0.0)
        return HSB(h, s, b)
    }

    fun brighter() = derive(0.0, 1.0, 1.0 / 0.7)
    fun desaturate() = derive(0.0, 0.7, 1.0)

    fun toRGB(): RGB {
        var hue = hue
        // normalize the hue
        val normalizedHue = ((hue % 360) + 360) % 360
        hue = normalizedHue / 360

        var r: Double = 0.0
        var g: Double = 0.0
        var b: Double = 0.0
        if (saturation == 0.0) {
            r = brightness
            g = brightness
            b = brightness
        } else {
            val h = (hue - Math.floor(hue)) * 6.0
            val f = h - java.lang.Math.floor(h)
            val p = brightness * (1.0 - saturation)
            val q = brightness * (1.0 - saturation * f)
            val t = brightness * (1.0 - (saturation * (1.0 - f)))
            when (h.toInt()) {
                0 -> {
                    r = brightness
                    g = t
                    b = p
                }
                1 -> {
                    r = q
                    g = brightness
                    b = p
                }
                2 -> {
                    r = p
                    g = brightness
                    b = t
                }
                3 -> {
                    r = p
                    g = q
                    b = brightness
                }
                4 -> {
                    r = t
                    g = p
                    b = brightness
                }
                5 -> {
                    r = brightness
                    g = p
                    b = q
                }
            }
        }
        return RGB(r, g, b)
    }
}
data class RGB(val red: Double, val green: Double, val blue: Double) {
    fun toWebString() = "#%x%x%x".format((red * 255).toInt(), (green * 255).toInt(), (blue * 255).toInt())

    fun toHSB(): HSB {
        var cmax = if (red > green) red else green
        if (blue > cmax) cmax = blue
        var cmin = if (red < green) red else green
        if (blue < cmin) cmin = blue

        val brightness = cmax.toDouble()
        val saturation = if (cmax != 0.0)
            (cmax - cmin).toDouble() / cmax
        else
            0.0

        var hue: Double
        if (saturation == 0.0) {
            hue = 0.0
        } else {
            val redc = (cmax - red) / (cmax - cmin)
            val greenc = (cmax - green) / (cmax - cmin)
            val bluec = (cmax - blue) / (cmax - cmin)
            if (red == cmax)
                hue = (bluec - greenc).toDouble()
            else if (green == cmax)
                hue = 2.0 + redc - bluec
            else
                hue = 4.0 + greenc - redc
            hue /= 6.0
            if (hue < 0)
                hue += 1.0
        }
        return HSB(hue * 360, saturation, brightness)
    }
}

val CORNFLOWER_BLUE = RGB(0.39215687, 0.58431375, 0.92941177)