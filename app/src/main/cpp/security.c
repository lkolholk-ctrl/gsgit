#include <jni.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <arpa/inet.h>
#include <sys/stat.h>
#include <sys/system_properties.h>
#include <dirent.h>
#include <android/log.h>


/* ════════════════════════════════════════════════════════
   BUILD-TIME XOR KEY — O-MVLL зашифрует это дополнительно
   ════════════════════════════════════════════════════════ */
#define BK0 0x4A
#define BK1 0x7F
#define BK2 0x3C
#define BK3 0xB1
#define BK4 0x92
#define BK5 0xE5
#define BK6 0x28
#define BK7 0xD4

/* Compile-time key lookup — только #define, никаких массивов */
#define BK(i) ((i)%8==0?BK0:(i)%8==1?BK1:(i)%8==2?BK2:(i)%8==3?BK3:\
               (i)%8==4?BK4:(i)%8==5?BK5:(i)%8==6?BK6:BK7)

/* Runtime массив для decode() */
static const uint8_t BUILD_KEY[8] = {BK0,BK1,BK2,BK3,BK4,BK5,BK6,BK7};

/* ════════════════════════════════════════════════════════
   HARDWARE KEY — генерируется из железа устройства
   ════════════════════════════════════════════════════════ */

static uint32_t djb2(const char* s, size_t len) {
    uint32_t h = 5381;
    for (size_t i = 0; i < len; i++)
        h = ((h << 5) + h) ^ (uint8_t)s[i];
    return h;
}

static void derive_hw_key(uint8_t out[8]) {
    char serial[PROP_VALUE_MAX] = {0};
    char board[PROP_VALUE_MAX]  = {0};
    char hw[PROP_VALUE_MAX]     = {0};
    char cpu[PROP_VALUE_MAX]    = {0};

    __system_property_get("ro.serialno",        serial);
    __system_property_get("ro.product.board",   board);
    __system_property_get("ro.boot.hardware",   hw);
    __system_property_get("ro.hardware",        cpu);

    char buf[PROP_VALUE_MAX * 4];
    int n = snprintf(buf, sizeof(buf), "%s|%s|%s|%s", serial, board, hw, cpu);
    if (n <= 0) n = 1;

    uint32_t h0 = djb2(buf, n);
    uint32_t h1 = djb2(buf + (n / 2), n - (n / 2));

    /* Собираем 8-байтный ключ из двух хэшей */
    out[0] = (h0 >>  0) & 0xFF;
    out[1] = (h0 >>  8) & 0xFF;
    out[2] = (h0 >> 16) & 0xFF;
    out[3] = (h0 >> 24) & 0xFF;
    out[4] = (h1 >>  0) & 0xFF;
    out[5] = (h1 >>  8) & 0xFF;
    out[6] = (h1 >> 16) & 0xFF;
    out[7] = (h1 >> 24) & 0xFF;

    /* Подмешиваем BUILD_KEY → итоговый ключ = HW XOR BUILD */
    for (int i = 0; i < 8; i++)
        out[i] ^= BUILD_KEY[i];

    memset(buf, 0, sizeof(buf));
}

/* Кэшируем ключ — вычисляем один раз */
static uint8_t  g_hw_key[8];
static int      g_hw_key_ready = 0;


static const uint8_t* get_hw_key(void) {
    if (!g_hw_key_ready) {
        
        if (!g_hw_key_ready) {
            derive_hw_key(g_hw_key);
            g_hw_key_ready = 1;
        }
        
    }
    return g_hw_key;
}

/* ════════════════════════════════════════════════════════
   МАКРОСЫ: шифрование строк compile-time
   X(byte, idx) = byte XOR BUILD_KEY[idx % 8]
   ════════════════════════════════════════════════════════ */
#define X(b, i) ((uint8_t)((b) ^ BK(i)))

/* Расшифровка зашифрованного буфера статическим ключом сборки */
static void decode(uint8_t* buf, size_t len) {
    for (size_t i = 0; i < len; i++)
        buf[i] ^= BUILD_KEY[i % 8];
}

/* Затереть буфер после использования */
#define WIPE(buf, len) do { volatile uint8_t* _p = (volatile uint8_t*)(buf); \
    for(size_t _i = 0; _i < (len); _i++) _p[_i] = 0; } while(0)

/* ════════════════════════════════════════════════════════
   ЗАШИФРОВАННЫЕ СТРОКИ (compile-time XOR с BUILD_KEY)
   Python для генерации:
     s = "/proc/self/maps"
     bk = [0x4A,0x7F,0x3C,0xB1,0x92,0xE5,0x28,0xD4]
     print([hex(c ^ bk[i%8]) for i,c in enumerate(s.encode())])
   ════════════════════════════════════════════════════════ */

/* "/proc/self/maps" */
static const uint8_t ENC_MAPS[] = {
    X('/',0),X('p',1),X('r',2),X('o',3),X('c',4),X('/',5),
    X('s',6),X('e',7),X('l',0),X('f',1),X('/',2),X('m',3),
    X('a',4),X('p',5),X('s',6),0x00
};

/* "frida" */
static const uint8_t ENC_FRIDA[] = {
    X('f',0),X('r',1),X('i',2),X('d',3),X('a',4),0x00
};

/* "gum-js-loop" */
static const uint8_t ENC_GUM[] = {
    X('g',0),X('u',1),X('m',2),X('-',3),X('j',4),X('s',5),
    X('-',6),X('l',7),X('o',0),X('o',1),X('p',2),0x00
};

/* "linjector" */
static const uint8_t ENC_LINJ[] = {
    X('l',0),X('i',1),X('n',2),X('j',3),X('e',4),X('c',5),
    X('t',6),X('o',7),X('r',0),0x00
};

/* "frida-agent" */
static const uint8_t ENC_AGENT[] = {
    X('f',0),X('r',1),X('i',2),X('d',3),X('a',4),X('-',5),
    X('a',6),X('g',7),X('e',0),X('n',1),X('t',2),0x00
};

/* "/system/bin/su" */
static const uint8_t ENC_SU[] = {
    X('/',0),X('s',1),X('y',2),X('s',3),X('t',4),X('e',5),
    X('m',6),X('/',7),X('b',0),X('i',1),X('n',2),X('/',3),
    X('s',4),X('u',5),0x00
};

/* "/system/xbin/su" */
static const uint8_t ENC_XBIN_SU[] = {
    X('/',0),X('s',1),X('y',2),X('s',3),X('t',4),X('e',5),
    X('m',6),X('/',7),X('x',0),X('b',1),X('i',2),X('n',3),
    X('/',4),X('s',5),X('u',6),0x00
};

/* "/sbin/su" */
static const uint8_t ENC_SBIN_SU[] = {
    X('/',0),X('s',1),X('b',2),X('i',3),X('n',4),X('/',5),
    X('s',6),X('u',7),0x00
};

/* "/data/local/su" */
static const uint8_t ENC_LOCAL_SU[] = {
    X('/',0),X('d',1),X('a',2),X('t',3),X('a',4),X('/',5),
    X('l',6),X('o',7),X('c',0),X('a',1),X('l',2),X('/',3),
    X('s',4),X('u',5),0x00
};

/* "/sbin/.magisk" */
static const uint8_t ENC_MAGISK1[] = {
    X('/',0),X('s',1),X('b',2),X('i',3),X('n',4),X('/',5),
    X('.',6),X('m',7),X('a',0),X('g',1),X('i',2),X('s',3),
    X('k',4),0x00
};

/* "/data/adb/magisk" */
static const uint8_t ENC_MAGISK2[] = {
    X('/',0),X('d',1),X('a',2),X('t',3),X('a',4),X('/',5),
    X('a',6),X('d',7),X('b',0),X('/',1),X('m',2),X('a',3),
    X('g',4),X('i',5),X('s',6),X('k',7),0x00
};

/* "/data/adb/ksu" */
static const uint8_t ENC_KSU[] = {
    X('/',0),X('d',1),X('a',2),X('t',3),X('a',4),X('/',5),
    X('a',6),X('d',7),X('b',0),X('/',1),X('k',2),X('s',3),
    X('u',4),0x00
};

/* "/data/adb/apatch" */
static const uint8_t ENC_APATCH[] = {
    X('/',0),X('d',1),X('a',2),X('t',3),X('a',4),X('/',5),
    X('a',6),X('d',7),X('b',0),X('/',1),X('a',2),X('p',3),
    X('a',4),X('t',5),X('c',6),X('h',7),0x00
};

/* "/proc/self/status" */
static const uint8_t ENC_STATUS[] = {
    X('/',0),X('p',1),X('r',2),X('o',3),X('c',4),X('/',5),
    X('s',6),X('e',7),X('l',0),X('f',1),X('/',2),X('s',3),
    X('t',4),X('a',5),X('t',6),X('u',7),X('s',0),0x00
};

/* "TracerPid:" */
static const uint8_t ENC_TRACER[] = {
    X('T',0),X('r',1),X('a',2),X('c',3),X('e',4),X('r',5),
    X('P',6),X('i',7),X('d',0),X(':',1),0x00
};

/* "ro.kernel.qemu" */
static const uint8_t ENC_PROP_QEMU[] = {
    X('r',0),X('o',1),X('.',2),X('k',3),X('e',4),X('r',5),
    X('n',6),X('e',7),X('l',0),X('.',1),X('q',2),X('e',3),
    X('m',4),X('u',5),0x00
};

/* "ro.hardware" */
static const uint8_t ENC_PROP_HW[] = {
    X('r',0),X('o',1),X('.',2),X('h',3),X('a',4),X('r',5),
    X('d',6),X('w',7),X('a',0),X('r',1),X('e',2),0x00
};

/* "goldfish" */
static const uint8_t ENC_GOLDFISH[] = {
    X('g',0),X('o',1),X('l',2),X('d',3),X('f',4),X('i',5),
    X('s',6),X('h',7),0x00
};

/* "ranchu" */
static const uint8_t ENC_RANCHU[] = {
    X('r',0),X('a',1),X('n',2),X('c',3),X('h',4),X('u',5),
    0x00
};

/* Декодировать строку во временный буфер */
#define DECODE_STR(enc, tmp) \
    uint8_t tmp[sizeof(enc)]; \
    memcpy(tmp, enc, sizeof(enc)); \
    decode(tmp, sizeof(enc) - 1);

/* ════════════════════════════════════════════════════════
   FRIDA PORT DETECTION
   ════════════════════════════════════════════════════════ */
static int check_port(int port) {
    int sock = socket(AF_INET, SOCK_STREAM, 0);
    if (sock < 0) return 0;

    struct timeval tv = {0, 150000};
    setsockopt(sock, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));
    setsockopt(sock, SOL_SOCKET, SO_SNDTIMEO, &tv, sizeof(tv));

    struct sockaddr_in addr;
    memset(&addr, 0, sizeof(addr));
    addr.sin_family      = AF_INET;
    addr.sin_port        = htons((uint16_t)port);
    addr.sin_addr.s_addr = htonl(INADDR_LOOPBACK);

    int r = connect(sock, (struct sockaddr*)&addr, sizeof(addr));
    close(sock);
    return r == 0;
}

static int detect_frida_ports(void) {
    /* Порты в виде арифметики — не видны как константы */
    int base = 27000 + 42;
    int ports[6];
    ports[0] = base;
    ports[1] = base + 1;
    ports[2] = base + 2;
    ports[3] = base - 1;
    ports[4] = base - 2;
    ports[5] = base - 3;
    for (int i = 0; i < 6; i++)
        if (check_port(ports[i])) return 1;
    return 0;
}

/* ════════════════════════════════════════════════════════
   /proc/self/maps SCAN
   ════════════════════════════════════════════════════════ */
static int detect_frida_maps(void) {
    DECODE_STR(ENC_MAPS, path_buf);
    FILE* f = fopen((char*)path_buf, "r");
    WIPE(path_buf, sizeof(path_buf));
    if (!f) return 0;

    DECODE_STR(ENC_FRIDA,  s_frida);
    DECODE_STR(ENC_GUM,    s_gum);
    DECODE_STR(ENC_LINJ,   s_linj);
    DECODE_STR(ENC_AGENT,  s_agent);

    char line[512];
    int found = 0;
    while (fgets(line, sizeof(line), f)) {
        if (strstr(line, (char*)s_frida)  ||
            strstr(line, (char*)s_gum)    ||
            strstr(line, (char*)s_linj)   ||
            strstr(line, (char*)s_agent)) {
            found = 1; break;
        }
    }
    fclose(f);
    WIPE(s_frida, sizeof(s_frida));
    WIPE(s_gum,   sizeof(s_gum));
    WIPE(s_linj,  sizeof(s_linj));
    WIPE(s_agent, sizeof(s_agent));
    return found;
}

/* ════════════════════════════════════════════════════════
   ROOT DETECTION
   ════════════════════════════════════════════════════════ */
static int detect_root(void) {
    DECODE_STR(ENC_SU,      su0);
    DECODE_STR(ENC_XBIN_SU, su1);
    DECODE_STR(ENC_SBIN_SU, su2);
    DECODE_STR(ENC_LOCAL_SU,su3);

    const char* paths[4] = {
        (char*)su0, (char*)su1, (char*)su2, (char*)su3
    };
    int found = 0;
    for (int i = 0; i < 4; i++)
        if (access(paths[i], F_OK) == 0) { found = 1; break; }

    WIPE(su0, sizeof(su0)); WIPE(su1, sizeof(su1));
    WIPE(su2, sizeof(su2)); WIPE(su3, sizeof(su3));
    return found;
}

/* ════════════════════════════════════════════════════════
   MAGISK DETECTION
   ════════════════════════════════════════════════════════ */
static int detect_magisk(void) {
    DECODE_STR(ENC_MAGISK1, m0);
    DECODE_STR(ENC_MAGISK2, m1);
    DECODE_STR(ENC_KSU,     m2);
    DECODE_STR(ENC_APATCH,  m3);

    const char* paths[4] = {
        (char*)m0, (char*)m1, (char*)m2, (char*)m3
    };
    int found = 0;
    for (int i = 0; i < 4; i++)
        if (access(paths[i], F_OK) == 0) { found = 1; break; }

    WIPE(m0, sizeof(m0)); WIPE(m1, sizeof(m1));
    WIPE(m2, sizeof(m2)); WIPE(m3, sizeof(m3));
    return found;
}

static int detect_debugger(void) {
    DECODE_STR(ENC_STATUS, path_buf);
    FILE* f = fopen((char*)path_buf, "r");
    WIPE(path_buf, sizeof(path_buf));
    if (!f) return 0;

    DECODE_STR(ENC_TRACER, prefix);
    char line[128];
    int tracer_pid = 0;
    while (fgets(line, sizeof(line), f)) {
        if (strncmp(line, (char*)prefix, 10) == 0) {
            tracer_pid = atoi(&line[10]);
            break;
        }
    }
    fclose(f);
    WIPE(prefix, sizeof(prefix));
    return tracer_pid != 0;
}

static int detect_emulator(void) {
    char qemu[PROP_VALUE_MAX] = {0};
    char hw[PROP_VALUE_MAX] = {0};

    DECODE_STR(ENC_PROP_QEMU, prop_qemu);
    __system_property_get((char*)prop_qemu, qemu);
    WIPE(prop_qemu, sizeof(prop_qemu));

    DECODE_STR(ENC_PROP_HW, prop_hw);
    __system_property_get((char*)prop_hw, hw);
    WIPE(prop_hw, sizeof(prop_hw));

    if (strcmp(qemu, "1") == 0) return 1;

    DECODE_STR(ENC_GOLDFISH, val_goldfish);
    DECODE_STR(ENC_RANCHU, val_ranchu);
    int is_emu = (strstr(hw, (char*)val_goldfish) != NULL || strstr(hw, (char*)val_ranchu) != NULL);
    WIPE(val_goldfish, sizeof(val_goldfish));
    WIPE(val_ranchu, sizeof(val_ranchu));

    return is_emu;
}

static int run_all_checks(void) {
    if (detect_frida_ports()) return 1;
    if (detect_frida_maps())  return 2;
    if (detect_root())        return 3;
    if (detect_magisk())      return 4;
    if (detect_debugger())    return 5;
    if (detect_emulator())    return 6;
    return 0;
}

/* ════════════════════════════════════════════════════════
   GITHUB TOKEN ENCRYPTION
   Шифруем токен hardware key-ом перед сохранением
   ════════════════════════════════════════════════════════ */

JNIEXPORT jbyteArray JNICALL
Java_gs_git_vps_security_NativeSecurity_encryptToken(
        JNIEnv* env, jclass clazz, jstring token) {
    (void)clazz;
    const char* t = (*env)->GetStringUTFChars(env, token, NULL);
    if (!t) return NULL;
    size_t len = strlen(t);

    jbyteArray result = (*env)->NewByteArray(env, (jsize)len);
    if (!result) { (*env)->ReleaseStringUTFChars(env, token, t); return NULL; }

    uint8_t* enc = (uint8_t*)malloc(len);
    if (!enc) { (*env)->ReleaseStringUTFChars(env, token, t); return result; }

    const uint8_t* k = get_hw_key();
    for (size_t i = 0; i < len; i++)
        enc[i] = (uint8_t)t[i] ^ k[i % 8];

    (*env)->SetByteArrayRegion(env, result, 0, (jsize)len, (jbyte*)enc);
    WIPE(enc, len);
    free(enc);
    (*env)->ReleaseStringUTFChars(env, token, t);
    return result;
}

JNIEXPORT jstring JNICALL
Java_gs_git_vps_security_NativeSecurity_decryptToken(
        JNIEnv* env, jclass clazz, jbyteArray encrypted) {
    (void)clazz;
    jsize len = (*env)->GetArrayLength(env, encrypted);
    if (len <= 0) return NULL;

    jbyte* raw = (*env)->GetByteArrayElements(env, encrypted, NULL);
    if (!raw) return NULL;

    uint8_t* dec = (uint8_t*)malloc(len + 1);
    if (!dec) { (*env)->ReleaseByteArrayElements(env, encrypted, raw, JNI_ABORT); return NULL; }

    const uint8_t* k = get_hw_key();
    for (jsize i = 0; i < len; i++)
        dec[i] = (uint8_t)raw[i] ^ k[i % 8];
    dec[len] = 0;

    jstring result = (*env)->NewStringUTF(env, (char*)dec);
    WIPE(dec, len + 1);
    free(dec);
    (*env)->ReleaseByteArrayElements(env, encrypted, raw, JNI_ABORT);
    return result;
}

/* ════════════════════════════════════════════════════════
   JNI EXPORTS
   ════════════════════════════════════════════════════════ */

JNIEXPORT jint JNICALL
Java_gs_git_vps_security_NativeSecurity_runSecurityChecks(
        JNIEnv* env, jclass clazz) {
    (void)env; (void)clazz;
    return run_all_checks();
}

JNIEXPORT jboolean JNICALL
Java_gs_git_vps_security_NativeSecurity_isFridaDetected(
        JNIEnv* env, jclass clazz) {
    (void)env; (void)clazz;
    return (jboolean)(detect_frida_ports() || detect_frida_maps());
}

JNIEXPORT jboolean JNICALL
Java_gs_git_vps_security_NativeSecurity_isRooted(
        JNIEnv* env, jclass clazz) {
    (void)env; (void)clazz;
    return (jboolean)detect_root();
}

JNIEXPORT jboolean JNICALL
Java_gs_git_vps_security_NativeSecurity_isMagiskDetected(
        JNIEnv* env, jclass clazz) {
    (void)env; (void)clazz;
    return (jboolean)detect_magisk();
}

JNIEXPORT jboolean JNICALL
Java_gs_git_vps_security_NativeSecurity_isDebuggerDetected(
        JNIEnv* env, jclass clazz) {
    (void)env; (void)clazz;
    return (jboolean)detect_debugger();
}

JNIEXPORT jboolean JNICALL
Java_gs_git_vps_security_NativeSecurity_isEmulatorDetected(
        JNIEnv* env, jclass clazz) {
    (void)env; (void)clazz;
    return (jboolean)detect_emulator();
}

JNIEXPORT jboolean JNICALL
Java_gs_git_vps_security_NativeSecurity_isEnvironmentSafe(
        JNIEnv* env, jclass clazz) {
    (void)env; (void)clazz;
    return (jboolean)(run_all_checks() == 0);
}
