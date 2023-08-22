![GitHub licence](https://img.shields.io/github/license/interaso/webpush?color=blue)
![Maven Central version](https://img.shields.io/maven-central/v/com.interaso/webpush?color=blue)
![Sonatype Nexus (Snapshots)](https://img.shields.io/nexus/s/com.interaso/webpush?label=snapshot&color=8A2BE2&server=https%3A%2F%2Fs01.oss.sonatype.org%2F)

# WebPush

Lightweight library for sending web push notifications with zero dependencies.

This library by default uses blocking HTTP client provided in the JDK, but you can also use it only to 
build your requests and combine it with any HTTP library that suits your needs.

* [Installation](#installation)
* [Usage](#usage)
  * [Sending notifications](#sending-notifications)
  * [VAPID keys](#vapid-keys)
  * [Using custom HTTP client](#using-custom-http-client)
* [Snapshots](#snapshots)
* [License](#license)

## Installation

Stable releases are available in Maven Central repository. 

### Gradle

Add the following to your `gradle.build.kts` file:

```kotlin
dependencies {
    implementation("com.interaso:webpush:0.0.4")
}
```

### Maven

Add the following to the `dependencies` block of your `pom.xml` file:

```xml
<dependency>
    <groupId>com.interaso</groupId>
    <artifactId>webpush</artifactId>
    <version>0.0.4</version>
</dependency>
```

## Usage

### Sending notifications

```kotlin
// Setup push service
val webPushService = WebPushService(
    subject = "https://www.example.com", // must start with "https://" or "mailto:"
    vapidKeys = VapidKeys.generate() // see VAPID keys section for more options
)

// Send push notification
webPushService.send(
    endpoint = "https://fcm.googleapis.com/fcm/send/cI_G6sNbxMo:APA91bG...",
    p256dh = "BPzdj8OB06SepRit5FpHUsaEPfs...",
    auth = "hv2EhUZIbsWt8CJ...",
    payload = "Example Notification",
    ttl = 60_000 , // optional (defaults to 28 days)
    topic = "test", // optional
    urgency = WebPush.Urgency.High // optional
)
```

### VAPID keys

```kotlin
// Generate new keys
val vapidKeys = VapidKeys.generate()

// Create from existing KeyPair
val vapidKeys = VapidKeys(
    publicKey = keyPair.public as ECPublicKey,
    privateKey = keyPair.private as ECPrivateKey
)

// Create from Base64 encoded strings 
val vapidKeys = VapidKeys.create(
    x509PublicKey = "MIIBIjANBgkqhkiG9w0BAQEF...",
    pkcs8PrivateKey = "MIIEvQIBADANBgkqhkiG..."
)

// Load from file (line separated)
val vapidKeys = VapidKeys.load(
    path = Path("path/to/vapid.keys"),
    generateMissing = true, // if file not found, generate one and save it
)

// Get application server key for JavaScript
val applicationServerKey = vapidKeys.applicationServerKey

// Serialize to Base64 encoded strings
val publicKey = vapidKeys.x509PublicKey
val privateKey = vapidKeys.pkcs8PrivateKey
```

### Using custom HTTP client

```kotlin
// Setup request builder with subject and VAPID keys
val webPush = WebPush(subject, vapidKeys)

// Generate request headers and encrypted body
val headers = webPush.getHeaders(endpoint, ttl, topic, urgency)
val body = webPush.getBody(payload, p256dh, auth)

// Use custom HTTP client to process request
val response = customHttpClient.post(endpoint, headers, body)

// Process custom response
when (response.status) {
    200, 201, 202 -> true // notification sent successfully
    404, 410 -> false // subscription is expired
    else -> throw RuntimeException("Unexpected status code")
}
```

## Snapshots

Development snapshots are available in the Sonatype snapshots repository. Make sure to include it in your repositories.

### Gradle

Add the following to your `gradle.build.kts` file:

```kotlin
repositories {
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
}
```

### Maven

Add the following to the `repositories` block of your `pom.xml` file:

```xml
<repository>
    <id>sonatype-snapshots</id>
    <url>https://s01.oss.sonatype.org/content/repositories/snapshots</url>
    <releases>
        <enabled>false</enabled>
    </releases>
    <snapshots>
        <enabled>true</enabled>
    </snapshots>
</repository>
```

## License

This project is licensed under the terms of the MIT license. See the [LICENSE](/LICENSE) file for more details.
