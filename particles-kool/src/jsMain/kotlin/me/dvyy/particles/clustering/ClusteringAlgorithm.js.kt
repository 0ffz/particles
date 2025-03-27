package me.dvyy.particles.clustering

actual fun cluster(
    data: Array<DoubleArray>,
    radius: Double,
    minPts: Int
): IntArray = IntArray(data.size) //TODO find a multiplatform impl for this