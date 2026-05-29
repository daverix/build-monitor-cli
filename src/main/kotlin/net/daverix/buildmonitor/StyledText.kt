package net.daverix.buildmonitor

enum class BackgroundColor(val color: Int) {
    Default(0),
    Black(40),
    Red(41),
    Green(42),
    Yellow(43),
    Blue(44),
    Magenta(45),
    Cyan(46),
    White(47),
    BlackBright(100),
    RedBright(101),
    GreenBright(102),
    YellowBright(103),
    BlueBright(104),
    MagentaBright(105),
    CyanBright(106),
    WhiteBright(107),
}

enum class TextColor(val color: Int) {
    Default(0),
    Black(30),
    Red(31),
    Green(32),
    Yellow(33),
    Blue(34),
    Magenta(35),
    Cyan(36),
    White(37),
    BlackBright(90),
    RedBright(91),
    GreenBright(92),
    YellowBright(93),
    BlueBright(94),
    MagentaBright(95),
    CyanBright(96),
    WhiteBright(97),
}

fun styled(
    text: String,
    bold: Boolean = false,
    strike: Boolean = false,
    italic: Boolean = false,
    underline: Boolean = false,
    color: TextColor = TextColor.Default,
    background: BackgroundColor = BackgroundColor.Default,
): String = StringBuilder().apply {
    append("\u001B[${background.color};${color.color}")

    if(bold) append(";1")
    if(italic) append(";3")
    if(underline) append(";4")
    if(strike) append(";9")

    append("m${text}\u001B[0m")
}.toString()