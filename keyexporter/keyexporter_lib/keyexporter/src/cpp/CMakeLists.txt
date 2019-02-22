cmake_minimum_required(VERSION 3.4.1)

add_library(scrypt_jni SHARED
             scrypt_jni.c crypto_scrypt.c sha256.c
              )

target_link_libraries(scrypt_jni android log)



