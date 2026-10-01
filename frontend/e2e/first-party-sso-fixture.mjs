import { createHash, randomBytes } from 'node:crypto';

/** Legacy first-party protocol fixture: initiate PKCE directly, without reintroducing a product login button. */
export async function startFirstPartyAuthorization(page, { app, appOrigin, clientId, authorizeUrl, returnTo }) {
  const redirectUri = `${appOrigin.replace(/\/$/, '')}/sso/callback`;
  const verifier = randomBytes(32).toString('base64url');
  const state = randomBytes(32).toString('base64url');
  await page.goto(`${appOrigin.replace(/\/$/, '')}/login`);
  await page.evaluate(({ key, pending }) => sessionStorage.setItem(key, JSON.stringify(pending)), {
    key: `namewta-${app}-sso-pending`,
    pending: { clientId, redirectUri, verifier, state, returnTo }
  });
  const authorize = new URL(authorizeUrl);
  authorize.search = new URLSearchParams({
    response_type: 'code',
    client_id: clientId,
    redirect_uri: redirectUri,
    state,
    code_challenge: createHash('sha256').update(verifier).digest('base64url'),
    code_challenge_method: 'S256'
  }).toString();
  await page.goto(authorize.toString(), { waitUntil: 'commit' });
}
