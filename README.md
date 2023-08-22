![Maven Central](https://img.shields.io/maven-central/v/com.interaso/webpush?color=_blue)
![Sonatype Snapshots](https://img.shields.io/nexus/s/com.interaso/webpush?label=sonatype-snapshots&color=8A2BE2&server=https%3A%2F%2Fs01.oss.sonatype.org%2F)

# WebPush

Lightweight library for sending web push notifications with zero dependencies.

This library by default uses blocking HTTP client provided in the JDK, but you can also use it only to
build your requests and combine it with any HTTP library that suits your needs.

## Installation

For Gradle, add the following to your `gradle.build.kts` file:

```kotlin
dependencies {
    implementation("com.interaso:webpush:0.0.4")
}
```

For Maven, add the following to the `dependencies` block of your `pom.xml` file:

```xml
<dependency>
    <groupId>com.interaso</groupId>
    <artifactId>webpush</artifactId>
    <version>0.0.4</version>
</dependency>
```

## Usage

### Sending notifications

The process starts with initializing the `WebPushService` with a subject (URL or `mailto:` prefixed e-mail address) 
and a set of [VAPID keys](#vapid-keys), which are covered later in this document.

```kotlin
val pushService = WebPushService(
    subject = "https://example.com", // or "mailto:example@example.com
    vapidKeys = VapidKeys.generate()
)
```

Once the service is set up, you're ready to send a push notification.

```kotlin
pushService.send(
    endpoint = "https://fcm.googleapis.com/fcm/send/...",
    p256dh = "BPzdj8OB06SepRit5FpHUsaEPfs...",
    auth = "hv2EhUZIbsWt8CJ...",
    payload = "Example Notification",
    ttl = 60_000 , // Optional (defaults to 28 days)
    topic = "test", // Optional
    urgency = WebPush.Urgency.High // Optional
)
```

These are the key parameter descriptions:

- `endpoint`: The recipient of the push notification, represented by a URL.
- `p256dh` and `auth`: Two values that are part of the push subscription and connected to a specific user.
- `payload`: The main content of your push notification.
- `ttl`: The duration (in milliseconds) for which the notification is valid.
- `topic` A header that replaces any pending notifications with the same topic.
- `urgency` A parameter determining how much of a priority the notification should have.

### VAPID keys

VAPID (Voluntary Application Server Identification) keys provides a method for application servers to identify
themselves to push services. This section primarily covers the handling of these keys.

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
    generateMissing = true, // If file not found, generate one and save it
)

// Get application server key for JavaScript
val applicationServerKey = vapidKeys.applicationServerKey

// Serialize to Base64 encoded strings
val publicKey = vapidKeys.x509PublicKey
val privateKey = vapidKeys.pkcs8PrivateKey
```

### Using custom HTTP client

You may prefer to use a different HTTP client for reasons of performance, suspendability, or familiarity.
The example demonstrates how to use `WebPush` class to generate request headers and the encrypted body
and how to process the response.

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
    200, 201, 202 -> true // Notification sent successfully
    404, 410 -> false // Subscription is expired
    else -> throw RuntimeException("Unexpected status code")
}
```

## Snapshots

Development snapshots are available in the Sonatype snapshots repository. Make sure to include it in your repositories.

For Gradle, add the following to your `gradle.build.kts` file:

```kotlin
repositories {
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots")
}
```

For Maven, add the following to the `repositories` block of your `pom.xml` file:

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
