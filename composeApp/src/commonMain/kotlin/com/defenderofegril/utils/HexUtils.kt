package com.defenderofegril.utils

// Kotlin
import androidx.compose.ui.geometry.Offset
import com.defenderofegril.model.Position
import kotlin.math.sqrt

/**
 * Converts a screen position to hex grid Position (even-q vertical layout).
 *
 * @param pointerPos Screen position (Offset)
 * @param offsetX Current pan offset X
 * @param offsetY Current pan offset Y
 * @param zoomLevel Current zoom level
 * @param hexSize Size of hexagon (in dp or px, must match rendering)
 * @return Position? - null if out of bounds
 */
fun screenToHexGridPosition(
    pointerPos: Offset,
    offsetX: Float,
    offsetY: Float,
    zoomLevel: Float,
    hexSize: Float
): Position? {
    //println("offsetY: $offsetY")


    // Adjust for pan and zoom
    val px = (pointerPos.x - offsetX) / zoomLevel
    val py = (pointerPos.y - offsetY) / zoomLevel

    // Hex geometry
    val w = sqrt(3f) * hexSize
    val h = hexSize * 2f

    // Calculate column (q) and row (r) for even-q vertical layout
    val q = ((px * 2f/3f) / hexSize).toInt()
    val r = ((py - ((q % 2) * h) / 2f) / h).toInt()


    val z = ((py * 2f/3f) / (hexSize - 20)).toInt()
    val z2 = ((py * 2f/3f) / (hexSize - 0)).toInt()
    val rz = ((py - ((q % 2) * ((hexSize - 20)) * 2f) / 2f) / (hexSize - 20)).toInt()
    val rz2 = ((py - ((q % 2) * ((hexSize - 0)) * 2f) / 2f) / (hexSize - 20)).toInt()
    val rz3 = ((py - ((q % 2) * ((hexSize - 20)) * 2f) / 2f) / (hexSize - 0)).toInt()

    println("screenToHexGridPosition: zoomLevel=$zoomLevel px=$px, py=$py, h=$h => q=$q, r=$r")
    println("hexSize: $hexSize\n" +
            "py: $py \n" +
            "pointerPos.y: ${pointerPos.y}\n" +
            "offsetY: $offsetY\n" +
            "q modulo: ${(q % 2)} \n" +
            "h: $h \n" +
            "negative: ${ (q % 2) * h / 2f }\n" +
            "divided: ${py - (q % 2) * h / 2f}\n" +
            "gesamt: ${(py - (q % 2) * h / 2f) /h}")
    val alternativePos = pixelToPointyHex(px, py, hexSize)
    println("Alternative calc: $alternativePos")

    val alternativePos2 = pixelToPointyHex(px, py, 36f)
    println("Alternative calc: $alternativePos2")

    println("z: $z")
    println("z2: $z2")
    println("rz: $rz")
    println("rz2: $rz2")
    println("rz3: $rz3")

    return Position(q, alternativePos2.y)
}
/*
    function pixel_to_pointy_hex(point):
    // invert the scaling
    var x = point.x / size
    var y = point.y / size
    // cartesian to hex
    var q = (sqrt(3)/3 * x  -  1./3 * y)
    var r = (                  2./3 * y)
    return axial_round(Hex(q, r))
 */
fun pixelToPointyHex(x: Float, y: Float, size: Float): Position {
    val nx = x / size
    val ny = y / size

    val q = (sqrt(3f)/3f * nx  -  1f/3f * ny)
    val r = (2f/3f * ny)

    return axialRound(q, r)
}


/*
function cube_round(frac):
    var q = round(frac.q)
    var r = round(frac.r)
    var s = round(frac.s)

    var q_diff = abs(q - frac.q)
    var r_diff = abs(r - frac.r)
    var s_diff = abs(s - frac.s)

    if q_diff > r_diff and q_diff > s_diff:
        q = -r-s
    else if r_diff > s_diff:
        r = -q-s
    else:
        s = -q-r

    return Cube(q, r, s)
 */
fun cubeRound(q: Float, r: Float, s: Float): Triple<Int, Int, Int> {
    var rq = kotlin.math.round(q)
    var rr = kotlin.math.round(r)
    var rs = kotlin.math.round(s)

    val qDiff = kotlin.math.abs(rq - q)
    val rDiff = kotlin.math.abs(rr - r)
    val sDiff = kotlin.math.abs(rs - s)

    if (qDiff > rDiff && qDiff > sDiff) {
        rq = -rr - rs
    } else if (rDiff > sDiff) {
        rr = -rq - rs
    } else {
        rs = -rq - rr
    }

    return Triple(rq.toInt(), rr.toInt(), rs.toInt())
}

/*
function axial_round(hex):
    return cube_to_axial(cube_round(axial_to_cube(hex)))
 */
fun axialRound(q: Float, r: Float): Position {
    val s = -q - r
    val (rq, rr, _) = cubeRound(q, r, s)
    return Position(rq, rr)
}
