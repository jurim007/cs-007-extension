# Albanian CloudStream Providers

CloudStream providers for Albanian-language movie sources.

## Providers

- **CineShqip** - catalog, search, genres, movie details and embed video sources
- **FilmaOn** - catalog, search, genres, movie details and multiple DooPlayer servers

## Requirements

- Android SDK
- JDK 17

## Build

```bash
ANDROID_HOME="$HOME/Android/Sdk" ANDROID_SDK_ROOT="$HOME/Android/Sdk" \
  ./gradlew :AlbanianProviders:make
```

The plugin artifact is generated at:

`AlbanianProviders/build/AlbanianProviders.cs3`

## CI

The GitHub Actions workflow builds all plugin modules and publishes generated `.cs3` artifacts and `plugins.json`.

## License

Public domain, unless otherwise noted.
