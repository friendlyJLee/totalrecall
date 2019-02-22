/**
 * Modified from com_android_server_locksettings_SyntheticPasswordManager.cpp in AOSP
 */



#include <jni.h>
#include "crypto_scrypt.h"


jbyteArray Java_edu_rice_seclab_keyexporter_ScryptKeyExporter_nativeScrypt
        (JNIEnv* env, jobject thiz, jbyteArray password, jbyteArray salt, jint N, jint r, jint p, jint outLen)
{
    if (!password || !salt) {
         return NULL;
    }

    int passwordLen = (*env)->GetArrayLength(env, password);
    int saltLen = (*env)->GetArrayLength(env, salt);
    jbyteArray ret = (*env)->NewByteArray(env, outLen);

    jbyte* passwordPtr = (jbyte*)((*env)->GetByteArrayElements(env, password, NULL));
    jbyte* saltPtr = (jbyte*)((*env)->GetByteArrayElements(env, salt, NULL));
    jbyte* retPtr = (jbyte*)((*env)->GetByteArrayElements(env, ret, NULL));

    int rc = crypto_scrypt((const uint8_t *)passwordPtr, passwordLen,
               (const uint8_t *)saltPtr, saltLen, N, r, p, (uint8_t *)retPtr,
                            outLen);
    (*env)->ReleaseByteArrayElements(env, password, passwordPtr, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, salt, saltPtr, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, ret, retPtr, 0);

    if (!rc) {
         return ret;
    } else {
         return NULL;
    }
}

