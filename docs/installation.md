# Installation

## On Desktop

- Download the [latest release](https://github.com/0ffz/particles/releases/latest)
- Install Java 21 or above (ex. from [here](https://adoptium.net/))
- Run in a terminal with the following command:
  - Windows/Linux: `java -jar path/to/particles.jar`
  - MacOS: `java -jar path/to/particles.jar -XstartOnFirstThread`

[//]: # (- On older hardware, you may pass the <code>--opengl</code> argument at the end of the line above.)

## In browser

Particles can run on any browser that supports WebGPU.
Currently this is best supported on Chromium-based browsers, with Firefox recently adding support.

- Install a Chromium based browser _(ex. Google Chrome, Edge, Brave, etc...)_
- On Linux, you may need to enable the <a href="chrome://flags/#enable-vulkan">Vulkan</a>
  and <a href="chrome://flags/#enable-unsafe-webgpu">unsafe WebGPU support</a> flags
- Open the <a href="https://particles.dvyy.me">online demo</a>
