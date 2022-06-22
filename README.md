# Cells Client for Android

[Work In Progress]

## Overview

TODO

### Pro & Cons (in comparison with directly using the web UI on your device)

TODO

## Built with

For the v3 version, we have re-written the Android application from scratch, using Kotlin and latest
Android libraries, mainly Androidx and Jetpack.

The main third party libraries that we rely upon are:

- Cells SDK for Java: a thin wrapper around the swagger generated API that handles low-level
  communication with the remote Pydio servers
- Room: wrapper around SQLite databases
- AWS S3 Sdk: manage file transfer with the remote server S3 API
- Koin: dependency injection
- Material 3: the latest version of Material Design for Android
- Glide: a very efficient library to manage cache and display images

## Contributing

Please read [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) for details on our code of conduct,
and [CONTRIBUTING.md](CONTRIBUTING.md) for details on the process for submitting pull requests to
us. You can find a comprehensive [Developer Guide](https://pydio.com/en/docs/developer-guide) on our
website. Our online docs are open-source as well, feel free to improve them by contributing!

We are also looking for help to translate the Cells interface in various languages. It is really
easy to participate: just navigate
to [our page in the Crowdin translation tool](https://crowdin.com/project/cells-android-client),
create an account and get started.

## Build

## Test

> **Warning**: Most procedures described below are not "backward" compatible:  
> you will have to un-install and fully re-install the application to be able to have the current main released version on your device. Handle with care!

### Beta testing from the store

You can test beta version by registering as a beta tester in the Play Store. After a few minutes,
you should be able to update and run the latest beta version.

You can directly opt-in:

- [from your android device](https://play.google.com/store/apps/details?id=com.pydio.android.Client)
- [from a web browser](https://play.google.com/apps/testing/com.pydio.android.Client): this link is
  also useful when you want to opt out.
