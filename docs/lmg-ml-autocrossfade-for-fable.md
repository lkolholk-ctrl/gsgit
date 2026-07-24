# LMG ML Auto-Crossfade — Integration Spec for Fable

**Model:** `automix_v2_1.tflite` (~800k params, fp16, 1.63 MB). Already running in production on the LMG
offline Juce/Oboe engine (log-confirmed).

**Task:** promote the model from "running" to the **automatic crossfade *composer*** for **both** paths:
the offline Juce mixer **and** the streaming `media3-lmg` fork (a forked Media3 with an `AudioFadeControl`
+ two audio renderers; MANUAL crossfade mode already implemented). This is an integration/design spec, not code.

## What / why

The on-device model **replaces the Apple server-side `AudioAnalysis`** step. Instead of shipping audio off to
a server to get back raw analysis features, we run `automix_v2_1.tflite` on-device and get back a **ready
crossfade recipe** — not raw features. The five outputs are *decisions* (blend? how long? where does B enter?
where in A does it start? which curve?), so the engine's job is to **execute** a recipe, not to interpret
features. Both engines drive one shared planner; only the *source of PCM* and the *analyze lead-time* differ.

The single biggest risk is **DSP drift**: the on-device mel/aux pipeline must **bit-match training exactly**.
Any divergence in framing, filterbank, dB reference, resample order, or numeric precision silently corrupts the
recipe (`c/d/o/s/type`) with no crash. Every parameter marked **CONFIRM** below is a hard blocker for parity.

---

## 1. Model I/O (verified from the tflite graph + a live runtime log)

**Inputs**
- `mel_a [1,431,128,1]` — log-mel of the **5 s tail of full track A** (outgoing).
- `mel_b [1,431,128,1]` — log-mel of the **5 s head of full track B** (incoming).
- `aux [1,32]` — pair features (only `rmsA, rmsB, bpmA, bpmB` confirmed; other 28 unknown).

**Preprocessing constants (from log):** `SR=44100`, window `5.0 s = 220500 samples`, `hop=512`, `n_mels=128`,
`~431 frames`, log-mel in dB clamped to `[-80, 0]` (`melRaw[-80..0]`).

**Outputs (5 positional tensors — a ready recipe, not features):**

| idx | name | tensor | role | maps to |
|-----|------|--------|------|---------|
| 0 | `c` | scalar 0..1 | compat / score | **gate** (blend vs skip) — honor it, not decorative |
| 1 | `d` | scalar 0..1 | duration | crossfade length (ms) |
| 2 | `o` | scalar 0..1 | offset | track-B entry point (ms) |
| 3 | `s` | scalar 0..1 | start | **fraction of A's length** → absolute start (ms) |
| 4 | `tlogits` | vector[6] | curve logits | `argmax` → one of 6 `FadeEffectType` |

Logged data points (used below; **two points cannot fix a mapping** — take exact constants from training):
`d=0.085→7117 ms`, `d=0.104→7600 ms`; `o=0.017→166 ms`, `o=0.096→955 ms`; `s≈0.9595`, `lenA≈237 s → 227414 ms`;
`type=0` chosen.

**Runtime cadence (log):** `analyze` fires ~40 s before A end → `PRED` (sub-second inference) → `plan` →
`XFADE` trigger when remaining ≈ xfade → `DONE` (deck swap). Some JUCE xruns (offline underruns) observed —
this drives the strict audio-thread rules in §6.

---

## 2. Preprocessing (bit-match to training)

The mel/aux math (§2.1–§2.3) is identical for both paths. **PCM sourcing (§2.4) is NOT symmetric** and hides
the two worst bugs: *tail-is-in-the-future* and *B-not-decoded-yet*. Read §2.4 as the high-risk section.

### 2.1 Which 5 s? (define windows before sourcing them)

- `mel_a` = the **last 5 s of track A's decoded audio, ending at A's end-of-stream** — **NOT** "5 s ending at
  the crossfade point." The start `s` is a model *output* (s≈0.96), so it cannot define the input window; that
  would be circular. Feed the natural file tail and let the model place the crossfade.
- `mel_b` = the **first 5 s of track B from sample 0** (post-decode, post deck-gain, **pre-fade**),
  **independent of `o`** — the model *predicts* the B-entry offset from a fixed head window; feeding an
  offset-shifted window is circular and wrong.
- At `analyze` (~40 s before A ends) **A's true tail is not yet decoded/played** — never source it from
  already-played PCM. See §2.4.

> **CONFIRM the window convention** against training: file-tail of A vs a fade-relative tail; head-of-B from
> sample 0 vs from the entry point. Document the confirmed rule as the fixed, deterministic preprocessing input.

### 2.2 Canonical audio format (both paths)

Feed the mel stage a **mono, float32, 44100 Hz** buffer of **exactly 220500 samples**.

- **Resample to 44100 first.** Device decode is frequently 48000 (Oboe/AudioTrack, streaming AAC/Opus). Playback
  may stay at device SR (`startMs`/`bEntryMs` are SR-independent); the **mel** must be at 44100.
- **Resample order is load-bearing.** Training almost certainly loaded the whole track at 44100
  (`librosa.load(sr=44100)`) *then* sliced 5 s. Resampling an isolated 5 s snippet yields different edge samples
  (filter transients at buffer boundaries). **Resample a padded super-window and crop the central 220500**, or
  replicate training's whole-track-then-slice order. Slice-then-resample-in-isolation fails edge parity.
- **Downmix stereo→mono** to match training (`(L+R)/2` vs `librosa.to_mono` vs left-only — a wrong downmix
  shifts every bin). Handle >2 channels and non-interleaved layouts.
- **No pre-emphasis / dither / gain / DC removal** unless training applied it.
- **PCM→float scale** matches training (`int16/32768` vs `/32767`) — affects RMS and mel.
- **Exact length; pad the correct end.** For the **A tail** pad the *front*; for the **B head** pad the *end*.
  `librosa.load` does **not** pad short tracks — if training didn't pad, on-device padding is itself a divergence.
  Prefer the sub-5 s **fallback** (§5) over silent padding.
- **Precision:** all DSP (resample/STFT/mel/dB) in **fp32/fp64**; cast to fp16 **only** at the final input-tensor
  boundary, and only if the tflite input tensor is actually fp16 (see §7). fp16 STFT/mel drifts badly.

### 2.3 STFT → mel → log-mel

Frame count `1 + floor(220500/512) = 431` implies **librosa `center=True`** with `n_fft/2` reflect padding
(`center=False` → 430 frames, shifts every frame).

> **Boundary-frame hazard:** per-slice reflect padding only matches training if training also computed the mel
> **per 5 s slice** with `center=True`. If training ran the STFT over the **whole track then sliced 431 frames**,
> the boundary frames saw *real neighboring audio*, not reflection. **CONFIRM per-slice vs full-track STFT** — it
> changes how the §2.2 super-window is built.

```
SR         = 44100
window     = 220500 samples (5.0 s)
n_fft      = 2048          # CONFIRM (log implies 2048)
hop        = 512
win_length = n_fft         # CONFIRM (== n_fft vs shorter)
window_fn  = "hann"        # CONFIRM (hann vs hamming; periodic vs symmetric)
center     = true          # asserted from frame math — VERIFY
pad_mode   = "reflect"     # CONFIRM
power      = 2.0           # CONFIRM (power 2.0 vs magnitude 1.0)
n_mels     = 128
fmin       = 0             # CONFIRM
fmax       = 22050         # CONFIRM (8000 common in music tagging)
mel_norm   = "slaney"      # CONFIRM (slaney area-norm vs none)
mel_scale  = "slaney"      # CONFIRM (slaney vs HTK — shifts every bin)
```

Pipeline (all fp32+):
1. STFT → `[1025, 431]` complex.
2. Power spectrogram `|S|^power`.
3. Mel filterbank `[128, 1025]` → mel power `[128, 431]`.
4. **log-mel dB, clamp `[-80, 0]`.** The **dB reference** is the single most error-prone param:
   `ref=max` (per-clip peak, top=0 dB, normalizes loudness away) vs `ref=1.0`/fixed eps (preserves absolute
   level). Because `aux` carries `rmsA/rmsB`, training *likely* uses a **fixed** ref — but **CONFIRM**. Also
   confirm `top_db`, log base (`10*log10` vs `20*log10` vs natural), and the exact `eps`/floor before `log10`
   (a different eps shifts near-silent bins into the −80 clamp where fp16 error is worst).
5. **Transpose to `[431, 128]`**, add batch+channel → `[1, 431, 128, 1]`. **CONFIRM axis order against the
   graph** — a transpose bug won't crash, only drift.

**Post-clamp normalization:** the log shows raw dB (`melRaw[-80..0]`), suggesting the model consumes **raw dB**
with no `[0,1]`/mean-std rescale — but **CONFIRM** none was applied in training. Applying (or omitting) a
normalization the other side did is guaranteed drift.

### 2.4 PCM sourcing (asymmetric — the high-risk section)

**Offline / Juce** (full decoded / seekable PCM — can read the future):
- **A tail:** last 220500 samples ending at A's EOS, via seek/`AudioFormatReader`. Exact.
- **B head:** first 220500 samples of deck B from sample 0, post deck-gain, **pre-fade**. If B isn't decodable
  by analyze time → §5 fallback.
- Resample+downmix each to §2.2 canonical form using the resample-then-slice order.

**Streaming / media3-lmg:**
- **A tail — decode-ahead from source, NOT a playback tap.** At `analyze` the newest decoded PCM is ~40 s
  *before* the true tail; a ring-buffer snapshot would score the middle of the track as the outro (the primary
  naïve-design bug). Instead perform an **out-of-band decode of A's final 5 s** directly from A's media source
  (seek extractor to `duration − 5 s`, decode to PCM) **off the render thread**. If A's duration is
  unknown/unseekable (live, VBR without seek table) → §5 fallback. The tap must read PCM **before** any
  `AudioFadeControl` fade/gain, or RMS/mel see an attenuated tail.
- **B head — capture-on-first-decode with a readiness guard.** B is buffered ahead for gapless, but at analyze
  time B **may not have started decoding**. Add a passthrough `AudioProcessor` on B's renderer that captures the
  first 220500-equivalent samples the moment B produces PCM, into a fixed buffer; freeze once full. If **not full**
  when analyze needs it: either force an out-of-band decode of B's head from source, or defer to §5. **Never feed
  a partially-filled / zero-padded B head as if complete.**
- **Tap format** = renderer's `AudioFormat`. Keep taps **zero-cost passthrough** (raw `memcpy` only); do all
  conversion (int16 `/32768`, float, endianness, channel layout, resample) in the **off-thread** preprocessor.
- **Audio-thread safety (xrun hazard):** taps only `memcpy` — **no FFT/resample/alloc/lock on the render
  thread.** Hand snapshots to the preprocessor via a lock-free double-buffer / atomic index. All FFT/inference
  runs off-thread (`analyze → PRED` off the render path).

### 2.5 aux[32]

Compute over the **same** canonical 44100/mono buffers as the mels, **pre-fade** — *except* where training used a
different analysis span (BPM).

- `rmsA`, `rmsB` — RMS over the 5 s window. **CONFIRM** scale (linear vs dBFS, normalized or not).
- `bpmA`, `bpmB` — tempo. **Analysis span is a streaming race:** tempo over a 5 s slice is noisy, so training may
  compute BPM over the **full track**. If so, on-device you cannot derive it from the 5 s window — and for
  incoming B you don't *have* the full track at analyze time. **CONFIRM the BPM span**; if full-track, source it
  from the existing track analyzer / metadata and treat "BPM unavailable" as a §5 fallback trigger. Confirm
  estimator (`librosa.beat.tempo` args, aggregation) and units (BPM vs normalized).
- **Unknown 28 slots — CONFIRM, do not implement until confirmed.** Candidates: key/Camelot + key-compat,
  spectral centroid/rolloff/flatness, ZCR, beat-phase/downbeat offset, LUFS/integrated loudness, onset density,
  HPR, MFCC means, band energies, tempo ratio, A duration, track-length fraction, beat-tracker confidence.
- **Per-slot scaling is non-optional:** a raw BPM `128` where the slot expects normalized `0.5` breaks the recipe
  as badly as a wrong mel. Every slot needs its confirmed range/transform, in the confirmed **positional order**.

---

## 3. Output mapping (model → `CrossfadeRecipe`)

All scale constants are **CONFIRM-THESE** from training code, not the graph. Where the log gives points, the
implied fit is shown and flagged under-determined.

> **fp16 precision note:** model outputs carry fp16 resolution even if the interpreter dequantizes I/O to fp32.
> Near 1.0 an fp16 step is ~2⁻¹¹ ≈ 4.9e-4; on a 237 s track that's ~±100 ms of positional jitter on `s`. Treat no
> output as sample-exact. Sample-accurate ms→frame conversion at apply time is placement precision only — it does
> **not** recover precision the fp16 output never emitted.

**Validate first:** every output element finite (`!NaN && !Inf`) → else abort automix, fall back (§5). Clamp
`c,d,o,s` to `[0,1]` **before** any descale.

**`c` → gate (no scaling).** If `c < C_GATE` the model is signalling "don't blend" → fall back to
default/manual. `C_GATE` **CONFIRM** (proposal ~0.35–0.5). `c` is advisory only — never feed into volume/curve.
Log `c` with the decision for tuning.

**`d` → crossfade ms.** Log points are inconsistent as a pure scale (83729 vs 73077 ms/unit) → **affine or
nonlinear, not `d*K`**. Provisional (CONFIRM): `xfadeMs ≈ 4956 + 25421*d` (reproduces both points <1 ms; implies
a ~5 s floor at `d=0`). Clamp to `[XFADE_MIN, XFADE_MAX]` (CONFIRM, propose 500..12000 ms) **after** mapping.

**`o` → track-B entry ms.** Affine solve gives `K≈9987, b≈-4` ⇒ effectively linear. Provisional:
`bEntryMs = o * O_MAX`, `O_MAX = 10000` (CONFIRM). This is the offset into B's head where B playback begins (skip
silent intro / count-in); it is expected to land inside the analyzed 5 s head, not a contradiction.

**`s` → absolute start ms.** `s` is a **fraction of A's total length**: `startMs = round(s * lenA_ms)`.
`lenA_ms` **must use training's length definition** — almost certainly decoded `sampleCount / SR`, not
container/header duration (VBR/streaming container duration can be off by seconds and scales `s` wrong). Guards,
in order: (1) `startMs = min(startMs, lenA_ms − xfadeMs)` (fade can't run past A's end); (2)
`startMs = max(startMs, earliestSchedulableMs)` where `earliestSchedulableMs = nowMs + prepLead` — a start
already in the past can't be scheduled; clamp forward or fall back if `< XFADE_MIN` of A remains; (3) if
`lenA_ms` is unknown (live) → fall back.

**`tlogits` → curve.** `argmax` is invariant under softmax — take `argmax` over the raw 6 logits on-device.
Bounds-check `curveIdx ∈ [0,6)` (malformed tensor → fallback curve `CONSTANT_POWER`). Encode the mapping as one
explicit table, read by both engines:

```kotlin
// Order is CONFIRM-THESE — set by training labels. type=0 seen; do NOT assume 0 == LINEAR.
val TYPE_TO_CURVE = arrayOf(
    FadeEffectType.LINEAR,         // 0  CONFIRM
    FadeEffectType.CUBIC,          // 1  CONFIRM
    FadeEffectType.EXPONENTIAL,    // 2  CONFIRM
    FadeEffectType.LOGARITHMIC,    // 3  CONFIRM
    FadeEffectType.CONSTANT_POWER, // 4  CONFIRM
    FadeEffectType.SIGMOID,        // 5  CONFIRM
)
```

> **Type-output shape ambiguity — CONFIRM:** the I/O lists "5 positional outputs" yet `type` is described as
> `argmax(tlogits[6])`. Either (a) a length-6 logits tensor is a distinct 5th output (`argmax` it), or (b) the
> 5th output is an already-reduced fp16 float index — if (b), **round-to-nearest** and clamp `[0,5]`, never floor
> (`2.9999→3`, not `2`). Do not assume `tlogits[6]` exists until confirmed.

**Normalized recipe** (immutable; both engines consume it, no engine re-reads raw floats):

```kotlin
data class CrossfadeRecipe(
    val proceed: Boolean,     // c >= C_GATE && all outputs finite && invariants hold
    val startFracA: Float,    // store the FRACTION, derive startMs = startFracA * lenA at apply time
    val xfadeMs: Long,
    val bEntryMs: Long,
    val curve: FadeEffectType,
    val score: Float,         // raw c, for telemetry
    val modelVersion: String,
    val source: Source,       // MODEL | FALLBACK
)
```

Store `startFracA`, not `startMs` — derive `startMs = startFracA * lenA` at apply time from the current known
length, so a cache hit survives refined length metadata. Construction order (each step uses the prior clamped
value): finite-check → `xfadeMs = clamp(f_d(d))` → `startMs` guards → `bEntryMs = o*O_MAX`.

**Invariants at construction** (any failure → fall back, log):
- `earliestSchedulableMs ≤ startMs ≤ lenA_ms − xfadeMs`
- `XFADE_MIN ≤ xfadeMs ≤ XFADE_MAX`
- `0 ≤ bEntryMs` **and** `bEntryMs + xfadeMs ≤ lenB_ms` (B must hold a full fade after entry or the fade-in goes
  silent near B's own tail)
- `curve` from `TYPE_TO_CURVE`; `lenA_ms`, `lenB_ms` known (> 0)

---

## 4. Scheduling & prefetch

Both engines drive the **same** `AutoMixScheduler` + `AutoMixModule`; only "source of B-head PCM" and analyze
lead-time differ.

> **Central timing fact:** `startMs` is a model **output**, so you cannot schedule "analyze by `startMs − GUARD`"
> against a value you learn only *after* analysis. All pre-analysis scheduling uses a **conservative earliest
> start** `startMsA_min = S_MIN * lenA`, where `S_MIN` is the smallest start-fraction the model emits (CONFIRM;
> assume `S_MIN ≈ 0.85` until confirmed). The refined `startMs` only *tightens* the actual XFADE trigger.

### 4.1 Shared module boundary

`AutoMixModule` (Kotlin/C++ core, no engine deps) exposes:
`suspend fun plan(pairKey, tailA, headB, aux) : Recipe`. It owns mel DSP, the tflite session, and `RecipeCache`.
It knows nothing about decks/renderers/transport — engines call it, it never calls back. **`plan()` always runs
on a worker thread, never on the JUCE/Oboe audio callback** (mel DSP is CPU-heavy; inference is
sub-second-but-not-instant — either on the audio thread is an xrun).

`PairKey = (trackA.id, trackB.id, tailA.sampleHash, headB.sampleHash)` — hashes guard against re-encodes/edits.

Both engines implement one interface so the scheduler is engine-agnostic:

```
interface DeckHost {
  fun lengthMsA(): Long?          // null if unknown (live)
  fun positionMsA(): Long
  fun remainingMs(): Long         // LONG_MAX if length unknown
  fun tailPcmA(): PcmRef          // exactly last 220500 samples @44100 mono
  fun headPcmB(): PcmRef?         // exactly first 220500 samples @44100 mono; null if not ready
  fun bufferedHeadMsB(): Long     // LONG_MAX on offline (always ready)
  fun applyRecipe(r: Recipe)      // arms crossfade; must not allocate/block
}
```

### 4.2 Analyze lead time

```
T_analyze_by = startMsA_min - GUARD
GUARD        = tailAvailDelay + dspTime + inferTime + planTime + armLatency + xrunSlack
```
- **Offline:** `dspTime+inferTime` ≈ sub-second; `GUARD ≈ 2 s`. Fire analyze at
  `min(startMsA_min − GUARD, lenA − 40 s)` — the current ~40 s-before-end heuristic satisfies this for
  normal-length tracks (the last-5 s tail always sits near EOF, decodable that early).
- **Streaming:** `GUARD` additionally covers B prefetch. Fire analyze at `startMsA_min − GUARD − prefetchLeadB`.
- **Refined start already passed / too close:** if `startMs ≤ positionMsA + armLatency`, clamp
  `startMs = positionMsA + armLatency` (arm immediately); if even that leaves `< durationMs` of A, fall back to a
  shorter neutral crossfade. Never arm a start in the past.
- **Crossfade must fit inside A:** enforce `startMs + durationMs ≤ lenA`; violation → clamp
  `startMs = lenA − durationMs` (or shorten). Otherwise A runs out mid-fade.
- **Unknown/non-finite `lenA`** (live, no container duration): fraction start is undefined → switch to
  remaining-based scheduling (fade begins when `remainingMs()` drops to `durationMs + tailPad`, neutral/cached
  recipe).
- **Short track** (`lenA < 5 s`, or `lenA < GUARD + durationMs`): tail unfillable / no analyze room → neutral
  fallback, no model call.

### 4.3 Streaming prefetch of B head (network jitter)

The head of B is the blocker: analysis needs B's first 5 s as PCM; playback needs B from `offsetMs` through the
crossfade.

```
prefetchLeadB   = clamp(headBytes / max(estThroughput, MIN_THROUGHPUT), MIN_LEAD, MAX_LEAD) * JITTER_K  // JITTER_K ≥ 3
headBytesNeeded ≈ pcm( max(5000ms, maxOffsetMs + maxDurationMs) )
```
- Guard the throughput term against zero/unknown estimate at stream start (`MIN_THROUGHPUT`, `MAX_LEAD`) so the
  lead can never divide-by-zero or explode earlier than track A's own start.
- Size is `max(5000, maxOffsetMs + maxDurationMs)`, **not** `5000 + maxOffsetMs` — B playback runs from
  `offsetMs` for the full `durationMs` (o ≤ ~955 ms, d ≤ ~7600 ms ⇒ ceiling ≈ 8.6 s, exceeds the 5 s analysis
  window). `maxOffsetMs`/`maxDurationMs` come from the confirmed d/o scale constants.
- Kick B-head fetch at `startMsA_min − prefetchLeadB − GUARD` (conservative `S_MIN`); the head window is
  offset-independent, so nothing about the fetch refines once the recipe lands. Fetch only the **head window**
  (range request / partial segment), hold decoded PCM pinned by `(trackB.id, sampleHash)` so PRED and playback
  reuse it. Watchdog: if `bufferedHeadMsB() < required` at `T_analyze_by` → fallback; **never block the audio
  thread on the network.**

### 4.4 Recipe cache

- `RecipeCache: LRU<PairKey, Recipe>`, in-memory + optional on-disk. Key **includes `modelVersion`** so a model
  bump invalidates. **Never cache `FALLBACK` recipes** — they encode transient conditions, not a model verdict.
- Lookup at `ARM_ANALYZE`. Hit → skip DSP+inference, go straight to `PLAN` (streaming still re-fetches B head for
  *playback*, not analysis). Recompute `startMs = startFracA * lenA` against current length. Cache the recipe,
  never the mel tensors (recipe is tiny; mels are large and reproducible).
- Invalidate on `modelVersion` change, `sampleHash` mismatch, or user transition override.
- **Prewarm:** when the next track is known (queue/autoplay), enqueue analyze for the upcoming pair during
  current playback. On streaming, prewarm fetch/decode runs **low-priority and throttled** so it can't starve the
  current track's buffer (a prewarm-induced xrun is worse than a cold analyze).

### 4.5 Unified timeline

```
now ───────────────────────────────────────── track A end
     │            │        │      │      │
     │            │        │      │      └─ XFADE start = startMs (trigger on positionMsA == startMs)
     │            │        │      └──────── PLAN + ARM (recipe validated, params published lock-free)
     │            │        └─────────────── PRED (sub-second, off audio thread)
     │            └──────────────────────── ANALYZE armed (tailA exact-window ready, headB decoded)
     └───────────────────────────────────── PREFETCH_B kick (streaming; at startMsA_min − prefetchLeadB − GUARD)
```

State machine: `ARM_ANALYZE → PREFETCH_B → PRED → PLAN(validate+clamp) → wait(positionMsA==startMs) → XFADE →
DONE(swap)`, any step short-circuiting to neutral fallback on failure. Offline: `PREFETCH_B` is a no-op.

**XFADE fires when `positionMsA` reaches `startMs`** (equivalently `remainingMs() == lenA − startMs`) — **not**
when `remaining ≈ durationMs`. `startMs` and `durationMs` are independent outputs; the fade generally starts with
more than `durationMs` of A left (e.g. start 227.4 s, duration 7.6 s, A end 237 s ⇒ 9.6 s remaining). Triggering
on `durationMs` misplaces every fade.

---

## 5. Fallback & gating

**Invariant:** the transition ships on time regardless of model state. Gating decides *which* recipe plays, never
*whether* a transition happens. **Every "ship" path applies at least a declick ramp** (even HARD_CUT: a ~5–10 ms
equal-power taper), so the on-time guarantee never produces an audible click.

Decision runs **once, off the audio thread**:

```
analyze → acquireInputs → preprocess → infer → validate → PlanDecision
PlanDecision = AUTOMIX(recipe) | MANUAL(equalPowerFixed) | GAPLESS | HARD_CUT
```

Precedence is two orthogonal axes:

**Availability (short-circuits, evaluated as facts land):**
0. **GAPLESS override** — album-adjacent pair by *metadata* (same `album`+`albumArtist`+consecutive
   `trackNumber`, or explicit gapless flag). Decided **before** any acquisition/inference; nothing else runs.
   Butt-join, zero overlap, no fade, no inference. Missing/partial tags → treat as not-gapless, proceed to model.
   Streaming caveat: if a sample-exact join across two renderers isn't achievable, degrade to a minimal
   `DECLICK_MS` equal-power splice rather than an audible gap.
1. **Input not ready by acquisition deadline** (`decisionDeadline − PREPROC_BUDGET`): streaming B-head
   undecodable, A length unknown/live, track shorter than the 5 s window, decode/resample failure →
   **MANUAL** if still schedulable, else **HARD_CUT**. Do **not** zero/edge-pad a short window — padding drifts.
2. **Deadline miss / inference error / preprocessing failure / non-finite output** → **MANUAL** if a MANUAL
   window is still schedulable, else **HARD_CUT**.

**Content (only on a finite, in-time result):**
3. **Compat gate:** `c < C_MIN` → MANUAL; hard-reject band `c < C_REJECT` → HARD_CUT (with declick).
4. **OOD / sanity gate fail** → MANUAL.
5. **Realizability fail** (see below) → MANUAL.
6. Otherwise → **AUTOMIX(recipe)**.

**Compat/score gate.** `C_MIN` tunable, start **0.35** (CONFIRM against training score distribution). MANUAL =
equal-power `CONSTANT_POWER`, `MANUAL_XFADE_MS` (default 4000, capped by input availability), start at
`A_end − MANUAL_XFADE_MS`, B entry 0. `C_REJECT` default 0.12 → HARD_CUT (still declicked). One-shot per
transition, no hysteresis; log `c`.

**OOD detection** (model trained on beat-driven material; ambient/classical/speech/bad-rips drift). Reject to
MANUAL when any: `bpmA/bpmB` outside `[BPM_MIN,BPM_MAX]` (~60–200) or beat-tracker confidence low;
`|bpmA − bpmB|` beyond match range **and** `c` marginal; `rmsA/rmsB` below silence floor **or peak-clipped**
(detect from PCM peak/true-peak, not RMS); **mel flatness** — per-frame variance below threshold or a large
fraction of frames pinned at −80 dB. The OOD gate must read the **same on-device aux values the model consumed**
(not a second estimator), and mel statistics must be computed on the **post-normalization** representation
actually fed to the model — if the dB ref is per-example (`np.max`), calibrate the flatness threshold against
that normalization.

**Recipe realizability (joint bounds — per-field clamps alone are insufficient):**
- **Fade fits inside A:** `start + duration ≤ A_len − END_GUARD_MS`; violation → MANUAL (a model placing the fade
  past EOF is distrusted, not silently clamped).
- **Start is schedulable given analyze lead:** `s` is a fraction of *total* A length but analyze fires only
  `ANALYZE_LEAD` (~40 s) before A end, so any `s` mapping to `start < A_end − ANALYZE_LEAD` is *already in the
  past*. Require `start ≥ now + SCHED_GUARD`; else → MANUAL. Preferred long-term: make `ANALYZE_LEAD` **adaptive**
  to cover `A_len·(1 − S_MIN)` (CONFIRM/tuning item).
- Per-field defense-in-depth: `d`→ms in `[XFADE_MIN, 12000]`, `o`→ms in `[0, B_len−ε]`, `s`-fraction in
  `[0.5, 0.995]`; any → MANUAL.

**Deadline ("never block a transition").** `decisionDeadline` must **not** depend on the model's own
`startFraction` (circular — the deadline exists for when no recipe returns). Compute from the earliest
schedulable start: `decisionDeadline = (A_end − ANALYZE_LEAD) − SAFETY_MS`. **`SAFETY_MS` is per-engine:** offline
Juce ~750 ms; **streaming must cover B-deck prefetch/seek-to-`o` network latency (seconds)** — derive from
measured prefetch p95. Inference runs on a bounded worker with `INFER_TIMEOUT_MS`: a TFLite `invoke()` is **not
cancellable**, so the soft timeout abandons only the *result* (compute keeps holding CPU) — size it from measured
p99, and an overrun must **trip the circuit breaker**. Late arrival never mutates an installed plan (single
writer, CAS the plan slot; superseded result dropped). One in-flight inference per boundary; supersede on
queue change rather than queueing.

**fp16 edge cases.** Reject NaN/±Inf; clamp `c,d,o,s` to `[0,1]` before descale. Degenerate/out-of-range type →
curve index **0** (log-confirmed default), never crash `argmax`/lookup. Enforce `d ≥ XFADE_MIN` (`o=0` is fine);
a zero-length fade → promote to MANUAL, never emit a 0 ms crossfade. Keep descale constants, `XFADE_MIN`, and the
6-class order as **fp32 host constants** (never baked into the fp16 graph).

Offline can never hit *B-not-ready* (`bufferedHeadMsB()==LONG_MAX`, compiles out) but **still uses the
inference-failure/validation fallbacks** — an offline model crash must not kill the transition.

**Telemetry** (off the audio thread, per transition): `c`, `d/o/s` raw+descaled, `type` (raw+resolved curve),
`PlanDecision`, decision latency, deadline margin, acquisition outcome (B-head ready? A_len known?), reject-reason
enum, and which realizability constraint failed. Metrics `automix.b_not_ready`, `automix.infer_fail`,
`automix.gate_reject`, `automix.output_clamped` kept separate. Needed to tune `C_MIN`/`C_REJECT`, OOD bounds,
`ANALYZE_LEAD`, per-engine `SAFETY_MS`.

---

## 6. Audio-thread / xrun rules (hard) and per-engine apply

Observed JUCE xruns → strict separation. **The audio/render thread does ZERO of:** mel/STFT DSP, TFLite
`invoke()`, allocation, file/network I/O, seek, formatted logging. It only reads an already-installed immutable
plan and applies gain per block (LUT lookup/interpolation only).

- Preprocessing and inference run on a **dedicated worker**; results delivered by **lock-free single-slot
  hand-off** (atomic pointer swap of an immutable plan; **release** on publish, **acquire** on read; no lock ever
  touched from the audio callback).
- **Plan reclamation (use-after-free hazard):** on swap the audio thread may still read the previous plan. Do not
  free at swap time — use RCU-style retirement (epoch/seqlock or retire-on-next-worker-cycle grace period). All
  plan alloc/free on the worker.
- Plan is fully materialized on the worker before hand-off: sample-accurate start offset, per-block gain LUT for
  the chosen curve (LUT length = exact crossfade sample count), B-deck prime/seek offset already prefetched.
- **Pre-warm** the interpreter at engine init (allocate tensors + one dummy invoke) so the first real inference
  has no cold-start spike near the deadline. Re-warm after any delegate reset (GPU/NNAPI switch,
  return-from-background).
- **Circuit breaker:** on thermal throttling, a blown `INFER_TIMEOUT_MS`, or an invoke overrun, stay in **MANUAL
  for the rest of the session** rather than repeatedly risking xruns. Resets on next engine init.

**Offline (Juce mixer).** Deck A plays to `startMs`; the mixer begins the fade of length `xfadeMs`. Deck B is
seeked to `bEntryMs` and started at `startMs` (`frame = round(ms * 44100 / 1000)` — placement precision only).
Gain automation samples A's fade-out and B's fade-in from `curve`; for `CONSTANT_POWER` the branches are
`cos/sin`-law (keep the pair coupled so summed power stays ~unity). `DONE`/swap at `startMs + xfadeMs`.
**Pre-seek and pre-buffer deck B during the analyze→trigger window** — never seek B at trigger time; all heavy
prep (seek, decode-ahead, resample, gain-ramp table, mel/inference) completes before `startMs`, none on the audio
callback.

**Streaming (media3-lmg).** Reuse the existing MANUAL `AudioFadeControl` + two renderers; automix only supplies
the params MANUAL mode took from the UI, so AUTOMIX and MANUAL share one applier (only the *source* of fade params
differs — pass `TYPE_TO_CURVE` output straight through, no second remap). **Arm against A's media playback
position, not wall clock** — `A_playbackStart + startMs` desyncs under pause/seek/buffering/rate change; trigger
when renderer A's playback position reaches `startMs`. Seek renderer B to `bEntryMs` and prime it (decode + fill,
held muted) during the analyze window; verify B has `bEntryMs + xfadeMs` of decodable audio before arming. Same
`CONSTANT_POWER` coupling on the shared playback clock. Gate/fallback: if `!recipe.proceed` or A-tail/B-head
couldn't be fetched in time, `AudioFadeControl` runs its default/manual crossfade.

---

## 7. Parity gate

Run the reference training preprocessor and the on-device pipeline on the **same** A-tail/B-head WAV pair.

- **Compare mels in fp32, before any fp16 cast.** `mel_a`/`mel_b` max-abs-diff ≤ **~1e-3 at fp32**, no
  frame/axis offset.
- **Don't claim `1e-3` on an fp16-cast tensor.** fp16 has ~10 mantissa bits; near −80 dB the representable step
  is ≈ 0.06, so the cast alone injects ~0.03–0.06. If (and only if) the tflite input tensor is fp16, apply a
  realistic fp16 tolerance (~0.05, larger near −80). If the input tensor is fp32 (common: fp16 = *weight*
  quantization, runtime upcasts on CPU), keep `1e-3`.
- **Isolate preprocessing from model precision.** Feed *both* reference- and device-preprocessed inputs into the
  **same `automix_v2_1.tflite`, on the same delegate as production** (CPU vs GPU/fp16 delegate changes internal
  precision). Comparing device-preproc→fp16-model against training-preproc→fp32-training-model conflates the two
  and always "diverges."
- **aux:** exact per-slot match within each slot's confirmed scaling tolerance.
- **End-to-end:** the five outputs reproduce logged references (`d=0.085→7117 ms`, `type=0`, etc.) **using the
  confirmed affine/fraction ms-mappings** — not a linear-through-origin assumption. If outputs diverge,
  **preprocessing (or the mapping) is wrong — fix it there; never compensate in the ms-scale constants.**
- **Fallback coverage:** verify each §5 fallback fires (B-not-ready, sub-5 s, unseekable/live A, low `c`,
  inference timeout, NaN/out-of-range output) and yields a clean default crossfade with **no audio-thread stall
  or xrun**.

---

## 8. CONFIRM from training code (blockers before parity is claimed)

**Mel params:** `n_fft`, `win_length`, window type + periodic/symmetric; `center`/`pad_mode`; **per-slice vs
full-track STFT-then-slice** (boundary frames); `power`; mel `fmin`/`fmax`/`mel_norm`/`mel_scale`; **dB reference**
(`max` vs fixed) + `top_db` + `eps`/floor + log base; **input normalization** (raw dB vs `[0,1]` vs mean/std);
tensor axis order; **resampler algorithm AND resample-vs-slice order**; stereo→mono downmix; PCM→float scale;
short-track behavior (pad vs skip).

**32 aux slots:** exact **order and count**; `rms` definition (linear vs dBFS); `bpm` estimator + **analysis span
(5 s vs full track)** + units; beat-tracker confidence index; the 28 unknown features + **per-slot
scaling/normalization**.

**d/o/s scale constants:** `f_d` (affine vs nonlinear; log fit `≈4956 + 25421*d`, 2 points only); `O_MAX` for `o`
(log implies ~10000 ms); confirm `s` is fraction-of-length and **which length** (decoded sample count vs container
duration); `XFADE_MIN`/`XFADE_MAX`. Also confirm `S_MIN` (smallest emitted start-fraction), `C_GATE`/`C_MIN`,
`C_REJECT`, and whether `ANALYZE_LEAD` covers the trusted `s` band (or make it adaptive).

**6 type-class order:** the integer→`FadeEffectType` map (`type=0` seen; do **not** assume 0 == LINEAR) **and**
whether output 4 is a 6-logit tensor (`argmax`) or an already-reduced fp16 float index (round-to-nearest).

---

## 9. Definition of Done

- Shared `AutoMixModule` + `AutoMixScheduler` drive **both** engines through one `DeckHost` interface; `plan()`
  and all mel DSP/inference run **off the audio thread**.
- On-device mel + aux pipeline **bit-matches training** — parity gate (§7) green in fp32, like-for-like model,
  every §8 item confirmed (no guessed constants remain in code).
- `automix_v2_1.tflite` produces a `CrossfadeRecipe` for real pairs; the five outputs reproduce the logged
  references via the confirmed mappings.
- `mel_a` = A's file tail ending at EOS; `mel_b` = B's head from sample 0 — sourced via out-of-band decode on
  streaming (never a playback-tap ring buffer, never zero-padded).
- Gating honors `c`; realizability (fits-in-A, schedulable-start, joint B-entry bounds) and fp16 validation
  enforced; **every fallback path (§5) is reachable before the trigger** and yields a clean declicked transition.
- Streaming B-head prefetch is sized `max(5 s, maxOffset+maxDuration)`, kicked ahead of analyze, jitter-guarded,
  with a watchdog that never blocks the audio thread.
- Lock-free plan hand-off with RCU reclamation; interpreter pre-warmed; circuit breaker latches MANUAL on
  thermal/timeout/overrun. **No JUCE xruns / streaming underruns at the swap boundary.**
- Recipe cache keyed by `PairKey`+`modelVersion`, never caches FALLBACK, prewarms the next pair (throttled on
  streaming).
- Telemetry emits per-transition decisions + separated fallback-cause metrics for threshold tuning.
- **Behavior of the already-shipping offline path is unchanged except where the recipe is now model-driven** —
  this is an integration, not a rewrite.
