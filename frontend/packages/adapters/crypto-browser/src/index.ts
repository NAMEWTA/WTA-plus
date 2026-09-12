import type { CryptoPort, EncryptedRequest } from '@namewta/platform-contracts';
import * as CryptoJSModule from 'crypto-js';
import { JSEncrypt } from 'jsencrypt';

const CryptoJS = ('default' in CryptoJSModule ? CryptoJSModule.default : CryptoJSModule) as typeof CryptoJSModule;

export interface BrowserCryptoOptions {
  privateKey: string;
  publicKey: string;
  randomValues?: (bytes: Uint8Array<ArrayBuffer>) => Uint8Array<ArrayBuffer>;
}

export function createBrowserCryptoAdapter(options: BrowserCryptoOptions): CryptoPort {
  if (!options.publicKey.trim()) throw new Error('Request public key is required');
  if (!options.privateKey.trim()) throw new Error('Response private key is required');
  const randomValues = options.randomValues ?? (bytes => globalThis.crypto.getRandomValues(bytes));

  return {
    encryptRequest(data: string): EncryptedRequest {
      const random = randomValues(new Uint8Array(new ArrayBuffer(32)));
      const keyText = Array.from(random, value => value.toString(16).padStart(2, '0'))
        .join('')
        .slice(0, 32);
      const aesKey = CryptoJS.enc.Utf8.parse(keyText);
      const wrapper = new JSEncrypt();
      wrapper.setPublicKey(options.publicKey);
      const encryptedKey = wrapper.encrypt(CryptoJS.enc.Base64.stringify(aesKey));
      if (!encryptedKey) throw new Error('Unable to encrypt request key');
      return {
        data: CryptoJS.AES.encrypt(data, aesKey, { mode: CryptoJS.mode.ECB, padding: CryptoJS.pad.Pkcs7 }).toString(),
        encryptedKey
      };
    },
    decryptResponse(data: string, encryptedKey: string): unknown {
      if (!encryptedKey.trim()) throw new Error('Unable to decrypt response key');
      const wrapper = new JSEncrypt();
      wrapper.setPrivateKey(options.privateKey);
      const base64Key = wrapper.decrypt(encryptedKey);
      if (!base64Key) throw new Error('Unable to decrypt response key');
      const aesKey = CryptoJS.enc.Base64.parse(base64Key);
      const text = CryptoJS.AES.decrypt(data, aesKey, {
        mode: CryptoJS.mode.ECB,
        padding: CryptoJS.pad.Pkcs7
      }).toString(CryptoJS.enc.Utf8);
      try {
        return JSON.parse(text);
      } catch (cause) {
        throw new Error('Unable to decrypt response payload', { cause });
      }
    }
  };
}
