# Creating a project

Adding custom forces requires compiling this project with them added on top. We publish the `particles-kool` module, which includes an api to create your forces and initialize the program.

`particles-demo` is an example project that depends on the engine and adds several example forces. We'll try to provide a full template project to copy soon, however if you are familiar with gradle it's possible to get started with its `build.gradle.kts` as a reference.

<tip>
In the meantime feel free to experiment by cloning this repository and working directly in the demo project.
</tip>

## Initializing the simulation

Use the `launchParticles` function to configure the simulation and start it. The function takes a list of your custom forces as a parameter, after which they can be used in configs.

The demo module shows an example with the main class located in `commonMain`.
