package me.dvyy.particles.clustering

expect fun cluster(data: Array<DoubleArray>, radius: Double, minPts: Int): ClusterInfo

class ClusterInfo(
    val clusters: IntArray = intArrayOf(),
    val sizes: IntArray = intArrayOf(),
    val count: Int = 0,
) {
}