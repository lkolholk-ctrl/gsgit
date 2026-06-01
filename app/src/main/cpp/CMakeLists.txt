cmake_minimum_required(VERSION 3.22.1)
project("gsgit_security")

set(OMVLL_PLUGIN "${CMAKE_SOURCE_DIR}/../omvll/libOMVLL.so")
set(OMVLL_CONFIG "${CMAKE_SOURCE_DIR}/../omvll/omvll_config.py")

add_library(gsgit_security SHARED security.c)

target_link_libraries(gsgit_security
    android
    log
)

if(CMAKE_BUILD_TYPE STREQUAL "Release")
    if(EXISTS "${OMVLL_PLUGIN}")
        target_compile_options(gsgit_security PRIVATE
            -fpass-plugin=${OMVLL_PLUGIN}
            -O2
        )
        target_compile_definitions(gsgit_security PRIVATE
            OMVLL_CONFIG=${OMVLL_CONFIG}
        )
        message(STATUS "O-MVLL obfuscation ENABLED")
    else()
        message(WARNING "O-MVLL plugin not found, building without obfuscation")
    endif()
endif()
