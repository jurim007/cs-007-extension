# 007

Independent CloudStream plugins for Albanian-language movie and series sources.

## Providers

- **CineShqip** - movie and series catalog/search, episode playback, and alternate video servers
- **FilmaOn** - movie and series catalog/search, episode playback, and alternate DooPlayer servers

## Requirements

- Android SDK
- JDK 17

## Build

```bash
ANDROID_HOME="$HOME/Android/Sdk" ANDROID_SDK_ROOT="$HOME/Android/Sdk" \
  ./gradlew :CineShqip:make :FilmaOn:make makePluginsJson
```

The plugins are generated separately:

- `CineShqip/build/CineShqip.cs3`
- `FilmaOn/build/FilmaOn.cs3`

## Add the repository to CloudStream

Add this repository URL in CloudStream:

`https://raw.githubusercontent.com/jurim007/cs-007-extension/builds/repo.json`

## CI

The GitHub Actions workflow builds all plugin modules and publishes generated `.cs3` artifacts and `plugins.json`.

## License

Public domain, unless otherwise noted.
