# Tersoff Simplified

Simple version of the Tersoff potential introduced in [(Tersoff, 1988)](https://doi.org/10.1103/PhysRevB.37.6991). We
take the form which does not consider angles between pairs of particles, instead it diminishes the force between
particles as a particle gets more neighbours.

:info-circle: Adjusting neighbour calculation is currently not possible via config, but will be added in the future.
{ .info }

## Example usage

```yaml
particles:
  ligand:
    color: ffffff
    radius: 1.5
interactions:
  tersoff_simple:
    ligand-ligand: { beta: 0.33675, n: 22.956, A: 3264.7, B: 95.373, lambda1: 3.2394, lambda2: 1.3258 }
```