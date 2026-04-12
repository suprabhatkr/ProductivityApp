# App module notes

Routing and navigation

- The canonical source for route names is `com.example.productivityapp.navigation.AppRoutes`.
- Do NOT use hard-coded route strings such as `navController.navigate("home")` or `composable("water")`.
  Use `AppRoutes.HOME`, `AppRoutes.WATER`, etc.

Build-time safeguard

- A Gradle verification task `verifyNoHardcodedRoutes` runs automatically before Kotlin compilation.
  It scans Kotlin sources for usages of `navController.navigate("...")` or `composable("...")` with the route names
  `home`, `steps`, `run`, `sleep`, `water`, or `settings`. If any are found outside of `AppRoutes.kt` or legacy markers,
  the build will fail with a helpful message.

If you need to add a new route:

1. Add it to `AppRoutes.kt`.
2. Update `MainActivity`/navigation host to register the route using `AppRoutes`.
3. Update any callers to use the named constant.

