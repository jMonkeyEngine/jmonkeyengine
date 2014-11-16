#ifndef JME_UTIL_H
#define JME_UTIL_H

#include <stddef.h>
#include <stdio.h>

#ifndef NDEBUG
#include <android/log.h>
#define LOGI(fmt, ...) __android_log_print(ANDROID_LOG_INFO, \
                       "OpenALSoft", fmt, ##__VA_ARGS__);
#else
#define LOGI(fmt, ...)
#endif

#endif