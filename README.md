# Cells Client for Android

Pydio Cells Android application is a light Android Client to manage your files on your Android
Device.

## A custom Android app, why?

On your Android device, you can use any web browser to directly interact with your distant Cells
server with _"usual"_ user interface. The web application is reactive and can be easily used like
you would do from your personal computer, only on a smaller screen.

With a limited subset of features, the Android application can yet prove to be useful because it has
been optimized for mobile usage:

- it manages and caches authorization, no need to re-log at each connection
- it has a aggressive cache optimisation strategy to avoid downloading the files when not necessary
- you can configure what you download depending on your connection status (e.g. only download
  thumbnails when you are on an un-metered network)
- when offline, you can access the sub-parts of the remote server that have been already cached
- you can also mark parts of you distant document repository as "offline": corresponding files are
  then downloaded and regularly synchronized when you are online, providing full access to these
  later when you are offline.
- you control the disk usage of the app on your device and can clear the cache to gain space on
  demand.

## Built with

For the v3 version, we have re-written the Android application from scratch, using Kotlin and latest
Android libraries, e.g. Androidx and Jetpack.

The main third party libraries that we rely upon are:

- [Cells SDK for Java](https://github.com/pydio/cells-sdk-java): a thin wrapper around the swagger generated API that handles low-level
  communication with the remote Pydio servers
- [Room](https://developer.android.com/jetpack/androidx/releases/room): wrapper around SQLite databases
- [AWS S3 Sdk](https://aws.amazon.com/sdk-for-java): manage file transfer with the remote server S3 API
- [Koin](https://insert-koin.io): dependency injection
- [Material 3](https://m3.material.io): the latest version of Material Design for Android
- [Glide](https://bumptech.github.io/glide): a very efficient image loading library

## Contributing

Please read [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) for details on our code of conduct,
and [CONTRIBUTING.md](CONTRIBUTING.md) for details on the process for submitting pull requests to
us. You can find a comprehensive [Developer Guide](https://pydio.com/en/docs/developer-guide) on our
website. Our online docs are open-source as well, feel free to improve them by contributing!

We are also looking for help to translate the Cells interface in various languages. It is really
easy to participate: just navigate
to [our page in the Crowdin translation tool](https://crowdin.com/project/cells-android-client),
create an account and get started.

## Testing

> **Warning**: Most procedures described below are not "backward" compatible:  
> you will have to un-install and fully re-install the application to be able to have the current main released version on your device. Handle with care!

### Beta testing from the store

You can test beta version by registering as a beta tester in the Play Store. After a few minutes,
you should be able to update and run the latest beta version.

You can directly opt-in:

- [from your android device](https://play.google.com/store/apps/details?id=com.pydio.android.Client)
- [from a web browser](https://play.google.com/apps/testing/com.pydio.android.Client)

Once you have opt-in to be a beta tester, it might seem difficult to opt out and start again to use
the app as a _normal_ user. To do so:

- Open the [testing link for web browser](https://play.google.com/apps/testing/com.pydio.android.Client),
- Log in your google account (the same you have used when registering as beta tester),
- Click on the `LEAVE THE PROGRAM` button,
- Uninstall the app,
- Once you are not a tester anymore (it can take time due to some processing on Google's side),
  re-install the app.

### Build and test from Android Studio

Please refer to the [developer corner](./CONTRIBUTING.md) in the same repository.
