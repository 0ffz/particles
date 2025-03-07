package me.dvyy.particles

import me.dvyy.particles.compute.forces.PairwiseForce

expect fun launchParticles(forces: List<PairwiseForce>)
