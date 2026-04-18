cmake_minimum_required(VERSION 3.18.1)

project("llama_jni")

# Set C++ standard
set(CMAKE_CXX_STANDARD 17)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# Find Android logging library
find_library(log-lib log)

# Add llama.cpp as subdirectory
add_subdirectory(llama.cpp)

# Include llama.cpp headers
include_directories(llama.cpp/include)

# Create JNI shared library
add_library(
    llama_jni
    SHARED
    llama_jni.cpp
)

# Link libraries
target_link_libraries(
    llama_jni
    llama
    ${log-lib}
)

# Set compile options
target_compile_options(
    llama_jni
    PRIVATE
    -Wall
    -Wextra
    -O3
)