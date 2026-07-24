# Rapido Glyph ETA

An Android proof of concept for the Nothing Phone (4a) Pro. It reads Rapido
passenger notifications, extracts the pickup ETA, and displays it on the
phone's 13×13 Glyph Matrix.

## How it works

1. `RapidoNotificationListener` receives notifications from
   `com.rapido.passenger`.
2. The app first reads Android 16's Live Update status-chip value
   (`shortCriticalText`) or future `when` timestamp, then falls back to visible
   notification text such as `7 min` or `arriving by 10:45 PM`.
3. The latest arrival time is stored locally in app-private preferences and
   counted down once per minute. It expires five minutes after arrival.
4. `EtaGlyphToyService` renders values such as `7m` or `12m` as a 3×5 pixel
   font on the 13×13 matrix.

The app does not use the network and does not send notification content
anywhere.

## Important platform constraint

Phone (4a) Pro supports only Always-On Display (AOD) Glyph Toys. After
installing the app, select **Rapido ETA** in:

`Settings → Glyph Interface → Flip to Glyph → Always-on Glyph Toy`

Only one AOD Glyph Toy can be selected at a time.

## Setup

Requirements:

- Android Studio with Android SDK 36
- JDK 17
- Nothing Phone (4a) Pro
- Nothing's Glyph Matrix SDK 2.0

Then:

1. Download `glyph-matrix-sdk-2.0.aar` from the official
   [Glyph Matrix Developer Kit][gdk].
2. Put it at `app/libs/glyph-matrix-sdk-2.0.aar`.
3. Open this repository in Android Studio and install the `debug` build.
4. In the app, grant notification access.
5. Select the **Rapido ETA** Always-On Glyph Toy.
6. Use **Test with 7 minutes** before testing a real ride.

To inspect every numeric frame on real hardware, tap **Start 1–99 sweep
(3 sec each)**. The selected Glyph Toy displays each value in order, loops
from `99m` back to `1m`, and continues until **Stop number sweep** is tapped.

## Essential Key refresh

Android may suspend background timers while the phone is locked. For a
user-triggered refresh without holding a wake lock, enable **Essential Key
Glyph refresh** under Android Accessibility settings. A press of the Nothing
Essential Key resubmits the current ETA frame.
Before restoring the ETA, a short confirmation animation sends two fading
trails from the center-right edge around the perimeter to the center-left edge.

The accessibility service requests hardware key filtering only, cannot
retrieve window content, does not consume the key event, and does not use the
network. Android permits only one accessibility service to filter hardware
keys at a time, so other key-remapping accessibility services must be disabled
while using this feature.

If the ETA is visible in the app but not on the matrix, tap **Copy debug
dump** and paste the result into an issue or chat. The dump contains device
and app build details, the latest Rapido notification payload, and recent
Glyph Toy service lifecycle/render events. Review it for personal information
before sharing.

The manifest uses Nothing's `test` key. The API-key restriction was removed
for apps targeting Android 16+, but the metadata remains for compatibility.

## First real-ride test

Rapido's exact notification wording and custom extras can change. The app
shows the complete text payload it can read under **Latest Rapido notification
payload**. If the ETA appears in the status bar but is not parsed, copy that
diagnostic text into a GitHub issue (remove names, phone numbers, locations,
PINs, or other personal details first). Add the new wording as a failing test
in `EtaParserTest`, then update the parser.

Android 16 exposes the status-chip value used by Live Update notifications.
This project reads that value when Rapido supplies it, then falls back to the
underlying notification text.

## Scope

Current:

- Rapido passenger app only
- Relative and clock-time ETAs
- 0–99 minute display
- Local-only processing

Next:

- Test against real Rapido notification samples
- Improve the toy preview and matrix layout
- Add other ride-hailing apps behind explicit adapters

## Licence

This repository's source is MIT licensed. Nothing's proprietary Glyph Matrix
SDK is not included and remains subject to [Nothing's SDK licence][gdk-license].

[gdk]: https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit
[gdk-license]: https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit/blob/main/LICENSE.md
