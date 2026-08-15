cmake_minimum_required(VERSION 3.22.1)

project("fxmobile_dsp" LANGUAGES CXX)

set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

set(CMAKE_CXX_FLAGS_RELEASE "${CMAKE_CXX_FLAGS_RELEASE} -O3 -flto -fvisibility=hidden -fvisibility-inlines-hidden -ffast-math -fno-rtti")

find_package(oboe REQUIRED CONFIG)

set(FXENGINE_DIR ${CMAKE_CURRENT_SOURCE_DIR}/../../../../native/fxengine)

if(ANDROID_ABI STREQUAL "arm64-v8a")
    set(RUST_TARGET aarch64-linux-android)
    set(RUST_CC_TRIPLE aarch64-linux-android)
elseif(ANDROID_ABI STREQUAL "armeabi-v7a")
    set(RUST_TARGET armv7-linux-androideabi)
    set(RUST_CC_TRIPLE armv7a-linux-androideabi)
elseif(ANDROID_ABI STREQUAL "x86_64")
    set(RUST_TARGET x86_64-linux-android)
    set(RUST_CC_TRIPLE x86_64-linux-android)
elseif(ANDROID_ABI STREQUAL "x86")
    set(RUST_TARGET i686-linux-android)
    set(RUST_CC_TRIPLE i686-linux-android)
else()
    message(FATAL_ERROR "Unsupported ANDROID_ABI=${ANDROID_ABI}")
endif()

if(CMAKE_HOST_WIN32)
    set(NDK_HOST windows-x86_64)
    set(CLANG_EXT .cmd)
    set(AR_NAME llvm-ar.exe)
elseif(CMAKE_HOST_APPLE)
    set(NDK_HOST darwin-x86_64)
    set(CLANG_EXT "")
    set(AR_NAME llvm-ar)
else()
    set(NDK_HOST linux-x86_64)
    set(CLANG_EXT "")
    set(AR_NAME llvm-ar)
endif()

set(NDK_LLVM ${ANDROID_NDK}/toolchains/llvm/prebuilt/${NDK_HOST}/bin)
set(RUST_API 26)
set(RUST_CC ${NDK_LLVM}/${RUST_CC_TRIPLE}${RUST_API}-clang${CLANG_EXT})
set(RUST_AR ${NDK_LLVM}/${AR_NAME})

find_program(CARGO_EXECUTABLE cargo
    HINTS "$ENV{USERPROFILE}/.cargo/bin" "$ENV{HOME}/.cargo/bin"
    REQUIRED)

string(TOUPPER "${RUST_TARGET}" RUST_TARGET_UPPER)
string(REPLACE "-" "_" RUST_TARGET_ENV "${RUST_TARGET_UPPER}")
set(LINKER_ENV CARGO_TARGET_${RUST_TARGET_ENV}_LINKER)

set(RUST_LIB ${CMAKE_CURRENT_BINARY_DIR}/rust/${RUST_TARGET}/release/libfxengine.a)

file(GLOB_RECURSE FXENGINE_SOURCES
    ${FXENGINE_DIR}/src/*.rs
    ${FXENGINE_DIR}/Cargo.toml
)

add_custom_command(
    OUTPUT ${RUST_LIB}
    COMMAND ${CMAKE_COMMAND} -E env
        CARGO_TARGET_DIR=${CMAKE_CURRENT_BINARY_DIR}/rust
        ${LINKER_ENV}=${RUST_CC}
        AR=${RUST_AR}
        ${CARGO_EXECUTABLE} build --release --target ${RUST_TARGET} --manifest-path ${FXENGINE_DIR}/Cargo.toml
    DEPENDS ${FXENGINE_SOURCES}
    COMMENT "Building Rust fxengine for ${RUST_TARGET}"
    VERBATIM
)

add_custom_target(fxengine_rust DEPENDS ${RUST_LIB})

add_library(fxengine STATIC IMPORTED)
set_target_properties(fxengine PROPERTIES IMPORTED_LOCATION ${RUST_LIB})
add_dependencies(fxengine fxengine_rust)

include_directories(
    dsp
    audio
    ${FXENGINE_DIR}/include
)

add_library(
    fxmobile_dsp
    SHARED
    native-lib.cpp
    audio/OboeAudioStream.cpp
)

add_dependencies(fxmobile_dsp fxengine_rust)

target_link_libraries(
    fxmobile_dsp
    PRIVATE
    fxengine
    oboe::oboe
    log
    android
    dl
    m
)
