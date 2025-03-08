# Pairwise

A symmetrical force applied to a pair of particles.

## Parameters

- `distance: Float`, the distance between the pair of particles.

## Returns

A `Float` as a 1-dimensional force between these two particles, (i.e. either directly attracting, or directly repulsing). Calculated as a sum of all nearby particles within a distance of `minGridSize` of this particle.
