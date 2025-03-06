# Configuration

The application loads a configuration file which defines application options, particle interactions, and default parameter values.

Some parameters can be edited live, these will show up as a slider, but others like particle count need a reload, and thus can only be edited in the config file. The config file is never updated, so their values are stored as overrides, with a button to reset them to the configuration defaults.

## Config file definition

```yaml
[[[simulation|#simulation]]]: 
  count: 10000
  minGridSize: 5.0
  dT: 0.001
  conversionRate: 100
  maxVelocity: 1.21
  maxForce: 100000.0
  threeDimensions: false
  passesPerFrame: 100
  size:
    width: 1000
    height: 1000
    depth: 1000
[[[particles|#particles]]]:
  hydrogen:
    color: ffffff
    radius: 1.0
    distribution: 2
  oxygen:
    color: ff0000
    radius: 1.5
    distribution: 1
    convertTo:
      type: hydrogen
      chance: 0.01
[[[interactions|#interactions]]]:
  hydrogen-oxygen:
    lennardJones:
      sigma: [[[!param;max=10|#parameters]]] 5.0
      epsilon: !param;max=500 5.0
  hydrogen-hydrogen:
    lennardJones:
      sigma: !param;max=10 5.0
      epsilon: !param;max=500 5.0
  oxygen-oxygen:
    lennardJones:
      sigma: !param;max=10 10.0
      epsilon: !param;max=500 5.0
```

### Simulation {id="simulation"}

### Particles {id="particles"}

### Interactions {id="interactions"}

### Parameters {id="parameters"}
