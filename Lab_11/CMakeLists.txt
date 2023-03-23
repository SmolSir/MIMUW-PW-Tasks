cmake_minimum_required (VERSION 3.8)
project (Lab_11)

set(CMAKE_C_STANDARD "11")
set(CMAKE_C_FLAGS "-g -Wall -Wextra -Wno-sign-compare -Wno-unused-parameter")


add_executable(simple_handler simple_handler.c)

add_executable(signal_mask signal_mask.c)

add_executable(more_info more_info.c)

add_executable(terminator terminator.c)

install(TARGETS DESTINATION .)
