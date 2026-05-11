package com.uav.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Function;

@Slf4j
@Component
public class JwtKeyRotationService {
    private static final int MIN_KEY_LENGTH_BYTES = 32;
    private static final long ROTATION_INTERVAL_MS = 86400000L;
    private static final int MAX_HISTORY_SIZE = 3;

    @Value("${jwt.secret:}")
    private String secret;

    @Value("${jwt.expiration:86400000}")
    private long expiration;

    @Value("${jwt.key-rotation.enabled:true}")
    private boolean keyRotationEnabled;

    @Value("${jwt.key-store.path:}")
    private String keyStorePath;

    @Value("${jwt.key-store.password:}")
    private String keyStorePassword;

    @Value("${jwt.key.alias:jwt-signing-key}")
    private String keyAlias;

    private Key signingKey;
    private final Map<String, Key> keyVersionMap = new ConcurrentHashMap<>();
    private final AtomicLong currentKeyVersion = new AtomicLong(1);
    private long lastRotationTime;

    @PostConstruct
    public void init() {
        if (keyRotationEnabled) {
            initializeKeyRotation();
        } else {
            initializeStaticKey();
        }
        lastRotationTime = System.currentTimeMillis();
    }

    private void initializeStaticKey() {
        if (secret != null && !secret.isEmpty() && secret.getBytes().length >= MIN_KEY_LENGTH_BYTES) {
            this.signingKey = Keys.hmacShaKeyFor(secret.getBytes());
            keyVersionMap.put("v1", this.signingKey);
            log.info("JWT static key initialized (version: v1)");
        } else {
            KeyPair keyPair = generateKeyPair();
            this.signingKey = keyPair.getPrivate();
            keyVersionMap.put("v1", this.signingKey);
            log.warn("JWT auto-generated secure key (version: v1) - NOT for production without persistent storage");
        }
    }

    private void initializeKeyRotation() {
        if (keyStorePath != null && !keyStorePath.isEmpty()) {
            loadKeysFromKeyStore();
        } else {
            initializeFromSecret();
        }

        if (shouldRotate()) {
            rotateKey();
        }
    }

    private void initializeFromSecret() {
        String versionedSecret = secret + "_v" + currentKeyVersion.get();
        if (versionedSecret.getBytes().length >= MIN_KEY_LENGTH_BYTES) {
            this.signingKey = Keys.hmacShaKeyFor(versionedSecret.getBytes());
        } else {
            KeyPair keyPair = generateKeyPair();
            this.signingKey = keyPair.getPrivate();
        }
        keyVersionMap.put("v" + currentKeyVersion.get(), signingKey);
        log.info("JWT key initialized with version: v{}", currentKeyVersion.get());
    }

    private void loadKeysFromKeyStore() {
        try {
            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            char[] password = keyStorePassword != null ? keyStorePassword.toCharArray() : null;

            if (password != null && password.length > 0) {
                keyStore.load(null, password);
                Key key = keyStore.getKey(keyAlias, password);
                if (key != null) {
                    this.signingKey = key;
                    keyVersionMap.put("v" + currentKeyVersion.get(), signingKey);
                    log.info("JWT key loaded from keystore (version: v{})", currentKeyVersion.get());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load JWT key from keystore, generating new key: {}", e.getMessage());
            KeyPair keyPair = generateKeyPair();
            this.signingKey = keyPair.getPrivate();
            keyVersionMap.put("v" + currentKeyVersion.get(), signingKey);
        }
    }

    private boolean shouldRotate() {
        return System.currentTimeMillis() - lastRotationTime > ROTATION_INTERVAL_MS;
    }

    public synchronized Key rotateKey() {
        long newVersion = currentKeyVersion.incrementAndGet();
        String versionStr = "v" + newVersion;

        KeyPair newKeyPair = generateKeyPair();
        this.signingKey = newKeyPair.getPrivate();
        keyVersionMap.put(versionStr, this.signingKey);

        cleanupOldKeys(newVersion);

        lastRotationTime = System.currentTimeMillis();

        log.info("JWT key rotated to version: {}", versionStr);
        persistKeyVersionMapping();

        return this.signingKey;
    }

    private void cleanupOldKeys(long currentVersion) {
        long minVersionToKeep = currentVersion - MAX_HISTORY_SIZE;
        keyVersionMap.entrySet().removeIf(entry -> {
            String version = entry.getKey();
            try {
                long vNum = Long.parseLong(version.substring(1));
                return vNum < minVersionToKeep;
            } catch (NumberFormatException e) {
                return false;
            }
        });
    }

    private void persistKeyVersionMapping() {
        log.debug("Persisting key version mapping to Nacos/config server: {}", keyVersionMap.keySet());
    }

    public Key getSigningKey() {
        return signingKey;
    }

    public Key getSigningKeyForVersion(String version) {
        return keyVersionMap.get(version);
    }

    public String getCurrentKeyVersion() {
        return "v" + currentKeyVersion.get();
    }

    public Map<String, Object> getKeyVersionInfo() {
        Map<String, Object> info = new HashMap<>();
        info.put("currentVersion", getCurrentKeyVersion());
        info.put("availableVersions", keyVersionMap.keySet());
        info.put("lastRotationTime", new Date(lastRotationTime));
        info.put("nextRotationTime", new Date(lastRotationTime + ROTATION_INTERVAL_MS));
        info.put("rotationEnabled", keyRotationEnabled);
        return info;
    }

    private KeyPair generateKeyPair() {
        return Keys.keyPairFor(SignatureAlgorithm.RS256);
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public String extractKeyVersion(String token) {
        return extractClaim(token, claims -> claims.get("keyVersion", String.class));
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        Claims claims = null;
        String version = extractKeyVersion(token);

        Key key = version != null ? keyVersionMap.get(version) : null;

        if (key == null) {
            for (Map.Entry<String, Key> entry : keyVersionMap.entrySet()) {
                try {
                    claims = Jwts.parserBuilder()
                        .setSigningKey(entry.getValue())
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
                    break;
                } catch (Exception e) {
                    continue;
                }
            }
        } else {
            claims = Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
        }

        if (claims == null) {
            throw new SecurityException("Unable to validate token with any available key version");
        }

        return claims;
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("keyVersion", getCurrentKeyVersion());
        claims.put("tokenId", UUID.randomUUID().toString());
        return createToken(claims, userDetails.getUsername());
    }

    public String generateTokenWithKeyVersion(UserDetails userDetails, String keyVersion) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("keyVersion", keyVersion);
        claims.put("tokenId", UUID.randomUUID().toString());
        Key key = keyVersionMap.get(keyVersion);
        if (key == null) {
            throw new IllegalArgumentException("Invalid key version: " + keyVersion);
        }
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(userDetails.getUsername())
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(key, SignatureAlgorithm.RS256)
            .compact();
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
            .setClaims(claims)
            .setSubject(subject)
            .setIssuedAt(new Date(System.currentTimeMillis()))
            .setExpiration(new Date(System.currentTimeMillis() + expiration))
            .signWith(getSigningKey(), SignatureAlgorithm.RS256)
            .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            log.warn("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }

    public Boolean validateTokenWithVersion(String token, UserDetails userDetails, String expectedVersion) {
        try {
            String tokenVersion = extractKeyVersion(token);
            if (!expectedVersion.equals(tokenVersion)) {
                log.warn("Token version mismatch: expected {}, got {}", expectedVersion, tokenVersion);
                return false;
            }
            return validateToken(token, userDetails);
        } catch (Exception e) {
            log.warn("JWT token validation failed: {}", e.getMessage());
            return false;
        }
    }
}
