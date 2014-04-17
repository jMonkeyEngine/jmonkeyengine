#ifndef CONFIG_H
#define CONFIG_H

/* Define to the library version */
#define ALSOFT_VERSION "1.15.1"

#define ALIGN(x) __attribute__  ((aligned(x)))

/* Define if we have the Android backend */
/* #define HAVE_ANDROID 1 */

/* Define if we have the ALSA backend */
/* #define HAVE_ALSA */

/* Define if we have the OSS backend */
/* #cmakedefine HAVE_OSS */

/* Define if we have the Solaris backend */
/* #cmakedefine HAVE_SOLARIS */

/* Define if we have the SndIO backend */
/* #cmakedefine HAVE_SNDIO */

/* Define if we have the MMDevApi backend */
/* #cmakedefine HAVE_MMDEVAPI */

/* Define if we have the DSound backend */
/* #cmakedefine HAVE_DSOUND */

/* Define if we have the Windows Multimedia backend */
/* #cmakedefine HAVE_WINMM */

/* Define if we have the PortAudio backend */
/* #cmakedefine HAVE_PORTAUDIO */

/* Define if we have the PulseAudio backend */
/* #cmakedefine HAVE_PULSEAUDIO */

/* Define if we have the CoreAudio backend */
/* #cmakedefine HAVE_COREAUDIO */

/* Define if we have the OpenSL backend */
#define HAVE_OPENSL /* THIS BACKEND WORKS ON >=2.3 Android!! */

/* Define if we have the Wave Writer backend */
/* #cmakedefine HAVE_WAVE */

/* Define if we have dlfcn.h */
#define HAVE_DLFCN_H

/* Define if we have the stat function */
#define HAVE_STAT

/* Define if we have the powf function */
/* #define HAVE_POWF 1 */

/* Define if we have the sqrtf function */
/* #define HAVE_SQRTF 1 */

/* Define if we have the cosf function */
/* #define HAVE_COSF 1 */

/* Define if we have the sinf function */
/* #define HAVE_SINF 1 */

/* Define if we have the acosf function */
/* #define HAVE_ACOSF 1 */

/* Define if we have the asinf function */
/* #define  HAVE_ASINF 1 */

/* Define if we have the atanf function */
/* #define HAVE_ATANF 1 */

/* Define if we have the atan2f function */
/* #define HAVE_ATAN2F 1 */

/* Define if we have the fabsf function */
/* #define HAVE_FABSF 1 */

/* Define if we have the log10f function */
/* #define HAVE_LOG10F 1 */

/* Define if we have the floorf function */
/* #define HAVE_FLOORF 1 */

/* Define if we have the strtof function */
#define HAVE_STRTOF

/* Define if we have stdint.h */
#define HAVE_STDINT_H

/* Define if we have the __int64 type */
/* #cmakedefine HAVE___INT64 */

/* Define to the size of a long int type */
#define SIZEOF_LONG 4

/* Define to the size of a long long int type */
#define SIZEOF_LONG_LONG 8

/* Define if we have GCC's destructor attribute */
#define HAVE_GCC_DESTRUCTOR

/* Define if we have GCC's format attribute */
#define HAVE_GCC_FORMAT

/* Define if we have pthread_np.h */
/* #cmakedefine HAVE_PTHREAD_NP_H */

/* Define if we have arm_neon.h */
/* #cmakedefine HAVE_ARM_NEON_H */

/* Define if we have guiddef.h */
/* #cmakedefine HAVE_GUIDDEF_H */

/* Define if we have guiddef.h */
/* #cmakedefine HAVE_INITGUID_H */

/* Define if we have ieeefp.h */
/* #cmakedefine HAVE_IEEEFP_H */

/* Define if we have float.h */
/* #cmakedefine HAVE_FLOAT_H */

/* Define if we have fpu_control.h */
/* #cmakedefine HAVE_FPU_CONTROL_H */

/* Define if we have fenv.h */
#define HAVE_FENV_H

/* Define if we have fesetround() */
/* #cmakedefine HAVE_FESETROUND */

/* Define if we have _controlfp() */
/* #cmakedefine HAVE__CONTROLFP */

/* Define if we have pthread_setschedparam() */
#define HAVE_PTHREAD_SETSCHEDPARAM

/* Define if we have the restrict keyword */
/* #cmakedefine  HAVE_RESTRICT 1 */

/* Define if we have the __restrict keyword */
#define RESTRICT __restrict

#endif
