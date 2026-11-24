// Core sets for Zelda's mainline games, one edition per title.
// Source: https://retroachievements.org/hub/7438
const games: ReadonlyArray<readonly [number, string]> = [
  [1454, "The Legend of Zelda"],
  [1453, "Zelda II: The Adventure of Link"],
  [355, "A Link to the Past"],
  [5371, "Link's Awakening DX"],
  [10113, "Ocarina of Time"],
  [10679, "Majora's Mask"],
  [676, "Oracle of Seasons"],
  [710, "Oracle of Ages"],
  [22310, "Four Swords Anniversary Edition"],
  [9190, "The Wind Waker"],
  [3534, "Four Swords Adventures"],
  [559, "The Minish Cap"],
  [3934, "Twilight Princess"],
  [14323, "Phantom Hourglass"],
  [9898, "Spirit Tracks"],
  [202, "Skyward Sword"],
];

const supabase = Deno.env.get("SUPABASE_URL")!;
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!;
const anonKey = Deno.env.get("SUPABASE_ANON_KEY")!;

const json = (value: unknown, status = 200) =>
  Response.json(value, { status, headers: { "Cache-Control": "no-store" } });

async function rpc(name: string, body: unknown) {
  const response = await fetch(`${supabase}/rest/v1/rpc/${name}`, {
    method: "POST",
    headers: {
      apikey: serviceKey,
      Authorization: `Bearer ${serviceKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify(body),
    signal: AbortSignal.timeout(8000),
  });
  if (!response.ok) {
    const error = await response.json().catch(() => null);
    throw new Error(error?.code === "23505" ? "linked" : "database");
  }
  return response.json();
}

function count(value: unknown): number {
  const n = Number(value);
  return Number.isInteger(n) && n >= 0 && n <= 10000 ? n : 0;
}

async function retroRequest(endpoint: string, query: Record<string, string>, timeout = 9500) {
  const url = new URL(`https://retroachievements.org/API/${endpoint}.php`);
  url.search = new URLSearchParams(query).toString();
  // The official API expects the key in its query string. Never log this URL or errors from fetch.
  const remote = await fetch(url, { redirect: "error", signal: AbortSignal.timeout(timeout) });
  if ([401, 403, 404, 422].includes(remote.status)) throw new Error("credentials");
  if (!remote.ok) throw new Error("upstream");
  return remote.json();
}

// RetroAchievements orders completion rows by their last unlock, newest first.
async function latestZeldaUnlock(user: string, key: string, eligible: Set<number>) {
  if (!eligible.size) return null;
  const pageSize = 500;
  const deadline = Date.now() + 5000;
  for (let offset = 0; offset < pageSize * 3; offset += pageSize) {
    const remaining = deadline - Date.now();
    if (remaining <= 0) return null;
    const page = await retroRequest("API_GetUserCompletionProgress", {
      u: user, y: key, c: String(pageSize), o: String(offset),
    }, remaining);
    const results = page?.Results;
    if (!Array.isArray(results)) return null;
    for (const row of results) {
      const id = Number(row?.GameID);
      const rawDate = row?.MostRecentAwardedDate;
      if (eligible.has(id) && typeof rawDate === "string" && Number.isFinite(Date.parse(rawDate))) {
        return { id, date: new Date(rawDate).toISOString() };
      }
    }
    if (results.length < pageSize || offset + results.length >= Number(page.Total)) break;
  }
  return null;
}

Deno.serve(async (request) => {
  if (request.method !== "POST") return json({ error: "method" }, 405);
  const authorization = request.headers.get("Authorization");
  if (!authorization?.startsWith("Bearer ")) return json({ error: "auth" }, 401);
  try {
    const auth = await fetch(`${supabase}/auth/v1/user`, {
      headers: { apikey: anonKey, Authorization: authorization },
      signal: AbortSignal.timeout(8000),
    });
    if (!auth.ok) return json({ error: "auth" }, 401);
    const account = await auth.json();
    const input = await request.json().catch(() => null);
    const handle = typeof input?.handle === "string" ? input.handle.trim() : "";
    const key = typeof input?.key === "string" ? input.key.trim() : "";
    if (!/^[A-Za-z0-9_-]{1,32}$/.test(handle) || !/^[A-Za-z0-9]{16,128}$/.test(key)) {
      return json({ error: "credentials" }, 400);
    }
    if (!(await rpc("hc_retro_begin", { p_user: account.id }))) {
      return json({ error: "cooldown" }, 429);
    }
    const link = await rpc("hc_retro_link_status", { p_user: account.id });
    let verifiedHandle = link?.handle;
    let verifiedId = link?.retro_id;
    let challenge: string | null = null;
    if (!verifiedId || verifiedHandle.toLowerCase() !== handle.toLowerCase()) {
      if (!link?.challenge && !verifiedId) return json({ error: "verification" }, 400);
      const profile = await retroRequest("API_GetUserProfile", { u: handle, y: key });
      const retroId = profile?.ULID;
      const canonicalHandle = profile?.User;
      if (typeof retroId !== "string" || !/^[0-9A-HJKMNP-TV-Z]{26}$/.test(retroId) ||
          typeof canonicalHandle !== "string" || !/^[A-Za-z0-9_-]{1,32}$/.test(canonicalHandle)) {
        return json({ error: "credentials" }, 400);
      }
      if (retroId !== verifiedId) {
        if (!link?.challenge || typeof profile.Motto !== "string" || !profile.Motto.includes(link.challenge)) {
          return json({ error: "verification" }, 400);
        }
        challenge = link.challenge;
      }
      verifiedId = retroId;
      verifiedHandle = canonicalHandle;
    }
    const payload = await retroRequest("API_GetUserProgress", {
      u: verifiedId,
      y: key,
      i: games.map(([id]) => id).join(","),
    });
    if (!payload || typeof payload !== "object" || Array.isArray(payload)) {
      return json({ error: "retro_unavailable" }, 502);
    }
    // An empty object is ambiguous. Keep the saved snapshot when the upstream response cannot be verified.
    if (!games.some(([id]) => payload[id] && typeof payload[id] === "object")) {
      return json({ error: "credentials_or_empty" }, 400);
    }
    const progress = games.flatMap(([id, title]) => {
      const row = payload[id];
      if (!row) return [];
      const possible = count(row.NumPossibleAchievements ?? row.numPossibleAchievements);
      // Softcore includes achievements also unlocked in hardcore in this endpoint.
      const earned = Math.min(possible, Math.max(
        count(row.NumAchieved ?? row.numAchieved),
        count(row.NumAchievedHardcore ?? row.numAchievedHardcore),
      ));
      return earned > 0 ? [{ id, title, earned, possible }] : [];
    });
    const latest = await latestZeldaUnlock(verifiedId, key, new Set(progress.map((game) => game.id)))
      .catch(() => null);
    const snapshot = await rpc("hc_retro_save", {
      p_user: account.id,
      p_handle: verifiedHandle,
      p_retro_id: verifiedId,
      p_games: progress.map((game) => latest?.id === game.id ? { ...game, last_earned: latest.date } : game),
      p_challenge: challenge,
    });
    return json(snapshot);
  } catch (failure) {
    if (failure instanceof Error && failure.message === "credentials") return json({ error: "credentials" }, 400);
    if (failure instanceof Error && failure.message === "linked") return json({ error: "linked" }, 409);
    return json({ error: "sync_failed" }, 502);
  }
});
