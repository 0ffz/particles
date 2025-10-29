# Force types

All custom forces extend the `Force` class, and are configured similarly. All forces are functions run in a GPU compute
shader, they differ primarily in which parameters they have access to (ex. pairwise functions get the distance between a
particle pair.)

Like the rest of the GPU code in this program, these are written with [KSL](https://github.com/kool-engine/kool?tab=readme-ov-file#kool-shader-language), with examples available in the demo module.

## Defining a force

Extend one of the force type classes (ex. `PairwiseForce`) and implement `createFunction`. These also take a name for your force, which will be used in configs. See also the pages for each force on the sidebar.

Below is a full example for an individual force shader, with explanations following it:

```kotlin
object ConstantForce : IndividualForce("gravity") {
    // define parameters here
    val force = param<Float>("force")
    
    override fun KslIndividualForceFunction.createFunction() {
        // convert to shader parameters here
        val force = force.asShaderParam()
        
        body {
            // KSL shader code here
            float3Value(0f.const, force, 0f.const)
        }
    }
}
```


## Force parameters

Forces can define parameters which will be exposed in the config, as well as the editor as live sliders. These are attached as uniforms, per individual interaction definition (ex. for pairwise functions the config may define one set of parameters for `hydrogen-hydrogen` interactions, and another for `hydrogen-oxygen`).

See the config section of the docs for more info about defining force parameters.


### Defining parameters

Use `param<Float>("paramNameHere")` to declare your function parameters, the type in brackets is a serializable type used to read the parameter from config.

### Using parameters in your shader

To attach a passed parameter to your shader, use the `asShaderParam()` function provided in the `createFunction()` block (ex. `val myShaderParam = myParameter.asShaderParam()`). 

<note>
These are currently only provided for <code>Int</code> and <code>Float</code> parameters, with the 2, 3, 4 dimension versions coming soon. This limitation comes from what types can be passed as uniforms to the GPU.
</note>

