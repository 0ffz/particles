package me.dvyy.particles.clustering

import smile.clustering.dbscan

actual fun cluster(
    data: Array<DoubleArray>,
    radius: Double,
    minPts: Int
): ClusterInfo {
    val scan = dbscan(data, minPts, radius)
    return ClusterInfo(
        clusters = scan.group(),
        sizes = scan.size(),
        count = scan.k()
    )
}