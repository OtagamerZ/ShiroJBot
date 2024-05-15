/*
 * This file is part of Shiro J Bot.
 * Copyright (C) 2019-2023  Yago Gimenez (KuuHaKu)
 *
 * Shiro J Bot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Shiro J Bot is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Shiro J Bot.  If not, see <https://www.gnu.org/licenses/>
 */

package com.kuuhaku.util;

import com.kuuhaku.Constants;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.cert.CertificateException;

public abstract class Certifier {
    private static KeyPair keys;

    private static KeyPair getKeys() {
        if (keys == null) {
            try (InputStream is = IO.getResourceAsStream("keystore.jks")) {
                char[] p = System.getenv("DB_PASS").toCharArray();

                KeyStore keyStore = KeyStore.getInstance("JCEKS");
                keyStore.load(is, p);
                KeyStore.PasswordProtection pass = new KeyStore.PasswordProtection(p);
                KeyStore.PrivateKeyEntry entry = (KeyStore.PrivateKeyEntry) keyStore.getEntry("key", pass);

                PublicKey pubKey = entry.getCertificate().getPublicKey();
                PrivateKey privKey = entry.getPrivateKey();

                keys = new KeyPair(pubKey, privKey);
            } catch (NoSuchAlgorithmException | IOException | KeyStoreException | CertificateException |
                     UnrecoverableEntryException e) {
				Constants.LOGGER.error("Failed to retrieve certification keys: {}", e, e);
            }
        }

        return keys;
    }

    public static String sign(String data) {
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(getKeys().getPrivate());
            sig.update(data.getBytes(StandardCharsets.UTF_8));

            return IO.atob(sig.sign());
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
			Constants.LOGGER.error("Failed to sign data: {}", e, e);
            return null;
        }
    }

    public static boolean verify(String data) {
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(getKeys().getPublic());
            sig.update(data.getBytes(StandardCharsets.UTF_8));

            return sig.verify(IO.btoa(data));
        } catch (NoSuchAlgorithmException | SignatureException | InvalidKeyException e) {
			Constants.LOGGER.error("Failed to verify data: {}", e, e);
            return false;
        }
    }
}
