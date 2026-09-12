import * as CryptoJSModule from 'crypto-js';
import { beforeEach, describe, expect, it, vi } from 'vitest';

const CryptoJS = ('default' in CryptoJSModule ? CryptoJSModule.default : CryptoJSModule) as typeof CryptoJSModule;

const rsa = vi.hoisted(() => ({ decrypt: vi.fn(), encrypt: vi.fn(), setPrivateKey: vi.fn(), setPublicKey: vi.fn() }));
vi.mock('jsencrypt', () => ({
  JSEncrypt: class {
    decrypt = rsa.decrypt;
    encrypt = rsa.encrypt;
    setPrivateKey = rsa.setPrivateKey;
    setPublicKey = rsa.setPublicKey;
  }
}));

import { createBrowserCryptoAdapter } from './index';

describe('browser crypto adapter', () => {
  beforeEach(() => vi.clearAllMocks());

  it('fails closed when either independently injected key material is missing', () => {
    expect(() => createBrowserCryptoAdapter({ publicKey: '', privateKey: 'response-key' })).toThrow(
      'Request public key is required'
    );
    expect(() => createBrowserCryptoAdapter({ publicKey: 'request-key', privateKey: '' })).toThrow(
      'Response private key is required'
    );
  });

  it('wraps a generated request key with the injected request public key', () => {
    rsa.encrypt.mockReturnValue('wrapped-key');
    const adapter = createBrowserCryptoAdapter({
      publicKey: 'request-key',
      privateKey: 'response-key',
      randomValues: bytes => bytes.fill(1)
    });
    const result = adapter.encryptRequest('{"value":1}');
    expect(rsa.setPublicKey).toHaveBeenCalledWith('request-key');
    expect(result.encryptedKey).toBe('wrapped-key');
    expect(result.data).not.toContain('"value"');
  });

  it('fails closed when RSA request key wrapping fails', () => {
    rsa.encrypt.mockReturnValue(false);
    const adapter = createBrowserCryptoAdapter({
      publicKey: 'request-key',
      privateKey: 'response-key',
      randomValues: bytes => bytes.fill(1)
    });
    expect(() => adapter.encryptRequest('{"value":1}')).toThrow('Unable to encrypt request key');
  });

  it('decrypts response data with the separately injected response key', () => {
    const aesKey = CryptoJS.enc.Utf8.parse('1234567890123456');
    rsa.decrypt.mockReturnValue(CryptoJS.enc.Base64.stringify(aesKey));
    const encrypted = CryptoJS.AES.encrypt('{"code":200,"value":"ok"}', aesKey, {
      mode: CryptoJS.mode.ECB,
      padding: CryptoJS.pad.Pkcs7
    }).toString();
    const adapter = createBrowserCryptoAdapter({ publicKey: 'request-key', privateKey: 'response-key' });

    expect(adapter.decryptResponse(encrypted, 'wrapped-response-key')).toEqual({ code: 200, value: 'ok' });
    expect(rsa.setPrivateKey).toHaveBeenCalledWith('response-key');
    expect(rsa.decrypt).toHaveBeenCalledWith('wrapped-response-key');
  });

  it('fails closed for malformed response keys and payloads', () => {
    const adapter = createBrowserCryptoAdapter({ publicKey: 'request-key', privateKey: 'response-key' });
    rsa.decrypt.mockReturnValue(false);
    expect(() => adapter.decryptResponse('ciphertext', 'bad-key')).toThrow('Unable to decrypt response key');

    const aesKey = CryptoJS.enc.Utf8.parse('1234567890123456');
    rsa.decrypt.mockReturnValue(CryptoJS.enc.Base64.stringify(aesKey));
    const encrypted = CryptoJS.AES.encrypt('not-json', aesKey, {
      mode: CryptoJS.mode.ECB,
      padding: CryptoJS.pad.Pkcs7
    }).toString();
    expect(() => adapter.decryptResponse(encrypted, 'wrapped-response-key')).toThrow(
      'Unable to decrypt response payload'
    );
  });
});
