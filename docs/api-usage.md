# Getting started

:info-circle: This section is intended for developers that want to write custom forces or create more complicated
simulations that cannot be made with the Particle HIVE application out of the box.
{ .info }

Particle HIVE is built on [Kotlin](https://kotlinlang.org/), a programming language based on Java that can compile to
different platforms like desktop, mobile, and web. It is built on top
of [Kool engine](https://github.com/kool-engine/kool), a lightweight game engine that lets us write compute shaders that
efficiently run our simulation on the GPU.

Kool introduces an intermediate language for shaders called KSL which lets us programmatically write shaders. Our API
for custom forces is built on this language and lets users extend the simulation without modifying the source code.
