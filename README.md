# JxBrowser in a Gradle-based Compose desktop application

This example demonstrates how to configure a Gradle project with JxBrowser to embed a `BrowserView` widget into a Compose desktop application to display web pages.

## Prerequisites

* Java 17 or newer.
* Your JxBrowser license key, or a [free 30-day evaluation key][web-form].

## Run the Compose application

Use the following command to start the application:

```bash
./gradlew run -Djxbrowser.license.key=<your_license_key>
```

Once launched, the app loads the [HTML5 test page][html5-test-page]:

![BrowserView in Compose Desktop app][compose-browser-view]


[web-form]: https://www.teamdev.com/jxbrowser#evaluate
[html5-test-page]: https://html5test.teamdev.com
[compose-browser-view]: compose-browser-view.png
