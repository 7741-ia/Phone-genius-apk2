<!-- LOVABLE:BEGIN -->
> [!IMPORTANT]
> This project is connected to [Lovable](https://lovable.dev). Avoid rewriting
> published git history — force pushing, or rebasing/amending/squashing commits
> that are already pushed — as it rewrites history on Lovable's side and the
> user will likely lose their project history.
>
> Commits you push to the connected branch sync back to Lovable and show up in
> the editor, so keep the branch in a working state.
<!-- LOVABLE:END -->

## Architecture rules
- Device data flows only through `DeviceAdapter` implementations in `src/core/adapters` (demo / web / native); every metric carries its `source` so fictitious values are never shown as real.
- Diagnostic, alert and action logic lives in pure modules under `src/core` (no React, no network) so it can be ported to Kotlin and unit-tested.
- Native Android capabilities are reached only via the `PhoneGeniusNative` bridge contract in `src/core/adapters/native-bridge.ts`; never assume browser access to Android internals.
- No remote backend, paid API, or browser speech recognition (it sends audio to servers); persistence is localStorage via `src/core/storage.ts`.
