self.addEventListener("push", (event) => {
    event.waitUntil(
        clients.matchAll({includeUncontrolled: true}).then(windows => {
            windows[0].postMessage(event.data.text());
        })
    );
});
