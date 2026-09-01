# react-native-stera

## Getting started

`$ npm install react-native-stera --save`

### Mostly automatic installation

`$ react-native link react-native-stera`

## Usage
```javascript
import Stera from 'react-native-stera';

// TODO: What to do with the module?
Stera;
```

## Supported devices

Panasonic stera (`JT-C60`, `JT-C61`) and tance (`JT-VT10`). Detection matches the
`JT-<letters><digits>` family and then confirms by resolving the PaymentApi service,
rather than checking against a list of known model names — an exact list left this
module silently inert on the `JT-C61`, and Panasonic's app development guideline
(JT-C60/C61 v2.02 §3.3.3.3) warns that the model name changes between generations.

Off a terminal, every method is a no-op: `initialize()` returns early, `isSupported()`
resolves false, and `getConstants().isStera` is false. Read the constant rather than
awaiting `isSupported()` if you need to branch during a first render.

## Tests

The unit tests under `android/src/test` cover the decision logic that has no business
touching a device: model matching, the settle-once promise bookkeeping, and null-Intent
handling in `onActivityResult`.

There is no standalone Gradle build here — `android/` has no `settings.gradle`, its
wrapper is Gradle 6.7 (which will not run on a modern JDK), and it resolves React
Native out of a consuming app's `node_modules`. So run the tests from an app that
depends on this module:

```
./gradlew :SeteMares_react-native-stera:testDebugUnitTest
```
