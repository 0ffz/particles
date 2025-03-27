package me.dvyy.particles.clustering

import smile.clustering.dbscan

actual fun cluster(
    data: Array<DoubleArray>,
    radius: Double,
    minPts: Int
): IntArray {
    return dbscan(data, minPts, radius).group()
}