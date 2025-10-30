# Gravity

Applies a constant downward force, defined by the `force` parameter.

Note: on the atomic scale this is nearly negligible. This force is only included for demonstration purposes.
{ .info }

## Example usage

```yaml
particles:
  a:
    color: ffffff
    radius: 2
    distribution: 1
interactions:
  gravity:
    a: { force: 0.005 }
```