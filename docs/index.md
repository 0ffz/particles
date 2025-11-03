---
template: landing
title: Documentation
desc: A realtime molecular dynamics simulation
items:
  - title: User guide
    desc: Install and run Particle HIVE
    icon: school
    url: /installation
  - title: Developer docs
    desc: Create custom simulations
    icon: code
    url: /api-usage
#  - title: About
#    icon: info-circle
#    desc: Learn more about this project
#    url: /about
---

<picture>
  <source media="(prefers-color-scheme: dark)" srcset="assets/banner-dark.png"></source>
  <source media="(prefers-color-scheme: light)" srcset="assets/banner-light.png"></source>
  <img src="assets/banner-light.png"></img>
</picture>

_Particle HIVE_ is an open source tool to run particle force simulations on the GPU. We provide some atomic potentials
and a Kotlin API for custom simulations. The WebGPU target allows less technical users to run demos in their browser
without the need for special hardware like an external graphics card.
