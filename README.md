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

## Continuous integration

Pushes and pull requests run unit tests only. They do not assemble or upload
an APK. To create an installable debug APK, manually run the **Android checks**
workflow and enable its **Build and upload an installable debug APK** option.

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
4. Follow the two required setup rows in the app:
   grant notification access, then select the **Rapido ETA** Always-On Glyph Toy.
5. Optionally enable **Refresh with Essential Key**.
6. Expand **Developer tools** and use **Preview 7 min on Glyph** before a real ride.
   Preview frames expire after three seconds and never replace a live Rapido ETA.

The in-app **Glyph brightness** slider scales only the frames submitted by
Rapido Glyph ETA. It is stored independently and does not change Nothing OS's
device-wide Glyph brightness setting.

The main screen’s **Custom Glyph** section can import a Glyph Museum JSON design as the resting frame.
The app expands Phone (4a) Pro's 137 physical LED values into the SDK's 13×13
coordinate space; full 169-value matrix files are accepted too. It preserves
relative intensity, scales the brightest pixel to full output, and stores the
design locally until **Restore built-in Resting Glyph** is used.

## Essential Key refresh

Android may suspend background timers while the phone is locked. For a
user-triggered refresh without holding a wake lock, enable **Essential Key
Glyph refresh** under Android Accessibility settings. A press of the Nothing
Essential Key resubmits the current ETA frame.
Before restoring the ETA, a short circular confirmation animation sends two
fading trails from the ring's rightmost point around its upper and lower arcs
to the leftmost point.

The accessibility service requests hardware key filtering only, cannot
retrieve window content, does not consume the key event, and does not use the
network. Android permits only one accessibility service to filter hardware
keys at a time, so other key-remapping accessibility services must be disabled
while using this feature.

If the ETA is visible in the app but not on the matrix, use **Copy redacted
debug dump**. It contains device/app build details and recent Glyph Toy
service events, but replaces notification content with a size summary. A raw
dump remains available behind a warning for cases where the exact notification
wording is required; review it for names, phone numbers, locations, PINs, and
booking details before sharing.

The manifest uses Nothing's `test` key. The API-key restriction was removed
for apps targeting Android 16+, but the metadata remains for compatibility.

## First real-ride test

Rapido's exact notification wording and custom extras can change. Developer
tools show the complete text payload locally on the device. If the ETA appears
in the status bar but is not parsed, use the warned raw dump only when the
exact text is needed, and remove names, phone numbers, locations, PINs, or
other personal details before sharing. Add the new wording as a failing test
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

The bundled Doto title font is provided by Google Fonts under the SIL Open
Font License 1.1. Its licence text is included in
`app/src/main/res/raw/doto_ofl.txt`.

[gdk]: https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit
[gdk-license]: https://github.com/Nothing-Developer-Programme/GlyphMatrix-Developer-Kit/blob/main/LICENSE.md
