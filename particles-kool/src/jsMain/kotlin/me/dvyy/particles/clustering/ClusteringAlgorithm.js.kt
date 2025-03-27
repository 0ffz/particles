package me.dvyy.particles.clustering

actual fun cluster(
    data: Array<DoubleArray>,
    radius: Double,
    minPts: Int
): ClusterInfo = ClusterInfo() //TODO find a multiplatform impl for this