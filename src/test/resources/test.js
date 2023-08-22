const registration = await navigator.serviceWorker.register("sw.js");

navigator.serviceWorker.addEventListener("message", (event) => {
    document.body.textContent = event.data;
})

await Notification.requestPermission();

const applicationServerKey = await fetch("/vapid")

const subscription = await registration.pushManager.subscribe({
    userVisibleOnly: true,
    applicationServerKey: new Uint8Array(await applicationServerKey.arrayBuffer()),
});

const json = subscription.toJSON();
const body = new FormData();

body.append("endpoint", json.endpoint);
body.append("p256dh", json.keys["p256dh"]);
body.append("auth", json.keys["auth"]);

await fetch("/send", {method: "post", body: body});
