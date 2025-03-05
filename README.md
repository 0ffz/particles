# Particles

A GPU particle simulation built on [Kool Engine](https://github.com/kool-engine/kool).
The goal is to provide a simple API for writing arbitrary particle interactions as [compute shaders](https://learnopengl.com/Guest-Articles/2022/Compute-Shaders/Introduction).

These can run efficiently on most consumer hardware, including in browser using WebGPU.
We provide a compiled version which uses Lennard Jones potential with parameters that can be configured live in the application.

## Features

- Pairwise interactions that can be configured differently for any pair of particle types
- UI for editing simulation parameters parameters live
- 2D/3D simulation

## About the simulation

The particle simulation runs entirely on the GPU, with Kotlin code to setup the GPU pipeline,
create shaders that compile down to different targets (OpenGL, Vulkan, WebGPU), and for application UI.

The pipeline on the GPU currently looks like this:

- Split particles into grid cells and sort them to be close together in memory
- Calculate net forces on particles by iterating through particles pairs (with n-particle interactions planned)
- Update positions and velocities using the velocity Verlet algorithm
- Every n steps, render all particles using instanced rendering

## Running

- Download the [latest release](https://github.com/0ffz/particles/releases/latest)
- Install Java 21 or above (ex. from [here](https://adoptium.net/))
- Run in a terminal with `java -jar path/to/particles.jar`

A browser runnable version is coming soon.

### Developers

- Clone this repository
- Run with `./gradlew run` (or `gradlew.bat run` on Windows)
